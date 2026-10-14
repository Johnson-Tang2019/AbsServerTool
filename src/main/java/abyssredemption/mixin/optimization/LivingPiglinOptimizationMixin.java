// SPDX-License-Identifier: MIT
// Adapted from Carpet-Igny-Addition; Copyright (c) 2025 Melationin.
// Full permission notice: META-INF/licenses/piglin-optimization-MIT.txt.
package abyssredemption.mixin.optimization;

import abyssredemption.optimization.PiglinOptimization;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingPiglinOptimizationMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void absservertool$skipTravel(Vec3 direction, CallbackInfo callback) {
        if (PiglinOptimization.active((LivingEntity)(Object)this)) callback.cancel();
    }
    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void absservertool$skipPush(CallbackInfo callback) {
        if (PiglinOptimization.active((LivingEntity)(Object)this)) callback.cancel();
    }
}
