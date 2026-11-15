package dev.crosshairlite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;

/** PNG validation and embedded storage; imported originals need not stay at their source path. */
public final class CrosshairImages {
    private CrosshairImages() {}
    public static String importPng(Path path) throws IOException {
        if(Files.size(path)>16_000_000) throw new IOException("PNG must be smaller than 16 MB.");
        byte[] bytes=Files.readAllBytes(path);decode(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    public static BufferedImage decode(byte[] bytes) throws IOException {
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if(input==null) throw new IOException("Cannot read image.");
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw new IOException("Choose a valid PNG image.");
            var reader=readers.next();
            try {
                if(!reader.getFormatName().equalsIgnoreCase("png")) throw new IOException("Only PNG images are supported.");
                reader.setInput(input,true,true);
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<1||h<1||w>4096||h>4096) throw new IOException("PNG dimensions must be between 1 and 4096 pixels.");
                return reader.read(0);
            } finally { reader.dispose(); }
        }
    }
    public static BufferedImage decode(String data) throws IOException {
        try { return decode(Base64.getDecoder().decode(data)); }
        catch(IllegalArgumentException e) { throw new IOException("The saved PNG is invalid.",e); }
    }
    public static BufferedImage trim(BufferedImage image) {
        int left=image.getWidth(),top=image.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) if((image.getRGB(x,y)>>>24)!=0) {
            left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
        }
        return right<0?new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB):image.getSubimage(left,top,right-left+1,bottom-top+1);
    }
}
