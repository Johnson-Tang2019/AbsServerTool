# AbsServerTool 开发文档

> 项目阶段：MVP / v0.1.0  
> 目标平台：Minecraft Java Edition 26.2 + Fabric  
> Mod 名称：AbsServerTool  
> Mod ID：`absservertool`  
> Java 基础包名：`abyssredemption`  
> 运行环境：纯服务端（Dedicated Server 优先，同时兼容集成服务器）  
> 第一阶段功能：死亡榜、累计在线时间榜、方块总放置量榜、每周方块放置量榜  
> 数据原则：原版已有的数据直接读取原版 Statistics；原版没有的数据由 AbsServerTool 以最小增量方式维护

---

## 1. 项目定位

AbsServerTool 是一个面向 Minecraft Fabric 服务器的轻量级服务器工具 Mod。

第一阶段不追求“大而全”，只完成四个可靠、低侵入、无需客户端安装的排行榜：

1. **累计在线时间榜**
2. **死亡次数榜**
3. **方块总放置量榜**
4. **每周方块放置量榜**

累计在线时间与死亡次数由 Minecraft 原版 Statistics 直接维护，AbsServerTool 只读取原版数据。

方块放置量不同：Java 版原版没有可以直接精确读取的“玩家累计成功放置方块总数”统计，因此 AbsServerTool 必须从 Mod 安装并开始运行后自行记录成功的玩家方块放置事件。累计总量使用自定义原版 Statistic 持久化；周统计使用 Fabric 26.2 的持久化服务器级数据存储。

这样做的主要优点：

- 在线时间与死亡数在 Mod 后装后仍能读取服务器已有历史数据。
- 方块放置量只能从 AbsServerTool 开始记录后精确累计，不能可靠还原安装前历史。
- 删除 Mod 不会破坏原版统计信息。
- 不需要监听每个游戏 tick 自己累加时间。
- 不需要修改玩家 NBT。
- 不需要客户端安装。
- 数据与原版 `/stats`、统计界面所依据的数据保持一致。
- 第一版仅允许为“确认成功的 BlockItem 放置”使用一个最小、单一职责的服务端 Mixin；其余统计读取不使用 Mixin。

---

## 2. 第一阶段功能范围

### 2.1 必须实现

#### A. 累计在线时间榜

读取原版：

```text
minecraft:custom
└── minecraft:play_time
```

排行榜按照累计 `play_time` 从高到低排列。

显示示例：

```text
========== 在线时间榜 ==========
#1 AbyssRedemption   328天 14小时 27分
#2 PlayerA           205天 03小时 51分
#3 PlayerB            97天 18小时 06分
#4 PlayerC            32天 11小时 42分
------------------------------
第 1 / 3 页 · 共 27 名玩家
```

这里的“在线时间”指：

> 玩家在该服务器世界中累计被原版 `minecraft:play_time` 统计的时间。

它不是“活跃时间”，不会自动扣除 AFK。

#### B. 死亡榜

读取原版：

```text
minecraft:custom
└── minecraft:deaths
```

排行榜按死亡次数从高到低排列。

显示示例：

```text
========== 死亡榜 ==========
#1 PlayerC          428 次
#2 PlayerA          193 次
#3 AbyssRedemption  117 次
#4 PlayerB           61 次
------------------------------
第 1 / 3 页 · 共 27 名玩家
```


#### C. 方块总放置量榜

原版没有可直接用于该需求的历史“方块总放置量”统计，因此 AbsServerTool 注册一个自定义 Statistic：

```text
absservertool:blocks_placed
```

每当玩家**成功完成一次方块物品放置**时：

```text
blocks_placed += 1
```

排行榜：

```text
========== 方块总放置量 ==========
#1 BuilderA          1,284,552
#2 AbyssRedemption     827,341
#3 PlayerB              92,114
------------------------------
第 1 / 3 页 · 共 27 名玩家
```

重要限制：

> 该数据只能从 AbsServerTool 首次启用此功能之后开始精确累计。安装前的方块放置历史无法从原版 Statistics 中可靠重建。

#### D. 每周方块放置量榜

AbsServerTool 同时为每次成功方块放置增加当前自然周计数。

默认周定义：

```text
星期一 00:00:00
    ↓
下周星期一 00:00:00
```

周边界使用配置的 `ZoneId` 计算，避免直接依赖服务器启动时间。

显示示例：

```text
========== 本周方块放置量 ==========
2026-W38

#1 AbyssRedemption   38,214
#2 BuilderA          21,887
#3 PlayerC            9,311
------------------------------
第 1 / 2 页 · 共 16 名玩家
```

周统计不是“总量差值临时计算”，而是每次成功放置时同步增加当前周的独立计数，以便跨重启、跨周后仍可保留历史。

---

## 3. 明确不在 v0.1.0 中实现的内容

以下功能暂时不要加入第一版，以避免项目一开始就膨胀：

- AFK 时间检测
- 今日在线时间
- 本周在线时间
- 最近 7 天在线时间
- 每次登录会话时长
- 玩家在线/离线历史曲线
- PvP 击杀榜
- 生物击杀榜
- 挖掘方块榜
- Web 面板
- 数据库
- SQLite / MySQL
- LuckPerms 强依赖
- PlaceholderAPI
- 客户端 GUI
- 自定义网络协议
- 自定义数据包
- 为其他功能额外增加 Mixin（方块成功放置检测的单一 Mixin 除外）

这些可以在后续版本中逐步扩展。

---

## 4. 技术基线

### 4.1 Minecraft / Fabric

目标：

```text
Minecraft: 26.2
Loader: Fabric Loader
API: Fabric API 26.2 对应版本
Java: 使用 Minecraft 26.2 / Fabric 官方开发模板要求的 Java 版本
Mappings: 项目创建时固定一种映射体系，不要在同一源码中混用 Yarn 与 Mojang/官方映射名称
```

Fabric 26.2 官方文档仍提供 Statistics 系统，并使用 `CommandRegistrationCallback` 注册服务端命令。

项目生成后应以实际 `gradle.properties` 与 `fabric.mod.json` 中锁定的版本为准。

### 4.2 Mod 元信息

推荐：

```text
Name: AbsServerTool
Mod ID: absservertool
Version: 0.1.0
Environment: server
Base package: abyssredemption
Entrypoint: abyssredemption.AbsServerTool
```

如果希望代码结构更规整，可以将入口仍放在根包：

```java
package abyssredemption;
```

其余模块放在子包：

```text
abyssredemption.command
abyssredemption.stats
abyssredemption.player
abyssredemption.config
abyssredemption.util
```

不要再套一层 `absservertool`，因为本项目已经明确将基础包名设为 `abyssredemption`。

---

## 5. 推荐项目结构

```text
AbsServerTool/
├─ build.gradle
├─ gradle.properties
├─ settings.gradle
├─ gradlew
├─ gradlew.bat
├─ LICENSE
├─ README.md
├─ DEVELOPMENT.md
│
└─ src/
   └─ main/
      ├─ java/
      │  └─ abyssredemption/
      │     ├─ AbsServerTool.java
      │     │
      │     ├─ command/
      │     │  ├─ AbsServerCommands.java
      │     │  ├─ PlaytimeLeaderboardCommand.java
      │     │  ├─ DeathLeaderboardCommand.java
      │     │  └─ BlockPlacementCommand.java
      │     │
      │     ├─ stats/
      │     │  ├─ PlayerStatsService.java
      │     │  ├─ VanillaStatsReader.java
      │     │  ├─ BlockPlacementService.java
      │     │  ├─ WeeklyPlacementStore.java
      │     │  ├─ PlacementWeekKey.java
      │     │  ├─ OnlineStatsReader.java
      │     │  ├─ OfflineStatsReader.java
      │     │  ├─ LeaderboardService.java
      │     │  ├─ PlayerStatSnapshot.java
      │     │  └─ LeaderboardEntry.java
      │     │
      │     ├─ player/
      │     │  ├─ PlayerNameResolver.java
      │     │  └─ PlayerIdentity.java
      │     │
      │     ├─ mixin/
      │     │  └─ BlockItemPlacementMixin.java
      │     │
      │     ├─ config/
      │     │  ├─ AbsServerConfig.java
      │     │  └─ ConfigManager.java
      │     │
      │     └─ util/
      │        ├─ TimeFormatter.java
      │        ├─ MessageFormatter.java
      │        └─ PageUtil.java
      │
      └─ resources/
         ├─ fabric.mod.json
         └─ assets/
            └─ absservertool/
```

第一版不要创建 `client/` 源集。

---

## 6. 核心架构

总体数据流：

```text
玩家执行命令
       │
       ▼
AbsServerCommands
       │
       ▼
LeaderboardService
       │
       ▼
PlayerStatsService
       │
       ├──────── 在线玩家 ────────► OnlineStatsReader
       │                              │
       │                              ▼
       │                      内存中的原版 StatCounter
       │
       └──────── 离线玩家 ────────► OfflineStatsReader
                                      │
                                      ▼
                              world/stats/<uuid>.json
       │
       ▼
PlayerNameResolver
       │
       ▼
UUID -> 玩家名
       │
       ▼
排序 / 分页 / 格式化
       │
       ▼
发送聊天消息
```

### 6.1 设计原则

`PlayerStatsService` 是统计系统的统一入口。

命令层不应该直接：

- 打开 JSON 文件
- 找世界目录
- 解析 UUID
- 处理在线玩家
- 自己排序

命令层只负责：

1. 获取命令参数。
2. 请求排行榜服务。
3. 把返回的数据格式化并发给命令执行者。

这样后面扩展击杀榜、挖掘榜时可以直接复用底层。

---

## 7. 原版统计数据

### 7.1 累计在线时间

目标统计项：

```text
minecraft:play_time
```

属于：

```text
minecraft:custom
```

逻辑表达：

```java
Stats.CUSTOM + Stats.PLAY_TIME
```

原版该统计值内部以 tick 为单位累计。

转换：

```text
20 ticks = 1 second
1200 ticks = 1 minute
72000 ticks = 1 hour
1728000 ticks = 1 day
```

推荐内部始终保存原始值：

```java
long playTimeTicks;
```

只在输出阶段转换成人类可读格式。

虽然原版统计值通常由整数结构提供，但业务层建议立即提升到 `long`：

```java
long value = Integer.toUnsignedLong(rawValue);
```

或者至少：

```java
long value = rawValue;
```

这样业务接口以后更容易升级。

### 7.2 死亡次数

目标统计项：

```text
minecraft:deaths
```

同样属于：

```text
minecraft:custom
```

逻辑表达：

```java
Stats.CUSTOM + Stats.DEATHS
```

业务层字段：

```java
long deaths;
```

### 7.3 不要使用 Scoreboard 作为主要数据源

不要依赖管理员提前创建：

```mcfunction
/scoreboard objectives add deaths deathCount
```

原因：

- Scoreboard objective 可能不存在。
- 可能创建得比服务器存档晚。
- 管理员可能删除。
- 与 Statistics 数据源重复。
- 会增加服务器配置要求。

排行榜直接读原版 Statistics。

---

## 7A. 方块放置统计设计

### 7A.1 为什么不能直接读取原版历史统计

不要尝试把以下统计简单相加来当作方块放置量：

```text
minecraft:used/*
```

Java 版中，方块物品成功放置并不等价于“物品使用次数”统计，因此它不能作为准确的方块放置总数来源。

也不要根据：

```text
背包方块减少数量
```

推断放置量，因为物品可能：

- 被丢弃
- 被容器移动
- 被合成
- 被命令修改
- 在创造模式中不消耗
- 被其他 Mod 修改

因此必须在“服务器确认成功放置”这一刻计数。

### 7A.2 统计语义

v0.1.0 对“放置 1 个方块”的定义：

> 一个真实玩家通过一次成功的 `BlockItem` 放置动作，使服务器接受该方块放置，则计 1 次。

默认包括：

```text
普通方块
楼梯
半砖
告示牌
活板门
箱子
潜影盒
红石元件
作物类 BlockItem
门
床
其他通过 BlockItem 成功放置的方块
```

默认不包括：

```text
/setblock
/fill
结构生成
世界生成
活塞移动
掉落方块落地
末影人搬运
机器/Mod 自动放置器
水桶倒水
熔岩桶倒熔岩
打火石点火
纯右键交互但没有真正放置成功
```

门、床等一次玩家放置操作可能导致世界中出现多个 BlockState：

```text
一次玩家操作 = 1 次放置
```

而不是按最终改变的方块格子数量计算。

### 7A.3 创造模式

默认：

```json
"countCreativePlacements": true
```

即创造模式真人玩家的成功放置同样计入。

如果服务器不希望建筑管理员影响榜单，可以配置为 `false`。

### 7A.4 Fake Player

默认：

```json
"includeFakePlayers": false
```

Fabric/其他 Mod 创建的自动化 FakePlayer 不进入真人玩家放置榜。

---

## 7B. 成功放置事件捕获

### 7B.1 为什么不直接使用 UseBlockCallback

Fabric 26.2 的 `UseBlockCallback` / `ItemEvents.USE_ON` 属于“交互发生”层面的回调，并不能天然等价于：

```text
服务器最终成功放置了方块
```

如果只监听右键：

```text
右键箱子
右键失败
目标位置不可放置
被保护插件取消
物品执行其他交互
```

都有产生误计数的风险。

因此 v0.1.0 不允许：

```text
右键一次 -> placement +1
```

### 7B.2 推荐 Hook

第一版允许一个最小 Mixin：

```text
BlockItem#placeBlock(BlockPlaceContext, BlockState)
```

或 26.2 当前 mappings 中与其等价的最终成功设置方块方法。

要求：

```text
方法返回 true
AND
context.getPlayer() 是 ServerPlayer
AND
不是 FakePlayer（默认）
AND
满足游戏模式配置
```

才调用：

```java
BlockPlacementService.recordPlacement(serverPlayer);
```

伪代码：

```java
@Inject(
    method = "placeBlock",
    at = @At("RETURN")
)
private void absservertool$afterPlace(
    BlockPlaceContext context,
    BlockState state,
    CallbackInfoReturnable<Boolean> cir
) {
    if (!cir.getReturnValue()) return;

    if (!(context.getPlayer() instanceof ServerPlayer player)) {
        return;
    }

    BlockPlacementService.recordPlacement(player);
}
```

注意：

> 这是设计伪代码。Minecraft 26.2 最终方法名、签名必须以项目锁定 mappings 的实际源码为准，不允许为了匹配文档而强行使用旧版本名称。

### 7B.3 Mixin 必须保持极小

Mixin 只负责：

```text
确认成功
       ↓
提取 ServerPlayer
       ↓
调用 service
```

禁止在 Mixin 内：

```text
写 JSON
算周数
排序
发聊天
扫描玩家
读配置文件
```

所有业务逻辑放进：

```text
BlockPlacementService
```

这样后续 26.3/27.x 方法签名变化时，只需要修一个薄 Hook。

---

## 7C. 累计放置量持久化

### 7C.1 自定义原版 Statistic

注册：

```text
absservertool:blocks_placed
```

推荐：

```java
public static final Identifier BLOCKS_PLACED =
    register("blocks_placed", StatFormatter.DEFAULT);
```

成功放置：

```java
player.awardStat(BLOCKS_PLACED);
```

这样累计总量会跟随 Minecraft 玩家 Statistics 的原生持久化机制保存。

优点：

- 不需要额外维护“总量数据库”
- UUID 天然与玩家 Stats 文件关联
- 玩家离线后仍能读取
- 服务端重启不会清零

### 7C.2 历史起点

必须在文档和命令显示中明确：

```text
统计起点 = AbsServerTool 启用 blocks_placed 功能的时间
```

不能宣称这是服务器开服以来的历史总放置量。

推荐第一次启用时在服务器级数据中写入：

```text
placementTrackingStartedAt
```

例如：

```text
2026-09-17T10:30:00+08:00
```

管理员查询时可以看到：

```text
方块放置统计自 2026-09-17 起记录
```

---

## 7D. 每周放置量持久化

### 7D.1 推荐使用 Global Data Attachment

Fabric 26.2 提供服务器级 `GlobalAttachments`，可从：

```java
MinecraftServer.globalAttachments()
```

获取。

周数据使用**持久化 Data Attachment + Codec**，而不是自己每次覆盖 JSON 文件。

推荐数据结构：

```java
public record WeeklyPlacementData(
    String trackingStartedAt,
    Map<String, Map<UUID, Long>> weeks
) {}
```

例如逻辑数据：

```json
{
  "trackingStartedAt": "2026-09-17T10:30:00+08:00",
  "weeks": {
    "2026-W38": {
      "uuid-a": 38124,
      "uuid-b": 21887
    },
    "2026-W39": {
      "uuid-a": 9122
    }
  }
}
```

实际 Data Attachment 应通过 `Codec` 序列化，不要求底层文件恰好长成上述 JSON。

### 7D.2 不同步到客户端

这是纯服务端数据：

```text
persistent = true
client sync = false
```

排行榜仍通过聊天组件把最终结果发送给客户端。

### 7D.3 数据不可变更新

Fabric Data Attachment 对持久化数据应采用不可变值语义。

每次放置：

```text
读取当前 WeeklyPlacementData
       ↓
复制并更新当前 week / UUID
       ↓
setAttached / modifyAttached
```

不要直接拿内部 `Map` 后原地修改而不重新写回 Attachment。

---

## 7E. 周键与时区

使用 ISO week：

```text
YYYY-Www
```

例如：

```text
2026-W38
```

默认：

```text
weekStartsOn = MONDAY
timezone = SYSTEM
```

推荐配置允许显式写：

```json
"timezone": "Asia/Shanghai"
```

或者：

```json
"timezone": "Asia/Tokyo"
```

周键必须基于配置的 `ZoneId` 生成，而不是：

```text
服务器启动后每 7 天
```

否则重启或停机会破坏自然周语义。

### 7E.1 修改时区

管理员如果修改 `timezone`：

- 已保存的旧周键不重写。
- 新放置按新时区计算。
- 日志给出 warning。
- 文档提醒尽量在正式启用前确定时区。

---

## 7F. 周数据保留策略

推荐默认：

```json
"weeklyRetentionWeeks": 12
```

保留最近 12 个自然周。

每次跨入新周时清理：

```text
早于 retention 的周
```

而不是每次放置都遍历清理。

如果：

```json
"weeklyRetentionWeeks": 0
```

定义为永久保留。

第一版至少必须保留：

```text
当前周
上一周
```

这样未来可以很容易增加“上周榜”。

---

## 7G. 方块放置命令

推荐命令树扩展为：

```text
absserver
├─ playtime
│  └─ [page]
├─ deaths
│  └─ [page]
└─ blocks
   └─ placed
      ├─ total
      │  └─ [page]
      └─ weekly
         └─ [page]
```

命令：

```mcfunction
/absserver blocks placed total
/absserver blocks placed total 2
/absserver blocks placed weekly
/absserver blocks placed weekly 2
```

### 7G.1 总量榜

```text
========== 方块总放置量 ==========
统计自：2026-09-17

#1 AbyssRedemption   827,341
#2 BuilderA          694,112
...
```

### 7G.2 本周榜

```text
========== 本周方块放置量 ==========
2026-W38

#1 AbyssRedemption   38,214
#2 BuilderA          21,887
...
```

### 7G.3 后续可扩展

后续版本可以加入：

```mcfunction
/absserver blocks placed lastweek
/absserver blocks placed week 2026-W37
/absserver blocks placed player <player>
```

v0.1.0 不强制实现历史周查询。

---

## 7H. 放置榜缓存

总放置量榜与周榜也进入 `LeaderboardCache`。

但是每发生一次成功放置时，不需要立刻重新排序。

做：

```text
record placement
       ↓
标记 placement leaderboard cache dirty
```

下一次有玩家查询时再重建。

这样高强度建筑时不会出现：

```text
每放 1 个方块 -> 对全服玩家重新排序
```

---

## 7I. 方块放置统计验收测试

### 成功放置

玩家放 64 个石头：

```text
total +64
weekly +64
```

### 放置失败

目标位置不可放：

```text
+0
```

### 右键容器

手持石头右键箱子但打开 GUI：

```text
+0
```

### 门

放置 1 扇门：

```text
+1
```

不是 +2。

### 床

放置 1 张床：

```text
+1
```

不是 +2。

### 创造模式

配置：

```json
"countCreativePlacements": true
```

则成功放置：

```text
+1
```

关闭配置则：

```text
+0
```

### 命令

```mcfunction
/setblock ~ ~ ~ stone
```

```text
+0
```

### 活塞

活塞推动已有方块：

```text
+0
```

### 自动放置器 / FakePlayer

默认：

```text
+0
```

### 重启

重启服务器后：

```text
total 不丢失
weekly 不丢失
```

### 跨周

周一进入新周后：

```text
total 继续累计
weekly 当前榜从新周独立累计
旧周数据仍按 retention 保留
```

---

## 7J. 方块统计的数据可靠性声明

对外显示时必须区分：

```text
死亡榜 / 在线时间榜
```

可以包含安装 AbsServerTool 之前的原版历史统计。

而：

```text
方块总放置量 / 每周放置量
```

只能保证：

> 自 AbsServerTool 启用方块放置追踪之后的数据准确。

绝对禁止把首次启用时的 `0` 描述成：

```text
玩家开服以来从未放置方块
```

它只能表示：

```text
AbsServerTool 记录期内为 0
```

---

## 8. 在线玩家与离线玩家的数据策略

这是整个项目最重要的设计点之一。

### 8.1 在线玩家

玩家在线时，统计数据可能仍在内存中变化，而磁盘上的：

```text
world/stats/<uuid>.json
```

不一定刚刚写盘。

因此：

> 在线玩家必须优先读取服务器内存中的原版统计对象。

逻辑：

```text
如果 UUID 当前在线
    读取在线 ServerPlayer 的 StatCounter / ServerStatsCounter
否则
    读取磁盘 stats JSON
```

这样排行榜不会出现玩家在线挂机了几小时，但排行榜仍显示旧值的问题。

### 8.2 离线玩家

离线玩家读取：

```text
<level root>/stats/<uuid>.json
```

服务器中常见结构：

```text
world/
├─ level.dat
├─ playerdata/
├─ advancements/
└─ stats/
   ├─ <uuid-a>.json
   ├─ <uuid-b>.json
   └─ <uuid-c>.json
```

注意：

> 不要把世界目录硬编码成字符串 `"world"`。

管理员可以在 `server.properties` 中修改：

```properties
level-name=xxx
```

必须通过服务器 API 获取当前 level root / world save path。

### 8.3 数据优先级

统一规则：

```text
在线内存数据 > stats JSON
```

禁止将两者相加。

JSON 是同一统计数据的持久化版本，不是另一份独立累计量。

---

## 9. OfflineStatsReader 设计

### 9.1 输入

```java
UUID playerUuid
```

### 9.2 输出

推荐：

```java
Optional<PlayerStatSnapshot>
```

数据模型：

```java
public record PlayerStatSnapshot(
    UUID uuid,
    long playTimeTicks,
    long deaths
) {}
```

### 9.3 JSON 解析目标

只读取需要的字段。

概念结构：

```json
{
  "stats": {
    "minecraft:custom": {
      "minecraft:play_time": 123456789,
      "minecraft:deaths": 52
    }
  }
}
```

不要把完整 stats 文件映射成几十个 Java 类。

推荐使用 Gson 的 `JsonObject` 做防御性解析。

逻辑：

```text
root
 └ stats
    └ minecraft:custom
       ├ minecraft:play_time
       └ minecraft:deaths
```

### 9.4 缺失值规则

如果：

```text
minecraft:play_time
```

不存在：

```text
playTimeTicks = 0
```

如果：

```text
minecraft:deaths
```

不存在：

```text
deaths = 0
```

如果整个 stats 文件损坏：

- 不让命令崩溃。
- 写 warning 日志。
- 跳过该玩家。
- 继续处理其他玩家。

例如：

```text
[AbsServerTool] Failed to read statistics for UUID xxx: malformed JSON
```

一个坏文件不能让整个排行榜失效。

---

## 10. OnlineStatsReader 设计

职责：

> 从当前在线 ServerPlayer 对应的原版统计计数器读取数据。

伪代码：

```java
PlayerStatSnapshot read(ServerPlayer player) {
    var stats = player.getStats();

    int playTime = stats.getValue(
        Stats.CUSTOM.get(Stats.PLAY_TIME)
    );

    int deaths = stats.getValue(
        Stats.CUSTOM.get(Stats.DEATHS)
    );

    return new PlayerStatSnapshot(
        player.getUUID(),
        playTime,
        deaths
    );
}
```

注意：

26.2 实际类名、getter 名称需要按照项目当前 mappings 生成的源码/API 为准。

开发时优先确认：

```text
ServerPlayer
ServerStatsCounter
Stats.CUSTOM
Stats.PLAY_TIME
Stats.DEATHS
```

不要为了兼容旧教程直接复制 1.20.x 的 Yarn 类名。

---

## 11. 玩家发现策略

排行榜必须解决一个问题：

> “服务器到底有哪些历史玩家？”

仅遍历在线玩家显然不够。

### 11.1 权威集合

第一版推荐直接扫描：

```text
stats/*.json
```

文件名就是 UUID：

```text
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.json
```

因此：

```text
stats 文件集合
        +
当前在线玩家 UUID
        =
本次排行榜的玩家集合
```

之所以还要加在线玩家，是为了处理极端情况：

- 玩家刚第一次加入。
- stats 文件尚未生成或还没写盘。
- 玩家已经在线产生统计，但磁盘文件暂时不存在。

### 11.2 不要以 usercache.json 作为玩家集合

`usercache.json` 只用于辅助 UUID -> 名称。

它不应该决定“排行榜有哪些玩家”，因为：

- cache 可能被清理。
- cache 有容量/历史变化。
- stats 文件才是已有统计数据存在的直接证据。

---

## 12. 玩家名称解析

排行榜不能只显示 UUID。

推荐名称解析优先级：

```text
1. 当前在线玩家对象中的 GameProfile 名称
2. Minecraft Server 的 profile cache
3. usercache.json / 服务器已有 profile 缓存
4. 无法解析时显示缩短后的 UUID
```

禁止为了每次执行排行榜而请求 Mojang API。

原因：

- 网络延迟。
- API 限速。
- 离线模式服务器不适用。
- 网络失败不应该影响排行榜。

### 12.1 名字变化

Minecraft 玩家可能改名。

因此：

> UUID 是身份主键，名称只是显示信息。

内部所有缓存、统计、排序必须以 UUID 为 key。

正确：

```java
Map<UUID, PlayerStatSnapshot>
```

不要：

```java
Map<String, PlayerStatSnapshot>
```

---

## 13. 排行榜服务

推荐统一模型：

```java
public record LeaderboardEntry(
    UUID uuid,
    String playerName,
    long value
) {}
```

### 13.1 在线时间榜

```java
List<LeaderboardEntry> getPlaytimeLeaderboard()
```

排序：

```text
playTimeTicks DESC
```

同值时：

```text
playerName ASC
```

用于保证结果稳定。

### 13.2 死亡榜

```java
List<LeaderboardEntry> getDeathLeaderboard()
```

排序：

```text
deaths DESC
```

同值：

```text
playerName ASC
```

### 13.3 是否显示 0

默认：

```text
play_time = 0 -> 不显示
deaths = 0 -> 可不显示
```

推荐配置：

```json
{
  "includeZeroValues": false
}
```

这样一个从未死亡的玩家不会占据死亡榜尾部几百行。

---

## 14. 命令设计

第一版根命令：

```mcfunction
/absserver
```

### 14.1 在线时间榜

```mcfunction
/absserver playtime
```

默认：

```text
第 1 页
每页 10 名
```

指定页：

```mcfunction
/absserver playtime 2
```

### 14.2 死亡榜

```mcfunction
/absserver deaths
```

指定页：

```mcfunction
/absserver deaths 2
```

### 14.3 帮助

```mcfunction
/absserver
```

输出：

```text
AbsServerTool v0.1.0

/absserver playtime [页码]
  查看累计在线时间榜

/absserver deaths [页码]
  查看死亡次数榜
```

### 14.4 可选短别名

v0.1.0 可以暂不加入。

后续若需要：

```mcfunction
/abst playtime
/abst deaths
```

应当使用 Brigadier redirect 或复用同一执行方法，禁止复制两套命令逻辑。

---

## 15. 权限设计

两个排行榜都是只读功能。

v0.1.0 默认：

```text
所有玩家可执行
控制台可执行
RCON 可执行
```

不要求 OP。

未来如果增加：

```text
/absserver reload
/absserver debug
/absserver purge
```

再单独要求管理权限。

第一版不要强制依赖 LuckPerms。

如果以后需要权限插件兼容，可独立加入 permission adapter 层。

---

## 16. Brigadier 命令树

概念：

```text
absserver
├─ playtime
│  └─ [page:int >= 1]
└─ deaths
   └─ [page:int >= 1]
```

Fabric 侧注册：

```java
CommandRegistrationCallback.EVENT.register(
    (dispatcher, registryAccess, environment) -> {
        AbsServerCommands.register(dispatcher);
    }
);
```

命令层只调用：

```java
LeaderboardService
```

不要在 `.executes(...)` lambda 内写几十行统计读取代码。

---

## 17. 输出格式

### 17.1 在线时间格式化

`TimeFormatter`：

输入：

```java
long ticks
```

输出示例：

```text
43秒
18分 32秒
3小时 27分
2天 13小时 08分
328天 14小时 27分
```

排行榜为了紧凑，推荐：

```text
<1 小时       -> 12分 31秒
<1 天         -> 8小时 17分
>=1 天        -> 32天 11小时 42分
```

不需要显示 tick。

### 17.2 排名颜色建议

仅使用原版文本样式，不需要客户端 Mod。

例如：

```text
#1 金色
#2 灰白
#3 深金/铜色近似
其他 灰色
玩家名 白色
数字 黄色
页码 深灰
```

颜色只是显示，不参与业务逻辑。

### 17.3 Hover 文本

可以作为 v0.1.0 的小增强，不是硬要求。

在线时间项 hover：

```text
UUID: ...
原始统计: 123456789 ticks
```

死亡项 hover：

```text
UUID: ...
Deaths: 117
```

---

## 18. 分页

配置：

```text
pageSize = 10
```

计算：

```java
int totalPages = Math.max(1, (totalEntries + pageSize - 1) / pageSize);
```

如果请求：

```mcfunction
/absserver deaths 999
```

不要抛异常。

推荐返回：

```text
页码超出范围。死亡榜当前共 3 页。
```

### 18.1 可点击翻页

推荐加入原版 ClickEvent：

```text
[上一页] [下一页]
```

点击运行：

```mcfunction
/absserver deaths 2
```

纯服务端即可实现，不需要客户端 Mod。

---

## 19. 缓存设计

服务器可能有：

- 20 名玩家
- 200 名玩家
- 2000 名历史玩家

如果每个玩家每次敲命令都把全部 JSON 文件重新打开一次，会造成不必要的磁盘访问。

### 19.1 第一版推荐缓存

维护：

```java
LeaderboardCache
```

缓存：

```text
完整在线时间榜
完整死亡榜
生成时间
```

默认 TTL：

```text
30 秒
```

流程：

```text
执行排行榜命令
      │
      ├─ 缓存 < 30 秒
      │      └─ 直接返回
      │
      └─ 缓存过期
             └─ 重新构建
```

### 19.2 在线玩家问题

即使缓存 30 秒，也只是排行榜显示最多延迟几十秒，可接受。

如果希望更实时：

```text
cacheTtlSeconds = 10
```

默认推荐：

```text
30
```

### 19.3 不要每 tick刷新

禁止：

```text
ServerTickEvents.END_SERVER_TICK
 -> 扫描所有 stats JSON
```

这是完全没有必要的 I/O。

---

## 20. 异步 I/O

离线 stats 文件读取属于磁盘 I/O。

对于只有几十名历史玩家的服务器，同步读取通常问题不大。

但为了让架构可以扩展，推荐设计为：

```text
磁盘读取/解析 -> 后台 executor
Minecraft 对象访问/聊天发送 -> server thread
```

### 20.1 必须注意

不要在后台线程随意访问：

```text
MinecraftServer
ServerPlayer
PlayerList
ServerLevel
```

这些 Minecraft 对象通常应在 server thread 使用。

推荐：

```text
1. server thread 收集在线玩家 snapshot
2. 后台读取离线 JSON
3. 合并普通 Java DTO
4. server.execute(...) 回主线程发消息
```

MVP 若先同步实现，也必须保证：

- 不在 tick listener 高频调用。
- 有 30 秒缓存。
- 文件数量不大时可接受。

后续再异步化。

---

## 21. 配置文件

路径建议：

```text
config/absservertool.json
```

默认：

```json
{
  "leaderboard": {
    "pageSize": 10,
    "cacheTtlSeconds": 30,
    "includeZeroValues": false
  },
  "display": {
    "showUuidOnHover": true,
    "enableClickablePagination": true
  },
  "placements": {
    "enabled": true,
    "countCreativePlacements": true,
    "includeFakePlayers": false,
    "timezone": "SYSTEM",
    "weekStartsOn": "MONDAY",
    "weeklyRetentionWeeks": 12
  }
}
```

### 21.1 配置错误

配置 JSON 损坏时：

```text
1. 打 warning
2. 不覆盖原文件
3. 使用默认配置继续启动
```

不要因为排行榜配置损坏导致服务器无法启动。

---

## 22. 日志

Logger 名称：

```text
AbsServerTool
```

启动：

```text
[AbsServerTool] Initializing AbsServerTool 0.1.0
[AbsServerTool] Registered server commands
[AbsServerTool] Statistics directory: ...
```

排行榜正常查询不要刷 INFO。

错误：

```text
WARN  malformed stats file
WARN  invalid UUID filename
WARN  cannot resolve player name
```

调试信息以后可以放 DEBUG。

---

## 23. 异常与边界情况

必须处理以下情况。

### 23.1 空服务器

没有任何 stats 文件：

```text
在线时间榜暂无数据。
```

和：

```text
死亡榜暂无数据。
```

### 23.2 玩家从未死亡

如果：

```text
minecraft:deaths
```

不存在：

```text
0
```

默认由于 `includeZeroValues=false`，不进入死亡榜。

### 23.3 stats 文件损坏

只跳过对应 UUID。

### 23.4 非 UUID 文件

例如：

```text
stats/README.txt
stats/test.json
```

忽略。

只接受：

```text
<valid UUID>.json
```

### 23.5 UUID 有统计但名称未知

显示：

```text
Unknown-550e8400
```

或者：

```text
550e8400…
```

内部仍使用完整 UUID。

### 23.6 玩家正在在线

使用内存统计覆盖磁盘统计。

禁止：

```text
diskPlaytime + onlinePlaytime
```

### 23.7 世界目录自定义

通过 API 获取 save root，不写死 `world/`。

### 23.8 命令由控制台执行

不能假定：

```java
source.getPlayer()
```

一定存在。

排行榜命令必须允许：

```text
Server Console
RCON
Command Block（可根据需要限制）
Player
```

至少 Player 与 Server Console 必须工作。

---

## 24. 性能目标

v0.1.0 的目标：

```text
在线玩家：0~100
历史 stats：0~5000
```

正常缓存命中时：

```text
不读取磁盘
只做分页 + 文本输出
```

缓存重建时：

```text
每个 stats 文件最多读取一次
每个文件只提取两个统计值
```

复杂度：

```text
读取：O(n)
排序：O(n log n)
分页：O(k)
```

其中：

```text
n = 历史玩家数
k = 每页条目数
```

这是完全可以接受的。

---

## 25. 数据模型

### PlayerIdentity

```java
public record PlayerIdentity(
    UUID uuid,
    String name
) {}
```

### PlayerStatSnapshot

```java
public record PlayerStatSnapshot(
    UUID uuid,
    long playTimeTicks,
    long deaths
) {}
```

### LeaderboardEntry

```java
public record LeaderboardEntry(
    UUID uuid,
    String playerName,
    long value
) {}
```

### LeaderboardPage

推荐增加：

```java
public record LeaderboardPage(
    List<LeaderboardEntry> entries,
    int page,
    int totalPages,
    int totalEntries
) {}
```

这样命令层不需要自己计算分页。

---

## 26. PlayerStatsService API

推荐接口：

```java
public final class PlayerStatsService {

    public Map<UUID, PlayerStatSnapshot> collectAllStats(
        MinecraftServer server
    );

    public Optional<PlayerStatSnapshot> getPlayerStats(
        MinecraftServer server,
        UUID uuid
    );
}
```

内部步骤：

```text
1. 定位 stats 目录
2. 扫描所有 UUID 文件
3. 读取离线 snapshot
4. 获取当前在线玩家
5. 使用在线 snapshot 覆盖相同 UUID
6. 返回不可变 Map
```

最终：

```text
一个 UUID 永远只有一条最终记录
```

---

## 27. LeaderboardService API

推荐：

```java
public final class LeaderboardService {

    public LeaderboardPage getPlaytimePage(
        MinecraftServer server,
        int page
    );

    public LeaderboardPage getDeathsPage(
        MinecraftServer server,
        int page
    );

    public void invalidateCache();
}
```

未来扩展时可以演化成：

```java
getLeaderboard(LeaderboardType type, int page)
```

但 MVP 不必过度抽象。

---

## 28. 缓存失效

第一版：

```text
TTL 自动过期
```

后续可以在以下事件主动失效：

```text
玩家加入
玩家退出
玩家死亡
服务器保存
```

但是第一版无需为了“绝对实时”监听一大堆事件。

`play_time` 本来就在持续变化，因此即便死亡事件立即失效，在线时间仍然会继续变化。

所以统一 TTL 更简单可靠。

---

## 29. 不自己监听死亡事件计数

不要：

```java
AFTER_DEATH -> customDeathMap++
```

因为原版已经有：

```text
minecraft:deaths
```

自建计数会产生：

- 安装前历史丢失
- 双重数据源
- 数据迁移
- 数据错位
- 事件漏记风险

AbsServerTool 第一原则：

> 原版有可靠统计，就读取原版；原版没有，才创建自己的统计。

---

## 30. 不自己监听 tick 计算总在线时间

同样不要：

```java
每 tick:
    for player:
        customPlaytime[player]++
```

因为原版已经维护：

```text
minecraft:play_time
```

第一版只做读取与展示。

---

## 31. 主入口设计

`abyssredemption.AbsServerTool`

职责只包括：

```text
初始化 logger
加载 config
创建 service
注册 command
注册必要 lifecycle event
输出启动日志
```

入口类不要承担：

- JSON stats 解析
- 排序
- 时间格式化
- 玩家名称查找

示意：

```java
public final class AbsServerTool implements ModInitializer {

    public static final String MOD_ID = "absservertool";

    @Override
    public void onInitialize() {
        ConfigManager.load();
        AbsServerCommands.register();
    }
}
```

实际代码根据 Fabric 26.2 模板/API调整。

---

## 32. fabric.mod.json

概念配置：

```json
{
  "schemaVersion": 1,
  "id": "absservertool",
  "version": "${version}",
  "name": "AbsServerTool",
  "description": "Server-side statistics and utility tools.",
  "environment": "server",
  "entrypoints": {
    "main": [
      "abyssredemption.AbsServerTool"
    ]
  },
  "depends": {
    "fabricloader": "*",
    "minecraft": "26.2",
    "fabric-api": "*"
  }
}
```

开发时应替换 `*` 为项目模板中实际验证过的兼容范围。

---

## 33. 客户端兼容

目标：

```text
客户端没有安装 AbsServerTool
        ↓
仍可以正常加入服务器
```

因此：

```json
"environment": "server"
```

且第一版不能引用：

```text
MinecraftClient
client.gui
client.network
client.render
```

所有显示使用：

```text
服务器发送的 Component/Text
ClickEvent
HoverEvent
```

客户端只负责使用原版聊天 UI 渲染。

---

## 34. 测试计划

### 34.1 单元测试

最值得独立测试的是纯 Java 部分。

#### TimeFormatter

输入：

```text
0
20
1200
72000
1728000
```

验证：

```text
0秒
1秒
1分
1小时
1天
```

#### OfflineStatsReader

测试 JSON：

1. 两个字段都有。
2. 只有 play_time。
3. 只有 deaths。
4. `stats` 不存在。
5. `minecraft:custom` 不存在。
6. 空 JSON。
7. 损坏 JSON。

#### 排序

输入：

```text
A = 10
B = 30
C = 20
```

输出：

```text
B
C
A
```

#### 同值排序

```text
Alex = 20
Steve = 20
```

保证结果稳定。

#### 分页

```text
0
1
10
11
20
21
```

条数据分别验证总页数。

---

## 35. 集成测试场景

至少准备三个测试玩家：

```text
PlayerA
PlayerB
PlayerC
```

### 场景 1：全部离线

执行：

```mcfunction
/absserver playtime
/absserver deaths
```

验证能读取磁盘历史统计。

### 场景 2：PlayerA 在线

让 PlayerA 在线一段时间。

执行：

```mcfunction
/absserver playtime
```

确认数据显示的是在线内存值，而不是旧 JSON。

### 场景 3：死亡

PlayerA 死亡一次。

等待/刷新缓存后：

```mcfunction
/absserver deaths
```

值 +1。

### 场景 4：重启

服务器重启后再次查询。

排行榜保持一致。

这可以证明没有依赖 AbsServerTool 自己的临时内存计数。

### 场景 5：卸载再安装

1. 关闭服务器。
2. 移除 AbsServerTool。
3. 启动并游玩。
4. 关闭服务器。
5. 重新加入 AbsServerTool。
6. 查询排行榜。

历史数据仍应该完整。

这是“原版 Statistics 为权威数据源”的关键验收测试。

---

## 36. 验收标准

v0.1.0 完成必须满足：

### 构建

- [ ] Fabric 26.2 环境可成功 `gradlew build`
- [ ] 无客户端源码依赖
- [ ] 服务端可正常启动
- [ ] 客户端不安装 Mod 也能进入

### 在线时间榜

- [ ] `/absserver playtime` 可执行
- [ ] 默认显示 Top 10
- [ ] 支持页码
- [ ] 正确读取历史离线玩家
- [ ] 正确读取在线玩家最新值
- [ ] 按累计时间降序
- [ ] 时间格式正确
- [ ] 不重复累计磁盘值与内存值

### 死亡榜

- [ ] `/absserver deaths` 可执行
- [ ] 支持页码
- [ ] 能读取历史死亡数
- [ ] 按死亡次数降序
- [ ] 玩家死亡后数据最终正确更新
- [ ] 0 次死亡默认不显示

### 健壮性

- [ ] 空服务器不报错
- [ ] 损坏 stats 文件不导致服务器崩溃
- [ ] 未知 UUID 名称有 fallback
- [ ] 自定义 level-name 正常
- [ ] 控制台可执行
- [ ] 无 stats 文件时有友好提示

---

## 37. 开发顺序

推荐严格按下面顺序实现。

### Phase 1：项目骨架

1. 创建 Fabric 26.2 项目。
2. Mod ID：`absservertool`。
3. 包：`abyssredemption`。
4. 设置 server environment。
5. 确保空 Mod 可以启动 Dedicated Server。

### Phase 2：单玩家统计读取

实现：

```text
PlayerStatSnapshot
OfflineStatsReader
OnlineStatsReader
```

先通过日志验证：

```text
UUID
play_time
deaths
```

正确。

### Phase 3：收集全部玩家

实现：

```text
PlayerStatsService
```

完成：

```text
扫描 stats
+ 在线玩家覆盖
```

### Phase 4：玩家名称

实现：

```text
PlayerNameResolver
```

输出：

```text
UUID -> name
```

### Phase 5：排行榜

实现：

```text
LeaderboardService
```

完成：

```text
sort
filter
page
```

### Phase 6：命令

实现：

```text
/absserver playtime [page]
/absserver deaths [page]
```

### Phase 7：缓存

增加：

```text
30s TTL
```

### Phase 8：显示优化

加入：

```text
颜色
hover
上一页/下一页点击按钮
```

### Phase 9：异常测试

测试：

```text
坏 JSON
空目录
未知 UUID
自定义世界名
控制台
多玩家
```

---

## 38. 第一版禁止的实现方式

为了防止后续自动编码跑偏，明确禁止：

### 禁止 1

创建：

```text
absservertool-player-data.json
```

来重复保存总死亡与总在线时间。

### 禁止 2

每 tick 给玩家在线时间 +1。

### 禁止 3

监听死亡事件重新维护一份死亡总数。

### 禁止 4

要求客户端安装 Mod。

### 禁止 5

硬编码：

```java
Path.of("world/stats")
```

### 禁止 6

把玩家名当唯一 ID。

### 禁止 7

每执行一次 `/absserver playtime` 都无缓存扫描几千个文件。

### 禁止 8

因为一个 stats JSON 损坏就抛异常终止整个排行榜。

### 禁止 9

为死亡榜、在线时间榜或普通命令逻辑增加 Mixin。

方块成功放置检测允许且只允许一个极薄的 BlockItem 放置 Hook；禁止把统计业务逻辑写在 Mixin 中。

### 禁止 10

第一版接入数据库。

### 禁止 11

用 `minecraft:used/*` 相加冒充“方块总放置量”。

### 禁止 12

把一次右键交互直接当成一次成功放置。

### 禁止 13

把门/床这种一次放置产生多个 BlockState 的情况按格子数重复计数。

---

## 39. 后续扩展预留

v0.1.0 完成后推荐按以下路线扩展。

### v0.2

玩家单独查询：

```mcfunction
/absserver stats <player>
```

显示：

```text
累计在线时间
死亡次数
生物击杀
玩家击杀
移动距离
挖掘数量
```

### v0.3

更多排行榜：

```mcfunction
/absserver top mobkills
/absserver top playerkills
/absserver top mined
```

仍优先读取原版 Statistics。

### v0.4

真正需要 AbsServerTool 自建的数据：

```text
最近 24h 在线时间
最近 7 天在线时间
每日在线时间
登录会话
AFK 时间
```

这些原版统计无法直接提供，需要自己的增量数据。

### v0.5+

加入服务器诊断：

```text
堵塞熔炉追踪
猪灵交易负载
漏斗热点
高实体区块
村民状态诊断
```

这些功能与 AbsTool 客户端工具保持分离。

---

## 40. 最终架构原则

AbsServerTool 第一阶段遵守以下规则：

```text
原版已经统计的
    ↓
直接读取原版

原版没有统计的
    ↓
AbsServerTool 自己维护
```

玩家身份：

```text
UUID 是主键
名称只负责显示
```

数据源：

```text
在线玩家 -> 内存统计
离线玩家 -> stats JSON
```

覆盖规则：

```text
online > disk
```

展示层：

```text
LeaderboardService
        ↓
Command
        ↓
Vanilla chat component
```

不需要：

```text
客户端 Mod
数据库
为原版死亡/在线时间重复建库
自建累计死亡计数
自建累计在线 tick 计数
```

唯一允许的 Mixin：

```text
BlockItem 成功放置 -> 通知 BlockPlacementService
```

它只用于获得 Fabric 26.2 当前没有直接提供的“成功放置后”精确 Hook。

---

# 41. v0.1.0 最终功能定义

AbsServerTool v0.1.0 需要稳定完成四类排行榜。

### 原版历史统计

```mcfunction
/absserver playtime [page]
```

> 累计原版 `minecraft:play_time` 排行榜。

```mcfunction
/absserver deaths [page]
```

> 原版 `minecraft:deaths` 排行榜。

这两项能够读取 AbsServerTool 安装前已经存在的原版历史统计。

### AbsServerTool 自建增量统计

```mcfunction
/absserver blocks placed total [page]
```

> 自启用方块追踪以来的累计成功放置量。

```mcfunction
/absserver blocks placed weekly [page]
```

> 当前自然周的成功方块放置量。

方块放置统计必须满足：

- 只在服务器确认成功放置后 +1。
- 一次玩家放置动作只计 1。
- 门、床不因产生两个方块状态而计 2。
- `/setblock`、`/fill`、世界生成、活塞移动不计。
- 默认排除 FakePlayer。
- 创造模式是否计入可配置，默认计入。
- 总量通过自定义 Statistic `absservertool:blocks_placed` 持久化。
- 周数据通过 Fabric 26.2 持久化 Global Data Attachment 保存。
- 周边界默认周一，并支持配置 `ZoneId`。
- 总量与周数据跨服务器重启不丢失。
- 必须明确告诉管理员：方块放置历史只能从启用追踪后开始准确统计。

四个排行榜共同要求：

- UUID 作为唯一玩家身份。
- 支持分页。
- 默认每页 10 名。
- 使用缓存，避免频繁磁盘读取与重复排序。
- 纯服务端。
- 客户端零安装。
- 控制台可查询。
- 单个损坏数据不能拖垮整个命令。

这就是 AbsServerTool v0.1.0 第一阶段的完整开发边界。

---

## 42. 开发参考

Fabric 26.2 官方文档：

- Statistics  
  https://docs.fabricmc.net/develop/statistics

- Creating Commands  
  https://docs.fabricmc.net/develop/commands/basics

- Data Attachments  
  https://docs.fabricmc.net/develop/serialization/data-attachments

- Events  
  https://docs.fabricmc.net/develop/events

Fabric API 26.2：

- `CommandRegistrationCallback`  
  https://maven.fabricmc.net/docs/fabric-api-0.149.1%2B26.2/net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback.html

开发时应以项目锁定的 Fabric API 与 mappings 版本生成的实际源码为最终准绳。若 API 名称与本文伪代码有差异，应调整调用名称，但不能改变本文确定的数据源和架构原则。
