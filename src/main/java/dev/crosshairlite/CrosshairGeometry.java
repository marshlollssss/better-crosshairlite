package dev.crosshairlite;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.*;

/** Continuous geometry centered at (0,0), independent of GUI scale and pixel parity. */
public final class CrosshairGeometry {
    private CrosshairGeometry() {}
    public static Area build(CrosshairConfig c,int spread) {
        Area area=new Area();
        double r=c.size+spread,g=c.gap+spread,t=c.thickness;
        switch(c.shape) {
            case CROSS,T -> {
                if(c.top && c.shape!=CrosshairConfig.Shape.T) add(area,new Rectangle2D.Double(-t/2,-g-c.size,t,c.size));
                if(c.bottom) add(area,new Rectangle2D.Double(-t/2,g,t,c.size));
                if(c.left) add(area,new Rectangle2D.Double(-g-c.size,-t/2,c.size,t));
                if(c.right) add(area,new Rectangle2D.Double(g,-t/2,c.size,t));
            }
            case X -> {
                double k=Math.sqrt(0.5);
                if(c.top) line(area,-g*k,-g*k,-(g+c.size)*k,-(g+c.size)*k,t);
                if(c.right) line(area,g*k,-g*k,(g+c.size)*k,-(g+c.size)*k,t);
                if(c.left) line(area,-g*k,g*k,-(g+c.size)*k,(g+c.size)*k,t);
                if(c.bottom) line(area,g*k,g*k,(g+c.size)*k,(g+c.size)*k,t);
            }
            case DOT -> add(area,new Ellipse2D.Double(-c.dotSize/2.0,-c.dotSize/2.0,c.dotSize,c.dotSize));
            case CIRCLE -> {
                add(area,new Ellipse2D.Double(-r,-r,2*r,2*r));
            }
            case SQUARE -> {
                add(area,new Rectangle2D.Double(-r,-r,2*r,2*r));
                double inner=Math.max(0,r-t);
                area.subtract(new Area(new Rectangle2D.Double(-inner,-inner,2*inner,2*inner)));
            }
            case DIAMOND -> {
                Path2D path=new Path2D.Double();path.moveTo(0,-r);path.lineTo(r,0);path.lineTo(0,r);path.lineTo(-r,0);path.closePath();
                add(area,new BasicStroke((float)t,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER).createStrokedShape(path));
            }
            case DESIGNER -> {
                double cell=(c.customSize+spread*2)/33.0;
                for(int y=0;y<33;y++) for(int x=0;x<33;x++)
                    if(c.drawing[y*33+x]) add(area,new Rectangle2D.Double((x-16.5)*cell,(y-16.5)*cell,cell,cell));
            }
            case IMAGE,VALORANT -> { }
        }
        if(c.centerDot && c.shape!=CrosshairConfig.Shape.IMAGE && c.shape!=CrosshairConfig.Shape.DESIGNER)
            add(area,new Ellipse2D.Double(-c.dotSize/2.0,-c.dotSize/2.0,c.dotSize,c.dotSize));
        return area;
    }
    private static void add(Area area,Shape shape) { area.add(new Area(shape)); }
    private static void line(Area area,double x1,double y1,double x2,double y2,double t) {
        add(area,new BasicStroke((float)t,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND).createStrokedShape(new Line2D.Double(x1,y1,x2,y2)));
    }
    public static Area outline(Area area,int width) {
        Area border=new Area(new BasicStroke(width*2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND).createStrokedShape(area));
        border.subtract(area);return border;
    }
}
