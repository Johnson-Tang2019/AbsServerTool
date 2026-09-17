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
                if (loaded != null) config = loaded;
            }
        } catch (Exception e) {
            AbsServerTool.LOGGER.warn("Malformed AbsServerTool config; using defaults", e);
        }
    }

    public static AbsServerConfig get() { return config; }
}
