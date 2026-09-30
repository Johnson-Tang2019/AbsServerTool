package abyssredemption.waypoint;

import java.util.UUID;

public record SharedWaypoint(UUID id, String name, String dimensionId, int x, int y, int z, int color,
                             String symbol, boolean enabled, UUID createdBy, long createdAt, long updatedAt, long revision) {
}
