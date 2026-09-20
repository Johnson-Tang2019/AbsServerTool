package abyssredemption.vanilla;

import abyssredemption.AbsServerTool;
import abyssredemption.player.CarpetFakePlayerDetector;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

public final class VanillaStatsReader {
    private final VanillaPlacementStatsProvider placements;
    public VanillaStatsReader(boolean includeModdedBlockItems) { placements = new VanillaPlacementStatsProvider(includeModdedBlockItems); }
    public Map<UUID, VanillaStatSnapshot> readAll(MinecraftServer server) {
        Map<UUID, VanillaStatSnapshot> result = new HashMap<>();
        Path stats = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).getParent().resolve("stats");
        if (Files.isDirectory(stats)) try (var files = Files.list(stats)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try { UUID uuid = UUID.fromString(path.getFileName().toString().replace(".json", "")); result.put(uuid, readOffline(path)); }
                catch (Exception e) { AbsServerTool.LOGGER.warn("Skipping malformed stats file {}", path); }
            });
        } catch (Exception e) { AbsServerTool.LOGGER.warn("Cannot scan statistics directory {}", stats, e); }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (CarpetFakePlayerDetector.isFake(player)) {
                result.remove(player.getUUID());
                continue;
            }
            result.put(player.getUUID(), readOnline(player));
        }
        return Map.copyOf(result);
    }
    private VanillaStatSnapshot readOnline(ServerPlayer player) {
        return new VanillaStatSnapshot(player.getStats().getValue(Stats.CUSTOM, Stats.PLAY_TIME), player.getStats().getValue(Stats.CUSTOM, Stats.DEATHS), placements.readOnline(player));
    }
    private VanillaStatSnapshot readOffline(Path path) throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject stats = root.has("stats") && root.get("stats").isJsonObject() ? root.getAsJsonObject("stats") : new JsonObject();
        JsonObject custom = stats.has("minecraft:custom") && stats.get("minecraft:custom").isJsonObject() ? stats.getAsJsonObject("minecraft:custom") : new JsonObject();
        JsonObject used = stats.has("minecraft:used") && stats.get("minecraft:used").isJsonObject() ? stats.getAsJsonObject("minecraft:used") : new JsonObject();
        return new VanillaStatSnapshot(value(custom, "minecraft:play_time"), value(custom, "minecraft:deaths"), placements.readUsed(used));
    }
    private long value(JsonObject object, String key) { var value = object.get(key); return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? Math.max(0, value.getAsLong()) : 0; }
}
