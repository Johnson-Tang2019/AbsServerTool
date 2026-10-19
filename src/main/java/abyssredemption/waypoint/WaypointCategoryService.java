package abyssredemption.waypoint;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.network.ProtocolConstants;
import abyssredemption.network.payload.WaypointCategories;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Separate optional category store: never changes waypoint revisions or the old wire format. */
public final class WaypointCategoryService {
    public record Data(long revision, Map<Integer,String> names) { public Data { names=Map.copyOf(names); } }
    private WaypointCategoryService() {}
    public static Path path(ServerPlayer player) {
        return player.level().getServer().getWorldPath(LevelResource.ROOT).resolve("data/absservertool_waypoint_categories.json");
    }
    public static Data load(Path file) throws IOException {
        if(!Files.exists(file)) return new Data(0,Map.of());
        try {
            var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(root.get("schemaVersion").getAsInt()!=1) throw new IOException("Unknown category schema");
            long revision=root.get("revision").getAsLong();
            var names=new TreeMap<Integer,String>();
            for(var e:root.getAsJsonObject("names").entrySet()) {
                int color=Integer.parseInt(e.getKey()); String name=e.getValue().getAsString();
                if(color<0 || color>15 || !WaypointCategories.validName(name)) throw new IOException("Invalid category");
                names.put(color,name);
            }
            if(revision<0 || names.size()>16) throw new IOException("Invalid category revision/count");
            return new Data(revision,names);
        } catch(RuntimeException failure) { throw new IOException("Cannot parse categories; refusing overwrite",failure); }
    }
    public static void save(Path file,Data data) throws IOException {
        Files.createDirectories(file.getParent());
        var root=new JsonObject(); root.addProperty("schemaVersion",1); root.addProperty("revision",data.revision());
        var names=new JsonObject(); data.names().forEach((color,name)->names.addProperty(color.toString(),name)); root.add("names",names);
        var temp=Files.createTempFile(file.getParent(),"waypoint_categories_",".tmp");
        try {
            Files.writeString(temp,root.toString());
            try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException failure) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public static int validateEdit(boolean enabled,boolean admin,long revision,WaypointCategories.Request request) {
        if(!enabled) return 7;
        if(!admin) return 1;
        if(request.expectedRevision()!=revision) return 3;
        if(request.color()<0 || request.color()>15 || !WaypointCategories.validName(request.name())) return 4;
        return revision==Long.MAX_VALUE ? 5 : 0;
    }
    public static WaypointCategories.Snapshot handle(ServerPlayer player,WaypointCategories.Request request) {
        int version=ProtocolConstants.PROTOCOL_VERSION;
        if(request.version()!=version) return new WaypointCategories.Snapshot(version,request.requestId(),4,0,Map.of());
        boolean enabled=ConfigManager.get().waypoints().enabled();
        if(!enabled) return new WaypointCategories.Snapshot(version,request.requestId(),7,0,Map.of());
        try {
            var old=load(path(player));
            if(!request.edit()) return new WaypointCategories.Snapshot(version,request.requestId(),0,old.revision(),old.names());
            int status=validateEdit(enabled,SharedWaypointService.canManage(player),old.revision(),request);
            if(status!=0) return new WaypointCategories.Snapshot(version,request.requestId(),status,old.revision(),old.names());
            var names=new TreeMap<>(old.names()); String name=request.name().strip();
            if(name.isEmpty()) names.remove(request.color()); else names.put(request.color(),name);
            var updated=new Data(old.revision()+1,names); save(path(player),updated);
            return new WaypointCategories.Snapshot(version,request.requestId(),0,updated.revision(),updated.names());
        } catch(IOException failure) {
            AbsServerTool.LOGGER.error("Unable to access waypoint categories",failure);
            return new WaypointCategories.Snapshot(version,request.requestId(),8,0,Map.of());
        }
    }
}
