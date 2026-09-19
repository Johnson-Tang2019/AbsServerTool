package abyssredemption.stats;

import abyssredemption.config.ConfigManager;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class BlockPlacementService {
    public void recordPlacement(ServerPlayer player) {
        var config = ConfigManager.get().placements();
        if (!config.enabled() || (!config.countCreativePlacements() && player.gameMode().isCreative())) return;
        if (!config.includeFakePlayers() && player.connection == null) return;
        UUID uuid = player.getUUID();
        player.awardStat(ModStats.BLOCKS_PLACED);
        WeeklyPlacementStore.increment(player.level().getServer(), uuid);
    }

    public long weekly(MinecraftServer server, UUID uuid) { return WeeklyPlacementStore.getCurrentWeekCount(server, uuid); }
}
