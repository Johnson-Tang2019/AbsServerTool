package abyssredemption.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class OfflineStatsReader {
    public PlayerStatSnapshot read(Path file, UUID uuid) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        JsonObject custom = root.has("stats") && root.get("stats").isJsonObject() ? root.getAsJsonObject("stats").getAsJsonObject("minecraft:custom") : null;
        if (custom == null) return new PlayerStatSnapshot(uuid, 0, 0);
        return new PlayerStatSnapshot(uuid, value(custom, "minecraft:play_time"), value(custom, "minecraft:deaths"));
    }
    private long value(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsLong() : 0;
    }
}
