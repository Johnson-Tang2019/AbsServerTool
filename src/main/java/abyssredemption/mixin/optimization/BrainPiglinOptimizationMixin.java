// SPDX-License-Identifier: MIT
// Adapted from Carpet-Igny-Addition; Copyright (c) 2025 Melationin.
// Full permission notice: META-INF/licenses/piglin-optimization-MIT.txt.
package abyssredemption.mixin.optimization;

import abyssredemption.optimization.PiglinOptimization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Brain.class, priority = 1500)
public abstract class BrainPiglinOptimizationMixin {
    @Inject(method = "tickSensors", at = @At("HEAD"), cancellable = true)
    private void absservertool$skipSensors(ServerLevel level, LivingEntity entity, CallbackInfo callback) {
        if (PiglinOptimization.active(entity)) callback.cancel();
    }
}
