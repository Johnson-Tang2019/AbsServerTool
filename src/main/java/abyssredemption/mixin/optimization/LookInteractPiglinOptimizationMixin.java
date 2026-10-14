// SPDX-License-Identifier: MIT
// Adapted from Carpet-Igny-Addition; Copyright (c) 2025 Melationin.
// Full permission notice: META-INF/licenses/piglin-optimization-MIT.txt.
package abyssredemption.mixin.optimization;

import abyssredemption.optimization.PiglinOptimization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SetLookAndInteract.class)
public abstract class LookInteractPiglinOptimizationMixin {
    @Inject(method = "lambda$create$2", at = @At("HEAD"), cancellable = true)
    private static void absservertool$skipInteraction(BehaviorBuilder.Instance<?> instance, MemoryAccessor<?, ?> look,
            int distance, EntityType<?> type, MemoryAccessor<?, ?> interaction, MemoryAccessor<?, ?> visible,
            ServerLevel level, LivingEntity entity, long time, CallbackInfoReturnable<Boolean> callback) {
        if (PiglinOptimization.active(entity)) callback.setReturnValue(false);
    }
}
