package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Error codes: 1=protocol, 2=disabled, 3=storage, 4=rate limit. */
public record SharedWaypointErrorPayload(int protocolVersion, long requestId, int code, String message) implements CustomPacketPayload {
    public static final Type<SharedWaypointErrorPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "shared_waypoint_error"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedWaypointErrorPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarInt(p.code()); b.writeUtf(p.message(), 128); },
            b -> new SharedWaypointErrorPayload(b.readVarInt(), b.readVarLong(), b.readVarInt(), b.readUtf(128)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
