# 架构说明

## 设计目标

1. **核心与功能解耦**：核心不认识任何具体功能，删掉某个功能后其余部分照常运行；
2. **功能之间不互相引用**：功能拿不到另一个功能的实现类，只能通过接口服务或事件通信；
3. **单个功能出错不影响整局**：功能初始化、回调异常都会被隔离并记录日志；
4. **纯逻辑可测试**：计时、轮换、任务抽取等不依赖 Minecraft，可直接跑单元测试。

## 三层结构

```
com.relay.api      接口与数据（无具体实现）
com.relay.core     核心实现：计时、轮换、交接、快照、命令、设置面板
com.relay.feature  可插拔功能：tasks / announce / hud
```

### api

| 接口 | 作用 |
| --- | --- |
| `RelayFeature` | 功能生命周期：`attach` / `onSessionStart` / `onTurnStart` / `onSecond` / `onTurnEnd` / `onSessionEnd` / `onPlayerDeath` / `onPlayerJoin` / `onPlayerLeave` |
| `RelayBootstrap` | 早期初始化钩子（在 `onInitialize` 阶段调用），用于注册网络通道等必须先于客户端初始化的东西 |
| `RelayContext` | 功能能看到的全部能力：`config()` / `timer()` / `turns()` / `snapshots()` / `messenger()` / `events()` / `server()` / `register()` / `find()` |
| `RelayTimer` / `RelayTurns` / `RelaySnapshots` / `RelayMessenger` / `RelayEvents` | 计时、轮换、快照、消息、事件总线 |
| `RelayServices` | 服务注册表：`register(接口, 实现)` / `find(接口)`；`SecondListener` 用于每秒回调 |
| `TaskService` | 任务功能对外暴露的只读视图（HUD 通过它读任务，不依赖 tasks 实现） |
| `RelayConfig` | 配置数据对象 |

### core

- `RelayRuntime`：核心状态机，实现 `RelayContext` / `RelayTurns` / `RelayTimer`；
- `Countdown` / `TurnQueue`：纯逻辑，可单测；
- `SnapshotService`：交接快照（只保留"待交接"的一份，套用后立即消费）；
- `FeatureRegistry`：`ServiceLoader` 发现功能并隔离异常；
- `RelayCommand` / `RelayMenu`：指令与聊天栏点击式设置面板。

### feature

- `tasks`：任务池、任务实例、进度判定、刷新与预算；
- `announce`：开局/接棒/倒计时/结束的标题、音效、聊天播报；
- `hud`：服务端每秒同步 + 客户端渲染；含容器界面任务面板。

## 启动时序

```
Fabric onInitialize
 └─ RelayMod.runBootstraps()           // RelayBootstrap：注册网络通道
     └─ （客户端）RelayClient.onInitializeClient()   // 注册 HUD 接收器与绘制层
ServerLifecycleEvents.SERVER_STARTED
 └─ new RelayRuntime(server)
     ├─ 读取/回写 config/relay.json
     └─ FeatureRegistry.load()          // ServiceLoader 发现 RelayFeature 并 attach
```

网络通道必须在 `RelayBootstrap` 阶段注册：客户端的 `registerGlobalReceiver` 要求通道已存在，而 `RelayFeature.attach` 要等到服务器启动，来不及（1.0.1 的崩溃就是这样来的）。

## 功能之间如何通信

**服务**：一个功能注册接口实现，另一个功能按接口取用。

```java
// tasks 功能
context.register(TaskService.class, this);

// hud 功能
ctx.find(TaskService.class).ifPresent(service -> { ... });
```

HUD 拿不到 `TaskService` 时自动不显示任务卡，不报错——这就是删除 tasks 后 HUD 仍能工作的原因。

**事件**：类型化事件总线，发布者与订阅者互不认识。

```java
ctx.events().subscribe(RelayEvents.TimeAdjusted.class, event -> { ... });
ctx.events().publish(new RelayEvents.TimeAdjusted(player, +60, "任务完成"));
```

## 可插拔清单

功能通过 `src/main/resources/META-INF/services/` 声明：

```
META-INF/services/com.relay.api.RelayFeature
  com.relay.feature.tasks.TaskFeature
  com.relay.feature.announce.AnnounceFeature
  com.relay.feature.hud.HudSyncFeature

META-INF/services/com.relay.api.RelayBootstrap
  com.relay.feature.hud.HudSyncFeature
```

`FeatureRegistry` 用 `ServiceLoader` 读取，单个条目加载失败只会跳过它。

## 新增一个功能

1. 在 `com.relay.feature.<id>` 下实现 `RelayFeature`：

```java
public final class MyFeature implements RelayFeature {
    private RelayContext ctx;

    @Override public String id() { return "myfeature"; }
    @Override public int order() { return 50; }   // 越小越先加载

    @Override
    public void attach(RelayContext context) {
        this.ctx = context;
        // 注册服务、订阅事件、注册 Fabric 事件……
    }

    @Override
    public void onTurnStart(TurnInfo turn) { /* ... */ }
}
```

2. 在 `META-INF/services/com.relay.api.RelayFeature` 追加一行类名；
3. 需要早期初始化（网络通道等）就同时实现 `RelayBootstrap` 并加入对应清单；
4. 重新构建。核心无需修改。

## 删除一个功能

1. 删掉 `com/relay/feature/<id>/` 整个包；
2. 删掉 `META-INF/services/...` 里对应行；
3. `./gradlew build`。

不改源码验证：

```bash
./gradlew slimJar -PexcludeFeature=tasks
```

生成的 jar 不含 tasks 包、服务清单只剩两条，服务器仍可正常启动，日志为：

```
功能 announce 已加载
功能 hud 已加载
接力生存已就绪
```

## HUD 协议

- 通道：`relay:hud_v2`（`CustomPacketPayload`，`StreamCodec` 手写读写）；
- 服务端每秒（以及换人时）向所有玩家发送当前状态：棒次、剩余时间、当前玩家名/UUID、缩放、任务列表（标题/描述/进度/限时/状态）；
- 发送前用 `ServerPlayNetworking.canSend` 判断客户端是否注册了该通道：
  - 客户端没装模组或版本不一致 → 跳过发送，并给该玩家发一条聊天提示，**不会踢人**；
- 修改包字段时**务必同时改通道名**（例如 `relay:hud_v3`），否则旧客户端会按旧格式解析新包，触发网络协议错误被踢下线。

## 测试策略

`src/test/java` 下是纯逻辑测试，不依赖 Minecraft：

- `CountdownTest`：倒计时、加减时间、暂停、格式化；
- `TurnQueueTest`：顺序轮换、多轮循环、无限轮、掉线移除；
- `TaskInstanceTest`：进度上限、超时、按权重抽取。

涉及 Minecraft 对象（玩家、快照、HUD）的部分通过开发服务器 `./gradlew runServer` 验证加载与命令，客户端渲染需实机确认。
