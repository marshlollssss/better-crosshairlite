// Derived from ReGlass (MIT), Copyright (c) 2025 ReStudio By RedxAx.
package dev.crosshairlite.mixin.glass.accessor;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {

    @Accessor("vertexConsumers")
    VertexConsumerProvider.Immediate getVertexConsumers();
}

