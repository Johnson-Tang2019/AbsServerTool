package abyssredemption.player;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;

public final class PlayerNameResolver {
    private final Map<UUID, String> userCache = new HashMap<>();
    private long userCacheLastModified = Long.MIN_VALUE;

    public String resolve(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getGameProfile().name();
        loadUserCache(server);
        String cachedName = userCache.get(uuid);
        if (cachedName != null && !cachedName.isBlank()) return cachedName;
        return "Unknown-" + uuid.toString().substring(0, 8);
    }

    private void loadUserCache(MinecraftServer server) {
        try {
            var path = server.getServerDirectory().resolve("usercache.json");
            long modified = Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : 0L;
            if (modified == userCacheLastModified) return;
            userCache.clear();
            if (Files.exists(path)) {
                var json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
                if (json.isJsonArray()) for (var entry : json.getAsJsonArray()) {
                    if (!entry.isJsonObject() || !entry.getAsJsonObject().has("uuid") || !entry.getAsJsonObject().has("name")) continue;
                    try { userCache.put(UUID.fromString(entry.getAsJsonObject().get("uuid").getAsString()), entry.getAsJsonObject().get("name").getAsString()); }
                    catch (IllegalArgumentException ignored) { }
                }
            }
            userCacheLastModified = modified;
        } catch (Exception ignored) {
            // A missing or malformed user cache must not break statistics output.
        }
    }
}
