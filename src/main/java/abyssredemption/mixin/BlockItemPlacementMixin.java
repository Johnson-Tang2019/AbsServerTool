package abyssredemption.mixin;

import abyssredemption.AbsServerTool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemPlacementMixin {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void absservertool$afterPlace(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue()) return;
        if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
            AbsServerTool.PLACEMENTS.recordPlacement(player);
        }
    }
}
