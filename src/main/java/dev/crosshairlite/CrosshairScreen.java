package dev.crosshairlite;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import dev.crosshairlite.glass.client.LiquidGlassUniforms;
import dev.crosshairlite.glass.client.api.ReGlassApi;
import dev.crosshairlite.glass.client.api.WidgetStyle;
import java.io.IOException;
import java.util.function.*;

public final class CrosshairScreen extends Screen {
    private final Screen parent;
    private CrosshairConfig draft = CrosshairLite.config.copy();
    private int tab, row, column, x0, controlWidth;
    private DesignerCanvas canvas;
    private int crosshairSection;
    private int configScroll;
    private record ConfigControl(net.minecraft.client.gui.widget.ClickableWidget widget,int y) {}
    private final java.util.List<ConfigControl> configControls=new java.util.ArrayList<>();
    private EditorLayout layout;
    private final DesignerHistory designerHistory=new DesignerHistory();
    private GlassDropdown dropdown;
    private static final CrosshairConfig.Shape[] TYPES={CrosshairConfig.Shape.CROSS,CrosshairConfig.Shape.CIRCLE,CrosshairConfig.Shape.X,CrosshairConfig.Shape.IMAGE,CrosshairConfig.Shape.DESIGNER};
    private static final java.util.List<String> TYPE_NAMES=java.util.List.of("Cross","Circle","X","Import","Drawn");
    private final java.nio.file.Path library=CrosshairLite.CONFIG_PATH.getParent().resolve("crosshairlite/imports");
    private boolean importing,painting;
    private boolean bindingKey;
    private int autosaveTicks;
    private String savedSnapshot=draft.snapshot();
    private String status = "Changes save automatically.";
    private static final String[] TABS={"Settings","Crosshair","Designer","Import","Valorant"};
    private int valorantSection;
    private TextFieldWidget codeField;
    public CrosshairScreen(Screen parent) { super(Text.literal("Better Crosshairlite")); this.parent=parent; }
    @Override protected void init() {
        dropdown=null;configControls.clear();configScroll=0;
        status="Changes save automatically.";
        layout=EditorLayout.of(width,height,tab==2);
        x0=layout.x()+10;controlWidth=(layout.configWidth()-28)/2;row=0;column=0;
        int tabWidth=(width-32)/TABS.length;
        for(int i=0;i<TABS.length;i++) {
            final int selected=i;
            GlassWidgets.GlassButton b=addDrawableChild(GlassWidgets.button(Text.literal(TABS[i]),button->{tab=selected;clearAndInit();}).dimensions(16+i*tabWidth,40,tabWidth-4,20).build());
            b.active=tab!=i;
        }
        if(tab==4&&draft.mode()!=CrosshairConfig.Mode.VALORANT)draft.selectMode(CrosshairConfig.Mode.VALORANT);
        switch(tab) {
            case 1 -> {
                String[] sections={"Shape & position","Style","Targets"};
                int sectionWidth=(layout.configWidth()-20)/3;
                for(int i=0;i<sections.length;i++) {
                    final int selectedSection=i;
                    var control=addDrawableChild(GlassWidgets.button(Text.literal(sections[i]),b->{crosshairSection=selectedSection;clearAndInit();}).dimensions(x0+i*sectionWidth,70,sectionWidth-4,20).build());
                    control.active=crosshairSection!=i;
                }
                if(crosshairSection==0) {
                var defaultTypes=java.util.List.of(CrosshairConfig.Shape.CROSS,CrosshairConfig.Shape.CIRCLE,CrosshairConfig.Shape.X);
                CrosshairConfig.Shape selectedShape=draft.mode()==CrosshairConfig.Mode.DEFAULT?draft.shape:draft.defaultShape;
                int selected=defaultTypes.indexOf(selectedShape);
                dropdown=addDrawableChild(new GlassDropdown(x0,94,controlWidth*2+8,java.util.List.of("Cross","Circle","X"),Math.max(0,selected),3,index->{draft.selectDefaultShape(defaultTypes.get(index));clearAndInit();}));
                dropdown.setPrefix("Shape: ");row=1;column=0;
                if(selectedShape==CrosshairConfig.Shape.CROSS || selectedShape==CrosshairConfig.Shape.X) {
                    slider("Length",1,40,()->draft.size,v->draft.size=v);
                    slider("Gap",0,30,()->draft.gap,v->draft.gap=v);
                    slider("Thickness",1,10,()->draft.thickness,v->draft.thickness=v);
                    slider("Opacity %",0,100,()->draft.opacity,v->draft.opacity=v);
                    toggle("Top arm",()->draft.top,v->draft.top=v);
                    toggle("Bottom arm",()->draft.bottom,v->draft.bottom=v);
                    toggle("Left arm",()->draft.left,v->draft.left=v);
                    toggle("Right arm",()->draft.right,v->draft.right=v);
                } else if(selectedShape==CrosshairConfig.Shape.CIRCLE) {
                    slider("Radius",1,40,()->draft.size,v->draft.size=v);
                    slider("Opacity %",0,100,()->draft.opacity,v->draft.opacity=v);
                }
                slider("Horizontal offset",-100,100,()->draft.offsetX,v->draft.offsetX=v);
                slider("Vertical offset",-100,100,()->draft.offsetY,v->draft.offsetY=v);

                } else if(crosshairSection==1) {
                color("Crosshair",()->draft.color,v->draft.color=v);
                color("Outline",()->draft.outlineColor,v->draft.outlineColor=v);
                toggle("Outline",()->draft.outline,v->draft.outline=v);
                slider("Outline width",1,4,()->draft.outlineWidth,v->draft.outlineWidth=v);
                toggle("Rainbow",()->draft.rainbow,v->draft.rainbow=v);
                slider("Rainbow speed",1,20,()->draft.rainbowSpeed,v->draft.rainbowSpeed=v);
                toggle("Center dot",()->draft.centerDot,v->draft.centerDot=v);
                slider("Dot size",1,12,()->draft.dotSize,v->draft.dotSize=v);

                } else {
                    toggle("Shield active",()->draft.shieldActiveColorEnabled,v->draft.shieldActiveColorEnabled=v);
                    toggle("Shield disabled",()->draft.shieldDisabledColorEnabled,v->draft.shieldDisabledColorEnabled=v);
                    ((net.minecraft.client.gui.widget.ClickableWidget)children().getLast()).setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Tracks the shield-disable sound for the vanilla 5-second cooldown. Custom server cooldowns may differ.")));
                    color("Shield active",()->draft.shieldActiveColor,v->draft.shieldActiveColor=v);
                    color("Shield disabled",()->draft.shieldDisabledColor,v->draft.shieldDisabledColor=v);
                    toggle("Hostile mob",()->draft.hostileColorEnabled,v->draft.hostileColorEnabled=v);
                    row++;column=0;
                    toggle("Passive mob",()->draft.passiveColorEnabled,v->draft.passiveColorEnabled=v);
                    row++;column=0;
                    toggle("Player color",()->draft.playerColorEnabled,v->draft.playerColorEnabled=v);
                    row++;column=0;
                    color("Hostile mob",()->draft.hostileColor,v->draft.hostileColor=v);
                    color("Passive mob",()->draft.passiveColor,v->draft.passiveColor=v);
                    color("Player",()->draft.playerColor,v->draft.playerColor=v);

                }
            }
            case 0 -> {
                dropdown=addDrawableChild(new GlassDropdown(x0,70,controlWidth*2+8,java.util.List.of("Default","Import","Drawn","Valorant"),draft.mode().ordinal(),4,index->{draft.selectMode(CrosshairConfig.Mode.values()[index]);clearAndInit();}));
                dropdown.setPrefix("Crosshair mode: ");row=1;column=0;
                toggle("Enabled",()->draft.enabled,v->draft.enabled=v);
                slider("Dynamic amount",0,20,()->draft.dynamicAmount,v->draft.dynamicAmount=v);
                toggle("Attack dynamics",()->draft.attackDynamics,v->draft.attackDynamics=v);
                toggle("Item dynamics",()->draft.itemDynamics,v->draft.itemDynamics=v);
                toggle("Show in F3",()->draft.showInF3,v->draft.showInF3=v);
                toggle("Hide in third person",()->draft.hideThirdPerson,v->draft.hideThirdPerson=v);
                toggle("Vanilla attack indicator",()->draft.vanillaAttackIndicator,v->draft.vanillaAttackIndicator=v);
                toggle("Hide in spectator",()->draft.hideSpectator,v->draft.hideSpectator=v);
                button(bindingKey?"Press a key…":"Open editor: "+CrosshairLite.open.getBoundKeyLocalizedText().getString(),()->{
                    bindingKey=true;clearAndInit();status="Press a key to bind. Escape cancels; Delete unbinds.";
                });
            }
            case 2 -> initDesigner();
            case 3 -> initImport();
            case 4 -> initValorant();
        }
        if(tab==1||tab==4) {
            for(var child:children()) if(child instanceof net.minecraft.client.gui.widget.ClickableWidget widget && widget.getY()>=94)
                configControls.add(new ConfigControl(widget,widget.getY()));
            updateConfigScroll();
        }
        addDrawableChild(GlassWidgets.button(Text.literal("Done"),b->close()).dimensions(width/2-50,height-30,100,20).build());
    }
    private static int pct(double v) { return (int)Math.round(v*100); }
    private void initValorant() {
        String[] sections={"General","Inner lines","Outer lines","Code"};
        int sectionWidth=(layout.configWidth()-20)/sections.length;
        for(int i=0;i<sections.length;i++) {
            final int selectedSection=i;
            var control=addDrawableChild(GlassWidgets.button(Text.literal(sections[i]),b->{valorantSection=selectedSection;clearAndInit();}).dimensions(x0+i*sectionWidth,70,sectionWidth-4,20).build());
            control.active=valorantSection!=i;
        }
        final ValorantProfile p=draft.valorant;
        switch(valorantSection) {
            case 0 -> {
                dropdown=addDrawableChild(new GlassDropdown(x0,94,controlWidth*2+8,ValorantProfile.PRESET_NAMES,Math.max(0,Math.min(8,p.color)),5,index->{p.color=index;clearAndInit();}));
                dropdown.setPrefix("Color: ");row=1;column=0;
                if(p.color==8) color("Custom",()->p.customRgb,v->p.customRgb=v);
                toggle("Outlines",()->p.outlines,v->p.outlines=v);
                slider("Outline opacity %",0,100,()->pct(p.outlineOpacity),v->p.outlineOpacity=v/100.0);
                slider("Outline thickness",0,6,()->p.outlineThickness,v->p.outlineThickness=v);
                row++;column=0;
                toggle("Center dot",()->p.centerDot,v->p.centerDot=v);
                slider("Dot opacity %",0,100,()->pct(p.dotOpacity),v->p.dotOpacity=v/100.0);
                slider("Dot thickness",0,6,()->p.dotThickness,v->p.dotThickness=v);
                row++;column=0;
                toggle("Fade with firing error",()->p.fade,v->p.fade=v);
            }
            case 1 -> valorantLines(p.inner);
            case 2 -> valorantLines(p.outer);
            default -> {
                codeField=new GlassWidgets.GlassTextField(textRenderer,nextX(),nextY(),controlWidth*2+8,20,Text.literal("Valorant crosshair code"));
                codeField.setMaxLength(1000);codeField.setText(p.toCode());
                addDrawableChild(codeField);row++;column=0;
                button("Import code from box",()->importValorantCode(codeField.getText()));
                button("Paste and import",()->importValorantCode(client.keyboard.getClipboard()));
                button("Copy code",()->{client.keyboard.setClipboard(draft.valorant.toCode());status="Copied crosshair code to the clipboard.";});
                button("Reset to default",()->{draft.valorant=new ValorantProfile();clearAndInit();status="Reset to the default crosshair.";});
            }
        }
    }
    private void valorantLines(ValorantProfile.Lines g) {
        toggle("Show lines",()->g.show,v->g.show=v);
        slider("Opacity %",0,100,()->pct(g.opacity),v->g.opacity=v/100.0);
        slider("Thickness",0,10,()->g.thickness,v->g.thickness=v);
        slider(g.separate?"Horizontal length":"Length",0,20,()->g.length,v->g.length=v);
        toggle("Split H/V length",()->g.separate,v->{g.separate=v;if(v)g.lengthV=g.length;clearAndInit();});
        if(g.separate) slider("Vertical length",0,20,()->g.lengthV,v->g.lengthV=v);
        slider("Offset",0,40,()->g.offset,v->g.offset=v);
        toggle("Movement error",()->g.moveError,v->g.moveError=v);
        slider("Movement multiplier %",0,300,()->pct(g.moveMult),v->g.moveMult=v/100.0);
        toggle("Firing error",()->g.fireError,v->g.fireError=v);
        slider("Firing multiplier %",0,300,()->pct(g.fireMult),v->g.fireMult=v/100.0);
    }
    private void importValorantCode(String code) {
        try {
            draft.valorant=ValorantProfile.fromCode(code);
            draft.shape=CrosshairConfig.Shape.VALORANT;
            clearAndInit();
            status="Imported crosshair code. Saved automatically.";
        } catch(IllegalArgumentException e) {
            status="Could not import: "+e.getMessage();
        }
    }
    private void renderValorantPreview(DrawContext context) {
        int previewX=layout.previewX(),previewY=layout.y(),side=layout.previewSize();
        ReGlassApi.create(context).dimensions(previewX,previewY,side,side).cornerRadius(14)
            .style(WidgetStyle.create().tint(0xFFFFFF,0.10f).blurRadius(8)).render();
        double centerX=Math.round(previewX+side/2.0),centerY=Math.round(previewY+side/2.0);
        double guiScale=Math.max(1.0,client.getWindow().getScaleFactor());
        ValorantRenderer.drawPreview(context,draft.valorant,centerX,centerY,2.0/guiScale);
        context.drawCenteredTextWithShadow(textRenderer,Text.literal("Valorant crosshair (2x)"),(int)centerX,previewY+side-22,0xFFE5EDF2);
    }
    private void initDesigner() {
        int cell=layout.cellSize(),side=33*cell;
        int gridX=layout.gridX(),gridY=layout.gridY();
        int centerY=layout.y()+layout.height()/2;
        canvas=addDrawableChild(new DesignerCanvas(gridX,gridY,cell,draft,designerHistory));
        int toolWidth=Math.max(30,Math.min(120,gridX-layout.x()-20));
        int toolX=gridX-10-toolWidth;
        int rightX=gridX+side+10,rightWidth=toolWidth;
        int pitch=Math.max(18,Math.min(26,(layout.height()-10)/5)),toolY=centerY-(pitch*4+18)/2;
        var pencil=addDrawableChild(GlassWidgets.button(Text.literal("Pencil"),b->canvas.erase=false).dimensions(toolX,toolY,toolWidth,18).build());
        var erase=addDrawableChild(GlassWidgets.button(Text.literal("Erase"),b->canvas.erase=true).dimensions(toolX,toolY+pitch,toolWidth,18).build());
        pencil.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Draw with the left mouse button")));
        erase.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Erase with the left mouse button; right-drag also erases")));
        addDrawableChild(GlassWidgets.button(Text.literal("Redo"),b->canvas.redo()).dimensions(toolX,toolY+pitch*2,toolWidth,18).build());
        addDrawableChild(GlassWidgets.button(Text.literal("Undo"),b->canvas.undo()).dimensions(toolX,toolY+pitch*3,toolWidth,18).build());
        addDrawableChild(GlassWidgets.button(Text.literal("Clear"),b->canvas.clear()).dimensions(toolX,toolY+pitch*4,toolWidth,18).build());
        addDrawableChild(new GlassWidgets.GlassSlider(rightX,toolY+pitch,rightWidth,18,Text.empty(),0) {
            {updateMessage();}
            @Override protected void updateMessage(){setMessage(Text.literal("Brush: "+canvas.brush));}
            @Override protected void applyValue(){canvas.brush=1+(int)Math.round(value*4);updateMessage();}
        });
        addDrawableChild(new GlassWidgets.GlassSlider(rightX,toolY+pitch*2,rightWidth,18,Text.empty(),(draft.customSize-4)/156.0) {
            {updateMessage();}
            @Override protected void updateMessage(){setMessage(Text.literal("Size: "+draft.customSize));}
            @Override protected void applyValue(){draft.customSize=4+(int)Math.round(value*156);updateMessage();}
        });
        status="Left-drag to draw; right-drag to erase. Aim is the center of the grid.";
    }
    private void initImport() {
        button("Choose PNG…",this::choosePng);
        java.util.List<String> files;
        try {files=ImportedCrosshairs.list(library);}
        catch(IOException e){files=java.util.List.of();status="Could not read imported crosshairs.";}
        final var names=files;
        final var displayNames=names.stream().map(ImportedCrosshairs::displayName).toList();
        dropdown=addDrawableChild(new GlassDropdown(nextX(),nextY(),controlWidth,displayNames,names.indexOf(draft.imageFile),Math.max(1,Math.min(6,(layout.height()-28)/20)),index->selectImported(names.get(index))));
        dropdown.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Saved imported crosshairs")));advance();
        slider("Image size",4,160,()->draft.imageSize,v->draft.imageSize=v);
        toggle("Image outline",()->draft.imageOutline,v->draft.imageOutline=v);
        toggle("Smooth image",()->draft.imageSmoothing,v->draft.imageSmoothing=v);
        status="Imported PNGs are kept in config/crosshairlite/imports.";
    }
    private void selectImported(String filename) {
        if(importing)return;
        importing=true;status="Loading PNG…";
        java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try{return ImportedCrosshairs.load(library,filename);}
            catch(Exception e){throw new java.util.concurrent.CompletionException(e);}
        }).whenComplete((entry,error)->client.execute(()->completeImport(entry,error)));
    }
    private void completeImport(ImportedCrosshairs.Entry entry,Throwable error) {
        importing=false;
        if(client.currentScreen!=this)return;
        if(error!=null){Throwable cause=error.getCause()==null?error:error.getCause();status="Import failed: "+cause.getMessage();return;}
        draft.imagePng=entry.png();draft.imageFile=entry.filename();draft.imageTint=false;draft.trimImage=true;draft.shape=CrosshairConfig.Shape.IMAGE;
        clearAndInit();
        if(autosave())status="Selected "+ImportedCrosshairs.displayName(entry.filename())+". Saved automatically.";
    }
    private void choosePng() {
        if(importing)return;
        importing=true;
        java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try(var stack=org.lwjgl.system.MemoryStack.stackPush()) {
                var filters=stack.mallocPointer(1);filters.put(0,stack.UTF8("*.png"));
                return org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Import crosshair PNG","",filters,"PNG images",false);
            }
        }).whenComplete((path,error)->client.execute(()->{
            importing=false;
            if(client.currentScreen!=this)return;
            if(error!=null)status="File picker unavailable. Drop the PNG onto the game window instead.";
            else if(path!=null)loadPng(path);
        }));
    }
    private void loadPng(String path) {
        if(importing)return;
        if(path==null||path.isBlank()){status="Choose or paste a PNG file path first.";return;}
        importing=true;status="Importing PNG…";
        java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try {return ImportedCrosshairs.importFile(library,java.nio.file.Path.of(path.strip().replaceAll("^\"|\"$","")));}
            catch(Exception e){throw new java.util.concurrent.CompletionException(e);}
        }).whenComplete((entry,error)->client.execute(()->completeImport(entry,error)));
    }

    @Override public boolean mouseClicked(net.minecraft.client.gui.Click click,boolean doubled) {
        if(dropdown!=null && dropdown.open)return dropdown.mouseClicked(click,doubled);
        if(tab==2 && canvas!=null && canvas.mouseClicked(click,doubled)) {
            painting=true;setFocused(canvas);return true;
        }
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(net.minecraft.client.gui.Click click,double dx,double dy) {
        if(painting && tab==2)return canvas.mouseDragged(click,dx,dy);
        return super.mouseDragged(click,dx,dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        if(painting){painting=false;return true;}
        return super.mouseReleased(click);
    }
    @Override public void onFilesDropped(java.util.List<java.nio.file.Path> paths) {
        if(paths.isEmpty())return;
        tab=3;clearAndInit();loadPng(paths.getFirst().toString());
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(dropdown!=null && dropdown.open)return dropdown.mouseScrolled(x,y,horizontal,vertical);
        if((tab==1||tab==4) && x>=layout.x() && x<layout.x()+layout.configWidth() && y>=94 && y<layout.y()+layout.height()) {
            configScroll-= (int)Math.round(vertical*22);updateConfigScroll();return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyInput key) {
        if(bindingKey) {
            bindingKey=false;
            if(key.key()!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                CrosshairLite.open.setBoundKey(key.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE
                    ?net.minecraft.client.util.InputUtil.UNKNOWN_KEY:net.minecraft.client.util.InputUtil.fromKeyCode(key));
                net.minecraft.client.option.KeyBinding.updateKeysByCode();
                client.options.write();
            }
            clearAndInit();return true;
        }
        if(CrosshairLite.open.matchesKey(key)) { close();return true; }
        if(dropdown!=null && dropdown.open && dropdown.keyPressed(key))return true;
        return super.keyPressed(key);
    }
    private void updateConfigScroll() {
        int bottom=layout.y()+layout.height()-6;
        int maximum=configControls.stream().mapToInt(c->c.y()+c.widget().getHeight()).max().orElse(bottom);
        configScroll=Math.max(0,Math.min(configScroll,Math.max(0,maximum-bottom)));
        for(var control:configControls) {
            var widget=control.widget();widget.setY(control.y()-configScroll);
            widget.visible=widget.getY()>=94 && widget.getY()+widget.getHeight()<=bottom;
        }
    }
    private int nextX() { return x0+column*(controlWidth+8); }
    private int nextY() { return (tab==1||tab==4?94:70)+row*Math.min(26,Math.max(20,(height-136)/6)); }
    private void advance() { column++; if(column==2) {column=0;row++;} }
    private void button(String label,Runnable action) {
        addDrawableChild(GlassWidgets.button(Text.literal(label),b->action.run()).dimensions(nextX(),nextY(),controlWidth,20).build());advance();
    }
    private void toggle(String label,BooleanSupplier get,Consumer<Boolean> set) {
        addDrawableChild(GlassWidgets.button(Text.literal(label+": "+(get.getAsBoolean()?"On":"Off")),b->{set.accept(!get.getAsBoolean());b.setMessage(Text.literal(label+": "+(get.getAsBoolean()?"On":"Off")));}).dimensions(nextX(),nextY(),controlWidth,20).build());advance();
    }
    private void slider(String label,int min,int max,IntSupplier get,IntConsumer set) {
        addDrawableChild(new GlassWidgets.GlassSlider(nextX(),nextY(),controlWidth,20,Text.empty(),(get.getAsInt()-min)/(double)(max-min)) {
            { updateMessage(); }
            @Override protected void updateMessage() { setMessage(Text.literal(label+": "+get.getAsInt())); }
            @Override protected void applyValue() { set.accept(min+(int)Math.round(value*(max-min)));updateMessage(); }
        });advance();
    }
    private void color(String label,IntSupplier get,IntConsumer set) {
        GlassWidgets.GlassButton caption=addDrawableChild(GlassWidgets.button(Text.literal(label+" color"),b->{}).dimensions(nextX(),nextY(),controlWidth,20).build());
        caption.active=false;advance();
        TextFieldWidget field=new GlassWidgets.GlassTextField(textRenderer,nextX(),nextY(),controlWidth,20,Text.literal(label+" hex color"));
        field.setMaxLength(7);field.setText(String.format("#%06X",get.getAsInt()));
        field.setChangedListener(value->{
            if(value.matches("#?[0-9a-fA-F]{6}")) {set.accept(Integer.parseInt(value.replace("#",""),16));field.setEditableColor(0xFFFFFFFF);}
            else field.setEditableColor(0xFFFF7777);
        });
        field.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(label+": enter a six-digit hex color, e.g. #55FFFF")));
        addDrawableChild(field);advance();
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        LiquidGlassUniforms.get().tryApplyBlur(context);
        ReGlassApi.create(context).dimensions(layout.x(),layout.y(),layout.configWidth(),layout.height()).cornerRadius(14)
            .style(WidgetStyle.create().tint(0xFFFFFF,0.12f).blurRadius(8).shadow(12f,0.2f,0f,3f)).render();
        context.drawCenteredTextWithShadow(textRenderer,title,width/2,16,0xFFFFFFFF);
        if(tab!=2) renderPreview(context);
        if(tab==1||tab==4) {
            int bottom=layout.y()+layout.height()-6;
            int contentBottom=configControls.stream().mapToInt(c->c.y()+c.widget().getHeight()).max().orElse(bottom);
            int maxScroll=Math.max(0,contentBottom-bottom);
            if(maxScroll>0) {
                int trackHeight=bottom-96,thumbHeight=Math.max(12,trackHeight*trackHeight/(trackHeight+maxScroll));
                int thumbY=96+configScroll*(trackHeight-thumbHeight)/maxScroll;
                ReGlassApi.create(context).dimensions(layout.x()+layout.configWidth()-6,96,3,trackHeight).cornerRadius(1.5f)
                    .style(WidgetStyle.create().tint(0xFFFFFF,0.10f).blurRadius(8)).render();
                ReGlassApi.create(context).dimensions(layout.x()+layout.configWidth()-6,thumbY,3,thumbHeight).cornerRadius(1.5f)
                    .style(WidgetStyle.create().tint(0xFFFFFF,0.5f).blurRadius(8)).render();
            }
        }
        context.drawCenteredTextWithShadow(textRenderer,Text.literal(status),width/2,height-43,0xFFE5EDF2);
        // Glass is composited separately from vanilla text. Suppress covered controls
        // so their labels and glass surfaces cannot bleed through the popup.
        var covered=new java.util.ArrayList<net.minecraft.client.gui.widget.ClickableWidget>();
        if(dropdown!=null && dropdown.open) for(var child:children()) {
            if(child instanceof net.minecraft.client.gui.widget.ClickableWidget widget
                    && widget!=dropdown && widget.visible && dropdown.covers(widget)) {
                covered.add(widget);widget.visible=false;
            }
        }
        try { super.render(context,mouseX,mouseY,delta); }
        finally { for(var widget:covered)widget.visible=true; }
        if(dropdown!=null && dropdown.open) {
            context.createNewRootLayer();
            dropdown.renderPopup(context);
        }
    }
    private void renderPreview(DrawContext context) {
        if(draft.shape==CrosshairConfig.Shape.VALORANT){renderValorantPreview(context);return;}
        int previewX=layout.previewX(),previewY=layout.y(),side=layout.previewSize();
        ReGlassApi.create(context).dimensions(previewX,previewY,side,side).cornerRadius(14)
            .style(WidgetStyle.create().tint(0xFFFFFF,0.10f).blurRadius(8)).render();
        double centerX=previewX+side/2.0,centerY=previewY+side/2.0;
        context.getMatrices().pushMatrix();context.getMatrices().translate((float)centerX,(float)centerY);
        context.getMatrices().translate(-0.5f,-0.5f);
        int guide=Math.min(30,side/4);
        context.fill(-guide,0,guide+1,1,0x226C869B);context.fill(0,-guide,1,guide+1,0x226C869B);
        context.getMatrices().translate(0.5f,0.5f);
        int spread=0; // Preview stays at rest; item actions animate the in-game crosshair only.
        double extent=switch(draft.shape){
            case IMAGE -> draft.imageSize/2.0;
            case DESIGNER -> draft.customSize/2.0;
            case DOT -> draft.dotSize/2.0;
            default -> draft.size+draft.gap+spread+draft.thickness;
        };
        float previewScale=(float)Math.min(1,Math.max(8,(side-58)/2.0)/Math.max(1,extent+draft.outlineWidth));
        context.getMatrices().scale(previewScale,previewScale);
        CrosshairRenderer.draw(context,draft,0,0,spread,false);
        context.getMatrices().popMatrix();
        String name=draft.shape==CrosshairConfig.Shape.IMAGE&&!draft.imageFile.isEmpty()?ImportedCrosshairs.displayName(draft.imageFile):TYPE_NAMES.get(Math.max(0,java.util.Arrays.asList(TYPES).indexOf(draft.shape)));
        context.drawCenteredTextWithShadow(textRenderer,Text.literal(textRenderer.trimToWidth(name,side-12)),(int)centerX,previewY+side-22,0xFFE5EDF2);
    }
    private boolean autosave() {
        String current=draft.snapshot();
        if(current.equals(savedSnapshot))return true;
        try {
            draft.save(CrosshairLite.CONFIG_PATH);
            CrosshairLite.config=draft.copy();savedSnapshot=draft.snapshot();return true;
        } catch(IOException e) {
            status="Autosave failed. Your changes are still here.";
            CrosshairLite.LOGGER.error("Saving crosshair settings failed",e);return false;
        }
    }
    @Override public void tick() {
        super.tick();
        if(++autosaveTicks>=5){autosaveTicks=0;autosave();}
    }
    @Override public void removed() { autosave();super.removed(); }
    @Override public void close() { if(autosave())client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
