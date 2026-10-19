# 故障排查

[首页](Home.zh-CN.md)

## 没有 /absserver

检查 Minecraft 版本、加载器、服务端 `mods/`、Fabric API/NeoForge 依赖与启动日志。

## `abstoolmod` GUI 显示不支持

确认双方 Minecraft 版本匹配、AbsServerTool 已加载、协议为 v2，并且服务端声明对应 capability。性能分析使用 capability bit 9，且请求还要通过管理员权限。

## 今日/本周显示 partial

说明缺少精确时间边界快照，例如首次安装不在 00:00。属于预期保护。

## 假人仍在排行

对历史存档中无法自动识别的 fake player，把名称或 UUID 加入：

~~~text
config/absservertool-excluded-players.json
~~~

## Profiler 不结束

自动结束依赖服务器 Tick。如果空服务器触发 Minecraft empty-pause，Session 会一起暂停。进入服务器或调整空服暂停设置后再分析。

## QQ 不发送

确认 NapCat OneBot HTTP 地址不是 WebUI 端口，服务器能访问该地址、群号已添加、Token 正确，并先运行：

~~~text
/absserverbot send
~~~

## 共享标点不能编辑

默认管理权限为 OP level 2。客户端还必须完成 protocol v2 Hello、权限和权威 snapshot 同步。
