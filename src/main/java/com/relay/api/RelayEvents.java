package com.relay.api;

import java.util.function.Consumer;

/**
 * 极简类型化事件总线，用于功能之间解耦通信（核心不感知具体事件类型）。
 */
public interface RelayEvents {

    <T> void subscribe(Class<T> type, Consumer<T> listener);

    <T> void publish(T event);

    /** 加/扣时间。功能（例如任务）只发事件，由核心决定怎么改表。 */
    record TimeAdjusted(java.util.UUID player, int seconds, String source) {
    }
}
