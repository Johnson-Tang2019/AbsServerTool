package abyssredemption.neoforge;

import abyssredemption.network.NetworkRateLimiter;
import abyssredemption.network.ProtocolConstants;

import abyssredemption.config.ConfigManager;
import abyssredemption.network.payload.*;
import abyssredemption.snapshot.SnapshotStore;
import abyssredemption.statistics.StatisticsLeaderboardType;
import abyssredemption.statistics.StatisticsService;
import abyssredemption.stats.LeaderboardEntry;
import abyssredemption.stats.LeaderboardPage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AbsServerNetworkingNeoForge {
    private static final NetworkRateLimiter RATE_LIMITER = new NetworkRateLimiter();
    private static final StatisticsService STATISTICS = new StatisticsService();
    private AbsServerNetworkingNeoForge() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2").optional();
        registrar.playToServer(HelloRequestPayload.TYPE, HelloRequestPayload.CODEC, (payload, context) -> handleHello(payload, context));
        registrar.playToServer(LeaderboardRequestPayload.TYPE, LeaderboardRequestPayload.CODEC, (payload, context) -> handleLeaderboard(payload, context));
        registrar.playToServer(OverviewRequestPayload.TYPE, OverviewRequestPayload.CODEC, (payload, context) -> handleOverview(payload, context));
        registrar.playToServer(TrendRequestPayload.TYPE, TrendRequestPayload.CODEC, (payload, context) -> handleTrend(payload, context));
        registrar.playToClient(HelloResponsePayload.TYPE, HelloResponsePayload.CODEC);
        registrar.playToClient(LeaderboardResponsePayload.TYPE, LeaderboardResponsePayload.CODEC);
        registrar.playToClient(StatsErrorPayload.TYPE, StatsErrorPayload.CODEC);
        registrar.playToClient(OverviewResponsePayload.TYPE, OverviewResponsePayload.CODEC);
        registrar.playToClient(TrendResponsePayload.TYPE, TrendResponsePayload.CODEC);
    }

    private static net.minecraft.server.MinecraftServer server(IPayloadContext context) { return ((ServerPlayer) context.player()).level().getServer(); }
    private static void handleHello(HelloRequestPayload payload, IPayloadContext context) { context.enqueueWork(() -> { if (payload.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) { context.reply(new StatsErrorPayload(2, 0, 1, "Unsupported protocol")); return; } var config = ConfigManager.get(); var today = SnapshotStore.getOrCreateTodayBaseline(server(context)); context.reply(new HelloResponsePayload(2, "0.2.4", ProtocolConstants.ALL_CAPABILITIES, config.leaderboard().pageSize(), today.dateKey(), config.snapshot().timezone(), today.partial())); }); }
    private static void handleLeaderboard(LeaderboardRequestPayload payload, IPayloadContext context) { context.enqueueWork(() -> { if (!valid(payload.protocolVersion(), context, payload.requestId())) return; if (!allow(context)) return; var type = StatisticsLeaderboardType.fromNetworkId(payload.leaderboardType()); if (type == null) { error(context, payload.requestId(), 3, "Unsupported board"); return; } if (payload.page() < 1) { error(context, payload.requestId(), 2, "Invalid page"); return; } var page = page(server(context), type, payload.page()); if (payload.page() > page.totalPages()) { error(context, payload.requestId(), 2, "Invalid page"); return; } var entries = new java.util.ArrayList<LeaderboardResponsePayload.Entry>(); for (int i = 0; i < page.entries().size(); i++) { var item = page.entries().get(i); entries.add(new LeaderboardResponsePayload.Entry((page.page() - 1) * ConfigManager.get().leaderboard().pageSize() + i + 1, item.uuid(), item.playerName(), item.value(), server(context).getPlayerList().getPlayer(item.uuid()) != null)); } context.reply(new LeaderboardResponsePayload(2, payload.requestId(), type.networkId(), page.page(), page.totalPages(), page.totalEntries(), System.currentTimeMillis(), "原版 Statistics / 快照差分", entries)); }); }
    private static void handleOverview(OverviewRequestPayload payload, IPayloadContext context) { context.enqueueWork(() -> { if (!valid(payload.protocolVersion(), context, payload.requestId()) || !allow(context)) return; context.reply(OverviewResponsePayload.from(payload.requestId(), STATISTICS.getOverview(server(context)))); }); }
    private static void handleTrend(TrendRequestPayload payload, IPayloadContext context) { context.enqueueWork(() -> { if (!valid(payload.protocolVersion(), context, payload.requestId()) || !allow(context)) return; if ((payload.periodType() == 0 && (payload.count() < 1 || payload.count() > 90)) || (payload.periodType() == 1 && (payload.count() < 1 || payload.count() > 26))) { error(context, payload.requestId(), 2, "Invalid trend count"); return; } if (payload.periodType() != 0 && payload.periodType() != 1) { error(context, payload.requestId(), 3, "Unsupported trend"); return; } context.reply(payload.periodType() == 0 ? TrendResponsePayload.daily(payload.requestId(), STATISTICS.getDailyMetrics(server(context), payload.count())) : TrendResponsePayload.weekly(payload.requestId(), STATISTICS.getWeeklyMetrics(server(context), payload.count()))); }); }
    private static boolean valid(int version, IPayloadContext context, long requestId) { if (version != ProtocolConstants.PROTOCOL_VERSION) { error(context, requestId, 1, "Unsupported protocol"); return false; } return true; }
    private static boolean allow(IPayloadContext context) { if (!RATE_LIMITER.allow(context.player().getUUID(), ConfigManager.get().network().minimumRequestIntervalMillis())) { error(context, 0, 4, "Rate limited"); return false; } return true; }
    private static void error(IPayloadContext context, long requestId, int code, String message) { context.reply(new StatsErrorPayload(2, requestId, code, message)); }
    private static LeaderboardPage page(net.minecraft.server.MinecraftServer server, StatisticsLeaderboardType type, int page) { var resolver = new abyssredemption.player.PlayerNameResolver(); var all = STATISTICS.getLeaderboardValues(server, type).entrySet().stream().filter(e -> ConfigManager.get().leaderboard().includeZeroValues() || e.getValue() > 0).map(e -> new LeaderboardEntry(e.getKey(), resolver.resolve(server, e.getKey()), e.getValue())).sorted(java.util.Comparator.comparingLong(LeaderboardEntry::value).reversed().thenComparing(LeaderboardEntry::playerName, String.CASE_INSENSITIVE_ORDER).thenComparing(e -> e.uuid().toString())).toList(); int size = Math.max(1, Math.min(100, ConfigManager.get().leaderboard().pageSize())); int pages = Math.max(1, (all.size() + size - 1) / size); int from = Math.min((Math.max(1, page) - 1) * size, all.size()); return new LeaderboardPage(all.subList(from, Math.min(from + size, all.size())), Math.max(1, page), pages, all.size()); }
}
