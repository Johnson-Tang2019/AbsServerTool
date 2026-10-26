package abyssredemption.mixin.player;

import abyssredemption.player.ExcludedPlayers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture even bots spawned and removed between two statistics polls. */
@Mixin(PlayerList.class)
public abstract class PlayerListExclusionMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void absservertool$rememberFakePlayer(ServerPlayer player, CallbackInfo ci) {
        ExcludedPlayers.get().observe(player);
    }
}
