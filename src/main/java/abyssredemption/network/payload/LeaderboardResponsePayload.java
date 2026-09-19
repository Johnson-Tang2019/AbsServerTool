package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LeaderboardResponsePayload(int protocolVersion, long requestId, int leaderboardType, int page, int totalPages, int totalEntries, long generatedAtEpochMillis, String contextLabel, List<Entry> entries) implements CustomPacketPayload {
    public record Entry(int rank, UUID uuid, String playerName, long value, boolean online) {}
    public static final Type<LeaderboardResponsePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "leaderboard_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardResponsePayload> CODEC = StreamCodec.of(LeaderboardResponsePayload::write, LeaderboardResponsePayload::read);
    private static void write(RegistryFriendlyByteBuf b, LeaderboardResponsePayload v) {
        b.writeVarInt(v.protocolVersion()); b.writeVarLong(v.requestId()); b.writeVarInt(v.leaderboardType()); b.writeVarInt(v.page()); b.writeVarInt(v.totalPages()); b.writeVarInt(v.totalEntries()); b.writeLong(v.generatedAtEpochMillis()); b.writeUtf(v.contextLabel(), 128); b.writeVarInt(v.entries().size());
        for (Entry e : v.entries()) { b.writeVarInt(e.rank()); b.writeUUID(e.uuid()); b.writeUtf(e.playerName(), 64); b.writeVarLong(e.value()); b.writeBoolean(e.online()); }
    }
    private static LeaderboardResponsePayload read(RegistryFriendlyByteBuf b) {
        int protocol = b.readVarInt(); long id = b.readVarLong(); int type = b.readVarInt(); int page = b.readVarInt(); int pages = b.readVarInt(); int total = b.readVarInt(); long generated = b.readLong(); String context = b.readUtf(128); int count = Math.min(100, b.readVarInt()); List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(new Entry(b.readVarInt(), b.readUUID(), b.readUtf(64), b.readVarLong(), b.readBoolean()));
        return new LeaderboardResponsePayload(protocol, id, type, page, pages, total, generated, context, List.copyOf(entries));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
