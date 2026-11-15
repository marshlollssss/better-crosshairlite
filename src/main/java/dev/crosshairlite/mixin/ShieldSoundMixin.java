package dev.crosshairlite.mixin;

import dev.crosshairlite.ShieldTargets;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ShieldSoundMixin {
    @Inject(method="onPlaySound",at=@At("TAIL"))
    private void crosshairlite$shieldDisabled(PlaySoundS2CPacket packet,CallbackInfo ci) {
        ShieldTargets.onSound(packet);
    }
    @Inject(method="onPlaySoundFromEntity",at=@At("TAIL"))
    private void crosshairlite$entityShieldDisabled(net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket packet,CallbackInfo ci) {
        ShieldTargets.onEntitySound(packet);
    }
}
