package dev.crosshairlite;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

final class DesignerCanvas extends ClickableWidget {
    private final CrosshairConfig draft;
    private final int cell;
    private final DesignerHistory history;
    boolean erase;
    int brush=1,lastX=-1,lastY=-1;
    DesignerCanvas(int x,int y,int cell,CrosshairConfig draft,DesignerHistory history) {
        super(x,y,33*cell,33*cell,Text.literal("Crosshair designer: left-drag to draw, right-drag to erase"));
        this.cell=cell;this.draft=draft;this.history=history;
    }
    void snapshot() { history.snapshot(draft.drawing); }
    void undo() { draft.drawing=history.undo(draft.drawing); }
    void redo() { draft.drawing=history.redo(draft.drawing); }
    void clear() { snapshot();draft.drawing=new boolean[33*33]; }
    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(!isMouseOver(click.x(),click.y()) || (click.button()!=0&&click.button()!=1)) return false;
        snapshot();lastX=lastY=-1;paint(click);return true;
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy) {
        if(click.button()!=0&&click.button()!=1) return false;
        paint(click);return true;
    }
    private void paint(Click click) {
        int x=Math.max(0,Math.min(32,(int)((click.x()-getX())/cell)));
        int y=Math.max(0,Math.min(32,(int)((click.y()-getY())/cell)));
        boolean value=click.button()!=1&&!erase;
        if(lastX<0) {lastX=x;lastY=y;}
        int steps=Math.max(Math.abs(x-lastX),Math.abs(y-lastY));
        for(int i=0;i<=steps;i++) {
            int px=steps==0?x:(int)Math.round(lastX+(x-lastX)*i/(double)steps);
            int py=steps==0?y:(int)Math.round(lastY+(y-lastY)*i/(double)steps);
            for(int by=0;by<brush;by++) for(int bx=0;bx<brush;bx++) {
                int xx=px+bx-brush/2,yy=py+by-brush/2;
                set(xx,yy,value);
            }
        }
        lastX=x;lastY=y;draft.shape=CrosshairConfig.Shape.DESIGNER;
    }
    private void set(int x,int y,boolean value) { if(x>=0&&y>=0&&x<33&&y<33) draft.drawing[y*33+x]=value; }
    @Override protected void renderWidget(DrawContext ctx,int mouseX,int mouseY,float delta) {
        for(int y=0;y<33;y++) for(int x=0;x<33;x++) {
            int color=draft.drawing[y*33+x]?0xFF000000|draft.color:((x+y)%2==0?0xFF39434A:0xFF272E34);
            ctx.fill(getX()+x*cell,getY()+y*cell,getX()+(x+1)*cell,getY()+(y+1)*cell,color);
        }
        // Cell boundaries are distinct from the checkerboard; the aim guide bisects the center cell.
        for(int i=0;i<=33;i++) {
            int x=getX()+i*cell,y=getY()+i*cell;
            ctx.fill(x,getY(),x+1,getY()+height+1,0x665F737F);
            ctx.fill(getX(),y,getX()+width+1,y+1,0x665F737F);
        }
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(getX()+width/2f,getY()+height/2f);
        ctx.getMatrices().translate(-0.5f,-0.5f);
        ctx.fill(0,-height/2,1,height/2+1,0xBB9ADFFF);
        ctx.fill(-width/2,0,width/2+1,1,0xBB9ADFFF);
        ctx.getMatrices().popMatrix();
    }
    @Override protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }
}
