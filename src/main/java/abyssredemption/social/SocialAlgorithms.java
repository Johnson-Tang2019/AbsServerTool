package abyssredemption.social;

import abyssredemption.social.SocialModels.Verdict;
import abyssredemption.social.SocialSnapshots.*;
import java.time.*;
import java.util.*;

/** Pure functions; all eligibility and scores are derived from timestamped vanilla observations. */
public final class SocialAlgorithms {
    private SocialAlgorithms() {}
    public record Visit(long join,long leave,long beginTicks,long endTicks,boolean shortEligible) { public long playTicks() { return endTicks-beginTicks; } }
    public static List<Visit> visits(List<Session> sessions,int mergeMinutes) {
        List<Visit> result=new ArrayList<>();
        for(var s:sessions.stream().sorted(Comparator.comparingLong(Session::joinEpochMillis)).toList()) {
            if(!s.quality().equals("COMPLETE") || s.leavePlayTimeTicks()<s.joinPlayTimeTicks()) continue;
            var v=new Visit(s.joinEpochMillis(),s.leaveEpochMillis(),s.joinPlayTimeTicks(),s.leavePlayTimeTicks(),s.reason().equals("LOGOUT"));
            if(!result.isEmpty()) {
                var last=result.getLast(); long gap=v.join()-last.leave();
                if(gap>=0 && gap<=mergeMinutes*60000L && v.beginTicks()>=last.endTicks()) {
                    result.set(result.size()-1,new Visit(last.join(),v.leave(),last.beginTicks(),v.endTicks(),last.shortEligible()&&v.shortEligible())); continue;
                }
            }
            result.add(v);
        }
        return List.copyOf(result);
    }
    private static Verdict protection(SocialConfig c,Data data,Presence p,long now) {
        if(p==null || p.lastJoinAtEpochMillis()<=0) return Verdict.status("OBSERVING","NOT_OBSERVED");
        long start=Math.max(data.observationStartedAtEpochMillis(),p.firstObservedAtEpochMillis());
        if(now-start<c.minimumObservationDays()*86400000L) return Verdict.status("OBSERVING","NEW_PLAYER");
        return null;
    }
    public static Verdict pigeon(SocialConfig c,Data data,String uuid,SocialVanillaCounters current,long now,boolean online) {
        var p=data.presence().get(uuid); var guard=protection(c,data,p,now); if(guard!=null) return guard;
        var cfg=c.pigeon(); int days=(int)Math.max(0,(now-p.lastJoinAtEpochMillis())/86400000L);
        if(current==null || !current.valid()) return Verdict.status("INCOMPLETE","COUNTER_REGRESSION");
        if(p.sessions().stream().anyMatch(s->s.joinEpochMillis()>=now-cfg.flashLookbackDays()*86400000L && (s.quality().equals("INVALID") || current.playTimeTicks()<s.joinPlayTimeTicks() || current.playTimeTicks()<s.leavePlayTimeTicks())))
            return Verdict.status("INCOMPLETE","COUNTER_REGRESSION");
        if(current.playTimeTicks()<cfg.historicalMemberMinPlayMinutes()*1200L && activeDays(data,uuid,now,90,15*1200L)<3)
            return Verdict.status("OBSERVING","NOT_ENOUGH_PLAYTIME");
        if(!online && days>=cfg.archiveAfterDays()) return Verdict.status("ARCHIVED","ARCHIVED");
        if(!online && days>=cfg.absenceDays()) {
            int score=Math.min(100,40+(int)Math.round(60.0*(days-cfg.absenceDays())/(cfg.archiveAfterDays()-cfg.absenceDays())));
            return new Verdict("RANKED","ABSENT",1,score,days,0,0,0,0,0);
        }
        if(online) return Verdict.status("OBSERVING","OPEN_SESSION");
        long since=now-cfg.flashLookbackDays()*86400000L;
        if(p.historyTruncatedAtEpochMillis()>since || p.sessions().stream().anyMatch(s->s.joinEpochMillis()>=since && !s.quality().equals("COMPLETE")))
            return Verdict.status("INCOMPLETE","INTERRUPTED_SESSION");
        var visits=visits(p.sessions(),cfg.reconnectMergeMinutes()).stream().filter(v->v.join()>=since).toList();
        long shortCount=visits.stream().filter(v->v.shortEligible()&&v.playTicks()<=cfg.shortVisitMaxMinutes()*1200L).count();
        boolean meaningful=visits.stream().anyMatch(v->v.playTicks()>=cfg.meaningfulVisitMinMinutes()*1200L);
        // A recent logout can still merge with a reconnect, so wait until that grace interval expires.
        boolean settled=visits.isEmpty() || now-visits.getLast().leave()>cfg.reconnectMergeMinutes()*60000L;
        if(days<cfg.absenceDays() && shortCount>=cfg.shortVisitMinCount() && !meaningful && settled)
            return new Verdict("RANKED","FLASH",2,Math.min(95,55+10*((int)shortCount-cfg.shortVisitMinCount())),days,0,0,0,0,(int)shortCount);
        return Verdict.status("NORMAL","NORMAL");
    }
    public static int activeDays(Data data,String uuid,long now,int days,long minTicks) {
        ZoneId zone=ZoneId.of(data.timezone()); LocalDate today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate(); int count=0;
        for(int i=days;i>0;i--) {
            var start=data.dailyBaselines().get(today.minusDays(i).toString()); var end=data.dailyBaselines().get(today.minusDays(i-1).toString());
            if(start==null || end==null || start.partial() || end.partial()) continue;
            var a=start.players().get(uuid); var b=end.players().get(uuid); if(a==null || b==null) continue;
            try { if(b.minus(a).playTimeTicks()>=minTicks) count++; } catch(IllegalArgumentException ignored) { }
        }
        return count;
    }
    public static Verdict wanderer(SocialConfig c,Data data,String uuid,SocialVanillaCounters current,long now) {
        var p=data.presence().get(uuid); var guard=protection(c,data,p,now); if(guard!=null) return guard;
        var cfg=c.wanderer(); var zone=ZoneId.of(data.timezone()); var today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        long since=today.minusDays(cfg.lookbackDays()).atStartOfDay(zone).toInstant().toEpochMilli();
        var base=data.dailyBaselines().get(today.minusDays(cfg.lookbackDays()).toString());
        if(base==null || base.partial() || !base.players().containsKey(uuid)) return Verdict.status("OBSERVING","NOT_ENOUGH_DAYS");
        if(p.historyTruncatedAtEpochMillis()>since) return Verdict.status("INCOMPLETE","TRUNCATED_HISTORY");
        // Any missed day/rollback leaves the window unprovable; never treat missing values as zero.
        var prior=base.players().get(uuid);
        for(int i=cfg.lookbackDays()-1;i>=0;i--) {
            var day=data.dailyBaselines().get(today.minusDays(i).toString());
            if(day==null || day.partial() || !day.players().containsKey(uuid)) return Verdict.status("INCOMPLETE","MISSING_BASELINE");
            try { day.players().get(uuid).minus(prior); } catch(IllegalArgumentException e) { return Verdict.status("INCOMPLETE","COUNTER_REGRESSION"); }
            prior=day.players().get(uuid);
        }
        boolean modeEvidence=false;
        for(var s:p.sessions()) if(s.leaveEpochMillis()==0 || s.leaveEpochMillis()>=since) {
            if(!s.modesKnown() || s.quality().equals("INTERRUPTED") || s.quality().equals("INVALID")) return Verdict.status("INCOMPLETE","UNSUPPORTED_GAMEMODE");
            modeEvidence=true;
            if(!cfg.includeCreativeAndSpectator() && s.restrictedModeSeen()) return Verdict.status("INCOMPLETE","UNSUPPORTED_GAMEMODE");
        }
        if(!modeEvidence) return Verdict.status("INCOMPLETE","UNSUPPORTED_GAMEMODE");
        if(p.lastJoinAtEpochMillis()<since && p.lastLeaveAtEpochMillis()<since && p.sessions().stream().noneMatch(s->s.quality().equals("OPEN")))
            return Verdict.status("NORMAL","NOT_OBSERVED");
        try {
            if(current==null || !current.valid()) return Verdict.status("INCOMPLETE","COUNTER_REGRESSION");
            current.minus(prior);
            var d=current.minus(base.players().get(uuid));
            int active=activeDays(data,uuid,now,cfg.lookbackDays(),1);
            var midnight=data.dailyBaselines().get(today.toString());
            if(current.minus(midnight.players().get(uuid)).playTimeTicks()>0) active++;
            long foot=d.onFootCm(), actions=d.actions(); double hours=d.playTimeTicks()/72000.0;
            double footShare=foot/Math.max(1.0,(double)foot+d.vehicleCm());
            if(d.playTimeTicks()<cfg.minimumPlayMinutes()*1200L) return Verdict.status("NORMAL","NOT_ENOUGH_PLAYTIME");
            if(active<cfg.minimumActiveDays()) return Verdict.status("NORMAL","NOT_ENOUGH_DAYS");
            if(foot<cfg.minimumOnFootMeters()*100L) return Verdict.status("NORMAL","NOT_ENOUGH_FOOT_MOVEMENT");
            if(footShare<cfg.minimumOnFootShare()) return Verdict.status("NORMAL","TRANSPORT_DOMINATED");
            double rate=actions/Math.max(hours,0.01);
            if(rate>cfg.maximumObservableActionsPerHour()) return Verdict.status("NORMAL","HIGH_OPERATION_RATE");
            int score=(int)Math.round(40*clamp(foot/1000000.0))+(int)Math.round(20*clamp(active/7.0))
                    +(int)Math.round(40*clamp(1-rate/cfg.maximumObservableActionsPerHour()));
            return new Verdict("RANKED","WANDERER",3,Math.min(100,score),
                    (int)((now-p.lastJoinAtEpochMillis())/86400000L),d.playTimeTicks(),foot,actions,active,0);
        } catch(IllegalArgumentException | ArithmeticException e) { return Verdict.status("INCOMPLETE","COUNTER_REGRESSION"); }
    }
    private static double clamp(double value) { return Math.max(0,Math.min(1,value)); }
}
