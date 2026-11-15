package dev.crosshairlite;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;

public final class CrosshairRenderer {
    private CrosshairRenderer() {}
    private record Key(String geometry,String png,int drawingHash,int scale) {}
    private record Texture(Identifier foreground,Identifier outline,int size,int pixels) {}
    private static final Map<Key,Texture> CACHE=new LinkedHashMap<>(24,0.75f,true);
    private static int serial;
    public static void draw(DrawContext ctx,CrosshairConfig c,double cx,double cy,int spread,boolean target) {
        var mc=MinecraftClient.getInstance();
        Integer highlight=target?TargetColors.override(c,TargetColors.classify(mc.targetedEntity)):null;
        Integer shield=target?ShieldTargets.color(c,mc.targetedEntity):null;
        if(shield!=null)highlight=shield;
        boolean imageMask=c.shape==CrosshairConfig.Shape.IMAGE&&highlight!=null;
        int scale=Math.max(1,(int)Math.ceil(mc.getWindow().getScaleFactor()));
        Key key=new Key(c.shape+":"+c.size+":"+c.gap+":"+c.thickness+":"+c.dotSize+":"+c.centerDot+":"+c.outline+":"+c.outlineWidth+":"+c.top+":"+c.bottom+":"+c.left+":"+c.right+":"+spread+":"+c.customSize+":"+c.imageSize+":"+c.trimImage+":"+c.imageTint+":"+c.imageOutline+":"+c.imageSmoothing+":"+imageMask,
            c.shape==CrosshairConfig.Shape.IMAGE?c.imagePng:"",c.shape==CrosshairConfig.Shape.DESIGNER?Arrays.hashCode(c.drawing):0,scale);
        Texture texture=CACHE.get(key);
        if(texture==null) {
            try {
                var layers=CrosshairRaster.build(c,spread,scale,imageMask);
                texture=new Texture(upload(layers.foreground()),upload(layers.outline()),layers.guiSize(),layers.foreground().getWidth());
                CACHE.put(key,texture);
                while(CACHE.size()>24 || CACHE.values().stream().mapToLong(t->2L*t.pixels*t.pixels).sum()>8_000_000) {
                    var iterator=CACHE.entrySet().iterator();var old=iterator.next().getValue();iterator.remove();
                    mc.getTextureManager().destroyTexture(old.foreground);mc.getTextureManager().destroyTexture(old.outline);
                }
            } catch(Exception e) { CrosshairLite.LOGGER.error("Could not render custom crosshair",e);return; }
        }
        int rgb=c.rainbow?Color.HSBtoRGB((System.currentTimeMillis()%120000L)/120000f*c.rainbowSpeed,0.85f,1f)&0xFFFFFF:c.color;
        if(highlight!=null)rgb=highlight;
        if(c.shape==CrosshairConfig.Shape.IMAGE&&!c.imageTint&&highlight==null) rgb=0xFFFFFF;
        int alpha=Math.round(c.opacity*255f/100)<<24;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float)(cx-texture.size/2.0),(float)(cy-texture.size/2.0));
        if(c.shape==CrosshairConfig.Shape.IMAGE?c.imageOutline:c.outline) ctx.drawTexture(RenderPipelines.GUI_TEXTURED,texture.outline,0,0,0,0,texture.size,texture.size,texture.pixels,texture.pixels,texture.pixels,texture.pixels,alpha|c.outlineColor);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED,texture.foreground,0,0,0,0,texture.size,texture.size,texture.pixels,texture.pixels,texture.pixels,texture.pixels,alpha|rgb);
        ctx.getMatrices().popMatrix();
    }
    private static Identifier upload(BufferedImage image) {
        NativeImage nativeImage=new NativeImage(image.getWidth(),image.getHeight(),false);
        for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) nativeImage.setColorArgb(x,y,image.getRGB(x,y));
        Identifier id=Identifier.of("crosshairlite","generated/"+(serial++));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id,new NativeImageBackedTexture(()->"Better Crosshairlite",nativeImage));return id;
    }
}
