package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Status is the stable numeric code of SharedWaypointService.Status. */
public record SharedWaypointMutationResultPayload(int protocolVersion, long requestId, int status, UUID waypointId,
                                                   long waypointRevision, long datasetRevision) implements CustomPacketPayload {
    public static final Type<SharedWaypointMutationResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "shared_waypoint_mutation_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedWaypointMutationResultPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarInt(p.status());
                b.writeBoolean(p.waypointId() != null); if (p.waypointId() != null) b.writeUUID(p.waypointId());
                b.writeVarLong(p.waypointRevision()); b.writeVarLong(p.datasetRevision()); },
            b -> { int protocol = b.readVarInt(); long request = b.readVarLong(); int status = b.readVarInt();
                UUID id = b.readBoolean() ? b.readUUID() : null;
                return new SharedWaypointMutationResultPayload(protocol, request, status, id, b.readVarLong(), b.readVarLong()); });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
