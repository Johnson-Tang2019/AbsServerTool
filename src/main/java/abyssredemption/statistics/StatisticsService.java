package abyssredemption.statistics;

import abyssredemption.config.ConfigManager;
import abyssredemption.snapshot.DailySnapshot;
import abyssredemption.snapshot.VanillaStatSnapshot;
import abyssredemption.snapshot.SnapshotStore;
import abyssredemption.stats.PlayerStatSnapshot;
import abyssredemption.stats.PlayerStatsService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class StatisticsService {
    private final PlayerStatsService current = new PlayerStatsService();
    public ServerOverview getOverview(MinecraftServer server) {
        ZoneId zone = zone(); LocalDate today = LocalDate.now(zone); var todayBase = SnapshotStore.getOrCreateTodayBaseline(server); LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); var weekBase = SnapshotStore.getSnapshot(server, monday.toString()).orElse(null); Map<UUID, PlayerStatSnapshot> now = current.collectAllStats(server);
        long todayTime=0, weekTime=0, todayPlaces=0, weekPlaces=0, todayDeaths=0, weekDeaths=0; int dau=0, wau=0;
        for (var entry : now.entrySet()) { var t = entry.getValue(); var d = todayBase.players().get(entry.getKey().toString()); long dt = delta(t.playTimeTicks(), d == null ? 0 : d.playTimeTicks()); long dd = delta(t.deaths(), d == null ? 0 : d.deaths()); long dp = delta(t.vanillaBlockPlacementCount(), d == null ? 0 : d.vanillaBlockPlacementCount()); todayTime += dt; todayDeaths += dd; todayPlaces += dp; if (dt > 0) dau++; if (weekBase != null) { var w = weekBase.players().get(entry.getKey().toString()); long wt = delta(t.playTimeTicks(), w == null ? 0 : w.playTimeTicks()); weekTime += wt; weekDeaths += delta(t.deaths(), w == null ? 0 : w.deaths()); weekPlaces += delta(t.vanillaBlockPlacementCount(), w == null ? 0 : w.vanillaBlockPlacementCount()); if (wt > 0) wau++; } }
        return new ServerOverview(server.getPlayerList().getPlayerCount(), now.size(), dau, wau, todayTime, weekTime, todayPlaces, weekPlaces, todayDeaths, weekDeaths, todayBase.partial(), weekBase == null || weekBase.partial(), today.toString(), weekKey(today));
    }
    public java.util.Map<UUID, Long> getLeaderboardValues(MinecraftServer server, StatisticsLeaderboardType type) {
        var now = current.collectAllStats(server); var values = new java.util.HashMap<UUID, Long>();
        if (type == StatisticsLeaderboardType.TOTAL_PLAYTIME || type == StatisticsLeaderboardType.TOTAL_DEATHS || type == StatisticsLeaderboardType.TOTAL_PLACEMENTS) {
            for (var e : now.entrySet()) values.put(e.getKey(), switch (type) { case TOTAL_PLAYTIME -> e.getValue().playTimeTicks(); case TOTAL_DEATHS -> e.getValue().deaths(); case TOTAL_PLACEMENTS -> e.getValue().vanillaBlockPlacementCount(); default -> 0L; });
            return Map.copyOf(values);
        }
        ZoneId zone = zone(); LocalDate today = LocalDate.now(zone); LocalDate baseDate = type.name().startsWith("TODAY") ? today : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); var base = SnapshotStore.getSnapshot(server, baseDate.toString()).orElse(null);
        for (var e : now.entrySet()) { var b = base == null ? null : base.players().get(e.getKey().toString()); long value = switch (type) { case TODAY_PLAYTIME, WEEK_PLAYTIME -> delta(e.getValue().playTimeTicks(), b == null ? 0 : b.playTimeTicks()); case TODAY_DEATHS, WEEK_DEATHS -> delta(e.getValue().deaths(), b == null ? 0 : b.deaths()); case TODAY_PLACEMENTS, WEEK_PLACEMENTS -> delta(e.getValue().vanillaBlockPlacementCount(), b == null ? 0 : b.vanillaBlockPlacementCount()); default -> 0L; }; values.put(e.getKey(), value); }
        return Map.copyOf(values);
    }
    public static long delta(long current, long baseline) { return current < baseline ? 0 : current - baseline; }
    public java.util.List<DailyMetric> getDailyMetrics(MinecraftServer server, int days) {
        ZoneId zone = zone(); LocalDate end = LocalDate.now(zone); int count = Math.max(1, Math.min(90, days)); var snapshots = SnapshotStore.getSnapshotsBetween(server, end.minusDays(count), end); var byDate = new java.util.HashMap<String, DailySnapshot>(); snapshots.forEach(s -> byDate.put(s.dateKey(), s)); var result = new ArrayList<DailyMetric>();
        for (int i = count - 1; i >= 0; i--) { LocalDate date = end.minusDays(i); DailySnapshot start = byDate.get(date.toString()); DailySnapshot finish = byDate.get(date.plusDays(1).toString()); if (finish == null && date.equals(end)) { result.add(metricFromCurrent(server, date, start)); continue; } if (start == null || finish == null) { result.add(new DailyMetric(date.toString(), 0, 0, 0, 0, false)); continue; } result.add(metricBetween(start, finish, date.toString())); }
        return List.copyOf(result);
    }
    public java.util.List<WeeklyMetric> getWeeklyMetrics(MinecraftServer server, int weeks) {
        ZoneId zone = zone(); LocalDate end = LocalDate.now(zone); int count = Math.max(1, Math.min(26, weeks)); LocalDate thisMonday = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); var snapshots = SnapshotStore.getSnapshotsBetween(server, thisMonday.minusWeeks(count), thisMonday.plusDays(1)); var byDate = new java.util.HashMap<String, DailySnapshot>(); snapshots.forEach(s -> byDate.put(s.dateKey(), s)); var result = new ArrayList<WeeklyMetric>();
        for (int i = count - 1; i >= 0; i--) { LocalDate monday = thisMonday.minusWeeks(i), next = monday.plusWeeks(1); DailySnapshot start = byDate.get(monday.toString()), finish = byDate.get(next.toString()); if (finish == null && next.isAfter(end)) { result.add(metricWeekFromCurrent(server, monday, start)); continue; } result.add(start == null || finish == null ? new WeeklyMetric(weekKey(monday), 0, 0, 0, 0, false) : metricWeekBetween(start, finish, weekKey(monday))); }
        return List.copyOf(result);
    }
    private DailyMetric metricFromCurrent(MinecraftServer server, LocalDate date, DailySnapshot start) { var now = current.collectAllStats(server); long play=0, place=0, deaths=0; int active=0; for (var e: now.entrySet()) { var base=start == null ? null : start.players().get(e.getKey().toString()); long p=delta(e.getValue().playTimeTicks(), base == null ? 0 : base.playTimeTicks()); if(p>0)active++; play+=p; place+=delta(e.getValue().vanillaBlockPlacementCount(), base == null ? 0 : base.vanillaBlockPlacementCount()); deaths+=delta(e.getValue().deaths(), base == null ? 0 : base.deaths()); } return new DailyMetric(date.toString(), active, play, place, deaths, start != null && !start.partial()); }
    private WeeklyMetric metricWeekFromCurrent(MinecraftServer server, LocalDate monday, DailySnapshot start) { var now=current.collectAllStats(server); long play=0, place=0, deaths=0; int active=0; for(var e:now.entrySet()){var b=start==null?null:start.players().get(e.getKey().toString()); long p=delta(e.getValue().playTimeTicks(),b==null?0:b.playTimeTicks()); if(p>0)active++; play+=p; place+=delta(e.getValue().vanillaBlockPlacementCount(),b==null?0:b.vanillaBlockPlacementCount()); deaths+=delta(e.getValue().deaths(),b==null?0:b.deaths());} return new WeeklyMetric(weekKey(monday),active,play,place,deaths,start!=null&&!start.partial()); }
    private DailyMetric metricBetween(DailySnapshot start, DailySnapshot finish, String date) { long play=0,place=0,deaths=0; int active=0; Set<String> ids=new HashSet<>(finish.players().keySet()); ids.addAll(start.players().keySet()); for(String id:ids){var a=start.players().get(id);var b=finish.players().get(id);long p=delta(b==null?0:b.playTimeTicks(),a==null?0:a.playTimeTicks());if(p>0)active++;play+=p;place+=delta(b==null?0:b.vanillaBlockPlacementCount(),a==null?0:a.vanillaBlockPlacementCount());deaths+=delta(b==null?0:b.deaths(),a==null?0:a.deaths());} return new DailyMetric(date,active,play,place,deaths,!start.partial()&&!finish.partial()); }
    private WeeklyMetric metricWeekBetween(DailySnapshot start, DailySnapshot finish, String week) { var d=metricBetween(start,finish,week); return new WeeklyMetric(week,d.activePlayers(),d.playTimeTicks(),d.placements(),d.deaths(),d.complete()); }
    public static String weekKey(LocalDate date) { return "%d-W%02d".formatted(date.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR), date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)); }
    private ZoneId zone() { String value = ConfigManager.get().snapshot().timezone(); return "SYSTEM".equalsIgnoreCase(value) ? ZoneId.systemDefault() : ZoneId.of(value); }
}
