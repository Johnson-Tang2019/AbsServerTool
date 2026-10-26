# 命令

[首页](Home.zh-CN.md)

## 统计

~~~text
/absserver stats
/absserver activity daily
/absserver activity weekly

/absserver playtime [page]
/absserver playtime total|today|week [page]

/absserver deaths [page]
/absserver deaths total|today|week [page]

/absserver blocks total|today|week [page]
/absserver advancements [page]
~~~

兼容旧用法：

~~~text
/absserver blocks placed total [page]
/absserver blocks placed weekly [page]
~~~

## 排除假人统计

管理员（OP 等级 2）：

~~~text
/absserver excludePlayer
/absserver excludePlayer add <名字或UUID>
/absserver excludePlayer remove <名字或UUID>
~~~

不带参数查看排除名单。新增或移除后立即刷新统计并保存，名字不区分大小写。
历史离线假人若没有被记录过，需要手动添加；名字已不在 usercache.json 时使用 UUID。
同一玩家的名字项与 UUID 项相互独立，恢复统计时需要分别移除。

## 性能分析

管理员：

~~~text
/absserver lag profile [seconds]
/absserver lag status
/absserver lag stop
/absserver lag last
~~~

默认 30 秒，当前默认允许 1～3600 秒。

## 猪灵优化

管理员：

~~~text
/absserver piglinOptimization
/absserver piglinOptimization true|false
/absserver piglinOptimization limit <count>
~~~

默认关闭，阈值 100。

## QQ 播报

管理员根命令：

~~~text
/absserverbot status
/absserverbot url <OneBot HTTP root>
/absserverbot token <token>
/absserverbot cleartoken
/absserverbot group add <id>
/absserverbot group remove <id>
/absserverbot time <hour> <minute>
/absserverbot timezone <ZoneId>
/absserverbot enable
/absserverbot disable
/absserverbot send
~~~

Token 建议从服务端控制台输入，避免进入客户端命令历史。
