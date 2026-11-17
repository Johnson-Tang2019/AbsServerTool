package abyssredemption.social;

import abyssredemption.social.SocialSnapshots.*;
import abyssredemption.social.SocialModels.*;
import abyssredemption.social.network.SocialPayloads;
import abyssredemption.player.ExcludedPlayers;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public final class SocialVerification {
    private static final long DAY=86400000L,NOW=Instant.parse("2026-10-26T12:00:00Z").toEpochMilli(),MEMBER=3600000;
    private static final UUID ID=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final SocialConfig CONFIG=SocialConfig.defaults();
    private static int assertions;
    public static void main(String[] args) throws Exception {
        algorithms(); persistence(); wire();
        System.out.println("SOCIAL_VERIFICATION_PASSED: "+assertions+" assertions; eligibility, visits, missing baselines, mode history, counter resets, persistence, exclusion, pagination and strict codec");
    }
    private static SocialVanillaCounters counters(long play,long foot,long actions,long elytra) {
        return new SocialVanillaCounters(play,foot,0,0,elytra,0,0,0,actions,0,0,0,0);
    }
    private static Session session(long join,long minutes,long ticks,String reason) {
        return new Session(join,join+minutes*60000,ticks,ticks+minutes*1200,"survival","survival","COMPLETE",reason,true,false);
    }
    private static Data data(long lastJoin,List<Session> sessions,Map<String,Day> days) {
        return new Data(1,"UTC","fixture",NOW-100*DAY,Map.copyOf(days),Map.of(ID.toString(),new Presence(NOW-100*DAY,lastJoin,lastJoin+60000,List.copyOf(sessions),0)));
    }
    private static Data window(boolean modeKnown,boolean restricted) {
        var days=new HashMap<String,Day>(); var today=Instant.ofEpochMilli(NOW).atZone(ZoneOffset.UTC).toLocalDate();
        for(int i=0;i<=14;i++) {
            int active=Math.min(i,3); var value=counters(MEMBER+active*96000L,100+active*500000L/3,active*16/3,0);
            days.put(today.minusDays(14-i).toString(),new Day(NOW-(14-i)*DAY,false,Map.of(ID.toString(),value)));
        }
        var s=new Session(NOW-13*DAY,NOW-DAY,MEMBER,MEMBER+288000,"survival","survival","COMPLETE","LOGOUT",modeKnown,restricted);
        return data(NOW-DAY,List.of(s),days);
    }
    private static void algorithms() {
        var full=counters(MEMBER,0,0,0);
        var absent=SocialAlgorithms.pigeon(CONFIG,data(NOW-10*DAY,List.of(),Map.of()),ID.toString(),full,NOW,false);
        check(absent.subtype()==1 && absent.score()>=40 && absent.score()<100,"A absent");
        check(SocialAlgorithms.pigeon(CONFIG,data(NOW-70*DAY,List.of(),Map.of()),ID.toString(),full,NOW,false).state().equals("ARCHIVED"),"B archived");
        var shorts=new ArrayList<Session>(); for(int i=5;i>0;i--) shorts.add(session(NOW-i*DAY,1,MEMBER+i*1200,"LOGOUT"));
        var flash=SocialAlgorithms.pigeon(CONFIG,data(NOW-DAY,shorts,Map.of()),ID.toString(),counters(MEMBER+20000,0,0,0),NOW,false);
        check(flash.subtype()==2 && flash.score()==75 && flash.shortVisits()==5,"C flash");
        long start=NOW-DAY;
        var visits=SocialAlgorithms.visits(List.of(session(start,1,1000,"LOGOUT"),session(start+3*60000,20,2200,"LOGOUT")),3);
        check(visits.size()==1 && visits.getFirst().playTicks()==21*1200L,"D reconnect, offline gap not counted");
        var open=new Session(start,0,MEMBER,0,"survival","survival","INTERRUPTED","CRASH",false,false);
        check(SocialAlgorithms.pigeon(CONFIG,data(NOW-DAY,List.of(open),Map.of()),ID.toString(),full,NOW,false).state().equals("INCOMPLETE"),"E interrupted");
        var young=new Data(1,"UTC","fixture",NOW-2*DAY,Map.of(),data(NOW-DAY,List.of(),Map.of()).presence());
        check(SocialAlgorithms.pigeon(CONFIG,young,ID.toString(),full,NOW,false).state().equals("OBSERVING"),"F observation grace");
        var current=counters(MEMBER+288000,500100,16,0); var w=SocialAlgorithms.wanderer(CONFIG,window(true,false),ID.toString(),current,NOW);
        check(w.subtype()==3 && w.score()==56 && w.activeDays()==3,"G typical wanderer exact formula");
        var zeroWindow=window(true,false);
        check(!SocialAlgorithms.wanderer(CONFIG,zeroWindow,ID.toString(),counters(MEMBER+1080000,3100,16,0),NOW).ranked(),"H idle excluded");
        check(!SocialAlgorithms.wanderer(CONFIG,zeroWindow,ID.toString(),counters(MEMBER+288000,30100,16,2500000),NOW).ranked(),"I elytra excluded");
        check(SocialAlgorithms.wanderer(CONFIG,zeroWindow,ID.toString(),counters(MEMBER+288000,500100,400,0),NOW).reason().equals("HIGH_OPERATION_RATE"),"J builders excluded");
        var unknown=new Data(1,"UTC","fixture",NOW-100*DAY,Map.of(),Map.of());
        check(SocialAlgorithms.pigeon(CONFIG,unknown,ID.toString(),full,NOW,false).reason().equals("NOT_OBSERVED"),"K no fabricated lastSeen");
        check(SocialAlgorithms.wanderer(CONFIG,zeroWindow,ID.toString(),counters(1,1,0,0),NOW).reason().equals("COUNTER_REGRESSION"),"L reset incomplete");
        var result=SocialLeaderboardService.calculate(CONFIG,zeroWindow,Map.of(ID,current),Map.of(ID,"中文玩家"),Set.of(ID),new ExcludedPlayers.Snapshot(Set.of(ID),Set.of()),NOW);
        check(result.boards().values().stream().allMatch(List::isEmpty) && result.wanderer().get(ID).state().equals("EXCLUDED"),"M fake exclusion");
        result=SocialLeaderboardService.calculate(CONFIG,zeroWindow,Map.of(ID,current),Map.of(ID,"中文玩家"),Set.of(ID),new ExcludedPlayers.Snapshot(Set.of(),Set.of()),NOW);
        check(result.boards().get(Board.STREET_WANDERER).size()==1,"N UUID identity");
        check(!SocialAlgorithms.wanderer(CONFIG,data(NOW-DAY,List.of(),Map.of()),ID.toString(),current,NOW).ranked(),"P no baseline fallback");
        check(SocialAlgorithms.wanderer(CONFIG,window(false,false),ID.toString(),current,NOW).reason().equals("UNSUPPORTED_GAMEMODE"),"Q unknown mode");
        check(SocialAlgorithms.wanderer(CONFIG,window(true,true),ID.toString(),current,NOW).reason().equals("UNSUPPORTED_GAMEMODE"),"Q creative and back is remembered");
        check(counters(144000,1000000,0,0).playTimeTicks()/72000==2 && counters(0,1000000,0,0).onFootCm()/100000==10,"Units");
        var page=SocialLeaderboardService.page(CONFIG,"UTC",result,Board.STREET_WANDERER,1);
        check(page.entries().getFirst().playerName().equals("中文玩家"),"Unicode names");
        var finalResult=result;
        rejects(()->SocialLeaderboardService.page(CONFIG,"UTC",finalResult,Board.STREET_WANDERER,Integer.MAX_VALUE),"Pagination overflow");
        rejects(()->new SocialVanillaCounters(0,Long.MAX_VALUE,1,0,0,0,0,0,0,0,0,0,0).onFootCm(),"Counter sum overflow");
        var stopped=new ArrayList<>(shorts); stopped.add(session(NOW-2*DAY+3600000,20,MEMBER,"SERVER_STOP"));
        check(!SocialAlgorithms.pigeon(CONFIG,data(NOW-DAY,stopped,Map.of()),ID.toString(),counters(MEMBER+100000,0,0,0),NOW,false).ranked(),"Server stop does not erase meaningful visit");
        check(!SocialAlgorithms.pigeon(CONFIG,data(NOW-DAY,shorts,Map.of()),ID.toString(),full,NOW,true).ranked(),"Open visit cannot be flash");
        var large=new HashMap<UUID,SocialVanillaCounters>(); for(int i=0;i<1000;i++) large.put(new UUID(0,i+1),full);
        long then=System.nanoTime(); var stress=SocialLeaderboardService.calculate(CONFIG,unknown,large,Map.of(),Set.of(),new ExcludedPlayers.Snapshot(Set.of(),Set.of()),NOW);
        check(stress.pigeon().size()==1000 && System.nanoTime()-then<5_000_000_000L,"1000 identity pure calculation bound");
    }
    private static void persistence() throws Exception {
        var directory=Files.createTempDirectory("social-verification-"); var file=directory.resolve("social.json");
        try {
            var store=new SocialSnapshotStore(file,CONFIG,"UTC","fixture",NOW-100*DAY);
            check(store.join(ID,counters(MEMBER,0,0,0),"survival",NOW-DAY),"Join snapshot");
            check(!store.join(ID,counters(MEMBER,0,0,0),"survival",NOW-DAY+1),"Duplicate join is idempotent");
            store.mode(ID,"creative"); store.mode(ID,"survival");
            store.write(store.data()); var recovered=new SocialSnapshotStore(file,CONFIG,"UTC","fixture",NOW);
            check(recovered.data().presence().get(ID.toString()).sessions().getFirst().quality().equals("INTERRUPTED"),"Crash recovery");
            check(recovered.data().presence().get(ID.toString()).sessions().getFirst().restrictedModeSeen(),"Intermediate creative mode persists");
            recovered.join(ID,counters(MEMBER,0,0,0),"survival",NOW);
            check(recovered.leave(ID,counters(MEMBER+1200,0,0,0),"survival",NOW+60000,"SERVER_STOP"),"Stop snapshot");
            check(!recovered.leave(ID,counters(MEMBER+1200,0,0,0),"survival",NOW+60001,"LOGOUT"),"Duplicate leave is idempotent");
            var date=Instant.ofEpochMilli(NOW).atZone(ZoneOffset.UTC).toLocalDate();
            recovered.baseline(date,true,Map.of(ID,counters(MEMBER,0,0,0)),NOW);
            recovered.write(recovered.data()); var restart=new SocialSnapshotStore(file,CONFIG,"UTC","fixture",NOW);
            check(restart.data().dailyBaselines().get(date.toString()).partial(),"O restart midday remains partial");
            var changed=new SocialSnapshotStore(file,CONFIG,"UTC","changed-registry",NOW+1);
            check(changed.data().observationStartedAtEpochMillis()==NOW+1 && changed.data().dailyBaselines().get(date.toString()).partial(),"Registry migration restarts observation");
            Files.writeString(file,"{broken"); boolean failed=false;
            try { new SocialSnapshotStore(file,CONFIG,"UTC","fixture",NOW); } catch(java.io.IOException expected) { failed=true; }
            check(failed && Files.readString(file).equals("{broken"),"Corrupt store preserved");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
    }
    private static RegistryFriendlyByteBuf buffer() { return new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY); }
    private static void wire() throws Exception {
        var request=new SocialPayloads.Request(2,300,3,2); var b=buffer();
        try {
            SocialPayloads.Request.CODEC.encode(b,request); byte[] bytes=new byte[b.readableBytes()]; b.getBytes(0,bytes);
            check(java.util.HexFormat.of().formatHex(bytes).equals("02ac020302"),"Request golden bytes");
            Files.write(Path.of("social-request.bin"),bytes);
            check(SocialPayloads.Request.CODEC.decode(b).equals(request) && b.readableBytes()==0,"Request consumes exact bytes");
        } finally { b.release(); }
        var entry=new SocialPayloads.Entry(1,ID,"中文玩家",56,3,NOW-DAY,1,288000,500000,16,3,0,true);
        var response=new SocialPayloads.Response(2,300,3,1,1,1,NOW,NOW-14*DAY,NOW,true,List.of(entry)); b=buffer();
        try {
            SocialPayloads.Response.CODEC.encode(b,response); byte[] bytes=new byte[b.readableBytes()]; b.getBytes(0,bytes); Files.write(Path.of("social-response.bin"),bytes);
            check(SocialPayloads.Response.CODEC.decode(b).equals(response) && b.readableBytes()==0,"Response roundtrip exact bytes");
            for(int count:new int[]{-1,51}) {
                var invalid=buffer(); try { header(invalid,count); rejects(()->SocialPayloads.Response.CODEC.decode(invalid),"Reject count "+count); } finally { invalid.release(); }
            }
            for(int board:new int[]{-1,4}) rejects(()->new SocialPayloads.Response(2,1,board,1,1,0,NOW,0,NOW,true,List.of()),"Unknown board");
            rejects(()->new SocialPayloads.Entry(1,ID,"Player",101,3,0,0,0,0,0,0,0,false),"Score bound");
            var invalid=buffer(); try {
                header(invalid,1); invalid.writeVarInt(1); invalid.writeUUID(ID); invalid.writeVarInt(2); invalid.writeByte(0xc3); invalid.writeByte(0x28);
                rejects(()->SocialPayloads.Response.CODEC.decode(invalid),"Malformed UTF-8");
            } finally { invalid.release(); }
        } finally { b.release(); }
    }
    private static void header(RegistryFriendlyByteBuf b,int count) {
        b.writeVarInt(2); b.writeVarLong(300); b.writeVarInt(3); b.writeVarInt(1); b.writeVarInt(1); b.writeVarInt(1);
        b.writeLong(NOW); b.writeLong(0); b.writeLong(NOW); b.writeBoolean(true); b.writeVarInt(count);
    }
    private static void check(boolean condition,String message) { assertions++; if(!condition) throw new AssertionError(message); }
    private static void rejects(Runnable action,String message) { boolean rejected=false; try { action.run(); } catch(RuntimeException expected) { rejected=true; } check(rejected,message); }
}
