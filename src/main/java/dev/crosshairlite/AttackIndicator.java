package dev.crosshairlite;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public final class AttackIndicator {
    public static void draw(DrawContext context,CrosshairConfig config) {
        var mc=MinecraftClient.getInstance();
        if(!config.vanillaAttackIndicator||mc.player==null||mc.player.isSpectator())return;
        float progress=mc.player.getAttackCooldownProgress(0);
        int x=context.getScaledWindowWidth()/2-8+config.offsetX;
        int y=context.getScaledWindowHeight()/2+9+config.offsetY;
        if(progress>=1&&mc.player.getAttackCooldownProgressPerTick()>5&&mc.targetedEntity instanceof LivingEntity target&&target.isAlive()) {
            context.drawGuiTexture(RenderPipelines.CROSSHAIR,Identifier.ofVanilla("hud/crosshair_attack_indicator_full"),x,y,16,16);
        } else if(progress<1) {
            context.drawGuiTexture(RenderPipelines.CROSSHAIR,Identifier.ofVanilla("hud/crosshair_attack_indicator_background"),x,y,16,4);
            context.drawGuiTexture(RenderPipelines.CROSSHAIR,Identifier.ofVanilla("hud/crosshair_attack_indicator_progress"),16,4,0,0,x,y,(int)(progress*17),4);
        }
    }
}
