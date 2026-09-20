package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OverviewRequestPayload(int protocolVersion, long requestId) implements CustomPacketPayload {
    public static final Type<OverviewRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "overview_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OverviewRequestPayload> CODEC = StreamCodec.of((b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); }, b -> new OverviewRequestPayload(b.readVarInt(), b.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
