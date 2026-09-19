# AbsServerTool

AbsServerTool is a server-side Fabric 26.2 mod for lightweight player leaderboards.

Implemented in v0.1.0:

- `/absserver playtime [page]` reads vanilla `minecraft:play_time`.
- `/absserver deaths [page]` reads vanilla `minecraft:deaths`.
- Offline stats are read from the configured world's `stats` directory.
- Online player statistics override disk data.
- Results are cached for 30 seconds by default.
- Malformed files and unknown player names are handled without stopping the server.
- `/absserver blocks placed total [page]` reads the persistent custom placement statistic.
- `/absserver blocks placed weekly [page]` reads persistent ISO-week placement data.
- Successful `BlockItem` placement is counted once; commands, world generation and failed placement are not counted.
- Fabric custom payloads provide optional Hello and leaderboard requests for the NeoForge AbsMod client.
- The server remains usable by clients without AbsMod.

Weekly placement data is stored with Fabric 26.2 Global Data Attachments and is retained according to `weeklyRetentionWeeks`.

See [doc/AbsServerTool_DEVELOPMENT_v2.md](doc/AbsServerTool_DEVELOPMENT_v2.md) for the complete product boundary and acceptance plan.
