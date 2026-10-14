package abyssredemption.optimization;

import abyssredemption.config.ConfigManager;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class PiglinOptimizationCommands {
    private PiglinOptimizationCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("piglinOptimization").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> status(context.getSource()))
                .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                    var config = ConfigManager.get().piglinOptimization();
                    return set(context.getSource(), BoolArgumentType.getBool(context, "enabled"), config.entityLimit());
                }))
                .then(Commands.literal("limit").then(Commands.argument("count", IntegerArgumentType.integer(1, 10000))
                        .executes(context -> set(context.getSource(), ConfigManager.get().piglinOptimization().enabled(),
                                IntegerArgumentType.getInteger(context, "count")))));
    }
    private static int status(CommandSourceStack source) {
        var settings = ConfigManager.get().piglinOptimization();
        source.sendSuccess(() -> Component.literal("猪灵交易优化: " + (settings.enabled() ? "开启" : "关闭")
                + " · 堆叠阈值: " + settings.entityLimit() + " · 检查间隔: 100 刻"), false);
        return 1;
    }
    private static int set(CommandSourceStack source, boolean enabled, int limit) {
        try {
            ConfigManager.setPiglinOptimization(enabled, limit);
            source.sendSuccess(() -> Component.literal("猪灵交易优化设置已保存。开启后会限制堆叠猪灵的自主移动、感知与挤压。"), true);
            return status(source);
        } catch (java.io.IOException e) {
            abyssredemption.AbsServerTool.LOGGER.error("Cannot save piglin optimization settings", e);
            source.sendFailure(Component.literal("无法保存配置，设置未修改。"));
            return 0;
        }
    }
}
