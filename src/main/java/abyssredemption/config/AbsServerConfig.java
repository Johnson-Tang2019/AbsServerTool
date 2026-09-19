package abyssredemption.config;

public record AbsServerConfig(Leaderboard leaderboard, Display display, Placements placements, Network network) {
    public record Leaderboard(int pageSize, int cacheTtlSeconds, boolean includeZeroValues) {}
    public record Display(boolean showUuidOnHover, boolean enableClickablePagination) {}
    public record Placements(boolean enabled, boolean countCreativePlacements, boolean includeFakePlayers,
                             String timezone, String weekStartsOn, int weeklyRetentionWeeks) {}
    public record Network(boolean enabled, long minimumRequestIntervalMillis) {}

    public static AbsServerConfig defaults() {
        return new AbsServerConfig(new Leaderboard(10, 30, false), new Display(true, true),
                new Placements(true, true, false, "SYSTEM", "MONDAY", 12), new Network(true, 500));
    }
}
