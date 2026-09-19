package abyssredemption.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkRateLimiter {
    private final Map<UUID, Long> lastRequest = new ConcurrentHashMap<>();
    public boolean allow(UUID uuid, long intervalMillis) {
        long now = System.currentTimeMillis();
        Long previous = lastRequest.put(uuid, now);
        return previous == null || now - previous >= intervalMillis;
    }
    public void forget(UUID uuid) { lastRequest.remove(uuid); }
}
