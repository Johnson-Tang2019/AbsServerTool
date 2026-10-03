package abyssredemption.performance;

import java.util.List;
import java.util.UUID;

/** Immutable snapshot; only the most recent report is retained in memory. */
public record LagProfileReport(long reportId, long sessionId, int requestedDurationSeconds, long actualDurationMillis,
                               boolean stoppedEarly, long startedAtEpochMillis, long endedAtEpochMillis,
                               TickSummary tickSummary, long instrumentedChunkTickNs, long instrumentedEntityTickNs,
                               long instrumentedBlockEntityTickNs, long instrumentedScheduledBlockTickNs,
                               long instrumentedScheduledFluidTickNs, long residualNs, int saveOverlapSlowTickCount,
                               List<DimensionReportEntry> dimensions, List<ChunkReportEntry> chunks, List<PlayerReportEntry> players,
                               List<SlowTickEntry> slowTicks) {
    public record TickSummary(int tickCount, long averageNs, long p50Ns, long p95Ns, long p99Ns, long maxNs,
                              int over50ms, int over100ms, int over200ms) {}
    public record TypeTimingEntry(String typeId, long timeNs, long calls) {}
    public record DimensionReportEntry(String dimension, long totalTickNs, long maxTickNs, long tickCount) {}
    public record PlayerReportEntry(UUID uuid, String name, int uniqueLoadAssociations,
                                    int sharedLoadAssociations, int generatedLoadAssociations,
                                    int associatedMeasuredChunks, long associatedChunkMeasuredNs) {}
    public record ChunkReportEntry(String dimension, int chunkX, int chunkZ, long totalMeasuredNs,
                                   long chunkTickNs, long entityTickNs, long blockEntityTickNs,
                                   long scheduledBlockTickNs, long scheduledFluidTickNs,
                                   long entityTickCalls, long blockEntityTickCalls, long loadCount,
                                   long generatedLoadCount, int loadAttribution, List<String> associatedPlayerNames,
                                   List<TypeTimingEntry> topEntityTypes, List<TypeTimingEntry> topBlockEntityTypes) {}
    public record SlowTickChunkEntry(String dimension, int chunkX, int chunkZ, long measuredNs) {}
    public record SlowTickEntry(long tickIndex, long timestampEpochMillis, long durationNs,
                                boolean saveActive, List<SlowTickChunkEntry> topChunks) {}
}
