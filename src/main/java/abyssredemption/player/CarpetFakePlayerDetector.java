package abyssredemption.player;

import net.minecraft.server.level.ServerPlayer;

/** Detects Carpet fake players without requiring Carpet as a compile dependency. */
public final class CarpetFakePlayerDetector {
    private CarpetFakePlayerDetector() {}

    public static boolean isFake(ServerPlayer player) {
        for (Class<?> type = player.getClass(); type != null; type = type.getSuperclass()) {
            if ("carpet.patches.EntityPlayerMPFake".equals(type.getName())) return true;
        }
        return false;
    }

    public static boolean isShadow(ServerPlayer player) {
        if (!isFake(player)) return false;
        try { return player.getClass().getField("isAShadow").getBoolean(player); }
        catch (ReflectiveOperationException exception) { return false; }
    }
}
