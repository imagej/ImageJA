import ij.*;
import ij.process.*;
import ij.plugin.PlugIn;

public class Step_Maker implements PlugIn {
	static final int W=400, H=100, STEPS=10, SW=W/STEPS;

	public void run(String arg) {
		if (IJ.versionLessThan("1.18p"))
			return;
		makeSteps("Byte", 0, 255);
		makeSteps("Short", 0, 65535);
		makeSteps("Float", -1.5, 1.5);
		makeSteps("RGB", 0, 255);
	}
		
	void makeSteps(String title, double min, double max) {
		double inc=(max-min)/(STEPS-1);
		ImageProcessor ip;
		if (title.equals("Byte"))
			ip = new ByteProcessor(W, H);
		else if (title.equals("Short"))
			ip = new ShortProcessor(W, H);
		else if (title.equals("Float"))
			ip = new FloatProcessor(W, H);
		else
			ip = new ColorProcessor(W, H);
		double v=min;
		for (int x=0; x<W; x+=SW, v+=inc) {
			ip.setRoi(x, 0, SW, H);
			ip.setValue(v);
			ip.fill();
		}
		ip.resetMinAndMax();
		title += title.equals("Float")?" ("+min+"-"+max+")":" ("+(int)min+"-"+(int)max+")"; 
		new ImagePlus(title, ip).show();
	}

}

