import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

/** StackAverage
  *
  * Calculates the average values of pixels located at the same
  * position in each slice of the stack and adds a slice showing the average values to the end of the stack.
  *
  * This is an example plugin from the ImageJ plugin writing tutorial.
  * The tutorial can be downloaded at 
  * http://www.fhs-hagenberg.ac.at/staff/burger/ImageJ/tutorial
  */
public class StackAverage_ implements PlugInFilter {

	protected ImagePlus imp;
		
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G+STACK_REQUIRED;
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		int[] sum;
		// takes pixels of one slice
		byte[] pixels;
		int dimension = ip.getWidth()*ip.getHeight();
		sum = new int[dimension];
		// get the pixels of each slice in the stack
		for (int i=1;i<=stack.getSize();i++) {
			pixels = (byte[]) stack.getPixels(i);
			// add the value of each pixel an the corresponding position of the sum array
			for (int j=0;j<dimension;j++) {
				sum[j]+=0xff & pixels[j];
			}
		}
		byte[] average = new byte[dimension];
		// divide each entry by the number of slices
		for (int j=0;j<dimension;j++) {
			average[j] = (byte) ((sum[j]/stack.getSize()) & 0xff);
		}
		// add the resulting image as new slice
		stack.addSlice("Average",average);
		imp.setSlice(stack.getSize());
	}

}

