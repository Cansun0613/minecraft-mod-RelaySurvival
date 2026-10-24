package com.relay.core;

/**
 * 纯逻辑倒计时，方便单元测试。
 */
public final class Countdown {

    private int remaining;
    private int total;
    private boolean paused;

    public void reset(int seconds) {
        this.total = Math.max(1, seconds);
        this.remaining = this.total;
        this.paused = false;
    }

    public int tick() {
        if (!paused && remaining > 0) {
            remaining--;
        }
        return remaining;
    }

    public void add(int seconds) {
        remaining = Math.max(0, Math.min(remaining + seconds, 24 * 3600));
    }

    public void set(int seconds) {
        remaining = Math.max(0, Math.min(seconds, 24 * 3600));
    }

    public int remaining() {
        return remaining;
    }

    public int total() {
        return total;
    }

    public boolean paused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean expired() {
        return remaining <= 0;
    }

    public static String format(int seconds) {
        int s = Math.max(0, seconds);
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
