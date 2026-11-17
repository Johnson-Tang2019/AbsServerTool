# AbsServerTool updates (Minecraft 26.3)

The Fabric and NeoForge servers check the official GitHub Releases feed at startup and every six hours. The updater only manages AbsServerTool itself, and only if it is loaded from a standalone JAR directly in this server's `mods` directory.

- `/absserver update`: view status.
- `/absserver update check`: check now.
- `/absserver update install`: download and verify the update currently shown by the checker.

Use the server console or an operator with permission level 2. Installation is staged until a normal shutdown. **Wait for the installation log before starting the server again**, especially if your hosting panel automatically restarts the server. The updater never stops or restarts the server.

Settings are in `config/absservertool-update/settings.properties` and are read at startup:

```properties
autoCheck=true
channel=beta
autoInstall=false
```

The Beta channel accepts stable versions and `vX.Y.Z-beta.N` releases; use `channel=stable` for stable only. Set `autoInstall=true` to stage compatible updates after a successful check. Drafts, older versions, mismatched loaders or Minecraft versions, and releases without a SHA-256 digest are skipped.

The detached installer waits for the original Java process to exit, backs up the original JAR and replaces it atomically under the existing filename. The log and `.backup` file are in the `config/absservertool-update/install-*` directory printed on shutdown. To roll back, stop the server and copy that backup over the installed JAR, retaining its `.jar` filename.

Some hosting panels terminate all child processes on shutdown. If the log does not report success, verify the installed JAR and use the official release download for manual installation. Do not treat a staged download as an installed update.
