package abyssredemption.stats;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.util.PageUtil;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class LeaderboardService {
    private final PlayerStatsService stats = new PlayerStatsService();
    private long expiresAt;
    private Map<UUID, PlayerStatSnapshot> cached = Map.of();
    private Map<UUID, String> names = Map.of();

    public LeaderboardPage playtime(MinecraftServer server, int page) { return page(server, page, false); }
    public LeaderboardPage deaths(MinecraftServer server, int page) { return page(server, page, true); }
    public LeaderboardPage blocksTotal(MinecraftServer server, int page) { return page(server, page, LeaderboardType.BLOCKS_TOTAL); }
    public LeaderboardPage blocksWeekly(MinecraftServer server, int page) { return page(server, page, LeaderboardType.BLOCKS_WEEKLY); }

    private LeaderboardPage page(MinecraftServer server, int page, boolean death) {
        refresh(server);
        boolean includeZero = ConfigManager.get().leaderboard().includeZeroValues();
        List<LeaderboardEntry> all = cached.values().stream()
                .map(snapshot -> new LeaderboardEntry(snapshot.uuid(), names.get(snapshot.uuid()), death ? snapshot.deaths() : snapshot.playTimeTicks()))
                .filter(entry -> includeZero || entry.value() > 0)
                .sorted(Comparator.comparingLong(LeaderboardEntry::value).reversed()
                        .thenComparing(LeaderboardEntry::playerName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.uuid().toString()))
                .toList();
        int size = Math.max(1, ConfigManager.get().leaderboard().pageSize());
        int totalPages = PageUtil.totalPages(all.size(), size);
        int actual = PageUtil.clamp(page, totalPages);
        int from = Math.min((actual - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return new LeaderboardPage(all.subList(from, to), actual, totalPages, all.size());
    }

    private LeaderboardPage page(MinecraftServer server, int page, LeaderboardType type) {
        refresh(server);
        boolean includeZero = ConfigManager.get().leaderboard().includeZeroValues();
        List<LeaderboardEntry> all = cached.values().stream()
                .map(snapshot -> new LeaderboardEntry(snapshot.uuid(), names.get(snapshot.uuid()),
                        type == LeaderboardType.BLOCKS_TOTAL ? snapshot.totalBlocksPlaced() : AbsServerTool.PLACEMENTS.weekly(server, snapshot.uuid())))
                .filter(entry -> includeZero || entry.value() > 0)
                .sorted(Comparator.comparingLong(LeaderboardEntry::value).reversed()
                        .thenComparing(LeaderboardEntry::playerName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.uuid().toString()))
                .toList();
        return slice(all, page);
    }

    private LeaderboardPage slice(List<LeaderboardEntry> all, int page) {
        int size = Math.max(1, ConfigManager.get().leaderboard().pageSize());
        int totalPages = PageUtil.totalPages(all.size(), size);
        int actual = PageUtil.clamp(page, totalPages);
        int from = Math.min((actual - 1) * size, all.size());
        return new LeaderboardPage(all.subList(from, Math.min(from + size, all.size())), actual, totalPages, all.size());
    }

    private void refresh(MinecraftServer server) {
        if (System.currentTimeMillis() < expiresAt) return;
        cached = stats.collectAllStats(server);
        names = stats.names(server, cached);
        expiresAt = System.currentTimeMillis() + ConfigManager.get().leaderboard().cacheTtlSeconds() * 1000L;
    }

    public void invalidateCache() { expiresAt = 0; }
}
