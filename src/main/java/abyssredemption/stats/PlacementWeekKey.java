package abyssredemption.stats;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

public final class PlacementWeekKey {
    private PlacementWeekKey() {}
    public static String current(String configuredZone) {
        ZoneId zone = "SYSTEM".equalsIgnoreCase(configuredZone) ? ZoneId.systemDefault() : ZoneId.of(configuredZone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        return "%d-W%02d".formatted(now.get(IsoFields.WEEK_BASED_YEAR), now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}
