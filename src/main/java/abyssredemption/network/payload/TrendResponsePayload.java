package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import abyssredemption.statistics.DailyMetric;
import abyssredemption.statistics.WeeklyMetric;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TrendResponsePayload(int protocolVersion, long requestId, int periodType, List<Entry> entries) implements CustomPacketPayload {
    public record Entry(String key, int activePlayers, long playTimeTicks, long placements, long deaths, boolean complete) {}
    public static final Type<TrendResponsePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "trend_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrendResponsePayload> CODEC = StreamCodec.of(TrendResponsePayload::write, TrendResponsePayload::read);
    private static void write(RegistryFriendlyByteBuf b, TrendResponsePayload p) { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarInt(p.periodType()); b.writeVarInt(p.entries().size()); for (var e : p.entries()) { b.writeUtf(e.key(), 16); b.writeVarInt(e.activePlayers()); b.writeVarLong(e.playTimeTicks()); b.writeVarLong(e.placements()); b.writeVarLong(e.deaths()); b.writeBoolean(e.complete()); } }
    private static TrendResponsePayload read(RegistryFriendlyByteBuf b) { int v=b.readVarInt(); long id=b.readVarLong(); int period=b.readVarInt(); int n=b.readVarInt(); if(n<0||n>90) throw new IllegalArgumentException("Invalid trend entry count"); List<Entry> entries=new ArrayList<>(n); for(int i=0;i<n;i++) entries.add(new Entry(b.readUtf(16),b.readVarInt(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readBoolean())); return new TrendResponsePayload(v,id,period,List.copyOf(entries)); }
    public static TrendResponsePayload daily(long requestId, List<DailyMetric> metrics) { return new TrendResponsePayload(2, requestId, 0, metrics.stream().map(m -> new Entry(m.dateKey(),m.activePlayers(),m.playTimeTicks(),m.placements(),m.deaths(),m.complete())).toList()); }
    public static TrendResponsePayload weekly(long requestId, List<WeeklyMetric> metrics) { return new TrendResponsePayload(2, requestId, 1, metrics.stream().map(m -> new Entry(m.weekKey(),m.activePlayers(),m.playTimeTicks(),m.placements(),m.deaths(),m.complete())).toList()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
