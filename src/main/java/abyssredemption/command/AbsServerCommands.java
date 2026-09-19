package abyssredemption.command;

import abyssredemption.stats.LeaderboardService;
import abyssredemption.util.MessageFormatter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class AbsServerCommands {
    private static final LeaderboardService LEADERBOARDS = new LeaderboardService();
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
        dispatcher.register(Commands.literal("absserver")
                .then(Commands.literal("playtime").executes(context -> showPlaytime(context.getSource(), 1)).then(page))
                .then(Commands.literal("deaths").executes(context -> showDeaths(context.getSource(), 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(context -> showDeaths(context.getSource(), IntegerArgumentType.getInteger(context, "page")))))
                .then(Commands.literal("blocks").then(Commands.literal("placed").then(total).then(weekly))));
    }

    private static int showPlaytime(CommandSourceStack source, int page) {
        if (page < 1) return invalidPage(source);
        var result = LEADERBOARDS.playtime(source.getServer(), page); if (result.page() != page) return outOfRange(source, result.totalPages());
        MessageFormatter.sendLeaderboard(source, "在线时间榜", result, true); return 1;
    }
    private static int showDeaths(CommandSourceStack source, int page) {
        if (page < 1) return invalidPage(source);
        var result = LEADERBOARDS.deaths(source.getServer(), page); if (result.page() != page) return outOfRange(source, result.totalPages());
        MessageFormatter.sendLeaderboard(source, "死亡榜", result, false); return 1;
    }
    private static int showBlocks(CommandSourceStack source, int page, boolean weekly) {
        if (page < 1) return invalidPage(source);
        var result = weekly ? LEADERBOARDS.blocksWeekly(source.getServer(), page) : LEADERBOARDS.blocksTotal(source.getServer(), page);
        if (result.page() != page) return outOfRange(source, result.totalPages());
        MessageFormatter.sendLeaderboard(source, weekly ? "本周方块放置量" : "方块总放置量", result, false);
        return 1;
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
