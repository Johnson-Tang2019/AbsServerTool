package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Operation 1=create, 2=update, 3=delete. The server ignores content fields on delete. */
public record SharedWaypointMutationRequestPayload(int protocolVersion, long requestId, int operation, UUID waypointId,
        long expectedRevision, String name, String dimensionId, int x, int y, int z, int color,
        String symbol, boolean enabled) implements CustomPacketPayload {
    public static final Type<SharedWaypointMutationRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "shared_waypoint_mutation_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedWaypointMutationRequestPayload> CODEC = StreamCodec.of(
            SharedWaypointMutationRequestPayload::write, SharedWaypointMutationRequestPayload::read);
    private static void write(RegistryFriendlyByteBuf b, SharedWaypointMutationRequestPayload p) {
        b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarInt(p.operation());
        b.writeBoolean(p.waypointId() != null); if (p.waypointId() != null) b.writeUUID(p.waypointId());
        b.writeVarLong(p.expectedRevision()); b.writeUtf(p.name(), 64); b.writeUtf(p.dimensionId(), 128);
        b.writeInt(p.x()); b.writeInt(p.y()); b.writeInt(p.z()); b.writeInt(p.color());
        b.writeUtf(p.symbol(), 8); b.writeBoolean(p.enabled());
    }
    private static SharedWaypointMutationRequestPayload read(RegistryFriendlyByteBuf b) {
        int protocol = b.readVarInt(); long request = b.readVarLong(); int operation = b.readVarInt();
        UUID id = b.readBoolean() ? b.readUUID() : null; long revision = b.readVarLong();
        return new SharedWaypointMutationRequestPayload(protocol, request, operation, id, revision,
                b.readUtf(64), b.readUtf(128), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readUtf(8), b.readBoolean());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
