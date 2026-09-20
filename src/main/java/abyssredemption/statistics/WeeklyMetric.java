package abyssredemption.statistics;

public record WeeklyMetric(String weekKey, int activePlayers, long playTimeTicks, long placements, long deaths, boolean complete) {}
