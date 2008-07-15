import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Slice_Labeler implements PlugInFilter {
	ImagePlus imp;
	
	static int x = 2;
	static int size = 12;
	static int y = size+3;
	Font font;
	static String name = "Text";
	static int start = 1;
	static int step = 1;
	static int interval = 1;
	static int digits = 4;
	boolean firstSlice = true;
	boolean canceled;
	boolean useRoiLoc;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		setLocation(imp);
		return DOES_ALL+DOES_STACKS+STACK_REQUIRED;
	}

	public void run(ImageProcessor ip) {
		if (firstSlice)
			showDialog(ip);
		if (canceled)
			return;
		ip.setFont(font);
		ip.setColor(Toolbar.getForegroundColor());
		String s = getString(step);
		ip.moveTo(x, y);
		ip.drawString(s);
		step += interval;
	}
	
	void setLocation(ImagePlus imp) {
		if (imp==null) return;
		Roi roi = imp.getRoi();
		if (roi!=null && roi.getType()==Roi.RECTANGLE) {
			Rectangle r = roi.getBoundingRect();
			x = r.x;
			y = r.y+r.height;
			useRoiLoc = true;
		}
	}

	String getString(int step) {
		if  (step==0 || digits==0)
			return name;
		else {
			String n = "000000000000" + (int)step;
			n = n.substring(n.length()-digits);
			return (name + n);
		}
	}

	void showDialog(ImageProcessor ip) {
		firstSlice = false;
		Rectangle roi = ip.getRoi();
		
		GenericDialog gd = new GenericDialog("Slice Labeler");
		gd.addStringField("Label text:", name);
		gd.addNumericField("Starting Number:",start, 0);
		gd.addNumericField("Increment:", interval, 0);
		gd.addNumericField("Digits:", digits, 0);
		gd.addNumericField("Font Size:", size, 0);
				
		gd.showDialog();
		if (gd.wasCanceled())
			{canceled = true; return;}
		start = (int)gd.getNextNumber();
 		interval = (int)gd.getNextNumber();
		digits = (int)gd.getNextNumber();
		if (digits<0) digits = 0;
		if (digits>12) digits = 12;
		size = (int)gd.getNextNumber();
		if (!useRoiLoc)
			y = size+3;
		name = gd.getNextString();
		font = new Font("SansSerif", Font.PLAIN, size);
		ip.setFont(font);
		step = start;
		
		imp.startTiming();
	}

}
