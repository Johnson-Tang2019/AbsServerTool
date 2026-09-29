package abyssredemption.qq;

import abyssredemption.AbsServerTool;
import abyssredemption.statistics.ServerOverview;
import abyssredemption.statistics.StatisticsService;
import abyssredemption.util.TimeFormatter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class QQPublisher {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final StatisticsService STATISTICS = new StatisticsService();

    private QQPublisher() {}

    public static boolean configured(QQConfig config) {
        return !config.napcatUrl.isBlank() && !config.groupIds.isEmpty();
    }

    public static int publish(MinecraftServer server, CommandSourceStack source) {
        QQConfig config = QQConfigStore.get();
        if (!configured(config)) {
            if (source != null) source.sendFailure(Component.literal("请先设置 NapCat 地址和 QQ 群号。"));
            return 0;
        }
        URI endpoint;
        try {
            endpoint = endpoint(config.napcatUrl);
        } catch (IllegalArgumentException exception) {
            if (source != null) source.sendFailure(Component.literal("NapCat 地址无效，请使用 http:// 或 https:// 地址。"));
            return 0;
        }
        ServerOverview overview = STATISTICS.getOverview(server);
        String message = format(overview);
        String token = config.accessToken;
        for (long groupId : config.groupIds) {
            JsonObject body = new JsonObject();
            body.addProperty("group_id", groupId);
            var segments = new com.google.gson.JsonArray();
            JsonObject segment = new JsonObject();
            segment.addProperty("type", "text");
            JsonObject data = new JsonObject();
            data.addProperty("text", message);
            segment.add("data", data);
            segments.add(segment);
            body.add("message", segments);
            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
            if (!token.isBlank()) request.header("Authorization", "Bearer " + token);
            CLIENT.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                    .whenComplete((response, error) -> {
                        if (error != null) {
                            AbsServerTool.LOGGER.warn("Failed to send QQ report to group {}: {}", groupId, error.toString());
                        } else if (response.statusCode() != 200 || !successful(response.body())) {
                            AbsServerTool.LOGGER.warn("NapCat rejected QQ report for group {} (HTTP {})", groupId, response.statusCode());
                        } else {
                            AbsServerTool.LOGGER.info("Sent AbsServerTool report to QQ group {}", groupId);
                        }
                    });
        }
        if (source != null) source.sendSuccess(() -> Component.literal("已提交 " + config.groupIds.size() + " 个 QQ 群的统计消息；发送结果见服务器日志。"), false);
        return 1;
    }

    public static URI endpoint(String baseUrl) {
        URI base = URI.create(baseUrl.trim());
        if (!("http".equalsIgnoreCase(base.getScheme()) || "https".equalsIgnoreCase(base.getScheme()))
                || base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Invalid NapCat HTTP URL");
        }
        String path = base.getPath();
        if (path != null && !path.isEmpty() && !"/".equals(path)) throw new IllegalArgumentException("Expected NapCat HTTP base URL");
        return base.resolve("/send_group_msg");
    }

    private static boolean successful(String body) {
        try {
            JsonObject result = JsonParser.parseString(body).getAsJsonObject();
            return result.has("status") && "ok".equals(result.get("status").getAsString())
                    && (!result.has("retcode") || result.get("retcode").getAsInt() == 0);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String format(ServerOverview value) {
        return "【服务器统计 · " + value.dateKey() + "】\n"
                + "在线 " + value.onlinePlayers() + " 人 · 已知玩家 " + value.knownPlayers() + " 人\n"
                + "今日活跃 " + value.dau() + (value.todayPartial() ? "*" : "") + " 人 · 本周活跃 " + value.wau() + (value.weekPartial() ? "*" : "") + " 人\n"
                + "今日在线 " + TimeFormatter.formatTicks(value.todayPlayTimeTicks()) + " · 本周在线 " + TimeFormatter.formatTicks(value.weekPlayTimeTicks()) + "\n"
                + "今日放置 " + value.todayPlacements() + " · 本周放置 " + value.weekPlacements() + "\n"
                + "今日死亡 " + value.todayDeaths() + " · 本周死亡 " + value.weekDeaths()
                + (value.todayPartial() || value.weekPartial() ? "\n* 数据基线不完整，仅代表当前可追踪时间段。" : "");
    }
}
