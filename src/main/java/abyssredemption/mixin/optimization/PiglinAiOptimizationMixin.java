// SPDX-License-Identifier: MIT
// Adapted from Carpet-Igny-Addition; Copyright (c) 2025 Melationin.
// Full permission notice: META-INF/licenses/piglin-optimization-MIT.txt.
package abyssredemption.mixin.optimization;

import abyssredemption.optimization.PiglinOptimization;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinAi.class)
public abstract class PiglinAiOptimizationMixin {
    @Inject(method = "wantsToPickup", at = @At("HEAD"), cancellable = true)
    private static void absservertool$onlyPickUpGold(Piglin piglin, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (!stack.is(Items.GOLD_INGOT) && PiglinOptimization.active(piglin)) callback.setReturnValue(false);
    }
    @Inject(method = "isPlayerHoldingLovedItem", at = @At("HEAD"), cancellable = true)
    private static void absservertool$skipLovedItemCheck(LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        if (PiglinOptimization.active(entity)) callback.setReturnValue(false);
    }
}
