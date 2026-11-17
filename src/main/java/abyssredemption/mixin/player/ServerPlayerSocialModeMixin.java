package abyssredemption.mixin.player;

import abyssredemption.social.SocialRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** One state snapshot after an accepted mode change, including changes between join and logout. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSocialModeMixin {
    @Inject(method="setGameMode",at=@At("RETURN"))
    private void absservertool$socialMode(GameType mode,CallbackInfoReturnable<Boolean> result) {
        if(result.getReturnValueZ()) SocialRuntime.mode((ServerPlayer)(Object)this);
    }
}
