# AbsServerTool Wiki

[简体中文](Home.zh-CN.md)

Current main line: **AbsServerTool 0.3.9 for Minecraft 26.3**, with Fabric and NeoForge server artifacts. Minecraft 26.2 is maintained on `codex/26.2-maintenance`.

Current features include vanilla-statistics dashboards, daily snapshots, DAU/WAU, trends, advancement completion, an explicit on-demand performance profiler, server-owned shared waypoints, NapCat QQ reports, optional piglin barter optimization, and protocol v2 integration for the client `abstoolmod` (Ab's Tool).

See the Chinese pages in the sidebar for the complete current documentation.

Version 0.3.9 remembers observed Carpet fake players after logout and restart,
including bots removed between statistics polls. Administrators (OP level 2) can
exclude legacy offline bots with `/absserver excludePlayer add <nameOrUuid>`, list
entries with `/absserver excludePlayer`, and remove an entry with
`/absserver excludePlayer remove <nameOrUuid>`. Exclusions apply to rankings,
advancements and historical displays without deleting player data. Names are
case-insensitive; use UUIDs when an offline name is no longer in `usercache.json`.
Offline identities never observed as bots cannot be inferred from vanilla stats.
