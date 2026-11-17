package abyssredemption.social;

import java.util.List;
import java.util.UUID;

/** Optional configuration; cumulative gameplay statistics are never stored here. */
public record SocialConfig(boolean enabled, boolean allowPublicView, int pageSize, int cacheTtlSeconds,
        int minimumObservationDays, int dailyRetentionDays, int sessionRetentionDays,
        int maxSessionSnapshotsPerPlayer, List<String> excludedUuids, Pigeon pigeon, Wanderer wanderer) {
    public record Pigeon(boolean enabled, int historicalMemberMinPlayMinutes, int absenceDays, int archiveAfterDays,
            int shortVisitMaxMinutes, int shortVisitMinCount, int meaningfulVisitMinMinutes,
            int flashLookbackDays, int reconnectMergeMinutes) {}
    public record Wanderer(boolean enabled, int lookbackDays, int minimumPlayMinutes, int minimumActiveDays,
            long minimumOnFootMeters, double maximumObservableActionsPerHour, double minimumOnFootShare,
            boolean includeCreativeAndSpectator, boolean includeModdedItems) {}
    public static SocialConfig defaults() {
        return new SocialConfig(true, true, 10, 30, 14, 120, 90, 128, List.of(),
                new Pigeon(true,180,7,60,5,3,15,14,3),
                new Wanderer(true,14,120,2,2000,12,0.6,false,true));
    }
    public SocialConfig validated() {
        if (pageSize<1 || pageSize>50 || cacheTtlSeconds<1 || cacheTtlSeconds>3600
                || minimumObservationDays<1 || minimumObservationDays>365 || dailyRetentionDays<2 || dailyRetentionDays>366
                || sessionRetentionDays<1 || sessionRetentionDays>366 || maxSessionSnapshotsPerPlayer<8 || maxSessionSnapshotsPerPlayer>1024
                || excludedUuids==null || excludedUuids.size()>10000 || pigeon==null || wanderer==null)
            throw new IllegalArgumentException("Invalid social leaderboard configuration");
        excludedUuids.forEach(UUID::fromString);
        if (pigeon.absenceDays()<1 || pigeon.archiveAfterDays()<=pigeon.absenceDays() || pigeon.archiveAfterDays()>366
                || pigeon.historicalMemberMinPlayMinutes()<1 || pigeon.shortVisitMaxMinutes()<1
                || pigeon.shortVisitMinCount()<1 || pigeon.meaningfulVisitMinMinutes()<=pigeon.shortVisitMaxMinutes()
                || pigeon.flashLookbackDays()<1 || pigeon.flashLookbackDays()>sessionRetentionDays
                || pigeon.reconnectMergeMinutes()<0 || pigeon.reconnectMergeMinutes()>60
                || wanderer.lookbackDays()<1 || wanderer.lookbackDays()>=dailyRetentionDays
                || wanderer.minimumPlayMinutes()<1 || wanderer.minimumActiveDays()<1 || wanderer.minimumActiveDays()>wanderer.lookbackDays()
                || wanderer.minimumOnFootMeters()<1 || wanderer.minimumOnFootMeters()>1000000000L
                || !Double.isFinite(wanderer.maximumObservableActionsPerHour()) || wanderer.maximumObservableActionsPerHour()<=0
                || !Double.isFinite(wanderer.minimumOnFootShare()) || wanderer.minimumOnFootShare()<0 || wanderer.minimumOnFootShare()>1)
            throw new IllegalArgumentException("Invalid social leaderboard thresholds or retention");
        return this;
    }
}
