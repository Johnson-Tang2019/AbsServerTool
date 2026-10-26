# 安装与版本

[首页](Home.zh-CN.md)

## Minecraft 26.3

当前 `main` 为 AbsServerTool **0.3.9**，同时构建 Fabric 与 NeoForge 服务端 JAR。

- Java 25 开发/构建环境。
- Fabric 构建基线：Loader 0.19.5、Fabric API 0.161.0+26.3。
- Fabric 元数据要求 Minecraft 26.3.x、Fabric API >= 0.161.0。
- NeoForge 构建基线：26.3.0.23-beta。

只把对应加载器和 Minecraft 版本的 JAR 放入服务器 `mods/`。

## Minecraft 26.2

26.2 位于 `codex/26.2-maintenance`。不要把 26.3 主线 JAR 放进 26.2 服务器。

## 客户端

客户端不需要安装 AbsServerTool。需要图形化统计、性能报告或共享标点时，可安装客户端 `abstoolmod`（Ab's Tool）。

安装后可先执行：

~~~text
/absserver stats
~~~

确认核心统计命令已经加载。
