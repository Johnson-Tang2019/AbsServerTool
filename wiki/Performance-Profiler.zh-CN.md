# 性能分析

[首页](Home.zh-CN.md)

## 按需启动

Profiler 默认 `IDLE`，不会常驻统计，也不会因为 TPS 下降自动启动。

~~~text
/absserver lag profile
/absserver lag profile <seconds>
~~~

默认 30 秒；当前默认范围 1～3600 秒。也可由有权限的 `abstoolmod` GUI 启动、停止和查看报告。

## 报告内容

- Tick Count、平均 MSPT、P50 / P95 / P99 / 最大 Tick。
- >50 / >100 / >200 ms Tick。
- 各维度 Tick 时间。
- 插桩测得的 Chunk、Entity、Block Entity、scheduled block/fluid tick 时间。
- Chunk Load 与新区块生成。
- Slow Tick 的 Top Chunk。
- 与 World Save 重叠的慢 Tick。
- 玩家与加载/模拟范围的关联。
- Chunk 内 Entity / Block Entity 类型 Tick Calls 与耗时。

## 正确理解结果

“测得耗时”只代表插桩覆盖路径，不是完整 JVM CPU flamegraph。

“玩家关联”表示玩家位置/加载范围与 Chunk 重叠，**不证明玩家造成卡顿**。

要定位具体 Java 方法或其他 Mod 调用栈，可配合 spark/JFR。

Profiler 只保留最近一次报告在内存中，服务器重启后清空。
