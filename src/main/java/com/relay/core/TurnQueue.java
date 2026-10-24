package com.relay.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 纯逻辑接力队列：按顺序轮换，可以设定总轮数（0 = 无限轮，直到手动结束/通关）。
 * 两人两轮就是 1 → 2 → 1 → 2。
 */
public final class TurnQueue {

    private final List<UUID> order = new ArrayList<>();
    private int index = -1;
    private int rounds = 0;
    private int round = 0;
    private int served = 0;

    public void start(List<UUID> players, int rounds) {
        order.clear();
        order.addAll(players);
        this.rounds = Math.max(0, rounds);
        this.round = 0;
        this.served = 0;
        this.index = order.isEmpty() ? -1 : 0;
    }

    public boolean active() {
        return index >= 0 && index < order.size();
    }

    /** 当前是第几棒（从 0 开始，跨轮累计）。 */
    public int turnNumber() {
        return served;
    }

    /** 总棒数；0 表示无限轮。 */
    public int totalTurns() {
        return rounds <= 0 ? 0 : order.size() * rounds;
    }

    public int round() {
        return round;
    }

    public int rounds() {
        return rounds;
    }

    public int total() {
        return order.size();
    }

    public List<UUID> order() {
        return List.copyOf(order);
    }

    public UUID current() {
        return active() ? order.get(index) : null;
    }

    public UUID peekNext() {
        int next = index + 1;
        if (next < order.size()) {
            return order.get(next);
        }
        if (rounds <= 0 || round + 1 < rounds) {
            return order.isEmpty() ? null : order.get(0);
        }
        return null;
    }

    /** 后面还有没有下一棒（不改变队列状态）。 */
    public boolean hasNext() {
        if (order.isEmpty()) {
            return false;
        }
        if (index + 1 < order.size()) {
            return true;
        }
        return rounds <= 0 || round + 1 < rounds;
    }

    /** 前进到下一棒，返回是否还有下一棒。 */
    public boolean advance() {
        if (order.isEmpty()) {
            return false;
        }
        if (index + 1 < order.size()) {
            index++;
            served++;
            return true;
        }
        if (rounds > 0 && round + 1 >= rounds) {
            return false;
        }
        round++;
        index = 0;
        served++;
        return true;
    }

    /** 中途加入：追加到队尾。 */
    public boolean add(UUID player) {
        if (order.contains(player)) {
            return false;
        }
        order.add(player);
        if (index < 0) {
            index = 0;
        }
        return true;
    }

    public boolean remove(UUID player) {
        int at = order.indexOf(player);
        if (at < 0) {
            return false;
        }
        order.remove(at);
        if (at <= index) {
            index--;
        }
        if (index < -1) {
            index = -1;
        }
        return true;
    }

    public void clear() {
        order.clear();
        index = -1;
        round = 0;
        served = 0;
    }

    public Optional<UUID> nextOnline(Predicate<UUID> online) {
        int start = index + 1;
        for (int i = start; i < order.size(); i++) {
            if (online.test(order.get(i))) {
                return Optional.of(order.get(i));
            }
        }
        if (rounds <= 0 || round + 1 < rounds) {
            for (UUID id : order) {
                if (online.test(id)) {
                    return Optional.of(id);
                }
            }
        }
        return Optional.empty();
    }
}
