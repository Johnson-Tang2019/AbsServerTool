package abyssredemption.performance.network;

import abyssredemption.config.ConfigManager;
import abyssredemption.network.NetworkRateLimiter;
import abyssredemption.network.ProtocolConstants;
import abyssredemption.network.payload.StatsErrorPayload;
import abyssredemption.performance.LagProfileReport;
import abyssredemption.performance.PerformanceProfiler;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.function.Consumer;

/** Server-thread request validation shared by Fabric and NeoForge. */
public final class PerformanceRequestService {
    private static final NetworkRateLimiter START_LIMIT = new NetworkRateLimiter();
    private static final NetworkRateLimiter STOP_LIMIT = new NetworkRateLimiter();
    private static final NetworkRateLimiter STATUS_LIMIT = new NetworkRateLimiter();
    private static final NetworkRateLimiter REPORT_LIMIT = new NetworkRateLimiter();
    private PerformanceRequestService() {}
    private static void error(Consumer<CustomPacketPayload> send,long id,int code,String message){send.accept(new StatsErrorPayload(ProtocolConstants.PROTOCOL_VERSION,id,code,message));}
    private static boolean check(ServerPlayer player,int version,long id,Consumer<CustomPacketPayload> send){
        if(version!=ProtocolConstants.PROTOCOL_VERSION){error(send,id,1,"Unsupported protocol");return false;}
        if(!ConfigManager.get().performanceProfiler().enabled()){error(send,id,20,"Performance profiler disabled");return false;}
        if(!Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack())){error(send,id,21,"Permission denied");return false;}
        return true;
    }
    private static boolean limit(ServerPlayer player,long id,Consumer<CustomPacketPayload> send,NetworkRateLimiter limiter,long ms){
        if(limiter.allow(player.getUUID(),ms))return true;error(send,id,4,"Rate limited");return false;
    }
    private static LagProfileReport report(long requestedId,long requestId,Consumer<CustomPacketPayload> send){
        LagProfileReport report=PerformanceProfiler.lastReport();
        if(report==null){error(send,requestId,26,"No report");return null;}
        if(requestedId!=0&&report.reportId()!=requestedId){error(send,requestId,27,"Report not found");return null;}
        return report;
    }
    private static <T> List<T> page(List<T> all,int requested,long id,Consumer<CustomPacketPayload> send){
        int size=ConfigManager.get().performanceProfiler().reportPageSize();
        int pages=Math.max(1,(all.size()+size-1)/size);
        if(requested<1||requested>pages){error(send,id,28,"Invalid page");return null;}
        int from=(requested-1)*size;return all.subList(from,Math.min(all.size(),from+size));
    }
    private static int pages(int count){int size=ConfigManager.get().performanceProfiler().reportPageSize();return Math.max(1,(count+size-1)/size);}
    public static void start(ServerPlayer player,MinecraftServer server,PerformanceControlPayloads.StartRequest p,Consumer<CustomPacketPayload> send){
        if(!check(player,p.protocolVersion(),p.requestId(),send))return;
        var config=ConfigManager.get().performanceProfiler();
        if(p.durationSeconds()<config.minimumDurationSeconds()||(config.maximumDurationSeconds()!=0&&p.durationSeconds()>config.maximumDurationSeconds())){error(send,p.requestId(),22,"Invalid duration");return;}
        if(!limit(player,p.requestId(),send,START_LIMIT,1000))return;
        var result=PerformanceProfiler.start(server,p.durationSeconds());
        if(result.errorCode()!=0){error(send,p.requestId(),result.errorCode(),"Cannot start profiler");return;}
        send.accept(PerformanceControlPayloads.StartResponse.from(p.requestId(),result));
    }
    public static void status(ServerPlayer player,PerformanceControlPayloads.StatusRequest p,Consumer<CustomPacketPayload> send){
        if(!check(player,p.protocolVersion(),p.requestId(),send)||!limit(player,p.requestId(),send,STATUS_LIMIT,500))return;
        send.accept(PerformanceControlPayloads.StatusResponse.from(p.requestId(),PerformanceProfiler.status(),ConfigManager.get().performanceProfiler()));
    }
    public static void stop(ServerPlayer player,PerformanceControlPayloads.StopRequest p,Consumer<CustomPacketPayload> send){
        if(!check(player,p.protocolVersion(),p.requestId(),send))return;
        if(p.sessionId()<=0){error(send,p.requestId(),25,"Session not found");return;}
        if(!limit(player,p.requestId(),send,STOP_LIMIT,500))return;
        var result=PerformanceProfiler.stop(p.sessionId());
        if(result.errorCode()!=0){error(send,p.requestId(),result.errorCode(),"Cannot stop profiler");return;}
        var r=result.report();send.accept(new PerformanceControlPayloads.StopResponse(ProtocolConstants.PROTOCOL_VERSION,p.requestId(),r.sessionId(),r.reportId(),r.actualDurationMillis(),r.stoppedEarly()));
    }
    public static void summary(ServerPlayer player,PerformanceReportPayloads.SummaryRequest p,Consumer<CustomPacketPayload> send){
        if(!check(player,p.protocolVersion(),p.requestId(),send)||!limit(player,p.requestId(),send,REPORT_LIMIT,250))return;
        var r=report(p.reportId(),p.requestId(),send);if(r!=null)send.accept(PerformanceReportPayloads.SummaryResponse.from(p.requestId(),r));
    }
    public static void chunks(ServerPlayer player,PerformanceReportPayloads.ChunkPageRequest request,Consumer<CustomPacketPayload> send){
        var p=request.value();if(!check(player,p.protocolVersion(),p.requestId(),send))return;
        if(p.page()<1){error(send,p.requestId(),28,"Invalid page");return;}
        if(!limit(player,p.requestId(),send,REPORT_LIMIT,250))return;
        var r=report(p.reportId(),p.requestId(),send);if(r==null)return;var items=page(r.chunks(),p.page(),p.requestId(),send);if(items!=null)send.accept(new PerformanceReportPayloads.ChunkPageResponse(2,p.requestId(),r.reportId(),p.page(),pages(r.chunks().size()),r.chunks().size(),items));
    }
    public static void players(ServerPlayer player,PerformanceReportPayloads.PlayerPageRequest request,Consumer<CustomPacketPayload> send){
        var p=request.value();if(!check(player,p.protocolVersion(),p.requestId(),send))return;
        if(p.page()<1){error(send,p.requestId(),28,"Invalid page");return;}
        if(!limit(player,p.requestId(),send,REPORT_LIMIT,250))return;
        var r=report(p.reportId(),p.requestId(),send);if(r==null)return;var items=page(r.players(),p.page(),p.requestId(),send);if(items!=null)send.accept(new PerformanceReportPayloads.PlayerPageResponse(2,p.requestId(),r.reportId(),p.page(),pages(r.players().size()),r.players().size(),items));
    }
    public static void slowTicks(ServerPlayer player,PerformanceReportPayloads.SlowTickPageRequest request,Consumer<CustomPacketPayload> send){
        var p=request.value();if(!check(player,p.protocolVersion(),p.requestId(),send))return;
        if(p.page()<1){error(send,p.requestId(),28,"Invalid page");return;}
        if(!limit(player,p.requestId(),send,REPORT_LIMIT,250))return;
        var r=report(p.reportId(),p.requestId(),send);if(r==null)return;var items=page(r.slowTicks(),p.page(),p.requestId(),send);if(items!=null)send.accept(new PerformanceReportPayloads.SlowTickPageResponse(2,p.requestId(),r.reportId(),p.page(),pages(r.slowTicks().size()),r.slowTicks().size(),items));
    }
    public static void detail(ServerPlayer player,PerformanceReportPayloads.ChunkDetailRequest p,Consumer<CustomPacketPayload> send){
        if(!check(player,p.protocolVersion(),p.requestId(),send))return;
        if(p.dimension().isBlank()||p.dimension().length()>128){error(send,p.requestId(),29,"Invalid chunk");return;}
        if(!limit(player,p.requestId(),send,REPORT_LIMIT,250))return;
        var r=report(p.reportId(),p.requestId(),send);if(r==null)return;
        for(var e:r.chunks())if(e.dimension().equals(p.dimension())&&e.chunkX()==p.chunkX()&&e.chunkZ()==p.chunkZ()){
            send.accept(new PerformanceReportPayloads.ChunkDetailResponse(2,p.requestId(),r.reportId(),e));return;
        }
        error(send,p.requestId(),29,"Invalid chunk");
    }
}
