package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StatsErrorPayload(int protocolVersion, long requestId, int errorCode, String message) implements CustomPacketPayload {
    public static final Type<StatsErrorPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "stats_error"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatsErrorPayload> CODEC = StreamCodec.of(
            (b, v) -> { b.writeVarInt(v.protocolVersion()); b.writeVarLong(v.requestId()); b.writeVarInt(v.errorCode()); b.writeUtf(v.message(), 256); },
            b -> new StatsErrorPayload(b.readVarInt(), b.readVarLong(), b.readVarInt(), b.readUtf(256)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
