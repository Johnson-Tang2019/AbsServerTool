package abyssredemption.player;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class PlayerNameResolver {
    public String resolve(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getGameProfile().name();
        var user = server.getProfileCache().get(uuid);
        return user.map(profile -> profile.name()).orElse("Unknown-" + uuid.toString().substring(0, 8));
    }
}
