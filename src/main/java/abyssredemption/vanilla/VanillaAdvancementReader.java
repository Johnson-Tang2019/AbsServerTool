package abyssredemption.vanilla;

import abyssredemption.AbsServerTool;
import abyssredemption.player.ExcludedPlayers;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Reads completed, displayable advancements from vanilla player progress. */
public final class VanillaAdvancementReader {
    public Map<UUID, Long> readAll(MinecraftServer server) {
        ExcludedPlayers.get().observe(server);
        Map<UUID, Long> result = new HashMap<>();
        new VanillaStatsReader(false).readAll(server).keySet().forEach(uuid -> result.put(uuid, 0L));
        Path folder = server.getWorldPath(LevelResource.ROOT).resolve("advancements");
        if (Files.isDirectory(folder)) try (var files = Files.list(folder)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    String filename = path.getFileName().toString();
                    UUID uuid = UUID.fromString(filename.substring(0, filename.length() - 5));
                    if (!ExcludedPlayers.get().contains(server, uuid)) result.put(uuid, readOffline(server, path));
                } catch (Exception exception) {
                    AbsServerTool.LOGGER.warn("Skipping malformed advancements file {}", path, exception);
                }
            });
        } catch (Exception exception) {
            AbsServerTool.LOGGER.warn("Cannot scan advancements directory {}", folder, exception);
        }
        for (var player : server.getPlayerList().getPlayers()) {
            if (ExcludedPlayers.get().contains(server, player.getUUID())) {
                result.remove(player.getUUID());
                continue;
            }
            long count = server.getAdvancements().getAllAdvancements().stream()
                    .filter(holder -> holder.value().display().isPresent())
                    .filter(holder -> server.getPlayerList().getPlayerAdvancements(player).getOrStartProgress(holder).isDone())
                    .count();
            result.put(player.getUUID(), count);
        }
        return Map.copyOf(result);
    }

    private long readOffline(MinecraftServer server, Path path) throws Exception {
        var root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        long count = 0;
        for (var entry : root.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null || !entry.getValue().isJsonObject()) continue;
            var advancement = server.getAdvancements().get(id);
            if (advancement == null || advancement.value().display().isEmpty()) continue;
            var progress = AdvancementProgress.CODEC.parse(JsonOps.INSTANCE, entry.getValue()).result().orElse(null);
            if (progress == null) continue;
            progress.update(advancement.value().requirements());
            if (progress.isDone()) count++;
        }
        return count;
    }
}
