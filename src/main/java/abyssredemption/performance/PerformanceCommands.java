package abyssredemption.performance;

import abyssredemption.config.ConfigManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class PerformanceCommands {
    private PerformanceCommands() {}

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("lag").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("profile")
                        .executes(c -> start(c.getSource(), ConfigManager.get().performanceProfiler().defaultDurationSeconds()))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                .executes(c -> start(c.getSource(), IntegerArgumentType.getInteger(c, "seconds")))))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("stop").executes(c -> stop(c.getSource())))
                .then(Commands.literal("last").executes(c -> last(c.getSource())));
    }

    private static int start(CommandSourceStack source, int seconds) {
        var result = PerformanceProfiler.start(source.getServer(), seconds);
        if (result.errorCode() != 0) return error(source, result.errorCode());
        send(source, "性能分析已开始：Session #" + result.sessionId() + "，目标 " + seconds + " 秒。");
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        var current = PerformanceProfiler.status();
        var result = PerformanceProfiler.stop(current.sessionId());
        if (result.errorCode() != 0) return error(source, result.errorCode());
        send(source, "性能分析已提前停止，报告 #" + result.report().reportId() + "，实际 "
                + String.format("%.2f", result.report().actualDurationMillis() / 1000.0) + " 秒。");
        return 1;
    }

    private static int status(CommandSourceStack source) {
        var value = PerformanceProfiler.status();
        if (value.state() == ProfilerState.IDLE) {
            send(source, "当前没有正在运行的性能分析。最近报告：" + (value.latestReportId() == 0 ? "无" : "#" + value.latestReportId()));
            return 1;
        }
        send(source, "AbsServerTool Profiler · " + value.state() + " · Session #" + value.sessionId());
        send(source, "已运行 " + String.format("%.1f", value.elapsedMillis() / 1000.0) + "s，剩余 "
                + String.format("%.1f", Math.max(0, value.requestedSeconds() * 1000L - value.elapsedMillis()) / 1000.0)
                + "s，目标 " + value.requestedSeconds() + "s");
        return 1;
    }

    private static int last(CommandSourceStack source) {
        LagProfileReport report = PerformanceProfiler.lastReport();
        if (report == null) return error(source, 26);
        var ticks = report.tickSummary();
        send(source, "========== AbsServerTool 性能分析 ==========");
        send(source, "报告 #" + report.reportId() + " · Session #" + report.sessionId() + " · "
                + String.format("%.2f", report.actualDurationMillis() / 1000.0) + "s · Ticks " + ticks.tickCount()
                + (report.stoppedEarly() ? " · 提前停止" : ""));
        send(source, "平均/P50/P95/P99/最大 MSPT：" + ms(ticks.averageNs()) + " / " + ms(ticks.p50Ns())
                + " / " + ms(ticks.p95Ns()) + " / " + ms(ticks.p99Ns()) + " / " + ms(ticks.maxNs()));
        send(source, ">50ms " + ticks.over50ms() + " · >100ms " + ticks.over100ms()
                + " · >200ms " + ticks.over200ms() + " · 保存重叠慢 Tick " + report.saveOverlapSlowTickCount());
        for (var dimension : report.dimensions())
            send(source, "维度 " + dimension.dimension() + "：总 " + ms(dimension.totalTickNs())
                    + "ms，最大 " + ms(dimension.maxTickNs()) + "ms，" + dimension.tickCount() + " ticks");
        send(source, "测得耗时 Top Chunk（并非真实总 CPU 耗时）：");
        for (int i = 0; i < Math.min(3, report.chunks().size()); i++) {
            var chunk = report.chunks().get(i);
            send(source, (i + 1) + ". " + chunk.dimension() + " [" + chunk.chunkX() + "," + chunk.chunkZ()
                    + "] " + ms(chunk.totalMeasuredNs()) + "ms");
        }
        if (!report.players().isEmpty()) send(source, "加载关联玩家（不表示玩家造成卡顿）：");
        for (int i = 0; i < Math.min(3, report.players().size()); i++) {
            var player = report.players().get(i);
            send(source, player.name() + " unique-load=" + player.uniqueLoadAssociations()
                    + " shared-load=" + player.sharedLoadAssociations() + " generated=" + player.generatedLoadAssociations());
        }
        send(source, "使用支持此能力的 AbsMod GUI 查看详细报告。");
        return 1;
    }

    private static int error(CommandSourceStack source, int code) {
        String message = switch (code) {
            case 20 -> "性能分析功能已禁用";
            case 22 -> "分析秒数超出服务器配置范围";
            case 23 -> "已有性能分析正在运行";
            case 24 -> "当前没有正在运行的分析";
            case 25 -> "Session 不匹配";
            case 26 -> "暂无性能报告";
            default -> "性能分析失败（" + code + "）";
        };
        source.sendFailure(Component.literal(message));
        return 0;
    }

    private static String ms(long ns) { return String.format("%.2f", ns / 1_000_000.0); }
    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }
}
