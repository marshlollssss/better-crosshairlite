package dev.crosshairlite;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import dev.crosshairlite.glass.client.api.ReGlassApi;
import dev.crosshairlite.glass.client.api.WidgetStyle;

/** Explicit API rendering: independent of ReGlass's automatic game-wide redesign. */
final class GlassWidgets {
    private GlassWidgets() {}

    static void surface(DrawContext context, ClickableWidget widget, int mouseX, int mouseY) {
        ReGlassApi.create(context).dimensions(widget.getX(),widget.getY(),widget.getWidth(),widget.getHeight())
            .cornerRadius(7).hover(widget.active && widget.isMouseOver(mouseX,mouseY)?1f:0f)
            .focus(widget.isFocused()?1f:0f)
            .style(WidgetStyle.create().layer(11).tint(0xFFFFFF,widget.active?0.12f:0.06f).blurRadius(8).smoothing(0.003f).shadow(3f,0.18f,0f,1f)).render();

    }

    static Builder button(Text text, java.util.function.Consumer<GlassButton> action) { return new Builder(text,action); }
    static final class Builder {
        private final Text text;
        private final java.util.function.Consumer<GlassButton> action;
        private int x,y,width,height;
        Builder(Text text,java.util.function.Consumer<GlassButton> action) { this.text=text;this.action=action; }
        Builder dimensions(int x,int y,int width,int height) { this.x=x;this.y=y;this.width=width;this.height=height;return this; }
        GlassButton build() { return new GlassButton(x,y,width,height,text,action); }
    }
    static final class GlassButton extends ClickableWidget {
        private final java.util.function.Consumer<GlassButton> action;
        GlassButton(int x,int y,int width,int height,Text text,java.util.function.Consumer<GlassButton> action) {
            super(x,y,width,height,text);this.action=action;
        }
        @Override protected void renderWidget(DrawContext context,int mouseX,int mouseY,float delta) {
            surface(context,this,mouseX,mouseY);label(context,this);
        }
        @Override public void onClick(net.minecraft.client.gui.Click click,boolean doubled) { action.accept(this); }
        @Override public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
            if(active && visible && isFocused() && (input.key()==257 || input.key()==32 || input.key()==335)) {
                playDownSound(MinecraftClient.getInstance().getSoundManager());action.accept(this);return true;
            }
            return false;
        }
        @Override protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }
    static void label(DrawContext context,ClickableWidget widget) {
        var renderer=MinecraftClient.getInstance().textRenderer;
        float scale=Math.min(1f,Math.max(0.65f,(widget.getWidth()-8f)/Math.max(1,renderer.getWidth(widget.getMessage()))));
        String text=renderer.trimToWidth(widget.getMessage().getString(),Math.max(1,(int)((widget.getWidth()-8)/scale)));
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(widget.getX()+widget.getWidth()/2f,widget.getY()+widget.getHeight()/2f);
        context.getMatrices().scale(scale,scale);
        context.drawCenteredTextWithShadow(renderer,Text.literal(text),0,-4,widget.active?0xFFFFFFFF:0xFFA6B7C4);
        context.getMatrices().popMatrix();
    }
    abstract static class GlassSlider extends SliderWidget {
        GlassSlider(int x,int y,int width,int height,Text text,double value) { super(x,y,width,height,text,value); }
        @Override public void renderWidget(DrawContext context,int mouseX,int mouseY,float delta) {
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            ReGlassApi.create(context).dimensions(x,y,w,h).cornerRadius(7)
                .style(WidgetStyle.create().layer(11).tint(0xFFFFFF,0.15f).blurRadius(8).smoothing(0.003f)).render();
            int knobX=x+(int)Math.round(value*(w-8));
            ReGlassApi.create(context).dimensions(knobX,y+2,8,h-4).cornerRadius(4)
                .hover(isHovered()?1f:0f).focus(isFocused()?1f:0f)
                .style(WidgetStyle.create().layer(12).tint(0xFFFFFF,0.48f).blurRadius(8).smoothing(0.003f).shadow(3f,0.2f,0f,1f)).render();
            label(context,this);
        }
    }
    static final class GlassTextField extends TextFieldWidget {
        GlassTextField(TextRenderer renderer,int x,int y,int width,int height,Text text) {
            super(renderer,x,y,width,height,text);
            setCentered(true);
            // Retain vanilla editing, selection, caret and narration over a local glass surface.
        }
        @Override public void renderWidget(DrawContext context,int mouseX,int mouseY,float delta) {
            surface(context,this,mouseX,mouseY);
            setDrawsBackground(false);
            int originalX=getX(),originalY=getY(),originalWidth=getWidth();
            setX(originalX+4);setY(originalY+6);setWidth(originalWidth-8);
            try { super.renderWidget(context,mouseX,mouseY,delta); }
            finally { setX(originalX);setY(originalY);setWidth(originalWidth); }
        }
    }
}
