package abyssredemption.statistics;

public record DailyMetric(String dateKey, int activePlayers, long playTimeTicks, long placements, long deaths, boolean complete) {}
