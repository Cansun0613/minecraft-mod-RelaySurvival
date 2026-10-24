package com.relay.core;

import com.relay.api.RelaySnapshots;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 交接用的玩家状态快照：背包（含盔甲/副手）、经验、血量、饥饿、状态效果、坐标朝向。
 * 只保留"最近一次交接"的快照——它属于下一棒，而不是被抓取的那个玩家。
 */
public final class SnapshotService implements RelaySnapshots {

    public record PlayerState(
            List<ItemStack> items,
            int selected,
            int xpLevel,
            float xpProgress,
            int totalXp,
            float health,
            int food,
            float saturation,
            List<MobEffectInstance> effects,
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
    }

    private final MinecraftServer server;
    private PlayerState pending;

    public SnapshotService(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void capture(ServerPlayer player) {
        var inv = player.getInventory();
        List<ItemStack> items = new ArrayList<>(inv.getContainerSize());
        for (int i = 0; i < inv.getContainerSize(); i++) {
            items.add(inv.getItem(i).copy());
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            effects.add(new MobEffectInstance(effect));
        }
        pending = new PlayerState(
                items,
                inv.getSelectedSlot(),
                player.experienceLevel,
                player.experienceProgress,
                player.totalExperience,
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                effects,
                player.level().dimension(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        );
    }

    @Override
    public boolean applyTo(ServerPlayer player) {
        PlayerState state = pending;
        if (state == null) {
            return false;
        }
        ServerLevel level = server.getLevel(state.dimension());
        if (level != null) {
            player.teleportTo(level, state.x(), state.y(), state.z(), java.util.Set.of(), state.yaw(), state.pitch(), false);
        }

        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = i < state.items().size() ? state.items().get(i).copy() : ItemStack.EMPTY;
            inv.setItem(i, stack);
        }
        inv.setSelectedSlot(state.selected());

        player.experienceLevel = state.xpLevel();
        player.experienceProgress = state.xpProgress();
        player.totalExperience = state.totalXp();

        player.setHealth(Math.min(state.health(), player.getMaxHealth()));
        player.getFoodData().setFoodLevel(state.food());
        player.getFoodData().setSaturation(state.saturation());

        player.removeAllEffects();
        for (MobEffectInstance effect : state.effects()) {
            player.addEffect(new MobEffectInstance(effect));
        }
        return true;
    }

    /** 交接完成后清空，避免下一棒重复拿到同一份快照。 */
    public void consume() {
        pending = null;
    }

    @Override
    public boolean has(UUID player) {
        return pending != null;
    }

    @Override
    public void clear() {
        pending = null;
    }
}
