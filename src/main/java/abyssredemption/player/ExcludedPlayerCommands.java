package abyssredemption.player;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ExcludedPlayerCommands {
    private ExcludedPlayerCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        var node = Commands.literal("excludePlayer").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    ExcludedPlayers.get().observe(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal(ExcludedPlayers.get().describe()), false);
                    return 1;
                });
        for (boolean exclude : new boolean[]{true, false}) {
            node.then(Commands.literal(exclude ? "add" : "remove")
                    .then(Commands.argument("nameOrUuid", StringArgumentType.word()).executes(context -> {
                        String identity = StringArgumentType.getString(context, "nameOrUuid");
                        try {
                            ExcludedPlayers.get().edit(identity, exclude);
                            context.getSource().sendSuccess(() -> Component.literal((exclude ? "已排除统计: " : "已移除排除项: ") + identity), true);
                            return 1;
                        } catch (java.io.IOException exception) {
                            context.getSource().sendFailure(Component.literal("排除名单保存失败，设置未修改。请检查服务器日志及文件权限。"));
                            abyssredemption.AbsServerTool.LOGGER.warn("Cannot update player exclusions", exception);
                            return 0;
                        }
                    })));
        }
        return node;
    }
}
