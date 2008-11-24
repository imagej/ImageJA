import ij.*;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.util.StringSorter;
import java.awt.*;
import java.util.Vector;

/** This sample ImageJ plugin filter blends an image with another one,
 *  i.e., it adds another image with user-specified weight.
 *
 *  Dialog Options:
 *    - "Image 2" - allows to select the image that should be added to
 *      the current (foreground) image (named "Image1" below).
 *    - Check "Fix Weight 1 = 1" for calculating
 *          Image1 + weight2*Image2
 *      Without this option, the calculation is
 *          (1-weight2)*Image1 + weight2*Image2
 *    - "Weight 2" is the weight of the second image; it can be positive
 *      (add) or negative (subtract Image2).
 *    - If the current image is a stack, as for most ImageJ commands, a
 *      dialog is displayed asking whether to process the whole stack.
 *      If Image2 is a stack, note that the current slice of Image2 is
 *      always used, even if it is a stack of the same size as Image1.
 *
 *  Notes: Calculations are done on the raw images, not taking grayscale
 *  calibration into account.
 *
 *  Version 2008-10-25 Michael Schmid - preview added. Requires ImageJ 1.38x or later.

/*
A few things to note about (Extended)PlugInFilters:
    1) Plugins working on an image (e.g., filters) should implement the
       PlugInFilter interface. If preview is desired, the ExtendedPlugInFilter
       and DialogListener interfaces should be implemented.
    2) User plugins do not use the package statement.
    3) Plugins residing in the "plugins" folder, and with at least one
       underscore in their name, are automatically installed in the PlugIns
       menu.
    4) Plugins can be installed in other ImageJ menus by packaging them as
       JAR files.
    5) The class name ("Blend_Images") and file name ("Blend_Images.java")
       must be the same.
    6) An ImagePlus is, roughly speaking, an image or stack that has a name
       and usually its own window. An ImageProcessor (ip) carries image data
       for a single image (grayscale or 24 bit/pixel RGB color), e.g. the
       image displayed in the window of an ImagePlus (composite images are
       an exception). Depending on the image type, an ImageProcesor can be
       a ByteProcessor (8 bit), ShortProcessor (16 bit), FloatProcessor (32 bit)
       or ColorProcessor (RGB).
    7) Processing should be done in the run(ip) method. It will be called repeatedly
       to process all the slices of a stack if requested by the user.
    8) This filter works with selections ("region of interest", roi), including
       non-rectangular selections. It is the programmer's responsibility to modify
       only the pixels within the ip.getRoi() rectangle.
       With the flag SUPPORTS_MASKING, for non-rectangular rois ImageJ reverts
       out-of-roi pixels in that rectangle to the original.
    9) For images with a roi, in the run(ip) method one can rely on the
       ImageProcessor ip having the roi of the ImagePlus. Otherwise, this cannot
       be guaranteed.
   10) An ExtendedPlugInFilter is invoked by ImageJ in the folowing way:
       - setup(arg, imp):  The filter should return its flags.
       - showDialog(imp, command, pfr): usually the dialog asking for parameters.
         Preview is possible during this phase: ImageJ calls the run(ip) method.
         The filter should return its flags.
       - setNPasses(nPasses): Informs the filter how often run(ip) will be called.
       - run(ip): Processing of the image; called more than once for stack slices
         with DOES_STACKS flag or RGB images with CONVERT_TO_FLOAT flag.
       - setup("final", imp): called only if flag FINAL_PROCESSING has been specified.
       Flag DONE stops this sequence of calls.
   11) An (Extended)PlugInFilter supports Undo for single images (not for
       processing a full stack) unless one of the NO_UNDO or NO_CHANGES flags
       is specified. ImageJ takes care of storing a copy of the pixels ("snapshot")
       for undo.
Note on this filter:
       This filter uses the following methods of the ImageProcessor class:
       ip.toFloat(i, fp), ip.setPixels(i, fp) and ip.getNChannels(),
       simplifying code that does float operations on grayscale images of
       any type or on the channels of color images. See the run(ip) method below.

*/

public class Blend_Images implements ExtendedPlugInFilter, DialogListener {
    /** Flags that specify the capabilities and needs of the filter */
    private static int FLAGS =      //bitwise or of the following flags:
            DOES_ALL |              //this plugin processes 8-bit, 16-bit, 32-bit gray & 24-bit/pxl RGB
            SUPPORTS_MASKING |      //For non-rectangular ROIs: ImageJ reverts unselected pixels in the roi rectangle
            PARALLELIZE_STACKS |    //handle stack slices in parallel threads on multiprocessor machines
            KEEP_PREVIEW;           //When using preview, the preview image can be kept as a result
    /* Parameters from the dialog. These are declared static, so they are
     * remembered until ImageJ quits. Note that this makes it impossible to
     * run the plugin in parallel threads with different parameters!*/
    static String image2name = "";  //name of the other image that will be added ("Image 2")
    static float weight1;           //weight of the image that the filter is applied to ("Image 1")
    static float weight2 = 0.5f;    //weight of the other image, that will be added ("Image 2")
    static boolean fixWeight1 = false; //Whether to set weight1 = 1. Otherwise weight1 = 1 - weight2

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
    public int setup (String arg, ImagePlus imp) {
        if (IJ.versionLessThan("1.38x"))    // generates an error message for older versions
            return DONE;
        return FLAGS;
    }

    /** Ask the user for the parameters. This method of an ExtendedPlugInFilter
     *  is called by ImageJ after setup.
     * @param imp       The ImagePlus containing the image (or stack) to process.
     * @param command   The ImageJ command (as it appears the menu) that has invoked this filter
     * @param pfr       A reference to the PlugInFilterRunner, needed for preview
     * @return          Flags, i.e. a code describing supported formats etc.
     */
    public int showDialog (ImagePlus imp, String command, PlugInFilterRunner pfr) {
        String[] suitableImages = getSuitableImages(imp);  // images that we can blend with the current one
        if (suitableImages == null) {
            String type = imp.getBitDepth()==24?"RGB":"grayscale";
            IJ.error(command+" Error", "No suitable image ("+type+", "+
                    imp.getWidth()+"x"+imp.getHeight()+") to blend with");
            return DONE;
        }
        GenericDialog gd = new GenericDialog(command+"...");
        gd.addMessage("Image 1: "+imp.getTitle());
        gd.addChoice("Image 2:", suitableImages, image2name);
        gd.addCheckbox("Fix Weight 1 = 1", fixWeight1);
        gd.addNumericField("Weight 2", weight2, 3);
        gd.addPreviewCheckbox(pfr);
        gd.addDialogListener(this);
        gd.showDialog();                            // user input (or reading from macro) happens here
        if (gd.wasCanceled())                       // dialog cancelled?
            return DONE;
        IJ.register(this.getClass());       // protect static class variables (parameters) from garbage collection
        return IJ.setupDialog(imp, FLAGS);  // for stacks ask "process all slices?" and sets the DOES_STACKS flag
    }

    /** Listener to modifications of the input fields of the dialog.
     *  Here the parameters are read from the input dialog.
     *  @param gd The GenericDialog that the input belongs to
     *  @param e  The input event
     *  @return whether the input is valid and the filter may be run with these parameters
     */
    public boolean dialogItemChanged (GenericDialog gd, AWTEvent e) {
        image2name = gd.getNextChoice();
        fixWeight1 = gd.getNextBoolean();
        weight2 = (float)gd.getNextNumber();
        if (fixWeight1)
            weight1 = 1f;
        else
            weight1 = 1f - weight2;
        return !gd.invalidNumber();             //input is valid if all numeric input is ok
    }

    /**
     * This method is called by ImageJ once for a single image or repeatedly
     * for all images in a stack.
     * It creates a weighted sum (blend) of image ip and an image ip2 determined
     * previously in the dialog.
     * @param ip The image that should be processed
     */
    public void run (ImageProcessor ip) {
        ImagePlus imp2 = WindowManager.getImage(image2name);
        ImageProcessor ip2 = null;              // the image that we will blend ip with
        if (imp2 != null)
            ip2 = imp2.getProcessor();
        if (ip2 == null) 
            return;             // should never happen, we have imp2 from a list of suitable images
        FloatProcessor fp1 = null, fp2 = null;  // non-float images will be converted to these
        for (int i=0; i<ip.getNChannels(); i++) { //grayscale: once. RBG: once per color, i.e., 3 times
            fp1 = ip.toFloat(i, fp1);           // convert image or color channel to float (unless float already)
            fp2 = ip2.toFloat(i, fp2);
            blendFloat(fp1, weight1, fp2, weight2);
            ip.setPixels(i, fp1);               // convert back from float (unless ip is a FloatProcessor)
        }
    }

    /** Set the number of calls of the run(ip) method. This information is
     *  needed for displaying a progress bar; unused here.
     */
    public void setNPasses (int nPasses) {}

    /**
     * Blend a FloatProcessor (i.e., a 32-bit image) with another one, i.e.
     * set the pixel values of fp1 according to a weighted sum of the corresponding
     * pixels of fp1 and fp2. This is done for pixels in the rectangle fp1.getRoi()
     * only.
     * Note that both FloatProcessors, fp1 and fp2 must have the same width and height.
     * @param fp1 The FloatProcessor that will be modified.
     * @param weight1 The weight of the pixels of fp1 in the sum.
     * @param fp2  The FloatProcessor that will be read only.
     * @param weight2 The weight of the pixels of fp2 in the sum.
     */
    public static void blendFloat (FloatProcessor fp1, float weight1, FloatProcessor fp2, float weight2) {
        Rectangle r = fp1.getRoi();
        int width = fp1.getWidth();
        float[] pixels1 = (float[])fp1.getPixels();     // array of the pixels of fp1
        float[] pixels2 = (float[])fp2.getPixels();
        for (int y=r.y; y<(r.y+r.height); y++)          // loop over all pixels inside the roi rectangle
            for (int x=r.x; x<(r.x+r.width); x++) {
                int i = x + y*width;                    // this is how the pixels are addressed
                pixels1[i] = weight1*pixels1[i] + weight2*pixels2[i]; //the weighted sum
            }
    }


    /**
     * Get a list of open images with the same size and number of channels as the current
     * ImagePlus (number of channels is 1 for grayscale, 3 for RGB). The current image is
     * not entered in to the list.
     * @return A sorted list of the names of the images. Duplicate names are listed only once.
     */
    String[] getSuitableImages (ImagePlus imp) {
        int width = imp.getWidth();          // determine properties of the current image
        int height = imp.getHeight();
        int channels = imp.getProcessor().getNChannels();
        int thisID = imp.getID();
        int[] fullList = WindowManager.getIDList();//IDs of all open image windows
        Vector suitables = new Vector(fullList.length); //will hold names of suitable images
        for (int i=0; i<fullList.length; i++) { // check images for suitability, make condensed list
            ImagePlus imp2 = WindowManager.getImage(fullList[i]);
            if (imp2.getWidth()==width && imp2.getHeight()==height &&
                    imp2.getProcessor().getNChannels()==channels && fullList[i]!=thisID) {
                String name = imp2.getTitle();  // found suitable image
                if (!suitables.contains(name))  // enter only if a new name
                    suitables.addElement(name);
            }
        }
        if (suitables.size() == 0)
            return null;                        // nothing found
        String[] suitableImages = new String[suitables.size()];
        for (int i=0; i<suitables.size(); i++)  // vector to array conversion
            suitableImages[i] = (String)suitables.elementAt(i);
        StringSorter.sort(suitableImages);
        return suitableImages;
    }
}
