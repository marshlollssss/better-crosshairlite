package dev.crosshairlite;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShieldTargetsTest {
    @Test void shieldColorsAreOptionalAndIndependent() {
        var c=new CrosshairConfig();
        assertNull(ShieldTargets.select(c,true,false));
        assertNull(ShieldTargets.select(c,false,true));
        c.shieldActiveColorEnabled=true;
        assertEquals(0x55FF55,ShieldTargets.select(c,true,false));
        assertNull(ShieldTargets.select(c,false,true));
        c.shieldDisabledColorEnabled=true;c.shieldDisabledColor=0xAA1234;
        assertEquals(0xAA1234,ShieldTargets.select(c,false,true));
        assertNull(ShieldTargets.select(c,false,false));
    }
    @Test void preferencesSurviveCopyAndColorSanitizing() {
        var c=new CrosshairConfig();c.vanillaAttackIndicator=true;
        c.shieldActiveColorEnabled=true;c.shieldDisabledColorEnabled=true;
        c.shieldActiveColor=0xFF123456;c.shieldDisabledColor=0xFFABCDEF;c.sanitize();
        var copy=c.copy();assertTrue(copy.vanillaAttackIndicator);
        assertTrue(copy.shieldActiveColorEnabled);assertTrue(copy.shieldDisabledColorEnabled);
        assertEquals(0x123456,copy.shieldActiveColor);assertEquals(0xABCDEF,copy.shieldDisabledColor);
    }
}
