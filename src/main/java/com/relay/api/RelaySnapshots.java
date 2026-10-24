package com.relay.api;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 交接时的玩家状态快照：背包（含盔甲/副手）、经验、血量、饥饿、状态效果、坐标朝向。
 */
public interface RelaySnapshots {

    void capture(ServerPlayer player);

    /** 把快照套用到玩家身上；没有快照时返回 false。 */
    boolean applyTo(ServerPlayer player);

    boolean has(UUID player);

    void clear();
}
