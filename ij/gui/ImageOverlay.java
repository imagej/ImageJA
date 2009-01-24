package ij.gui;

import java.awt.*;
import java.awt.image.*;
import ij.*;

/** Other data structures might be more space efficient, but
 * for simplicity with draw I used a standard 32-bit structure */
public class ImageOverlay {
    
    boolean display = false;
    protected int width, height;
    protected ImagePlus imp;
    protected MemoryImageSource source;
    protected ColorModel cm;
    protected Image img;
    protected int[] pixels; // 32-bit
    
    
    public ImageOverlay(ImagePlus imp) {
        this.imp = imp;
        width = imp.getWidth();
        height = imp.getHeight();
        cm = ColorModel.getRGBdefault();
        pixels = new int[width*height];
        source = new MemoryImageSource(width, height, cm, pixels, 0, width);
        source.setAnimated(true);
        source.setFullBufferUpdates(true);
        img = Toolkit.getDefaultToolkit().createImage(source);
        reset();
        show();
    }
    
    
    public void show() {
        display = true;
    }
    
    public void hide() {
        display = false;
    }
    public void setPixel(int x, int y, int i) {
        if ((x>=0) && (y>=0) && (x < width) && (y < height)) {
            pixels[x+ width*y] = i;
        }
    }
    
    public void setLine(int x1, int y1, int x2, int y2, int i) {
        int dx = x2-x1;
        int dy = y2-y1;
        int absdx = dx>=0?dx:-dx;
        int absdy = dy>=0?dy:-dy;
        int n = absdy>absdx?absdy:absdx;
        double xinc = (double)dx/n;
        double yinc = (double)dy/n;
        n++;
        double x = x1<0?x1-0.5:x1+0.5;
        double y = y1<0?y1-0.5:y1+0.5;
        do {
            setPixel((int)x, (int)y,i);
            x += xinc;
            y += yinc;
        } while (--n>0);
    }
    
    public void reset(){
        img = Toolkit.getDefaultToolkit().createImage(source);
        imp.getWindow().getCanvas().repaint();
        //Necessary to update from within a mouse event, but seems wrong
    }
    
    public int getPixel(int x, int y){
        return pixels[x+ width*y];
    }
    public Image getImage() {
        return img;
    }
}
