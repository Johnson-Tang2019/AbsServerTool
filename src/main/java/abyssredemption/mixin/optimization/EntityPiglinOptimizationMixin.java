// SPDX-License-Identifier: MIT
// Adapted from Carpet-Igny-Addition; Copyright (c) 2025 Melationin.
// Full permission notice: META-INF/licenses/piglin-optimization-MIT.txt.
package abyssredemption.mixin.optimization;

import abyssredemption.config.ConfigManager;
import abyssredemption.optimization.PiglinOptimization;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPiglinOptimizationMixin implements PiglinOptimization.State {
    @Unique private int absservertool$nearbyPiglins;
    @Unique private long absservertool$generation = -1;
    public int absservertool$nearbyPiglins() { return absservertool$nearbyPiglins; }
    public long absservertool$generation() { return absservertool$generation; }
    @Inject(method = "tick", at = @At("HEAD"))
    private void absservertool$samplePiglins(CallbackInfo callback) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof Piglin) || self.level().isClientSide()) return;
        if (!ConfigManager.get().piglinOptimization().enabled()) {
            absservertool$nearbyPiglins = 0; absservertool$generation = -1; return;
        }
        if (absservertool$generation != PiglinOptimization.generation() || (self.tickCount + self.getId()) % 100 == 0) {
            absservertool$nearbyPiglins = self.level().getEntities(self.getType(),
                    self.getBoundingBox(), entity -> true).size();
            absservertool$generation = PiglinOptimization.generation();
        }
    }
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void absservertool$skipSelfMovement(MoverType type, Vec3 movement, CallbackInfo callback) {
        if (type == MoverType.SELF && PiglinOptimization.active((Entity)(Object)this)) callback.cancel();
    }
}
