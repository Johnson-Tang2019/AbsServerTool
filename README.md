# AbsServerTool

AbsServerTool is a server-side Fabric 26.2 mod for lightweight player leaderboards.

Implemented in v0.1.0:

- `/absserver playtime [page]` reads vanilla `minecraft:play_time`.
- `/absserver deaths [page]` reads vanilla `minecraft:deaths`.
- Offline stats are read from the configured world's `stats` directory.
- Online player statistics override disk data.
- Results are cached for 30 seconds by default.
- Malformed files and unknown player names are handled without stopping the server.

The block-placement service and its minimal successful-placement hook are present as the extension point for the persistent total and weekly placement leaderboards. Their persistence should be completed against the exact Data Attachments API exposed by the selected Fabric 26.2 mappings before release.

See [doc/AbsServerTool_DEVELOPMENT_v2.md](doc/AbsServerTool_DEVELOPMENT_v2.md) for the complete product boundary and acceptance plan.
