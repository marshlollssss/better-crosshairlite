package dev.crosshairlite;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** PNG library under config/crosshairlite/imports; selection uses a basename only. */
public final class ImportedCrosshairs {
    private ImportedCrosshairs() {}
    public record Entry(String filename,String png) {}
    public static Entry importFile(Path folder,Path source) throws IOException {
        return store(folder,source.getFileName().toString(),CrosshairImages.importPng(source));
    }
    public static Entry store(Path folder,String original,String png) throws IOException {
        byte[] bytes;
        try { bytes=Base64.getDecoder().decode(png);CrosshairImages.decode(bytes); }
        catch(IllegalArgumentException e){throw new IOException("Invalid PNG data",e);}
        String clean=original.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]","_");
        if(clean.isBlank()||clean.equals(".")||clean.equals(".."))clean="crosshair.png";
        if(!clean.toLowerCase(Locale.ROOT).endsWith(".png"))clean+=".png";
        Files.createDirectories(folder);
        String name=clean;int counter=2;
        while(Files.exists(folder.resolve(name))) {
            if(Arrays.equals(Files.readAllBytes(folder.resolve(name)),bytes))return new Entry(name,png);
            name=clean.substring(0,clean.length()-4)+" ("+(counter++)+").png";
        }
        Path temp=Files.createTempFile(folder,"import-",".tmp");
        try {Files.write(temp,bytes);Files.move(temp,folder.resolve(name),StandardCopyOption.REPLACE_EXISTING);}
        finally {Files.deleteIfExists(temp);}
        return new Entry(name,png);
    }
    public static String displayName(String filename) {
        return filename.replaceFirst("-[0-9a-fA-F]{12}(?=\\.png$)","");
    }
    public static List<String> list(Path folder) throws IOException {
        if(!Files.isDirectory(folder))return List.of();
        try(var files=Files.list(folder)) {
            return files.filter(Files::isRegularFile).map(p->p.getFileName().toString()).filter(n->n.toLowerCase(Locale.ROOT).endsWith(".png"))
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
    }
    public static Entry load(Path folder,String name) throws IOException {
        if(name==null||name.isBlank()||!Path.of(name).getFileName().toString().equals(name))throw new IOException("Invalid library filename");
        return new Entry(name,CrosshairImages.importPng(folder.resolve(name)));
    }
}
