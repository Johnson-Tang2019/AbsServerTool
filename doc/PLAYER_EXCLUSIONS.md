# Player statistics exclusions (26.3)

Carpet fake players are excluded while online. Observed non-shadow fake UUIDs are
saved in `config/absservertool-excluded-players.json` and stay excluded after logout
and restart. The shared PlayerList removal hook also captures bots removed between
polls. No Carpet runtime dependency is added.

Vanilla statistics files do not identify whether an offline player was a Carpet
bot. Existing offline bots that this mod never observed require an explicit entry:

```text
/absserver excludePlayer
/absserver excludePlayer add SleepingBot
/absserver excludePlayer add <UUID>
/absserver excludePlayer remove SleepingBot
/absserver excludePlayer remove <UUID>
```

Commands require OP level 2 and persist immediately. An exclusion applies to all
statistics, advancement rankings and historical snapshot displays without deleting
vanilla player data or snapshot records. Names match case-insensitively using the
online profile or usercache.json; use a UUID when the old name is no longer cached.
UUID and name entries are independent: remove both if both were added. A currently
running fake is always excluded and will be recorded again after manual removal.
Carpet shadows are temporarily excluded but do not automatically blacklist the
real player's UUID. A UUID previously used by a regular bot requires manual removal
if its owner later joins as a real player.

Manually edited JSON is read at server startup/first use; use the commands for live
changes. An unreadable file is preserved and automatic persistence is refused.
Minecraft 26.2 source and maintenance branch are not part of this change.
