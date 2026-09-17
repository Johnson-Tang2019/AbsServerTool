package abyssredemption.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

public final class OnlineStatsReader {
    public PlayerStatSnapshot read(ServerPlayer player) {
        return new PlayerStatSnapshot(player.getUUID(), player.getStats().getValue(Stats.CUSTOM, Stats.PLAY_TIME),
                player.getStats().getValue(Stats.CUSTOM, Stats.DEATHS));
    }
}
