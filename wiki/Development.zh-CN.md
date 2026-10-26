# 开发结构

[首页](Home.zh-CN.md)

当前 `main`：Minecraft 26.3 / AbsServerTool 0.3.9。

共享核心：

~~~text
src/main/java/abyssredemption/
  statistics/
  snapshot/
  vanilla/
  performance/
  waypoint/
  qq/
  optimization/
  network/
  command/
  config/
  player/
~~~

加载器适配：

~~~text
26.3Fabric/
26.3NeoForge/
~~~

Minecraft 26.2 在 `codex/26.2-maintenance`。

使用 Java 25。仓库含 profiler 与 piglin dedicated-server regression 源集，测试 fixture 不进入生产 JAR。

写开发文档前的事实来源顺序：

1. 当前源码。
2. `gradle.properties`。
3. `CHANGELOG.md`。
4. README 与功能独立文档。

不要用旧开发草案覆盖已经实现的代码。
