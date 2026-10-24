package com.relay.api;

/**
 * 可插拔功能。核心通过 {@link java.util.ServiceLoader} 发现实现，
 * 删除某个功能的类与 META-INF/services 条目后，其余功能照常运行。
 */
public interface RelayFeature {

    /** 唯一标识，也是配置文件里开关用的名字。 */
    String id();

    /** 越小越先加载。 */
    default int order() {
        return 100;
    }

    default boolean enabledByDefault() {
        return true;
    }

    /** 核心启动时调用一次。抛异常会被核心捕获并跳过该功能，不影响其他功能。 */
    void attach(RelayContext ctx);

    default void detach() {
    }

    default void onSessionStart(SessionInfo info) {
    }

    default void onSessionEnd(SessionInfo info, boolean victory) {
    }

    default void onTurnStart(TurnInfo turn) {
    }

    /** 每秒一次，turn.remaining() 是当前剩余秒数。 */
    default void onSecond(TurnInfo turn) {
    }

    default void onTurnEnd(TurnInfo turn, TurnEndReason reason) {
    }

    default void onPlayerDeath(java.util.UUID player) {
    }

    default void onPlayerJoin(java.util.UUID player) {
    }

    default void onPlayerLeave(java.util.UUID player) {
    }
}
