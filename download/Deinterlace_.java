import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/** This plugin de-interlaces an image or stack using one of three methods. */
public class Deinterlace_ implements PlugIn {
	static final String[] methods = {"Evan field only", "Odd field only", "Double: even then odd", "Double: odd then even"};
	static String method = methods[0];

	 public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		if (imp.getBitDepth()==32) {
			IJ.error("32-bit images are not supported");
			return;
		}
		if (!showDialog(imp))
			return;
		ImageStack stack = imp.getStack();
		int n = stack.getSize();
		if (n==1)
		Undo.setup(Undo.TRANSFORM, imp);
		if (method.equals(methods[0])) {
			for (int i=1; i<=n; i++)
				evenOnly(stack.getProcessor(i));
		} else if (method.equals(methods[1])) {
			for (int i=1; i<=n; i++)
				oddOnly(stack.getProcessor(i));
		} else if (method.equals(methods[2])) {			for (int i=1; i<=stack.getSize(); i+=2) {
				ImageProcessor even = stack.getProcessor(i);
				ImageProcessor odd = even.duplicate();
				evenOnly(even);
				oddOnly(odd);
				stack.addSlice(null, odd, i);
			}
		} else {
			for (int i=1; i<=stack.getSize(); i+=2) {
				ImageProcessor odd = stack.getProcessor(i);
				ImageProcessor even = odd.duplicate();
				oddOnly(odd);
				evenOnly(even);
				stack.addSlice(null, even, i);
			}
		}
		imp.setStack(stack);
	 }

	boolean showDialog(ImagePlus imp) {
		GenericDialog gd = new GenericDialog("De-interlace");
		gd.addChoice("Method:", methods, method);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		method = gd.getNextChoice();
		return true;
	}

	/** Replaces odd lines with average of the two adjacent lines. */
	 public void evenOnly(ImageProcessor ip) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		int bottom = (height&1)==1?height:height-1;
		int[]  p1=new int[3],p2=new int[3],avg=new int[3];
		int samples = ip instanceof ColorProcessor?3:1;
		for (int y=1; y<bottom; y+=2) {
			 for (int x=0; x<width; x++) {
				 p1 = ip.getPixel(x, y-1, p1);
				 p2 = ip.getPixel(x, y+1, p2);
				 for (int i=0; i<samples; i++) {
					  avg[i] = ((p1[i]+p2[i])/2);
					  ip.putPixel(x, y, avg);
				}
			}
		}
		if (bottom!=height) {
			int[] aRow = new int[width];
			ip.getRow(0, height-2, aRow, width);
			ip.putRow(0, height-1, aRow, width);
		}
	 }

	/** Replaces even lines with average of the two adjacent lines. */
	 public void oddOnly(ImageProcessor ip) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		int[] aRow = new int[width];
		ip.getRow(0, 1, aRow, width);
		ip.putRow(0, 0, aRow, width);
		int bottom = (height&1)==1?height-1:height;
		int[]  p1=new int[3],p2=new int[3],avg=new int[3];
		int samples = ip instanceof ColorProcessor?3:1;
		for (int y=2; y<bottom; y+=2) {
			 for (int x=0; x<width; x++) {
				 p1 = ip.getPixel(x, y-1, p1);
				 p2 = ip.getPixel(x, y+1, p2);
				 for (int i=0; i<samples; i++) {
					  avg[i] = ((p1[i]+p2[i])/2);
					  ip.putPixel(x, y, avg);
				}
			}
		}
		if (bottom!=height) {
			ip.getRow(0, height-2, aRow, width);
			ip.putRow(0, height-1, aRow, width);
		}
	 }

}
