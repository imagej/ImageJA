import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.filter.EDM;
import ij.*;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
* Sample code for binary erosion using the EDM
*
* Note that the definition of 'radius' here is different from that
* in the RankFilters (Process>Filters>Sow Circular Masks).
* E.g., radius=1 erodes only the 4-connected nearest pixels, not the
* (8-connected) 3x3 neighborhood of a background pixel
*
* Code by Michael Schmid, version 2009-04-24
*/
public class Erode_Demo implements ExtendedPlugInFilter, DialogListener {
    // Filter parameters are static, thus remembered as defaults for the next invocation
    // Note that this makes it impossible to run the filter in parallel threads with different filter parameters!
    private static double radius = 2;           // how much to erode
    private static boolean fromEdge;            // whether to erode from the edge

    private boolean previewing = false;
    private FloatProcessor previewEdm;
    private boolean lastFromEdge;
    private int flags = DOES_8G|SUPPORTS_MASKING|KEEP_PREVIEW|PARALLELIZE_STACKS;

    /**
     * This method is called by ImageJ for initialization.
     * @param arg Unused here. For plugins in a .jar file this argument string can
     *            be specified in the plugins.config file of the .jar archive.
     * @param imp The ImagePlus containing the image (or stack) to process.
     * @return    The method returns flags (i.e., a bit mask) specifying the
     *            capabilities (supported formats, etc.) and needs of the filter.
     *            See PlugInFilter.java and ExtendedPlugInFilter in the ImageJ
     *            sources for details.
     */
    public int setup(String arg, ImagePlus imp) {
        if (IJ.versionLessThan("1.42n"))        // generates an error message for older versions
            return DONE;
        return flags;
    }

    // Called by ImageJ after setup.
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        if (!imp.getProcessor().isBinary()) {
            IJ.error("8-bit binary image (0 and 255) required.");
            return DONE;
        }
        // The dialog
        GenericDialog gd = new GenericDialog(command+"...");
        gd.addNumericField("Radius", radius, 1);
        gd.addCheckbox("Erode from edges", fromEdge);
        gd.addPreviewCheckbox(pfr);             // passing pfr makes the filter ready for preview
        if (IJ.getVersion().compareTo("1.42p")>=0)
        	gd.addHelp("http://rsb.info.nih.gov/ij/plugins/erode-demo.html");
        gd.addDialogListener(this);             // the DialogItemChanged method will be called on user input
        previewing = true;
        gd.showDialog();                        // display the dialog; preview runs in the background now
        previewing = false;
        if (gd.wasCanceled()) return DONE;
        IJ.register(this.getClass());           // protect static class variables (filter parameters) from garbage collection
        return IJ.setupDialog(imp, flags);      // ask whether to process all slices of stack (if a stack)
    }

    // Called after modifications to the dialog. Returns true if valid input.
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        radius = gd.getNextNumber();
        fromEdge = gd.getNextBoolean();
        return (!gd.invalidNumber() && radius>=0);
    }

    // Process a single image.
    // When processing a full stack, called by ImageJ for each stack slice.
    public void run(ImageProcessor ip) {
        // Create the Euclidian Distance Map
        boolean background255 = (ip.isInvertedLut() && Prefs.blackBackground) ||
                (!ip.isInvertedLut() && !Prefs.blackBackground);
        int backgroundValue = background255 ? (byte)255 : 0;
        FloatProcessor floatEdm;
        if (previewing && previewEdm!=null && fromEdge==lastFromEdge) {
            floatEdm = previewEdm;              //use previous EDM during preview
        } else {
            floatEdm = new EDM().makeFloatEDM(ip, backgroundValue, fromEdge);
            if (floatEdm==null) return;         //interrupted during preview?
            previewEdm = floatEdm;
            lastFromEdge = fromEdge;
        }

        // Pixels with an EDM value < radius will be set to background
        int width = ip.getWidth();
        int height = ip.getHeight();
        byte[] bPixels = (byte[])ip.getPixels();
        float[] fPixels = (float[])floatEdm.getPixels();
        Rectangle roiRect = ip.getRoi();
        for (int y=roiRect.y; y<roiRect.y+roiRect.height; y++)
            for (int x=roiRect.x, p=x+y*width; x<roiRect.x+roiRect.width; x++,p++)
                if (fPixels[p] <= radius)
                    bPixels[p] = (byte)backgroundValue;
        return;
    }

    /** This method is called by ImageJ to set the number of calls to run(ip)
     *  corresponding to 100% of the progress bar. No progress bar here */
    public void setNPasses (int nPasses) {}

}
