package abyssredemption.stats;

import abyssredemption.config.ConfigManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class BlockPlacementService {
    private final Map<UUID, Long> totals = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, Long>> weekly = new ConcurrentHashMap<>();

    public void recordPlacement(ServerPlayer player) {
        var config = ConfigManager.get().placements();
        if (!config.enabled() || (!config.countCreativePlacements() && player.gameMode().isCreative())) return;
        UUID uuid = player.getUUID();
        totals.merge(uuid, 1L, Long::sum);
        String week = PlacementWeekKey.current(config.timezone());
        weekly.computeIfAbsent(week, ignored -> new ConcurrentHashMap<>()).merge(uuid, 1L, Long::sum);
    }

    public long total(UUID uuid) { return totals.getOrDefault(uuid, 0L); }
    public long weekly(String week, UUID uuid) { return weekly.getOrDefault(week, Map.of()).getOrDefault(uuid, 0L); }
}
