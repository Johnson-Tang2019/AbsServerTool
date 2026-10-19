# 功能总览

[首页](Home.zh-CN.md)

## 原版统计与快照

AbsServerTool 读取 Minecraft 原版累计数据：`minecraft:play_time`、`minecraft:deaths`、BlockItem 的 `minecraft:used`，以及可显示 advancement 完成状态。

Mod 保存每日**时间快照**，而不是自己重新计数玩家行为。当前累计值减去日/周边界快照得到 DAU、WAU 和今日/本周增量。

## 性能 Profiler

默认完全空闲。只有管理员命令或 `abstoolmod` GUI 显式启动后才采样。报告包含 Tick 百分位、维度、Chunk/Entity/Block Entity、scheduled tick、Chunk Load/Generate、Slow Tick、World Save 重叠与玩家区域关联。

## 共享标点

服务器持有 Xaero 共享标点权威数据、revision 与管理权限，客户端只负责显示和编辑入口。服务端不需要 Xaero。

## QQ 播报

通过 NapCat OneBot v11 HTTP 向一个或多个 QQ 群发送每日服务器统计。

## 猪灵交易优化

0.3.7 新增，默认关闭。针对大量堆叠猪灵减少部分移动、感知、交互和推挤工作，同时保留原版金锭接受、交易时序和 loot table。
