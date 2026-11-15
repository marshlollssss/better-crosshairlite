package dev.crosshairlite;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TargetColorsTest {
    @Test void eachCategoryIsIndependentlyToggleable() {
        var c=new CrosshairConfig();
        for(var kind:TargetColors.Kind.values())assertNull(TargetColors.override(c,kind));
        c.hostileColorEnabled=true;c.hostileColor=0x123456;
        assertEquals(0x123456,TargetColors.override(c,TargetColors.Kind.HOSTILE));
        assertNull(TargetColors.override(c,TargetColors.Kind.PLAYER));assertNull(TargetColors.override(c,TargetColors.Kind.PASSIVE));
        c.playerColorEnabled=true;c.playerColor=0xABCDEF;c.passiveColorEnabled=true;c.passiveColor=0x334455;
        assertEquals(0xABCDEF,TargetColors.override(c,TargetColors.Kind.PLAYER));
        assertEquals(0x334455,TargetColors.override(c,TargetColors.Kind.PASSIVE));
        c.hostileColorEnabled=false;assertNull(TargetColors.override(c,TargetColors.Kind.HOSTILE));
    }
    @Test void categoryColorsSurviveDraftCopyAndAreClamped() {
        var c=new CrosshairConfig();c.playerColorEnabled=true;c.playerColor=0xFF123456;c.sanitize();
        var copy=c.copy();assertTrue(copy.playerColorEnabled);assertEquals(0x123456,copy.playerColor);
    }
}
