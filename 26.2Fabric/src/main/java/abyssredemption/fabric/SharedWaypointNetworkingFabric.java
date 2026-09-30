package abyssredemption.fabric;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.network.NetworkRateLimiter;
import abyssredemption.network.ProtocolConstants;
import abyssredemption.network.payload.*;
import abyssredemption.waypoint.SharedWaypointService;
import abyssredemption.waypoint.SharedWaypointStore;
import java.io.IOException;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class SharedWaypointNetworkingFabric {
    private static final NetworkRateLimiter SYNC_LIMITER = new NetworkRateLimiter();
    private static final NetworkRateLimiter MUTATION_LIMITER = new NetworkRateLimiter();
    private SharedWaypointNetworkingFabric() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(SharedWaypointSyncRequestPayload.TYPE, SharedWaypointSyncRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SharedWaypointMutationRequestPayload.TYPE, SharedWaypointMutationRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SharedWaypointSnapshotPayload.TYPE, SharedWaypointSnapshotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SharedWaypointDeltaPayload.TYPE, SharedWaypointDeltaPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SharedWaypointPermissionsPayload.TYPE, SharedWaypointPermissionsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SharedWaypointMutationResultPayload.TYPE, SharedWaypointMutationResultPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SharedWaypointErrorPayload.TYPE, SharedWaypointErrorPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SharedWaypointSyncRequestPayload.TYPE, (p, c) -> {
            if (p.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) { error(c.player(), p.requestId(), 1, "Unsupported protocol"); return; }
            if (!ConfigManager.get().waypoints().enabled()) { error(c.player(), p.requestId(), 2, "Waypoints disabled"); return; }
            if (!SYNC_LIMITER.allow(c.player().getUUID(), 500)) { error(c.player(), p.requestId(), 4, "Rate limited"); return; }
            sendSnapshot(c.player(), p.requestId());
        });
        ServerPlayNetworking.registerGlobalReceiver(SharedWaypointMutationRequestPayload.TYPE, (p, c) -> {
            if (!ServerPlayNetworking.canSend(c.player(), SharedWaypointMutationResultPayload.TYPE)) return;
            if (p.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) { error(c.player(), p.requestId(), 1, "Unsupported protocol"); return; }
            if (!ConfigManager.get().waypoints().enabled()) { result(c.player(), p.requestId(), SharedWaypointService.Status.DISABLED.code(), null, 0, 0); return; }
            if (!MUTATION_LIMITER.allow(c.player().getUUID(), 1000)) {
                result(c.player(), p.requestId(), SharedWaypointService.Status.RATE_LIMITED.code(), null, 0, 0);
                return;
            }
            var change = SharedWaypointService.mutate(c.server(), c.player(), p.operation(), p.waypointId(), p.expectedRevision(),
                    p.name(), p.dimensionId(), p.x(), p.y(), p.z(), p.color(), p.symbol(), p.enabled());
            var id = change.waypoint() == null ? change.deletedId() : change.waypoint().id();
            result(c.player(), p.requestId(), change.status().code(), id, change.waypointRevision(), change.datasetRevision());
            if (change.status() != SharedWaypointService.Status.SUCCESS) return;
            var delta = new SharedWaypointDeltaPayload(ProtocolConstants.PROTOCOL_VERSION, change.datasetRevision(),
                    change.operation(), id, change.waypointRevision(), change.waypoint());
            for (ServerPlayer player : c.server().getPlayerList().getPlayers())
                if (ServerPlayNetworking.canSend(player, SharedWaypointDeltaPayload.TYPE)) ServerPlayNetworking.send(player, delta);
        });
    }

    public static void sendInitial(ServerPlayer player) { sendSnapshot(player, 0); }

    private static void sendSnapshot(ServerPlayer player, long requestId) {
        if (!ConfigManager.get().waypoints().enabled() || !ServerPlayNetworking.canSend(player, SharedWaypointSnapshotPayload.TYPE)
                || !ServerPlayNetworking.canSend(player, SharedWaypointPermissionsPayload.TYPE)) return;
        try {
            SharedWaypointStore.Data data = SharedWaypointService.snapshot(player.level().getServer());
            ServerPlayNetworking.send(player, new SharedWaypointPermissionsPayload(ProtocolConstants.PROTOCOL_VERSION, true,
                    SharedWaypointService.canManage(player)));
            ServerPlayNetworking.send(player, new SharedWaypointSnapshotPayload(ProtocolConstants.PROTOCOL_VERSION,
                    requestId, data.datasetRevision(), data.waypoints().values().stream().sorted(java.util.Comparator.comparing(w -> w.id().toString())).toList()));
        } catch (IOException exception) { AbsServerTool.LOGGER.error("Unable to synchronize shared waypoints", exception);
            error(player, requestId, 3, "Waypoint storage unavailable"); }
    }

    private static void result(ServerPlayer player, long requestId, int status, java.util.UUID id, long revision, long datasetRevision) {
        ServerPlayNetworking.send(player, new SharedWaypointMutationResultPayload(ProtocolConstants.PROTOCOL_VERSION,
                requestId, status, id, revision, datasetRevision));
    }
    private static void error(ServerPlayer player, long requestId, int code, String message) {
        if (ServerPlayNetworking.canSend(player, SharedWaypointErrorPayload.TYPE))
            ServerPlayNetworking.send(player, new SharedWaypointErrorPayload(ProtocolConstants.PROTOCOL_VERSION, requestId, code, message));
    }
}
