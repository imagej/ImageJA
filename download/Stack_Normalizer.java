import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/* J. Walter 2002-01-28 */

/** The Stack_Normalizer recalculates the grey levels of the stack, so that the minimum and maximum grey level after normalization are equal to the specified values.
The minimum and maximum grey levels are determined in the whole stack and not just in one plane. For RGB images all channels are normalized to the same min/max values. */
public class Stack_Normalizer implements PlugIn {
    static double newMin = 0.0;
    static double newMax = 255.0;

    public void run(String arg) {
        ImagePlus imp = IJ.getImage();
        GenericDialog gd = new GenericDialog("Normalize to:", IJ.getInstance());
        int bitDepth = imp.getBitDepth();
        int digits = bitDepth==32?2:0;
        gd.addNumericField("Minimum", newMin, digits);
        gd.addNumericField("Maximum", newMax, digits);
        gd.showDialog();
        if (gd.wasCanceled()) return;
        if (gd.invalidNumber()) {
            IJ.showMessage("Error", "Invalid input Number");
            return;
        }
        newMin = gd.getNextNumber();
        newMax = gd.getNextNumber();
        if (bitDepth==24)
            normalizeColorStack(imp);
        else
            normalizeStack(imp);
        imp.getProcessor().resetMinAndMax();
        imp.updateAndRepaintWindow();
    }

    void normalizeStack(ImagePlus imp) {
        ImageStack stack = imp.getStack();
        int size = stack.getSize();
        double v;
        int width, height;
        int rx, ry, rw, rh;

        ImageProcessor ip = imp.getProcessor();
        width = ip.getWidth();
        height = ip.getHeight();
        Rectangle roi = ip.getRoi();
        byte[] mask = ip.getMaskArray();

        if (roi != null) {
            rx = roi.x;
            ry = roi.y;
            rw = roi.width;
            rh = roi.height;
        } else {
            rx = 0;
            ry = 0;
            rw = width;
            rh = height;
        }

        IJ.showStatus("Finding min and max");
        ImageStatistics stats = new StackStatistics(imp);
        double roiMin = stats.min;
        double roiMax = stats.max;
        if (roiMax<=roiMin) roiMax = roiMin+1;
        double scale = (newMax-newMin) / (roiMax-roiMin);
        double offset = (newMin*roiMax - newMax*roiMin) / (roiMax-roiMin);
        IJ.log(roiMin+"  "+roiMax+"  "+scale+"  "+offset);

        for (int slice=1; slice<=size; slice++) {
            IJ.showStatus("Normalizing: "+slice+"/"+size);
            IJ.showProgress(slice, size);
            ip = stack.getProcessor(slice);
            for (int y=0; y<height; y++) {
                int i = y * width + rx;
                for (int x=0; x<width; x++) {
                    v = ip.getPixelValue(x,y);
                    v = scale*v+offset;
                    ip.putPixelValue(x,y,v);
                    i++;
                }
            }
        }

    }

    void normalizeColorStack(ImagePlus imp) {
        ImageStack stack = imp.getStack();
        int size = stack.getSize();
        int v, r, g, b;
        int width, height;
        int rx, ry, rw, rh;

        ImageProcessor ip = imp.getProcessor();
        width = ip.getWidth();
        height = ip.getHeight();
        Rectangle roi = ip.getRoi();
        byte[] mask = ip.getMaskArray();

        if (roi != null) {
            rx = roi.x;
            ry = roi.y;
            rw = roi.width;
            rh = roi.height;
        } else {
            rx = 0;
            ry = 0;
            rw = width;
            rh = height;
        }

        // Find min and max

        int roiMinR = Integer.MAX_VALUE;
        int roiMinG = Integer.MAX_VALUE;
        int roiMinB = Integer.MAX_VALUE;
        int roiMaxR = -Integer.MAX_VALUE;
        int roiMaxG = -Integer.MAX_VALUE;
        int roiMaxB = -Integer.MAX_VALUE;

        for (int slice=1; slice<=size; slice++) {
            IJ.showStatus("MinMax: "+slice+"/"+size);
            IJ.showProgress(slice, size);
            ip = stack.getProcessor(slice);
            if (mask == null) {
                for (int y=ry; y<(ry+rh); y++) {
                    int i = y * width + rx;
                    for (int x=rx; x<(rx+rw); x++) {
                        v = ip.getPixel(x,y);
                        r = (v&0xff0000)>>16;
                        g = (v&0xff00)>>8;
                        b = (v&0xff);
                        if (r<roiMinR) roiMinR = r;
                        if (g<roiMinG) roiMinG = g;
                        if (b<roiMinB) roiMinB = b;
                        if (r>roiMaxR) roiMaxR = r;
                        if (g>roiMaxG) roiMaxG = g;
                        if (b>roiMaxB) roiMaxB = b;
                        i++;
                    }
                }
            } else {
                for (int y=ry, ym = 0; y<(ry+rh); y++, ym++) {
                    int i = y * width + rx;
                    int im = ym * rw;
                    for (int x=rx; x<(rx+rw); x++) {
                        if(mask[im]!=0) {
                            v = ip.getPixel(x,y);
                            r = (v&0xff0000)>>16;
                            g = (v&0xff00)>>8;
                            b = (v&0xff);
                            if (r<roiMinR) roiMinR = r;
                            if (g<roiMinG) roiMinG = g;
                            if (b<roiMinB) roiMinB = b;
                            if (r>roiMaxR) roiMaxR = r;
                            if (g>roiMaxG) roiMaxG = g;
                            if (b>roiMaxB) roiMaxB = b;
                        }
                        i++; im++;
                    }
                }
            }

        }

        if (roiMaxR<=roiMinR) roiMaxR = roiMinR+1;
        if (roiMaxG<=roiMinG) roiMaxG = roiMinG+1;
        if (roiMaxB<=roiMinB) roiMaxB = roiMinB+1;


        double scaleR = (newMax - newMin) / (roiMaxR - roiMinR);
        double scaleG = (newMax - newMin) / (roiMaxG - roiMinG);
        double scaleB = (newMax - newMin) / (roiMaxB - roiMinB);
        double offsetR = (newMin*roiMaxR - newMax*roiMinR) / (roiMaxR - roiMinR);
        double offsetG = (newMin*roiMaxG - newMax*roiMinG) / (roiMaxG - roiMinG);
        double offsetB = (newMin*roiMaxB - newMax*roiMinB) / (roiMaxB - roiMinB);

        for (int slice=1; slice<=size; slice++) {
            IJ.showStatus("MinMax: "+slice+"/"+size);
            IJ.showProgress(slice, size);
            ip = stack.getProcessor(slice);
            for (int y=ry; y<(ry+rh); y++) {
                int i = y * width + rx;
                for (int x=rx; x<(rx+rw); x++) {
                    v = ip.getPixel(x,y);
                    r = (v&0xff0000)>>16;
                    g = (v&0xff00)>>8;
                    b = (v&0xff);

                    r = (int) (scaleR*r+offsetR+0.5);
                    if (r<0) r=0;
                    if (r>255) r=255;
                    g = (int) (scaleG*g+offsetG+0.5);
                    if (g<0) g=0;
                    if (g>255) g=255;
                    b = (int) (scaleB*b+offsetB+0.5);
                    if (b<0) b=0;
                    if (b>255) b=255;

                    ip.putPixel(x,y,(r<<16)|(g<<8)|b);
                    i++;
                }
            }

        }

    }

}
