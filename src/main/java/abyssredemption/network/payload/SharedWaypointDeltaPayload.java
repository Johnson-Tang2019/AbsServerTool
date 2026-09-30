package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import abyssredemption.waypoint.SharedWaypoint;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Operation 1=add, 2=update, 3=delete. Delete has no waypoint body. */
public record SharedWaypointDeltaPayload(int protocolVersion, long datasetRevision, int operation,
                                         UUID waypointId, long waypointRevision, SharedWaypoint waypoint) implements CustomPacketPayload {
    public static final Type<SharedWaypointDeltaPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "shared_waypoint_delta"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedWaypointDeltaPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.datasetRevision()); b.writeVarInt(p.operation());
                b.writeUUID(p.waypointId()); b.writeVarLong(p.waypointRevision()); b.writeBoolean(p.waypoint() != null);
                if (p.waypoint() != null) SharedWaypointCodec.write(b, p.waypoint()); },
            b -> { int protocol = b.readVarInt(); long revision = b.readVarLong(); int operation = b.readVarInt();
                UUID id = b.readUUID(); long itemRevision = b.readVarLong();
                return new SharedWaypointDeltaPayload(protocol, revision, operation, id, itemRevision,
                        b.readBoolean() ? SharedWaypointCodec.read(b) : null); });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
