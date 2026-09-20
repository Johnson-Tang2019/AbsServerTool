package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HelloResponsePayload(int protocolVersion, String serverModVersion, int capabilities, int pageSize, String trackingStartedDate, String timezone, boolean todayPartial) implements CustomPacketPayload {
    public static final Type<HelloResponsePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "hello_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloResponsePayload> CODEC = StreamCodec.of(
            (b, v) -> { b.writeVarInt(v.protocolVersion()); b.writeUtf(v.serverModVersion(), 32); b.writeVarInt(v.capabilities()); b.writeVarInt(v.pageSize()); b.writeUtf(v.trackingStartedDate(), 16); b.writeUtf(v.timezone(), 64); b.writeBoolean(v.todayPartial()); },
            b -> new HelloResponsePayload(b.readVarInt(), b.readUtf(32), b.readVarInt(), b.readVarInt(), b.readUtf(16), b.readUtf(64), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
