package dev.crosshairlite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairImagesTest {
    @TempDir Path temp;
    @Test void importsTransparentPngAndCentersVisibleContentWithoutStretching() throws Exception {
        BufferedImage source=new BufferedImage(40,30,BufferedImage.TYPE_INT_ARGB);
        for(int y=5;y<11;y++)for(int x=20;x<32;x++)source.setRGB(x,y,0xFF4488CC);
        Path path=temp.resolve("example.png");ImageIO.write(source,"PNG",path.toFile());
        String data=CrosshairImages.importPng(path);
        var decoded=CrosshairImages.decode(data);assertEquals(0,decoded.getRGB(0,0));
        var trimmed=CrosshairImages.trim(decoded);assertEquals(12,trimmed.getWidth());assertEquals(6,trimmed.getHeight());
        var c=new CrosshairConfig();c.shape=CrosshairConfig.Shape.IMAGE;c.imagePng=data;c.outline=false;c.imageSize=24;
        var image=CrosshairRaster.build(c,0,4).foreground();
        int minX=image.getWidth(),minY=image.getHeight(),maxX=-1,maxY=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>127) {
            minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
        }
        assertEquals(image.getWidth(),minX+maxX+1);assertEquals(image.getHeight(),minY+maxY+1);
        assertEquals(2.0,(maxX-minX+1)/(double)(maxY-minY+1),0.05);
    }
    @Test void rejectsOtherFormatsEvenWithPngExtension() throws Exception {
        Path path=temp.resolve("fake.png");Files.writeString(path,"not an image");
        assertThrows(java.io.IOException.class,()->CrosshairImages.importPng(path));
    }
    @Test void designerAndImageSurviveSaveAndDraftCopies() throws Exception {
        var c=new CrosshairConfig();c.shape=CrosshairConfig.Shape.DESIGNER;c.drawing[16*33+16]=true;c.customSize=49;
        var copy=c.copy();copy.drawing[0]=true;assertFalse(c.drawing[0]);
        Path path=temp.resolve("config.json");c.save(path);var loaded=CrosshairConfig.load(path);
        assertTrue(loaded.drawing[16*33+16]);assertEquals(49,loaded.customSize);
    }
    @Test void emptyTransparentPngRemainsEmpty() throws Exception {
        BufferedImage image=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
        var trimmed=CrosshairImages.trim(image);assertEquals(1,trimmed.getWidth());assertEquals(0,trimmed.getRGB(0,0));
    }
    @Test void pngDoesNotInheritShapeOutlineAndScalingStaysCrisp() throws Exception {
        BufferedImage source=new BufferedImage(3,3,BufferedImage.TYPE_INT_ARGB);source.setRGB(1,1,0xFFFFFFFF);
        Path path=temp.resolve("pixel.png");ImageIO.write(source,"PNG",path.toFile());
        var c=new CrosshairConfig();c.shape=CrosshairConfig.Shape.IMAGE;c.imagePng=CrosshairImages.importPng(path);
        c.trimImage=false;c.imageSize=27;c.outline=true;
        var layers=CrosshairRaster.build(c,0,4);int outlinePixels=0;
        for(int y=0;y<layers.foreground().getHeight();y++)for(int x=0;x<layers.foreground().getWidth();x++) {
            int alpha=layers.foreground().getRGB(x,y)>>>24;assertTrue(alpha==0||alpha==255);
            if((layers.outline().getRGB(x,y)>>>24)!=0)outlinePixels++;
        }
        assertEquals(0,outlinePixels);
        c.imageOutline=true;layers=CrosshairRaster.build(c,0,4);boolean found=false;
        for(int y=0;y<layers.outline().getHeight();y++)for(int x=0;x<layers.outline().getWidth();x++)found|=(layers.outline().getRGB(x,y)>>>24)!=0;
        assertTrue(found);
    }
}
