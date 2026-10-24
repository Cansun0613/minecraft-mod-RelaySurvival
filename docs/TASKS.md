# 任务格式

任务池在**服务端**的 `config/relay-tasks.json`，首次启动自动生成（内置 156 条）。
修改后需要重启服务端（单人：退出存档再进）才生效；客户端不需要同步这个文件。

## 文件结构

```json
{
  "tasks": [
    { ...任务1... },
    { ...任务2... }
  ]
}
```

## 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 唯一编号，建议按分类前缀（`EXP-` 探索、`RES-` 资源、`FGT-` 战斗、`ANY-` 任意、`SET-` 套装、`ADV-` 进度、`STA-` 统计） |
| `category` | string | 任务卡左上角显示的分类名 |
| `title` | string | 任务标题 |
| `description` | string | 完整条件，悬停详情/容器面板里显示 |
| `type` | enum | 见下表 |
| `param` | string | 条件参数，含义随 `type` 变化 |
| `target` | int | 目标数量（进度条分母） |
| `timeLimit` | int | 限时秒数（`taskTimeLimit` 关闭时不生效） |
| `successSeconds` | int | 完成奖励秒数 |
| `failSeconds` | int | 超时扣除秒数 |
| `tier` | `S/A/B/C/D` | 难度，决定出现权重与推荐奖励 |

## 类型

| type | 判定方式 | param 示例 |
| --- | --- | --- |
| `DISTANCE` | 与"本棒起点"的水平距离 ≥ target | 留空 |
| `BLOCK_BREAK` | 破坏方块计数 | `minecraft:oak_log`、`minecraft:sand,minecraft:red_sand`、`#minecraft:logs` |
| `MOB_KILL` | 击杀实体计数 | `minecraft:zombie` |
| `ITEM_COUNT` | 背包内该物品总数 ≥ target | `minecraft:iron_ingot` |
| `ITEM_SET` | 拥有列表里**所有**物品，进度 = 已集齐种类 | `minecraft:wooden_axe,...,minecraft:wooden_sword` |
| `ITEM_ANY` | 拥有列表里**任意一件**即完成 | `minecraft:cod,minecraft:salmon` |
| `STAT` | 原版统计值 ≥ target | `minecraft:fish_caught` |
| `DIMENSION` | 玩家处于该维度 | `minecraft:the_nether` |
| `ADVANCEMENT` | 该进度已完成 | `minecraft:story/mine_stone` |

### 关于 param 的几种写法

- **单个 id**：`minecraft:stone`
- **多个 id**：`minecraft:sand,minecraft:red_sand`（逗号分隔，命中任意一个）
- **方块标签**：`#minecraft:logs`（含所有原木与菌柄）、`#minecraft:coal_ores`、`#minecraft:iron_ores`、`#minecraft:copper_ores`
- **统计单位**：距离类统计是**厘米**，例如坐船 300 格写 `30000`
- **进度 id**：对应 `data/minecraft/advancement/` 下的路径，如 `story/mine_stone`

## 难度、权重与奖励

出现概率由 `relay.json` 的 `tierWeights` 控制，默认：

| 难度 | 权重 | 默认奖励 | 默认超时 |
| --- | --- | --- | --- |
| S | 1 | +300 秒 | −60 秒 |
| A | 2 | +180 秒 | −60 秒 |
| B | 4 | +120 秒 | −60 秒 |
| C | 7 | +90 秒 | −60 秒 |
| D | 10 | +60 秒 | −60 秒 |

权重越小越稀有；`S` 档目前没有任务，保留给后续超难任务。

## 添加任务

### 例 1：方块标签任务

```json
{
  "id": "RES-100",
  "category": "资源",
  "title": "伐木工",
  "description": "破坏 16 个原木（所有种类）",
  "type": "BLOCK_BREAK",
  "param": "#minecraft:logs",
  "target": 16,
  "timeLimit": 240,
  "successSeconds": 60,
  "failSeconds": 60,
  "tier": "D"
}
```

### 例 2：套装任务

```json
{
  "id": "SET-100",
  "category": "套装",
  "title": "一整套铁质工具",
  "description": "同时拥有铁斧、铁锄、铁镐、铁锹、铁剑",
  "type": "ITEM_SET",
  "param": "minecraft:iron_axe,minecraft:iron_hoe,minecraft:iron_pickaxe,minecraft:iron_shovel,minecraft:iron_sword",
  "target": 5,
  "timeLimit": 420,
  "successSeconds": 180,
  "failSeconds": 60,
  "tier": "A"
}
```

### 例 3：统计任务

```json
{
  "id": "STA-100",
  "category": "统计",
  "title": "钓鱼佬",
  "description": "钓到 4 条鱼（统计）",
  "type": "STAT",
  "param": "minecraft:fish_caught",
  "target": 4,
  "timeLimit": 300,
  "successSeconds": 90,
  "failSeconds": 60,
  "tier": "C"
}
```

## 常见问题

- **任务一直完不成**：检查 id 是否存在于当前版本（方块/物品/实体/进度/统计），拼写错误会导致永远无法命中；
- **`ITEM_COUNT` 与 `ITEM_ANY` 的区别**：前者看数量（≥ target），后者只看有没有；
- **`ITEM_SET` 的 target 应等于列表长度**，进度按"已拥有几种"计算；
- **重名不报错**，但同时抽到两个同名任务时 HUD 上不好区分，建议标题唯一；
- **改完没生效**：任务池只在服务端启动时读取，重启服务端或退出存档重进；
- **多人服**：只需服务器上的 `config/relay-tasks.json`，客户端不用改。
