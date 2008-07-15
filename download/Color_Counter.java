import ij.*;
import ij.process.*;
import ij.plugin.filter.*;

/** This plugin counts the number of unique colors in an RGB image. */
public class Color_Counter implements PlugInFilter {
	ImagePlus imp;
		static final int MAX_COLORS = 16777216;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_RGB+NO_UNDO+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		boolean[] counter = new boolean[MAX_COLORS];
		int[] pixels = (int[])ip.getPixels();
		for (int i=0; i<pixels.length; i++)
			counter[pixels[i]&0xffffff] = true;
		int count = 0;
		for (int i=0; i<MAX_COLORS; i++) {
			if (counter[i])
				count++;
		}
		IJ.write("Color count: "+count);	
	}

}
