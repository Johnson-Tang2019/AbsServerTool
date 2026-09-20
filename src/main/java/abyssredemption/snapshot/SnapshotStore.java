package abyssredemption.snapshot;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.vanilla.VanillaStatsReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public final class SnapshotStore {
    private static final AttachmentType<SnapshotStoreData> ATTACHMENT = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "snapshot_store"), SnapshotStoreData.CODEC);
    private SnapshotStore() {}
    public static void initialize() {}
    private static ZoneId zone() { String value = ConfigManager.get().snapshot().timezone(); return "SYSTEM".equalsIgnoreCase(value) ? ZoneId.systemDefault() : ZoneId.of(value); }
    private static SnapshotStoreData empty() { return new SnapshotStoreData(1, ConfigManager.get().snapshot().timezone(), LocalDate.now(zone()).toString(), Map.of(), 0L); }
    public static SnapshotStoreData getOrCreate(MinecraftServer server) { return server.globalAttachments().getAttachedOrSet(ATTACHMENT, empty()); }
    public static void markStartup(MinecraftServer server) {
        var old = getOrCreate(server);
        if (old.lastShutdownAtEpochMillis() != 0L) {
            server.globalAttachments().setAttached(ATTACHMENT, new SnapshotStoreData(old.schemaVersion(), old.timezone(), old.trackingStartedDate(), old.dailySnapshots(), 0L));
        }
    }
    public static Optional<DailySnapshot> getSnapshot(MinecraftServer server, String dateKey) { return Optional.ofNullable(getOrCreate(server).dailySnapshots().get(dateKey)); }
    public static DailySnapshot getOrCreateTodayBaseline(MinecraftServer server) {
        LocalDate date = LocalDate.now(zone());
        return getSnapshot(server, date.toString()).orElseGet(() -> {
            var stored = getOrCreate(server);
            boolean cleanPreviousShutdown = stored.lastShutdownAtEpochMillis() > 0;
            captureDayBaseline(server, date, !cleanPreviousShutdown);
            if (!cleanPreviousShutdown) AbsServerTool.LOGGER.warn("Snapshot baseline created after unclean shutdown; daily data may be partial.");
            return getSnapshot(server, date.toString()).orElseThrow();
        });
    }
    public static void captureDayBaseline(MinecraftServer server, LocalDate date, boolean partial) {
        var old = getOrCreate(server); var days = new HashMap<>(old.dailySnapshots());
        String trackingStartedDate = old.dailySnapshots().isEmpty() ? date.toString() : old.trackingStartedDate();
        var reader = new VanillaStatsReader(ConfigManager.get().statistics().includeModdedBlockItems());
        Map<String, VanillaStatSnapshot> players = new HashMap<>();
        reader.readAll(server).forEach((uuid, value) -> players.put(uuid.toString(), new VanillaStatSnapshot(value.playTimeTicks(), value.deaths(), value.vanillaBlockPlacementCount())));
        days.put(date.toString(), new DailySnapshot(date.toString(), System.currentTimeMillis(), partial, Map.copyOf(players)));
        server.globalAttachments().setAttached(ATTACHMENT, new SnapshotStoreData(old.schemaVersion(), old.timezone(), trackingStartedDate, Map.copyOf(days), old.lastShutdownAtEpochMillis()));
    }
    public static List<DailySnapshot> getSnapshotsBetween(MinecraftServer server, LocalDate start, LocalDate end) { return getOrCreate(server).dailySnapshots().values().stream().filter(s -> { LocalDate date = LocalDate.parse(s.dateKey()); return !date.isBefore(start) && !date.isAfter(end); }).sorted(java.util.Comparator.comparing(DailySnapshot::dateKey)).toList(); }
    public static void cleanupOldSnapshots(MinecraftServer server, LocalDate currentDate) {
        int keep = ConfigManager.get().snapshot().retentionDays(); if (keep <= 0) return;
        var old = getOrCreate(server); var days = new HashMap<>(old.dailySnapshots()); LocalDate cutoff = currentDate.minusDays(keep - 1L); days.keySet().removeIf(key -> key.compareTo(cutoff.toString()) < 0);
        server.globalAttachments().setAttached(ATTACHMENT, new SnapshotStoreData(old.schemaVersion(), old.timezone(), old.trackingStartedDate(), Map.copyOf(days), old.lastShutdownAtEpochMillis()));
    }
    public static void recordCleanShutdown(MinecraftServer server) {
        var old = getOrCreate(server);
        server.globalAttachments().setAttached(ATTACHMENT, new SnapshotStoreData(old.schemaVersion(), old.timezone(), old.trackingStartedDate(), old.dailySnapshots(), System.currentTimeMillis()));
    }
}
