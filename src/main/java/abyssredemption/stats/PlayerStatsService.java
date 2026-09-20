package abyssredemption.stats;

import abyssredemption.player.PlayerNameResolver;
import abyssredemption.config.ConfigManager;
import abyssredemption.vanilla.VanillaStatSnapshot;
import abyssredemption.vanilla.VanillaStatsReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class PlayerStatsService {
    private Map<UUID, PlayerStatSnapshot> cached = Map.of();
    private long cacheExpiresAtMillis;
    public Map<UUID, PlayerStatSnapshot> collectAllStats(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now < cacheExpiresAtMillis) return cached;
        Map<UUID, PlayerStatSnapshot> result = new HashMap<>();
        var reader = new VanillaStatsReader(ConfigManager.get().statistics().includeModdedBlockItems());
        reader.readAll(server).forEach((uuid, snapshot) -> result.put(uuid, new PlayerStatSnapshot(uuid, snapshot.playTimeTicks(), snapshot.deaths(), snapshot.vanillaBlockPlacementCount())));
        cached = Map.copyOf(result);
        cacheExpiresAtMillis = now + ConfigManager.get().statistics().currentStatsCacheSeconds() * 1000L;
        return cached;
    }

    public Map<UUID, String> names(MinecraftServer server, Map<UUID, PlayerStatSnapshot> stats) {
        PlayerNameResolver resolver = new PlayerNameResolver();
        Map<UUID, String> names = new HashMap<>();
        stats.keySet().forEach(uuid -> names.put(uuid, resolver.resolve(server, uuid)));
        return names;
    }
}
