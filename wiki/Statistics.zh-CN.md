# 统计、快照与 DAU/WAU

[首页](Home.zh-CN.md)

## 数据来源

| 指标 | 原版来源 |
| --- | --- |
| 在线时间 | `minecraft:play_time` |
| 死亡 | `minecraft:deaths` |
| 方块放置指标 | 当前注册 BlockItem 的 `minecraft:used` 聚合 |
| 进度 | 可显示 advancement 的 vanilla 完成状态 |

方块指标遵循原版 Item Used 语义，AbsServerTool 不监听放置事件去创建第二套计数。

## 在线/离线玩家

在线玩家读取内存最新统计；离线玩家读取世界存档。在线值覆盖同 UUID 的磁盘值，不相加。UUID 是身份主键。

## DAU / WAU

~~~text
今日值 = 当前累计值 - 今日基线
本周值 = 当前累计值 - 周一基线
~~~

DAU 是今日 `play_time` 出现正增量的不同玩家数；WAU 同理作用于本周。它们不是登录次数。

快照默认保留 180 天，时区默认 `SYSTEM`。边界不完整时会标记 partial。

## 假人过滤

运行期间识别到的 Carpet fake player 会被排除。旧存档假人可配置在：

~~~text
config/absservertool-excluded-players.json
~~~
