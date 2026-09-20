package abyssredemption.statistics;

public enum StatisticsLeaderboardType {
    TOTAL_PLAYTIME(0), TODAY_PLAYTIME(1), WEEK_PLAYTIME(2),
    TOTAL_DEATHS(10), TODAY_DEATHS(11), WEEK_DEATHS(12),
    TOTAL_PLACEMENTS(20), TODAY_PLACEMENTS(21), WEEK_PLACEMENTS(22);
    private final int networkId;
    StatisticsLeaderboardType(int networkId) { this.networkId = networkId; }
    public int networkId() { return networkId; }
    public static StatisticsLeaderboardType fromNetworkId(int id) { for (var value : values()) if (value.networkId == id) return value; return null; }
}
