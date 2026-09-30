package abyssredemption.waypoint;

import abyssredemption.AbsServerTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;

/** World-owned authoritative data. A failed load never overwrites the existing file. */
public final class SharedWaypointStore {
    public record Data(long datasetRevision, Map<UUID, SharedWaypoint> waypoints) {}

    private SharedWaypointStore() {}

    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("absservertool_shared_waypoints.json");
    }

    public static Data load(MinecraftServer server) throws IOException {
        Path file = path(server);
        if (!Files.exists(file)) return new Data(0, Map.of());
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != 1) throw new IOException("Unknown shared waypoint schema");
            long revision = root.get("datasetRevision").getAsLong();
            if (revision < 0) throw new IOException("Negative dataset revision");
            Map<UUID, SharedWaypoint> entries = new LinkedHashMap<>();
            for (var element : root.getAsJsonArray("waypoints")) {
                try {
                    JsonObject value = element.getAsJsonObject();
                    SharedWaypoint waypoint = new SharedWaypoint(
                            UUID.fromString(value.get("id").getAsString()), value.get("name").getAsString(),
                            value.get("dimensionId").getAsString(), value.get("x").getAsInt(), value.get("y").getAsInt(),
                            value.get("z").getAsInt(), value.get("color").getAsInt(), value.get("symbol").getAsString(),
                            value.get("enabled").getAsBoolean(), UUID.fromString(value.get("createdBy").getAsString()),
                            value.get("createdAt").getAsLong(), value.get("updatedAt").getAsLong(), value.get("revision").getAsLong());
                    if (waypoint.revision() <= 0 || waypoint.name().isBlank() || waypoint.name().length() > 64
                            || waypoint.symbol().length() > 8 || waypoint.dimensionId().length() > 128
                            || waypoint.color() < 0 || waypoint.color() > 0xFFFFFF
                            || Math.abs((long) waypoint.x()) > 30_000_000L || Math.abs((long) waypoint.z()) > 30_000_000L
                            || waypoint.y() < -2048 || waypoint.y() > 2048
                            || waypoint.name().chars().anyMatch(Character::isISOControl)
                            || waypoint.symbol().chars().anyMatch(Character::isISOControl)
                            || Identifier.tryParse(waypoint.dimensionId()) == null
                            || entries.putIfAbsent(waypoint.id(), waypoint) != null)
                        throw new IllegalArgumentException("Invalid or duplicate waypoint");
                } catch (RuntimeException exception) {
                    AbsServerTool.LOGGER.warn("Skipping malformed shared waypoint in {}", file, exception);
                }
            }
            return new Data(revision, Map.copyOf(entries));
        } catch (RuntimeException exception) {
            throw new IOException("Cannot parse shared waypoint store; refusing to overwrite it", exception);
        }
    }

    public static void save(MinecraftServer server, Data data) throws IOException {
        Path file = path(server);
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("datasetRevision", data.datasetRevision());
        JsonArray entries = new JsonArray();
        for (SharedWaypoint waypoint : data.waypoints().values()) {
            JsonObject value = new JsonObject();
            value.addProperty("id", waypoint.id().toString());
            value.addProperty("name", waypoint.name());
            value.addProperty("dimensionId", waypoint.dimensionId());
            value.addProperty("x", waypoint.x()); value.addProperty("y", waypoint.y()); value.addProperty("z", waypoint.z());
            value.addProperty("color", waypoint.color()); value.addProperty("symbol", waypoint.symbol());
            value.addProperty("enabled", waypoint.enabled()); value.addProperty("createdBy", waypoint.createdBy().toString());
            value.addProperty("createdAt", waypoint.createdAt()); value.addProperty("updatedAt", waypoint.updatedAt());
            value.addProperty("revision", waypoint.revision());
            entries.add(value);
        }
        root.add("waypoints", entries);
        Path temporary = Files.createTempFile(file.getParent(), "absservertool_waypoints_", ".tmp");
        try {
            Files.writeString(temporary, root.toString(), StandardCharsets.UTF_8);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
