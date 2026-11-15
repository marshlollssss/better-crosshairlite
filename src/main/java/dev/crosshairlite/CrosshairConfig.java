package dev.crosshairlite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;

public final class CrosshairConfig {
    public enum Shape { CROSS, T, X, DOT, CIRCLE, SQUARE, DIAMOND, DESIGNER, IMAGE, VALORANT }
    public int configVersion = 2;
    public boolean editorKeyMigrated = false;
    public Shape shape = Shape.CROSS;
    public Shape defaultShape = Shape.CROSS;
    public enum Mode { DEFAULT, IMPORT, DRAWN, VALORANT }
    public ValorantProfile valorant = new ValorantProfile();
    public Mode mode() { return shape==Shape.IMAGE?Mode.IMPORT:shape==Shape.DESIGNER?Mode.DRAWN:shape==Shape.VALORANT?Mode.VALORANT:Mode.DEFAULT; }
    public void selectMode(Mode mode) {
        if(mode()==Mode.DEFAULT)defaultShape=shape;
        shape=switch(mode){case DEFAULT -> defaultShape;case IMPORT -> Shape.IMAGE;case DRAWN -> Shape.DESIGNER;case VALORANT -> Shape.VALORANT;};
    }
    public void selectDefaultShape(Shape selected) {
        if(selected!=Shape.CROSS&&selected!=Shape.CIRCLE&&selected!=Shape.X)return;
        defaultShape=selected;
        if(mode()==Mode.DEFAULT)shape=selected;
    }
    public boolean enabled = true, outline = true, centerDot = false, rainbow = false;
    public boolean vanillaAttackIndicator=false;
    public boolean shieldActiveColorEnabled=false,shieldDisabledColorEnabled=false;
    public int shieldActiveColor=0x55FF55,shieldDisabledColor=0xFF5555;
    public boolean hostileColorEnabled=false,playerColorEnabled=false,passiveColorEnabled=false;
    public int hostileColor=0xFF5555,playerColor=0x55AAFF,passiveColor=0x55FF88;
    public boolean showInF3 = true, itemDynamics = true, attackDynamics = true;
    public boolean dynamic = false, targetColor = false, hideUsingItem = false, hideThirdPerson = true;
    public boolean hideSpectator = true, top = true, bottom = true, left = true, right = true;
    public int size = 7, gap = 3, thickness = 2, dotSize = 2, outlineWidth = 1;
    public int color = 0xFFFFFF, outlineColor = 0x101820, target = 0xFF6666;
    public int opacity = 100, offsetX = 0, offsetY = 0, dynamicAmount = 5, rainbowSpeed = 5;
    public int customSize = 33, imageSize = 32;
    public boolean imageTint = false, trimImage = true, imageOutline = false, imageSmoothing = false;
    public String imagePng = "";
    public String imageFile = "";
    public boolean[] drawing = new boolean[33*33];
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public String snapshot() { return GSON.toJson(this); }
    public CrosshairConfig copy() { return GSON.fromJson(GSON.toJson(this), CrosshairConfig.class); }
    public void sanitize() {
        configVersion=3;
        if(valorant==null)valorant=new ValorantProfile();
        valorant.sanitize();
        if(imageFile==null)imageFile="";
        imageTint=false;trimImage=true;
        if(shape==Shape.T){shape=Shape.CROSS;top=false;}
        if(shape==Shape.DOT){shape=Shape.CIRCLE;size=Math.max(1,(dotSize+1)/2);}
        if(shape==Shape.SQUARE||shape==Shape.DIAMOND)shape=Shape.CROSS;
        customSize = clamp(customSize,4,160); imageSize = clamp(imageSize,4,160);
        if (imagePng == null || imagePng.length()>24_000_000) imagePng = "";
        if (drawing == null || drawing.length!=33*33) drawing = new boolean[33*33];
        if (shape == null) shape = Shape.CROSS;
        if(defaultShape!=Shape.CROSS&&defaultShape!=Shape.CIRCLE&&defaultShape!=Shape.X)defaultShape=Shape.CROSS;
        if(mode()==Mode.DEFAULT)defaultShape=shape;
        size = clamp(size,1,40); gap = clamp(gap,0,30); thickness = clamp(thickness,1,10);
        dotSize = clamp(dotSize,1,12); outlineWidth = clamp(outlineWidth,1,4); opacity = clamp(opacity,0,100);
        offsetX = clamp(offsetX,-100,100); offsetY = clamp(offsetY,-100,100);
        dynamicAmount = clamp(dynamicAmount,0,20); rainbowSpeed = clamp(rainbowSpeed,1,20);
        shieldActiveColor &= 0xFFFFFF;shieldDisabledColor &= 0xFFFFFF;
        hostileColor &= 0xFFFFFF;playerColor &= 0xFFFFFF;passiveColor &= 0xFFFFFF;
        color &= 0xFFFFFF; outlineColor &= 0xFFFFFF; target &= 0xFFFFFF;
    }
    private static int clamp(int v,int min,int max) { return Math.max(min,Math.min(max,v)); }
    public static CrosshairConfig load(Path path) {
        try {
            if (Files.exists(path)) {
                var json=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                CrosshairConfig c = GSON.fromJson(json,CrosshairConfig.class);
                if(!json.has("configVersion") && c!=null && c.color==0x55FFFF) c.color=0xFFFFFF;
                if (c == null) throw new IllegalArgumentException("Empty configuration");
                if(!json.has("attackDynamics"))c.attackDynamics=c.itemDynamics;
                c.sanitize();
                Path library=path.getParent().resolve("crosshairlite/imports");
                if(!c.imageFile.isEmpty()) {
                    try { c.imagePng=ImportedCrosshairs.load(library,c.imageFile).png(); }
                    catch(IOException e) { CrosshairLite.LOGGER.warn("Saved crosshair PNG unavailable; retaining embedded fallback",e); }
                } else if(!c.imagePng.isEmpty()) {
                    try { c.imageFile=ImportedCrosshairs.store(library,"imported.png",c.imagePng).filename(); }
                    catch(IOException e) { CrosshairLite.LOGGER.warn("Could not migrate PNG into library",e); }
                }
                return c;
            }
        } catch (IOException | RuntimeException e) {
            CrosshairLite.LOGGER.warn("Could not read crosshair settings; using defaults",e);
        }
        return new CrosshairConfig();
    }
    public void save(Path path) throws IOException {
        sanitize(); Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(),"crosshair-", ".tmp");
        try {
            Files.writeString(temp,GSON.toJson(this));
            try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public static CrosshairConfig preset(int i) {
        CrosshairConfig c = new CrosshairConfig();
        switch (i) {
            case 1 -> { c.shape=Shape.CIRCLE; c.size=2; c.dotSize=3; c.color=0xFFFFFF; }
            case 2 -> { c.shape=Shape.CIRCLE; c.size=8; c.thickness=1; c.centerDot=true; }
            case 3 -> { c.shape=Shape.CROSS; c.top=false; c.size=6; c.gap=4; c.color=0x99FF66; }
            case 4 -> { c.shape=Shape.X; c.size=5; c.gap=2; c.color=0xFF99DD; }
            case 5 -> { c.shape=Shape.DESIGNER; c.color=0xFFCC66;
                for(int y=0;y<33;y++)for(int x=0;x<33;x++)if(Math.abs(x-16)+Math.abs(y-16)==7)c.drawing[y*33+x]=true; }
            default -> { }
        }
        return c;
    }
}
