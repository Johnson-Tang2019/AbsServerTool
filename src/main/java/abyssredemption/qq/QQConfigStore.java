package abyssredemption.qq;

import abyssredemption.AbsServerTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.util.ArrayList;

public final class QQConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "absservertool_qq.json");
    private static QQConfig config = new QQConfig();

    private QQConfigStore() {}

    public static synchronized void load() {
        if (Files.exists(FILE)) {
            try {
                QQConfig parsed = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), QQConfig.class);
                if (parsed != null) {
                    validate(parsed);
                    config = parsed;
                    return;
                }
            } catch (Exception exception) {
                AbsServerTool.LOGGER.warn("Invalid QQ bridge configuration; using defaults", exception);
            }
        }
        config = new QQConfig();
        save();
    }

    public static synchronized QQConfig get() {
        return config;
    }

    public static synchronized void save() {
        try {
            validate(config);
            Files.createDirectories(FILE.getParent());
            Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(config), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            AbsServerTool.LOGGER.error("Unable to save QQ bridge configuration", exception);
        }
    }

    private static void validate(QQConfig value) {
        if (value.napcatUrl == null) value.napcatUrl = "";
        if (value.accessToken == null) value.accessToken = "";
        if (value.groupIds == null) value.groupIds = new ArrayList<>();
        value.groupIds.removeIf(id -> id == null || id <= 0);
        if (value.timezone == null || value.timezone.isBlank()) value.timezone = "Asia/Shanghai";
        ZoneId.of(value.timezone);
        if (value.reportHour < 0 || value.reportHour > 23) value.reportHour = 20;
        if (value.reportMinute < 0 || value.reportMinute > 59) value.reportMinute = 0;
        if (value.lastAttemptDate == null) value.lastAttemptDate = "";
    }
}
