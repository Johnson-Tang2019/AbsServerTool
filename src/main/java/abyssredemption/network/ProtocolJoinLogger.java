package abyssredemption.network;

import abyssredemption.AbsServerTool;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Log a successful protocol handshake once per player connection. */
public final class ProtocolJoinLogger {
    private static final Set<ServerPlayer> LOGGED_PLAYERS = Collections.newSetFromMap(new WeakHashMap<>());

    private ProtocolJoinLogger() {}

    public static void onSuccessfulHello(ServerPlayer player) {
        synchronized (LOGGED_PLAYERS) {
            if (!LOGGED_PLAYERS.add(player)) return;
        }
        AbsServerTool.LOGGER.info("玩家 {} ({}) 已加入世界，AbsServerTool 协议 v{} Hello 校验通过",
                player.getName().getString(), player.getUUID(), ProtocolConstants.PROTOCOL_VERSION);
    }
}
