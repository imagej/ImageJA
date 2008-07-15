import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;

/** This plugin calculates the ratio between two images (single frames or stacks),
    as used in Fura-2 experiments, for example. The code -- heavily annotated to help
    beginners like myself explore the world of ImageJ -- is largely based on Image
    Calculator Plus. Paulo Magalhães, 10dec03.

    The plugin requires two images of the same width and height,
    and of the same type (8-, 16-, or 32-bit); the images must be:
        i) two single images;
       ii) two stacks with the same number of frames; or
      iii) a stack as a first image and a single frame as a second - in this case,
           the single frame will be applied throughtout the complete stack.

    The resulting image (in 32-bit) is calculated as follows:

         intR = (intA - bkgA) / (int B - bkgB) * MF

    where intA, intB and intR are the intensities of the first, second and ratio images,
    respectively; bkgA and bkgB are constant background values (entered by the user)
    for the first and second images, respectively; MF is an arbitrary multiplication factor.

    If a pixel value becomes negative after background subtraction, it is set to zero
    before ratio calculation.

    In addition, the plugin can accept clipping values for either of the images:
    if the pixel intensity (after background subtraction) is lower than the clipping value
    for that image, it is set to zero before ratio calculation; the clipping values for either
    image are set by the user.

    NB: if two corresponding pixels in the first and second images are zero, their ratio
    is set to 1 (one); i.e., "zero by zero" division equals one.

    Revision history:
        v1 (30nov03) -- basic ratio calculation, with clipping.
        v2 (10dec03) -- added constant background subtraction; implemented "zero
            divided by zero equals one" feature (any other value divided by zero yields infinity).

    Future plans:
        background correction using a ROI (in one or both images).

*/ 

public class Ratio_Plus implements PlugIn {

    static String title = "Ratio Calculator v2";
    static double k1 = 1;
    static double t1 = 0;
    static double t2 = 0;
    static double b1=0;
    static double b2=0;
    int[] wList;
    private String[] titles;
    int width1 = 0;
    int width2 = 0;
    int height1 = 0;
    int height2 = 0;
    int slices1 = 0;
    int slices2 = 0;
    int i1Index;
    int i2Index;
    ImagePlus i1;
    ImagePlus i2;
    boolean replicate;

    public void run(String arg) {
        if (IJ.versionLessThan("1.27w"))
            return;
        wList = WindowManager.getIDList();
        if (wList==null || wList.length<2) {
            IJ.showMessage(title, "You need at least two images open...");
            return;
        }
        titles = new String[wList.length];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null)
                titles[i] = imp.getTitle();
            else
                titles[i] = "";
        }

        // Get user input
        if (!showDialog())
            return;

        // Start the calculation itself
        long start = System.currentTimeMillis();
        // Let the user know that something is happening...
        IJ.showStatus("A moment please...");

        if (replicate)
            i2 = replicateImage(i2, i1.getStackSize());
        else
            i2 = duplicateImage(i2);
        if (i2==null)
            {IJ.showMessage(title, "Out of memory"); return;}

        // Do the calculation itself
        calculate(i1, i2, k1, t1, t2, b1, b2);

        // Show the result
        i2.show();

        // End - show time used
        IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");
    }
    
    public boolean showDialog() {
        GenericDialog gd = new GenericDialog(title);
        gd.addChoice("Image1 (or Stack1):", titles, titles[0]);
        gd.addNumericField("Background1:", b1, 0);
        gd.addNumericField("Clipping_Value1:", t1, 0);
        gd.addChoice("Image2 (or Stack2):", titles, titles[1]);
        gd.addNumericField("Background2:", b2, 0);
        gd.addNumericField("Clipping_Value2:", t2, 0);
        gd.addNumericField("Multiplication Factor:", k1, 1);
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        int i1Index = gd.getNextChoiceIndex();
        int i2Index = gd.getNextChoiceIndex();
        b1 = gd.getNextNumber();
        t1 = gd.getNextNumber();
        b2 = gd.getNextNumber();
        t2 = gd.getNextNumber();
        k1 = gd.getNextNumber();
        i1 = WindowManager.getImage(wList[i1Index]);
        i2 = WindowManager.getImage(wList[i2Index]);
        // Check that a proper ratio can be calculated:
        //   the two images must have the same width and height
        //   and be of the same type
        width1 = i1.getWidth();
        width2 = i2.getWidth();
        height1 = i1.getHeight();
        height2 = i2.getHeight();
        slices1 = i1.getStackSize();
        slices2 = i2.getStackSize();
        if (height1 != height2) {
            IJ.showMessage(title, "Both images must have the same height and width.");
            return false;
        }
        else if (width1 != width2) {
            IJ.showMessage(title, "Both images must have the same height and width.");
            return false;
        }
        else if (i1.getType() != i2.getType()) {
            IJ.showMessage(title, "Both images must be of the same type.");
            return false;
        }
        if (slices2 > 1) {
            if (slices1 != slices2) {
                IJ.showMessage(title, "Both stacks must be of the same size.");
                return false;
            }
        }
        if (i1.getType() == 4) {
            IJ.showMessage(title, "RGB images are not accepted.");
            return false;
        }
        // If the second image is a single frame and the first is a stack,
        //   replicate the second image into a stack
        if (slices2==1 && slices1>1)
            replicate = true;
        return true;
    }

    public void calculate(ImagePlus i1, ImagePlus i2, double k1, double t1, double t2, double b1, double b2) {
        double v1, v2;
        int width  = i1.getWidth();
        int height = i1.getHeight();
        ImageProcessor ip1, ip2;
        int slices1 = i1.getStackSize();
        int slices2 = i2.getStackSize();
        ImageStack stack1 = i1.getStack();
        ImageStack stack2 = i2.getStack();
        int currentSlice = i2.getCurrentSlice();

        for (int n=1; n<=slices2; n++) {
            ip1 = stack1.getProcessor(n<=slices1?n:slices1);
            ip2 = stack2.getProcessor(n);
            for (int x=0; x<width; x++) {
                for (int y=0; y<height; y++) {
                    // Get intA
                    v1 = ip1.getPixelValue(x,y);
                    // Subtract bkgA
                    v1 = v1-b1;
                    // Get intB
                    v2 = ip2.getPixelValue(x,y);
                    // Subtract bkgB
                    v2 = v2-b2;
                    // Make sure no negative values are present
                    if (v1 < 0)
                        v1 = 0;
                    else if (v2 < 0)
                        v2 = 0;
                    // Clip values with T1
                    if (v1< t1)
                       v1=0;
                    // Clip values with T2
                    if (v2< t2)
                       v2=0;
                    // Correct zero/zero cases
                    if (v1==0 && v2==0)  {
                       v1=1;
                       v2=1;
                    }
                    // Calculate the ratio
                    v2 = v1/v2;
                    // Apply the multiplication factor
                    v2 = v2*k1;
             // Write the result
             ip2.putPixelValue(x, y, v2);
                }   
            }  
            if (n==currentSlice) {
                i2.getProcessor().resetMinAndMax();
                i2.updateAndDraw();
            }     
            IJ.showStatus(n+"/"+slices2);
        }
    }

   ImagePlus duplicateImage(ImagePlus img1) {
        ImageStack stack1 = img1.getStack();
        int width = stack1.getWidth();
        int height = stack1.getHeight();
        int n = stack1.getSize();
        ImageStack stack2 = img1.createEmptyStack();
        try {
            for (int i=1; i<=n; i++) {
                ImageProcessor ip1 = stack1.getProcessor(i);
                ImageProcessor ip2 = ip1.duplicate(); 
                ip2 = ip2.convertToFloat();
                stack2.addSlice(stack1.getSliceLabel(i), ip2);
            }
        }
        catch(OutOfMemoryError e) {
            stack2.trim();
            stack2 = null;
            return null;
        }
        ImagePlus img2 =  new ImagePlus("Ratio", stack2);
        return img2;
    }


  ImagePlus replicateImage(ImagePlus img1, int n) {
        ImageProcessor ip1 = img1.getProcessor();
        int width = ip1.getWidth();
        int height = ip1.getHeight();
        ImageStack stack2 = img1.createEmptyStack();
        try {
            for (int i=1; i<=n; i++) {
                ImageProcessor ip2 = ip1.duplicate(); 
                ip2 = ip2.convertToFloat();
                stack2.addSlice(null, ip2);
            }
        }
        catch(OutOfMemoryError e) {
            stack2.trim();
            stack2 = null;
            return null;
        }
        ImagePlus img2 =  new ImagePlus("Ratio", stack2);
        return img2;
    }

} 
