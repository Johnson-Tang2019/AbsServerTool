package abyssredemption.mixin.performance;

import abyssredemption.performance.PerformanceProfiler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public abstract class LevelBlockEntityPerformanceMixin {
    @Redirect(method = "tickBlockEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void absservertool$timeBlockEntity(TickingBlockEntity ticker) {
        if (!PerformanceProfiler.isRunning()) { ticker.tick(); return; }
        long start = System.nanoTime();
        try { ticker.tick(); }
        finally {
            if ((Object) this instanceof ServerLevel level)
                PerformanceProfiler.record(level, ticker.getPos(), PerformanceProfiler.Category.BLOCK_ENTITY,
                        System.nanoTime() - start, ticker.getType());
        }
    }
}
