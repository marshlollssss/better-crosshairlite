package dev.crosshairlite.mixin;
import dev.crosshairlite.CrosshairConfig;
import dev.crosshairlite.CrosshairLite;
import dev.crosshairlite.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)
    private void crosshairlite$render(DrawContext context, RenderTickCounter ticks, CallbackInfo ci) {
        CrosshairConfig c = CrosshairLite.config;
        if (!c.enabled) return;
        ci.cancel();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof dev.crosshairlite.CrosshairScreen || mc.player == null || mc.options.hudHidden || (c.hideThirdPerson && !mc.options.getPerspective().isFirstPerson())
            || (c.hideSpectator && mc.player.isSpectator()) || (!c.showInF3 && mc.getDebugHud().shouldShowDebugHud())) return;
        if(c.shape==CrosshairConfig.Shape.VALORANT) {
            dev.crosshairlite.ValorantRenderer.drawInGame(context,c.valorant);
            dev.crosshairlite.AttackIndicator.draw(context,c);
            return;
        }
        int spread=dev.crosshairlite.CrosshairDynamics.spread(c,mc.player);
        CrosshairRenderer.draw(context,c,context.getScaledWindowWidth()/2.0+c.offsetX,context.getScaledWindowHeight()/2.0+c.offsetY,spread,mc.targetedEntity != null);
        dev.crosshairlite.AttackIndicator.draw(context,c);
    }
}
