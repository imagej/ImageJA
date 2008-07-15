import ij.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;

/**Sets the active image's minimum and maximum displayed values. */

public class Display_Updater implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL-DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		if (IJ.versionLessThan("1.17w")) // uses new GenericDialog constructor
			return;
		int min = (int)ip.getMin();
		int max = (int)ip.getMax();
		GenericDialog gd = new GenericDialog("Update Display");
		gd.addNumericField("Minimum Displayed Value: ", min, 0);
		gd.addNumericField("Maximum Displayed Value: ", max, 0);
		gd.addCheckbox("Reset", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		min = (int)gd.getNextNumber();
		max = (int)gd.getNextNumber();
		boolean reset = gd.getNextBoolean();
		if (reset)
			ip.resetMinAndMax();
		else
			ip.setMinAndMax(min, max);
	}

	
}
