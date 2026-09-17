package abyssredemption.player;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class PlayerNameResolver {
    public String resolve(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getGameProfile().name();
        return "Unknown-" + uuid.toString().substring(0, 8);
    }
}
