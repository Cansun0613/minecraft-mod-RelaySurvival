package com.relay.core;

import com.relay.RelayMod;
import com.relay.api.RelayFeature;
import com.relay.api.RelayContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 通过 ServiceLoader 发现功能，核心不认识任何具体功能类。
 * 删除功能 = 删掉它的类与 META-INF/services 条目，核心照常工作。
 */
public final class FeatureRegistry {

    private final List<RelayFeature> features = new ArrayList<>();

    public void load(RelayContext ctx) {
        List<RelayFeature> discovered = new ArrayList<>();
        ServiceLoader<RelayFeature> loader = ServiceLoader.load(RelayFeature.class, RelayMod.class.getClassLoader());
        var iterator = loader.iterator();
        while (true) {
            RelayFeature feature;
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                feature = iterator.next();
            } catch (Throwable t) {
                RelayMod.LOGGER.error("跳过无法加载的功能实现", t);
                continue;
            }
            discovered.add(feature);
        }
        discovered.sort(Comparator.comparingInt(RelayFeature::order));

        for (RelayFeature feature : discovered) {
            String id = feature.id();
            if (!ctx.config().featureEnabled(id, feature.enabledByDefault())) {
                RelayMod.LOGGER.info("功能 {} 已按配置禁用", id);
                continue;
            }
            try {
                feature.attach(ctx);
                features.add(feature);
                RelayMod.LOGGER.info("功能 {} 已加载", id);
            } catch (Throwable t) {
                RelayMod.LOGGER.error("功能 {} 初始化失败，已跳过（其余功能不受影响）", id, t);
            }
        }
    }

    public void startSession(com.relay.api.SessionInfo info) {
        for (RelayFeature feature : features) {
            safely(feature.id(), () -> feature.onSessionStart(info));
        }
    }

    public void endSession(com.relay.api.SessionInfo info, boolean victory) {
        for (RelayFeature feature : features) {
            safely(feature.id(), () -> feature.onSessionEnd(info, victory));
        }
    }

    public void turnStart(com.relay.api.TurnInfo turn) {
        for (RelayFeature feature : features) {
            safely(feature.id(), () -> feature.onTurnStart(turn));
        }
    }

    public void second(com.relay.api.TurnInfo turn) {
        for (RelayFeature feature : features) {
            safely(feature.id(), () -> feature.onSecond(turn));
        }
    }

    public void turnEnd(com.relay.api.TurnInfo turn, com.relay.api.TurnEndReason reason) {
        for (RelayFeature feature : features) {
            safely(feature.id(), () -> feature.onTurnEnd(turn, reason));
        }
    }

    public void death(java.util.UUID player) {
        for (RelayFeature feature : features) {
            safely(feature.id(), () -> feature.onPlayerDeath(player));
        }
    }

    public void join(java.util.UUID player) {
        for (RelayFeature feature : features) {
            safely(feature.id(), () -> feature.onPlayerJoin(player));
        }
    }

    public void leave(java.util.UUID player) {
        for (RelayFeature feature : features) {
            safely(feature.id(), () -> feature.onPlayerLeave(player));
        }
    }

    public void shutdown() {
        for (RelayFeature feature : features) {
            safely(feature.id(), feature::detach);
        }
        features.clear();
    }

    public List<String> ids() {
        return features.stream().map(RelayFeature::id).toList();
    }

    private void safely(String id, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            RelayMod.LOGGER.error("功能 {} 回调异常（已忽略）", id, t);
        }
    }
}
