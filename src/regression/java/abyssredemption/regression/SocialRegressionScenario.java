package abyssredemption.regression;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.social.*;
import abyssredemption.social.SocialModels.Board;
import abyssredemption.social.SocialSnapshots.*;
import com.google.gson.*;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Synthetic history and real network clients, confined to the dedicated social-regression run directory. */
public final class SocialRegressionScenario {
    private static final long DAY=86400000L, MEMBER=3600000;
    private final Set<String> completed=new HashSet<>();
    private final Set<UUID> checkedPlayers=new HashSet<>();
    private int ticks;
    private boolean queryPassed;
    public void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("absserver").then(Commands.literal("socialtest").then(Commands.literal("done")
                .then(Commands.argument("client",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c->{
                    completed.add(com.mojang.brigadier.arguments.StringArgumentType.getString(c,"client"));
                    if(completed.size()>=2 && queryPassed) {
                        require(checkedPlayers.size()>=1,"No real connected player was checked");
                        AbsServerTool.LOGGER.info("SOCIAL_RUNTIME_REGRESSION_PASSED: two client loaders, 1000 offline stats, live counters, mode snapshot, asynchronous query and board results");
                        c.getSource().getServer().halt(false);
                    }
                    return 1;
                })))));
    }
    private static UUID id(int value) { return new UUID(0,value); }
    private static SocialVanillaCounters counters(long play,long foot,long actions) { return new SocialVanillaCounters(play,foot,0,0,0,0,0,0,actions,0,0,0,0); }
    public void started(MinecraftServer server) {
        try {
            require(server.getServerDirectory().toAbsolutePath().normalize().toString().contains("social-regression"),"Fixture must run in its dedicated directory");
            SocialRuntime.stop(server);
            var reader=new SocialStatsReader(true); var config=ConfigManager.get().socialLeaderboards();
            long now=System.currentTimeMillis(); String zone=ConfigManager.get().snapshot().timezone(); if(zone.equals("SYSTEM")) zone=ZoneId.systemDefault().getId();
            var today=Instant.ofEpochMilli(now).atZone(ZoneId.of(zone)).toLocalDate(); var world=server.getWorldPath(LevelResource.ROOT);
            var days=new HashMap<String,Day>(); var presence=new HashMap<String,Presence>(); var names=new JsonArray(); var current=new HashMap<UUID,SocialVanillaCounters>();
            for(int i=1;i<=1000;i++) {
                UUID id=id(i); current.put(id,counters(MEMBER,0,0)); var user=new JsonObject(); user.addProperty("name","SocialFixture"+i); user.addProperty("uuid",id.toString()); user.addProperty("expiresOn","2030-01-01 00:00:00 +0000"); names.add(user);
            }
            presence.put(id(1).toString(),new Presence(now-100*DAY,now-10*DAY,now-10*DAY+60000,List.of(),0));
            for(int i=4;i<=14;i++) presence.put(id(i).toString(),new Presence(now-100*DAY,now-10*DAY,now-10*DAY+60000,List.of(),0));
            var visits=new ArrayList<Session>();
            for(int i=5;i>0;i--) visits.add(new Session(now-i*DAY,now-i*DAY+60000,MEMBER+(5-i)*1200L,MEMBER+(6-i)*1200L,"survival","survival","COMPLETE","LOGOUT",true,false));
            presence.put(id(2).toString(),new Presence(now-100*DAY,now-DAY,now-DAY+60000,List.copyOf(visits),0));
            var session=new Session(now-13*DAY,now-DAY,MEMBER,MEMBER+288000,"survival","survival","COMPLETE","LOGOUT",true,false);
            presence.put(id(3).toString(),new Presence(now-100*DAY,now-DAY,now-DAY,List.of(session),0));
            for(int day=0;day<=14;day++) {
                int active=Math.min(day,3); var values=new HashMap<String,SocialVanillaCounters>();
                for(int i=1;i<=1000;i++) values.put(id(i).toString(),counters(MEMBER,0,0));
                values.put(id(3).toString(),counters(MEMBER+active*96000L,100+active*500000L/3,active*16/3));
                days.put(today.minusDays(14-day).toString(),new Day(now-(14-day)*DAY,false,Map.copyOf(values)));
            }
            current.put(id(3),counters(MEMBER+288000,500100,16));
            current.put(id(2),counters(MEMBER+6000,0,0));
            Path file=world.resolve("data/absservertool_social_snapshots.json");
            var store=new SocialSnapshotStore(file,config,zone,reader.fingerprint(),now);
            store.write(new Data(1,zone,reader.fingerprint(),now-100*DAY,Map.copyOf(days),Map.copyOf(presence)));
            Files.createDirectories(world.resolve("stats"));
            for(var entry:current.entrySet()) {
                var custom=new JsonObject(); custom.addProperty("minecraft:play_time",entry.getValue().playTimeTicks()); custom.addProperty("minecraft:walk_one_cm",entry.getValue().walkCm());
                var used=new JsonObject(); used.addProperty("minecraft:stone",entry.getValue().blockItemUse()); var stats=new JsonObject(); stats.add("minecraft:custom",custom); stats.add("minecraft:used",used);
                var document=new JsonObject(); document.add("stats",stats); document.addProperty("DataVersion",0);
                Files.writeString(world.resolve("stats").resolve(entry.getKey()+".json"),document.toString());
            }
            Files.writeString(server.getServerDirectory().resolve("usercache.json"),names.toString());
            SocialRuntime.start(server);
            AbsServerTool.LOGGER.info("SOCIAL_RUNTIME_FIXTURE_READY: 1000 identities with synthetic 14-day observations");
        } catch(Exception e) { throw new AssertionError("Cannot prepare social runtime fixture",e); }
    }
    public void tick(MinecraftServer server) {
        ticks++;
        if(ticks==20) {
            long begin=System.nanoTime();
            SocialRuntime.get(server).query(result->{
                var runtime=SocialRuntime.get(server);
                require(runtime.page(result,Board.PIGEON_ABSENT,1).entries().stream().anyMatch(e->e.uuid().equals(id(1))),"Absent fixture missing");
                require(runtime.page(result,Board.PIGEON_FLASH,1).entries().stream().anyMatch(e->e.uuid().equals(id(2))),"Flash fixture missing");
                require(runtime.page(result,Board.STREET_WANDERER,1).entries().stream().anyMatch(e->e.uuid().equals(id(3)) && e.score()==56),"Wanderer fixture missing");
                queryPassed=true; AbsServerTool.LOGGER.info("SOCIAL_RUNTIME_QUERY_PASSED: 1000 offline players read and scored asynchronously");
            },reason->{throw new AssertionError("Runtime query failed: "+reason);});
            require(System.nanoTime()-begin<250000000L,"Offline IO blocked the calling tick");
        }
        if(ticks%20==0) for(var player:server.getPlayerList().getPlayers()) if(checkedPlayers.add(player.getUUID())) {
            player.setGameMode(GameType.SURVIVAL);
            player.awardStat(Stats.WALK_ONE_CM,10000); player.awardStat(Stats.MOB_KILLS,2);
            player.awardStat(Stats.BLOCK_MINED.get(net.minecraft.world.level.block.Blocks.STONE),3);
            player.awardStat(Stats.ITEM_CRAFTED.get(net.minecraft.world.item.Items.STONE),4);
            player.awardStat(Stats.ITEM_USED.get(net.minecraft.world.item.Items.STONE),5);
            player.awardStat(Stats.TRADED_WITH_VILLAGER,6);
            var reader=new SocialStatsReader(true); var memory=reader.readOnline(player); player.getStats().save();
            try {
                var disk=reader.readOffline(server.getWorldPath(LevelResource.ROOT).resolve("stats").resolve(player.getUUID()+".json"));
                require(disk.equals(memory),"Online stats and saved vanilla JSON differ");
            } catch(Exception e) { throw new AssertionError(e); }
            player.setGameMode(GameType.CREATIVE); player.setGameMode(GameType.SURVIVAL);
            AbsServerTool.LOGGER.info("SOCIAL_LIVE_STATS_PASSED: distance, used, mined, crafted, kills and trades match vanilla JSON");
        }
        if(ticks>18000) { AbsServerTool.LOGGER.error("SOCIAL_RUNTIME_TIMEOUT: clients {}",completed); server.halt(false); throw new AssertionError("Social runtime client timeout"); }
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
