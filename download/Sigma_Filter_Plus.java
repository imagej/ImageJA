import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.*;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.process.*;
import ij.plugin.ContrastEnhancer;
import java.awt.*;
import java.awt.event.*;

/** This plugin-Filter provides a selective mean (averaging) filter.
* In contrast to the standard mean filter, it preserves edges better
* and is less sensitive to outliers.
* Based on Lee's sigma filter algorithm and a plugin by Tony Collins.
*   J.S. Lee, Digital image noise smoothing and the sigma filter, in:
*   Computer Vision, Graphics and Image Processing, vol. 24, 255-269 (1983).
* The "Outlier Aware" option is a modification of Lee's algorithm introduced
* by Tony Collins.
*
* The filter smoothens an image by taking an average over the
* neighboring pixels, but only includes those pixels that have a
* value not deviating from the current pixel by more than a given
* range. The range is defined by the standard deviation of the pixel
* values within the neighborhood ("Use pixels within ... sigmas").
* If the number of pixels in this range is too low (less than "Minimum
* pixel fraction"), averaging over all neighboring pixels is performed.
* With the "Outlier Aware" option, averaging over all neighboring
* pixels excludes the center pixel. Thus, outliers having a value
* very different from the surrounding are not included in the average,
* i.e., completely eliminated.
*
* For preserving the edges, values of "Use pixels within" between
* 1 and 2 sigmas are recommended. With high values, the filter will behave
* more like a traditional averaging filter, i.e. smoothen the edges.
* Typical values of the minimum pixel fraction are around 0.2, with higher
* values resulting in more noise supression, but smoother edges.
*
* If preserving the edges is not desired, "Use pixels within" 2-3 sigmas
* and a minimum pixel fraction around 0.8-0.9, together with the "Outlier
* Aware" option will smoothen the image, similar to a traditional filter,
* but without being influenced by outliers strongly deviating from the
* surrounding pixels (hot pixels, dead pixels etc.).
*
*
* Code by Michael Schmid, 2007-10-25
*/
public class Sigma_Filter_Plus implements ExtendedPlugInFilter, DialogListener {
    // Filter parameters
    private static double radius = 2.;          // The kernel radius, see Process>Filters>Show Circular Masks
    private static double sigmaWidth = 2.;      // Pixel value range in sigmas.
    private static double minPixFraction = 0.2; // The fraction of pixels that need to be inside the range for selective smoothing
    private static boolean outlierAware = true; // Whether outliers will be excluded from averaging
    // F u r t h e r   c l a s s   v a r i a b l e s
    int flags = DOES_ALL|SUPPORTS_MASKING|CONVERT_TO_FLOAT|SNAPSHOT|KEEP_PREVIEW|
            PARALLELIZE_STACKS;
    private int nPasses = 1;                    // The number of passes (color channels * stack slices)
    private int pass;                           // Current pass
    protected int kRadius;                      // kernel radius. Size is (2*kRadius+1)^2
    protected int kNPoints;                     // number of points in the kernel
    protected int[] lineRadius;                 // the length of each kernel line is 2*lineRadius+1

    /** Setup of the PlugInFilter. Returns the flags specifying the capabilities and needs
     * of the filter.
     *
     * @param arg   Defines type of filter operation
     * @param imp   The ImagePlus to be processed
     * @return      Flags specifying further action of the PlugInFilterRunner
     */    
    public int setup(String arg, ImagePlus imp) {
        return flags;
    }

    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        GenericDialog gd = new GenericDialog(command+"...");
        gd.addNumericField("Radius", radius, 1, 6, "Pixels");
        gd.addNumericField("Use Pixels Within", sigmaWidth, 1, 6, "Sigmas");
        gd.addNumericField("Minimum Pixel Fraction", minPixFraction, 1);
        gd.addCheckbox("Outlier Aware", outlierAware);
        gd.addPreviewCheckbox(pfr);     //passing pfr makes the filter ready for preview
        gd.addDialogListener(this);     //the DialogItemChanged method will be called on user input
        gd.showDialog();                //display the dialog; preview runs in the  now
        if (gd.wasCanceled()) return DONE;
        IJ.register(this.getClass());   //protect static class variables (filter parameters) from garbage collection
        return IJ.setupDialog(imp, flags);  //ask whether to process all slices of stack (if a stack)
    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        radius = gd.getNextNumber();
        sigmaWidth = gd.getNextNumber();
        minPixFraction = gd.getNextNumber();
        outlierAware = gd.getNextBoolean();
        if (gd.invalidNumber() || radius<0.5 || radius>10 || minPixFraction<0. || minPixFraction>1.)
            return false;
        makeKernel(radius);             //determine the kernel size once for all channels&slices
        return true;
    }

    public void run(ImageProcessor ip) {
        //copy class variables to local ones - this is necessary for preview
        int[] lineRadius;
        int kRadius, kNPoints, minPixNumber;
        synchronized(this) {                            //the two following items must be consistent
            lineRadius = (int[])(this.lineRadius.clone()); //cloning also required by doFiltering method
            kRadius = this.kRadius;                     //kernel radius
            kNPoints = this.kNPoints;                   //number of pixels in the kernel
            minPixNumber = (int)(kNPoints * minPixFraction + 0.999999); //min pixels in sigma range
        }
        if (Thread.currentThread().isInterrupted()) return;
        pass++;
        doFiltering((FloatProcessor)ip, kRadius, lineRadius, sigmaWidth, minPixNumber, outlierAware);
    }

    /** Filter a FloatProcessor according to filterType
     * @param ip The image subject to filtering
     * @param kRadius The kernel radius. The kernel has a side length of 2*kRadius+1
     * @param lineRadius The radius of the lines in the kernel. Line length of line i is 2*lineRadius[i]+1.
     * Note that the array <code>lineRadius</code> will be modified, thus call this method
     * with a clone of the original lineRadius array if the array should be used again.*/
    //
    // Data handling: The area needed for processing a line, i.e. a stripe of width (2*kRadius+1)
    // is written into the array 'cache'. This array is padded at the edges of the image so that
    // a surrounding with radius kRadius for each pixel processed is within 'cache'. Out-of-image
    // pixels are set to the value of the neares edge pixel. When adding a new line, the lines in
    // 'cache' are not shifted but rather the smaller array with the line lengths of the kernel is
    // shifted.
    //
    public void doFiltering(FloatProcessor ip, int kRadius, int[] lineRadius, double sigmaWidth, int minPixNumber, boolean outlierAware) {
        float[] pixels = (float[])ip.getPixels();   // array of the pixel values of the input image
        int width = ip.getWidth();
        int height = ip.getHeight();
        Rectangle roi = ip.getRoi();
        int xmin = roi.x - kRadius;
        int xEnd = roi.x + roi.width;
        int xmax = xEnd + kRadius;
        int kSize = 2*kRadius + 1;
        int cacheWidth = xmax - xmin;
        int xminInside = xmin>0 ? xmin : 0;
        int xmaxInside = xmax<width ? xmax : width;
        int widthInside = xmaxInside - xminInside;
        boolean smallKernel = kRadius < 2;
        float[] cache = new float[cacheWidth*kSize]; //a stripe of the image with height=2*kRadius+1
        for (int y=roi.y-kRadius, iCache=0; y<roi.y+kRadius; y++)
            for (int x=xmin; x<xmax; x++, iCache++)  // fill the cache for filtering the first line
                cache[iCache] = pixels[(x<0 ? 0 : x>=width ? width-1 : x) + width*(y<0 ? 0 : y>=height ? height-1 : y)];
        int nextLineInCache = 2*kRadius;            // where the next line should be written to
		double[] sums = new double[2];
        Thread thread = Thread.currentThread();     // needed to check for interrupted state
        long lastTime = System.currentTimeMillis();
        for (int y=roi.y; y<roi.y+roi.height; y++) {
            long time = System.currentTimeMillis();
            if (time-lastTime > 100) {
                lastTime = time;
                if (thread.isInterrupted()) return;
                showProgress(y/(double)(roi.height));
            }
            int ynext = y+kRadius;                  // C O P Y   N E W   L I N E  into cache
            if (ynext >= height) ynext = height-1;
            float leftpxl = pixels[width*ynext];    //edge pixels of the line replace out-of-image pixels
            float rightpxl = pixels[width-1+width*ynext];
            int iCache = cacheWidth*nextLineInCache;//where in the cache we have to copy to
            for (int x=xmin; x<0; x++, iCache++)
                cache[iCache] = leftpxl;
            System.arraycopy(pixels, xminInside+width*ynext, cache, iCache, widthInside);
            iCache += widthInside;
            for (int x=width; x<xmax; x++, iCache++)
                cache[iCache] = rightpxl;
            nextLineInCache = (nextLineInCache + 1) % kSize;
            boolean fullCalculation = true;         // F I L T E R   the line
            for (int x=roi.x, p=x+y*width, xCache0=kRadius;  x<xEnd; x++, p++, xCache0++) {
                double value = pixels[p];           //the current pixel
                if (fullCalculation) {
                    fullCalculation = smallKernel;  //for small kernel, always use the full area, not incremental algorithm
                    getAreaSums(cache, cacheWidth, xCache0, lineRadius, kSize, sums);
                } else
                    addSideSums(cache, cacheWidth, xCache0, lineRadius, kSize, sums);
                double mean = sums[0]/kNPoints;     //sum[0] is the sum over the pixels, sum[1] the sum over the squares
                double variance = sums[1]/kNPoints - mean*mean;

                double sigmaRange  = sigmaWidth*Math.sqrt(variance);
                double sigmaBottom = value - sigmaRange;
                double sigmaTop = value + sigmaRange;
                double sum = 0;
                int count = 0;
                for (int y1=0; y1<kSize; y1++) {                // for y1 within the cache stripe
                    for (int x1=xCache0-lineRadius[y1], iCache1=y1*cacheWidth+x1; x1<=xCache0+lineRadius[y1]; x1++, iCache1++) {
                        float v = cache[iCache1];                // a point within the kernel
                        if ((v>=sigmaBottom)&&(v<=sigmaTop)) {
                            sum += v;
                            count++;
                        }
                    }
                }
                //if there are too few pixels in the kernel that are within sigma range, the 
                //mean of the entire kernel is taken.
                if (count>=minPixNumber)
                    pixels[p] = (float)(sum/count);
                else {
                    if (outlierAware)
                        pixels[p] = (float)((sums[0]-value)/(kNPoints-1)); //assumes that the current pixel is an outlier
                    else
                        pixels[p] = (float)mean;
                }
            } // for x
            int newLineRadius0 = lineRadius[kSize-1];   //shift kernel lineRadii one line
            System.arraycopy(lineRadius, 0, lineRadius, 1, kSize-1);
            lineRadius[0] = newLineRadius0;
        } // for y
    }

    /** Get sum of values and values squared within the kernel area.
     *  xCache0 points to cache element equivalent to current x coordinate.
     *  Output is written to array sums[0] = sum; sums[1] = sum of squares */
    private void getAreaSums(float[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize, double[] sums) {
        double sum=0, sum2=0;
        for (int y=0; y<kSize; y++) {   // y within the cache stripe
            for (int x=xCache0-lineRadius[y], iCache=y*cacheWidth+x; x<=xCache0+lineRadius[y]; x++, iCache++) {
                float v = cache[iCache];
                sum += v;
                sum2 += v*v;
            }
        }
        sums[0] = sum;
        sums[1] = sum2;
        return;
    }

    /** Add all values and values squared at the right border inside minus at the left border outside the kernal area.
     *  Output is added or subtracted to/from array sums[0] += sum; sums[1] += sum of squares  when at 
     *  the right border, minus when at the left border */
    private void addSideSums(float[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize, double[] sums) {
        double sum=0, sum2=0;
        for (int y=0; y<kSize; y++) {   // y within the cache stripe
            int iCache0 = y*cacheWidth + xCache0;
            float v = cache[iCache0 + lineRadius[y]];
            sum += v;
            sum2 += v*v;
            v = cache[iCache0 - lineRadius[y] - 1];
            sum -= v;
            sum2 -= v*v;
        }
        sums[0] += sum;
        sums[1] += sum2;
        return;
    }

    /** Create a circular kernel of a given radius. Radius = 0.5 includes the 4 neighbors of the
     *  pixel in the center, radius = 1 corresponds to a 3x3 kernel size.
     *  The output is written to class variables kNPoints (number of points inside the kernel) and
     *  lineRadius, which is an array giving the radius of each line. Line length is 2*lineRadius+1.
     */
    public synchronized void makeKernel(double radius) {
        if (radius>=1.5 && radius<1.75) //this code creates the same sizes as the previous RankFilters
            radius = 1.75;
        else if (radius>=2.5 && radius<2.85)
            radius = 2.85;
        int r2 = (int) (radius*radius) + 1;
        kRadius = (int)(Math.sqrt(r2+1e-10));
        lineRadius = new int[2*kRadius+1];
        lineRadius[kRadius] = kRadius;
        kNPoints = 2*kRadius + 1;
        for (int y=1; y<=kRadius; y++) {
            int dx = (int)(Math.sqrt(r2-y*y+1e-10));
            lineRadius[kRadius+y] = dx;
            lineRadius[kRadius-y] = dx;
            kNPoints += 4*dx + 2;
        }
    }


    /** This method is called by ImageJ to set the number of calls to run(ip)
     *  corresponding to 100% of the progress bar */
    public void setNPasses (int nPasses) {
        this.nPasses = nPasses;
        pass = 0;
    }

    private void showProgress(double percent) {
        percent = (double)(pass-1)/nPasses + percent/nPasses;
        IJ.showProgress(percent);
    }

}
