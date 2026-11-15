// Derived from ReGlass (MIT), Copyright (c) 2025 ReStudio By RedxAx.
package dev.crosshairlite.glass.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.util.math.ColorHelper;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import dev.crosshairlite.glass.client.api.ReGlassConfig;
import dev.crosshairlite.glass.client.api.WidgetStyle;
import dev.crosshairlite.glass.client.gui.LiquidGlassGuiElementRenderState;
import dev.crosshairlite.glass.client.runtime.ReGlassAnim;
import dev.crosshairlite.glass.client.runtime.GlassRenderBounds;
import dev.crosshairlite.glass.client.runtime.GlassRenderBounds.Bounds;
import dev.crosshairlite.mixin.glass.accessor.GuiRenderStateAccessor;

public final class LiquidGlassUniforms {

    private static LiquidGlassUniforms instance;
    public static final int MAX_WIDGETS = 64;
    public static final int MAX_LAYERS = 16;
    public static final int MAX_BLUR_LEVELS = 5;

    public static LiquidGlassUniforms get() {
        if (instance == null) instance = new LiquidGlassUniforms();
        return instance;
    }
    public static void clearIfInitialized() {
        if (instance != null) instance.beginFrame(0);
    }
    public static void closeIfInitialized() {
        if (instance == null) return;
        instance.samplerInfo.close();instance.customUniforms.close();instance.widgetInfo.close();instance.bgConfig.close();
        instance = null;
    }

    private final GpuBuffer samplerInfo;
    private final GpuBuffer customUniforms;
    private final GpuBuffer widgetInfo;
    private final GpuBuffer bgConfig;

    private final List<LiquidGlassGuiElementRenderState> widgets = new ArrayList<>();
    private boolean screenWantsBlur = false;

    private List<Integer> usedBlurRadiiOrdered = new ArrayList<>();
    private final HashMap<Integer, Integer> blurRadiusToIndex = new HashMap<>();

    private static final class FadeState {
        float hover;
        float focus;
    }

    private final HashMap<Long, FadeState> fades = new HashMap<>();
    private double dtSeconds = 0.0;

    private LiquidGlassUniforms() {
        samplerInfo = RenderSystem.getDevice().createBuffer(() -> "reglass SamplerInfo", 130, 16);

        Std140SizeCalculator calc = new Std140SizeCalculator();
        calc.putFloat();
        calc.align(16);
        calc.putVec4();
        calc.putFloat();
        calc.align(16);
        calc.putVec3();
        calc.align(16);
        calc.putVec4();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        int customUniformsSize = calc.get();
        customUniforms = RenderSystem.getDevice().createBuffer(() -> "reglass CustomUniforms", 130, customUniformsSize);

        int widgetInfoSize = 16 + MAX_LAYERS * 32 + MAX_WIDGETS * (16 * 13);
        widgetInfo = RenderSystem.getDevice().createBuffer(() -> "reglass WidgetInfo", 130, widgetInfoSize);

        Std140SizeCalculator bcalc = new Std140SizeCalculator();
        bcalc.putFloat();
        bcalc.putFloat();
        bcalc.putVec2();
        int bgConfigSize = bcalc.get();
        bgConfig = RenderSystem.getDevice().createBuffer(() -> "reglass BgConfig", 130, bgConfigSize);
    }

    public void beginFrame(double dtSeconds) {
        widgets.clear();
        screenWantsBlur = false;
        usedBlurRadiiOrdered.clear();
        blurRadiusToIndex.clear();
        this.dtSeconds = Math.max(0.0, dtSeconds);
    }

    public void setScreenWantsBlur(boolean wantsBlur) { this.screenWantsBlur = wantsBlur; }

    public void uploadSharedUniforms() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int outW = mc.getFramebuffer().textureWidth;
        int outH = mc.getFramebuffer().textureHeight;

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(samplerInfo, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            b.putVec2((float) outW, (float) outH);
            b.putVec2((float) outW, (float) outH);
        }

        double[] mx = new double[1];
        double[] my = new double[1];
        GLFW.glfwGetCursorPos(mc.getWindow().getHandle(), mx, my);
        int fbH = mc.getFramebuffer().textureHeight;

        float time = (float) GLFW.glfwGetTime();
        ReGlassConfig config = ReGlassConfig.INSTANCE;

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(customUniforms, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            b.putFloat(time);
            b.align(16);
            float x = (float) (mx[0] * outW / Math.max(1, mc.getWindow().getWidth()));
            float y = fbH - (float) (my[0] * fbH / Math.max(1, mc.getWindow().getHeight()));
            b.putVec4(new Vector4f(x, y, 0f, 0f));
            b.putFloat(this.screenWantsBlur ? 1.0f : 0.0f);
            b.align(16);
            var dir2 = config.rimLight.direction();
            b.putVec3(new Vector3f(dir2.x, dir2.y, 0.0f));
            b.align(16);
            int rc = config.rimLight.color();
            b.putVec4(ColorHelper.getRed(rc) / 255f, ColorHelper.getGreen(rc) / 255f, ColorHelper.getBlue(rc) / 255f, config.rimLight.intensity());
            b.putFloat(config.pixelEpsilon);
            b.putFloat(ReGlassAnim.INSTANCE.debugStep());
            b.putFloat(config.features.pixelatedGrid ? 1.0f : 0.0f);
            b.putFloat(ReGlassAnim.INSTANCE.pixelatedGridSize());
            b.putFloat(ReGlassAnim.INSTANCE.hoverScalePx());
            b.putFloat(ReGlassAnim.INSTANCE.focusScalePx());
            b.putFloat(ReGlassAnim.INSTANCE.focusBorderWidthPx());
            b.putFloat(ReGlassAnim.INSTANCE.focusBorderIntensity());
            b.putFloat(ReGlassAnim.INSTANCE.focusBorderSpeed());
        }

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(bgConfig, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            b.putFloat(ReGlassAnim.INSTANCE.shadowExpand());
            b.putFloat(ReGlassAnim.INSTANCE.shadowFactor());
            float s = (float) mc.getWindow().getScaleFactor();
            b.putVec2(ReGlassAnim.INSTANCE.shadowOffsetX() * s, ReGlassAnim.INSTANCE.shadowOffsetY() * s);
        }
    }

    public void tryApplyBlur(DrawContext context) {
        GuiRenderState state = context.state;
        int blurLayer = ((GuiRenderStateAccessor) state).getBlurLayer();
        if (blurLayer == Integer.MAX_VALUE) state.applyBlur();
    }

    public void addWidget(LiquidGlassGuiElementRenderState element) {
        if (widgets.size() >= MAX_WIDGETS) return;
        widgets.add(element);
    }

    private static long rectKey(int x1, int y1, int x2, int y2) {
        long a = (((long) x1) & 0xFFFFFFFFL) | ((((long) y1) & 0xFFFFFFFFL) << 32);
        long b = (((long) x2) & 0xFFFFFFFFL) | ((((long) y2) & 0xFFFFFFFFL) << 32);
        long h = 1469598103934665603L;
        h ^= a; h *= 1099511628211L;
        h ^= b; h *= 1099511628211L;
        return h;
    }

    private float smoothToward(float current, float target, double dt, float tau) {
        if (tau <= 1e-5f) return target;
        float a = (float) (1.0 - Math.exp(-Math.max(0.0, dt) / Math.max(1e-4, tau)));
        float v = current + (target - current) * a;
        if (Math.abs(v - target) < 1e-4f) return target;
        return v;
    }

    public void uploadWidgetInfo() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int fbH = mc.getFramebuffer().textureHeight;
        float scale = (float) mc.getWindow().getScaleFactor();

        HashSet<Integer> requested = new HashSet<>();
        for (LiquidGlassGuiElementRenderState w : widgets) {
            WidgetStyle s = w.style();
            requested.add(Math.max(0, s.getBlurRadius()));
        }
        List<Integer> sorted = requested.stream().sorted().toList();
        usedBlurRadiiOrdered = new ArrayList<>();
        for (int i = 0; i < sorted.size() && i < MAX_BLUR_LEVELS; i++) usedBlurRadiiOrdered.add(sorted.get(i));
        if (usedBlurRadiiOrdered.isEmpty()) usedBlurRadiiOrdered.add(ReGlassAnim.INSTANCE.blurRadiusInt());
        blurRadiusToIndex.clear();
        for (int i = 0; i < usedBlurRadiiOrdered.size(); i++) blurRadiusToIndex.put(usedBlurRadiiOrdered.get(i), i);

        List<LiquidGlassGuiElementRenderState> renderWidgets = new ArrayList<>(widgets);
        renderWidgets.sort((a, b0) -> Integer.compare(a.style().getLayer(), b0.style().getLayer()));

        int[] layerStarts = new int[MAX_LAYERS];
        int[] layerCounts = new int[MAX_LAYERS];
        for (int i = 0; i < MAX_LAYERS; i++) {
            layerStarts[i] = renderWidgets.size();
        }
        for (int i = 0; i < renderWidgets.size(); i++) {
            int layer = Math.max(0, Math.min(MAX_LAYERS - 1, renderWidgets.get(i).style().getLayer()));
            if (layerCounts[layer] == 0) {
                layerStarts[layer] = i;
            }
            layerCounts[layer]++;
        }

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(widgetInfo, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            Bounds[] shapeBounds = new Bounds[renderWidgets.size()];
            Bounds[] shadowBounds = new Bounds[renderWidgets.size()];
            Bounds[] layerBounds = new Bounds[MAX_LAYERS];
            float[] smoothSum = new float[MAX_LAYERS];
            float[] smoothMax = new float[MAX_LAYERS];
            for (var w : renderWidgets) {
                int layer = Math.max(0, Math.min(MAX_LAYERS - 1, w.style().getLayer()));
                float smoothing = Math.abs(w.style().getSmoothing());
                smoothSum[layer] += smoothing;
                smoothMax[layer] = Math.max(smoothMax[layer], smoothing);
            }
            for (int i = 0; i < renderWidgets.size(); i++) {
                var w = renderWidgets.get(i);
                var style = w.style();
                int layer = Math.max(0, Math.min(MAX_LAYERS - 1, style.getLayer()));
                float x = w.x1() * scale;
                float y = fbH - w.y2() * scale;
                float width = (w.x2() - w.x1()) * scale;
                float height = (w.y2() - w.y1()) * scale;
                // Smooth unions can extend by k/4 per operation; subtraction may extend by k.
                float padding = (smoothMax[layer] + smoothSum[layer] * 0.25f + 0.011f) * fbH
                        + Math.abs(ReGlassAnim.INSTANCE.hoverScalePx()) + Math.abs(ReGlassAnim.INSTANCE.focusScalePx());
                Bounds scissor = null;
                var sc = w.scissorArea();
                if (sc != null) {
                    scissor = new Bounds(sc.getLeft() * scale, fbH - sc.getBottom() * scale,
                            sc.getRight() * scale, fbH - sc.getTop() * scale);
                }
                Bounds shape = GlassRenderBounds.shape(x, y, width, height, padding);
                Bounds shadow = GlassRenderBounds.shadow(x, y, width, height, style.getShadowExpand(),
                        style.getShadowFactor(), style.getShadowColorAlpha(),
                        style.getShadowOffsetX() * scale, style.getShadowOffsetY() * scale);
                if (scissor != null) { shape = shape.clip(scissor); shadow = shadow.clip(scissor); }
                shapeBounds[i] = shape;
                shadowBounds[i] = shadow;
                Bounds footprint = shape.union(shadow);
                layerBounds[layer] = layerBounds[layer] == null ? footprint : layerBounds[layer].union(footprint);
            }
            b.putFloat((float) renderWidgets.size());
            b.align(16);

            for (int i = 0; i < MAX_LAYERS; i++) {
                b.putVec4((float) layerStarts[i], (float) layerCounts[i], 0f, 0f);
            }

            for (Bounds bounds : layerBounds) {
                if (bounds == null || bounds.empty()) b.putVec4(0f, 0f, 0f, 0f);
                else b.putVec4(bounds.left(), bounds.bottom(), bounds.right(), bounds.top());
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    var w = renderWidgets.get(i);
                    float W = w.x2() - w.x1();
                    float H = w.y2() - w.y1();
                    float px = w.x1() * scale;
                    float pyTop = w.y1() * scale;
                    float pW = W * scale;
                    float pH = H * scale;
                    float cx = px + 0.5f * pW;
                    float cyTop = pyTop + 0.5f * pH;
                    float cyFB = (float) fbH - cyTop;
                    float rectX = cx - 0.5f * pW;
                    float rectY = cyFB - 0.5f * pH;
                    b.putVec4(rectX, rectY, pW, pH);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    var w = renderWidgets.get(i);
                    float rad = w.cornerRadius() * scale;
                    b.putVec4(rad, rad, rad, rad);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    var style = renderWidgets.get(i).style();
                    int c = style.getTintColor();
                    b.putVec4(ColorHelper.getRed(c) / 255f, ColorHelper.getGreen(c) / 255f, ColorHelper.getBlue(c) / 255f, style.getTintAlpha());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    WidgetStyle s = renderWidgets.get(i).style();
                    b.putVec4(s.getRefThickness(), s.getRefFactor(), s.getRefDispersion(), s.getRefFresnelRange());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }
            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    WidgetStyle s = renderWidgets.get(i).style();
                    b.putVec4(s.getRefFresnelHardness(), s.getRefFresnelFactor(), s.getGlareRange(), s.getGlareHardness());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }
            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    WidgetStyle s = renderWidgets.get(i).style();
                    b.putVec4(s.getGlareConvergence(), s.getGlareOppositeFactor(), s.getGlareFactor(), s.getGlareAngleRad());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    WidgetStyle s = renderWidgets.get(i).style();
                    b.putVec4(s.getSmoothing(), 0f, 0f, 0f);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    var w = renderWidgets.get(i);
                    ScreenRect sc = w.scissorArea();
                    if (sc != null) {
                        float sL = sc.getLeft() * scale;
                        float sR = sc.getRight() * scale;
                        float sT = sc.getTop() * scale;
                        float sB = sc.getBottom() * scale;
                        b.putVec4(sL, fbH - sB, sR, fbH - sT);
                    } else b.putVec4(0f, 0f, (float) mc.getFramebuffer().textureWidth, (float) mc.getFramebuffer().textureHeight);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    WidgetStyle s = renderWidgets.get(i).style();
                    float sx = s.getShadowOffsetX() * scale;
                    float sy = s.getShadowOffsetY() * scale;
                    b.putVec4(s.getShadowExpand(), s.getShadowFactor(), sx, sy);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    WidgetStyle s = renderWidgets.get(i).style();
                    int col = s.getShadowColor();
                    b.putVec4(ColorHelper.getRed(col) / 255f, ColorHelper.getGreen(col) / 255f, ColorHelper.getBlue(col) / 255f, s.getShadowColorAlpha());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < renderWidgets.size()) {
                    WidgetStyle s = renderWidgets.get(i).style();
                    int radius = Math.max(0, s.getBlurRadius());
                    Integer idx = blurRadiusToIndex.get(radius);
                    if (idx == null) idx = 0;
                    var w = renderWidgets.get(i);
                    long key = s.hasFadeKey() ? s.getFadeKey() : rectKey(w.x1(), w.y1(), w.x2(), w.y2());
                    FadeState fs = fades.computeIfAbsent(key, k -> new FadeState());
                    fs.hover = smoothToward(fs.hover, Math.max(0f, Math.min(1f, w.hover())), dtSeconds, 0.12f);
                    float targetFocus = Math.max(0f, Math.min(1f, w.focus()));
                    float responseSpeed = Math.max(0.5f, ReGlassConfig.INSTANCE.focusBorderSpeed);
                    fs.focus = smoothToward(fs.focus, targetFocus, dtSeconds,
                            (targetFocus > fs.focus ? 0.06f : 0.18f) / responseSpeed);
                    double h = Math.sin(w.x1() * 12.9898 + w.y1() * 78.233 + i * 37.719);
                    float seed = (float) (h - Math.floor(h));
                    b.putVec4((float) idx, fs.hover, fs.focus, seed);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }
            for (Bounds[] boundsArray : new Bounds[][] {shapeBounds, shadowBounds}) {
                for (int i = 0; i < MAX_WIDGETS; i++) {
                    if (i < boundsArray.length && !boundsArray[i].empty()) {
                        Bounds bounds = boundsArray[i];
                        b.putVec4(bounds.left(), bounds.bottom(), bounds.right(), bounds.top());
                    } else b.putVec4(0f, 0f, 0f, 0f);
                }
            }
        }
    }

    public int getCount() { return widgets.size(); }
    public double getFrameDeltaSeconds() { return dtSeconds; }
    public GpuBuffer getSamplerInfoBuffer() { return samplerInfo; }
    public GpuBuffer getCustomUniformsBuffer() { return customUniforms; }
    public GpuBuffer getWidgetInfoBuffer() { return widgetInfo; }
    public GpuBuffer getBgConfigBuffer() { return bgConfig; }
    public List<Integer> getUsedBlurRadiiOrdered() { return usedBlurRadiiOrdered; }
}
