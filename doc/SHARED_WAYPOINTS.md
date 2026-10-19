# Shared waypoints: server contract

This is the AbsServerTool side of the Xaero shared-waypoint feature. Minecraft
26.2 Fabric and NeoForge use the same model and protocol. Xaero is absent from
the server classpath; AbsTool is an optional client companion.

## Authority and storage

`SharedWaypoint` contains `id` (server-issued UUID), `name`, `dimensionId`
(Minecraft registry ID), `x/y/z`, RGB `color`, `symbol`, `enabled`, `createdBy`,
`createdAt`, `updatedAt`, and per-waypoint `revision`. The server also maintains
`datasetRevision`. The world-owned JSON file is
`<world>/data/absservertool_shared_waypoints.json`, schema version 1. A temporary
file is written before atomic replacement when supported. A failed read or
write refuses mutations rather than replacing the previous data. Malformed
individual entries are skipped during read and logged. World backups include
this file; global config contains limits only.

## Capability and wire protocol

Existing Hello v2 is unchanged. The server sets `CAP_SHARED_WAYPOINTS = 1<<8`
when the feature is enabled. An AbsTool client must not send waypoint requests
unless this bit is present. All payload IDs have namespace `absservertool`:

| Direction | ID | Fields |
| --- | --- | --- |
| C2S | `shared_waypoint_sync_request` | protocolVersion, requestId |
| C2S | `shared_waypoint_mutation_request` | protocolVersion, requestId, operation, optional waypointId, expectedRevision, name, dimensionId, x, y, z, color, symbol, enabled |
| S2C | `shared_waypoint_permissions` | protocolVersion, canRead, canManage |
| S2C | `shared_waypoint_snapshot` | protocolVersion, requestId, datasetRevision, complete waypoint list |
| S2C | `shared_waypoint_delta` | protocolVersion, datasetRevision, operation, waypointId, waypointRevision, optional full waypoint |
| S2C | `shared_waypoint_mutation_result` | protocolVersion, requestId, status, optional waypointId, waypointRevision, datasetRevision |
| S2C | `shared_waypoint_error` | protocolVersion, requestId, code, message |

Operation `1` is create, `2` update, and `3` delete. Create ignores the supplied
ID and assigns a new UUID. Update and delete require a matching ID and
`expectedRevision`. Delete ignores the content fields (send empty strings).
Each successful mutation increments `datasetRevision`; update increments the
waypoint revision, and delete reports the previous revision plus one. A
successful disk commit precedes delta broadcast. The initial snapshot and
permissions follow Hello for clients advertising the clientbound channels.
Explicit sync requests support re-synchronization after a revision gap.

Status codes: `0 SUCCESS`, `1 PERMISSION_DENIED`, `2 NOT_FOUND`,
`3 REVISION_CONFLICT`, `4 INVALID_DATA`, `5 LIMIT_REACHED`, `6 RATE_LIMITED`,
`7 DISABLED`, `8 STORAGE_ERROR`. These numeric values are stable. Request IDs
are echoed in responses. A failed mutation never emits a delta. Clients must
replace their session dataset from a snapshot and request a fresh snapshot on
an out-of-order/missing delta; disconnect clears the client session state.
Error codes are `1 protocol`, `2 disabled`, `3 storage`, and `4 rate limit`.

## Permission and validation

Each write rechecks the current server permission set. Configured level 2 maps
to Minecraft 26.2 `GAMEMASTERS`, 3 to `ADMINS`, and 4 to `OWNERS`. Client
permission packets are UI hints only. Name and symbol lengths, controls,
dimension registry ID and loaded level, coordinates, RGB color, revision,
dataset limit, and mutation rate are validated server-side. The server ignores
client-supplied creator, timestamps, and revision values other than the
expected revision. Sync is limited to one request per 500 ms per player;
mutations to one request per 1000 ms per player. Broadcast checks each client's
channel support independently, so older clients do not block other players.

## Scope and limitations

Only waypoints are synchronized. There is no map tile, exploration, route,
position, team, cross-server, or automatic Nether-coordinate synchronization.
AbsTool must implement the Xaero display/GUI bridge separately. This server
implementation does not edit player Xaero files or require Xaero on the server.

## Minecraft 26.3 category names (0.3.8)

AbsTool 0.5.0-beta.8 can read and rename the sixteen color categories using two
optional protocol-v2 channels. Existing waypoint payloads remain unchanged.

- C2S absservertool:waypoint_categories_request: version, requestId, edit flag, color, expectedRevision, name.
- S2C absservertool:waypoint_categories_snapshot: version, requestId, status, revision, color/name map.

A request with edit=false reads the current names. Rename requests require the
existing waypoint management permission, enabled waypoints, a matching category
revision, color 0..15, and a name of at most 32 characters without control or
formatting codes. Blank names restore the default label. Requests are limited
to one per player per 500 ms. Names are saved separately in the world's
data/absservertool_waypoint_categories.json using temporary-file replacement.
Only committed changes are broadcast. Old clients need not register either
channel; old servers continue to support ordinary waypoints in the new client.
Both loaders run verifyWaypointCategories as part of build/check for permission,
conflict, validation, restart persistence and corrupt-store preservation checks.
