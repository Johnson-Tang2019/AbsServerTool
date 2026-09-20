package abyssredemption.statistics;

public record ServerOverview(int onlinePlayers, int knownPlayers, int dau, int wau, long todayPlayTimeTicks, long weekPlayTimeTicks, long todayPlacements, long weekPlacements, long todayDeaths, long weekDeaths, boolean todayPartial, boolean weekPartial, String dateKey, String weekKey) {}
