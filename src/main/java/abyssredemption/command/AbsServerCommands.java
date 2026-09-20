package abyssredemption.command;

import abyssredemption.config.ConfigManager;
import abyssredemption.statistics.ServerOverview;
import abyssredemption.statistics.StatisticsService;
import abyssredemption.statistics.StatisticsLeaderboardType;
import abyssredemption.stats.LeaderboardEntry;
import abyssredemption.stats.LeaderboardPage;
import java.util.Comparator;
import abyssredemption.util.MessageFormatter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class AbsServerCommands {
    private static final StatisticsService STATISTICS = new StatisticsService();
    private AbsServerCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var page = com.mojang.brigadier.builder.RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("page", IntegerArgumentType.integer(1));
        page.executes(context -> showPlaytime(context.getSource(), IntegerArgumentType.getInteger(context, "page")));
        var total = Commands.literal("total")
                .executes(context -> showBlocks(context.getSource(), 1, false))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> showBlocks(context.getSource(), IntegerArgumentType.getInteger(context, "page"), false)));
        var weekly = Commands.literal("weekly")
                .executes(context -> showBlocks(context.getSource(), 1, true))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> showBlocks(context.getSource(), IntegerArgumentType.getInteger(context, "page"), true)));
        var playtimeBoards = Commands.literal("playtime")
                .then(Commands.literal("total").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.TOTAL_PLAYTIME, "累计在线时间", 1)).then(boardPage(StatisticsLeaderboardType.TOTAL_PLAYTIME, "累计在线时间")))
                .then(Commands.literal("today").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.TODAY_PLAYTIME, "今日在线时间", 1)).then(boardPage(StatisticsLeaderboardType.TODAY_PLAYTIME, "今日在线时间")))
                .then(Commands.literal("week").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.WEEK_PLAYTIME, "本周在线时间", 1)).then(boardPage(StatisticsLeaderboardType.WEEK_PLAYTIME, "本周在线时间")));
        var deathBoards = Commands.literal("deaths")
                .then(Commands.literal("total").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.TOTAL_DEATHS, "累计死亡", 1)).then(boardPage(StatisticsLeaderboardType.TOTAL_DEATHS, "累计死亡")))
                .then(Commands.literal("today").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.TODAY_DEATHS, "今日死亡", 1)).then(boardPage(StatisticsLeaderboardType.TODAY_DEATHS, "今日死亡")))
                .then(Commands.literal("week").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.WEEK_DEATHS, "本周死亡", 1)).then(boardPage(StatisticsLeaderboardType.WEEK_DEATHS, "本周死亡")));
        var placementBoards = Commands.literal("blocks")
                .then(Commands.literal("total").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.TOTAL_PLACEMENTS, "累计方块放置", 1)).then(boardPage(StatisticsLeaderboardType.TOTAL_PLACEMENTS, "累计方块放置")))
                .then(Commands.literal("today").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.TODAY_PLACEMENTS, "今日方块放置", 1)).then(boardPage(StatisticsLeaderboardType.TODAY_PLACEMENTS, "今日方块放置")))
                .then(Commands.literal("week").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.WEEK_PLACEMENTS, "本周方块放置", 1)).then(boardPage(StatisticsLeaderboardType.WEEK_PLACEMENTS, "本周方块放置")));
        dispatcher.register(Commands.literal("absserver")
                .then(Commands.literal("stats").executes(context -> showOverview(context.getSource())))
                .then(playtimeBoards).then(deathBoards).then(placementBoards)
                .then(Commands.literal("activity")
                        .then(Commands.literal("daily").executes(context -> showDailyActivity(context.getSource())))
                        .then(Commands.literal("weekly").executes(context -> showWeeklyActivity(context.getSource()))))
                .then(Commands.literal("playtime").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.TOTAL_PLAYTIME, "累计在线时间", 1)).then(page))
                .then(Commands.literal("deaths").executes(context -> showStatisticsBoard(context.getSource(), StatisticsLeaderboardType.TOTAL_DEATHS, "累计死亡", 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(context -> showDeaths(context.getSource(), IntegerArgumentType.getInteger(context, "page")))))
                .then(Commands.literal("blocks").then(Commands.literal("placed").then(total).then(weekly))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer> boardPage(StatisticsLeaderboardType type, String title) {
        return Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(context -> showStatisticsBoard(context.getSource(), type, title, IntegerArgumentType.getInteger(context, "page")));
    }

    private static int showStatisticsBoard(CommandSourceStack source, StatisticsLeaderboardType type, String title, int page) {
        var values = STATISTICS.getLeaderboardValues(source.getServer(), type);
        var resolver = new abyssredemption.player.PlayerNameResolver();
        var all = values.entrySet().stream().filter(e -> ConfigManager.get().leaderboard().includeZeroValues() || e.getValue() > 0)
                .map(e -> new LeaderboardEntry(e.getKey(), resolver.resolve(source.getServer(), e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(LeaderboardEntry::value).reversed().thenComparing(LeaderboardEntry::playerName, String.CASE_INSENSITIVE_ORDER).thenComparing(e -> e.uuid().toString())).toList();
        int size = Math.max(1, Math.min(100, ConfigManager.get().leaderboard().pageSize())); int totalPages = Math.max(1, (all.size() + size - 1) / size); if (page > totalPages) return outOfRange(source, totalPages); int from = (page - 1) * size; var result = new LeaderboardPage(all.subList(from, Math.min(from + size, all.size())), page, totalPages, all.size());
        MessageFormatter.sendLeaderboard(source, title, result, type == StatisticsLeaderboardType.TOTAL_PLAYTIME || type == StatisticsLeaderboardType.TODAY_PLAYTIME || type == StatisticsLeaderboardType.WEEK_PLAYTIME); return 1;
    }

    private static int showOverview(CommandSourceStack source) {
        ServerOverview o = STATISTICS.getOverview(source.getServer());
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("========== 服务器统计 =========="), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("在线人数: " + o.onlinePlayers() + " · 已知玩家: " + o.knownPlayers()), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("今日活跃: " + o.dau() + (o.todayPartial() ? "*" : "") + " · 本周活跃: " + o.wau() + (o.weekPartial() ? "*" : "")), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("今日在线时长: " + abyssredemption.util.TimeFormatter.formatTicks(o.todayPlayTimeTicks())), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("本周在线时长: " + abyssredemption.util.TimeFormatter.formatTicks(o.weekPlayTimeTicks())), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("今日放置: " + String.format("%,d", o.todayPlacements()) + " · 本周放置: " + String.format("%,d", o.weekPlacements())), false);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("今日死亡: " + o.todayDeaths() + " · 本周死亡: " + o.weekDeaths()), false);
        if (o.todayPartial() || o.weekPartial()) source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("* 数据基线不完整，仅代表当前可追踪时间段。"), false);
        return 1;
    }

    private static int showDailyActivity(CommandSourceStack source) {
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("========== 最近 7 日活跃统计 =========="), false);
        for (var metric : STATISTICS.getDailyMetrics(source.getServer(), 7)) {
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(metric.dateKey() + " · 活跃 " + metric.activePlayers() + " · 在线 " + abyssredemption.util.TimeFormatter.formatTicks(metric.playTimeTicks()) + (metric.complete() ? "" : " *")), false);
        }
        return 1;
    }

    private static int showWeeklyActivity(CommandSourceStack source) {
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("========== 最近 6 周活跃统计 =========="), false);
        for (var metric : STATISTICS.getWeeklyMetrics(source.getServer(), 6)) {
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(metric.weekKey() + " · 活跃 " + metric.activePlayers() + " · 在线 " + abyssredemption.util.TimeFormatter.formatTicks(metric.playTimeTicks()) + (metric.complete() ? "" : " *")), false);
        }
        return 1;
    }

    private static int showPlaytime(CommandSourceStack source, int page) {
        if (page < 1) return invalidPage(source);
        return showStatisticsBoard(source, StatisticsLeaderboardType.TOTAL_PLAYTIME, "累计在线时间", page);
    }
    private static int showDeaths(CommandSourceStack source, int page) {
        if (page < 1) return invalidPage(source);
        return showStatisticsBoard(source, StatisticsLeaderboardType.TOTAL_DEATHS, "累计死亡", page);
    }
    private static int showBlocks(CommandSourceStack source, int page, boolean weekly) {
        if (page < 1) return invalidPage(source);
        return showStatisticsBoard(source, weekly ? StatisticsLeaderboardType.WEEK_PLACEMENTS : StatisticsLeaderboardType.TOTAL_PLACEMENTS, weekly ? "本周方块放置量" : "累计方块放置量", page);
    }
    private static int invalidPage(CommandSourceStack source) {
        source.sendFailure(net.minecraft.network.chat.Component.literal("页码必须大于等于 1。"));
        return 0;
    }
    private static int outOfRange(CommandSourceStack source, int totalPages) {
        source.sendFailure(net.minecraft.network.chat.Component.literal("页码超出范围，当前共 " + totalPages + " 页。"));
        return 0;
    }
    private static int empty(CommandSourceStack source, String message) { source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(message), false); return 1; }
}
