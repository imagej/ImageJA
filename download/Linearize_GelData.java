import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
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
public class Linearize_GelData implements PlugInFilter, Measurements {
	private ImagePlus imp;
	private GenericDialog gd;
	private double scaleFactor = 1.0D/21025.0D;//default value 
	
	public int setup(String arg, ImagePlus imp) {
		if (IJ.versionLessThan("1.18o"))
			return DONE;
		this.imp = imp;
		return DOES_ALL;
	}
	
	public void run(ImageProcessor ip) {
		
		switch (imp.getBitDepth()) {
		
		case 16:
			//interface
			gd = new GenericDialog(".gel data transformer", IJ.getInstance());
			gd.addNumericField("Scale Factor: 1/",21025,0);//0 = digits
			gd.showDialog();
			
			if (gd.wasCanceled())
				return;
			
			//scale factor
			scaleFactor = 1.0D/gd.getNextNumber();		
			//end interface
			
			ImagePlus workImage = linearGel(imp,scaleFactor);		
			workImage.show();
			
			displayValues();
			break;
			
		default:
			IJ.error("16 bits grayscale image required!");
		
		}
		
		
	}
	
	/**
	 * 
	 */
	private void displayValues() {
		System.out.println("****VALUES****");
		System.out.println("scaleFactor= "+scaleFactor);
		System.out.println("*************");
	}
	
	
	/**
	 * Do transformation of Square-root encoded Data to Linear Data
	 * 
	 */
	public static ImagePlus linearGel(ImagePlus imagePlus,double scaleFact) {
		ImageProcessor ip = imagePlus.getProcessor();
		ImageProcessor ip2 = imagePlus.getProcessor();
		Calibration cal = imagePlus.getCalibration();
		Calibration cal2 = imagePlus.getCalibration();
		
		// 32bits conversion
		ip.setCalibrationTable(cal.getCTable());
		ip = ip.convertToFloat();
		ip2.setCalibrationTable(cal2.getCTable());
		ip2 = ip2.convertToFloat();
		
		try {
			int mode = Blitter.MULTIPLY;
			ip.copyBits(ip2, 0, 0, mode);
		}
		catch (IllegalArgumentException e) {
			IJ.error("\""+imagePlus.getTitle()+"\": "+e.getMessage());
			return null;
		}
		if (!(ip instanceof ByteProcessor)) 
			ip.resetMinAndMax();
		
		ip.multiply(scaleFact);
		
		ImagePlus result = new ImagePlus("linear_"+ imagePlus.getShortTitle(),ip);
		result.setCalibration(cal);
		
		
		// 16 bits conversion
		ImageConverter ic = new ImageConverter(result);
		ic.convertToGray16();
		
		//result.show();
		return result;
	}
	
	
	
}

