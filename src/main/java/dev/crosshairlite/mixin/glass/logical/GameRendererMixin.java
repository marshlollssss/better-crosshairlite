// Derived from ReGlass (MIT), Copyright (c) 2025 ReStudio By RedxAx.
package dev.crosshairlite.mixin.glass.logical;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.crosshairlite.glass.client.LiquidGlassPipelines;
import dev.crosshairlite.glass.client.LiquidGlassPrecomputeRuntime;
import dev.crosshairlite.glass.client.LiquidGlassUniforms;
import dev.crosshairlite.glass.client.api.ReGlassConfig;
import dev.crosshairlite.glass.client.gui.QuadVertexBufferProvider;
import dev.crosshairlite.glass.client.runtime.ReGlassAnim;
import dev.crosshairlite.mixin.glass.accessor.GameRendererAccessor;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "render", at = @At("HEAD"))
    private void crosshairGlass$beginGuiFrame(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (!(client.currentScreen instanceof dev.crosshairlite.CrosshairScreen)) {
            LiquidGlassUniforms.clearIfInitialized();
            return;
        }
        double deltaTicks;
        try {
            deltaTicks = tickCounter.getDynamicDeltaTicks();
        } catch (Throwable t) {
            deltaTicks = 1.0 / 60.0 * 20.0;
        }
        double dt = deltaTicks / 20.0;
        LiquidGlassUniforms.get().beginFrame(dt);
        ReGlassAnim.INSTANCE.update(ReGlassConfig.INSTANCE, dt);
    }

    @Inject(method = "renderBlur", at = @At("HEAD"), cancellable = true)
    private void crosshairGlass$renderLiquidGlass(CallbackInfo ci) {
        if (!(client.currentScreen instanceof dev.crosshairlite.CrosshairScreen)) return;
        LiquidGlassUniforms uniforms = LiquidGlassUniforms.get();
        if (uniforms.getCount() > 0) {
            ci.cancel();
            uniforms.uploadSharedUniforms();
            uniforms.uploadWidgetInfo();
            List<Integer> radii = uniforms.getUsedBlurRadiiOrdered();
            LiquidGlassPrecomputeRuntime.get().setRequestedRadii(radii);
            LiquidGlassPrecomputeRuntime.get().run();
            Framebuffer mainFb = this.client.getFramebuffer();
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "reglass liquid glass pass",
                    mainFb.getColorAttachmentView(),
                    OptionalInt.empty(),
                    mainFb.useDepthAttachment ? mainFb.getDepthAttachmentView() : null,
                    OptionalDouble.empty()
            )) {
                RenderPipeline pipeline = LiquidGlassPipelines.getGuiPipeline();
                pass.setPipeline(pipeline);

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("SamplerInfo", uniforms.getSamplerInfoBuffer());
                pass.setUniform("CustomUniforms", uniforms.getCustomUniformsBuffer());
                pass.setUniform("WidgetInfo", uniforms.getWidgetInfoBuffer());
                pass.setUniform("BgConfig", uniforms.getBgConfigBuffer());
                pass.bindTexture("Sampler0", LiquidGlassPrecomputeRuntime.get().getLiveBackdropView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));

                GuiRenderer guiRenderer = ((GameRendererAccessor) this).getGuiRenderer();
                GpuBuffer quadVB = ((QuadVertexBufferProvider) guiRenderer).crosshairGlass$getQuadVertexBuffer();
                RenderSystem.ShapeIndexBuffer quadIBInfo = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
                GpuBuffer quadIB = quadIBInfo.getIndexBuffer(6);
                pass.setVertexBuffer(0, quadVB);
                pass.setIndexBuffer(quadIB, quadIBInfo.getIndexType());
                for (int i = 0; i < 5; i++) {
                    String samplerName = switch (i) {
                        case 0 -> "Sampler1";
                        case 1 -> "Sampler2";
                        case 2 -> "Sampler3";
                        case 3 -> "Sampler4";
                        default -> "Sampler5";
                    };
                    if (i < radii.size()) {
                        int r = radii.get(i);
                            pass.bindTexture(samplerName, LiquidGlassPrecomputeRuntime.get().getBlurredViewForRadius(r), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                    } else {
                        if (!radii.isEmpty()) {
                            int r0 = radii.getFirst();
                            pass.bindTexture(samplerName, LiquidGlassPrecomputeRuntime.get().getBlurredViewForRadius(r0), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                        } else pass.bindTexture(samplerName, LiquidGlassPrecomputeRuntime.get().getBlurredViewForRadius(0), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                    }
                }
                pass.drawIndexed(0, 0, 6, 1);
            }
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void crosshairGlass$closeSamplingResources(CallbackInfo ci) {
        LiquidGlassPrecomputeRuntime.closeIfInitialized();
        LiquidGlassUniforms.closeIfInitialized();
    }
}
