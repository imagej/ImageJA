import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.process.ShortProcessor;

/**
 *	This plugin simplifies the task of background subtraction and
 *	image normalization given a brightfield image and/or a background image.
 *	This plugin is currently limited to 16-bit images, but could easily be
 *	extended to 8-bit, 32-bit, or even color images. <p>
 *
 *	Possible normalization methods: <p><p>
 *	
 *	If both BRIGHT and DARK images are specified <p>
 *	  IMAGE = scale * (IMAGE - DARK) / (BRIGHT - DARK) <p>
 *  If only BRIGHT image is specified <p>
 *	  IMAGE = scale * IMAGE / BRIGHT <p>
 *	If only DARK image is specified <p>
 *	  IMAGE = IMAGE - DARK <p><p>
 *
 *	@author	Jeffrey Kuhn
 *	@author	The University of Texas at Austin
 *	@author	jkuhn@ccwf.cc.utexas.edu
 *
 *	Modified to process stacks by Ben Dubin-Thaler
 *	Columbia University
 *	bjd14@columbia.edu
*/
public class Normalize_ implements PlugInFilter {

	/** If "none" is selected, then that image is not used in the calculation */
	private static final String strNONE = "-- none --";

	/** Name of bright image */
	private static String strBrightName = null;		

	/** Name of dark image */
	private static String strDarkName = null;

	/** Number to scale resulting image by after division by BRIGHT */
	private static double dScale = 4095.0;			

	/** ImageJ ID of bright image */
	private int iBrightID;							

	/** ImageJ ID of dark image */
	private int iDarkID;		
	
	/** The image window to work on */					
	private ImagePlus impImage;

	/** whether to process the entire stack */					
	static boolean processStack = false;

	// setup is a method within PlugInFilter that is run once at the start of plugin exceution
	public int setup(String arg, ImagePlus imp) {
		this.impImage = imp;

		IJ.register(Normalize_.class);
 		
		int[] aiWinList = WindowManager.getIDList();
		if (aiWinList==null) {
			IJ.error("No windows are open.");
			return DONE;
		}
		String[] astrWinTitles = new String[aiWinList.length + 1];
		astrWinTitles[0] = strNONE;
		for (int i=0; i<aiWinList.length; i++) {
			ImagePlus imp2 = WindowManager.getImage(aiWinList[i]);
			if (imp2 != null) {
				astrWinTitles[i+1] = imp2.getTitle();
			} else {
				astrWinTitles[i+1] = "";
			}
		}
		if (strBrightName == null)
			strBrightName = strNONE;
		if (strDarkName == null)
			strDarkName = strNONE;
		
 		int stackSize = imp.getStackSize();
 		
		/* 	dialog for input values		
			structure sizes are to be given as percentages of 
			the length of the longer edge of the ROI */
		GenericDialog gd = new GenericDialog("Normalize Image", IJ.getInstance());
		gd.addMessage("IMAGE to normalize: " + imp.getTitle());
		gd.addChoice("BRIGHT Image", astrWinTitles, strBrightName);
		gd.addChoice("DARK Image", astrWinTitles, strDarkName);
		gd.addNumericField("scale", dScale, 2);
		if (stackSize > 1) {
			gd.addCheckbox("Process entire Stack", processStack);	
		}	
		
		gd.addMessage("Possible normalization methods");
		gd.addMessage("= scale*(IMAGE-DARK)/(BRIGHT-DARK)");
		gd.addMessage("= scale*IMAGE/BRIGHT");
		gd.addMessage("= IMAGE-DARK");
		gd.showDialog();
		
		if(gd.wasCanceled())
			return DONE;
		
		int iIndex;
		dScale = gd.getNextNumber();
		
		iIndex = gd.getNextChoiceIndex();
		if (iIndex == 0) {
			iBrightID = 0;
			strBrightName = null;
		} else {
			iBrightID = aiWinList[iIndex-1];
			strBrightName = astrWinTitles[iIndex];
		}
		
		iIndex = gd.getNextChoiceIndex();
		if (iIndex == 0) {
			iDarkID = 0;
			strDarkName = null;
		} else {
			iDarkID = aiWinList[iIndex-1];
			strDarkName = astrWinTitles[iIndex];
		}
		
		if (stackSize > 1) {
			processStack = gd.getNextBoolean();
		}
 		
 		int returnValue = DOES_16;
 		
 		if (stackSize > 1 && processStack) 
 			returnValue = returnValue | DOES_STACKS;
		
		return returnValue;
				
	}


	/**
	 * Normalize the current image.
	 */
	public void run(ImageProcessor ip) {

		int iW = ip.getWidth();
		int iH = ip.getHeight();
		int iLen = iW * iH;

		
		ImagePlus impBright = null;
		ImagePlus impDark = null;

		if (strBrightName != null) {
			impBright = WindowManager.getImage(iBrightID);
			if (impBright.getType() != impImage.getType()) {
				IJ.error("The BRIGHT image type does not match this image type.");
				return;
			}
			if ((impBright.getProcessor().getWidth() != iW) || (impBright.getProcessor().getHeight() != iH)) {
				IJ.error("The BRIGHT image size does not match this image size.");
				return;
			}
		}
		if (strDarkName != null) {
			impDark = WindowManager.getImage(iDarkID);
			if (impDark.getType() != impImage.getType()) {
				IJ.error("The DARK image type does not match this image type.");
				return;
			}
			if ((impDark.getProcessor().getWidth() != iW) || (impDark.getProcessor().getHeight() != iH)) {
				IJ.error("The DARK image size does not match this image size.");
				return;
			}
		}

		// At least one of bright or dark must have been specified
		if ((impBright == null) && (impDark == null)) {
			IJ.error("Neither a BRIGHT image nor a DARK image were specified. Normalization cannot proceed.");
			return;
		}

		switch (impImage.getType()) {
		case ImagePlus.GRAY16:
			processGray16Unsigned(dScale, ip, impBright, impDark, iLen);
			break;

		/* TODO: Support other image types */

		default:
			IJ.error("Normalize does not support this image type.");
			return;
		}

		// Reset the min and max displayed values
		double min = ip.getMin();
		double max = ip.getMax();
		ip.setMinAndMax(min, max);
	}

	/**
	 * Normalize a 16-bit unsigned image
	 */
	void processGray16Unsigned(double dScale, ImageProcessor ip, ImagePlus impBright, 
							 ImagePlus impDark, int iLen) {
		if ((impBright != null) && (impDark != null)) {
			short[] asImage = (short[])ip.getPixels();
			short[] asBright = (short[])impBright.getProcessor().getPixels();
			short[] asDark = (short[])impDark.getProcessor().getPixels();
			for (int i=0; i<iLen; i++) {
				asImage[i] = (short)(dScale * ((asImage[i]&0xffff) - (asDark[i]&0xffff)) / ((asBright[i]&0xffff) - (asDark[i]&0xffff)));
			}
		} else if (impBright != null) {
			short[] asImage = (short[])ip.getPixels();
			short[] asBright = (short[])impBright.getProcessor().getPixels();
			for (int i=0; i<iLen; i++) {
				asImage[i] = (short)(dScale * (asImage[i]&0xffff) / (asBright[i]&0xffff));
			}
		} else if (impDark != null) {
			short[] asImage = (short[])ip.getPixels();
			short[] asDark = (short[])impDark.getProcessor().getPixels();
			for (int i=0; i<iLen; i++) {
				asImage[i] = (short)((asImage[i]&0xffff) - (asDark[i]&0xffff));
			}
		}
	}

	/* TODO: Support other image types */
}
