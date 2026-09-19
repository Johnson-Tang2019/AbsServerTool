package abyssredemption.stats;

public enum LeaderboardType {
    PLAYTIME(0), DEATHS(1), BLOCKS_TOTAL(2), BLOCKS_WEEKLY(3);
    private final int networkId;
    LeaderboardType(int networkId) { this.networkId = networkId; }
    public int networkId() { return networkId; }
    public static LeaderboardType fromNetworkId(int id) {
        for (LeaderboardType value : values()) if (value.networkId == id) return value;
        return null;
    }
}
