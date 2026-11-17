package abyssredemption.social;

import java.util.List;
import java.util.UUID;

public final class SocialModels {
    private SocialModels() {}
    public enum Board {
        PIGEON_ALL(0), PIGEON_ABSENT(1), PIGEON_FLASH(2), STREET_WANDERER(3);
        public final int id;
        Board(int id) { this.id=id; }
        public static Board of(int id) { for(var b:values()) if(b.id==id) return b; throw new IllegalArgumentException("Unknown social board"); }
    }
    public record Entry(int rank, UUID uuid, String playerName, int score, int subtype, long lastSeenAtEpochMillis,
            int daysSinceLastSeen, long windowPlayTimeTicks, long onFootDistanceCm, long observableActions,
            int activeDays, int shortVisits, boolean online) {
        public Entry ranked(int rank) { return new Entry(rank,uuid,playerName,score,subtype,lastSeenAtEpochMillis,daysSinceLastSeen,windowPlayTimeTicks,onFootDistanceCm,observableActions,activeDays,shortVisits,online); }
    }
    public record Page(int boardType,int page,int totalPages,int totalEntries,long generatedAtEpochMillis,
            long windowStartEpochMillis,long windowEndEpochMillis,boolean complete,List<Entry> entries) {}
    public record Verdict(String state,String reason,int subtype,int score,int days,long playTicks,long footCm,
            long actions,int activeDays,int shortVisits) {
        public boolean ranked() { return subtype>0; }
        public static Verdict status(String state,String reason) { return new Verdict(state,reason,0,0,-1,0,0,0,0,0); }
    }
}
