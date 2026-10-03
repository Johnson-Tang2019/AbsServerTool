package abyssredemption.performance.network;

import abyssredemption.AbsServerTool;
import abyssredemption.network.ProtocolConstants;
import abyssredemption.performance.LagProfileReport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bounded binary report transport. Page data is copied from the immutable latest report. */
public final class PerformanceReportPayloads {
    private PerformanceReportPayloads() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> packetType(String id) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, id));
    }
    private static void header(RegistryFriendlyByteBuf b, int version, long requestId) { b.writeVarInt(version); b.writeVarLong(requestId); }
    private static void reportHeader(RegistryFriendlyByteBuf b, int version, long requestId, long reportId) { header(b, version, requestId); b.writeVarLong(reportId); }
    private static void pageHeader(RegistryFriendlyByteBuf b, int version, long requestId, long reportId, int page, int pages, int count) {
        reportHeader(b, version, requestId, reportId); b.writeVarInt(page); b.writeVarInt(pages); b.writeVarInt(count);
    }
    private static void writeString(RegistryFriendlyByteBuf b, String value, int max) { b.writeUtf(value, max); }

    public record SummaryRequest(int protocolVersion, long requestId, long reportId) implements CustomPacketPayload {
        public static final Type<SummaryRequest> TYPE = packetType("performance_report_summary_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, SummaryRequest> CODEC = StreamCodec.of(
                (b,p) -> reportHeader(b,p.protocolVersion,p.requestId,p.reportId), b -> new SummaryRequest(b.readVarInt(),b.readVarLong(),b.readVarLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record SummaryResponse(int protocolVersion, long requestId, LagProfileReport report,
                                  int chunkEntries, int playerEntries, int slowTickEntries) implements CustomPacketPayload {
        public static final Type<SummaryResponse> TYPE = packetType("performance_report_summary_response");
        public static SummaryResponse from(long requestId, LagProfileReport report) {
            return new SummaryResponse(ProtocolConstants.PROTOCOL_VERSION, requestId, report,
                    report.chunks().size(), report.players().size(), report.slowTicks().size());
        }
        public static final StreamCodec<RegistryFriendlyByteBuf, SummaryResponse> CODEC = StreamCodec.of((b,p) -> {
            var r=p.report; var t=r.tickSummary(); reportHeader(b,p.protocolVersion,p.requestId,r.reportId()); b.writeVarLong(r.sessionId());
            b.writeVarInt(r.requestedDurationSeconds()); b.writeVarLong(r.actualDurationMillis()); b.writeBoolean(r.stoppedEarly());
            b.writeLong(r.startedAtEpochMillis()); b.writeLong(r.endedAtEpochMillis());
            b.writeVarInt(t.tickCount()); b.writeVarLong(t.averageNs()); b.writeVarLong(t.p50Ns()); b.writeVarLong(t.p95Ns()); b.writeVarLong(t.p99Ns()); b.writeVarLong(t.maxNs());
            b.writeVarInt(t.over50ms()); b.writeVarInt(t.over100ms()); b.writeVarInt(t.over200ms());
            b.writeVarLong(r.instrumentedChunkTickNs()); b.writeVarLong(r.instrumentedEntityTickNs()); b.writeVarLong(r.instrumentedBlockEntityTickNs());
            b.writeVarLong(r.instrumentedScheduledBlockTickNs()); b.writeVarLong(r.instrumentedScheduledFluidTickNs()); b.writeVarLong(r.residualNs());
            b.writeVarInt(r.saveOverlapSlowTickCount()); b.writeVarInt(p.chunkEntries); b.writeVarInt(p.playerEntries); b.writeVarInt(p.slowTickEntries);
        }, b -> {
            int version=b.readVarInt(); long requestId=b.readVarLong(), reportId=b.readVarLong(), sessionId=b.readVarLong();
            int requested=b.readVarInt(); long duration=b.readVarLong(); boolean early=b.readBoolean(); long started=b.readLong(), ended=b.readLong();
            var t=new LagProfileReport.TickSummary(b.readVarInt(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readVarInt());
            long chunk=b.readVarLong(), entity=b.readVarLong(), blockEntity=b.readVarLong(), block=b.readVarLong(), fluid=b.readVarLong(), residual=b.readVarLong();
            int overlap=b.readVarInt(), chunks=b.readVarInt(), players=b.readVarInt(), slow=b.readVarInt();
            return new SummaryResponse(version,requestId,new LagProfileReport(reportId,sessionId,requested,duration,early,started,ended,t,chunk,entity,blockEntity,block,fluid,residual,overlap,
                    List.of(),List.of(),List.of(),List.of()),chunks,players,slow);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PageRequest(int protocolVersion,long requestId,long reportId,int page) {
        static void write(RegistryFriendlyByteBuf b, PageRequest p) { reportHeader(b,p.protocolVersion,p.requestId,p.reportId); b.writeVarInt(p.page); }
        static PageRequest read(RegistryFriendlyByteBuf b) { return new PageRequest(b.readVarInt(),b.readVarLong(),b.readVarLong(),b.readVarInt()); }
    }
    public record ChunkPageRequest(PageRequest value) implements CustomPacketPayload {
        public static final Type<ChunkPageRequest> TYPE=packetType("performance_chunk_page_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,ChunkPageRequest> CODEC=StreamCodec.of((b,p)->PageRequest.write(b,p.value),b->new ChunkPageRequest(PageRequest.read(b)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record PlayerPageRequest(PageRequest value) implements CustomPacketPayload {
        public static final Type<PlayerPageRequest> TYPE=packetType("performance_player_page_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,PlayerPageRequest> CODEC=StreamCodec.of((b,p)->PageRequest.write(b,p.value),b->new PlayerPageRequest(PageRequest.read(b)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record SlowTickPageRequest(PageRequest value) implements CustomPacketPayload {
        public static final Type<SlowTickPageRequest> TYPE=packetType("performance_slow_tick_page_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,SlowTickPageRequest> CODEC=StreamCodec.of((b,p)->PageRequest.write(b,p.value),b->new SlowTickPageRequest(PageRequest.read(b)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record ChunkDetailRequest(int protocolVersion,long requestId,long reportId,String dimension,int chunkX,int chunkZ) implements CustomPacketPayload {
        public static final Type<ChunkDetailRequest> TYPE=packetType("performance_chunk_detail_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,ChunkDetailRequest> CODEC=StreamCodec.of((b,p)->{reportHeader(b,p.protocolVersion,p.requestId,p.reportId);writeString(b,p.dimension,128);b.writeVarInt(p.chunkX);b.writeVarInt(p.chunkZ);},
                b->new ChunkDetailRequest(b.readVarInt(),b.readVarLong(),b.readVarLong(),b.readUtf(128),b.readVarInt(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static void writeChunkBase(RegistryFriendlyByteBuf b,LagProfileReport.ChunkReportEntry e){
        writeString(b,e.dimension(),128);b.writeVarInt(e.chunkX());b.writeVarInt(e.chunkZ());b.writeVarLong(e.totalMeasuredNs());
        b.writeVarLong(e.chunkTickNs());b.writeVarLong(e.entityTickNs());b.writeVarLong(e.blockEntityTickNs());b.writeVarLong(e.scheduledBlockTickNs());b.writeVarLong(e.scheduledFluidTickNs());
        b.writeVarLong(e.entityTickCalls());b.writeVarLong(e.blockEntityTickCalls());b.writeVarLong(e.loadCount());b.writeVarLong(e.generatedLoadCount());b.writeVarInt(e.loadAttribution());
        b.writeVarInt(e.associatedPlayerNames().size());int n=Math.min(5,e.associatedPlayerNames().size());for(int i=0;i<n;i++)writeString(b,e.associatedPlayerNames().get(i),64);
    }
    private static LagProfileReport.ChunkReportEntry readChunkBase(RegistryFriendlyByteBuf b){
        String dimension=b.readUtf(128);int x=b.readVarInt(),z=b.readVarInt();long total=b.readVarLong(),chunk=b.readVarLong(),entity=b.readVarLong(),be=b.readVarLong(),block=b.readVarLong(),fluid=b.readVarLong();
        long entityCalls=b.readVarLong(),beCalls=b.readVarLong(),loads=b.readVarLong(),generated=b.readVarLong();int attribution=b.readVarInt(),nameCount=b.readVarInt();
        if(nameCount<0||nameCount>100000)throw new IllegalArgumentException("Invalid player count");List<String> names=new ArrayList<>();for(int i=0;i<Math.min(5,nameCount);i++)names.add(b.readUtf(64));
        return new LagProfileReport.ChunkReportEntry(dimension,x,z,total,chunk,entity,be,block,fluid,entityCalls,beCalls,loads,generated,attribution,names,List.of(),List.of());
    }
    private static void writeTypes(RegistryFriendlyByteBuf b,List<LagProfileReport.TypeTimingEntry> types){int n=Math.min(20,types.size());b.writeVarInt(n);for(int i=0;i<n;i++){var t=types.get(i);writeString(b,t.typeId(),128);b.writeVarLong(t.timeNs());b.writeVarLong(t.calls());}}
    private static List<LagProfileReport.TypeTimingEntry> readTypes(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>20)throw new IllegalArgumentException("Invalid type count");List<LagProfileReport.TypeTimingEntry> out=new ArrayList<>(n);for(int i=0;i<n;i++)out.add(new LagProfileReport.TypeTimingEntry(b.readUtf(128),b.readVarLong(),b.readVarLong()));return out;}
    public record ChunkPageResponse(int protocolVersion,long requestId,long reportId,int page,int totalPages,int totalEntries,List<LagProfileReport.ChunkReportEntry> entries) implements CustomPacketPayload {
        public static final Type<ChunkPageResponse> TYPE=packetType("performance_chunk_page_response");
        public static final StreamCodec<RegistryFriendlyByteBuf,ChunkPageResponse> CODEC=StreamCodec.of((b,p)->{pageHeader(b,p.protocolVersion,p.requestId,p.reportId,p.page,p.totalPages,p.totalEntries);b.writeVarInt(p.entries.size());p.entries.forEach(e->writeChunkBase(b,e));},
                b->{int v=b.readVarInt();long req=b.readVarLong(),id=b.readVarLong();int page=b.readVarInt(),pages=b.readVarInt(),total=b.readVarInt(),n=b.readVarInt();if(n<0||n>20)throw new IllegalArgumentException("Invalid page size");List<LagProfileReport.ChunkReportEntry> out=new ArrayList<>(n);for(int i=0;i<n;i++)out.add(readChunkBase(b));return new ChunkPageResponse(v,req,id,page,pages,total,out);});
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record PlayerPageResponse(int protocolVersion,long requestId,long reportId,int page,int totalPages,int totalEntries,List<LagProfileReport.PlayerReportEntry> entries) implements CustomPacketPayload {
        public static final Type<PlayerPageResponse> TYPE=packetType("performance_player_page_response");
        public static final StreamCodec<RegistryFriendlyByteBuf,PlayerPageResponse> CODEC=StreamCodec.of((b,p)->{pageHeader(b,p.protocolVersion,p.requestId,p.reportId,p.page,p.totalPages,p.totalEntries);b.writeVarInt(p.entries.size());for(var e:p.entries){b.writeUUID(e.uuid());writeString(b,e.name(),64);b.writeVarInt(e.uniqueLoadAssociations());b.writeVarInt(e.sharedLoadAssociations());b.writeVarInt(e.generatedLoadAssociations());b.writeVarInt(e.associatedMeasuredChunks());b.writeVarLong(e.associatedChunkMeasuredNs());}},
                b->{int v=b.readVarInt();long req=b.readVarLong(),id=b.readVarLong();int page=b.readVarInt(),pages=b.readVarInt(),total=b.readVarInt(),n=b.readVarInt();if(n<0||n>20)throw new IllegalArgumentException("Invalid page size");List<LagProfileReport.PlayerReportEntry> out=new ArrayList<>(n);for(int i=0;i<n;i++)out.add(new LagProfileReport.PlayerReportEntry(b.readUUID(),b.readUtf(64),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarLong()));return new PlayerPageResponse(v,req,id,page,pages,total,out);});
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record SlowTickPageResponse(int protocolVersion,long requestId,long reportId,int page,int totalPages,int totalEntries,List<LagProfileReport.SlowTickEntry> entries) implements CustomPacketPayload {
        public static final Type<SlowTickPageResponse> TYPE=packetType("performance_slow_tick_page_response");
        public static final StreamCodec<RegistryFriendlyByteBuf,SlowTickPageResponse> CODEC=StreamCodec.of((b,p)->{pageHeader(b,p.protocolVersion,p.requestId,p.reportId,p.page,p.totalPages,p.totalEntries);b.writeVarInt(p.entries.size());for(var e:p.entries){b.writeVarLong(e.tickIndex());b.writeLong(e.timestampEpochMillis());b.writeVarLong(e.durationNs());b.writeBoolean(e.saveActive());b.writeVarInt(e.topChunks().size());for(var c:e.topChunks()){writeString(b,c.dimension(),128);b.writeVarInt(c.chunkX());b.writeVarInt(c.chunkZ());b.writeVarLong(c.measuredNs());}}},
                b->{int v=b.readVarInt();long req=b.readVarLong(),id=b.readVarLong();int page=b.readVarInt(),pages=b.readVarInt(),total=b.readVarInt(),n=b.readVarInt();if(n<0||n>20)throw new IllegalArgumentException("Invalid page size");List<LagProfileReport.SlowTickEntry> out=new ArrayList<>(n);for(int i=0;i<n;i++){long tick=b.readVarLong(),time=b.readLong(),ns=b.readVarLong();boolean save=b.readBoolean();int cn=b.readVarInt();if(cn<0||cn>3)throw new IllegalArgumentException("Invalid top chunk count");List<LagProfileReport.SlowTickChunkEntry> cs=new ArrayList<>(cn);for(int j=0;j<cn;j++)cs.add(new LagProfileReport.SlowTickChunkEntry(b.readUtf(128),b.readVarInt(),b.readVarInt(),b.readVarLong()));out.add(new LagProfileReport.SlowTickEntry(tick,time,ns,save,cs));}return new SlowTickPageResponse(v,req,id,page,pages,total,out);});
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record ChunkDetailResponse(int protocolVersion,long requestId,long reportId,LagProfileReport.ChunkReportEntry entry) implements CustomPacketPayload {
        public static final Type<ChunkDetailResponse> TYPE=packetType("performance_chunk_detail_response");
        public static final StreamCodec<RegistryFriendlyByteBuf,ChunkDetailResponse> CODEC=StreamCodec.of((b,p)->{reportHeader(b,p.protocolVersion,p.requestId,p.reportId);writeChunkBase(b,p.entry);writeTypes(b,p.entry.topEntityTypes());writeTypes(b,p.entry.topBlockEntityTypes());},
                b->{int v=b.readVarInt();long req=b.readVarLong(),id=b.readVarLong();var e=readChunkBase(b);var entities=readTypes(b);var blockEntities=readTypes(b);return new ChunkDetailResponse(v,req,id,new LagProfileReport.ChunkReportEntry(e.dimension(),e.chunkX(),e.chunkZ(),e.totalMeasuredNs(),e.chunkTickNs(),e.entityTickNs(),e.blockEntityTickNs(),e.scheduledBlockTickNs(),e.scheduledFluidTickNs(),e.entityTickCalls(),e.blockEntityTickCalls(),e.loadCount(),e.generatedLoadCount(),e.loadAttribution(),e.associatedPlayerNames(),entities,blockEntities));});
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
