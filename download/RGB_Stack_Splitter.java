import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/** Splits an RGB image or stack into three 8-bit grayscale images or stacks. */

public class RGB_Stack_Splitter implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB+NO_UNDO;
    }

    public void run(ImageProcessor ip) {
        splitStack(imp);
    }

    public void splitStack(ImagePlus imp) {
         int w = imp.getWidth();
         int h = imp.getHeight();
         ImageStack rgbStack = imp.getStack();
         ImageStack redStack = new ImageStack(w,h);
         ImageStack greenStack = new ImageStack(w,h);
         ImageStack blueStack = new ImageStack(w,h);
         byte[] r,g,b;
         ColorProcessor cp;
         int n = rgbStack.getSize();
         for (int i=1; i<=n; i++) {
             IJ.showStatus(i+"/"+n);
             r = new byte[w*h];
             g = new byte[w*h];
             b = new byte[w*h];
             cp = (ColorProcessor)rgbStack.getProcessor(1);
             cp.getRGB(r,g,b);
             rgbStack.deleteSlice(1);
             //System.gc();
             redStack.addSlice(null,r);
             greenStack.addSlice(null,g);
             blueStack.addSlice(null,b);
             IJ.showProgress((double)i/n);
        }
        String title = imp.getTitle();
        imp.hide();
        new ImagePlus(title+" (red)",redStack).show();
        new ImagePlus(title+" (green)",greenStack).show();
        new ImagePlus(title+" (blue)",blueStack).show();
    }
}



