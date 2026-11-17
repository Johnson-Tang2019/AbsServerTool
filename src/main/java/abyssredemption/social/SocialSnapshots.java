package abyssredemption.social;

import java.util.List;
import java.util.Map;

public final class SocialSnapshots {
    private SocialSnapshots() {}
    public record Session(long joinEpochMillis, long leaveEpochMillis, long joinPlayTimeTicks,
            long leavePlayTimeTicks, String joinGameMode, String leaveGameMode, String quality, String reason,
            boolean modesKnown, boolean restrictedModeSeen) {}
    public record Presence(long firstObservedAtEpochMillis, long lastJoinAtEpochMillis, long lastLeaveAtEpochMillis,
            List<Session> sessions, long historyTruncatedAtEpochMillis) {}
    public record Day(long capturedAtEpochMillis, boolean partial, Map<String,SocialVanillaCounters> players) {}
    public record Data(int schemaVersion, String timezone, String registryFingerprint,
            long observationStartedAtEpochMillis, Map<String,Day> dailyBaselines, Map<String,Presence> presence) {}
}
