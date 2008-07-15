import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.gui.*;
import ij.process.*;
import java.awt.*;

/** ColorInverter
  *
  * Inverts the pixels in the ROI of a RGB image.
  *
  * This is an example plugin from the ImageJ plugin writing tutorial.
  * The tutorial can be downloaded at 
  * http://www.fhs-hagenberg.ac.at/staff/burger/ImageJ/tutorial
  */
public class ColorInverter_ implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about"))
			{showAbout(); return DONE;}
                return DOES_RGB+NO_CHANGES;

	}

	public void run(ImageProcessor ip) {
		
		// get width, height and the region of interest
		int w = ip.getWidth();     
		int h = ip.getHeight();    
		Rectangle roi = ip.getRoi();

		// create a new image with the same size and copy the pixels of the original image
		ImagePlus inverted = NewImage.createRGBImage ("Inverted image", w, h, 1, NewImage.FILL_BLACK);
		ImageProcessor inv_ip = inverted.getProcessor();
		inv_ip.copyBits(ip,0,0,Blitter.COPY);
		int[] pixels = (int[]) inv_ip.getPixels();

		// invert the pixels in the ROI
		for (int i=roi.y; i<roi.y+roi.height; i++) {
			int offset =i*w; 
			for (int j=roi.x; j<roi.x+roi.width; j++) {
				int pos = offset+j;
				int c = pixels[pos];
		    	int r = (c&0xff0000)>>16;
		    	int g = (c&0x00ff00)>>8;
		    	int b = (c&0x0000ff);
		   		r = 255-r;
		   		g=255-g;
		   		b=255-b;
		   		pixels[pos] = ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff);		
			}
		}

		inverted.show();
		inverted.updateAndDraw();

	}

	void showAbout() {
		IJ.showMessage("ColorInverter",
			"inverts ROI of a RGB image"
		);
	}

}

