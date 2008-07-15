import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.image.*;
import ij.plugin.*;

/**	This plugin imports a LUT from a 3 or 4  column text file, such as those created
	by the LUT_Lister plugin (rsb.info.nih.gov/ij/plugins/lut-lister.html). The
	LUT is added to the current image or, if no image is open, a new 256x32 image
	is created to display the LUT.  */
public class LUT_Importer implements PlugIn {

	public void run(String arg) {
		if (IJ.versionLessThan("1.26f"))
			return;
		TextReader tr = new TextReader();
		ImageProcessor ip = tr.open();
		if (ip==null)
			return;
		int width = ip.getWidth();
		int height = ip.getHeight();
		if (!((width==3||width==4)&&(height==256||height==257))) {
			IJ.showMessage("LUT Importer", "3 or 4 column text file required.\n"
				+"This file has "+width+" columns.");
			return;
		}
		int x = width==4?1:0;
		int y = 0;
		ip.setRoi(x, y, 3, 256);
		ip = ip.crop();
		//new ImagePlus("ip", ip).show();
		byte[] reds = new byte[256];
		byte[] greens = new byte[256];
		byte[] blues = new byte[256];
		for (int i=0; i<256; i++) {
			reds[i] = (byte)ip.getPixelValue(0,i);
			greens[i] = (byte)ip.getPixelValue(1,i);
			blues[i] = (byte)ip.getPixelValue(2,i);
		}
		IndexColorModel cm = new IndexColorModel(8, 256, reds, greens, blues);
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			makeImage(tr.getName(), cm);
		else {
			imp.getProcessor().setColorModel(cm);
			imp.updateAndDraw();
		}
		
	}
	
	void makeImage(String title, IndexColorModel cm) {
		int width = 256;
		int height = 32;
		byte[] pixels = new byte[width*height];
		ByteProcessor bp = new ByteProcessor(width, height, pixels, cm);
		int[] ramp = new int[width];
		for (int i=0; i<width; i++)
			ramp[i] = i; 
		for (int y=0; y<height; y++)
			bp.putRow(0, y, ramp, width);
		new ImagePlus(title, bp).show();
  	}
}
