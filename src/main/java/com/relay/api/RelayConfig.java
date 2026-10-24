package com.relay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 可配置参数。功能模块只能读，改配置由核心命令负责。
 */
public final class RelayConfig {

    /** 每棒时长（秒）。 */
    public int turnSeconds = 600;

    /** 当前玩家死亡时扣除的秒数。 */
    public int deathPenaltySeconds = 60;

    /** 开局最少人数。 */
    public int minPlayers = 1;

    /** 每棒刷几个任务（任务功能读取）。 */
    public int taskCount = 2;

    /** 完成/超时后是否立刻补一个新任务（false = 保留灰卡）。 */
    public boolean taskRefresh = false;

    /** 刷新模式下的补任务次数：总任务数 = taskCount + taskRefills；-1 = 无限。 */
    public int taskRefills = -1;

    /** 任务是否有时间限制（false = 永不超时）。 */
    public boolean taskTimeLimit = true;

    /** 旁观者是否锁定视角跟着当前玩家（false = 自由旁观，换人时统一传送到交接点）。 */
    public boolean lockView = true;

    /** 是否启用任务功能的默认开关。 */
    public boolean tasksEnabled = true;

    /** 开局时是否把玩家出生点重置到当前交接点。 */
    public boolean setRespawnOnHandoff = true;

    /** 单局轮数：0 = 无限轮（循环到手动结束/通关）。 */
    public int rounds = 0;

    /** 击杀末影龙是否立即判定通关。 */
    public boolean victoryOnDragon = true;

    /** 剩余多少秒时播报警告，0 表示关闭。 */
    public int warnSeconds = 60;

    /** HUD 缩放倍率，1.0 为原始大小。 */
    public double hudScale = 0.75;

    /** 难度权重，依次对应 S/A/B/C/D；数值越低出现概率越小。 */
    public java.util.List<Double> tierWeights = new java.util.ArrayList<>(java.util.List.of(1.0, 2.0, 4.0, 7.0, 10.0));

    /** 固定接力顺序（玩家名），为空则按加入顺序。 */
    public List<String> order = new ArrayList<>();

    /** 被禁用的功能 id。 */
    public Set<String> disabledFeatures = new LinkedHashSet<>();

    public boolean featureEnabled(String id, boolean defaultValue) {
        if (disabledFeatures != null && disabledFeatures.contains(id)) {
            return false;
        }
        return defaultValue;
    }

    public RelayConfig copy() {
        RelayConfig c = new RelayConfig();
        c.turnSeconds = turnSeconds;
        c.deathPenaltySeconds = deathPenaltySeconds;
        c.minPlayers = minPlayers;
        c.taskCount = taskCount;
        c.taskRefresh = taskRefresh;
        c.taskRefills = taskRefills;
        c.taskTimeLimit = taskTimeLimit;
        c.lockView = lockView;
        c.tasksEnabled = tasksEnabled;
        c.setRespawnOnHandoff = setRespawnOnHandoff;
        c.rounds = rounds;
        c.victoryOnDragon = victoryOnDragon;
        c.warnSeconds = warnSeconds;
        c.hudScale = hudScale;
        c.tierWeights = tierWeights == null ? null : new java.util.ArrayList<>(tierWeights);
        c.order = new ArrayList<>(order == null ? List.of() : order);
        c.disabledFeatures = new LinkedHashSet<>(disabledFeatures == null ? Set.of() : disabledFeatures);
        return c;
    }

    public void clamp() {
        turnSeconds = Math.max(30, Math.min(turnSeconds, 24 * 3600));
        deathPenaltySeconds = Math.max(0, Math.min(deathPenaltySeconds, turnSeconds));
        minPlayers = Math.max(1, Math.min(minPlayers, 16));
        taskCount = Math.max(0, Math.min(taskCount, 4));
        rounds = Math.max(0, Math.min(rounds, 99));
        taskRefills = taskRefills < 0 ? -1 : Math.min(taskRefills, 99);
        warnSeconds = Math.max(0, Math.min(warnSeconds, turnSeconds));
        hudScale = Math.max(0.5, Math.min(hudScale, 1.5));
        if (tierWeights == null || tierWeights.size() != 5) {
            tierWeights = new java.util.ArrayList<>(java.util.List.of(1.0, 2.0, 4.0, 7.0, 10.0));
        } else {
            for (int i = 0; i < tierWeights.size(); i++) {
                Double v = tierWeights.get(i);
                tierWeights.set(i, v == null || v <= 0 ? 0.01 : Math.min(v, 1000.0));
            }
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static RelayConfig load(Path path) {
        RelayConfig cfg = new RelayConfig();
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                RelayConfig read = GSON.fromJson(json, RelayConfig.class);
                if (read != null) {
                    cfg = read;
                }
            } catch (Exception ignored) {
                // 配置坏了就用默认值，不让服务器起不来
            }
        }
        if (cfg.order == null) {
            cfg.order = new ArrayList<>();
        }
        if (cfg.disabledFeatures == null) {
            cfg.disabledFeatures = new LinkedHashSet<>();
        }
        cfg.clamp();
        return cfg;
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
