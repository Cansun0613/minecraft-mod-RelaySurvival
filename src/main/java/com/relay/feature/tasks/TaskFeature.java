package com.relay.feature.tasks;

import com.relay.RelayMod;
import com.relay.api.RelayContext;
import com.relay.api.RelayFeature;
import com.relay.api.TurnEndReason;
import com.relay.api.TurnInfo;
import com.relay.api.task.TaskService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 随机任务功能。核心不认识它：它只通过 RelayContext 提供的接口干活，
 * 并把 TaskService 注册进去供 HUD 等功能按接口读取。
 */
public final class TaskFeature implements RelayFeature, TaskService {

    private RelayContext ctx;
    private TaskPool pool;
    private boolean enabled;

    private final List<TaskInstance> active = new ArrayList<>();
    private final Random random = new Random();
    private Vec3 startPos;
    private UUID currentPlayer;
    private long finishSeq;
    private int refillsUsed;

    @Override
    public String id() {
        return "tasks";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public void attach(RelayContext context) {
        this.ctx = context;
        this.enabled = context.config().tasksEnabled && context.config().taskCount > 0;
        if (!enabled) {
            RelayMod.LOGGER.info("任务功能未启用（config: tasksEnabled/taskCount）");
            return;
        }
        Path path = context.server().getServerDirectory().resolve("config").resolve("relay-tasks.json");
        this.pool = TaskPool.load(path);
        context.register(TaskService.class, this);

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                addBlockProgress(serverPlayer, state);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof ServerPlayer killer) {
                String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                addProgress(killer, TaskType.MOB_KILL, entityId, 1);
            }
        });
        RelayMod.LOGGER.info("任务池已加载 {} 条", pool.size());
    }

    @Override
    public void onSessionStart(com.relay.api.SessionInfo info) {
        active.clear();
    }

    @Override
    public void onTurnStart(TurnInfo turn) {
        active.clear();
        finishSeq = 0;
        refillsUsed = 0;
        currentPlayer = turn.playerId();
        if (!enabled || currentPlayer == null) {
            return;
        }
        ctx.player(currentPlayer).ifPresent(player -> startPos = player.position());
        active.addAll(TaskInstance.pick(pool.all(), ctx.config().taskCount, random, active, this::weightOf));
        for (TaskInstance instance : active) {
            instance.setTimed(ctx.config().taskTimeLimit);
        }
        if (!active.isEmpty()) {
            ctx.messenger().chatTo(currentPlayer, Component.literal("本棒任务已刷新，共 " + active.size() + " 个")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public void onSecond(TurnInfo turn) {
        if (!enabled || active.isEmpty()) {
            return;
        }
        ServerPlayer player = ctx.player(turn.playerId()).orElse(null);
        if (player == null) {
            return;
        }
        List<TaskInstance> finished = new ArrayList<>();
        List<TaskInstance> failed = new ArrayList<>();

        for (TaskInstance instance : active) {
            if (!instance.isActive()) {
                continue;
            }
            instance.tickSecond();
            updateProgress(player, instance);
            if (instance.done()) {
                finished.add(instance);
            } else if (instance.expired()) {
                failed.add(instance);
            }
        }
        for (TaskInstance instance : finished) {
            complete(instance);
        }
        for (TaskInstance instance : failed) {
            fail(instance);
        }
    }

    @Override
    public void onTurnEnd(TurnInfo turn, TurnEndReason reason) {
        active.clear();
        startPos = null;
    }

    @Override
    public void detach() {
        active.clear();
        pool = null;
        enabled = false;
    }

    private void updateProgress(ServerPlayer player, TaskInstance instance) {
        TaskDefinition def = instance.definition();
        switch (def.type()) {
            case DISTANCE -> {
                if (startPos != null) {
                    double dx = player.getX() - startPos.x;
                    double dz = player.getZ() - startPos.z;
                    instance.setProgress((int) Math.sqrt(dx * dx + dz * dz));
                }
            }
            case ITEM_COUNT -> instance.setProgress(countItems(player, def.param()));
            case STAT -> {
                try {
                    instance.setProgress(player.getStats().getValue(
                            net.minecraft.stats.Stats.CUSTOM,
                            net.minecraft.resources.Identifier.parse(def.param())));
                } catch (Exception ignored) {
                }
            }
            case ITEM_SET -> {
                int have = 0;
                for (String id : def.param().split(",")) {
                    if (!id.isBlank() && countItems(player, id.trim()) > 0) {
                        have++;
                    }
                }
                instance.setProgress(have);
            }
            case ITEM_ANY -> {
                for (String id : def.param().split(",")) {
                    if (!id.isBlank() && countItems(player, id.trim()) > 0) {
                        instance.setProgress(def.target());
                        break;
                    }
                }
            }
            case DIMENSION -> {
                String dimension = player.level().dimension().identifier().toString();
                if (dimension.equals(def.param())) {
                    instance.setProgress(def.target());
                }
            }
            case ADVANCEMENT -> {
                if (hasAdvancement(player, def.param())) {
                    instance.setProgress(def.target());
                }
            }
            default -> {
                // BLOCK_BREAK / MOB_KILL 由事件累加
            }
        }
    }

    private void addBlockProgress(ServerPlayer player, net.minecraft.world.level.block.state.BlockState state) {
        if (!enabled || currentPlayer == null || !player.getUUID().equals(currentPlayer)) {
            return;
        }
        for (TaskInstance instance : active) {
            TaskDefinition def = instance.definition();
            if (def.type() == TaskType.BLOCK_BREAK && matchesBlock(def.param(), state)) {
                instance.addProgress(1);
            }
        }
    }

    /** 支持三种写法：单个方块 id、逗号分隔的多个 id、#命名空间:标签。 */
    private boolean matchesBlock(String spec, net.minecraft.world.level.block.state.BlockState state) {
        if (spec == null || spec.isBlank()) {
            return false;
        }
        if (spec.startsWith("#")) {
            try {
                var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                        net.minecraft.resources.Identifier.parse(spec.substring(1)));
                return state.is(tag, s -> true);
            } catch (Exception e) {
                return false;
            }
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (String id : spec.split(",")) {
            if (blockId.equals(id.trim())) {
                return true;
            }
        }
        return false;
    }

    private void addProgress(ServerPlayer player, TaskType type, String param, int delta) {
        if (!enabled || currentPlayer == null || !player.getUUID().equals(currentPlayer)) {
            return;
        }
        for (TaskInstance instance : active) {
            TaskDefinition def = instance.definition();
            if (def.type() == type && def.param().equals(param)) {
                instance.addProgress(delta);
            }
        }
    }

    private void complete(TaskInstance instance) {
        // 完成的任务留在列表里变灰打勾，不再补新任务（和原版一致）
        instance.markDone(++finishSeq);
        TaskDefinition def = instance.definition();
        ctx.timer().add(def.successSeconds(), "任务完成");
        ctx.messenger().chat(Component.literal("✔ " + def.title() + " 完成，时间 +" + def.successSeconds() + " 秒")
                .withStyle(ChatFormatting.GREEN));
        if (currentPlayer != null) {
            ctx.messenger().soundTo(currentPlayer, SoundEvents.PLAYER_LEVELUP, 0.7f, 1.4f);
        }
        // 刷新模式下完成的任务也保留（沉底），只补一个新任务
        if (canRefill() && refill() > 0) {
            refillsUsed++;
        }
    }

    private void fail(TaskInstance instance) {
        instance.markFailed(++finishSeq);
        TaskDefinition def = instance.definition();
        ctx.timer().add(-def.failSeconds(), "任务失败");
        ctx.messenger().chat(Component.literal("✘ " + def.title() + " 超时，时间 -" + def.failSeconds() + " 秒")
                .withStyle(ChatFormatting.RED));
        if (currentPlayer != null) {
            ctx.messenger().soundTo(currentPlayer, SoundEvents.VILLAGER_NO, 0.8f, 1f);
        }
        // 刷新模式下完成的任务也保留（沉底），只补一个新任务
        if (canRefill() && refill() > 0) {
            refillsUsed++;
        }
    }

    private boolean canRefill() {
        if (!ctx.config().taskRefresh) {
            return false;
        }
        return ctx.config().taskRefills < 0 || refillsUsed < ctx.config().taskRefills;
    }

    /** 只补"进行中"的任务到 taskCount。 */
    private int refill() {
        if (!ctx.turns().running()) {
            return 0;
        }
        int added = 0;
        long live = active.stream().filter(TaskInstance::isActive).count();
        while (live < ctx.config().taskCount) {
            List<TaskInstance> picked = TaskInstance.pick(pool.all(), 1, random, active, this::weightOf);
            if (picked.isEmpty()) {
                break;
            }
            picked.get(0).setTimed(ctx.config().taskTimeLimit);
            active.addAll(picked);
            live++;
            added++;
        }
        return added;
    }

    private double weightOf(TaskDefinition def) {
        List<Double> weights = ctx.config().tierWeights;
        if (weights == null || weights.size() < 5) {
            return 1.0;
        }
        Double weight = weights.get(def.tierIndex());
        return weight == null || weight <= 0 ? 0.01 : weight;
    }

    private int countItems(ServerPlayer player, String itemId) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean hasAdvancement(ServerPlayer player, String advancementId) {
        try {
            var id = net.minecraft.resources.Identifier.parse(advancementId);
            var holder = ctx.server().getAdvancements().get(id);
            return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------- TaskService

    @Override
    public List<TaskView> active() {
        // 进行中的排前面，已完成/超时的按发生顺序排到后面
        List<TaskInstance> ordered = new ArrayList<>(active);
        ordered.sort(Comparator.comparingInt((TaskInstance i) -> i.isActive() ? 0 : 1)
                .thenComparingLong(i -> i.isActive() ? 0L : i.finishedAt()));
        List<TaskView> views = new ArrayList<>(ordered.size());
        for (TaskInstance instance : ordered) {
            TaskDefinition def = instance.definition();
            views.add(new TaskView(def.id(), def.category(), def.tier(), def.title(), def.description(),
                    instance.progress(), def.target(), instance.secondsLeft(), def.timeLimit(),
                    def.successSeconds(), def.failSeconds(), instance.state().name(), instance.isTimed()));
        }
        return views;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }
}
