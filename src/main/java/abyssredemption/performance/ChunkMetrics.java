package abyssredemption.performance;

import java.util.HashMap;
import java.util.Map;

final class ChunkMetrics {
    static final class TypeTiming {
        long ns;
        long calls;
        void add(long elapsed) { ns += elapsed; calls++; }
    }
    long chunkTickNs, entityTickNs, blockEntityTickNs, scheduledBlockTickNs, scheduledFluidTickNs;
    long chunkTickCalls, entityTickCalls, blockEntityTickCalls, scheduledBlockTickCalls, scheduledFluidTickCalls;
    long loadCount, generatedLoadCount;
    long maxSingleEntityTickNs, maxSingleBlockEntityTickNs, maxSingleChunkTickNs;
    int loadAttribution = 3;
    final Map<String, TypeTiming> entityTypes = new HashMap<>();
    final Map<String, TypeTiming> blockEntityTypes = new HashMap<>();

    long totalMeasuredNs() { return chunkTickNs + entityTickNs + blockEntityTickNs + scheduledBlockTickNs + scheduledFluidTickNs; }

    void add(PerformanceProfiler.Category category, long elapsed, String typeId) {
        switch (category) {
            case CHUNK -> { chunkTickNs += elapsed; chunkTickCalls++; maxSingleChunkTickNs = Math.max(maxSingleChunkTickNs, elapsed); }
            case ENTITY -> { entityTickNs += elapsed; entityTickCalls++; maxSingleEntityTickNs = Math.max(maxSingleEntityTickNs, elapsed);
                if (typeId != null) entityTypes.computeIfAbsent(typeId, ignored -> new TypeTiming()).add(elapsed); }
            case BLOCK_ENTITY -> { blockEntityTickNs += elapsed; blockEntityTickCalls++; maxSingleBlockEntityTickNs = Math.max(maxSingleBlockEntityTickNs, elapsed);
                if (typeId != null) blockEntityTypes.computeIfAbsent(typeId, ignored -> new TypeTiming()).add(elapsed); }
            case SCHEDULED_BLOCK -> { scheduledBlockTickNs += elapsed; scheduledBlockTickCalls++; }
            case SCHEDULED_FLUID -> { scheduledFluidTickNs += elapsed; scheduledFluidTickCalls++; }
        }
    }
}
