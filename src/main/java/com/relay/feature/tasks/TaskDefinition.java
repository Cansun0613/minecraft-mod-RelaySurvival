package com.relay.feature.tasks;

public record TaskDefinition(
        String id,
        String category,
        String title,
        String description,
        TaskType type,
        String param,
        int target,
        int timeLimit,
        int successSeconds,
        int failSeconds,
        String tier
) {
    public TaskDefinition {
        if (timeLimit <= 0) {
            timeLimit = 240;
        }
        if (target <= 0) {
            target = 1;
        }
        if (tier == null || tier.isBlank()) {
            tier = "C";
        }
        tier = tier.trim().toUpperCase();
        if (!tier.matches("[SABCD]")) {
            tier = "C";
        }
    }

    /** 难度权重下标：S=0 A=1 B=2 C=3 D=4。 */
    public int tierIndex() {
        return switch (tier) {
            case "S" -> 0;
            case "A" -> 1;
            case "B" -> 2;
            case "D" -> 4;
            default -> 3;
        };
    }
}
