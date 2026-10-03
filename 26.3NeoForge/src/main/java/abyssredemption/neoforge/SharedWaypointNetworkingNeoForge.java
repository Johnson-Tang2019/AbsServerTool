package abyssredemption.neoforge;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.network.NetworkRateLimiter;
import abyssredemption.network.ProtocolConstants;
import abyssredemption.network.payload.*;
import abyssredemption.waypoint.SharedWaypointService;
import java.io.IOException;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SharedWaypointNetworkingNeoForge {
    private static final NetworkRateLimiter SYNC_LIMITER = new NetworkRateLimiter();
    private static final NetworkRateLimiter MUTATION_LIMITER = new NetworkRateLimiter();
    private SharedWaypointNetworkingNeoForge() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(SharedWaypointSyncRequestPayload.TYPE, SharedWaypointSyncRequestPayload.CODEC, (p, c) ->
                c.enqueueWork(() -> sync(p, c)));
        registrar.playToServer(SharedWaypointMutationRequestPayload.TYPE, SharedWaypointMutationRequestPayload.CODEC, (p, c) ->
                c.enqueueWork(() -> mutate(p, c)));
        registrar.playToClient(SharedWaypointSnapshotPayload.TYPE, SharedWaypointSnapshotPayload.CODEC);
        registrar.playToClient(SharedWaypointDeltaPayload.TYPE, SharedWaypointDeltaPayload.CODEC);
        registrar.playToClient(SharedWaypointPermissionsPayload.TYPE, SharedWaypointPermissionsPayload.CODEC);
        registrar.playToClient(SharedWaypointMutationResultPayload.TYPE, SharedWaypointMutationResultPayload.CODEC);
        registrar.playToClient(SharedWaypointErrorPayload.TYPE, SharedWaypointErrorPayload.CODEC);
    }

    private static boolean canSend(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
        return NetworkRegistry.hasChannel(player.connection, type.id());
    }

    private static void sync(SharedWaypointSyncRequestPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (payload.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) { error(player, payload.requestId(), 1, "Unsupported protocol"); return; }
        if (!ConfigManager.get().waypoints().enabled()) { error(player, payload.requestId(), 2, "Waypoints disabled"); return; }
        if (!SYNC_LIMITER.allow(player.getUUID(), 500)) { error(player, payload.requestId(), 4, "Rate limited"); return; }
        sendSnapshot(player, payload.requestId());
    }

    private static void mutate(SharedWaypointMutationRequestPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (!canSend(player, SharedWaypointMutationResultPayload.TYPE)) return;
        if (payload.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) { error(player, payload.requestId(), 1, "Unsupported protocol"); return; }
        if (!ConfigManager.get().waypoints().enabled()) { context.reply(new SharedWaypointMutationResultPayload(ProtocolConstants.PROTOCOL_VERSION,
                payload.requestId(), SharedWaypointService.Status.DISABLED.code(), null, 0, 0)); return; }
        if (!MUTATION_LIMITER.allow(player.getUUID(), 1000)) {
            context.reply(new SharedWaypointMutationResultPayload(ProtocolConstants.PROTOCOL_VERSION, payload.requestId(),
                    SharedWaypointService.Status.RATE_LIMITED.code(), null, 0, 0));
            return;
        }
        var server = player.level().getServer();
        var change = SharedWaypointService.mutate(server, player, payload.operation(), payload.waypointId(),
                payload.expectedRevision(), payload.name(), payload.dimensionId(), payload.x(), payload.y(), payload.z(),
                payload.color(), payload.symbol(), payload.enabled());
        var id = change.waypoint() == null ? change.deletedId() : change.waypoint().id();
        context.reply(new SharedWaypointMutationResultPayload(ProtocolConstants.PROTOCOL_VERSION, payload.requestId(),
                change.status().code(), id, change.waypointRevision(), change.datasetRevision()));
        if (change.status() != SharedWaypointService.Status.SUCCESS) return;
        var delta = new SharedWaypointDeltaPayload(ProtocolConstants.PROTOCOL_VERSION, change.datasetRevision(),
                change.operation(), id, change.waypointRevision(), change.waypoint());
        for (ServerPlayer recipient : server.getPlayerList().getPlayers())
            if (canSend(recipient, SharedWaypointDeltaPayload.TYPE)) PacketDistributor.sendToPlayer(recipient, delta);
    }

    public static void sendInitial(ServerPlayer player) { sendSnapshot(player, 0); }

    private static void sendSnapshot(ServerPlayer player, long requestId) {
        if (!ConfigManager.get().waypoints().enabled() || !canSend(player, SharedWaypointSnapshotPayload.TYPE)
                || !canSend(player, SharedWaypointPermissionsPayload.TYPE)) return;
        try {
            var data = SharedWaypointService.snapshot(player.level().getServer());
            PacketDistributor.sendToPlayer(player, new SharedWaypointPermissionsPayload(ProtocolConstants.PROTOCOL_VERSION,
                    true, SharedWaypointService.canManage(player)));
            PacketDistributor.sendToPlayer(player, new SharedWaypointSnapshotPayload(ProtocolConstants.PROTOCOL_VERSION,
                    requestId, data.datasetRevision(), data.waypoints().values().stream().sorted(java.util.Comparator.comparing(w -> w.id().toString())).toList()));
        } catch (IOException exception) { AbsServerTool.LOGGER.error("Unable to synchronize shared waypoints", exception);
            error(player, requestId, 3, "Waypoint storage unavailable"); }
    }
    private static void error(ServerPlayer player, long requestId, int code, String message) {
        if (canSend(player, SharedWaypointErrorPayload.TYPE))
            PacketDistributor.sendToPlayer(player, new SharedWaypointErrorPayload(ProtocolConstants.PROTOCOL_VERSION, requestId, code, message));
    }
}
