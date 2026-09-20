package abyssredemption.snapshot;

import abyssredemption.config.ConfigManager;
import java.time.LocalDate;
import java.time.ZoneId;
import net.minecraft.server.MinecraftServer;

public final class SnapshotScheduler {
    private static LocalDate lastObservedDate;
    private SnapshotScheduler() {}
    public static void tick(MinecraftServer server) {
        ZoneId zone = "SYSTEM".equalsIgnoreCase(ConfigManager.get().snapshot().timezone()) ? ZoneId.systemDefault() : ZoneId.of(ConfigManager.get().snapshot().timezone());
        LocalDate today = LocalDate.now(zone);
        if (lastObservedDate == null) { lastObservedDate = today; SnapshotStore.markStartup(server); SnapshotStore.getOrCreateTodayBaseline(server); return; }
        if (!today.equals(lastObservedDate)) { SnapshotStore.captureDayBaseline(server, today, false); SnapshotStore.cleanupOldSnapshots(server, today); lastObservedDate = today; abyssredemption.AbsServerTool.LOGGER.info("Captured daily vanilla statistics baseline {}", today); }
    }
}
