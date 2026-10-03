package abyssredemption.performance;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** Explicitly started profiler. The idle hook path is a single volatile read. */
public final class PerformanceProfiler {
    public enum Category { CHUNK, ENTITY, BLOCK_ENTITY, SCHEDULED_BLOCK, SCHEDULED_FLUID }
    public record StartResult(int errorCode, long sessionId, int durationSeconds, long startedAtMillis,
                              long expectedEndMillis) {}
    public record StopResult(int errorCode, LagProfileReport report) {}
    public record Status(ProfilerState state, long sessionId, int requestedSeconds,
                         long startedAtMillis, long expectedEndMillis, long elapsedMillis, long latestReportId) {}

    private static volatile ProfilerSession active;
    private static volatile ProfilerState state = ProfilerState.IDLE;
    private static volatile LagProfileReport lastReport;
    private static long nextSessionId = 1;
    private static long nextReportId = 1;

    private PerformanceProfiler() {}

    public static boolean isRunning() { return state == ProfilerState.RUNNING; }
    public static LagProfileReport lastReport() { return lastReport; }

    public static StartResult start(MinecraftServer server, int seconds) {
        var config = ConfigManager.get().performanceProfiler();
        if (!config.enabled()) return new StartResult(20, 0, 0, 0, 0);
        if (seconds < config.minimumDurationSeconds() || (config.maximumDurationSeconds() != 0 && seconds > config.maximumDurationSeconds()))
            return new StartResult(22, 0, 0, 0, 0);
        if (state != ProfilerState.IDLE) return new StartResult(23, 0, 0, 0, 0);
        ProfilerSession session = new ProfilerSession(server, nextSessionId++, seconds, config);
        active = session;
        state = ProfilerState.RUNNING;
        AbsServerTool.LOGGER.info("Performance profiler session {} started for {} seconds", session.sessionId, seconds);
        return new StartResult(0, session.sessionId, seconds, session.startedAtMillis,
                session.startedAtMillis + seconds * 1000L);
    }

    public static StopResult stop(long sessionId) {
        ProfilerSession session = active;
        if (session == null || state != ProfilerState.RUNNING) return new StopResult(24, null);
        if (session.sessionId != sessionId) return new StopResult(25, null);
        return new StopResult(0, finish(true));
    }

    private static LagProfileReport finish(boolean early) {
        ProfilerSession session = active;
        state = ProfilerState.FINALIZING;
        try {
            LagProfileReport report = session.finish(nextReportId++, early);
            lastReport = report;
            AbsServerTool.LOGGER.info("Performance profiler report {} finished: {} ticks, {} chunks",
                    report.reportId(), report.tickSummary().tickCount(), report.chunks().size());
            return report;
        } finally {
            active = null;
            state = ProfilerState.IDLE;
        }
    }

    public static Status status() {
        ProfilerSession session = active;
        LagProfileReport last = lastReport;
        if (session == null) return new Status(state, 0, 0, 0, 0, 0, last == null ? 0 : last.reportId());
        return new Status(state, session.sessionId, session.requestedSeconds, session.startedAtMillis,
                session.startedAtMillis + session.requestedSeconds * 1000L,
                Math.max(0, (System.nanoTime() - session.startNs) / 1_000_000L), last == null ? 0 : last.reportId());
    }

    public static void onServerTickStart(MinecraftServer server) {
        ProfilerSession session = active;
        if (session == null || session.server != server || state != ProfilerState.RUNNING) return;
        session.beginTick(System.nanoTime());
    }

    public static void onServerTickEnd(MinecraftServer server) {
        ProfilerSession session = active;
        if (session == null || session.server != server || state != ProfilerState.RUNNING) return;
        long now = System.nanoTime();
        session.endTick(now);
        if (now - session.startNs >= session.requestedSeconds * 1_000_000_000L) finish(false);
    }

    public static void onLevelTickStart(ServerLevel level) {
        ProfilerSession session = active;
        if (session == null || state != ProfilerState.RUNNING) return;
        session.levelTickStarts.put(level, System.nanoTime());
    }

    public static void onLevelTickEnd(ServerLevel level) {
        ProfilerSession session = active;
        if (session == null || state != ProfilerState.RUNNING) return;
        Long start = session.levelTickStarts.remove(level);
        if (start != null) session.recordLevel(level, Math.max(0, System.nanoTime() - start));
    }

    public static void onChunkLoad(ServerLevel level, ChunkPos pos, boolean generated) {
        ProfilerSession session = active;
        if (session == null || state != ProfilerState.RUNNING) return;
        session.recordLoad(level, pos, generated);
    }

    public static void onSaveStart() {
        ProfilerSession session = active;
        if (session == null || state != ProfilerState.RUNNING) return;
        session.saveActive = true;
        session.saveOverlapCurrentTick = true;
    }

    public static void onSaveEnd() {
        ProfilerSession session = active;
        if (session == null || state != ProfilerState.RUNNING) return;
        session.saveActive = false;
    }

    public static void record(ServerLevel level, BlockPos pos, Category category, long elapsed, String typeId) {
        ProfilerSession session = active;
        if (session == null || state != ProfilerState.RUNNING) return;
        session.record(level, pos.getX() >> 4, pos.getZ() >> 4, category, Math.max(0, elapsed), typeId);
    }

    public static void recordChunk(ServerLevel level, ChunkPos pos, long elapsed) {
        ProfilerSession session = active;
        if (session == null || state != ProfilerState.RUNNING) return;
        session.record(level, pos.x(), pos.z(), Category.CHUNK, Math.max(0, elapsed), null);
    }

    public static void onServerStopping() { active = null; state = ProfilerState.IDLE; lastReport = null; }
}
