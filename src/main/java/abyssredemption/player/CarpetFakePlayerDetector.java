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
}
