package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import abyssredemption.waypoint.SharedWaypoint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SharedWaypointSnapshotPayload(int protocolVersion, long requestId, long datasetRevision,
                                            List<SharedWaypoint> waypoints) implements CustomPacketPayload {
    public static final Type<SharedWaypointSnapshotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "shared_waypoint_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedWaypointSnapshotPayload> CODEC = StreamCodec.of(
            SharedWaypointSnapshotPayload::write, SharedWaypointSnapshotPayload::read);
    private static void write(RegistryFriendlyByteBuf b, SharedWaypointSnapshotPayload p) {
        b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarLong(p.datasetRevision());
        b.writeVarInt(p.waypoints().size()); for (var waypoint : p.waypoints()) SharedWaypointCodec.write(b, waypoint);
    }
    private static SharedWaypointSnapshotPayload read(RegistryFriendlyByteBuf b) {
        int protocol = b.readVarInt(); long request = b.readVarLong(); long revision = b.readVarLong();
        int count = b.readVarInt(); if (count < 0 || count > 1024) throw new IllegalArgumentException("Invalid waypoint count");
        List<SharedWaypoint> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(SharedWaypointCodec.read(b));
        return new SharedWaypointSnapshotPayload(protocol, request, revision, List.copyOf(entries));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
