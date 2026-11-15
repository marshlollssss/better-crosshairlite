package dev.crosshairlite;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EditorLayoutTest {
    @Test void previewStaysCompactAndConfigAlwaysGetsMoreSpace() {
        for(int width:new int[]{320,427,640,960,1470,2940})for(int height:new int[]{240,360,720,956}) {
            var l=EditorLayout.of(width,height,false);
            assertTrue(l.configWidth()>l.previewSize());assertTrue(l.previewSize()<=210);
            assertEquals(l.x()+l.width(),l.previewX()+l.previewSize());
            assertTrue(l.y()+l.height()<height-43);assertTrue(l.x()>=0);
        }
    }
    @Test void designerHasNoPreviewAndGridIsCenteredWithinHalfAPixel() {
        for(int width:new int[]{320,427,640,960,1470})for(int height:new int[]{240,360,720,956}) {
            var l=EditorLayout.of(width,height,true);int side=33*l.cellSize();
            assertEquals(0,l.previewSize());assertEquals(l.width(),l.configWidth());
            assertEquals(width/2.0,l.gridX()+side/2.0,1);
            assertEquals(l.y()+l.height()/2.0,l.gridY()+side/2.0,0.5);
            assertTrue(l.gridX()>l.x()+50);assertTrue(l.gridY()>=l.y());
        }
    }
}
