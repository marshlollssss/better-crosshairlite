package dev.crosshairlite;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import java.util.List;
import java.util.function.Consumer;

final class GlassDropdown extends ClickableWidget {
    private final List<String> labels;
    private final Consumer<Integer> select;
    private int selected,first,highlight;
    private String prefix="";
    private final int rows;
    boolean open;
    GlassDropdown(int x,int y,int width,List<String> labels,int selected,int maxRows,Consumer<Integer> select) {
        super(x,y,width,20,Text.literal(labels.isEmpty()?"No imported crosshairs":selected<0?"Choose imported crosshair":labels.get(selected)));
        this.labels=labels;this.selected=selected;this.highlight=Math.max(0,selected);this.select=select;rows=Math.max(1,Math.min(maxRows,labels.size()));
        active=!labels.isEmpty();
    }
    void setPrefix(String prefix) { this.prefix=prefix;setMessage(Text.literal(prefix+getMessage().getString())); }
    @Override protected void renderWidget(DrawContext context,int mx,int my,float delta) {
        GlassWidgets.surface(context,this,mx,my);
        var font=MinecraftClient.getInstance().textRenderer;
        GlassWidgets.label(context,this);
        context.drawTextWithShadow(font,open?"−":"v",getX()+width-14,getY()+6,0xFFFFFFFF);
    }
    boolean covers(ClickableWidget widget) {
        return widget.getX()<getX()+width && widget.getX()+widget.getWidth()>getX()
            && widget.getY()<getY()+22+rows*20 && widget.getY()+widget.getHeight()>getY()+22;
    }
    void renderPopup(DrawContext context) {
        if(!open)return;
        var font=MinecraftClient.getInstance().textRenderer;
        dev.crosshairlite.glass.client.api.ReGlassApi.create(context).dimensions(getX(),getY()+22,width,rows*20).cornerRadius(8)
            .style(dev.crosshairlite.glass.client.api.WidgetStyle.create().layer(13).tint(0x1A2230,0.68f).blurRadius(12).smoothing(0.003f).shadow(8f,0.25f,0f,2f)).render();
        for(int row=0;row<rows;row++) {
            int index=first+row;if(index>=labels.size())break;
            int y=getY()+22+row*20;
            if(index==highlight)dev.crosshairlite.glass.client.api.ReGlassApi.create(context).dimensions(getX()+3,y,width-6,20).cornerRadius(6)
                .style(dev.crosshairlite.glass.client.api.WidgetStyle.create().layer(14).tint(0xFFFFFF,0.22f).blurRadius(8)).render();
            context.drawCenteredTextWithShadow(font,Text.literal(font.trimToWidth(labels.get(index),Math.max(1,width-12))),getX()+width/2,y+6,0xFFFFFFFF);
        }
        if(labels.size()>rows)context.fill(getX()+width-3,getY()+20+first*rows*20/labels.size(),getX()+width-1,getY()+20+(first+rows)*rows*20/labels.size(),0xFFDAE7F0);
    }
    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(open) {
            if(click.x()>=getX()&&click.x()<getX()+width&&click.y()>=getY()+22&&click.y()<getY()+22+rows*20) {
                choose(first+(int)((click.y()-getY()-22)/20));return true;
            }
            open=false;return true;
        }
        if(active&&isMouseOver(click.x(),click.y())&&click.button()==0) {
            open=true;highlight=Math.max(0,selected);first=Math.max(0,Math.min(selected,labels.size()-rows));return true;
        }
        return false;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(!open)return false;
        first=Math.max(0,Math.min(labels.size()-rows,first-(int)Math.signum(vertical)));return true;
    }
    @Override public boolean keyPressed(KeyInput key) {
        if(!active)return false;
        if(key.key()==256&&open){open=false;return true;}
        if(key.key()==257||key.key()==32){if(open)choose(highlight);else open=true;return true;}
        if(key.key()==264||key.key()==265) {
            open=true;highlight=Math.max(0,Math.min(labels.size()-1,highlight+(key.key()==264?1:-1)));
            first=Math.max(0,Math.min(labels.size()-rows,Math.min(highlight,Math.max(first,highlight-rows+1))));return true;
        }
        return false;
    }
    private void choose(int index) {open=false;selected=index;highlight=index;setMessage(Text.literal(prefix+labels.get(index)));select.accept(index);}
    @Override protected void appendClickableNarrations(NarrationMessageBuilder builder){appendDefaultNarrations(builder);}
}
