package abyssredemption.social;

import abyssredemption.player.ExcludedPlayers;
import abyssredemption.social.SocialModels.*;
import abyssredemption.social.SocialSnapshots.Data;
import java.time.*;
import java.util.*;

/** Pure aggregation shared by commands, payloads and deterministic test fixtures. */
public final class SocialLeaderboardService {
    private SocialLeaderboardService() {}
    public record Result(Map<Board,List<Entry>> boards,Map<UUID,Verdict> pigeon,Map<UUID,Verdict> wanderer,
            Map<UUID,String> names,long now,long started,boolean complete) {}
    public static Result calculate(SocialConfig config,Data data,Map<UUID,SocialVanillaCounters> counters,
            Map<UUID,String> names,Set<UUID> online,ExcludedPlayers.Snapshot excluded,long now) {
        Set<UUID> extra=config.excludedUuids().stream().map(UUID::fromString).collect(java.util.stream.Collectors.toSet());
        var pigeons=new HashMap<UUID,Verdict>(); var wanderers=new HashMap<UUID,Verdict>();
        var boards=new EnumMap<Board,List<Entry>>(Board.class); for(var board:Board.values()) boards.put(board,new ArrayList<>());
        Set<UUID> ids=new HashSet<>(counters.keySet()); data.presence().keySet().forEach(id->ids.add(UUID.fromString(id)));
        boolean complete=now-data.observationStartedAtEpochMillis()>=config.minimumObservationDays()*86400000L;
        for(var id:ids) {
            String name=names.getOrDefault(id,"Unknown-"+id.toString().substring(0,8));
            boolean hidden=excluded.contains(id,name)||extra.contains(id);
            var pigeon=hidden?Verdict.status("EXCLUDED","EXCLUDED"):config.pigeon().enabled()?SocialAlgorithms.pigeon(config,data,id.toString(),counters.get(id),now,online.contains(id)):Verdict.status("NORMAL","DISABLED");
            var wanderer=hidden?Verdict.status("EXCLUDED","EXCLUDED"):config.wanderer().enabled()?SocialAlgorithms.wanderer(config,data,id.toString(),counters.get(id),now):Verdict.status("NORMAL","DISABLED");
            pigeons.put(id,pigeon); wanderers.put(id,wanderer);
            if(pigeon.state().equals("INCOMPLETE") || pigeon.state().equals("OBSERVING") || wanderer.state().equals("INCOMPLETE") || wanderer.state().equals("OBSERVING")) complete=false;
            for(var verdict:List.of(pigeon,wanderer)) if(verdict.ranked()) {
                var entry=new Entry(0,id,name,verdict.score(),verdict.subtype(),data.presence().get(id.toString()).lastJoinAtEpochMillis(),
                        verdict.days(),verdict.playTicks(),verdict.footCm(),verdict.actions(),verdict.activeDays(),verdict.shortVisits(),online.contains(id));
                if(verdict.subtype()==3) boards.get(Board.STREET_WANDERER).add(entry);
                else { boards.get(Board.PIGEON_ALL).add(entry); boards.get(verdict.subtype()==1?Board.PIGEON_ABSENT:Board.PIGEON_FLASH).add(entry); }
            }
        }
        boards.forEach((type,entries)->{
            Comparator<Entry> secondary=type==Board.STREET_WANDERER?Comparator.comparingLong(Entry::onFootDistanceCm).reversed():Comparator.comparingInt(Entry::daysSinceLastSeen).reversed();
            entries.sort(Comparator.comparingInt(Entry::score).reversed().thenComparing(secondary).thenComparing(e->e.uuid().toString()));
            for(int i=0;i<entries.size();i++) entries.set(i,entries.get(i).ranked(i+1));
        });
        var frozen=new EnumMap<Board,List<Entry>>(Board.class); boards.forEach((b,v)->frozen.put(b,List.copyOf(v)));
        return new Result(Map.copyOf(frozen),Map.copyOf(pigeons),Map.copyOf(wanderers),Map.copyOf(names),now,data.observationStartedAtEpochMillis(),complete);
    }
    public static Page page(SocialConfig config,String timezone,Result result,Board type,int page) {
        var entries=result.boards().get(type); int pages=Math.max(1,(entries.size()+config.pageSize()-1)/config.pageSize());
        if(page<1 || page>pages) throw new IllegalArgumentException("SOCIAL_INVALID_PAGE");
        int from=(page-1)*config.pageSize();
        long start=type==Board.STREET_WANDERER?Instant.ofEpochMilli(result.now()).atZone(ZoneId.of(timezone)).toLocalDate()
                .minusDays(config.wanderer().lookbackDays()).atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli():result.now()-config.pigeon().archiveAfterDays()*86400000L;
        return new Page(type.id,page,pages,entries.size(),result.now(),Math.max(0,start),result.now(),result.complete(),List.copyOf(entries.subList(from,Math.min(from+config.pageSize(),entries.size()))));
    }
}
