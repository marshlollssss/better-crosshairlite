package dev.crosshairlite;

/** Shared responsive measurements for widgets, glass panels and preview content. */
record EditorLayout(int x,int y,int width,int height,int configWidth,int previewX,int previewSize,boolean designer) {
    static EditorLayout of(int screenWidth,int screenHeight,boolean designer) {
        int width=Math.min(1100,screenWidth-32);
        int x=(screenWidth-width)/2,y=66;
        int height=Math.max(126,Math.min(designer?520:300,screenHeight-122));
        int preview=designer?0:Math.min(height,Math.max(80,Math.min(210,(int)(width*0.26))));
        int config=designer?width:width-preview-12;
        return new EditorLayout(x,y,width,height,config,x+config+12,preview,designer);
    }
    int cellSize() { return Math.max(1,Math.min((height-24)/33,(width-156)/33)); }
    int gridX() { return x+(width-33*cellSize())/2; }
    int gridY() { return y+(height-33*cellSize())/2; }
}
