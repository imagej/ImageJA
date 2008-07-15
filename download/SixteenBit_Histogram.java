import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.measure.*;
import java.awt.*;
import java.awt.image.*;

/** Generates a tabular histogram of a 16-bit image. */
public class SixteenBit_Histogram implements PlugInFilter {
	Calibration cal;
	ImageStatistics stats;

	public int setup(String arg, ImagePlus imp) {
		if (imp!=null) {
			cal = imp.getCalibration();
			stats = imp.getStatistics();
		}
		return DOES_16+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		display16bitHistogram(ip);
	}
    
	void display16bitHistogram(ImageProcessor ip) {
		int[] hist = ip.getHistogram();
		StringBuffer sb = new StringBuffer();
		int min = (int)cal.getRawValue(stats.min);
		int max = (int)cal.getRawValue(stats.max);
		for (int i=min; i<=max; i++)
			sb.append((int)cal.getCValue(i)+"\t"+hist[i]+"\n");
		new ij.text.TextWindow("Histogram", "Value\tCount", new String(sb), 300, 400);
	}

 }
