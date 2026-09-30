package abyssredemption.waypoint;

import abyssredemption.AbsServerTool;
import abyssredemption.config.ConfigManager;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Runs on the server thread; changes are broadcast only after a successful disk commit. */
public final class SharedWaypointService {
    public enum Status {
        SUCCESS(0), PERMISSION_DENIED(1), NOT_FOUND(2), REVISION_CONFLICT(3), INVALID_DATA(4),
        LIMIT_REACHED(5), RATE_LIMITED(6), DISABLED(7), STORAGE_ERROR(8);
        private final int code;
        Status(int code) { this.code = code; }
        public int code() { return code; }
    }
    public record Change(Status status, SharedWaypoint waypoint, UUID deletedId, long waypointRevision, long datasetRevision, int operation) {}
    private static final Map<MinecraftServer, SharedWaypointStore.Data> CACHE = new WeakHashMap<>();
    private SharedWaypointService() {}

    public static boolean canManage(ServerPlayer player) {
        int level = ConfigManager.get().waypoints().managePermissionLevel();
        var permission = switch (level) {
            case 2 -> Commands.LEVEL_GAMEMASTERS;
            case 3 -> Commands.LEVEL_ADMINS;
            default -> Commands.LEVEL_OWNERS;
        };
        return Commands.hasPermission(permission).test(player.createCommandSourceStack());
    }

    public static synchronized SharedWaypointStore.Data snapshot(MinecraftServer server) throws IOException {
        SharedWaypointStore.Data current = CACHE.get(server);
        if (current == null) { current = SharedWaypointStore.load(server); CACHE.put(server, current); }
        return current;
    }

    public static synchronized Change mutate(MinecraftServer server, ServerPlayer player, int operation, UUID id,
                                              long expectedRevision, String name, String dimensionId,
                                              int x, int y, int z, int color, String symbol, boolean enabled) {
        if (!ConfigManager.get().waypoints().enabled()) return failure(Status.DISABLED);
        if (!canManage(player)) return failure(Status.PERMISSION_DENIED);
        if (operation < 1 || operation > 3) return failure(Status.INVALID_DATA);
        try {
            var old = snapshot(server);
            if (old.datasetRevision() == Long.MAX_VALUE) return failure(Status.LIMIT_REACHED);
            if (operation != 1 && id == null) return failure(Status.INVALID_DATA);
            SharedWaypoint previous = operation == 1 ? null : old.waypoints().get(id);
            if (operation != 1 && previous == null) return failure(Status.NOT_FOUND);
            if (operation != 1 && previous.revision() != expectedRevision) return failure(Status.REVISION_CONFLICT);
            if (operation == 1 && old.waypoints().size() >= ConfigManager.get().waypoints().maxWaypoints()) return failure(Status.LIMIT_REACHED);
            if (operation != 3 && !valid(name, dimensionId, x, y, z, color, symbol, server)) return failure(Status.INVALID_DATA);
            long now = System.currentTimeMillis();
            SharedWaypoint waypoint = null;
            Map<UUID, SharedWaypoint> next = new LinkedHashMap<>(old.waypoints());
            if (operation == 3) next.remove(id);
            else {
                waypoint = new SharedWaypoint(operation == 1 ? UUID.randomUUID() : id, name, dimensionId, x, y, z,
                        color, symbol, enabled, operation == 1 ? player.getUUID() : previous.createdBy(),
                        operation == 1 ? now : previous.createdAt(), now, operation == 1 ? 1 : previous.revision() + 1);
                if (waypoint.revision() <= 0) return failure(Status.LIMIT_REACHED);
                next.put(waypoint.id(), waypoint);
            }
            SharedWaypointStore.Data updated = new SharedWaypointStore.Data(old.datasetRevision() + 1, Map.copyOf(next));
            SharedWaypointStore.save(server, updated);
            CACHE.put(server, updated);
            return new Change(Status.SUCCESS, waypoint, operation == 3 ? id : null,
                    operation == 3 ? previous.revision() + 1 : waypoint.revision(), updated.datasetRevision(), operation);
        } catch (IOException exception) {
            AbsServerTool.LOGGER.error("Shared waypoint mutation could not be persisted", exception);
            return failure(Status.STORAGE_ERROR);
        }
    }

    private static Change failure(Status status) { return new Change(status, null, null, 0, 0, 0); }

    private static boolean valid(String name, String dimensionId, int x, int y, int z, int color, String symbol,
                                 MinecraftServer server) {
        var config = ConfigManager.get().waypoints();
        if (name == null || name.isBlank() || name.length() > config.maxNameLength() || symbol == null
                || symbol.length() > config.maxSymbolLength() || dimensionId == null || dimensionId.length() > 128
                || color < 0 || color > 0xFFFFFF || Math.abs((long) x) > 30_000_000L || Math.abs((long) z) > 30_000_000L
                || y < -2048 || y > 2048) return false;
        if (name.chars().anyMatch(c -> Character.isISOControl(c)) || symbol.chars().anyMatch(c -> Character.isISOControl(c))) return false;
        Identifier parsed = Identifier.tryParse(dimensionId);
        return parsed != null && parsed.toString().equals(dimensionId)
                && server.levelKeys().stream().anyMatch(key -> key.identifier().equals(parsed));
    }
}
