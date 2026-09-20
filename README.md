# AbsServerTool

AbsServerTool is a server-side Fabric 26.2 mod for lightweight player leaderboards.

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
- Fabric protocol v2 provides Overview, Trend, and leaderboard requests for the NeoForge AbsMod client.
- The server remains usable by clients without AbsMod.

Daily vanilla-stat snapshots are stored with Fabric 26.2 Global Data Attachments and are retained according to `retentionDays`.

See [doc/AbsServerTool_DEVELOPMENT_v2.md](doc/AbsServerTool_DEVELOPMENT_v2.md) for the complete product boundary and acceptance plan.
