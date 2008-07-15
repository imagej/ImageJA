import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.Calibration;

/**
This plugin demonstrates how to create a signed 16-bit image from an array.
The signed image is created by adding 32768 and creating a calibration 
function that subtracts 32768.
*/
public class Signed_16bit_Image implements PlugIn {

	public void run(String arg) {
		int width = 256;
		int height = 256;
		int size = width*height;
		short[] data = new short[size];
 		short value = -32767;
		for (int i=0; i<size; i++)
			data[i] = value++;
		short[] pixels = new short[size];
		for (int i=0; i<size; i++)
			pixels[i] = (short)(data[i]+32768);
		ImageProcessor ip = new ShortProcessor(width, height, pixels, null);
		ImagePlus imp = new ImagePlus("Signed 16 bit", ip);
		double[] coeff = new double[2];
		coeff[0] = -32768.0;
		coeff[1] = 1.0;
 		imp.getCalibration().setFunction(Calibration.STRAIGHT_LINE, coeff, "gray value");
		imp.show();
	}

}
