package abyssredemption.network.payload;

import abyssredemption.AbsServerTool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SharedWaypointPermissionsPayload(int protocolVersion, boolean canRead, boolean canManage) implements CustomPacketPayload {
    public static final Type<SharedWaypointPermissionsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, "shared_waypoint_permissions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SharedWaypointPermissionsPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeBoolean(p.canRead()); b.writeBoolean(p.canManage()); },
            b -> new SharedWaypointPermissionsPayload(b.readVarInt(), b.readBoolean(), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
