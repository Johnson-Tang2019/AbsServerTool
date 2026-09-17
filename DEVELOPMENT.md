# Development Notes

The project follows the source-of-truth rules in `doc/AbsServerTool_DEVELOPMENT_v2.md`:

- UUID is the identity key; names are display data only.
- Vanilla Statistics remain authoritative for play time and deaths.
- The command layer does not read files or perform sorting.
- The only Mixin is the thin successful `BlockItem` placement hook.
- No client source set or client-only class is used.

The Fabric 26.2 mappings used by the build must be verified before locking the placement Mixin and Data Attachments implementation. Method names around `BlockItem#placeBlock` and global attachment registration are version-sensitive.
