import ij.*;
import ij.measure.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;

/*------------------------------------------------------------------
Sebastian Rhode
Version 1.2	2008-08-28

This plugin calculates the CCD readout noise on the basis of
two dark images with zero exposure. Both images have to be
recorded at different time points with identical CCD settings.
It can be used to check the noise value given by the manufacturer. 

Here you can find detailed information:

http://spiff.rit.edu/classes/phys559/lectures/readout/readout.html

Thanks to Michael Richmond for this little tutorial


I do not guarantee for correct results! Sebastian Rhode - 2008-08-28
--------------------------------------------------------------------*/

public class CCD_noise implements PlugIn {
	String title;
	public void run(String arg) {
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("No images are open.");
			return ;
		}

		String[] titles = new String[wList.length+1];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		String none = "*None*";
		titles[wList.length] = none;
                
                double adconv   =  1; // default A/D conversion factor cts --> electrons
                double emgain   =  1; // default EM-Gain at minimum Em-Gain selected
                                      // if CCD has no EM-Gain use 1 !!!
                
		GenericDialog gd = new GenericDialog("Calc CCD Noise");

		gd.addChoice("Dark Image I  :", titles, titles[0]);
		String title2 = titles.length>2?titles[1]:none;
		gd.addChoice("Dark Image II :", titles, title2);
		gd.addMessage("---------------------------------------------------------------");
        gd.addNumericField("EM Gain]", emgain, 0);
        gd.addMessage("If no EM CCD is used --> EM Gain = 1");
        gd.addMessage("---------------------------------------------------------------");
        gd.addNumericField("A/D Conversion Factor [e-/cts]", adconv, 2);
        gd.addMessage("If no idea ... --> ~0.85 * full well capacity / BitDepth of CCD");
		gd.addMessage("---------------------------------------------------------------");
        gd.addMessage("Both dark frames with zero exposure (' zero frames ')");
		gd.addMessage("have to be recored at different time points!");
		gd.showDialog();

		if (gd.wasCanceled()) 
			return ;

		int file0   =   gd.getNextChoiceIndex();
		int file1   =   gd.getNextChoiceIndex();
        emgain      =   gd.getNextNumber();
        adconv      =   gd.getNextNumber();
                
		// create ImagePlus and ImageProcessor classes
		ImagePlus imp_dp0 =WindowManager.getImage(wList[file0]); // read dark picture 1
		ImageProcessor ip_dp0 = imp_dp0.getProcessor();
		ImagePlus imp_dp1 =WindowManager.getImage(wList[file1]); // read dark picture 2
		ImageProcessor ip_dp1 = imp_dp1.getProcessor();
		
		// get width and height of picture
		int width	= ip_dp0.getWidth();
		int height	= ip_dp0.getHeight();
		
		// create a new image with the same size
                // use float to allow for negative pixel values
		FloatProcessor ip_diff = new FloatProcessor(width,height);
		ImagePlus imp_diff = new ImagePlus("Difference Image", ip_diff);

		// calculate difference Image
		for (int x=0; x<width; x++) {  
			for (int y=0; y<height; y++) {
				// subtraction darkpic1- darkpic2
				// add 100 to prevent any underflow --> negative pixel in diff image
                                float v = ip_dp0.getPixel(x, y) - ip_dp1.getPixel(x, y);
				ip_diff.putPixelValue(x, y, v);
			}
		}
                ImageCanvas imc_diff = new ImageCanvas(imp_diff);
                ImageWindow imwin_diff = new ImageWindow(imp_diff, imc_diff);
                imwin_diff.getImagePlus().getProcessor().resetMinAndMax();
		imp_diff.show();     
		
		// do the statistics
		ImageStatistics st0	= imp_dp0.getStatistics();
		ImageStatistics st1	= imp_dp1.getStatistics();
		ImageStatistics st_diff = imp_diff.getStatistics();
		double[]min = new double[3];
		double[]max = new double[3];
		double[]mean= new double[3];
		double[]std = new double[3];

		min[0] = st0.min; min[1] = st1.min; min[2] = st_diff.min;
		max[0] = st0.max; max[1] = st1.max; max[2] = st_diff.max;
		mean[0] = st0.mean; mean[1] = st1.mean;	mean[2] = st_diff.mean;
		std[0] = st0.stdDev; std[1] = st1.stdDev; std[2] = st_diff.stdDev;

                HistogramWindow hist_diff = new HistogramWindow("Histogram - Difference Image", imp_diff, st_diff);
                
                //-------- Results for StdDev for one image --> Readout Noise of CCD in counts
		double readout_noise = std[2]/Math.sqrt(2)/emgain;
                
		// and create the results tables
		String[] label = {"Dark1","Dark2","Diff"};
		ResultsTable rt1 = new ResultsTable();
		rt1.reset();
		for (int n=0; n <  3; n++) {
			rt1.incrementCounter();
			rt1.setLabel(label[n],n);
			rt1.addValue("min", min[n]);
			rt1.addValue("max", max[n]);
			rt1.addValue("mean", mean[n]);
			rt1.addValue("stdDev", std[n]);
		}
		rt1.show("Image Statistics");
		
		ResultsTable rt2 = new ResultsTable();
		rt2.reset();
		rt2.incrementCounter();
		rt2.addValue("Noise [cts]",readout_noise);
                rt2.addValue("Noise [e- ]",readout_noise*adconv);
		rt2.show("CCD Noise Results");
	}
}
