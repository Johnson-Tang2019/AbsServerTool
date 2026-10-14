// SPDX-License-Identifier: MIT
// Adapted from Carpet-Igny-Addition; Copyright (c) 2025 Melationin.
// Full permission notice: META-INF/licenses/piglin-optimization-MIT.txt.
package abyssredemption.mixin.optimization;

import abyssredemption.optimization.PiglinOptimization;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SetEntityLookTarget.class)
public abstract class LookTargetPiglinOptimizationMixin {
    @Inject(method = "lambda$create$5", at = @At("HEAD"), cancellable = true)
    private static void absservertool$skipLook(BehaviorBuilder.Instance<?> instance, MemoryAccessor<?, ?> look,
            Predicate<?> predicate, float distance, MemoryAccessor<?, ?> visible, ServerLevel level,
            LivingEntity entity, long time, CallbackInfoReturnable<Boolean> callback) {
        if (PiglinOptimization.active(entity)) callback.setReturnValue(false);
    }
}
