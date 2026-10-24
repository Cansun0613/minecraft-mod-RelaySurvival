package com.relay.core;

import com.relay.RelayMod;
import com.relay.api.RelayConfig;
import com.relay.api.RelayContext;
import com.relay.api.RelayEvents;
import com.relay.api.RelayMessenger;
import com.relay.api.RelayServices;
import com.relay.api.RelaySnapshots;
import com.relay.api.RelayTimer;
import com.relay.api.RelayTurns;
import com.relay.api.SessionInfo;
import com.relay.api.TurnEndReason;
import com.relay.api.TurnInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RelayRuntime implements RelayContext, RelayTurns, RelayTimer {

    private final MinecraftServer server;
    private final Path configPath;
    private final RelayConfig config;
    private final Countdown countdown = new Countdown();
    private final TurnQueue queue = new TurnQueue();
    private final SnapshotService snapshots;
    private final Messenger messenger;
    private final EventBus events = new EventBus();
    private final FeatureRegistry features = new FeatureRegistry();
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final List<RelayServices.SecondListener> secondListeners = new ArrayList<>();
    private final Map<UUID, String> names = new HashMap<>();

    private boolean running;
    private int tickCounter;
    private boolean pendingTimeUp;
    private SessionInfo session;

    public RelayRuntime(MinecraftServer server) {
        this.server = server;
        this.configPath = server.getServerDirectory().resolve("config").resolve("relay.json");
        this.config = RelayConfig.load(configPath);
        this.snapshots = new SnapshotService(server);
        this.messenger = new Messenger(server);
    }

    public void bootstrap() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            names.put(player.getUUID(), player.getGameProfile().name());
        }
        // 每次启动都回写一次，新增的配置项才会出现在文件里
        saveConfig();
        features.load(this);
    }

    public void shutdown() {
        running = false;
        features.shutdown();
        snapshots.clear();
        services.clear();
        secondListeners.clear();
    }

    public FeatureRegistry features() {
        return features;
    }

    // ------------------------------------------------------------------ tick

    public void tick() {
        if (!running) {
            return;
        }
        ServerPlayer current = currentPlayer();
        if (pendingTimeUp && (current == null || current.isAlive())) {
            pendingTimeUp = false;
            handoff(TurnEndReason.TIME_UP);
            return;
        }
        tickCounter++;
        // 每 5 tick（0.25 秒）检查一次旁观视角，按 Shift 脱离后会被重新锁回去
        if (config.lockView && tickCounter % 5 == 0) {
            enforceCameraLock();
        }
        if (tickCounter % 20 != 0) {
            return;
        }
        countdown.tick();
        TurnInfo info = info();
        features.second(info);
        for (RelayServices.SecondListener listener : List.copyOf(secondListeners)) {
            try {
                listener.onSecond(this);
            } catch (Throwable t) {
                RelayMod.LOGGER.error("每秒回调异常", t);
            }
        }
        if (countdown.expired()) {
            pendingTimeUp = true;
        }
    }

    // --------------------------------------------------------------- session

    public boolean startSession(List<UUID> order) {
        List<UUID> cleaned = new ArrayList<>();
        for (UUID id : order) {
            if (!cleaned.contains(id)) {
                cleaned.add(id);
            }
        }
        if (cleaned.size() < config.minPlayers) {
            messenger.chat(Component.literal("人数不足，至少需要 " + config.minPlayers + " 人").withStyle(ChatFormatting.RED));
            return false;
        }
        queue.start(cleaned, config.rounds);
        for (UUID id : cleaned) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                names.put(id, p.getGameProfile().name());
                p.setGameMode(GameType.SPECTATOR);
                p.setHealth(p.getMaxHealth());
                p.getFoodData().setFoodLevel(20);
            }
        }
        snapshots.clear();
        running = true;
        tickCounter = 0;
        pendingTimeUp = false;
        countdown.reset(config.turnSeconds);
        session = new SessionInfo(cleaned, config.turnSeconds);
        features.startSession(session);
        String roundText = config.rounds <= 0 ? "无限轮" : (config.rounds + " 轮");
        messenger.chat(Component.literal("接力生存开始！" + cleaned.size() + " 人 · " + roundText + " · 每棒 ")
                .append(fmtSeconds(config.turnSeconds)).withStyle(ChatFormatting.GOLD));
        beginTurn(queue.current());
        return true;
    }

    private void beginTurn(UUID id) {
        if (id == null) {
            stop(false);
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) {
            names.putIfAbsent(id, "离线玩家");
            if (queue.advance()) {
                beginTurn(queue.current());
            } else {
                stop(false);
            }
            return;
        }
        snapshots.applyTo(player);
        snapshots.consume();
        player.setGameMode(GameType.SURVIVAL);
        // 背包是逐格改的，必须主动推给客户端，否则客户端还显示旧物品
        syncInventory(player);
        if (config.setRespawnOnHandoff) {
            player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                    LevelData.RespawnData.of(player.level().dimension(), player.blockPosition(),
                            player.getYRot(), player.getXRot()),
                    true), false);
        }
        countdown.reset(config.turnSeconds);
        countdown.setPaused(false);
        features.turnStart(info());
        syncSpectators();
    }

    /** 旁观者：锁定视角看当前玩家；或者（非锁定模式）全部传送到当前玩家身边并解除锁定。 */
    private void syncSpectators() {
        ServerPlayer active = currentPlayer();
        if (active == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getUUID().equals(active.getUUID())) {
                resetCamera(player);
                continue;
            }
            if (player.gameMode() != GameType.SPECTATOR || !queue.order().contains(player.getUUID())) {
                continue;
            }
            if (config.lockView) {
                player.setCamera(active);
                player.connection.send(new ClientboundSetCameraPacket(active));
            } else {
                resetCamera(player);
                player.teleportTo((net.minecraft.server.level.ServerLevel) active.level(),
                        active.getX(), active.getY(), active.getZ(), java.util.Set.of(),
                        active.getYRot(), active.getXRot(), false);
            }
        }
    }

    private void syncInventory(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket(
                    i, inventory.getItem(i)));
        }
        player.inventoryMenu.broadcastChanges();
    }

    private void enforceCameraLock() {
        ServerPlayer active = currentPlayer();
        if (active == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getUUID().equals(active.getUUID())
                    || player.gameMode() != GameType.SPECTATOR
                    || !queue.order().contains(player.getUUID())) {
                continue;
            }
            if (player.getCamera() != active) {
                player.setCamera(active);
                player.connection.send(new ClientboundSetCameraPacket(active));
            }
        }
    }

    private void resetCamera(ServerPlayer player) {
        player.setCamera(null);
        player.connection.send(new ClientboundSetCameraPacket(player));
    }

    @Override
    public void handoff(TurnEndReason reason) {
        if (!running) {
            return;
        }
        TurnInfo info = info();
        UUID currentId = queue.current();
        ServerPlayer current = currentId == null ? null : server.getPlayerList().getPlayer(currentId);
        boolean willContinue = queue.hasNext();
        if (current != null) {
            // 只有真的还有下一棒时才把背包"交出去"；否则最后一棒结束时会把物品清空。
            if (current.isAlive() && willContinue) {
                snapshots.capture(current);
                // 先把其他人身上残留的旧物品清掉，再把上一棒的物品交给下一棒
                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    if (!other.getUUID().equals(current.getUUID())) {
                        other.getInventory().clearContent();
                        syncInventory(other);
                    }
                }
                current.getInventory().clearContent();
                syncInventory(current);
                current.experienceLevel = 0;
                current.experienceProgress = 0f;
                current.totalExperience = 0;
            }
            current.setGameMode(GameType.SPECTATOR);
        }
        pendingTimeUp = false;
        features.turnEnd(info, reason);
        if (!queue.advance()) {
            stop(false);
            return;
        }
        beginTurn(queue.current());
    }

    @Override
    public void skip() {
        handoff(TurnEndReason.SKIP);
    }

    @Override
    public void stop(boolean victory) {
        if (!running) {
            return;
        }
        running = false;
        countdown.setPaused(true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.gameMode() == GameType.SPECTATOR) {
                resetCamera(player);
                player.setGameMode(GameType.SURVIVAL);
            }
        }
        features.endSession(session, victory);
        if (victory) {
            messenger.title(Component.literal("通关！").withStyle(ChatFormatting.GOLD),
                    Component.literal("接力成功"), 10, 60, 20);
            messenger.sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        } else {
            messenger.title(Component.literal("接力结束").withStyle(ChatFormatting.RED),
                    Component.literal("世界还在，再来一局？"), 10, 50, 20);
        }
        RelayMenu.broadcast(this);
    }

    // ----------------------------------------------------------------- death

    public void onPlayerDeath(ServerPlayer player) {
        if (!running || !player.getUUID().equals(queue.current())) {
            return;
        }
        features.death(player.getUUID());
        if (config.deathPenaltySeconds > 0) {
            countdown.add(-config.deathPenaltySeconds);
            messenger.chat(Component.literal("☠ " + player.getGameProfile().name() + " 阵亡，剩余时间 -"
                    + config.deathPenaltySeconds + " 秒").withStyle(ChatFormatting.RED));
            messenger.sound(SoundEvents.BELL_RESONATE, 0.8f, 1f);
        }
        if (countdown.expired()) {
            pendingTimeUp = true;
        }
    }

    /**
     * 中途把玩家加进接力队列（开局后加入/掉线重连用）。
     * 返回 0=未开局，1=已在队列，2=加入成功。
     */
    public int addPlayer(ServerPlayer player) {
        if (!running) {
            return 0;
        }
        if (queue.order().contains(player.getUUID())) {
            return 1;
        }
        queue.add(player.getUUID());
        names.put(player.getUUID(), player.getGameProfile().name());
        player.setGameMode(GameType.SPECTATOR);
        if (config.lockView) {
            ServerPlayer active = currentPlayer();
            if (active != null) {
                player.setCamera(active);
                player.connection.send(new ClientboundSetCameraPacket(active));
            }
        }
        messenger.chat(Component.literal("＋ " + player.getGameProfile().name()
                + " 已加入接力队列（第 " + queue.total() + " 位）").withStyle(ChatFormatting.AQUA));
        return 2;
    }

    public void onDragonKilled() {
        if (running && config.victoryOnDragon) {
            stop(true);
        }
    }

    public void onJoin(ServerPlayer player) {
        names.put(player.getUUID(), player.getGameProfile().name());
        features.join(player.getUUID());
        if (!running) {
            return;
        }
        if (queue.order().contains(player.getUUID())) {
            player.setGameMode(GameType.SPECTATOR);
            if (config.lockView) {
                ServerPlayer active = currentPlayer();
                if (active != null) {
                    player.setCamera(active);
                    player.connection.send(new ClientboundSetCameraPacket(active));
                }
            }
        }
    }

    public void onLeave(ServerPlayer player) {
        names.put(player.getUUID(), player.getGameProfile().name());
        features.leave(player.getUUID());
        if (!running) {
            return;
        }
        if (player.getUUID().equals(queue.current())) {
            messenger.chat(Component.literal("⚠ " + player.getGameProfile().name() + " 退出，提前交接")
                    .withStyle(ChatFormatting.YELLOW));
            queue.remove(player.getUUID());
            handoff(TurnEndReason.PLAYER_LEFT);
        } else {
            queue.remove(player.getUUID());
        }
    }

    // ---------------------------------------------------------------- getters

    public ServerPlayer currentPlayer() {
        UUID id = queue.current();
        return id == null ? null : server.getPlayerList().getPlayer(id);
    }

    public String nameOfCached(UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) {
            names.put(id, p.getGameProfile().name());
        }
        return names.getOrDefault(id, id.toString().substring(0, 8));
    }

    @Override
    public RelayConfig config() {
        return config;
    }

    @Override
    public RelayTimer timer() {
        return this;
    }

    @Override
    public RelayTurns turns() {
        return this;
    }

    @Override
    public RelaySnapshots snapshots() {
        return snapshots;
    }

    @Override
    public RelayMessenger messenger() {
        return messenger;
    }

    @Override
    public RelayEvents events() {
        return events;
    }

    @Override
    public MinecraftServer server() {
        return server;
    }

    @Override
    public <T> void register(Class<T> type, T implementation) {
        services.put(type, implementation);
        if (implementation instanceof RelayServices.SecondListener listener) {
            secondListeners.add(listener);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> find(Class<T> type) {
        return Optional.ofNullable((T) services.get(type));
    }

    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (Exception ignored) {
        }
        config.save(configPath);
    }

    // ----------------------------------------------------------------- timer

    @Override
    public int remaining() {
        return countdown.remaining();
    }

    @Override
    public int turnSeconds() {
        return config.turnSeconds;
    }

    @Override
    public void setRemaining(int seconds) {
        countdown.set(seconds);
    }

    @Override
    public void add(int seconds) {
        countdown.add(seconds);
    }

    @Override
    public void add(int seconds, String source) {
        countdown.add(seconds);
        events.publish(new RelayEvents.TimeAdjusted(queue.current(), seconds, source));
    }

    @Override
    public void reset() {
        countdown.reset(config.turnSeconds);
    }

    @Override
    public boolean paused() {
        return countdown.paused();
    }

    @Override
    public void setPaused(boolean paused) {
        countdown.setPaused(paused);
    }

    // ----------------------------------------------------------------- turns

    @Override
    public boolean running() {
        return running;
    }

    @Override
    public int index() {
        return queue.turnNumber();
    }

    @Override
    public int total() {
        return queue.totalTurns();
    }

    @Override
    public Optional<UUID> current() {
        return Optional.ofNullable(queue.current());
    }

    @Override
    public Optional<UUID> next() {
        return Optional.ofNullable(queue.peekNext());
    }

    @Override
    public List<UUID> order() {
        return queue.order();
    }

    @Override
    public TurnInfo info() {
        UUID id = queue.current();
        return new TurnInfo(
                queue.turnNumber(),
                queue.totalTurns(),
                id,
                id == null ? "?" : nameOfCached(id),
                countdown.remaining(),
                config.turnSeconds);
    }

    @Override
    public void start(List<UUID> order) {
        startSession(order);
    }

    private static Component fmtSeconds(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return Component.literal(m + " 分 " + s + " 秒");
    }
}
