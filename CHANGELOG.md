# Changelog

## 0.3.0

- Add Fabric and NeoForge 26.2 server artifacts using a shared statistics core.
- Add NeoForge command, lifecycle, snapshot, and protocol-v2 integration.

## 0.2.4

- Exclude online Carpet `EntityPlayerMPFake` instances from all statistics.
- Detection uses the Carpet fake-player entity type, not player-name prefixes.

## 0.2.3

- Leaderboards include all players with loaded vanilla statistics, including players currently offline.
- Offline names are resolved from the server user cache when available.

## 0.2.2

- Leaderboards now include only currently online players; offline players remain available for historical snapshot calculations.

## [0.2.0] - 2026-09-20

### Changed

- Migrated placement statistics to vanilla `minecraft:used` BlockItem statistics.
- Removed the custom placement statistic and placement Mixin.
- Added persistent daily vanilla-stat snapshots, DAU/WAU, daily/weekly metrics, and V2 networking.
- Added Overview, Trend, and nine time-range leaderboard types.

## [0.1.1] - 2026-09-19

### Added

- Persistent total and ISO-week block-placement leaderboards.
- Fabric 26.2 custom statistics, Global Data Attachment storage, and retention.
- Optional protocol v1 networking for the NeoForge AbsMod client.
- Request validation, rate limiting, configuration compatibility, and FakePlayer filtering.

## [0.1.0] - 2026-09-17

### Added

- Server-side playtime and death leaderboards.
- UUID-based offline statistics reading with online-player override.
- Pagination, sorting, caching, malformed-file handling, and fallback player names.
- Minimal successful block-placement hook and placement service scaffold.
