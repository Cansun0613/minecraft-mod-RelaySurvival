package com.relay.feature.hud;

import com.relay.api.RelayBootstrap;
import com.relay.api.RelayContext;
import com.relay.api.RelayFeature;
import com.relay.api.RelayServices;
import com.relay.api.TurnEndReason;
import com.relay.api.TurnInfo;
import com.relay.api.task.TaskService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD 的服务端半边：把当前状态每秒同步给客户端。
 * 删掉本包后核心不再发送任何 HUD 包，其他功能不受影响。
 */
public final class HudSyncFeature implements RelayFeature, RelayBootstrap, RelayServices.SecondListener {

    private RelayContext ctx;

    @Override
    public String id() {
        return "hud";
    }

    @Override
    public int order() {
        return 40;
    }

    /**
     * 必须在 mod 初始化阶段注册通道：客户端注册接收器时要求通道已存在，
     * 而 RelayFeature.attach 要等到服务器启动，来不及。
     */
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(RelayHudPayload.TYPE, RelayHudPayload.CODEC);
    }

    @Override
    public void attach(RelayContext context) {
        this.ctx = context;
        context.register(RelayServices.SecondListener.class, this);
    }

    @Override
    public void onSecond(RelayContext context) {
        broadcast();
    }

    @Override
    public void onTurnStart(TurnInfo turn) {
        broadcast();
    }

    @Override
    public void onTurnEnd(TurnInfo turn, TurnEndReason reason) {
        broadcast();
    }

    @Override
    public void onSessionEnd(com.relay.api.SessionInfo info, boolean victory) {
        RelayHudPayload payload = new RelayHudPayload(false, 0, 0, 0, 0, "", "", "", (float) ctx.config().hudScale, List.of());
        for (ServerPlayer player : ctx.server().getPlayerList().getPlayers()) {
            send(player, payload);
        }
    }

    @Override
    public void onPlayerJoin(java.util.UUID player) {
        ctx.player(player).ifPresent(p -> {
            if (!ServerPlayNetworking.canSend(p, RelayHudPayload.TYPE)) {
                ctx.messenger().chatTo(player, net.minecraft.network.chat.Component.literal(
                        "⚠ 你的接力生存模组版本和服务器不一致，HUD 不会显示（其他功能正常），请更新到同一版本。")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
                com.relay.RelayMod.LOGGER.warn("玩家 {} 的 relay 版本与服务器不一致，跳过 HUD 同步", p.getGameProfile().name());
                return;
            }
            if (ctx.turns().running()) {
                send(p, build());
            } else {
                send(p, new RelayHudPayload(false, 0, 0, 0, 0, "", "", "", (float) ctx.config().hudScale, List.of()));
            }
        });
    }

    private void broadcast() {
        if (ctx == null || !ctx.turns().running()) {
            return;
        }
        RelayHudPayload payload = build();
        for (ServerPlayer player : ctx.server().getPlayerList().getPlayers()) {
            send(player, payload);
        }
    }

    private RelayHudPayload build() {
        TurnInfo info = ctx.turns().info();
        List<RelayHudPayload.TaskLine> lines = new ArrayList<>();
        ctx.find(TaskService.class).ifPresent(service -> {
            for (TaskService.TaskView view : service.active()) {
                lines.add(new RelayHudPayload.TaskLine(view.category(), view.tier(), view.title(),
                        view.description(), view.progress(), view.target(), view.secondsLeft(), view.timeLimit(),
                        view.successSeconds(), view.failSeconds(), view.state(), view.timed()));
            }
        });
        return new RelayHudPayload(true, info.index(), info.total(), info.remaining(),
                info.turnSeconds(), info.playerName(),
                info.playerId() == null ? "" : info.playerId().toString(),
                lines.isEmpty() ? "" : lines.get(0).category(),
                (float) ctx.config().hudScale, lines);
    }

    /** 客户端没装模组时跳过，避免给原版客户端发未知通道的包。 */
    private void send(ServerPlayer player, RelayHudPayload payload) {
        try {
            if (ServerPlayNetworking.canSend(player, RelayHudPayload.TYPE)) {
                ServerPlayNetworking.send(player, payload);
            }
        } catch (Throwable ignored) {
        }
    }
}
