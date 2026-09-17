package abyssredemption.stats;

import java.util.UUID;

public record LeaderboardEntry(UUID uuid, String playerName, long value) {}
