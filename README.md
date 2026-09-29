# AbsServerTool

AbsServerTool 0.3.1 provides server-side vanilla statistics, daily snapshots,
DAU/WAU, trends, and protocol v2 for Minecraft 26.2.

The project now builds two server artifacts:

- Fabric: `26.2Fabric/build/libs/absservertool-fabric-*.jar`
- NeoForge: `26.2NeoForge/build/libs/absservertool-neoforge-*.jar`

Both loaders share the statistics and snapshot core. Loader-specific entrypoint,
network registration, commands, and lifecycle integration are provided by each
platform module.

AbsServerTool is a server-side Fabric/NeoForge 26.2 mod for vanilla statistics,
snapshots, trends, and player leaderboards.

Implemented in v0.2.0:

- `/absserver playtime [page]` reads vanilla `minecraft:play_time`.
- `/absserver deaths [page]` reads vanilla `minecraft:deaths`.
- Offline stats are read from the configured world's `stats` directory.
- Online player statistics override disk data.
- Results are cached for 30 seconds by default.
- Malformed files and unknown player names are handled without stopping the server.
- `/absserver stats` reports current online/known players, DAU/WAU and daily/weekly deltas.
- `/absserver playtime|deaths|blocks total|today|week list [page]` exposes time-range leaderboards.
- Placement values are aggregated from vanilla `minecraft:used` BlockItem statistics.
- Both platform builds provide protocol v2 Overview, Trend, and leaderboard requests for the NeoForge AbsMod client.
- The server remains usable by clients without AbsMod.

Daily vanilla-stat snapshots are stored in the server world's `data` directory and are retained according to `retentionDays` on both loaders.

## QQ group reports via NapCat

The server mod can publish its live statistics to QQ groups without RCON, NoneBot,
Python, or changes to `server.properties`. It uses NapCat's OneBot v11 HTTP
`send_group_msg` API. Configure an **HTTP server** in NapCat and ensure the
Minecraft server can reach it. The NapCat WebUI port is not the OneBot HTTP port.

Only server admins can configure the bridge. Run these Minecraft commands after
installing the Fabric or NeoForge JAR:

```text
/absserverbot url http://127.0.0.1:3000
/absserverbot token YOUR_NAPCAT_HTTP_TOKEN
/absserverbot group add 123456789
/absserverbot time 20 0
/absserverbot timezone Asia/Shanghai
/absserverbot enable
/absserverbot send
/absserverbot status
```

Use the actual NapCat host and OneBot HTTP port. If NapCat has no HTTP token,
omit the `token` command. `cleartoken`, `group remove <id>`, and `disable` are
also available. `send` triggers a manual report; the automatic report runs once
per day at the configured time. Configuration is saved to
`config/absservertool_qq.json` by these commands, so no manual file editing is
required. A failed scheduled send is logged and can be retried with `send`.
The token is never shown by `status`, but entering it as a game command may
leave it in client command history; use the server console if possible.

See [doc/AbsServerTool_DEVELOPMENT_v2.md](doc/AbsServerTool_DEVELOPMENT_v2.md) for the complete product boundary and acceptance plan.
