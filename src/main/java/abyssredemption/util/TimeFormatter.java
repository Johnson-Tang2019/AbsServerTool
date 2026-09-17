package abyssredemption.util;

public final class TimeFormatter {
    private TimeFormatter() {}
    public static String formatTicks(long ticks) {
        long seconds = Math.max(0, ticks) / 20;
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;
        if (days > 0) return "%d天 %d小时 %d分".formatted(days, hours, minutes);
        if (hours > 0) return "%d小时 %d分".formatted(hours, minutes);
        if (minutes > 0) return "%d分 %d秒".formatted(minutes, seconds);
        return "%d秒".formatted(seconds);
    }
}
