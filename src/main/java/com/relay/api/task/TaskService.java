package com.relay.api.task;

/**
 * 任务功能对外暴露的只读视图。HUD 等功能通过
 * {@code ctx.service(TaskService.class)} 拿到它，不依赖任务功能的实现类。
 */
public interface TaskService {

    java.util.List<TaskView> active();

    boolean enabled();

    record TaskView(
            String id,
            String category,
            String tier,
            String title,
            String description,
            int progress,
            int target,
            int secondsLeft,
            int timeLimit,
            int successSeconds,
            int failSeconds,
            String state,
            boolean timed
    ) {
        public float ratio() {
            if (target <= 0) {
                return 0f;
            }
            return Math.min(1f, progress / (float) target);
        }
    }
}
