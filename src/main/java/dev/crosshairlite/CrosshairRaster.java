package dev.crosshairlite;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** Rasterize continuous curves at display resolution, rather than Minecraft GUI-pixel resolution. */
public final class CrosshairRaster {
    private CrosshairRaster() {}
    public record Layers(BufferedImage foreground,BufferedImage outline,int guiSize) {}
    public static Layers build(CrosshairConfig c,int spread,int scale) throws IOException {
        return build(c,spread,scale,false);
    }
    public static Layers build(CrosshairConfig c,int spread,int scale,boolean imageMask) throws IOException {
        if(c.shape==CrosshairConfig.Shape.DESIGNER) {
            var high=buildRaster(c,spread,scale*4,imageMask);
            return new Layers(reduceCoverage(high.foreground()),reduceCoverage(high.outline()),high.guiSize());
        }
        return buildRaster(c,spread,scale,imageMask);
    }
    // Average coverage instead of dropping samples from thin drawn strokes.
    private static BufferedImage reduceCoverage(BufferedImage source) {
        int side=source.getWidth()/4;
        var result=new BufferedImage(side,side,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<side;y++)for(int x=0;x<side;x++) {
            int alpha=0;
            for(int dy=0;dy<4;dy++)for(int dx=0;dx<4;dx++)alpha+=source.getRGB(x*4+dx,y*4+dy)>>>24;
            result.setRGB(x,y,((alpha+8)/16<<24)|0xFFFFFF);
        }
        return result;
    }
    private static Layers buildRaster(CrosshairConfig c,int spread,int scale,boolean imageMask) throws IOException {
        var area=CrosshairGeometry.build(c,spread);
        BufferedImage source=null;
        int half;
        if(c.shape==CrosshairConfig.Shape.IMAGE) {
            if(!c.imagePng.isEmpty()) source=CrosshairImages.decode(c.imagePng);
            if(source!=null && c.trimImage) source=CrosshairImages.trim(source);
            half=(int)Math.ceil(c.imageSize/2.0+spread)+c.outlineWidth+2;
        } else {
            var b=area.getBounds2D();
            half=(int)Math.ceil(Math.max(Math.max(Math.abs(b.getMinX()),Math.abs(b.getMaxX())),Math.max(Math.abs(b.getMinY()),Math.abs(b.getMaxY()))))+c.outlineWidth+2;
        }
        int side=Math.max(2,half*2),pixels=side*scale;
        BufferedImage fg=new BufferedImage(pixels,pixels,BufferedImage.TYPE_INT_ARGB);
        BufferedImage bg=new BufferedImage(pixels,pixels,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=graphics(fg,scale,half);
        if(source!=null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,c.imageSmoothing?RenderingHints.VALUE_INTERPOLATION_BICUBIC:RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if(c.imageTint||imageMask) {
                BufferedImage white=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<source.getHeight();y++) for(int x=0;x<source.getWidth();x++) white.setRGB(x,y,(source.getRGB(x,y)&0xFF000000)|0xFFFFFF);
                source=white;
            }
            double factor=(c.imageSize+spread*2)/(double)Math.max(source.getWidth(),source.getHeight());
            AffineTransform transform=new AffineTransform();
            transform.translate(-source.getWidth()*factor/2,-source.getHeight()*factor/2);transform.scale(factor,factor);
            g.drawImage(source,transform,null);
        } else { g.setColor(Color.WHITE);g.fill(area); }
        g.dispose();
        if(c.shape==CrosshairConfig.Shape.IMAGE?c.imageOutline:c.outline) {
            if(c.shape!=CrosshairConfig.Shape.IMAGE) {
                g=graphics(bg,scale,half);g.setColor(Color.WHITE);g.fill(CrosshairGeometry.outline(area,c.outlineWidth));g.dispose();
            } else if(source!=null) {
                int[] alpha=new int[pixels*pixels];
                for(int y=0;y<pixels;y++) for(int x=0;x<pixels;x++) alpha[y*pixels+x]=fg.getRGB(x,y)>>>24;
                int[] expanded=dilate(alpha,pixels,c.outlineWidth*scale);
                for(int y=0;y<pixels;y++) for(int x=0;x<pixels;x++) {
                    int i=y*pixels+x;
                    int a=Math.max(0,expanded[i]-alpha[i]);
                    bg.setRGB(x,y,(a<<24)|0xFFFFFF);
                }
            }
        }
        return new Layers(fg,bg,side);
    }
    // Sliding maximum in two passes keeps large PNG outlines linear in pixel count.
    private static int[] dilate(int[] source,int side,int radius) {
        int[] horizontal=new int[source.length],output=new int[source.length];
        for(int pass=0;pass<2;pass++) {
            int[] input=pass==0?source:horizontal,dest=pass==0?horizontal:output;
            for(int row=0;row<side;row++) {
                int[] deque=new int[side];int head=0,tail=0,next=0;
                for(int col=0;col<side;col++) {
                    while(next<side && next<=col+radius) {
                        int index=pass==0?row*side+next:next*side+row;
                        while(tail>head && input[pass==0?row*side+deque[tail-1]:deque[tail-1]*side+row]<=input[index])tail--;
                        deque[tail++]=next++;
                    }
                    while(tail>head && deque[head]<col-radius)head++;
                    dest[pass==0?row*side+col:col*side+row]=input[pass==0?row*side+deque[head]:deque[head]*side+row];
                }
            }
        }
        return output;
    }
    private static Graphics2D graphics(BufferedImage image,int scale,int half) {
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.scale(scale,scale);g.translate(half,half);return g;
    }
}
