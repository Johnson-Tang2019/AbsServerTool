# 猪灵交易机堆叠优化（26.3）

默认关闭。不需要 Carpet、Carpet-Igny-Addition 或其他新增依赖，Fabric/NeoForge 共用实现。26.2 维护分支不变。

管理员（OP 2 及以上）或控制台使用：

```mcfunction
/absserver piglinOptimization
/absserver piglinOptimization true
/absserver piglinOptimization false
/absserver piglinOptimization limit 100
```

设置立即保存到 `config/absservertool.json` 的 `piglinOptimization` 节点；默认 `enabled=false`、`entityLimit=100`，阈值范围 1–10000。老配置缺少节点时使用默认值。保存失败不会修改当前运行设置。

依据重叠包围盒内同类猪灵数量判定是否达到阈值（包含自身）；每个猪灵错峰每 100 刻重新检查，开关或阈值变化后下次实体刻立即重新检查。减少数量后最多约 5 秒退出优化。关闭开关会立即停止拦截，不必等计数刷新。

达到阈值后：跳过自主移动、travel、推挤/实体挤压、Brain 感知，以及寻找注视/互动对象；不再拾取非金锭物品。金锭拾取继续经过原版检查，保留交易时间与原版交易掉落表。外力移动（如活塞）不被 `move` 拦截。未达到阈值的猪灵、猪灵蛮兵、僵尸猪灵及其他实体不受影响。

**行为变化提示：** 上游优化不只是节省运算；它会关闭堆叠猪灵的自主移动、部分感知和挤压计算，包括正常挤压伤害路径，且感知记忆可能暂时陈旧。因此只建议用于受控的固定猪灵交易机，不建议在自然活动的猪灵群体上开启。不会直接加速交易，也不承诺固定性能提升比例。

## 来源和许可

移植自 Carpet-Igny-Addition `720e516803f3096e132da3562357dc5bd1c22aef` 的 MIT 许可 `mixins/optimizations` 文件（Melationin，2025），不是其其他 LGPL 代码。保留版权及完整许可，打包在 `META-INF/licenses/piglin-optimization-MIT.txt`。

上游：https://github.com/liuyuexiaoyu1/Carpet-Igny-Addition/tree/720e516803f3096e132da3562357dc5bd1c22aef/src/main/java/com/liuyue/igny/mixins/optimizations

区别：只针对猪灵；加入独立配置和管理员指令；关闭或改变设置时不使用陈旧的缓存状态；其他实体不受原上游 `isPlayerHoldingLovedItem` 全局计数逻辑影响。

## 验证

`./gradlew build :fabric:runPiglinRegression :neoforge:runPiglinRegression` 编译双加载器并分别启动不含 Carpet 的隔离测试服务端。两端使用同一套场景，测试权限、阈值、孤立猪灵和其他实体隔离、自主/外力移动、自然金锭拾取与完整交易掉落、关闭恢复及配置保存。测试源集不进入发行 JAR。运行环境分别为 `26.3Fabric/run/piglin-regression`、`26.3NeoForge/run/piglin-regression`，只监听 `127.0.0.1:25572`、`127.0.0.1:25573`，测试结束自动停止。构建和发布流程均执行两端交易验收。
