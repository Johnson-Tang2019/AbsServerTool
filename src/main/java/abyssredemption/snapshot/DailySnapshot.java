package abyssredemption.snapshot;

import java.util.Map;

public record DailySnapshot(String dateKey, long capturedAtEpochMillis, boolean partial, Map<String, VanillaStatSnapshot> players) {}
