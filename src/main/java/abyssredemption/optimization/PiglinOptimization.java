package abyssredemption.optimization;

import abyssredemption.config.ConfigManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.piglin.Piglin;

/** Piglin-only state; disabled settings and changed generations take effect immediately. */
public final class PiglinOptimization {
    private static long generation;
    private PiglinOptimization() {}
    public interface State {
        int absservertool$nearbyPiglins();
        long absservertool$generation();
    }
    public static long generation() { return generation; }
    public static void invalidate() { generation++; }
    public static boolean active(Entity entity) {
        return entity instanceof Piglin && !entity.level().isClientSide()
                && ConfigManager.get().piglinOptimization().enabled()
                && entity instanceof State state && state.absservertool$generation() == generation
                && state.absservertool$nearbyPiglins() >= ConfigManager.get().piglinOptimization().entityLimit();
    }
}
