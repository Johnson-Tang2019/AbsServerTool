package abyssredemption.social;

import abyssredemption.social.SocialSnapshots.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** In-memory timestamped observations. Call mutations on the server thread; write immutable copies on IO. */
public final class SocialSnapshotStore {
    private static final Gson JSON=new Gson();
    private final Path file;
    private final SocialConfig config;
    private Data data;
    private boolean available=true;
    private long revision;
    public SocialSnapshotStore(Path file,SocialConfig config,String timezone,String fingerprint,long now) throws IOException {
        this.file=file; this.config=config;
        if(Files.exists(file)) {
            if(Files.size(file)>128L*1024*1024) throw new IOException("Social store exceeds size limit");
            try { data=JSON.fromJson(Files.readString(file),Data.class); validate(data); }
            catch(Exception e) { available=false; throw new IOException("Invalid social store; original file preserved",e); }
            if(!data.timezone().equals(timezone) || !data.registryFingerprint().equals(fingerprint)) {
                var days=new HashMap<String,Day>(); data.dailyBaselines().forEach((key,day)->days.put(key,new Day(day.capturedAtEpochMillis(),true,day.players())));
                data=new Data(1,timezone,fingerprint,now,Map.copyOf(days),data.presence());
            }
            var presence=new HashMap<String,Presence>();
            data.presence().forEach((id,p)->presence.put(id,new Presence(p.firstObservedAtEpochMillis(),p.lastJoinAtEpochMillis(),p.lastLeaveAtEpochMillis(),
                    p.sessions().stream().map(s->s.quality().equals("OPEN") ? new Session(s.joinEpochMillis(),0,s.joinPlayTimeTicks(),0,s.joinGameMode(),s.leaveGameMode(),"INTERRUPTED","CRASH",false,s.restrictedModeSeen()):s).toList(),p.historyTruncatedAtEpochMillis())));
            replace(data.dailyBaselines(),presence);
        } else data=new Data(1,timezone,fingerprint,now,Map.of(),Map.of());
    }
    public Data data() { return data; }
    public long revision() { return revision; }
    private void replace(Map<String,Day> days,Map<String,Presence> players) {
        data=new Data(1,data.timezone(),data.registryFingerprint(),data.observationStartedAtEpochMillis(),Map.copyOf(days),Map.copyOf(players)); revision++;
    }
    public boolean join(UUID uuid,SocialVanillaCounters counters,String mode,long now) {
        String id=uuid.toString(); var old=data.presence().get(id);
        if(old!=null && old.sessions().stream().anyMatch(s->s.quality().equals("OPEN"))) return false;
        var sessions=new ArrayList<Session>(old==null?List.of():old.sessions());
        sessions.add(new Session(now,0,counters.playTimeTicks(),0,mode,mode,"OPEN","",true,restricted(mode)));
        var players=new HashMap<>(data.presence());
        players.put(id,trim(new Presence(old==null?now:old.firstObservedAtEpochMillis(),now,old==null?0:old.lastLeaveAtEpochMillis(),sessions,old==null?0:old.historyTruncatedAtEpochMillis()),now));
        replace(data.dailyBaselines(),players); return true;
    }
    public boolean leave(UUID uuid,SocialVanillaCounters counters,String mode,long now,String reason) {
        var p=data.presence().get(uuid.toString()); if(p==null) return false;
        var sessions=new ArrayList<>(p.sessions()); boolean changed=false;
        for(int i=0;i<sessions.size();i++) { var s=sessions.get(i); if(!s.quality().equals("OPEN")) continue;
            String quality=counters.playTimeTicks()<s.joinPlayTimeTicks() || now<s.joinEpochMillis()?"INVALID":"COMPLETE";
            sessions.set(i,new Session(s.joinEpochMillis(),now,s.joinPlayTimeTicks(),counters.playTimeTicks(),s.joinGameMode(),mode,quality,reason,s.modesKnown(),s.restrictedModeSeen()||restricted(mode))); changed=true;
        }
        if(!changed) return false;
        var players=new HashMap<>(data.presence()); players.put(uuid.toString(),trim(new Presence(p.firstObservedAtEpochMillis(),p.lastJoinAtEpochMillis(),now,sessions,p.historyTruncatedAtEpochMillis()),now));
        replace(data.dailyBaselines(),players); return true;
    }
    public boolean mode(UUID uuid,String mode) {
        var p=data.presence().get(uuid.toString()); if(p==null) return false; boolean changed=false; var sessions=new ArrayList<>(p.sessions());
        for(int i=0;i<sessions.size();i++) { var s=sessions.get(i); if(!s.quality().equals("OPEN") || s.leaveGameMode().equals(mode)) continue;
            sessions.set(i,new Session(s.joinEpochMillis(),0,s.joinPlayTimeTicks(),0,s.joinGameMode(),mode,"OPEN",s.reason(),s.modesKnown(),s.restrictedModeSeen()||restricted(mode))); changed=true;
        }
        if(changed) { var players=new HashMap<>(data.presence()); players.put(uuid.toString(),new Presence(p.firstObservedAtEpochMillis(),p.lastJoinAtEpochMillis(),p.lastLeaveAtEpochMillis(),List.copyOf(sessions),p.historyTruncatedAtEpochMillis())); replace(data.dailyBaselines(),players); }
        return changed;
    }
    private Presence trim(Presence p,long now) {
        long truncated=p.historyTruncatedAtEpochMillis(); var sessions=new ArrayList<>(p.sessions());
        while(!sessions.isEmpty() && (sessions.size()>config.maxSessionSnapshotsPerPlayer() || sessions.getFirst().joinEpochMillis()<now-config.sessionRetentionDays()*86400000L)) {
            var removed=sessions.removeFirst(); truncated=Math.max(truncated,removed.leaveEpochMillis()>0?removed.leaveEpochMillis():now);
        }
        return new Presence(p.firstObservedAtEpochMillis(),p.lastJoinAtEpochMillis(),p.lastLeaveAtEpochMillis(),List.copyOf(sessions),truncated);
    }
    public void baseline(LocalDate day,boolean partial,Map<UUID,SocialVanillaCounters> counters,long now) {
        var days=new HashMap<>(data.dailyBaselines()); var players=new HashMap<String,SocialVanillaCounters>(); counters.forEach((id,v)->players.put(id.toString(),v));
        days.put(day.toString(),new Day(now,partial,Map.copyOf(players)));
        days.keySet().removeIf(key->LocalDate.parse(key).isBefore(day.minusDays(config.dailyRetentionDays()-1L)));
        var presence=new HashMap<String,Presence>(); data.presence().forEach((id,p)->presence.put(id,trim(p,now)));
        replace(days,presence);
    }
    private static boolean restricted(String mode) { return !mode.equals("survival") && !mode.equals("adventure"); }
    private static void validate(Data d) {
        if(d==null || d.schemaVersion()!=1 || d.observationStartedAtEpochMillis()<0 || d.registryFingerprint()==null
                || d.dailyBaselines()==null || d.dailyBaselines().size()>366 || d.presence()==null || d.presence().size()>100000) throw new IllegalArgumentException("Invalid social schema");
        ZoneId.of(d.timezone());
        d.dailyBaselines().forEach((key,day)->{
            LocalDate.parse(key); if(day==null || day.capturedAtEpochMillis()<0 || day.players()==null || day.players().size()>100000) throw new IllegalArgumentException("Invalid day");
            day.players().forEach((id,c)->{ UUID.fromString(id); if(c==null || !c.valid()) throw new IllegalArgumentException("Invalid counters"); });
        });
        d.presence().forEach((id,p)->{
            UUID.fromString(id);
            if(p==null || p.firstObservedAtEpochMillis()<0 || p.lastJoinAtEpochMillis()<0 || p.lastLeaveAtEpochMillis()<0
                    || p.historyTruncatedAtEpochMillis()<0 || p.sessions()==null || p.sessions().size()>1024) throw new IllegalArgumentException("Invalid presence");
            long last=-1;
            for(var s:p.sessions()) {
                if(s==null || s.joinEpochMillis()<last || s.leaveEpochMillis()<0 || s.joinPlayTimeTicks()<0 || s.leavePlayTimeTicks()<0
                        || !Set.of("COMPLETE","OPEN","INTERRUPTED","INVALID").contains(s.quality())
                        || s.joinGameMode()==null || s.leaveGameMode()==null || s.reason()==null
                        || s.quality().equals("COMPLETE") && (s.leaveEpochMillis()<s.joinEpochMillis() || s.leavePlayTimeTicks()<s.joinPlayTimeTicks()))
                    throw new IllegalArgumentException("Invalid session");
                last=s.joinEpochMillis();
            }
        });
    }
    public void write(Data snapshot) throws IOException {
        if(!available) throw new IOException("Social store disabled"); validate(snapshot);
        Files.createDirectories(file.toAbsolutePath().getParent()); var temp=Files.createTempFile(file.toAbsolutePath().getParent(),"social-",".tmp");
        try { Files.writeString(temp,JSON.toJson(snapshot));
            try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
