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
                .executes(context -> empty(context.getSource(), "方块总放置量统计将在放置追踪启用后显示。"))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> empty(context.getSource(), "方块总放置量统计将在放置追踪启用后显示。")));
        var weekly = Commands.literal("weekly")
                .executes(context -> empty(context.getSource(), "本周方块放置量统计将在放置追踪启用后显示。"));
        dispatcher.register(Commands.literal("absserver")
                .then(Commands.literal("playtime").executes(context -> showPlaytime(context.getSource(), 1)).then(page))
                .then(Commands.literal("deaths").executes(context -> showDeaths(context.getSource(), 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(context -> showDeaths(context.getSource(), IntegerArgumentType.getInteger(context, "page")))))
                .then(Commands.literal("blocks").then(Commands.literal("placed").then(total).then(weekly))));
    }

    private static int showPlaytime(CommandSourceStack source, int page) {
        MessageFormatter.sendLeaderboard(source, "在线时间榜", LEADERBOARDS.playtime(source.getServer(), page), true); return 1;
    }
    private static int showDeaths(CommandSourceStack source, int page) {
        MessageFormatter.sendLeaderboard(source, "死亡榜", LEADERBOARDS.deaths(source.getServer(), page), false); return 1;
    }
    private static int empty(CommandSourceStack source, String message) { source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(message), false); return 1; }
}
