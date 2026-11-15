package dev.crosshairlite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairConfigTest {
    @TempDir Path temp;
    @Test void savesAndReloadsCustomizedSettings() throws Exception {
        Path file=temp.resolve("nested/settings.json");
        CrosshairConfig c=new CrosshairConfig();c.shape=CrosshairConfig.Shape.DIAMOND;c.color=0x123456;c.offsetX=-17;c.dynamic=true;
        c.save(file);
        CrosshairConfig loaded=CrosshairConfig.load(file);
        assertEquals(c.shape,loaded.shape);assertEquals(c.color,loaded.color);assertEquals(-17,loaded.offsetX);assertTrue(loaded.dynamic);
        assertEquals(1,Files.list(file.getParent()).count());
    }
    @Test void draftChangesDoNotChangeOriginal() {
        CrosshairConfig original=new CrosshairConfig();CrosshairConfig draft=original.copy();draft.size=30;draft.color=0;
        assertEquals(7,original.size);assertEquals(0xFFFFFF,original.color);
    }
    @Test void autosaveSnapshotDetectsDrawingAndSettingEdits() throws Exception {
        CrosshairConfig c=new CrosshairConfig();String before=c.snapshot();
        c.drawing[544]=true;assertNotEquals(before,c.snapshot());
        Path file=temp.resolve("autosave.json");c.save(file);
        assertEquals(c.snapshot(),CrosshairConfig.load(file).snapshot());
        before=c.snapshot();c.opacity=42;assertNotEquals(before,c.snapshot());
        c.save(file);assertEquals(42,CrosshairConfig.load(file).opacity);
    }
    @Test void savingClampsInvalidValues() throws Exception {
        CrosshairConfig c=new CrosshairConfig();c.gap=-2;c.rainbowSpeed=999;c.color=0xFF123456;
        Path file=temp.resolve("settings.json");c.save(file);CrosshairConfig loaded=CrosshairConfig.load(file);
        assertEquals(0,loaded.gap);assertEquals(20,loaded.rainbowSpeed);assertEquals(0x123456,loaded.color);
    }
}
