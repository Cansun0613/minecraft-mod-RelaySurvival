package com.relay.feature.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

public final class TaskInstance {

    public enum State {
        ACTIVE,
        DONE,
        FAILED
    }

    private final TaskDefinition definition;
    private int progress;
    private int secondsLeft;
    private State state = State.ACTIVE;
    private boolean timed = true;
    private long finishedAt = -1;

    public TaskInstance(TaskDefinition definition) {
        this.definition = definition;
        this.secondsLeft = definition.timeLimit();
    }

    public TaskDefinition definition() {
        return definition;
    }

    public int progress() {
        return progress;
    }

    public int secondsLeft() {
        return secondsLeft;
    }

    public State state() {
        return state;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public boolean isTimed() {
        return timed;
    }

    public void setTimed(boolean timed) {
        this.timed = timed;
    }

    public void markDone(long sequence) {
        state = State.DONE;
        finishedAt = sequence;
    }

    public void markFailed(long sequence) {
        state = State.FAILED;
        finishedAt = sequence;
    }

    /** 完成/超时的先后顺序；未结束为 -1。 */
    public long finishedAt() {
        return finishedAt;
    }

    public void setProgress(int value) {
        progress = Math.min(value, definition.target());
    }

    public void addProgress(int delta) {
        setProgress(progress + delta);
    }

    public void tickSecond() {
        if (timed) {
            secondsLeft--;
        }
    }

    public boolean done() {
        return progress >= definition.target();
    }

    public boolean expired() {
        return timed && secondsLeft <= 0;
    }

    /**
     * 按难度权重抽取：权重越低越稀有（S 最稀有，D 最常见）。
     */
    public static List<TaskInstance> pick(List<TaskDefinition> pool, int count, Random random,
                                          List<TaskInstance> exclude,
                                          ToDoubleFunction<TaskDefinition> weight) {
        List<TaskDefinition> candidates = new ArrayList<>(pool);
        for (TaskInstance instance : exclude) {
            candidates.removeIf(d -> d.id().equals(instance.definition().id()));
        }
        List<TaskInstance> picked = new ArrayList<>();
        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            double total = 0;
            for (TaskDefinition def : candidates) {
                total += Math.max(0.01, weight.applyAsDouble(def));
            }
            double roll = random.nextDouble() * total;
            int index = 0;
            double acc = 0;
            for (; index < candidates.size(); index++) {
                acc += Math.max(0.01, weight.applyAsDouble(candidates.get(index)));
                if (roll <= acc) {
                    break;
                }
            }
            if (index >= candidates.size()) {
                index = candidates.size() - 1;
            }
            picked.add(new TaskInstance(candidates.remove(index)));
        }
        return picked;
    }
}
