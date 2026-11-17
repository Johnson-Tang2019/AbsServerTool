package abyssredemption.social;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.player.ExcludedPlayers;
import abyssredemption.social.SocialModels.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** One instance per running world. No offline stats IO or score calculation runs on the tick thread. */
public final class SocialRuntime {
    private static SocialRuntime current;
    private final MinecraftServer server;
    private final SocialConfig config;
    private final SocialStatsReader reader;
    private final SocialSnapshotStore store;
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->{var thread=new Thread(r,"AbsServerTool-social-io"); thread.setDaemon(true); return thread;});
    private SocialLeaderboardService.Result cache;
    private CompletableFuture<SocialLeaderboardService.Result> computing;
    private long cacheRevision=-1,cacheExcludedRevision=-1;
    private long computingRevision,computingExcludedRevision;
    private int waiting;
    private boolean closed;
    private volatile boolean healthy=true;
    private SocialRuntime(MinecraftServer server) throws java.io.IOException {
        this.server=server; config=ConfigManager.get().socialLeaderboards(); reader=new SocialStatsReader(config.wanderer().includeModdedItems());
        String timezone=ConfigManager.get().snapshot().timezone(); if(timezone.equalsIgnoreCase("SYSTEM")) timezone=ZoneId.systemDefault().getId();
        store=new SocialSnapshotStore(server.getWorldPath(LevelResource.ROOT).resolve("data/absservertool_social_snapshots.json"),config,timezone,reader.fingerprint(),System.currentTimeMillis());
        persist();
    }
    public static void start(MinecraftServer server) {
        if(current!=null) current.io.shutdown(); current=null;
        if(!ConfigManager.get().socialLeaderboards().enabled()) return;
        try { current=new SocialRuntime(server); }
        catch(Exception e) { AbsServerTool.LOGGER.warn("Social leaderboards disabled; existing observation file preserved",e); }
    }
    public static boolean available(MinecraftServer server) { return current!=null && current.server==server && current.healthy && !current.closed; }
    public static SocialRuntime get(MinecraftServer server) { if(!available(server)) throw new IllegalStateException("SOCIAL_DISABLED"); return current; }
    private void assertThread() { if(!server.isSameThread()) throw new IllegalStateException("Social state must be accessed on the server thread"); }
    private void persist() {
        var snapshot=store.data();
        io.execute(()->{ try { store.write(snapshot); } catch(Exception e) { healthy=false; AbsServerTool.LOGGER.warn("Social snapshot save failed; leaderboards disabled",e); } });
        cache=null;
    }
    private boolean eligible(ServerPlayer p) { return !ExcludedPlayers.get().contains(server,p.getUUID()); }
    public static void join(ServerPlayer player) {
        var server=player.level().getServer(); if(!available(server)) return; var runtime=get(server); runtime.assertThread();
        if(runtime.eligible(player) && runtime.store.join(player.getUUID(),runtime.reader.readOnline(player),player.gameMode.getGameModeForPlayer().getName(),System.currentTimeMillis())) runtime.persist();
    }
    public static void leave(ServerPlayer player,String reason) {
        abyssredemption.social.network.SocialNetworking.forget(player.getUUID());
        var server=player.level().getServer(); if(!available(server)) return; var runtime=get(server); runtime.assertThread();
        if(runtime.eligible(player) && runtime.store.leave(player.getUUID(),runtime.reader.readOnline(player),player.gameMode.getGameModeForPlayer().getName(),System.currentTimeMillis(),reason)) runtime.persist();
    }
    public static void mode(ServerPlayer player) {
        var server=player.level().getServer(); if(!available(server)) return; var runtime=get(server); runtime.assertThread();
        if(runtime.store.mode(player.getUUID(),player.gameMode.getGameModeForPlayer().getName())) runtime.persist();
    }
    private Map<UUID,SocialVanillaCounters> online() {
        var values=new HashMap<UUID,SocialVanillaCounters>();
        for(var player:server.getPlayerList().getPlayers()) values.put(player.getUUID(),reader.readOnline(player));
        return Map.copyOf(values);
    }
    public static void newDay(MinecraftServer server,LocalDate date,boolean partial) {
        if(!available(server)) return; var runtime=get(server); runtime.assertThread();
        if(runtime.store.data().dailyBaselines().containsKey(date.toString())) return;
        var online=runtime.online(); var world=server.getWorldPath(LevelResource.ROOT); long at=System.currentTimeMillis();
        CompletableFuture.supplyAsync(()->{
            try { return runtime.reader.readAll(world,online); } catch(Exception e) { throw new CompletionException(e); }
        },runtime.io).whenComplete((values,error)->server.execute(()->{
            if(runtime.closed) return;
            if(error!=null) { runtime.healthy=false; AbsServerTool.LOGGER.warn("Social day baseline failed",error); return; }
            runtime.store.baseline(date,partial,values,at); runtime.persist();
        }));
    }
    public void query(Consumer<SocialLeaderboardService.Result> success,Consumer<String> failure) {
        assertThread();
        if(!healthy || closed) { failure.accept("SOCIAL_DISABLED"); return; }
        if(waiting>=256) { failure.accept("SOCIAL_RATE_LIMITED"); return; }
        var exclusions=ExcludedPlayers.get().snapshot(server); long excludedRevision=ExcludedPlayers.get().revision();
        if(cache!=null && cacheRevision==store.revision() && cacheExcludedRevision==excludedRevision && System.currentTimeMillis()-cache.now()<config.cacheTtlSeconds()*1000L) { success.accept(cache); return; }
        CompletableFuture<SocialLeaderboardService.Result> job=computing;
        if(job==null) {
            var snapshot=store.data(); long revision=store.revision(); var online=online(); var onlineNames=new HashMap<UUID,String>();
            server.getPlayerList().getPlayers().forEach(p->onlineNames.put(p.getUUID(),p.getGameProfile().name()));
            var world=server.getWorldPath(LevelResource.ROOT); var directory=server.getServerDirectory(); long now=System.currentTimeMillis();
            job=CompletableFuture.supplyAsync(()->{
                try {
                    var counters=reader.readAll(world,online); var names=SocialStatsReader.names(directory); names.putAll(onlineNames);
                    return SocialLeaderboardService.calculate(config,snapshot,counters,names,online.keySet(),exclusions,now);
                } catch(Exception e) { throw new CompletionException(e); }
            },io);
            computing=job;
            computingRevision=revision; computingExcludedRevision=excludedRevision;
            job.whenComplete((result,error)->server.execute(()->{
                computing=null;
                if(error==null && !closed && revision==store.revision() && excludedRevision==ExcludedPlayers.get().revision()) {
                    cache=result; cacheRevision=revision; cacheExcludedRevision=excludedRevision;
                }
            }));
        }
        waiting++;
        long resultRevision=computingRevision,resultExcludedRevision=computingExcludedRevision;
        job.whenComplete((result,error)->server.execute(()->{
            waiting--; if(closed) return;
            if(error!=null) { AbsServerTool.LOGGER.warn("Social query failed",error); failure.accept("SOCIAL_INTERNAL_ERROR"); }
            else {
                // Re-evaluate exclusions at delivery time so an edit cannot leak a queued result.
                var latest=ExcludedPlayers.get().snapshot(server);
                boolean changed=result.boards().values().stream().flatMap(List::stream).anyMatch(e->latest.contains(e.uuid(),e.playerName()));
                if(changed || resultRevision!=store.revision() || resultExcludedRevision!=ExcludedPlayers.get().revision()) { cache=null; failure.accept("SOCIAL_OBSERVING"); } else success.accept(result);
            }
        }));
    }
    public Page page(SocialLeaderboardService.Result result,Board board,int page) { return SocialLeaderboardService.page(config,store.data().timezone(),result,board,page); }
    public static void stop(MinecraftServer server) {
        if(current==null || current.server!=server) return; var runtime=current;
        for(var p:List.copyOf(server.getPlayerList().getPlayers())) leave(p,"SERVER_STOP");
        runtime.closed=true; runtime.io.shutdown();
        try { if(!runtime.io.awaitTermination(15,TimeUnit.SECONDS)) AbsServerTool.LOGGER.warn("Social snapshot IO did not finish before shutdown deadline"); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
