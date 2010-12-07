import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Byte_Find_Max implements PlugIn {
	
	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		if (imp.getBitDepth()!=8) {
			IJ.error("This plugin only works with 8-bit images.");
			return;
		}
		long start = System.currentTimeMillis();
		int[] pos = run(imp);
		int x=pos[0], y=pos[1], z=pos[2];
		String str = position(x,y,z,imp);
		showTime(imp, start);
		IJ.makePoint(x, y);
		imp.setPosition(z+1);
		double max = imp.getProcessor().getPixelValue(x,y);
		IJ.log("Maximum of "+max+" found at "+str);
	}

	public int[] run(ImagePlus imp) {
		int index=0, z=0;
		int max = -Integer.MAX_VALUE;
		ImageStack stack = imp.getStack();
		int width = imp.getWidth();
		int height = imp.getHeight();
		int n = width*height;
		int images = imp.getStackSize();
		for (int img=1; img<=images; img++) {
			ImageProcessor ip = stack.getProcessor(img);
			byte[] pixels = (byte[])ip.getPixels();
			for (int i=0; i<n; i++) {
				int v = pixels[i]&0xff;
				if (v>max) {
					max = v; 
					index = i;
					z = img-1;
				}
			}
		}
		int x = index%width;
		int y = index/width;
		int[] pos = new int[3];
		pos[0]=x; pos[1]=y; pos[2]=z;
		return pos;
	}

	void showTime(ImagePlus imp, long start) {
		int images = imp.getStackSize();
 		double time = (System.currentTimeMillis()-start)/1000.0;
		IJ.showTime(imp, start, "", images);
 		double mpixels = (double)(imp.getWidth())*imp.getHeight()*images/1000000;
 		IJ.log("\n"+imp);
 		IJ.log(IJ.d2s(mpixels/time,1)+" million pixels/second");
 	}

	String position(int x, int y, int z, ImagePlus imp) {
		String pos = x+","+y;
		if (imp.isHyperStack()) {
			int[] p = imp.convertIndexToPosition(z+1);
			pos += " (";
			pos += "c="+ p[0];
			pos += ",z="+ p[1];
			pos += ",t="+ p[2];
			pos += ")";
		} else if (imp.getStackSize()>1)
			pos += ","+ z;
		return pos;
	}

}
