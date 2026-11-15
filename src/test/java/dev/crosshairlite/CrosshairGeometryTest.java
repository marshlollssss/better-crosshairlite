package dev.crosshairlite;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairGeometryTest {
    @Test void symmetricShapesAreCenteredForOddAndEvenThickness() {
        for(var shape:new CrosshairConfig.Shape[]{CrosshairConfig.Shape.CROSS,CrosshairConfig.Shape.X,CrosshairConfig.Shape.CIRCLE,CrosshairConfig.Shape.DOT,CrosshairConfig.Shape.SQUARE,CrosshairConfig.Shape.DIAMOND})
            for(int thickness=1;thickness<=4;thickness++) {
                var c=new CrosshairConfig();c.shape=shape;c.thickness=thickness;c.dotSize=thickness;
                var bounds=CrosshairGeometry.build(c,0).getBounds2D();
                assertEquals(0,bounds.getCenterX(),1e-6,shape.toString());assertEquals(0,bounds.getCenterY(),1e-6,shape.toString());
            }
    }
    @Test void dotsAreDisksRatherThanSquares() {
        var c=CrosshairConfig.preset(1);c.shape=CrosshairConfig.Shape.DOT;c.dotSize=10;
        var area=CrosshairGeometry.build(c,0);
        assertTrue(area.contains(0,0));assertTrue(area.contains(4,0));assertFalse(area.contains(4,4));
    }
    @Test void tacticalTRetainsAimOrigin() {
        var c=CrosshairConfig.preset(3);var area=CrosshairGeometry.build(c,0);
        assertFalse(area.contains(0,-6));assertTrue(area.contains(0,6));assertTrue(area.contains(-6,0));assertTrue(area.contains(6,0));
    }
    @Test void circleExpandsSymmetrically() {
        var c=CrosshairConfig.preset(2);
        var still=CrosshairGeometry.build(c,0).getBounds2D();var moving=CrosshairGeometry.build(c,5).getBounds2D();
        assertEquals(still.getWidth()+10,moving.getWidth(),1e-6);assertEquals(0,moving.getCenterX(),1e-6);
    }
    @Test void circlesHaveAntialiasedEdgesAndCenteredCoverage() throws Exception {
        var c=CrosshairConfig.preset(2);c.outline=false;c.centerDot=false;
        var image=CrosshairRaster.build(c,0,6).foreground();
        boolean fractional=false;double sum=0,xsum=0,ysum=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int a=image.getRGB(x,y)>>>24;fractional|=a>0&&a<255;sum+=a;xsum+=(x+0.5)*a;ysum+=(y+0.5)*a;
        }
        assertTrue(fractional);assertEquals(image.getWidth()/2.0,xsum/sum,0.03);assertEquals(image.getHeight()/2.0,ysum/sum,0.03);
    }
    @Test void outlineDoesNotCoverShapeInterior() {
        var c=new CrosshairConfig();var area=CrosshairGeometry.build(c,0);var border=CrosshairGeometry.outline(area,2);
        var intersection=new java.awt.geom.Area(area);intersection.intersect(border);assertTrue(intersection.isEmpty());
    }
    @Test void malformedValuesAreClampedAndDefaultsAreWhite() {
        var c=new CrosshairConfig();assertEquals(0xFFFFFF,c.color);c.size=Integer.MAX_VALUE;c.opacity=-1;c.shape=null;c.offsetX=-999;c.drawing=null;
        c.sanitize();assertEquals(40,c.size);assertEquals(0,c.opacity);assertEquals(-100,c.offsetX);assertNotNull(c.shape);assertEquals(1089,c.drawing.length);
    }
    @Test void circleIsFilledAndDoesNotDependOnArmSwitches() {
        var c=new CrosshairConfig();c.shape=CrosshairConfig.Shape.CIRCLE;c.size=8;
        c.top=c.bottom=c.left=c.right=false;
        var circle=CrosshairGeometry.build(c,0);
        assertTrue(circle.contains(0,0));assertTrue(circle.contains(7,0));assertTrue(circle.contains(0,-7));assertFalse(circle.contains(7,7));
    }
    @Test void tinyDrawingsKeepSymmetricCoverageAtEveryGuiScale() throws Exception {
        var c=new CrosshairConfig();c.shape=CrosshairConfig.Shape.DESIGNER;c.outline=false;
        for(int n=8;n<=24;n++){c.drawing[16*33+n]=true;c.drawing[n*33+16]=true;}
        for(int size:new int[]{4,5,8,12})for(int scale=1;scale<=4;scale++) {
            c.customSize=size;var image=CrosshairRaster.build(c,0,scale).foreground();
            long total=0;
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
                int a=image.getRGB(x,y)>>>24;total+=a;
                assertEquals(a,image.getRGB(image.getWidth()-1-x,y)>>>24,2);
                assertEquals(a,image.getRGB(x,image.getHeight()-1-y)>>>24,2);
            }
            // 33 occupied cells; integrated alpha should retain their scaled area.
            double expected=33*Math.pow(size*scale/33.0,2)*255;
            assertEquals(expected,total,expected*0.15);
        }
    }
}
