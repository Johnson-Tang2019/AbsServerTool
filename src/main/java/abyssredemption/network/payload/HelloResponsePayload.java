package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HelloResponsePayload(int protocolVersion, String serverModVersion, int capabilities, int pageSize, long placementTrackingStartedAt, String currentWeekKey, String serverTimezone) implements CustomPacketPayload {
    public static final Type<HelloResponsePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "hello_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloResponsePayload> CODEC = StreamCodec.of(
            (b, v) -> { b.writeVarInt(v.protocolVersion()); b.writeUtf(v.serverModVersion(), 32); b.writeVarInt(v.capabilities()); b.writeVarInt(v.pageSize()); b.writeLong(v.placementTrackingStartedAt()); b.writeUtf(v.currentWeekKey(), 16); b.writeUtf(v.serverTimezone(), 64); },
            b -> new HelloResponsePayload(b.readVarInt(), b.readUtf(32), b.readVarInt(), b.readVarInt(), b.readLong(), b.readUtf(16), b.readUtf(64)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
