package abyssredemption.util;

public final class PageUtil {
    private PageUtil() {}
    public static int totalPages(int totalEntries, int pageSize) {
        return Math.max(1, (totalEntries + Math.max(1, pageSize) - 1) / Math.max(1, pageSize));
    }
    public static int clamp(int page, int totalPages) { return Math.max(1, Math.min(page, totalPages)); }
}
