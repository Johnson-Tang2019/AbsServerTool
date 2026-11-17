package abyssredemption.social;

/** Absolute vanilla counters (centimetres and ticks), never custom event counters. */
public record SocialVanillaCounters(long playTimeTicks, long walkCm, long sprintCm, long crouchCm,
        long elytraCm, long boatCm, long minecartCm, long horseCm, long blockItemUse,
        long blocksMined, long itemsCrafted, long mobKills, long villagerTrades) {
    public long[] values() { return new long[]{playTimeTicks,walkCm,sprintCm,crouchCm,elytraCm,boatCm,minecartCm,horseCm,blockItemUse,blocksMined,itemsCrafted,mobKills,villagerTrades}; }
    public boolean valid() { for(long value:values()) if(value<0) return false; return true; }
    public SocialVanillaCounters minus(SocialVanillaCounters baseline) {
        long[] a=values(), b=baseline.values();
        for(int i=0;i<a.length;i++) { if(a[i]<b[i] || b[i]<0) throw new IllegalArgumentException("COUNTER_REGRESSION"); a[i]-=b[i]; }
        return from(a);
    }
    public static SocialVanillaCounters from(long[] v) { return new SocialVanillaCounters(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],v[10],v[11],v[12]); }
    private static long sum(long... values) { long total=0; for(long v:values) total=Math.addExact(total,v); return total; }
    public long onFootCm() { return sum(walkCm,sprintCm,crouchCm); }
    public long vehicleCm() { return sum(elytraCm,boatCm,minecartCm,horseCm); }
    public long actions() { return sum(blockItemUse,blocksMined,itemsCrafted,mobKills,villagerTrades); }
}
