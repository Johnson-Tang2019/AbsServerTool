# 配置

[首页](Home.zh-CN.md)

主配置：

~~~text
config/absservertool.json
~~~

当前源码默认值：

| 分组 | 默认 |
| --- | --- |
| leaderboard | pageSize=10, cacheTtlSeconds=30, includeZeroValues=false |
| network | enabled=true, minimumRequestIntervalMillis=500 |
| statistics | includeModdedBlockItems=true, currentStatsCacheSeconds=30 |
| snapshot | timezone=SYSTEM, retentionDays=180 |
| waypoints | enabled=true, permission=2, max=256, name=64, symbol=8 |
| performanceProfiler | enabled=true, default=30s, min=1s, max=3600s, slowTick=50ms, statusPoll=2000ms, pageSize=20, maxSlowTicks=200 |
| piglinOptimization | enabled=false, entityLimit=100 |

源码仍保留 `placements` 兼容配置段，但当前方块指标的事实来源是原版 BlockItem `minecraft:used` + 时间快照，并不是 Mod 自建放置计数器。

其他配置：

~~~text
config/absservertool-excluded-players.json
config/absservertool_qq.json
~~~
