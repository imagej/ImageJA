import ij.*;
import ij.process.*;
import ij.measure.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

/* Plugin to convert an 8 bit greyscale, 16 bit unsigned or 32 bit float image 
 * to 16 bit signed
 *
 * Author: Jon Jackson <j.jackson@ucl.ac.uk>
 * Version 1.1
 * 23 / 08 / 2005
*/

public class Convert_To_Signed16 implements PlugInFilter {
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;		
		if (imp!=null && imp.getCalibration().isSigned16Bit()) {
			IJ.error("This is already a signed 16-bit image");
			return DONE;
		}
		return DOES_8G + DOES_16 + DOES_32;
	}

	public void run(ImageProcessor ip) {
		int bitDepth = imp.getBitDepth();
		boolean isRoi = imp.getRoi()!=null;
		imp.killRoi();
		boolean scaling = ImageConverter.getDoScaling();
		ImageConverter.setDoScaling(false);
		double max = ip.getMax();
		double min = ip.getMin();
		int slices = imp.getStackSize();
		if (slices == 1) {	
			if (bitDepth == 8) {
				new ImageConverter(imp).convertToGray16();
			}
			imp.getProcessor().add(32768);
			if (bitDepth == 32) {
				new ImageConverter(imp).convertToGray16();
			}
		} else {
			if (bitDepth == 8) {
				new StackConverter(imp).convertToGray16();
			}
			ImageStack stack = imp.getStack();
			for (int i = 1; i<=slices; i++) {
				ip = stack.getProcessor(i);
				ip.add(32768);
			}
			if (bitDepth == 32) {
				new StackConverter(imp).convertToGray16();
			}
		}
		ip = imp.getProcessor();
		double[] coeff = new double[2];
		coeff[0] = -32768.0;
		coeff[1] = 1.0;
		Calibration cal = imp.getCalibration();
		cal.setFunction(Calibration.STRAIGHT_LINE, coeff, "gray value");
		cal.getCTable(); // bug work around for ImageJ 1.33h or earlier
		ip.setMinAndMax(min + 32768.0, max + 32768.0);
		ImageConverter.setDoScaling(scaling);        
		if (isRoi)
            imp.restoreRoi();
		imp.updateAndDraw();
	}

}
