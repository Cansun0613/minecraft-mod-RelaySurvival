package com.relay.api;

public interface RelayTimer {

    int remaining();

    int turnSeconds();

    void setRemaining(int seconds);

    /** 加/减时间，结果不会低于 0。 */
    void add(int seconds);

    /** 带原因的加/减时间，核心会广播 {@link RelayEvents.TimeAdjusted}。 */
    default void add(int seconds, String source) {
        add(seconds);
    }

    void reset();

    boolean paused();

    void setPaused(boolean paused);
}
