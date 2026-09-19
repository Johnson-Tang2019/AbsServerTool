package abyssredemption.stats;

import java.util.UUID;

public record PlayerStatSnapshot(UUID uuid, long playTimeTicks, long deaths, long totalBlocksPlaced) {}
