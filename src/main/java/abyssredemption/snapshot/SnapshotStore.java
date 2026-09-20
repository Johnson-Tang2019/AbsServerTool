package abyssredemption.snapshot;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import abyssredemption.vanilla.VanillaStatsReader;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Cross-loader persistence for the only business data owned by the mod: snapshots. */
public final class SnapshotStore {
    private SnapshotStore() {}
    public static void initialize() {}
    private static ZoneId zone() { String value = ConfigManager.get().snapshot().timezone(); return "SYSTEM".equalsIgnoreCase(value) ? ZoneId.systemDefault() : ZoneId.of(value); }
    private static SnapshotStoreData empty() { return new SnapshotStoreData(1, ConfigManager.get().snapshot().timezone(), LocalDate.now(zone()).toString(), Map.of(), 0L); }
    private static Path path(MinecraftServer server) { return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("absservertool_snapshots.json"); }
    public static synchronized SnapshotStoreData getOrCreate(MinecraftServer server) {
        Path file = path(server);
        try {
            if (Files.exists(file)) { var result = SnapshotStoreData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))); var value = result.result().orElse(null); if (value != null) return value; AbsServerTool.LOGGER.warn("Invalid AbsServerTool snapshot data; starting with an empty store"); }
        } catch (Exception exception) { AbsServerTool.LOGGER.warn("Unable to read AbsServerTool snapshot data", exception); }
        SnapshotStoreData value = empty(); save(server, value); return value;
    }
    private static void save(MinecraftServer server, SnapshotStoreData value) {
        try { Path file = path(server); Files.createDirectories(file.getParent()); var encoded = SnapshotStoreData.CODEC.encodeStart(JsonOps.INSTANCE, value).result().orElseThrow(); Files.writeString(file, encoded.toString(), StandardCharsets.UTF_8); }
        catch (Exception exception) { AbsServerTool.LOGGER.error("Unable to save AbsServerTool snapshot data", exception); }
    }
    public static Optional<DailySnapshot> getSnapshot(MinecraftServer server, String dateKey) { return Optional.ofNullable(getOrCreate(server).dailySnapshots().get(dateKey)); }
    public static DailySnapshot getOrCreateTodayBaseline(MinecraftServer server) {
        LocalDate date = LocalDate.now(zone());
        return getSnapshot(server, date.toString()).orElseGet(() -> { var stored = getOrCreate(server); boolean clean = stored.lastShutdownAtEpochMillis() > 0; captureDayBaseline(server, date, !clean); if (!clean) AbsServerTool.LOGGER.warn("Snapshot baseline created after unclean shutdown; daily data may be partial."); return getSnapshot(server, date.toString()).orElseThrow(); });
    }
    public static void captureDayBaseline(MinecraftServer server, LocalDate date, boolean partial) {
        var old = getOrCreate(server); var days = new HashMap<>(old.dailySnapshots()); String trackingStartedDate = old.dailySnapshots().isEmpty() ? date.toString() : old.trackingStartedDate();
        var reader = new VanillaStatsReader(ConfigManager.get().statistics().includeModdedBlockItems()); Map<String, VanillaStatSnapshot> players = new HashMap<>();
        reader.readAll(server).forEach((uuid, value) -> players.put(uuid.toString(), new VanillaStatSnapshot(value.playTimeTicks(), value.deaths(), value.vanillaBlockPlacementCount())));
        days.put(date.toString(), new DailySnapshot(date.toString(), System.currentTimeMillis(), partial, Map.copyOf(players))); save(server, new SnapshotStoreData(old.schemaVersion(), old.timezone(), trackingStartedDate, Map.copyOf(days), old.lastShutdownAtEpochMillis()));
    }
    public static List<DailySnapshot> getSnapshotsBetween(MinecraftServer server, LocalDate start, LocalDate end) { return getOrCreate(server).dailySnapshots().values().stream().filter(snapshot -> { LocalDate date = LocalDate.parse(snapshot.dateKey()); return !date.isBefore(start) && !date.isAfter(end); }).sorted(java.util.Comparator.comparing(DailySnapshot::dateKey)).toList(); }
    public static void cleanupOldSnapshots(MinecraftServer server, LocalDate currentDate) { int keep = ConfigManager.get().snapshot().retentionDays(); if (keep <= 0) return; var old = getOrCreate(server); var days = new HashMap<>(old.dailySnapshots()); LocalDate cutoff = currentDate.minusDays(keep - 1L); days.keySet().removeIf(key -> key.compareTo(cutoff.toString()) < 0); save(server, new SnapshotStoreData(old.schemaVersion(), old.timezone(), old.trackingStartedDate(), Map.copyOf(days), old.lastShutdownAtEpochMillis())); }
    public static void markStartup(MinecraftServer server) { var old = getOrCreate(server); if (old.lastShutdownAtEpochMillis() != 0L) save(server, new SnapshotStoreData(old.schemaVersion(), old.timezone(), old.trackingStartedDate(), old.dailySnapshots(), 0L)); }
    public static void recordCleanShutdown(MinecraftServer server) { var old = getOrCreate(server); save(server, new SnapshotStoreData(old.schemaVersion(), old.timezone(), old.trackingStartedDate(), old.dailySnapshots(), System.currentTimeMillis())); }
}
