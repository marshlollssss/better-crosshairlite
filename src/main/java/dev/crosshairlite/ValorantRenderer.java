package dev.crosshairlite;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Draws a {@link ValorantProfile} with crisp, pixel-aligned rectangles. */
public final class ValorantRenderer {
    private ValorantRenderer() {}

    /** In game: centered on the window, one profile unit = one physical screen pixel. */
    public static void drawInGame(DrawContext ctx, ValorantProfile p) {
        var mc = MinecraftClient.getInstance();
        var window = mc.getWindow();
        double scale = Math.max(1.0, window.getScaleFactor());
        double cx = (window.getFramebufferWidth() / 2) / scale;
        double cy = (window.getFramebufferHeight() / 2) / scale;
        ValorantDynamics.update(mc.player);
        draw(ctx, p, cx, cy, 1.0 / scale, ValorantDynamics.movement(), ValorantDynamics.firing());
    }

    /** Settings preview: centered where asked, {@code pixelSize} GUI units per profile unit. */
    public static void drawPreview(DrawContext ctx, ValorantProfile p, double cx, double cy, double pixelSize) {
        draw(ctx, p, cx, cy, pixelSize, 0, 0);
    }

    private static void draw(DrawContext ctx, ValorantProfile p, double cx, double cy, double pixelSize, double move, double fire) {
        int rgb = p.rgb() & 0xFFFFFF;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) cx, (float) cy);
        ctx.getMatrices().scale((float) pixelSize, (float) pixelSize);
        for (var r : p.layout(move, fire, ValorantDynamics.MOVE_PX, ValorantDynamics.FIRE_PX)) {
            int alpha = Math.round(Math.max(0f, Math.min(1f, r.alpha())) * 255f);
            if (alpha == 0) continue;
            int color = (alpha << 24) | (r.outline() ? 0x000000 : rgb);
            ctx.fill(r.x1(), r.y1(), r.x2(), r.y2(), color);
        }
        ctx.getMatrices().popMatrix();
    }
}
