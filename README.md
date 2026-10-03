# AbsServerTool

AbsServerTool 0.3.5-SNAPSHOT provides server-side vanilla statistics, daily snapshots,
DAU/WAU, trends, explicit performance profiling, and protocol v2 for Minecraft 26.2.

The project now builds two server artifacts:

- Fabric: `26.2Fabric/build/libs/absservertool-fabric-*.jar`
- NeoForge: `26.2NeoForge/build/libs/absservertool-neoforge-*.jar`

Both loaders share the statistics and snapshot core. Loader-specific entrypoint,
network registration, commands, and lifecycle integration are provided by each
platform module.

## Server Shared Xaero Waypoints

AbsServerTool now provides an optional shared-waypoint server capability for
AbsTool clients. The server owns the waypoint dataset and stores it inside the
world save at `data/absservertool_shared_waypoints.json`. Server operators meeting
the configured permission level (default 2) can publish, update, and delete waypoints; ordinary
players receive read-only snapshots and live updates. The default limit is 256
waypoints. Older clients and players without AbsTool continue to use the server
normally. Xaero is **not** required on the server. This feature transfers only
waypoints, never Xaero map tiles or explored areas.

When a player's protocol v2 Hello request is accepted, the server console
logs the player name and UUID once for that connection. Clients that do not
send a compatible Hello do not produce this protocol notice.

Settings are in `config/absservertool.json` under `waypoints`: `enabled`,
`managePermissionLevel` (2–4), `maxWaypoints`, `maxNameLength`, and
`maxSymbolLength`. Existing configurations without this section use defaults.
The client-side Xaero integration belongs to AbsTool and is not part of this
server repository. See [the protocol and persistence design](doc/SHARED_WAYPOINTS.md).

## Server performance profiler

The profiler is idle until a permission-level-2 operator or the console runs
`/absserver lag profile [seconds]` (30 seconds by default). Use
`/absserver lag status`, `/absserver lag stop`, and `/absserver lag last` to
inspect or end a session. A report is finalized automatically at the requested
duration; only the most recent report is kept in memory, and a restart clears
it. Configure bounds and sampling limits under `performanceProfiler` in
`config/absservertool.json`.

Reports include tick percentiles, per-dimension time, instrumented chunk and
entity/block-entity timings, chunk loads, player proximity associations, and
slow ticks. Player associations describe overlapping simulation areas, **not
causation**. Instrumented timings are diagnostic estimates, not a JVM CPU
flamegraph. Protocol v2 advertises capability bit 9 and offers admin-only
start/status/stop and paged report requests; older clients remain supported.
An AbsMod GUI needs a separate client update to consume these packets.

AbsServerTool is a server-side Fabric/NeoForge 26.2 mod for vanilla statistics,
snapshots, trends, and player leaderboards.

Implemented in v0.2.0:

- `/absserver playtime [page]` reads vanilla `minecraft:play_time`.
- `/absserver deaths [page]` reads vanilla `minecraft:deaths`.
- `/absserver advancements [page]` ranks known players by completed, displayable advancements, including players with zero completions.
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

Carpet fake players currently online are excluded from all player statistics, including the online count. Their UUIDs are recorded in `config/absservertool-excluded-players.json`, so their saved stats and historical snapshots stay excluded after logout. Fake players that have never appeared while this version is running cannot be identified reliably from a vanilla stats file alone; add their UUIDs or names to the file's `uuids` or `names` list and restart the server. Do not add real player identities. Existing snapshot files are filtered when read, not deleted. The advancements leaderboard reads the world's vanilla `players/advancements` files and the live progress of online players; recipe and other non-display advancements are not counted.

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
