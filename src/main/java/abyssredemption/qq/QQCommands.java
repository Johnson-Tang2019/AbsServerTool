package abyssredemption.qq;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.time.ZoneId;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class QQCommands {
    private QQCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("absserverbot")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("url")
                        .then(Commands.argument("address", StringArgumentType.greedyString())
                                .executes(context -> setUrl(context.getSource(), StringArgumentType.getString(context, "address")))))
                .then(Commands.literal("token")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(context -> setToken(context.getSource(), StringArgumentType.getString(context, "value")))))
                .then(Commands.literal("cleartoken").executes(context -> setToken(context.getSource(), "")))
                .then(Commands.literal("group")
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", LongArgumentType.longArg(1))
                                        .executes(context -> addGroup(context.getSource(), LongArgumentType.getLong(context, "id")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", LongArgumentType.longArg(1))
                                        .executes(context -> removeGroup(context.getSource(), LongArgumentType.getLong(context, "id")))))
                )
                .then(Commands.literal("time")
                        .then(Commands.argument("hour", IntegerArgumentType.integer(0, 23))
                                .then(Commands.argument("minute", IntegerArgumentType.integer(0, 59))
                                        .executes(context -> setTime(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "hour"),
                                                IntegerArgumentType.getInteger(context, "minute"))))))
                .then(Commands.literal("timezone")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(context -> setTimezone(context.getSource(), StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("enable").executes(context -> setEnabled(context.getSource(), true)))
                .then(Commands.literal("disable").executes(context -> setEnabled(context.getSource(), false)))
                .then(Commands.literal("send").executes(context -> QQPublisher.publish(context.getSource().getServer(), context.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        QQConfig config = QQConfigStore.get();
        source.sendSuccess(() -> Component.literal("QQ 播报：" + (config.enabled ? "已启用" : "未启用")
                + " · NapCat: " + (config.napcatUrl.isBlank() ? "未配置" : config.napcatUrl)
                + " · Token: " + (config.accessToken.isBlank() ? "未配置" : "已配置")
                + " · 群号: " + config.groupIds
                + " · 时间: " + "%02d:%02d".formatted(config.reportHour, config.reportMinute)
                + " " + config.timezone), false);
        return 1;
    }

    private static int setUrl(CommandSourceStack source, String address) {
        try {
            QQPublisher.endpoint(address);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("请输入 NapCat HTTP 服务端根地址，例如 http://127.0.0.1:3000"));
            return 0;
        }
        QQConfigStore.get().napcatUrl = address.trim();
        QQConfigStore.save();
        return success(source, "NapCat 地址已保存。");
    }

    private static int setToken(CommandSourceStack source, String value) {
        QQConfigStore.get().accessToken = value.trim();
        QQConfigStore.save();
        return success(source, "NapCat Token 已保存；状态命令不会显示其内容。");
    }

    private static int addGroup(CommandSourceStack source, long id) {
        if (!QQConfigStore.get().groupIds.contains(id)) QQConfigStore.get().groupIds.add(id);
        QQConfigStore.save();
        return success(source, "已添加 QQ 群 " + id);
    }

    private static int removeGroup(CommandSourceStack source, long id) {
        QQConfigStore.get().groupIds.remove(id);
        QQConfigStore.save();
        return success(source, "已移除 QQ 群 " + id);
    }

    private static int setTime(CommandSourceStack source, int hour, int minute) {
        QQConfigStore.get().reportHour = hour;
        QQConfigStore.get().reportMinute = minute;
        QQConfigStore.save();
        return success(source, "每日播报时间已设置为 " + "%02d:%02d".formatted(hour, minute));
    }

    private static int setTimezone(CommandSourceStack source, String id) {
        try {
            ZoneId.of(id);
        } catch (Exception exception) {
            source.sendFailure(Component.literal("时区无效，例如 Asia/Shanghai"));
            return 0;
        }
        QQConfigStore.get().timezone = id;
        QQConfigStore.save();
        return success(source, "播报时区已设置为 " + id);
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        if (enabled && !QQPublisher.configured(QQConfigStore.get())) {
            source.sendFailure(Component.literal("请先设置 NapCat 地址和至少一个 QQ 群。"));
            return 0;
        }
        QQConfigStore.get().enabled = enabled;
        QQConfigStore.save();
        return success(source, enabled ? "已启用每日 QQ 播报。" : "已停用每日 QQ 播报。");
    }

    private static int success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
