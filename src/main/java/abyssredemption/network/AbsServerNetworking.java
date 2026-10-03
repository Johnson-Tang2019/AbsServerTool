package abyssredemption.network;

import abyssredemption.AbsServerTool;
import abyssredemption.fabric.SharedWaypointNetworkingFabric;
import abyssredemption.config.ConfigManager;
import abyssredemption.performance.network.PerformanceControlPayloads.*;
import abyssredemption.performance.network.PerformanceReportPayloads.*;
import abyssredemption.performance.network.PerformanceRequestService;
import abyssredemption.network.payload.*;
import abyssredemption.stats.LeaderboardEntry;
import abyssredemption.stats.LeaderboardPage;
import abyssredemption.statistics.StatisticsLeaderboardType;
import abyssredemption.stats.LeaderboardEntry;
import abyssredemption.stats.LeaderboardPage;
import abyssredemption.snapshot.SnapshotStore;
import abyssredemption.statistics.StatisticsService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

public final class AbsServerNetworking {
    private static final NetworkRateLimiter RATE_LIMITER = new NetworkRateLimiter();
    private static final StatisticsService STATISTICS = new StatisticsService();
    private AbsServerNetworking() {}
    private static void sendPerformance(net.minecraft.server.level.ServerPlayer player,
                                        net.minecraft.network.protocol.common.custom.CustomPacketPayload response) {
        if (ServerPlayNetworking.canSend(player, response.type())) ServerPlayNetworking.send(player, response);
    }

    public static void initialize() {
        if (!ConfigManager.get().network().enabled()) return;
        PayloadTypeRegistry.serverboundPlay().register(HelloRequestPayload.TYPE, HelloRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LeaderboardRequestPayload.TYPE, LeaderboardRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OverviewRequestPayload.TYPE, OverviewRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TrendRequestPayload.TYPE, TrendRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StartRequest.TYPE, StartRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StatusRequest.TYPE, StatusRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StopRequest.TYPE, StopRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SummaryRequest.TYPE, SummaryRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChunkPageRequest.TYPE, ChunkPageRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PlayerPageRequest.TYPE, PlayerPageRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SlowTickPageRequest.TYPE, SlowTickPageRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChunkDetailRequest.TYPE, ChunkDetailRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HelloResponsePayload.TYPE, HelloResponsePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LeaderboardResponsePayload.TYPE, LeaderboardResponsePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatsErrorPayload.TYPE, StatsErrorPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OverviewResponsePayload.TYPE, OverviewResponsePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrendResponsePayload.TYPE, TrendResponsePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StartResponse.TYPE, StartResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatusResponse.TYPE, StatusResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StopResponse.TYPE, StopResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SummaryResponse.TYPE, SummaryResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ChunkPageResponse.TYPE, ChunkPageResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlayerPageResponse.TYPE, PlayerPageResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlowTickPageResponse.TYPE, SlowTickPageResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ChunkDetailResponse.TYPE, ChunkDetailResponse.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HelloRequestPayload.TYPE, (payload, context) -> handleHello(payload, context));
        ServerPlayNetworking.registerGlobalReceiver(LeaderboardRequestPayload.TYPE, (payload, context) -> handleRequest(payload, context));
        ServerPlayNetworking.registerGlobalReceiver(OverviewRequestPayload.TYPE, (payload, context) -> handleOverview(payload, context));
        ServerPlayNetworking.registerGlobalReceiver(TrendRequestPayload.TYPE, (payload, context) -> handleTrend(payload, context));
        ServerPlayNetworking.registerGlobalReceiver(StartRequest.TYPE, (p,c) -> c.server().execute(() -> PerformanceRequestService.start(c.player(),c.server(),p,response -> sendPerformance(c.player(), response))));
        ServerPlayNetworking.registerGlobalReceiver(StatusRequest.TYPE, (p,c) -> c.server().execute(() -> PerformanceRequestService.status(c.player(),p,response -> sendPerformance(c.player(), response))));
        ServerPlayNetworking.registerGlobalReceiver(StopRequest.TYPE, (p,c) -> c.server().execute(() -> PerformanceRequestService.stop(c.player(),p,response -> sendPerformance(c.player(), response))));
        ServerPlayNetworking.registerGlobalReceiver(SummaryRequest.TYPE, (p,c) -> c.server().execute(() -> PerformanceRequestService.summary(c.player(),p,response -> sendPerformance(c.player(), response))));
        ServerPlayNetworking.registerGlobalReceiver(ChunkPageRequest.TYPE, (p,c) -> c.server().execute(() -> PerformanceRequestService.chunks(c.player(),p,response -> sendPerformance(c.player(), response))));
        ServerPlayNetworking.registerGlobalReceiver(PlayerPageRequest.TYPE, (p,c) -> c.server().execute(() -> PerformanceRequestService.players(c.player(),p,response -> sendPerformance(c.player(), response))));
        ServerPlayNetworking.registerGlobalReceiver(SlowTickPageRequest.TYPE, (p,c) -> c.server().execute(() -> PerformanceRequestService.slowTicks(c.player(),p,response -> sendPerformance(c.player(), response))));
        ServerPlayNetworking.registerGlobalReceiver(ChunkDetailRequest.TYPE, (p,c) -> c.server().execute(() -> PerformanceRequestService.detail(c.player(),p,response -> sendPerformance(c.player(), response))));
        SharedWaypointNetworkingFabric.register();
    }

    private static void handleHello(HelloRequestPayload payload, ServerPlayNetworking.Context context) {
        if (payload.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) {
            context.responseSender().sendPacket(new StatsErrorPayload(ProtocolConstants.PROTOCOL_VERSION, 0, 1, "Unsupported protocol"));
            return;
        }
        var config = ConfigManager.get();
        var today = SnapshotStore.getOrCreateTodayBaseline(context.server());
        int capabilities = ProtocolConstants.ALL_CAPABILITIES | (config.waypoints().enabled() ? ProtocolConstants.CAP_SHARED_WAYPOINTS : 0)
                | (config.performanceProfiler().enabled() ? ProtocolConstants.CAP_PERFORMANCE_PROFILER : 0);
        context.responseSender().sendPacket(new HelloResponsePayload(ProtocolConstants.PROTOCOL_VERSION, "0.3.5", capabilities,
                config.leaderboard().pageSize(), today.dateKey(), config.snapshot().timezone(), today.partial()));
        ProtocolJoinLogger.onSuccessfulHello(context.player());
        SharedWaypointNetworkingFabric.sendInitial(context.player());
    }

    private static void handleRequest(LeaderboardRequestPayload payload, ServerPlayNetworking.Context context) {
        if (payload.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) { sendError(context, payload.requestId(), 1, "Unsupported protocol"); return; }
        if (!RATE_LIMITER.allow(context.player().getUUID(), ConfigManager.get().network().minimumRequestIntervalMillis())) { sendError(context, payload.requestId(), 4, "Rate limited"); return; }
        StatisticsLeaderboardType type = StatisticsLeaderboardType.fromNetworkId(payload.leaderboardType());
        if (type == null) { sendError(context, payload.requestId(), 3, "Unsupported board"); return; }
        if (payload.page() < 1) { sendError(context, payload.requestId(), 2, "Invalid page"); return; }
        LeaderboardPage page = statisticsPage(context.server(), type, payload.page());
        if (payload.page() > page.totalPages()) { sendError(context, payload.requestId(), 2, "Invalid page"); return; }
        var entries = new java.util.ArrayList<LeaderboardResponsePayload.Entry>();
        for (int i = 0; i < page.entries().size(); i++) { var e = page.entries().get(i); entries.add(new LeaderboardResponsePayload.Entry((page.page() - 1) * ConfigManager.get().leaderboard().pageSize() + i + 1, e.uuid(), e.playerName(), e.value(), context.server().getPlayerList().getPlayer(e.uuid()) != null)); }
        String contextLabel = switch (type) {
            case TOTAL_PLAYTIME, TOTAL_DEATHS, TOTAL_PLACEMENTS -> "累计原版 Statistics";
            case TODAY_PLAYTIME, TODAY_DEATHS, TODAY_PLACEMENTS -> "今日快照差分";
            case WEEK_PLAYTIME, WEEK_DEATHS, WEEK_PLACEMENTS -> "本周快照差分";
            case COMPLETED_ADVANCEMENTS -> "已完成进度数";
        };
        context.responseSender().sendPacket(new LeaderboardResponsePayload(ProtocolConstants.PROTOCOL_VERSION, payload.requestId(), type.networkId(), page.page(), page.totalPages(), page.totalEntries(), System.currentTimeMillis(), contextLabel, entries));
    }

    private static LeaderboardPage statisticsPage(net.minecraft.server.MinecraftServer server, StatisticsLeaderboardType type, int page) {
        var resolver = new abyssredemption.player.PlayerNameResolver();
        var all = STATISTICS.getLeaderboardValues(server, type).entrySet().stream()
                .filter(e -> type == StatisticsLeaderboardType.COMPLETED_ADVANCEMENTS || ConfigManager.get().leaderboard().includeZeroValues() || e.getValue() > 0)
                .map(e -> new LeaderboardEntry(e.getKey(), resolver.resolve(server, e.getKey()), e.getValue()))
                .sorted(java.util.Comparator.comparingLong(LeaderboardEntry::value).reversed().thenComparing(LeaderboardEntry::playerName, String.CASE_INSENSITIVE_ORDER).thenComparing(e -> e.uuid().toString())).toList();
        int size = Math.max(1, Math.min(100, ConfigManager.get().leaderboard().pageSize())); int pages = Math.max(1, (all.size() + size - 1) / size); int from = Math.min((Math.max(1, page) - 1) * size, all.size()); return new LeaderboardPage(all.subList(from, Math.min(from + size, all.size())), Math.max(1, page), pages, all.size());
    }

    private static void handleOverview(OverviewRequestPayload payload, ServerPlayNetworking.Context context) {
        if (payload.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) { sendError(context, payload.requestId(), 1, "Unsupported protocol"); return; }
        if (!RATE_LIMITER.allow(context.player().getUUID(), ConfigManager.get().network().minimumRequestIntervalMillis())) { sendError(context, payload.requestId(), 4, "Rate limited"); return; }
        context.responseSender().sendPacket(OverviewResponsePayload.from(payload.requestId(), STATISTICS.getOverview(context.server())));
    }

    private static void handleTrend(TrendRequestPayload payload, ServerPlayNetworking.Context context) {
        if (payload.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) { sendError(context, payload.requestId(), 1, "Unsupported protocol"); return; }
        if (!RATE_LIMITER.allow(context.player().getUUID(), ConfigManager.get().network().minimumRequestIntervalMillis())) { sendError(context, payload.requestId(), 4, "Rate limited"); return; }
        if (payload.periodType() == 0 && (payload.count() < 1 || payload.count() > 90)) { sendError(context, payload.requestId(), 2, "Invalid daily trend count"); return; }
        if (payload.periodType() == 1 && (payload.count() < 1 || payload.count() > 26)) { sendError(context, payload.requestId(), 2, "Invalid weekly trend count"); return; }
        if (payload.periodType() != 0 && payload.periodType() != 1) { sendError(context, payload.requestId(), 3, "Unsupported trend"); return; }
        context.responseSender().sendPacket(payload.periodType() == 0 ? TrendResponsePayload.daily(payload.requestId(), STATISTICS.getDailyMetrics(context.server(), payload.count())) : TrendResponsePayload.weekly(payload.requestId(), STATISTICS.getWeeklyMetrics(context.server(), payload.count())));
    }

    private static void sendError(ServerPlayNetworking.Context context, long requestId, int code, String message) {
        context.responseSender().sendPacket(new StatsErrorPayload(ProtocolConstants.PROTOCOL_VERSION, requestId, code, message));
    }
}
