package abyssredemption.config;

import abyssredemption.AbsServerTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AbsServerConfig config = AbsServerConfig.defaults();
    private ConfigManager() {}

    public static void load() {
        Path path = Path.of("config", "absservertool.json");
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.writeString(path, GSON.toJson(config), StandardCharsets.UTF_8);
            } else {
                AbsServerConfig loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), AbsServerConfig.class);
                if (loaded != null) config = normalize(loaded);
            }
        } catch (Exception e) {
            AbsServerTool.LOGGER.warn("Malformed AbsServerTool config; using defaults", e);
        }
    }

    public static AbsServerConfig get() { return config; }

    private static AbsServerConfig normalize(AbsServerConfig value) {
        AbsServerConfig defaults = AbsServerConfig.defaults();
        var leaderboard = value.leaderboard() == null ? defaults.leaderboard() : value.leaderboard();
        leaderboard = new AbsServerConfig.Leaderboard(Math.max(1, Math.min(100, leaderboard.pageSize())), Math.max(1, leaderboard.cacheTtlSeconds()), leaderboard.includeZeroValues());
        var network = value.network() == null ? defaults.network() : value.network();
        network = new AbsServerConfig.Network(network.enabled(), Math.max(0, network.minimumRequestIntervalMillis()));
        var statistics = value.statistics() == null ? defaults.statistics() : value.statistics();
        statistics = new AbsServerConfig.Statistics(statistics.includeModdedBlockItems(), Math.max(1, statistics.currentStatsCacheSeconds()));
        var snapshot = value.snapshot() == null ? defaults.snapshot() : value.snapshot();
        snapshot = new AbsServerConfig.Snapshot(snapshot.timezone(), Math.max(0, snapshot.retentionDays()));
        return new AbsServerConfig(leaderboard,
                value.display() == null ? defaults.display() : value.display(),
                value.placements() == null ? defaults.placements() : value.placements(),
                network, statistics, snapshot);
    }
}
