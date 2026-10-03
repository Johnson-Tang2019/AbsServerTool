package abyssredemption.performance;

import abyssredemption.config.AbsServerConfig;
import abyssredemption.performance.LagProfileReport.*;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** Mutable session data, accessed only on the logical server thread. */
final class ProfilerSession {
    record PositionSample(long tickIndex, UUID uuid, String name, String dimension, int x, int z) {}
    static final class PlayerMetrics {
        String name;
        int uniqueLoads, sharedLoads, generatedLoads, associatedChunks;
        long associatedNs;
        PlayerMetrics(String name) { this.name = name; }
    }
    static final class DimensionMetrics {
        long totalNs, maxNs, ticks;
        void add(long elapsed) { totalNs += elapsed; maxNs = Math.max(maxNs, elapsed); ticks++; }
    }

    final MinecraftServer server;
    final long sessionId;
    final int requestedSeconds;
    final long startedAtMillis;
    final long startNs;
    final AbsServerConfig.PerformanceProfiler config;
    final LongArrayList tickDurations = new LongArrayList();
    final Map<ChunkProfileKey, ChunkMetrics> chunks = new HashMap<>();
    final Map<ChunkProfileKey, Long> currentTickChunkNs = new HashMap<>();
    final Map<UUID, PlayerMetrics> playerMetrics = new HashMap<>();
    final List<PositionSample> positionSamples = new ArrayList<>();
    final Map<String, DimensionMetrics> dimensions = new HashMap<>();
    final Map<ServerLevel, Long> levelTickStarts = new IdentityHashMap<>();
    final PriorityQueue<SlowTickEntry> slowTicks = new PriorityQueue<>(Comparator.comparingLong(SlowTickEntry::durationNs));
    long tickIndex, currentTickStartNs;
    int saveOverlapSlowTickCount;
    boolean saveActive, saveOverlapCurrentTick;

    ProfilerSession(MinecraftServer server, long sessionId, int requestedSeconds, AbsServerConfig.PerformanceProfiler config) {
        this.server = server;
        this.sessionId = sessionId;
        this.requestedSeconds = requestedSeconds;
        this.config = config;
        this.startedAtMillis = System.currentTimeMillis();
        this.startNs = System.nanoTime();
    }

    void beginTick(long now) { currentTickStartNs = now; currentTickChunkNs.clear(); saveOverlapCurrentTick = saveActive; }

    void endTick(long now) {
        if (currentTickStartNs == 0) return;
        long elapsed = Math.max(0, now - currentTickStartNs);
        tickDurations.add(elapsed);
        tickIndex++;
        if (elapsed >= config.slowTickThresholdMillis() * 1_000_000L) {
            if (saveOverlapCurrentTick || saveActive) saveOverlapSlowTickCount++;
            List<SlowTickChunkEntry> top = currentTickChunkNs.entrySet().stream()
                    .sorted(Map.Entry.<ChunkProfileKey, Long>comparingByValue().reversed()).limit(3)
                    .map(e -> new SlowTickChunkEntry(e.getKey().dimension(), e.getKey().x(), e.getKey().z(), e.getValue()))
                    .toList();
            SlowTickEntry entry = new SlowTickEntry(tickIndex, System.currentTimeMillis(), elapsed,
                    saveOverlapCurrentTick || saveActive, top);
            if (slowTicks.size() < config.maxSlowTicksStored()) slowTicks.add(entry);
            else if (!slowTicks.isEmpty() && slowTicks.peek().durationNs() < elapsed) { slowTicks.poll(); slowTicks.add(entry); }
        }
        currentTickChunkNs.clear();
        currentTickStartNs = 0;
        if (tickIndex % 20 == 0) samplePlayers();
    }

    void record(ServerLevel level, int chunkX, int chunkZ, PerformanceProfiler.Category category, long elapsed, String typeId) {
        ChunkProfileKey key = new ChunkProfileKey(level.dimension().identifier().toString(), chunkX, chunkZ);
        chunks.computeIfAbsent(key, ignored -> new ChunkMetrics()).add(category, elapsed, typeId);
        currentTickChunkNs.merge(key, elapsed, Long::sum);
    }

    void recordLevel(ServerLevel level, long elapsed) {
        dimensions.computeIfAbsent(level.dimension().identifier().toString(), ignored -> new DimensionMetrics()).add(elapsed);
    }

    void recordLoad(ServerLevel level, ChunkPos pos, boolean generated) {
        ChunkProfileKey key = new ChunkProfileKey(level.dimension().identifier().toString(), pos.x(), pos.z());
        ChunkMetrics metrics = chunks.computeIfAbsent(key, ignored -> new ChunkMetrics());
        metrics.loadCount++;
        if (generated) metrics.generatedLoadCount++;
        if (level.getChunkSource().getForceLoadedChunks().contains(pos.pack())) {
            metrics.loadAttribution = 2;
            return;
        }
        int candidates = 0;
        int viewDistance = server.getPlayerList().getViewDistance();
        List<net.minecraft.server.level.ServerPlayer> associated = new ArrayList<>();
        for (var player : server.getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(level.dimension())) continue;
            ChunkPos playerPos = player.chunkPosition();
            if (Math.max(Math.abs(playerPos.x() - pos.x()), Math.abs(playerPos.z() - pos.z())) <= viewDistance) {
                candidates++;
                associated.add(player);
            }
        }
        if (candidates == 0) return;
        metrics.loadAttribution = candidates == 1 ? 0 : 1;
        for (var player : associated) {
            PlayerMetrics playerMetrics = this.playerMetrics.computeIfAbsent(player.getUUID(), ignored -> new PlayerMetrics(player.getName().getString()));
            if (candidates == 1) playerMetrics.uniqueLoads++; else playerMetrics.sharedLoads++;
            if (generated) playerMetrics.generatedLoads++;
        }
    }

    private void samplePlayers() {
        for (var player : server.getPlayerList().getPlayers()) {
            ChunkPos pos = player.chunkPosition();
            positionSamples.add(new PositionSample(tickIndex, player.getUUID(), player.getName().getString(),
                    player.level().dimension().identifier().toString(), pos.x(), pos.z()));
        }
    }

    LagProfileReport finish(long reportId, boolean stoppedEarly) {
        long endedAt = System.currentTimeMillis();
        long[] sorted = tickDurations.toLongArray();
        Arrays.sort(sorted);
        long tickTotal = 0, max = 0;
        int over50 = 0, over100 = 0, over200 = 0;
        for (long duration : sorted) {
            tickTotal += duration; max = Math.max(max, duration);
            if (duration > 50_000_000L) over50++;
            if (duration > 100_000_000L) over100++;
            if (duration > 200_000_000L) over200++;
        }
        TickSummary summary = new TickSummary(sorted.length, sorted.length == 0 ? 0 : tickTotal / sorted.length,
                percentile(sorted, .50), percentile(sorted, .95), percentile(sorted, .99), max, over50, over100, over200);
        long[] categoryTotals = new long[5];
        List<ChunkReportEntry> chunkReports = new ArrayList<>();
        int simulationDistance = server.getPlayerList().getSimulationDistance();
        for (var entry : chunks.entrySet()) {
            ChunkProfileKey key = entry.getKey(); ChunkMetrics m = entry.getValue();
            categoryTotals[0] += m.chunkTickNs; categoryTotals[1] += m.entityTickNs;
            categoryTotals[2] += m.blockEntityTickNs; categoryTotals[3] += m.scheduledBlockTickNs;
            categoryTotals[4] += m.scheduledFluidTickNs;
            Map<UUID, String> nearby = new HashMap<>();
            for (PositionSample sample : positionSamples) {
                if (!key.dimension().equals(sample.dimension()) || Math.max(Math.abs(key.x() - sample.x()), Math.abs(key.z() - sample.z())) > simulationDistance) continue;
                nearby.put(sample.uuid(), sample.name());
            }
            for (var associated : nearby.entrySet()) {
                PlayerMetrics player = playerMetrics.computeIfAbsent(associated.getKey(), ignored -> new PlayerMetrics(associated.getValue()));
                player.associatedChunks++;
                player.associatedNs += m.totalMeasuredNs();
            }
            chunkReports.add(new ChunkReportEntry(key.dimension(), key.x(), key.z(), m.totalMeasuredNs(),
                    m.chunkTickNs, m.entityTickNs, m.blockEntityTickNs, m.scheduledBlockTickNs, m.scheduledFluidTickNs,
                    m.entityTickCalls, m.blockEntityTickCalls, m.loadCount, m.generatedLoadCount, m.loadAttribution,
                    nearby.values().stream().sorted().toList(), topTypes(m.entityTypes), topTypes(m.blockEntityTypes)));
        }
        chunkReports.sort(Comparator.comparingLong(ChunkReportEntry::totalMeasuredNs).reversed());
        List<PlayerReportEntry> players = playerMetrics.entrySet().stream()
                .map(e -> new PlayerReportEntry(e.getKey(), e.getValue().name, e.getValue().uniqueLoads,
                        e.getValue().sharedLoads, e.getValue().generatedLoads, e.getValue().associatedChunks, e.getValue().associatedNs))
                .sorted(Comparator.comparingLong(PlayerReportEntry::associatedChunkMeasuredNs).reversed()).toList();
        List<SlowTickEntry> slow = slowTicks.stream().sorted(Comparator.comparingLong(SlowTickEntry::durationNs).reversed()).toList();
        long instrumented = Arrays.stream(categoryTotals).sum();
        List<DimensionReportEntry> dimensionReports = dimensions.entrySet().stream()
                .map(e -> new DimensionReportEntry(e.getKey(), e.getValue().totalNs, e.getValue().maxNs, e.getValue().ticks))
                .sorted(Comparator.comparingLong(DimensionReportEntry::totalTickNs).reversed()).toList();
        return new LagProfileReport(reportId, sessionId, requestedSeconds, Math.max(0, endedAt - startedAtMillis),
                stoppedEarly, startedAtMillis, endedAt, summary, categoryTotals[0], categoryTotals[1], categoryTotals[2],
                categoryTotals[3], categoryTotals[4], Math.max(0, tickTotal - instrumented),
                saveOverlapSlowTickCount, dimensionReports, List.copyOf(chunkReports), List.copyOf(players), List.copyOf(slow));
    }

    private List<TypeTimingEntry> topTypes(Map<String, ChunkMetrics.TypeTiming> timings) {
        return timings.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue().ns, a.getValue().ns))
                .limit(config.maxTypeEntriesPerChunk())
                .map(e -> new TypeTimingEntry(e.getKey(), e.getValue().ns, e.getValue().calls)).toList();
    }

    private static long percentile(long[] sorted, double p) {
        if (sorted.length == 0) return 0;
        return sorted[Math.max(0, Math.min(sorted.length - 1, (int) Math.ceil(p * sorted.length) - 1))];
    }
}
