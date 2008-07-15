import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

/**This filter plugin runs for ~10 seconds or until the user 
    presses the Esc key.  It requires ImageJ 1.33m or later. 
    Note: you may need to activate an image window or the
    ImageJ window to get Esc presses to be recognized. */
public class Escape_Filter implements PlugInFilter {
	ImagePlus imp;
	boolean done;
	long timePerSlice;

	public int setup(String arg, ImagePlus imp) {
		if (IJ.versionLessThan("1.33m")) return DONE;
		this.imp = imp;
		if (imp!=null) timePerSlice = 10000L/imp.getStackSize();
		return DOES_ALL+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		IJ.showStatus("Press 'Esc' to abort");
		if (done) return;
		doSimulatedProcessing(imp);
	}

	public void doSimulatedProcessing(ImagePlus imp) {
		long start = System.currentTimeMillis();
		while ((System.currentTimeMillis()-start)<timePerSlice) {
			if (IJ.escapePressed()) 
				{IJ.beep(); done=true; return;}
			IJ.wait(10);
		}
	}

}
