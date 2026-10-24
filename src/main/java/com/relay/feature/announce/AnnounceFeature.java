package com.relay.feature.announce;

import com.relay.api.RelayContext;
import com.relay.api.RelayFeature;
import com.relay.api.SessionInfo;
import com.relay.api.TurnEndReason;
import com.relay.api.TurnInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * 播报功能：开局/接棒/倒计时提醒/结束。
 * 移除后核心与其他功能照常运行，只是没有提示音和标题。
 */
public final class AnnounceFeature implements RelayFeature {

    private RelayContext ctx;
    private int lastSecond = -1;

    @Override
    public String id() {
        return "announce";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void attach(RelayContext context) {
        this.ctx = context;
    }

    @Override
    public void onSessionStart(SessionInfo info) {
        ctx.messenger().title(Component.literal("接力生存").withStyle(ChatFormatting.GOLD),
                Component.literal("共 " + info.order().size() + " 棒 · 每棒 " + (info.turnSeconds() / 60) + " 分钟"),
                10, 40, 10);
        ctx.messenger().sound(SoundEvents.END_PORTAL_SPAWN, 0.4f, 1.2f);
    }

    @Override
    public void onTurnStart(TurnInfo turn) {
        lastSecond = -1;
        ctx.messenger().title(
                Component.literal("第 " + (turn.index() + 1) + " 棒").withStyle(ChatFormatting.YELLOW),
                Component.literal(turn.playerName() + " 接棒").withStyle(ChatFormatting.WHITE),
                5, 30, 10);
        ctx.messenger().chat(Component.literal("▶ " + turn.playerName() + " 接棒，计时开始")
                .withStyle(ChatFormatting.GOLD));
        ctx.messenger().sound(SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 1.5f);
    }

    @Override
    public void onSecond(TurnInfo turn) {
        int remaining = turn.remaining();
        if (remaining == lastSecond) {
            return;
        }
        lastSecond = remaining;
        int warn = ctx.config().warnSeconds;
        if (warn > 0 && remaining == warn) {
            ctx.messenger().chat(Component.literal("⏳ 还剩 " + (warn / 60) + " 分钟").withStyle(ChatFormatting.YELLOW));
            ctx.messenger().sound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f, 1f);
        } else if (warn > 0 && remaining == warn / 2) {
            ctx.messenger().actionBar(Component.literal("⚠ 剩余 " + remaining + " 秒").withStyle(ChatFormatting.RED));
        } else if (remaining <= 10 && remaining > 0) {
            ctx.messenger().actionBar(Component.literal("⚠ " + remaining + " 秒").withStyle(ChatFormatting.RED));
            ctx.messenger().sound(SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 1.8f);
        }
    }

    @Override
    public void onTurnEnd(TurnInfo turn, TurnEndReason reason) {
        String text = switch (reason) {
            case TIME_UP -> "⏰ 时间到，强制交接";
            case DEATH -> "☠ 阵亡，交接";
            case SKIP -> "⏭ 提前交接";
            case PLAYER_LEFT -> "⚠ 玩家离线，交接";
            case VICTORY -> "🏆 通关";
            case MANUAL_STOP -> "■ 手动结束";
        };
        ctx.messenger().chat(Component.literal(text + "（" + turn.playerName() + "）").withStyle(ChatFormatting.GRAY));
        ctx.messenger().sound(SoundEvents.BELL_RESONATE, 0.6f, 0.8f);
    }

    @Override
    public void onSessionEnd(SessionInfo info, boolean victory) {
        ctx.messenger().chat(Component.literal(victory ? "🏆 接力通关！" : "■ 接力结束").withStyle(
                victory ? ChatFormatting.GOLD : ChatFormatting.RED));
    }
}
