package com.relay.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RelayCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        com.relay.RelayMod.LOGGER.info("/relay 指令已注册");
        // 不做权限门槛：单人存档默认不开作弊时玩家不是 OP，加了门槛反而看不到指令
        dispatcher.register(Commands.literal("relay")
                .executes(ctx -> {
                    RelayMenu.send(ctx.getSource(), runtime());
                    return 1;
                })
                .then(Commands.literal("menu")
                        .executes(ctx -> {
                            RelayMenu.send(ctx.getSource(), runtime());
                            return 1;
                        }))
                .then(Commands.literal("start")
                        .executes(ctx -> start(ctx, List.of()))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> start(ctx, EntityArgument.getPlayers(ctx, "players")))))
                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            runtime().stop(false);
                            return 1;
                        }))
                .then(Commands.literal("skip")
                        .executes(ctx -> {
                            runtime().skip();
                            return 1;
                        }))
                .then(Commands.literal("status").executes(RelayCommand::status))
                .then(Commands.literal("time")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(30, 7200))
                                .executes(ctx -> {
                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                    runtime().config().turnSeconds = seconds;
                                    runtime().saveConfig();
                                    ctx.getSource().sendSuccess(() -> Component.literal("每棒时长已设为 " + seconds + " 秒（下一棒生效）"), true);
                                    return 1;
                                })))
                .then(Commands.literal("penalty")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600))
                                .executes(ctx -> {
                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                    runtime().config().deathPenaltySeconds = seconds;
                                    runtime().saveConfig();
                                    ctx.getSource().sendSuccess(() -> Component.literal("死亡扣时已设为 " + seconds + " 秒"), true);
                                    return 1;
                                })))
                .then(Commands.literal("tasks")
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, 4))
                                .executes(ctx -> {
                                    int count = IntegerArgumentType.getInteger(ctx, "count");
                                    runtime().config().taskCount = count;
                                    runtime().saveConfig();
                                    ctx.getSource().sendSuccess(() -> Component.literal("每棒任务数已设为 " + count), true);
                                    return 1;
                                })))
                .then(Commands.literal("hudscale")
                        .then(Commands.argument("scale", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.5f, 1.5f))
                                .executes(ctx -> {
                                    float scale = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "scale");
                                    runtime().config().hudScale = scale;
                                    runtime().saveConfig();
                                    ctx.getSource().sendSuccess(() -> Component.literal("HUD 缩放已设为 " + scale + "（下一秒刷新）"), true);
                                    return 1;
                                })))
                .then(Commands.literal("rounds")
                        .then(Commands.argument("rounds", IntegerArgumentType.integer(0, 99))
                                .executes(ctx -> {
                                    int rounds = IntegerArgumentType.getInteger(ctx, "rounds");
                                    runtime().config().rounds = rounds;
                                    runtime().saveConfig();
                                    String text = rounds <= 0 ? "无限轮" : rounds + " 轮";
                                    ctx.getSource().sendSuccess(() -> Component.literal("单局轮数已设为 " + text + "（下一局生效）"), true);
                                    return 1;
                                })))
                .then(Commands.literal("dragonwin")
                        .then(Commands.literal("on").executes(ctx -> setDragonWin(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setDragonWin(ctx, false))))
                .then(Commands.literal("respawn")
                        .then(Commands.literal("on").executes(ctx -> setRespawn(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setRespawn(ctx, false))))
                .then(Commands.literal("warn")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600))
                                .executes(ctx -> {
                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                    runtime().config().warnSeconds = seconds;
                                    runtime().saveConfig();
                                    String text = seconds <= 0 ? "关闭" : (seconds + " 秒");
                                    ctx.getSource().sendSuccess(() -> Component.literal("剩余提醒已设为 " + text), true);
                                    return 1;
                                })))                .then(Commands.literal("addplayer")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    int result = runtime().addPlayer(target);
                                    String name = target.getGameProfile().name();
                                    String message = switch (result) {
                                        case 0 -> "还没开局，先用 /relay start";
                                        case 1 -> name + " 已经在接力队列里了";
                                        default -> name + " 已加入接力队列";
                                    };
                                    ctx.getSource().sendSuccess(() -> Component.literal(message), result != 0);
                                    return result == 2 ? 1 : 0;
                                })))                .then(Commands.literal("refills")
                        .then(Commands.argument("count", IntegerArgumentType.integer(-1, 99))
                                .executes(ctx -> {
                                    int count = IntegerArgumentType.getInteger(ctx, "count");
                                    runtime().config().taskRefills = count;
                                    runtime().saveConfig();
                                    String text = count < 0 ? "无限" : (count + " 次");
                                    ctx.getSource().sendSuccess(() -> Component.literal("刷新次数已设为 " + text + "（仅在任务刷新模式生效）"), true);
                                    return 1;
                                })))                .then(Commands.literal("lockview")
                        .then(Commands.literal("on").executes(ctx -> setLockView(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setLockView(ctx, false))))                .then(Commands.literal("refresh")
                        .then(Commands.literal("on").executes(ctx -> setRefresh(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setRefresh(ctx, false))))
                .then(Commands.literal("timelimit")
                        .then(Commands.literal("on").executes(ctx -> setTimeLimit(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setTimeLimit(ctx, false))))                .then(Commands.literal("weights")
                        .then(Commands.literal("default")
                                .executes(ctx -> setWeights(ctx, java.util.List.of(1.0, 2.0, 4.0, 7.0, 10.0), "默认")))
                        .then(Commands.literal("easy")
                                .executes(ctx -> setWeights(ctx, java.util.List.of(1.0, 2.0, 3.0, 4.0, 12.0), "简单多")))
                        .then(Commands.literal("hard")
                                .executes(ctx -> setWeights(ctx, java.util.List.of(2.0, 4.0, 8.0, 10.0, 10.0), "困难多"))))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("配置文件在 config/relay.json，重载请重启服务器"), false);
                            return 1;
                        }))
                .then(Commands.literal("features")
                        .executes(ctx -> {
                            String ids = String.join(", ", runtime().features().ids());
                            ctx.getSource().sendSuccess(() -> Component.literal("已加载功能: " + ids), false);
                            return 1;
                        }))
                .then(Commands.literal("feature")
                        .then(Commands.literal("off")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            runtime().config().disabledFeatures.add(id);
                                            runtime().saveConfig();
                                            ctx.getSource().sendSuccess(() -> Component.literal("已禁用功能 " + id + "（重启生效）"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("on")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            runtime().config().disabledFeatures.remove(id);
                                            runtime().saveConfig();
                                            ctx.getSource().sendSuccess(() -> Component.literal("已启用功能 " + id + "（重启生效）"), true);
                                            return 1;
                                        })))));
    }

    private static RelayRuntime runtime() {
        return RelayCore.get();
    }

    private static int setDragonWin(CommandContext<CommandSourceStack> ctx, boolean value) {
        runtime().config().victoryOnDragon = value;
        runtime().saveConfig();
        ctx.getSource().sendSuccess(() -> Component.literal("击杀末影龙" + (value ? "立即通关" : "不再直接结束")), true);
        return 1;
    }

    private static int setRespawn(CommandContext<CommandSourceStack> ctx, boolean value) {
        runtime().config().setRespawnOnHandoff = value;
        runtime().saveConfig();
        ctx.getSource().sendSuccess(() -> Component.literal("交接重生点已" + (value ? "开启" : "关闭")), true);
        return 1;
    }

    private static int setLockView(CommandContext<CommandSourceStack> ctx, boolean value) {
        runtime().config().lockView = value;
        runtime().saveConfig();
        ctx.getSource().sendSuccess(() -> Component.literal("旁观视角已" + (value ? "锁定：跟随当前玩家" : "解锁：自由旁观（换人时会统一传送）")), true);
        return 1;
    }

    private static int setRefresh(CommandContext<CommandSourceStack> ctx, boolean value) {
        runtime().config().taskRefresh = value;
        runtime().saveConfig();
        ctx.getSource().sendSuccess(() -> Component.literal("任务刷新已" + (value ? "开启：完成/超时后补新任务" : "关闭：完成的任务保留")), true);
        return 1;
    }

    private static int setTimeLimit(CommandContext<CommandSourceStack> ctx, boolean value) {
        runtime().config().taskTimeLimit = value;
        runtime().saveConfig();
        ctx.getSource().sendSuccess(() -> Component.literal("任务限时已" + (value ? "开启" : "关闭：任务不会超时")), true);
        return 1;
    }

    private static int setWeights(CommandContext<CommandSourceStack> ctx, java.util.List<Double> weights, String label) {
        runtime().config().tierWeights = new java.util.ArrayList<>(weights);
        runtime().saveConfig();
        ctx.getSource().sendSuccess(() -> Component.literal("难度权重已切换为「" + label + "」"), true);
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> explicit) {
        RelayRuntime runtime = runtime();
        List<ServerPlayer> players = new ArrayList<>(explicit);
        if (players.isEmpty()) {
            players.addAll(ctx.getSource().getServer().getPlayerList().getPlayers());
        }
        if (!runtime.config().order.isEmpty()) {
            List<ServerPlayer> ordered = new ArrayList<>();
            for (String name : runtime.config().order) {
                players.stream()
                        .filter(p -> p.getGameProfile().name().equalsIgnoreCase(name))
                        .findFirst()
                        .ifPresent(ordered::add);
            }
            for (ServerPlayer p : players) {
                if (!ordered.contains(p)) {
                    ordered.add(p);
                }
            }
            players = ordered;
        }
        List<java.util.UUID> order = players.stream().map(ServerPlayer::getUUID).toList();
        return runtime.startSession(order) ? 1 : 0;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        RelayRuntime runtime = runtime();
        CommandSourceStack source = ctx.getSource();
        if (!runtime.running()) {
            source.sendSuccess(() -> Component.literal("当前没有进行中的接力").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        var info = runtime.info();
        String turn = info.total() > 0
                ? "第 " + (info.index() + 1) + "/" + info.total() + " 棒"
                : "第 " + (info.index() + 1) + " 棒";
        source.sendSuccess(() -> Component.literal(
                turn + "：" + info.playerName()
                        + "，剩余 " + Countdown.format(info.remaining())
                        + "，任务 " + runtime.config().taskCount + " 个/棒").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }
}
