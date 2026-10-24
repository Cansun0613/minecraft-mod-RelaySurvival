# 接力生存 · Relay Survival

![Minecraft](https://img.shields.io/badge/Minecraft-26.2-3C8527?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-0.20.1%2B-DBD0B4?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

Minecraft **26.2** / Fabric 的「接力生存」模组：每棒限时，时间归零强制交接；背包、经验、血量、状态效果、坐标全部原样传给下一棒；每棒抽随机任务，完成加时间、超时扣时间。

> 玩法灵感来自 B 站夏天y《天门接力挑战》/ 大炒面制造者Cen《区区接力生存》里使用的私人模组，以及 YouTube 作者 Andronicus 的 *Speedrunning Minecraft, but Every Minute the Player Changes*。
> 本项目是**独立实现**，不含原模组的任何代码或素材，与原作者无关。
> 任务设计部分参考了 [Yet Another Minecraft Bingo](https://github.com/YetAnotherMinecraftBingo) 的思路（物品收集、成套、任意一件）。

---

## 目录

- [玩法](#玩法)
- [特性](#特性)
- [环境要求](#环境要求)
- [安装](#安装)
- [快速开始](#快速开始)
- [指令](#指令)
- [配置](#配置)
- [任务系统](#任务系统)
- [交接与旁观](#交接与旁观)
- [从源码构建](#从源码构建)
- [项目结构](#项目结构)
- [已知限制](#已知限制)
- [致谢](#致谢)
- [License](#license)

---

## 玩法

一局里所有人在同一个世界、同一条命上接力：

1. 房主 `/relay start`，所有人先变旁观者；
2. 第一棒转生存，开始 10:00 倒计时；
3. 时间归零（或 `/relay skip`）→ 强制交接：当前玩家变旁观，下一棒原地接管上一棒的背包、经验、血量、饥饿、药水效果和坐标；
4. 每棒开局抽随机任务，完成 +时间、超时 −时间；
5. 击杀末影龙通关，或 `/relay stop` 结束本局。

这是**合作模式**：一队人、一条公共计时，没有个人分数。

## 特性

- **强制交接**：时间到自动换人，背包（快捷栏、主背包、盔甲、副手）、经验、血量、饥饿、状态效果、坐标朝向整套继承；交接后原玩家背包清空，避免复制。
- **轮换与轮数**：按顺序循环，`rounds` 可设 1~99 轮，0 = 无限轮（两人两轮就是 1→2→1→2）。
- **随机任务**：内置 156 条，分 S/A/B/C/D 五档难度，按权重抽取；9 种任务类型，支持方块标签。
- **时间奖惩**：完成奖励 D +60 / C +90 / B +120 / A +180 / S +300 秒；超时统一 −60 秒。
- **HUD**：左上角皮肤头像 + 名字 + 倒计时 + 进度条 + 任务卡；任务卡显示难度、剩余时间、进度，完成后变灰打勾并沉底；鼠标悬停看完整条件。
- **容器面板**：打开背包/箱子/熔炉等工作台界面时隐藏 HUD，改为在容器左侧显示所有任务的详细信息。
- **旁观锁定**：旁观者视角锁定当前玩家（等同原版旁观者左键 spectate），按 Shift 脱离后 0.25 秒自动锁回；可关闭，关闭后换人时统一传送到交接点。
- **中途拉人**：`/relay addplayer <玩家>` 把迟到/掉线重连的玩家加回队列。
- **聊天栏设置面板**：`/relay` 打开点击式面板，所有设置点一下即可修改，无需记指令。
- **可插拔架构**：核心不依赖任何功能模块，功能通过 `ServiceLoader` 发现，删掉一个功能核心照常运行（详见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)）。

## 环境要求

| 项目 | 要求 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.20.1 或更新 |
| Fabric API | 0.161.0+26.2 |
| Java | 25（游戏本身要求） |

- **服务端必须装**：所有玩法逻辑在服务端；
- **客户端建议装**：不带模组的客户端能进服，但没有 HUD 和设置面板；
- 客户端与服务端的模组**版本必须一致**，否则该玩家没有 HUD（1.1.2 起只提示、不踢人）。

## 安装

1. 给 26.2 安装 Fabric Loader；
2. 把 [Fabric API](https://modrinth.com/mod/fabric-api) 和本模组放进 `mods/`；
3. 服务端同理：`fabric-server-launch.jar` + 两个 mod。

从源码构建的 jar 在 `build/libs/relay-survival-<version>.jar`。

## 快速开始

```
/relay start                          # 所有在线玩家，按加入顺序
/relay start 夏天y 半城 王少栋 孙悟空   # 指定顺序
```

单人测试：创建存档 → 对局域网开放 → `/relay start`（`minPlayers` 默认 1，单人也能跑一棒）。

## 指令

所有指令都没有权限门槛，普通玩家也能用。

| 指令 | 说明 |
| --- | --- |
| `/relay` 或 `/relay menu` | 聊天栏点击式设置面板 |
| `/relay start [玩家…]` | 开局（不填玩家 = 所有在线玩家） |
| `/relay stop` | 结束本局 |
| `/relay skip` | 提前交接 |
| `/relay status` | 当前第几棒、谁在玩、剩多久 |
| `/relay addplayer <玩家>` | 把玩家加进进行中的接力队列 |
| `/relay time <30-7200>` | 每棒秒数（下一棒生效） |
| `/relay penalty <0-600>` | 死亡扣多少秒 |
| `/relay tasks <0-4>` | 每棒抽几个任务；0 = 关闭任务 |
| `/relay refresh on\|off` | 完成/超时后是否补新任务 |
| `/relay refills <-1-99>` | 刷新模式最多补几个任务，-1 = 无限 |
| `/relay timelimit on\|off` | 任务是否有时间限制 |
| `/relay rounds <0-99>` | 单局轮数，0 = 无限轮 |
| `/relay lockview on\|off` | 旁观者视角是否锁定当前玩家 |
| `/relay dragonwin on\|off` | 击杀末影龙是否立即通关 |
| `/relay respawn on\|off` | 交接时是否把重生点设到交接位置 |
| `/relay warn <0-600>` | 剩余多少秒提醒，0 = 关闭 |
| `/relay hudscale <0.5-1.5>` | HUD 整体缩放，默认 0.75，即时生效 |
| `/relay weights default\|easy\|hard` | 难度权重预设 |
| `/relay features` | 列出已加载的功能 |
| `/relay feature on\|off <id>` | 启用/禁用某个功能（重启生效） |
| `/relay reload` | 提示配置文件位置（修改需重启服务端） |

## 配置

首次启动生成两个文件（都在服务端实例的 `config/` 下）：

- `config/relay.json`：全局参数
- `config/relay-tasks.json`：任务池（156 条）

### relay.json

| 字段 | 默认 | 说明 |
| --- | --- | --- |
| `turnSeconds` | `600` | 每棒时长（秒） |
| `deathPenaltySeconds` | `60` | 当前玩家死亡扣多少秒 |
| `minPlayers` | `1` | 开局最少人数 |
| `taskCount` | `2` | 每棒抽几个任务 |
| `taskRefresh` | `false` | 完成/超时后是否补新任务 |
| `taskRefills` | `-1` | 刷新模式最多补几个，-1 = 无限；总任务数 = `taskCount + taskRefills` |
| `taskTimeLimit` | `true` | 任务是否限时（关掉后永不超时） |
| `tasksEnabled` | `true` | 是否启用任务功能 |
| `lockView` | `true` | 旁观者视角是否锁定当前玩家 |
| `rounds` | `0` | 单局轮数，0 = 无限轮 |
| `victoryOnDragon` | `true` | 击杀末影龙是否立即通关 |
| `setRespawnOnHandoff` | `true` | 交接时是否设置重生点 |
| `warnSeconds` | `60` | 剩余多少秒播报提醒，0 = 关闭 |
| `hudScale` | `0.75` | HUD 缩放倍率 |
| `order` | `[]` | 固定接力顺序（玩家名），空 = 按加入顺序 |
| `disabledFeatures` | `[]` | 禁用的功能 id：`tasks` / `announce` / `hud` |

改完 `relay.json` 需要重启服务端（单人则是退出存档再进）。大部分项也可以直接在 `/relay` 面板里点。

### relay-tasks.json

任务池，格式与扩展方法见 [docs/TASKS.md](docs/TASKS.md)。**只有服务端读这个文件**，客户端不需要同步。

## 任务系统

- 每棒开局按 `taskCount` 抽任务，同一棒不会重复，也不会和已完成的任务重复；
- 任务卡在 HUD 左侧：难度、分类、标题、进度、剩余时间；完成的变灰打勾并沉到列表底部，超时的变灰打叉；
- 鼠标悬停任务卡 → 右侧展开详情（完整条件、进度、限时、奖惩）；
- 打开任意容器界面 → HUD 隐藏，改为容器左侧显示所有任务详情；
- 难度决定出现概率：`tierWeights` 默认 `[1, 2, 4, 7, 10]`，依次对应 S/A/B/C/D，数值越小越稀有。

支持的任务类型：

| type | 说明 | param |
| --- | --- | --- |
| `DISTANCE` | 离本棒起点至少 N 格（水平） | 留空 |
| `BLOCK_BREAK` | 破坏 N 个方块 | 方块 id、逗号分隔多个 id，或标签如 `#minecraft:logs` |
| `MOB_KILL` | 击杀 N 只生物 | 实体 id，如 `minecraft:zombie` |
| `ITEM_COUNT` | 背包里累计 N 个物品 | 物品 id，如 `minecraft:iron_ingot` |
| `ITEM_SET` | 集齐列表里所有物品 | 逗号分隔的物品 id |
| `ITEM_ANY` | 拥有列表里任意一件 | 逗号分隔的物品 id |
| `STAT` | 原版统计达到 N（钓鱼/坐船/交易…） | 统计 id，如 `minecraft:fish_caught` |
| `DIMENSION` | 进入指定维度 | `minecraft:the_nether` |
| `ADVANCEMENT` | 完成指定进度 | 进度 id，如 `minecraft:story/mine_stone` |

任务示例：

```json
{
  "id": "RES-001",
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

## 交接与旁观

**交接**（时间到 / `/relay skip` / 当前玩家掉线且后面还有人）：

1. 采集当前玩家状态（背包 41 格、选中槽、经验、血量、饥饿、饱和度、状态效果、维度坐标朝向）；
2. 清空除上一棒外所有人的背包，并逐格发包刷新客户端；
3. 下一棒传送到交接位置，套用背包与状态，切换生存，重置计时，设置重生点；
4. 原玩家清空背包与经验后转旁观。

**旁观**：

- `lockView = true`（默认）：所有旁观者视角锁定当前玩家；按 Shift 脱离后每 5 tick（0.25 秒）自动锁回；
- `lockView = false`：自由旁观，但每次换人时所有旁观者会被传送到交接点，避免位置错乱。

## 从源码构建

需要 **JDK 25**：

```bash
./gradlew build          # 产物 build/libs/relay-survival-<version>.jar
./gradlew test           # 纯逻辑单元测试（倒计时 / 轮换 / 任务抽取）
./gradlew runServer      # 本地开发服务器（首次需在 run/ 下同意 eula）
./gradlew runClient      # 本地开发客户端
```

验证"功能可插拔"的精简包：

```bash
./gradlew slimJar -PexcludeFeature=tasks
# build/libs/relay-survival-<version>-no-tasks.jar
```

国内网络如果卡在下载 Gradle，可把 `gradle/wrapper/gradle-wrapper.properties` 里的
`distributionUrl` 换成镜像（例如腾讯云 `https://mirrors.cloud.tencent.com/gradle/gradle-9.8.2-bin.zip`）。

## 项目结构

```
src/main/java/com/relay/
├── api/            接口与数据（核心与功能都只依赖它）
├── core/           计时、轮换、交接、快照、命令、设置面板
└── feature/        可插拔功能：tasks / announce / hud
src/client/java/com/relay/client/
└── hud/            HUD 渲染、容器任务面板、状态缓存
src/test/java/      纯逻辑单元测试
```

架构细节、如何新增/删除功能见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 已知限制

- 仅适配 **26.2**，26.3 未测试；
- 离线模式下拿不到正版皮肤签名，HUD 头像大概率显示默认皮肤；
- 没有结算界面与个人贡献统计；
- 任务池是自设计文案（156 条），不是视频里原模组的原文案；
- 自定义任务的物品/方块/进度/统计 id 必须存在于当前版本，否则任务无法完成；
- HUD 渲染出现异常时会自动停用 HUD 并写日志，不会导致游戏崩溃；
- 修改任务池后需重启服务端（或退出存档重进）才生效。

## 致谢

- 玩法来源：B 站 [夏天y](https://space.bilibili.com/84130356)《天门接力挑战》、[大炒面制造者Cen](https://space.bilibili.com/14890801)《区区接力生存》；
- 原始灵感：Andronicus — *Speedrunning Minecraft, but Every Minute the Player Changes*；
- 任务设计参考：[Yet Another Minecraft Bingo](https://github.com/YetAnotherMinecraftBingo)；
- 模组基于 [Fabric](https://fabricmc.net/) 与 [Fabric API](https://modrinth.com/mod/fabric-api)。

## License

[MIT](LICENSE)
