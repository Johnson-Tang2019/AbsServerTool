package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SharedWaypointSyncRequestPayload(int protocolVersion, long requestId) implements CustomPacketPayload {
    public static final Type<SharedWaypointSyncRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "shared_waypoint_sync_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedWaypointSyncRequestPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); },
            b -> new SharedWaypointSyncRequestPayload(b.readVarInt(), b.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
