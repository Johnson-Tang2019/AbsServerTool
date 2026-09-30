package abyssredemption.network.payload;

import abyssredemption.waypoint.SharedWaypoint;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class SharedWaypointCodec {
    private SharedWaypointCodec() {}

    public static void write(RegistryFriendlyByteBuf b, SharedWaypoint w) {
        b.writeUUID(w.id()); b.writeUtf(w.name(), 64); b.writeUtf(w.dimensionId(), 128);
        b.writeInt(w.x()); b.writeInt(w.y()); b.writeInt(w.z()); b.writeInt(w.color());
        b.writeUtf(w.symbol(), 8); b.writeBoolean(w.enabled()); b.writeUUID(w.createdBy());
        b.writeLong(w.createdAt()); b.writeLong(w.updatedAt()); b.writeVarLong(w.revision());
    }

    public static SharedWaypoint read(RegistryFriendlyByteBuf b) {
        return new SharedWaypoint(b.readUUID(), b.readUtf(64), b.readUtf(128), b.readInt(), b.readInt(), b.readInt(),
                b.readInt(), b.readUtf(8), b.readBoolean(), b.readUUID(), b.readLong(), b.readLong(), b.readVarLong());
    }
}
