package abyssredemption.social.network;

import abyssredemption.config.ConfigManager;
import abyssredemption.network.NetworkRateLimiter;
import abyssredemption.network.payload.StatsErrorPayload;
import abyssredemption.social.*;
import java.util.function.Consumer;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class SocialNetworking {
    private static final NetworkRateLimiter LIMITER=new NetworkRateLimiter();
    private SocialNetworking() {}
    public static boolean mayView(net.minecraft.commands.CommandSourceStack source) {
        return ConfigManager.get().socialLeaderboards().allowPublicView() || Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
    }
    public static void forget(java.util.UUID uuid) { LIMITER.forget(uuid); }
    public static void request(ServerPlayer player,MinecraftServer server,SocialPayloads.Request request,Consumer<CustomPacketPayload> send) {
        long id=request.requestId();
        if(request.protocolVersion()!=2) { send.accept(new StatsErrorPayload(2,id,1,"Unsupported protocol")); return; }
        if(!ConfigManager.get().network().enabled() || !SocialRuntime.available(server) || !mayView(player.createCommandSourceStack())) { error(id,"SOCIAL_DISABLED",send); return; }
        if(!LIMITER.allow(player.getUUID(),Math.max(500,ConfigManager.get().network().minimumRequestIntervalMillis()))) { error(id,"SOCIAL_RATE_LIMITED",send); return; }
        SocialModels.Board board;
        try { board=SocialModels.Board.of(request.boardType()); } catch(IllegalArgumentException e) { error(id,"SOCIAL_UNSUPPORTED_BOARD",send); return; }
        var config=ConfigManager.get().socialLeaderboards();
        if(board==SocialModels.Board.STREET_WANDERER?!config.wanderer().enabled():!config.pigeon().enabled()) { error(id,"SOCIAL_DISABLED",send); return; }
        if(request.page()<1) { error(id,"SOCIAL_INVALID_PAGE",send); return; }
        var runtime=SocialRuntime.get(server);
        runtime.query(result->{
            if(player.hasDisconnected()) return;
            try {
                var page=runtime.page(result,board,request.page());
                var entries=page.entries().stream().map(e->new SocialPayloads.Entry(e.rank(),e.uuid(),e.playerName(),e.score(),e.subtype(),e.lastSeenAtEpochMillis(),e.daysSinceLastSeen(),e.windowPlayTimeTicks(),e.onFootDistanceCm(),e.observableActions(),e.activeDays(),e.shortVisits(),e.online())).toList();
                send.accept(new SocialPayloads.Response(2,id,board.id,page.page(),page.totalPages(),page.totalEntries(),page.generatedAtEpochMillis(),page.windowStartEpochMillis(),page.windowEndEpochMillis(),page.complete(),entries));
            } catch(IllegalArgumentException e) { error(id,"SOCIAL_INVALID_PAGE",send); }
        },reason->{ if(!player.hasDisconnected()) error(id,reason,send); });
    }
    private static void error(long id,String reason,Consumer<CustomPacketPayload> send) {
        int code=switch(reason) { case "SOCIAL_DISABLED"->40; case "SOCIAL_UNSUPPORTED_BOARD"->41; case "SOCIAL_OBSERVING"->42; case "SOCIAL_INVALID_PAGE"->43; case "SOCIAL_RATE_LIMITED"->44; default->45; };
        send.accept(new StatsErrorPayload(2,id,code,reason));
    }
}
