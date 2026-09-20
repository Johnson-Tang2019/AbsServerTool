package abyssredemption.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;

public record SnapshotStoreData(int schemaVersion, String timezone, String trackingStartedDate, Map<String, DailySnapshot> dailySnapshots, long lastShutdownAtEpochMillis) {
    private static final Codec<VanillaStatSnapshot> STAT_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("playTimeTicks").forGetter(VanillaStatSnapshot::playTimeTicks),
            Codec.LONG.fieldOf("deaths").forGetter(VanillaStatSnapshot::deaths),
            Codec.LONG.fieldOf("vanillaBlockPlacementCount").forGetter(VanillaStatSnapshot::vanillaBlockPlacementCount)
    ).apply(i, VanillaStatSnapshot::new));
    private static final Codec<DailySnapshot> DAY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("dateKey").forGetter(DailySnapshot::dateKey),
            Codec.LONG.fieldOf("capturedAtEpochMillis").forGetter(DailySnapshot::capturedAtEpochMillis),
            Codec.BOOL.fieldOf("partial").forGetter(DailySnapshot::partial),
            Codec.unboundedMap(Codec.STRING, STAT_CODEC).fieldOf("players").forGetter(DailySnapshot::players)
    ).apply(i, DailySnapshot::new));
    public static final Codec<SnapshotStoreData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("schemaVersion").forGetter(SnapshotStoreData::schemaVersion),
            Codec.STRING.fieldOf("timezone").forGetter(SnapshotStoreData::timezone),
            Codec.STRING.fieldOf("trackingStartedDate").forGetter(SnapshotStoreData::trackingStartedDate),
            Codec.unboundedMap(Codec.STRING, DAY_CODEC).fieldOf("dailySnapshots").forGetter(SnapshotStoreData::dailySnapshots),
            Codec.LONG.optionalFieldOf("lastShutdownAtEpochMillis", 0L).forGetter(SnapshotStoreData::lastShutdownAtEpochMillis)
    ).apply(i, SnapshotStoreData::new));
}
