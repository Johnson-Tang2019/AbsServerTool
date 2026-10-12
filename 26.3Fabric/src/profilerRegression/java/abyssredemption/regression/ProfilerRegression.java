package abyssredemption.regression;

import abyssredemption.AbsServerTool;
import abyssredemption.performance.PerformanceProfiler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.TickingBlockEntity;

/** Dedicated-server regression fixture, excluded from production JARs. */
public final class ProfilerRegression implements ModInitializer {
    private RetiringTicker retiring;
    private RetiringTicker missingSample;
    private int ticks;
    private long firstReportId;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var level = server.overworld();
            level.setChunkForced(0, 0, true);
            level.getChunk(0, 0);
            retiring = new RetiringTicker(false);
            missingSample = new RetiringTicker(true);
            level.addBlockEntityTicker(retiring);
            level.addBlockEntityTicker(missingSample);
            require(PerformanceProfiler.start(server, 2).errorCode() == 0, "Cannot start profiler");
            PerformanceProfiler.record(level, null, PerformanceProfiler.Category.BLOCK_ENTITY, 1, "missing");
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks > 240) throw new AssertionError("Profiler regression timed out");
            var report = PerformanceProfiler.lastReport();
            if (report == null || PerformanceProfiler.isRunning()) return;
            if (firstReportId == 0) {
                require(retiring.calls == 1 && missingSample.calls == 1, "Tickers must execute exactly once");
                var chunk = report.chunks().stream()
                        .filter(c -> c.chunkX() == 0 && c.chunkZ() == 0 && c.dimension().equals("minecraft:overworld"))
                        .findFirst().orElseThrow(() -> new AssertionError("Missing original chunk attribution"));
                require(chunk.topBlockEntityTypes().stream().anyMatch(t -> t.typeId().equals("regression:retiring") && t.calls() == 1),
                        "Removed ticker must retain its original position and type");
                require(report.chunks().stream().flatMap(c -> c.topBlockEntityTypes().stream())
                        .noneMatch(t -> t.typeId().equals("regression:missing")), "Unlocated samples must be skipped");
                require(report.tickSummary().tickCount() > 0, "Empty tick report");
                firstReportId = report.reportId();
                require(PerformanceProfiler.start(server, 1).errorCode() == 0, "Cannot restart profiler");
            } else if (report.reportId() != firstReportId) {
                require(report.tickSummary().tickCount() > 0, "Empty repeat report");
                AbsServerTool.LOGGER.info("PROFILER_REGRESSION_PASSED: retiring ticker, null sample, automatic report, repeat session");
                server.halt(false);
            }
        });
    }

    private static void require(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }

    private static final class RetiringTicker implements TickingBlockEntity {
        private final boolean missingSample;
        private int positionReads;
        private int calls;

        private RetiringTicker(boolean missingSample) { this.missingSample = missingSample; }
        @Override public void tick() { calls++; }
        @Override public boolean isRemoved() { return calls > 0; }
        @Override public BlockPos getPos() {
            positionReads++;
            return calls > 0 || (missingSample && positionReads > 1) ? null : new BlockPos(1, 64, 1);
        }
        @Override public String getType() {
            return calls > 0 ? "regression:detached" : missingSample ? "regression:missing" : "regression:retiring";
        }
    }
}
