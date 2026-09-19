package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HelloRequestPayload(int protocolVersion) implements CustomPacketPayload {
    public static final Type<HelloRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "hello_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloRequestPayload> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeVarInt(value.protocolVersion()),
            buf -> new HelloRequestPayload(buf.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
