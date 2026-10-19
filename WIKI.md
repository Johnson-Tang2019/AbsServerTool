# AbsServerTool Wiki

Wiki 源文件维护在 [wiki](wiki/) 目录。

- [简体中文首页](wiki/Home.zh-CN.md)
- [English Home](wiki/Home.md)

`.github/workflows/wiki-sync.yml` 会在 `wiki/**` 更新后同步到 GitHub Wiki，也可在 Actions 中手动运行。

如果第一次同步提示 Wiki 尚未初始化：先在 **Settings -> General -> Features** 开启 Wikis，在仓库 Wiki 标签中手工创建一次 Home 页面，然后重新运行 **Wiki Sync**。
