package abyssredemption.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;

public record WeeklyPlacementData(long trackingStartedAtEpochMillis, Map<String, Map<String, Long>> weeks) {
    public static final Codec<WeeklyPlacementData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("trackingStartedAtEpochMillis").forGetter(WeeklyPlacementData::trackingStartedAtEpochMillis),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.LONG))
                    .fieldOf("weeks").forGetter(WeeklyPlacementData::weeks)
    ).apply(instance, WeeklyPlacementData::new));
}
