package abyssredemption.stats;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;

public final class WeeklyPlacementStore {
    private static final AttachmentType<WeeklyPlacementData> ATTACHMENT = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "weekly_placements"), WeeklyPlacementData.CODEC);
    private static final WeeklyPlacementData EMPTY = new WeeklyPlacementData(System.currentTimeMillis(), Map.of());

    private WeeklyPlacementStore() {}

    public static void initialize() {}

    public static WeeklyPlacementData getOrCreate(MinecraftServer server) {
        return server.globalAttachments().getAttachedOrSet(ATTACHMENT, EMPTY);
    }

    public static long getCurrentWeekCount(MinecraftServer server, UUID uuid) {
        String week = PlacementWeekKey.current(ConfigManager.get().placements().timezone());
        return getWeekCounts(server, week).getOrDefault(uuid.toString(), 0L);
    }

    public static Map<String, Long> getWeekCounts(MinecraftServer server, String week) {
        return getOrCreate(server).weeks().getOrDefault(week, Map.of());
    }

    public static long getTrackingStartedAt(MinecraftServer server) {
        return getOrCreate(server).trackingStartedAtEpochMillis();
    }

    public static Set<UUID> trackedPlayers(MinecraftServer server) {
        return getOrCreate(server).weeks().values().stream().flatMap(map -> map.keySet().stream())
                .map(value -> { try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; } })
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static void prune(MinecraftServer server, Map<String, Map<String, Long>> weeks) {
        int retention = ConfigManager.get().placements().weeklyRetentionWeeks();
        if (retention <= 0 || weeks.size() <= retention) return;
        var keys = new java.util.ArrayList<>(weeks.keySet());
        keys.sort(String::compareTo);
        while (keys.size() > retention) weeks.remove(keys.remove(0));
    }

    public static void increment(MinecraftServer server, UUID uuid) {
        WeeklyPlacementData old = getOrCreate(server);
        String week = PlacementWeekKey.current(ConfigManager.get().placements().timezone());
        Map<String, Map<String, Long>> weeks = new HashMap<>(old.weeks());
        Map<String, Long> counts = new HashMap<>(weeks.getOrDefault(week, Map.of()));
        counts.merge(uuid.toString(), 1L, Long::sum);
        weeks.put(week, Map.copyOf(counts));
        prune(server, weeks);
        server.globalAttachments().setAttached(ATTACHMENT, new WeeklyPlacementData(old.trackingStartedAtEpochMillis(), Map.copyOf(weeks)));
    }
}
