# AbsServerTool

AbsServerTool 0.2.4 provides server-side vanilla statistics, daily snapshots,
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

See [doc/AbsServerTool_DEVELOPMENT_v2.md](doc/AbsServerTool_DEVELOPMENT_v2.md) for the complete product boundary and acceptance plan.
