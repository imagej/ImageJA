import java.lang.String;
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;


/**
 *	Combination of Java and Native Windows C code to capture an
 *	image with a Hamamatsu Orca 12-bit camera and a National Instruments Image
 *	Acquisition board. This plugin could serve as an example of how ImageJ can
 *	communicate with C code through JNI, allowing ImageJ to capture images when
 *	there is no Java programming library or Twain driver available for a frame
 *	grabber. <p>
 *
 *	@author	Jeffrey Kuhn
 *	@author	The University of Texas at Austin
 *	@author	jkuhn@ccwf.cc.utexas.edu
 */


public class AcquireIMAQ_ implements PlugIn {
	/** Width of image to acquire. The maximum size for this camera is 1024.
	 *	Note that "static" is used to insure that the last number is remembered
	 *	every time the plugin is called. */
	static int		iWidth = 1024;

	/** Height of image to acquire. The maximum size for this camera is 1024.
	 *	Note that "static" is used to insure that the last number is remembered
	 *	every time the plugin is called. */
	static int		iHeight = 1024;

	/** Name to give the newly acquired image */
	static String	strName = "Acquired";

	/** A unique number is tagged onto the end of the name of each newly acquired image. */
	static int		iImageNumber = 0;

	/** Length of camera exposure to use. (see @see Acquire for an explanation). */
	static double	dExposure = 1.0;

	/** 
	 *	Called when the plugin is loaded to load the C++ library
	 */
    static {
        System.loadLibrary("AcquireIMAQ_Native");
    }
    
	/**
	 * Java interface to C++ function which grabs images.
	 *
	 * @param iWidth	Width of the image to capture
	 * @param iHeight	Height of the image to capture
	 * @param aPixels	Array of short value in which to place the pixel values
	 * @param dParam	Array of image capture parameters passed to and from
	 *					the C++ Acquire function. The following is a list
	 *					of parameters:<p>
	 *					dParam[0] - Default camera exposure. The value of
	 *						exposure determines how bright the image is:<p>
	 *						for Partial frame exposure, 0 < exposure < 1.0 <p>
	 *						for Full frame exposure, exposure = 1.0 <p>
	 *						for Multiple frame exposre, exposure = an integer > 1 <p>
	 *
	 * @return			true if frame capture was successfull, false if a probem
	 *					occurred or the user canceled the operation.
	 */
	native boolean Acquire(int iWidth, int iHeight, short[] aPixels, double[] dParams);

	/**
	 *	Begin acquiring an image.
	 */
	public void run(String arg) {

		if (IJ.versionLessThan("1.18p"))
			return;
		GenericDialog gd = new GenericDialog("Acquisition");
		gd.addNumericField("Width  (1024 max):", iWidth, 0);
		gd.addNumericField("Height (1024 max):", iHeight, 0);
		gd.addNumericField("Exposure:", dExposure, 2);
		gd.addStringField("Name:", strName);
		gd.showDialog();		
		if (gd.wasCanceled()) 
			return;
		iWidth = (int) gd.getNextNumber();
		iHeight = (int) gd.getNextNumber();
		dExposure = gd.getNextNumber();
		strName = gd.getNextString();

		// make a new unsigned 16 bit image
		ImageProcessor ip = new ShortProcessor(iWidth, iHeight);
		short[] asPixels = (short[]) ip.getPixels();
		double[] adParams = new double[1];
		adParams[0] = dExposure;
		if (Acquire(iWidth, iHeight, asPixels, adParams)) {
			// retrieve the exposure if changed.
			dExposure = adParams[0];
			
			ip.resetMinAndMax();

			// Increment the image number and append it to the image name
			iImageNumber++;
			new ImagePlus(strName + " " + iImageNumber, ip).show();
		}
	 }
}

