package abyssredemption.stats;

import java.util.List;

public record LeaderboardPage(List<LeaderboardEntry> entries, int page, int totalPages, int totalEntries) {}
