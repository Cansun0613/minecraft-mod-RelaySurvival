package com.relay.feature.tasks;

public enum TaskType {
    /** 距离本棒起点至少 N 格（水平距离）。 */
    DISTANCE,
    /** 破坏 N 个指定方块。 */
    BLOCK_BREAK,
    /** 击杀 N 只指定生物。 */
    MOB_KILL,
    /** 背包里累计拥有 N 个指定物品。 */
    ITEM_COUNT,
    /** 收集齐 param 里列出的所有物品（逗号分隔），进度 = 已收集种类数。 */
    ITEM_SET,
    /** 拥有 param 里任意一种物品即可。 */
    ITEM_ANY,
    /** 进入指定维度。 */
    DIMENSION,
    /** 完成指定进度。 */
    ADVANCEMENT,
    /** 原版统计计数器达到 N，例如 fish_caught / boat_one_cm。 */
    STAT
}
