# Vanilla Block Placement Statistics Validation

This document records observations from a Minecraft 26.2 dedicated-server test.
AbsServerTool does not modify these statistics and does not add a placement counter.

## Test protocol

Environment requirements:

- Minecraft Java Edition 26.2 dedicated server
- Fabric server with AbsServerTool loaded
- A real player with permission to use `/stats` and the required items

Before each row, run the following command while looking at the test player:

```mcfunction
/stats entity <player> get @s minecraft.used:<item>
```

Replace `<item>` with the registry ID in the table. Record the returned value,
place the requested number of items normally, then run the same command again.
The command output and the corresponding server log should be retained with
the test date and Minecraft/Fabric versions. Do not edit the player's stats
JSON between the two readings.

For each item, record the `minecraft:used/<item>` value before and after placing
the requested amount. The delta is the vanilla result and is not corrected by
the mod.

| Item | Requested | Before | After | Delta | Notes |
|---|---:|---:|---:|---:|---|
| `minecraft:stone` | 10 | pending | pending | pending | |
| `minecraft:oak_planks` | 10 | pending | pending | pending | |
| `minecraft:stone_stairs` | 10 | pending | pending | pending | |
| `minecraft:chest` | 10 | pending | pending | pending | |
| `minecraft:oak_door` | 5 | pending | pending | pending | |
| `minecraft:white_bed` | 5 | pending | pending | pending | |
| `minecraft:oak_sign` | 5 | pending | pending | pending | |

The rows remain `pending` until the corresponding in-game test is performed;
no result is inferred from the implementation.

## Acceptance rule

This document is not considered verified while any row is `pending`. A delta
that differs from the requested count is a vanilla observation and must be
recorded in `Notes`; AbsServerTool must not compensate for it with an event
listener or a custom statistic.
