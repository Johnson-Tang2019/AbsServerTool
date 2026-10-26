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

在线 Carpet fake player 会被排除；运行期间观察到的普通假人 UUID 会持久保存，
退出和重启后仍排除。0.3.9 还会在假人退出时记录，覆盖两次轮询之间创建并移除的假人。
Carpet shadow 仅在假人在线期间自动排除，不会自动永久排除真人 UUID。

未被观察过的旧离线假人无法仅凭原版统计数据识别，管理员可立即添加：

~~~text
/absserver excludePlayer add <名字或UUID>
~~~

排除适用于在线人数、玩家数、在线时长、死亡、方块、进度榜、DAU/WAU 和历史快照显示，
不会删除原版玩家数据或快照。使用 `/absserver excludePlayer remove <名字或UUID>` 恢复。
正在运行的假人始终排除；移除其 UUID 后，自动观察会再次记录。

名单持久化文件：

~~~text
config/absservertool-excluded-players.json
~~~

命令修改立即生效。手工编辑文件需要重启后读取；文件损坏时会保留原文件并拒绝覆盖。
