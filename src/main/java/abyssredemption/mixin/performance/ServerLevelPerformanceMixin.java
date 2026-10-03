package abyssredemption.mixin.performance;

import abyssredemption.performance.PerformanceProfiler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelPerformanceMixin {
    @Unique private long absservertool$chunkStart;
    @Unique private long absservertool$entityStart;
    @Unique private long absservertool$blockStart;
    @Unique private long absservertool$fluidStart;

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void absservertool$chunkStart(LevelChunk chunk, int tickSpeed, CallbackInfo ci) {
        if (!PerformanceProfiler.isRunning()) return;
        absservertool$chunkStart = System.nanoTime();
    }

    @Inject(method = "tickChunk", at = @At("RETURN"))
    private void absservertool$chunkEnd(LevelChunk chunk, int tickSpeed, CallbackInfo ci) {
        if (!PerformanceProfiler.isRunning()) return;
        PerformanceProfiler.recordChunk((ServerLevel) (Object) this, chunk.getPos(), System.nanoTime() - absservertool$chunkStart);
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void absservertool$entityStart(Entity entity, CallbackInfo ci) {
        if (!PerformanceProfiler.isRunning()) return;
        absservertool$entityStart = System.nanoTime();
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void absservertool$entityEnd(Entity entity, CallbackInfo ci) {
        if (!PerformanceProfiler.isRunning()) return;
        long elapsed = System.nanoTime() - absservertool$entityStart;
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        PerformanceProfiler.record((ServerLevel) (Object) this, entity.blockPosition(),
                PerformanceProfiler.Category.ENTITY, elapsed, type);
    }

    @Inject(method = "tickBlock", at = @At("HEAD"))
    private void absservertool$blockStart(BlockPos pos, Block block, CallbackInfo ci) {
        if (!PerformanceProfiler.isRunning()) return;
        absservertool$blockStart = System.nanoTime();
    }

    @Inject(method = "tickBlock", at = @At("RETURN"))
    private void absservertool$blockEnd(BlockPos pos, Block block, CallbackInfo ci) {
        if (!PerformanceProfiler.isRunning()) return;
        PerformanceProfiler.record((ServerLevel) (Object) this, pos, PerformanceProfiler.Category.SCHEDULED_BLOCK,
                System.nanoTime() - absservertool$blockStart, null);
    }

    @Inject(method = "tickFluid", at = @At("HEAD"))
    private void absservertool$fluidStart(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (!PerformanceProfiler.isRunning()) return;
        absservertool$fluidStart = System.nanoTime();
    }

    @Inject(method = "tickFluid", at = @At("RETURN"))
    private void absservertool$fluidEnd(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (!PerformanceProfiler.isRunning()) return;
        PerformanceProfiler.record((ServerLevel) (Object) this, pos, PerformanceProfiler.Category.SCHEDULED_FLUID,
                System.nanoTime() - absservertool$fluidStart, null);
    }
}
