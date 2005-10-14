import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

/*
	Performs the Kuwahara Filter, a noise-reduction filter that preserves edges.

	a  a  ab   b  b
	a  a  ab   b  b
	ac ac abcd bd bd
	c  c  cd   d  d
	c  c  cd   d  d
    
	In the case of a 5x5 sampling window, the mean brightness and the  
	variance of each of the four 3x3 regions (a, b, c, d), are calculated
	and the value of the center pixel (abcd) is set to the mean value 
	of the region that with the smallest variance.
 
	Description based on the one at:
	http://www.incx.nec.co.jp/imap-vision/library/wouter/kuwahara.html
*/
public class Kuwahara_Filter  implements PlugInFilter {
	static int size = 5;

	public int setup(String arg, ImagePlus imp) {
		if (imp==null || !showDialog())
			return DONE;
		return IJ.setupDialog(imp, DOES_ALL-DOES_32+SUPPORTS_MASKING);
	}

	public void run(ImageProcessor ip) {
		if (ip instanceof ColorProcessor)
			filterRGB(ip);
		else
			filter(ip);
	}

	public void filter(ImageProcessor ip) {
		Rectangle roi = ip.getRoi();
		int width = roi.width;
		int height = roi.height;
		int size2 = (size+1)/2;
		int offset = (size-1)/2;
		int width2 = ip.getWidth() + offset;
		int height2 = ip.getHeight() + offset;
		float[][] mean = new float[width2][height2];
		float[][] variance = new float[width2][height2];
		int x1start = roi.x;
		int y1start = roi.y;
		int sum, sum2, n, v=0, xbase, ybase;
		for (int x1=x1start-offset; x1<x1start+width; x1++) {
			//IJ.showProgress(y1-y1start, height-1);
			for (int y1=y1start-offset; y1<y1start+height; y1++) {
				sum=0; sum2=0; n=0;
				for (int x2=x1; x2<x1+size2; x2++) {
					for (int y2=y1; y2<y1+size2; y2++) {
						v = ip.getPixel(x2, y2);
						sum += v;
						sum2 += v*v;
						n++;
					}
				}
				mean[x1+offset][y1+offset] = (float)(sum/n);
				variance[x1+offset][y1+offset] = (float)((n*sum2-sum*sum)/n);
			}
		}
for (int y=0; y<height2; y++) {
	for (int x=0; x<width2; x++) {
		variance[x][y] = x;
	}
}
		new ImagePlus("Variance", new FloatProcessor(variance)).show(); // 1.35b or later
		int xbase2=0, ybase2=0;
		float min = Float.MAX_VALUE;
		float var;
		for (int y1=y1start; y1<y1start+height; y1++) {
			//IJ.showProgress(y1-y1start, height-1);
			for (int x1=x1start; x1<x1start+width; x1++) {
				xbase = x1-offset+offset; ybase=y1;
				var = variance[xbase][ybase];
				if (var<min) {min= var; xbase2=xbase; ybase2=ybase;}
				xbase = x1+offset;
				var = variance[xbase][ybase];
				if (var<min) {min= var; xbase2=xbase; ybase2=ybase;}
				ybase = y1+offset;
				var = variance[xbase][ybase];
				if (var<min) {min= var; xbase2=xbase; ybase2=ybase;}
				xbase = x1; 
				var = variance[xbase][ybase];
				if (var<min) {min= var; xbase2=xbase; ybase2=ybase;}
				ip.putPixel(x1, y1, (int)(mean[xbase2][ybase2]+0.5));
			}
		}
	}

	void filterRGB(ImageProcessor ip) {
		ColorProcessor cp = (ColorProcessor)ip;
		int width = cp.getWidth();
		int height = cp.getHeight();
		int size = width*height;
		byte[] H = new byte[size];
		byte[] S = new byte[size];
		byte[] B = new byte[size];
		cp.getHSB(H, S, B);
		ImageProcessor ip2 = new ByteProcessor(width, height, B, null);
		filter(ip2);
		cp.setHSB(H, S, (byte[])ip2.getPixels());
	}

	boolean showDialog() {
		GenericDialog gd = new GenericDialog("Kuwahara Filter");
		gd.addNumericField("Sampling Window Width (must be odd):", size, 0, 3, "");
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		size = (int) gd.getNextNumber();
		if ((size&1)!=1) size--;
		if (size<3) size = 3;
		return true;
	}

}
