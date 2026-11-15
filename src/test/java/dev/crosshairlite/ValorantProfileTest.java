package dev.crosshairlite;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValorantProfileTest {
    @Test void parsesProCode() {
        var p=ValorantProfile.fromCode("0;s;1;P;c;5;h;0;m;1;0l;4;0o;2;0a;1;0f;0;1b;0;S;c;4;o;1");
        assertEquals(0x00FFFF,p.rgb());assertFalse(p.outlines);assertTrue(p.overrideFiringOffset);
        assertTrue(p.inner.show);assertEquals(4,p.inner.length);assertEquals(2,p.inner.offset);assertEquals(2,p.inner.thickness);
        assertFalse(p.inner.fireError);assertFalse(p.outer.show);
    }
    @Test void customColorAndUnknownKeysSurvive() {
        var p=ValorantProfile.fromCode("0;P;c;8;u;F28D9FFF;h;0;d;1;b;1;a;0.254;0t;3;0l;5;0o;0;0a;0.755;0f;0;1b;0");
        assertEquals(0xF28D9F,p.rgb());assertTrue(p.centerDot);assertEquals(0.254,p.dotOpacity,1e-9);
        assertTrue(p.toCode().contains(";b;1;"));
    }
    @Test void splitHorizontalVertical() {
        var p=ValorantProfile.fromCode("0;P;c;7;o;1;f;0;0t;1;0l;2;0v;5;0g;1;0o;2;0a;1;0f;0;1b;0");
        assertTrue(p.inner.separate);assertEquals(2,p.inner.horizontal());assertEquals(5,p.inner.vertical());
    }
    @Test void exportImportRoundTripKeepsLayout() {
        for(String code:new String[]{
            "0;s;1;P;c;5;h;0;m;1;0l;4;0o;2;0a;1;0f;0;1b;0;S;c;4;o;1",
            "0;P;c;1;t;3;o;1;f;0;0t;6;0l;20;0o;13;0a;1;0f;0;1t;9;1l;4;1o;9;1a;1;1m;0;1f;0"}) {
            var a=ValorantProfile.fromCode(code);var b=ValorantProfile.fromCode(a.toCode());
            assertEquals(a.layout(0.3,0.4,6,8),b.layout(0.3,0.4,6,8));assertEquals(a.toCode(),b.toCode());
        }
    }
    @Test void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class,()->ValorantProfile.fromCode(""));
        assertThrows(IllegalArgumentException.class,()->ValorantProfile.fromCode("not a crosshair"));
    }
    @Test void outlinesAreDrawnBeforeFills() {
        var p=ValorantProfile.fromCode("0;P;c;5;o;1;0t;2;0l;4;0o;2;0a;1;0f;0;1b;0");
        var rects=p.layout(0,0,6,8);
        assertEquals(8,rects.size());assertTrue(rects.get(0).outline());assertFalse(rects.get(7).outline());
    }
    @Test void errorPushesLinesOutwardByMultiplier() {
        var p=ValorantProfile.fromCode("0;P;c;5;h;0;0t;2;0l;4;0o;2;0a;1;0m;1;0s;0.5;0f;1;0e;1;1b;0");
        int rest=p.layout(0,0,6,8).get(0).x1();
        assertEquals(rest+8,p.layout(0,1,6,8).get(0).x1());
        assertEquals(rest+3,p.layout(1,0,6,8).get(0).x1());
    }
    @Test void configPersistsValorantProfile() {
        var c=new CrosshairConfig();c.selectMode(CrosshairConfig.Mode.VALORANT);
        c.valorant=ValorantProfile.fromCode("0;P;c;6;h;0;0l;3;0o;1;0a;1;0f;0;1b;0");
        var copy=c.copy();
        assertEquals(CrosshairConfig.Shape.VALORANT,copy.shape);assertEquals(6,copy.valorant.color);assertEquals(3,copy.valorant.inner.length);
    }
}
