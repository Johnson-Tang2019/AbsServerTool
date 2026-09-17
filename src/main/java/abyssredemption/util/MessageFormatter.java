package abyssredemption.util;

import abyssredemption.stats.LeaderboardEntry;
import abyssredemption.stats.LeaderboardPage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class MessageFormatter {
    private MessageFormatter() {}
    public static void sendLeaderboard(net.minecraft.commands.CommandSourceStack source, String title, LeaderboardPage page, boolean time) {
        source.sendSuccess(() -> Component.literal("========== " + title + " ==========").withStyle(ChatFormatting.GOLD), false);
        if (page.totalEntries() == 0) {
            source.sendSuccess(() -> Component.literal(title + "暂无数据。").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        int start = (page.page() - 1) * page.entries().size();
        for (int i = 0; i < page.entries().size(); i++) {
            LeaderboardEntry entry = page.entries().get(i);
            long value = entry.value();
            String rendered = time ? TimeFormatter.formatTicks(value) : String.format("%,d", value);
            source.sendSuccess(() -> Component.literal("#" + (start + i + 1) + " " + entry.playerName() + "  " + rendered)
                    .withStyle(ChatFormatting.WHITE), false);
        }
        source.sendSuccess(() -> Component.literal("------------------------------").withStyle(ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal("第 " + page.page() + " / " + page.totalPages() + " 页 · 共 " + page.totalEntries() + " 名玩家")
                .withStyle(ChatFormatting.GRAY), false);
    }
}
