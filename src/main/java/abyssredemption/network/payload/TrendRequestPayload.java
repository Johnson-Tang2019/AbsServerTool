package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TrendRequestPayload(int protocolVersion, long requestId, int periodType, int count) implements CustomPacketPayload {
    public static final Type<TrendRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "trend_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrendRequestPayload> CODEC = StreamCodec.of((b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarInt(p.periodType()); b.writeVarInt(p.count()); }, b -> new TrendRequestPayload(b.readVarInt(), b.readVarLong(), b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
