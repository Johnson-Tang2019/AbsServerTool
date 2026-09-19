# Development Notes

The project follows the source-of-truth rules in `doc/AbsServerTool_DEVELOPMENT_v2.md`:

- UUID is the identity key; names are display data only.
- Vanilla Statistics remain authoritative for play time and deaths.
- The command layer does not read files or perform sorting.
- The only Mixin is the thin successful `BlockItem` placement hook.
- No client source set or client-only class is used.

The Fabric 26.2 mappings used by the build have been verified for the placement Mixin and Global Data Attachments implementation. The placement hook injects only after a successful `BlockItem` placement; weekly data is copied before each Attachment update.

The server networking layer uses protocol version 1 and fixed payload IDs and field order shared with the NeoForge AbsMod 26.2 client. Runtime game tests still require a local EULA-approved Dedicated Server and should cover placement edge cases, restart persistence, week rollover, and client GUI requests.
