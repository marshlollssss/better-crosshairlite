package dev.crosshairlite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class ImportedCrosshairsTest {
    @TempDir Path temp;
    @Test void keepsOriginalPngInLibraryAndReimportsWithoutDuplicates() throws Exception {
        Path source=temp.resolve("my crosshair.png"),folder=temp.resolve("config/imports");
        var image=new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);image.setRGB(1,1,0x80FFFFFF);ImageIO.write(image,"PNG",source.toFile());
        var first=ImportedCrosshairs.importFile(folder,source);var second=ImportedCrosshairs.importFile(folder,source);
        assertEquals("my crosshair.png",first.filename());assertEquals(first.filename(),second.filename());assertEquals(1,ImportedCrosshairs.list(folder).size());
        assertArrayEquals(Files.readAllBytes(source),Files.readAllBytes(folder.resolve(first.filename())));
        Files.delete(source);assertEquals(first.png(),ImportedCrosshairs.load(folder,first.filename()).png());
    }
    @Test void selectionCannotEscapeLibrary() {
        assertThrows(java.io.IOException.class,()->ImportedCrosshairs.load(temp,"../outside.png"));
    }
    @Test void emptyFolderHasEmptySelectionList() throws Exception {
        assertTrue(ImportedCrosshairs.list(temp.resolve("missing")).isEmpty());
    }
    @Test void oldHashedNamesDisplayCleanlyAndCollisionsKeepBothPngs() throws Exception {
        assertEquals("nbPlus.png",ImportedCrosshairs.displayName("nbPlus-a1b2c3d4e5f6.png"));
        Path folder=temp.resolve("library"),file=temp.resolve("cross.png");
        var image=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);ImageIO.write(image,"PNG",file.toFile());
        var one=ImportedCrosshairs.importFile(folder,file);
        image.setRGB(0,0,0xFFFFFFFF);ImageIO.write(image,"PNG",file.toFile());
        var two=ImportedCrosshairs.importFile(folder,file);
        assertEquals("cross.png",one.filename());assertEquals("cross (2).png",two.filename());
        assertEquals(2,ImportedCrosshairs.list(folder).size());
    }
}
