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
