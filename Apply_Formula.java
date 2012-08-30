import ij.*;
import ij.process.*;
import ij.plugin.filter.*;

public class Apply_Formula implements PlugInFilter {
    ImagePlus imp;
    ImageStatistics stats;

    /** This plugin demonstrates how to apply a formula to every pixel in
        an image or stack. It works with all image types (except RGB), it is 
        multi-threaded when processing stacks, and it supports Undo. */
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        stats = imp!=null?imp.getStatistics():null;
        return DOES_ALL-DOES_RGB+DOES_STACKS+PARALLELIZE_STACKS;
    }

    public void run(ImageProcessor ip) {
        for (int i=0; i<ip.getPixelCount(); i++) {
            double v1 = ip.getf(i);
            double v2 = Math.pow(10, v1/stats.max);
            ip.setf(i, (float)v2);
        }
        ip.setMinAndMax(0, 10);
    }

}
