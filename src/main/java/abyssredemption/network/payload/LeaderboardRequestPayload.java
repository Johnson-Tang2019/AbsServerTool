package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LeaderboardRequestPayload(int protocolVersion, long requestId, int leaderboardType, int page) implements CustomPacketPayload {
    public static final Type<LeaderboardRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "leaderboard_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardRequestPayload> CODEC = StreamCodec.of(
            (b, v) -> { b.writeVarInt(v.protocolVersion()); b.writeVarLong(v.requestId()); b.writeVarInt(v.leaderboardType()); b.writeVarInt(v.page()); },
            b -> new LeaderboardRequestPayload(b.readVarInt(), b.readVarLong(), b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
