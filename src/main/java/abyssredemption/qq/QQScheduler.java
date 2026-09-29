package abyssredemption.qq;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import net.minecraft.server.MinecraftServer;

public final class QQScheduler {
    private static int ticks;

    private QQScheduler() {}

    public static void tick(MinecraftServer server) {
        if (++ticks % 20 != 0) return;
        QQConfig config = QQConfigStore.get();
        if (!config.enabled || !QQPublisher.configured(config)) return;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(config.timezone));
        LocalDate date = now.toLocalDate();
        if (config.lastAttemptDate.equals(date.toString())) return;
        if (now.getHour() < config.reportHour || now.getHour() == config.reportHour && now.getMinute() < config.reportMinute) return;
        config.lastAttemptDate = date.toString();
        QQConfigStore.save();
        QQPublisher.publish(server, null);
    }
}
