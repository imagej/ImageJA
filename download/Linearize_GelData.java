import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/**
 * - Transformation of Square-Root Encoded Data to Linear Data -
 * Transformation of pixels from the PhosphorImager or FluorImager data files (.gel files)
 * to linear values.
 * 
 * Formula: grayscale_value = Square(stored_value)*scale factor
 * 2 steps for each pixels:
 * 1/ multiply the pixel value by itself to square the data. You obtain a 32bit result.
 * 2/ Multiply the squared pixel value by the Scale factor. You obtain a double value.
 * 
 * Note that the Scale factor is a private tag of the .gel format of files. 
 * 16-bit GEL file is an extension to the standard TIFF using private tags.
 * The MD_ScalePixel tag  code is 332446
 * 
 * Example of data found: 4,75624256837099E-5 
 * it equals to 1/21025
 * We will use this value for Scale Factor default value
 * 
 * @author Remi Cathelin SIGENAE Team - INRA - cathelin@toulouse.inra.fr
 * @version 2006/01/25
 * 
 */
public class Linearize_GelData implements PlugIn, Measurements {
	
	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		if (imp.getBitDepth()!=16) {
			IJ.error("16-bit image required");
			return;
		}
		GenericDialog gd = new GenericDialog(".gel data transformer", IJ.getInstance());
		gd.addNumericField("Scale Factor: 1/",21025,0);//0 = digits
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		double scaleFactor = 1.0D/gd.getNextNumber();
		ImagePlus workImage = linearGel(imp, scaleFactor);	
		workImage.show("scaleFactor= "+scaleFactor);
	}
	
	/** Do transformation of Square-root encoded Data to Linear Data */
	public static ImagePlus linearGel(ImagePlus imp, double scaleFact) {
		ImageProcessor ip2 = imp.getProcessor();
		ip2 = ip2.convertToFloat();
		ip2.sqr();
		ip2.multiply(scaleFact);
		ip2.resetMinAndMax();
		ip2 = ip2.convertToShort(true);
		ImagePlus result = new ImagePlus("linear_"+ imp.getShortTitle(), ip2);
		result.setCalibration(imp.getCalibration());
		return result;
	}
		
}

