package com.relay.core;

import com.relay.api.TurnInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天栏里的"设置面板"：全部是原版可点击文本（ClickEvent.RunCommand），
 * 点一下就等于执行对应指令，不需要额外 GUI。
 */
public final class RelayMenu {

    private RelayMenu() {
    }

    public static void send(CommandSourceStack source, RelayRuntime runtime) {
        for (Component line : build(runtime)) {
            source.sendSuccess(() -> line, false);
        }
    }

    public static void broadcast(RelayRuntime runtime) {
        List<Component> lines = build(runtime);
        for (ServerPlayer player : runtime.server().getPlayerList().getPlayers()) {
            for (Component line : lines) {
                player.sendSystemMessage(line);
            }
        }
    }

    private static List<Component> build(RelayRuntime runtime) {
        List<Component> lines = new ArrayList<>();
        var config = runtime.config();

        lines.add(Component.literal("— 接力生存 · 点击选项直接修改 —")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        MutableComponent status = Component.literal("状态：");
        if (runtime.running()) {
            TurnInfo info = runtime.info();
            String turn = info.total() > 0
                    ? "第 " + (info.index() + 1) + "/" + info.total() + " 棒"
                    : "第 " + (info.index() + 1) + " 棒";
            status.append(Component.literal(turn + " · " + info.playerName() + " · 剩余 "
                    + Countdown.format(info.remaining())).withStyle(ChatFormatting.YELLOW));
        } else {
            status.append(Component.literal("未开局").withStyle(ChatFormatting.GRAY));
        }
        lines.add(status);

        lines.add(Component.literal("")
                .append(button("[▶ 开局]", "/relay start", "所有在线玩家按加入顺序开局"))
                .append(Component.literal("  "))
                .append(button("[⏭ 跳过]", "/relay skip", "立刻交接给下一棒"))
                .append(Component.literal("  "))
                .append(button("[■ 结束]", "/relay stop", "结束本局"))
                .append(Component.literal("  "))
                .append(button("[↻ 刷新]", "/relay", "刷新这个面板")));

        lines.add(option("每棒时长", Countdown.format(config.turnSeconds),
                button("[5 分]", "/relay time 300", "每棒 5 分钟"),
                button("[10 分]", "/relay time 600", "每棒 10 分钟"),
                button("[15 分]", "/relay time 900", "每棒 15 分钟"),
                button("[20 分]", "/relay time 1200", "每棒 20 分钟")));

        lines.add(option("死亡扣时", config.deathPenaltySeconds + " 秒",
                button("[0]", "/relay penalty 0", "死亡不扣时间"),
                button("[30]", "/relay penalty 30", "死亡 -30 秒"),
                button("[60]", "/relay penalty 60", "死亡 -60 秒"),
                button("[120]", "/relay penalty 120", "死亡 -120 秒")));

        lines.add(option("每棒任务", config.taskCount + " 个",
                button("[0]", "/relay tasks 0", "关闭任务"),
                button("[1]", "/relay tasks 1", "每棒 1 个任务"),
                button("[2]", "/relay tasks 2", "每棒 2 个任务"),
                button("[3]", "/relay tasks 3", "每棒 3 个任务"),
                button("[4]", "/relay tasks 4", "每棒 4 个任务")));

        lines.add(option("旁观视角", config.lockView ? "锁定" : "自由",
                button("[锁定]", "/relay lockview on", "旁观者视角跟随当前玩家"),
                button("[自由]", "/relay lockview off", "自由旁观，换人时统一传送到交接点")));
        lines.add(option("任务刷新", config.taskRefresh ? "开" : "关",
                button("[开]", "/relay refresh on", "完成/超时后立刻补一个新任务"),
                button("[关]", "/relay refresh off", "完成的任务保留在原地变灰")));

        lines.add(option("刷新次数", config.taskRefills < 0 ? "无限" : (config.taskRefills + " 次（任务总数 " + (config.taskCount + config.taskRefills) + "）"),
                button("[0]", "/relay refills 0", "不补任务，只保留开局抽到的"),
                button("[1]", "/relay refills 1", "补 1 个（总数 = 每棒任务数 + 1）"),
                button("[2]", "/relay refills 2", "补 2 个"),
                button("[3]", "/relay refills 3", "补 3 个"),
                button("[无限]", "/relay refills -1", "只要完成任务就补")));
        lines.add(option("任务限时", config.taskTimeLimit ? "开" : "关",
                button("[开]", "/relay timelimit on", "任务会倒计时，超时扣时间"),
                button("[关]", "/relay timelimit off", "任务永不超时")));
        lines.add(option("龙死通关", config.victoryOnDragon ? "开" : "关",
                button("[开]", "/relay dragonwin on", "击杀末影龙立即结束本局"),
                button("[关]", "/relay dragonwin off", "击杀末影龙不结束，继续接力")));

        lines.add(option("交接重生点", config.setRespawnOnHandoff ? "开" : "关",
                button("[开]", "/relay respawn on", "每棒把重生点设在交接位置"),
                button("[关]", "/relay respawn off", "不修改重生点")));

        lines.add(option("剩余提醒", config.warnSeconds <= 0 ? "关" : (config.warnSeconds + " 秒"),
                button("[30]", "/relay warn 30", "剩 30 秒时提醒"),
                button("[60]", "/relay warn 60", "剩 60 秒时提醒"),
                button("[120]", "/relay warn 120", "剩 120 秒时提醒"),
                button("[关]", "/relay warn 0", "关闭提醒")));
        lines.add(option("轮数", config.rounds <= 0 ? "无限" : config.rounds + " 轮",
                button("[1 轮]", "/relay rounds 1", "每人只上一次"),
                button("[2 轮]", "/relay rounds 2", "每人上两次（1→2→1→2）"),
                button("[3 轮]", "/relay rounds 3", "每人上三次"),
                button("[无限]", "/relay rounds 0", "一直轮换，直到手动结束/通关")));

        lines.add(option("HUD 缩放", String.format("%.2f", config.hudScale),
                button("[0.6]", "/relay hudscale 0.6", "更小的 HUD"),
                button("[0.75]", "/relay hudscale 0.75", "默认大小"),
                button("[0.9]", "/relay hudscale 0.9", "稍大"),
                button("[1.0]", "/relay hudscale 1.0", "原始大小")));

        lines.add(option("难度权重", weightsText(config.tierWeights),
                button("[默认]", "/relay weights default", "S/A/B/C/D = 1/2/4/7/10"),
                button("[简单多]", "/relay weights easy", "简单任务更常出现"),
                button("[困难多]", "/relay weights hard", "困难任务更常出现")));

        lines.add(Component.literal("任务功能：").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(config.taskCount > 0 ? "开" : "关")
                        .withStyle(config.taskCount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal("  "))
                .append(button("[开]", "/relay tasks 2", "恢复每棒 2 个任务"))
                .append(Component.literal("  "))
                .append(button("[关]", "/relay tasks 0", "关闭任务系统")));

        return lines;
    }

    private static Component option(String name, String value, Component... buttons) {
        MutableComponent line = Component.literal(name + " ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  "));
        for (int i = 0; i < buttons.length; i++) {
            if (i > 0) {
                line.append(Component.literal(" "));
            }
            line.append(buttons[i]);
        }
        return line;
    }

    private static Component button(String label, String command, String tooltip) {
        return Component.literal(label).withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(tooltip))));
    }

    private static String weightsText(List<Double> weights) {
        if (weights == null || weights.size() < 5) {
            return "1/2/4/7/10";
        }
        StringBuilder sb = new StringBuilder();
        String[] tiers = {"S", "A", "B", "C", "D"};
        for (int i = 0; i < 5; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(tiers[i]).append(trim(weights.get(i)));
        }
        return sb.toString();
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.format("%.1f", value);
    }
}
