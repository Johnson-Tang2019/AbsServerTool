package abyssredemption.network;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.network.payload.*;
import abyssredemption.stats.LeaderboardEntry;
import abyssredemption.stats.LeaderboardPage;
import abyssredemption.stats.LeaderboardService;
import abyssredemption.stats.LeaderboardType;
import abyssredemption.stats.PlacementWeekKey;
import abyssredemption.stats.WeeklyPlacementStore;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

public final class AbsServerNetworking {
    private static final LeaderboardService LEADERBOARDS = new LeaderboardService();
    private static final NetworkRateLimiter RATE_LIMITER = new NetworkRateLimiter();
    private AbsServerNetworking() {}

    public static void initialize() {
        if (!ConfigManager.get().network().enabled()) return;
        PayloadTypeRegistry.serverboundPlay().register(HelloRequestPayload.TYPE, HelloRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LeaderboardRequestPayload.TYPE, LeaderboardRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HelloResponsePayload.TYPE, HelloResponsePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LeaderboardResponsePayload.TYPE, LeaderboardResponsePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatsErrorPayload.TYPE, StatsErrorPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HelloRequestPayload.TYPE, (payload, context) -> handleHello(payload, context));
        ServerPlayNetworking.registerGlobalReceiver(LeaderboardRequestPayload.TYPE, (payload, context) -> handleRequest(payload, context));
    }

    private static void handleHello(HelloRequestPayload payload, ServerPlayNetworking.Context context) {
        if (payload.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) {
            context.responseSender().sendPacket(new StatsErrorPayload(ProtocolConstants.PROTOCOL_VERSION, 0, 1, "Unsupported protocol"));
            return;
        }
        var config = ConfigManager.get();
        context.responseSender().sendPacket(new HelloResponsePayload(ProtocolConstants.PROTOCOL_VERSION, "0.1.0", ProtocolConstants.ALL_CAPABILITIES,
                config.leaderboard().pageSize(), WeeklyPlacementStore.getTrackingStartedAt(context.server()),
                PlacementWeekKey.current(config.placements().timezone()), config.placements().timezone()));
    }

    private static void handleRequest(LeaderboardRequestPayload payload, ServerPlayNetworking.Context context) {
        if (payload.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) { sendError(context, payload.requestId(), 1, "Unsupported protocol"); return; }
        if (!RATE_LIMITER.allow(context.player().getUUID(), ConfigManager.get().network().minimumRequestIntervalMillis())) { sendError(context, payload.requestId(), 4, "Rate limited"); return; }
        LeaderboardType type = LeaderboardType.fromNetworkId(payload.leaderboardType());
        if (type == null) { sendError(context, payload.requestId(), 3, "Unsupported board"); return; }
        if (payload.page() < 1) { sendError(context, payload.requestId(), 2, "Invalid page"); return; }
        LeaderboardPage page = switch (type) {
            case PLAYTIME -> LEADERBOARDS.playtime(context.server(), payload.page());
            case DEATHS -> LEADERBOARDS.deaths(context.server(), payload.page());
            case BLOCKS_TOTAL -> LEADERBOARDS.blocksTotal(context.server(), payload.page());
            case BLOCKS_WEEKLY -> LEADERBOARDS.blocksWeekly(context.server(), payload.page());
        };
        if (payload.page() > page.totalPages()) { sendError(context, payload.requestId(), 2, "Invalid page"); return; }
        var entries = new java.util.ArrayList<LeaderboardResponsePayload.Entry>();
        for (int i = 0; i < page.entries().size(); i++) { var e = page.entries().get(i); entries.add(new LeaderboardResponsePayload.Entry((page.page() - 1) * ConfigManager.get().leaderboard().pageSize() + i + 1, e.uuid(), e.playerName(), e.value(), context.server().getPlayerList().getPlayer(e.uuid()) != null)); }
        String contextLabel = switch (type) {
            case PLAYTIME, DEATHS -> "";
            case BLOCKS_TOTAL -> "自 " + java.time.Instant.ofEpochMilli(WeeklyPlacementStore.getTrackingStartedAt(context.server())).atZone(java.time.ZoneId.systemDefault()).toLocalDate() + " 起记录";
            case BLOCKS_WEEKLY -> PlacementWeekKey.current(ConfigManager.get().placements().timezone());
        };
        context.responseSender().sendPacket(new LeaderboardResponsePayload(ProtocolConstants.PROTOCOL_VERSION, payload.requestId(), type.networkId(), page.page(), page.totalPages(), page.totalEntries(), System.currentTimeMillis(), contextLabel, entries));
    }

    private static void sendError(ServerPlayNetworking.Context context, long requestId, int code, String message) {
        context.responseSender().sendPacket(new StatsErrorPayload(ProtocolConstants.PROTOCOL_VERSION, requestId, code, message));
    }
}
