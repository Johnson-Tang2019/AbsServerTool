package abyssredemption.util;

import abyssredemption.stats.LeaderboardEntry;
import abyssredemption.stats.LeaderboardPage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import abyssredemption.config.ConfigManager;

public final class MessageFormatter {
    private MessageFormatter() {}
    public static void sendLeaderboard(net.minecraft.commands.CommandSourceStack source, String title, LeaderboardPage page, boolean time) {
        source.sendSuccess(() -> Component.literal("========== " + title + " ==========").withStyle(ChatFormatting.GOLD), false);
        if (page.totalEntries() == 0) {
            source.sendSuccess(() -> Component.literal(title + "暂无数据。").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        int start = (page.page() - 1) * Math.max(1, ConfigManager.get().leaderboard().pageSize());
        for (int i = 0; i < page.entries().size(); i++) {
            LeaderboardEntry entry = page.entries().get(i);
            long value = entry.value();
            String rendered = time ? TimeFormatter.formatTicks(value) : String.format("%,d", value);
            int rank = start + i + 1;
            source.sendSuccess(() -> Component.literal("#" + rank + " " + entry.playerName() + "  " + rendered)
                    .withStyle(ChatFormatting.WHITE), false);
        }
        source.sendSuccess(() -> Component.literal("------------------------------").withStyle(ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal("第 " + page.page() + " / " + page.totalPages() + " 页 · 共 " + page.totalEntries() + " 名玩家")
                .withStyle(ChatFormatting.GRAY), false);
    }
}
