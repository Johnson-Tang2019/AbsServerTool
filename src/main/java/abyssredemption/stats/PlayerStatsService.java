package abyssredemption.stats;

import abyssredemption.AbsServerTool;
import abyssredemption.player.PlayerNameResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class PlayerStatsService {
    private final OfflineStatsReader offline = new OfflineStatsReader();
    private final OnlineStatsReader online = new OnlineStatsReader();

    public Map<UUID, PlayerStatSnapshot> collectAllStats(MinecraftServer server) {
        Map<UUID, PlayerStatSnapshot> result = new HashMap<>();
        Path stats = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).getParent().resolve("stats");
        if (Files.isDirectory(stats)) {
            try (var files = Files.list(stats)) {
                files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                    String name = path.getFileName().toString().replace(".json", "");
                    try {
                        UUID uuid = UUID.fromString(name);
                        result.put(uuid, offline.read(path, uuid));
                    } catch (Exception e) {
                        AbsServerTool.LOGGER.warn("Skipping malformed stats file {}", path);
                    }
                });
            } catch (IOException e) {
                AbsServerTool.LOGGER.warn("Cannot scan statistics directory {}", stats, e);
            }
        }
        for (var player : server.getPlayerList().getPlayers()) result.put(player.getUUID(), online.read(player));
        for (UUID uuid : WeeklyPlacementStore.trackedPlayers(server)) result.putIfAbsent(uuid, new PlayerStatSnapshot(uuid, 0, 0, 0));
        return Map.copyOf(result);
    }

    public Map<UUID, String> names(MinecraftServer server, Map<UUID, PlayerStatSnapshot> stats) {
        PlayerNameResolver resolver = new PlayerNameResolver();
        Map<UUID, String> names = new HashMap<>();
        stats.keySet().forEach(uuid -> names.put(uuid, resolver.resolve(server, uuid)));
        return names;
    }
}
