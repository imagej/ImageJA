import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/** Splits an RGB image or stack into three 8-bit grayscale images or stacks (hue, saturation and brightness). */

public class HSB_Stack_Splitter implements PlugInFilter {
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
         ImageStack hsbStack = imp.getStack();
         ImageStack hueStack = new ImageStack(w,h);
         ImageStack satStack = new ImageStack(w,h);
         ImageStack brightStack = new ImageStack(w,h);
         byte[] hue,s,b;
         ColorProcessor cp;
         int n = hsbStack.getSize();
         for (int i=1; i<=n; i++) {
             IJ.showStatus(i+"/"+n);
             hue = new byte[w*h];
             s = new byte[w*h];
             b = new byte[w*h];
             cp = (ColorProcessor)hsbStack.getProcessor(1);
             cp.getHSB(hue,s,b);
             hsbStack.deleteSlice(1);
            //System.gc();
             hueStack.addSlice(null,hue);
             satStack.addSlice(null,s);
             brightStack.addSlice(null,b);
             IJ.showProgress((double)i/n);
        }
        String title = imp.getTitle();
        imp.hide();
        new ImagePlus(title+" (hue)",hueStack).show();
        new ImagePlus(title+" (saturation)",satStack).show();
        new ImagePlus(title+" (brightness)",brightStack).show();
    }
}


