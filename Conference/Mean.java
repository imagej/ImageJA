import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Mean implements PlugIn {

	public void run(String arg) {
		long start = System.currentTimeMillis();	
		ImageProcessor ip = IJ.getImage().getProcessor();
		int n = ip.getWidth()*ip.getHeight();
		byte[] pixels = (byte[])ip.getPixels();
		int sum = 0;
		for (int i=0; i<n; i++)
			sum += pixels[i]&255;
		IJ.log("time: "+(System.currentTimeMillis()-start));
  		IJ.log("mean: "+sum/n);
	}

}
