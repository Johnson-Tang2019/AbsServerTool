package abyssredemption.mixin.performance;

import abyssredemption.performance.PerformanceProfiler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class ServerSavePerformanceMixin {
    @Inject(method = "saveEverything", at = @At("HEAD"))
    private void absservertool$saveStart(boolean suppressLog, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir) {
        if (!PerformanceProfiler.isRunning()) return;
        PerformanceProfiler.onSaveStart();
    }

    @Inject(method = "saveEverything", at = @At("RETURN"))
    private void absservertool$saveEnd(boolean suppressLog, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir) {
        if (!PerformanceProfiler.isRunning()) return;
        PerformanceProfiler.onSaveEnd();
    }
}
