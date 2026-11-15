// Derived from ReGlass (MIT), Copyright (c) 2025 ReStudio By RedxAx.
package dev.crosshairlite.mixin.glass.client;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.crosshairlite.glass.client.gui.LiquidGlassGuiElementRenderer;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Shadow @Final private VertexConsumerProvider.Immediate vertexConsumers;
    @Shadow @Final @Mutable private Map<Class<? extends SpecialGuiElementRenderState>, SpecialGuiElementRenderer<?>> specialElementRenderers;

    // Extend the completed registry without redirecting the builder call used by ReGlass.
    @Inject(method = "<init>", at = @At("RETURN"))
    private void crosshairlite$registerRenderer(CallbackInfo ci) {
        var renderer = new LiquidGlassGuiElementRenderer(vertexConsumers);
        specialElementRenderers = ImmutableMap.<Class<? extends SpecialGuiElementRenderState>, SpecialGuiElementRenderer<?>>builder()
            .putAll(specialElementRenderers)
            .put(renderer.getElementClass(), renderer)
            .buildOrThrow();
    }
}
