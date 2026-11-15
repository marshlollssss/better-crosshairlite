package dev.crosshairlite;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairModeTest {
    @Test void defaultShapeSurvivesSwitchingBetweenSources() {
        var c=new CrosshairConfig();c.selectDefaultShape(CrosshairConfig.Shape.X);
        c.selectMode(CrosshairConfig.Mode.IMPORT);assertEquals(CrosshairConfig.Shape.IMAGE,c.shape);
        c.selectMode(CrosshairConfig.Mode.DRAWN);assertEquals(CrosshairConfig.Shape.DESIGNER,c.shape);
        c.selectMode(CrosshairConfig.Mode.DEFAULT);assertEquals(CrosshairConfig.Shape.X,c.shape);
    }
    @Test void editingDefaultShapeDoesNotSwitchActiveImportedMode() {
        var c=new CrosshairConfig();c.selectMode(CrosshairConfig.Mode.IMPORT);
        c.selectDefaultShape(CrosshairConfig.Shape.CIRCLE);assertEquals(CrosshairConfig.Mode.IMPORT,c.mode());
        c.selectMode(CrosshairConfig.Mode.DEFAULT);assertEquals(CrosshairConfig.Shape.CIRCLE,c.shape);
    }
}
