# 联动

[首页](Home.zh-CN.md)

## `abstoolmod`

Protocol v2 的可选客户端 `abstoolmod`（Ab's Tool）可展示统计总览、趋势、排行榜、完成进度、性能报告和共享标点。没有客户端 companion 的玩家仍可正常进服。

## Xaero 共享标点

服务器权威数据：

~~~text
<world>/data/absservertool_shared_waypoints.json
~~~

默认管理权限等级 2、最多 256 个标点、名称最长 64、symbol 最长 8。服务端无需安装 Xaero。

## NapCat QQ

配置文件：

~~~text
config/absservertool_qq.json
~~~

默认时区 `Asia/Shanghai`，默认计划 20:00，功能默认关闭。必须填 OneBot HTTP 服务端地址，不是 NapCat WebUI 端口。

## 猪灵优化

~~~text
/absserver piglinOptimization true|false
/absserver piglinOptimization limit <count>
~~~

默认关闭，默认阈值 100。
