import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

/**
 *	The user types in starting and ending coodinates, and the plugin
 *	create a straight line selection based on those values. 
 */
public class SpecifyLine_ implements PlugInFilter {
	ImagePlus imp;
	static int x1,y1,x2,y2;

	/**
	 *	Called by ImageJ when the filter is loaded
	 */
	public int setup(String arg, ImagePlus imp) {
		if (IJ.versionLessThan("1.18s"))
			return DONE;
		this.imp = imp;
		if (arg.equals("about"))
			{showAbout(); return DONE;}
		return DOES_ALL+NO_CHANGES;
	}

	/**
	 *	Called by ImageJ to process the image
	 */
	public void run(ImageProcessor ip) {
		if (!getCoordinates(ip))
			return;
		imp.setRoi(new Line(x1, y1, x2, y2,imp));
		IJ.register(SpecifyLine_.class);
	}

	/**
	 *	Creates a dialog box, allowing the user to enter the
	 *	starting and ending coodinates of the line.
	 */
	boolean getCoordinates(ImageProcessor ip) {
		if (x2==0 && y2==0) {
			Rectangle r = ip.getRoi();
			x1 = r.x;
			y1 = r.y;
			x2 = r.x+r.width;
			y2 = r.y+r.height;
		}
		GenericDialog gd = new GenericDialog("Line...");
		gd.addNumericField("X1:", x1, 0);
		gd.addNumericField("Y1:", y1, 0);
		gd.addNumericField("X2:", x2, 0);
		gd.addNumericField("Y2:",y2, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		x1 = (int) gd.getNextNumber();
		y1 = (int) gd.getNextNumber();
		x2 = (int) gd.getNextNumber();
		y2 = (int) gd.getNextNumber();
		return true;
	}

	/**
	 *	Displays a short message describing this plugin
	 */
	void showAbout() {
		IJ.showMessage("About ROISelect_...",
			"This plugin creates a straight line selection from starting\n"+
			"and ending coodinates entered into a dialog box."
		);
	}

}


