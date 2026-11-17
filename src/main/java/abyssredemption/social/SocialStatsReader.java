package abyssredemption.social;

import abyssredemption.vanilla.VanillaPlacementStatsProvider;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Registry keys are captured once on the server thread; offline IO uses only immutable DTOs. */
public final class SocialStatsReader {
    private final List<Item> items;
    private final List<Block> blocks;
    private final Set<String> itemKeys,blockKeys;
    private final VanillaPlacementStatsProvider placements;
    private final String fingerprint;
    public SocialStatsReader(boolean includeModded) {
        items=BuiltInRegistries.ITEM.stream().filter(i->includeModded || BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("minecraft")).toList();
        blocks=BuiltInRegistries.BLOCK.stream().filter(i->includeModded || BuiltInRegistries.BLOCK.getKey(i).getNamespace().equals("minecraft")).toList();
        itemKeys=items.stream().map(i->BuiltInRegistries.ITEM.getKey(i).toString()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        blockKeys=blocks.stream().map(i->BuiltInRegistries.BLOCK.getKey(i).toString()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        placements=new VanillaPlacementStatsProvider(includeModded);
        fingerprint="26.3:"+includeModded+":"+UUID.nameUUIDFromBytes((String.join(",",new TreeSet<>(itemKeys))+"|"+String.join(",",new TreeSet<>(blockKeys))).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    public String fingerprint() { return fingerprint; }
    public SocialVanillaCounters readOnline(ServerPlayer p) {
        var s=p.getStats();
        long mined=0,crafted=0; for(var b:blocks) mined=Math.addExact(mined,s.getValue(Stats.BLOCK_MINED,b));
        for(var i:items) crafted=Math.addExact(crafted,s.getValue(Stats.ITEM_CRAFTED,i));
        return new SocialVanillaCounters(s.getValue(Stats.CUSTOM,Stats.PLAY_TIME),s.getValue(Stats.CUSTOM,Stats.WALK_ONE_CM),
                s.getValue(Stats.CUSTOM,Stats.SPRINT_ONE_CM),s.getValue(Stats.CUSTOM,Stats.CROUCH_ONE_CM),s.getValue(Stats.CUSTOM,Stats.AVIATE_ONE_CM),
                s.getValue(Stats.CUSTOM,Stats.BOAT_ONE_CM),s.getValue(Stats.CUSTOM,Stats.MINECART_ONE_CM),s.getValue(Stats.CUSTOM,Stats.HORSE_ONE_CM),
                placements.readOnline(p),mined,crafted,s.getValue(Stats.CUSTOM,Stats.MOB_KILLS),s.getValue(Stats.CUSTOM,Stats.TRADED_WITH_VILLAGER));
    }
    public SocialVanillaCounters readOffline(Path file) throws java.io.IOException {
        if(Files.size(file)>16*1024*1024) throw new java.io.IOException("Stats file exceeds limit");
        var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject(); var stats=object(root,"stats"); var custom=object(stats,"minecraft:custom");
        long[] v=new long[13]; String[] keys={"play_time","walk_one_cm","sprint_one_cm","crouch_one_cm","aviate_one_cm","boat_one_cm","minecart_one_cm","horse_one_cm"};
        for(int i=0;i<keys.length;i++) v[i]=value(custom,"minecraft:"+keys[i]);
        v[8]=placements.readUsed(object(stats,"minecraft:used")); v[9]=sum(object(stats,"minecraft:mined"),blockKeys);
        v[10]=sum(object(stats,"minecraft:crafted"),itemKeys); v[11]=value(custom,"minecraft:mob_kills"); v[12]=value(custom,"minecraft:traded_with_villager");
        return SocialVanillaCounters.from(v);
    }
    public Map<UUID,SocialVanillaCounters> readAll(Path world,Map<UUID,SocialVanillaCounters> online) throws java.io.IOException {
        var result=new HashMap<UUID,SocialVanillaCounters>(); Path stats=world.resolve("stats");
        if(Files.isDirectory(stats)) try(var files=Files.list(stats)) {
            for(var file:files.filter(p->p.getFileName().toString().endsWith(".json")).toList()) {
                try { var id=UUID.fromString(file.getFileName().toString().replace(".json","")); if(!online.containsKey(id)) result.put(id,readOffline(file)); }
                catch(Exception e) { abyssredemption.AbsServerTool.LOGGER.warn("Social statistics unavailable for {}",file.getFileName()); }
            }
        }
        result.putAll(online); return Map.copyOf(result);
    }
    public static Map<UUID,String> names(Path serverDirectory) {
        var result=new HashMap<UUID,String>();
        try { var array=JsonParser.parseString(Files.readString(serverDirectory.resolve("usercache.json"))).getAsJsonArray();
            for(var element:array) try { var o=element.getAsJsonObject(); result.put(UUID.fromString(o.get("uuid").getAsString()),o.get("name").getAsString()); } catch(Exception ignored) { }
        } catch(Exception ignored) { }
        return result;
    }
    private static JsonObject object(JsonObject p,String key) { return p.has(key)?p.getAsJsonObject(key):new JsonObject(); }
    private static long value(JsonObject o,String key) {
        if(!o.has(key)) return 0;
        long n=o.get(key).getAsBigDecimal().longValueExact(); if(n<0) throw new IllegalArgumentException("Negative vanilla stat"); return n;
    }
    private static long sum(JsonObject o,Set<String> keys) { long total=0; for(var entry:o.entrySet()) if(keys.contains(entry.getKey())) total=Math.addExact(total,value(o,entry.getKey())); return total; }
}
