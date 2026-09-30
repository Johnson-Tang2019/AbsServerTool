package abyssredemption.config;

public record AbsServerConfig(Leaderboard leaderboard, Display display, Placements placements, Network network, Statistics statistics, Snapshot snapshot, Waypoints waypoints) {
    public record Leaderboard(int pageSize, int cacheTtlSeconds, boolean includeZeroValues) {}
    public record Display(boolean showUuidOnHover, boolean enableClickablePagination) {}
    public record Placements(boolean enabled, boolean countCreativePlacements, boolean includeFakePlayers,
                             String timezone, String weekStartsOn, int weeklyRetentionWeeks) {}
    public record Network(boolean enabled, long minimumRequestIntervalMillis) {}
    public record Statistics(boolean includeModdedBlockItems, int currentStatsCacheSeconds) {}
    public record Snapshot(String timezone, int retentionDays) {}
    public record Waypoints(boolean enabled, int managePermissionLevel, int maxWaypoints, int maxNameLength, int maxSymbolLength) {}

    public static AbsServerConfig defaults() {
        return new AbsServerConfig(new Leaderboard(10, 30, false), new Display(true, true),
                new Placements(true, true, false, "SYSTEM", "MONDAY", 12), new Network(true, 500), new Statistics(true, 30), new Snapshot("SYSTEM", 180),
                new Waypoints(true, 2, 256, 64, 8));
    }
}
