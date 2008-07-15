import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.measure.*;
import ij.gui.*;
import java.awt.*;

public class Shading_Corrector implements PlugInFilter, Measurements {

	boolean canceled;

	static final String ABOUT =
		"This plugin corrects acquired images for shading due to\n"+
		"uneven illumination or CCD pixel irregularities. It does\n"+
		"this using the formula\n"+
		" \n"+
		"      I = I(M/BF)\n"+
		" \n"+
		"where I is the active image, BF is the blank field image\n"+
		"('blank-field'), and M is the mean of the blank field image.\n"+
		"Install this plugin with the argument 'rename' to create a\n"+
		"a command that renames the active image to 'blank-field.'";

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about"))
			{showAbout(); return(DONE);}
		if (arg.equals("rename"))
			{rename(); return(DONE);}
		if (imp!=null && imp.getTitle().equals("blank-field"))
			{error("Cannot correct \"blank-field\"."); return DONE;}
		return DOES_ALL+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		if (canceled)
			return;
 		ImagePlus blankImage = getBlankField("blank-field");
 		if (blankImage!=null)
			try {correctShading(ip, blankImage.getProcessor());}
			catch (Exception e) {error(e.getMessage());}
		else
			error("Unable to find an image named 'blank-field'.");
	}

	/** Changes the title of the active image to "blank-field". */
	public void rename() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		if (imp.getTitle().equals("blank-field"))
			{error("This image is already named 'blank-field'"); return;}
		int[] list = WindowManager.getIDList();
		if (list==null)
			return;
		for (int i=0; i<list.length; i++) {
			ImagePlus img = WindowManager.getImage(list[i]);
			if (img!=null && img.getTitle().equals("blank-field")) {
				img.changes = false;
				ImageWindow win = img.getWindow();
				if (win!=null)
					win.close();
			}
		}
		imp.setTitle("blank-field");
	}

	public ImagePlus getBlankField(String title) {
		int[] list = WindowManager.getIDList();
		if (list==null)
			return null;
		ImagePlus blankImage = null;
		int count = 0;
		for (int i=0; i<list.length; i++) {
			ImagePlus img = WindowManager.getImage(list[i]);
			if (img!=null && img.getTitle().equals(title)) {
				blankImage = img;
				count++;
			}
		}
		if (count>1)
			error("Using the first of "+count+" 'blank-field' images.");
		return blankImage;
	}
	
	public void correctShading(ImageProcessor ip, ImageProcessor blankImage) {
 		int width = ip.getWidth();
		int height = ip.getHeight();
		if (width!=blankImage.getWidth() || height!=blankImage.getHeight())
			throw new IllegalArgumentException("Image and blank-field do not have the same dimensions");
		if (ip instanceof ByteProcessor)
			correctByteShading(ip, blankImage, width, height);
		else if (ip instanceof ShortProcessor)
			correctShortShading(ip, blankImage, width, height);
		else
  			error("Unsupported data type");
 	}
	
	public void correctByteShading(ImageProcessor ip, ImageProcessor blankImage, int width, int height) {
 		if (!(blankImage instanceof ByteProcessor))
  			{error("Image and blank-field are not the same type"); return;}
  		ImageStatistics stats = ImageStatistics.getStatistics(blankImage, MEAN, null);
  		double mean = stats.mean;
  		byte[] img = (byte[])ip.getPixels();
  		byte[] bg = (byte[])blankImage.getPixels();
		for (int i=0; i<width*height; i++)
 			img[i] = (byte)(((img[i]&255)*(mean/(bg[i]&255)))+0.5);
  	}
 	
 	public void correctShortShading(ImageProcessor ip, ImageProcessor blankImage, int width, int height) {
 		if (!(blankImage instanceof ShortProcessor))
  			{error("Image and blank-field are not the same type"); return;}
  		ImageStatistics stats = ImageStatistics.getStatistics(blankImage, MEAN, null);
  		double mean = stats.mean;
  		short[] img = (short[])ip.getPixels();
  		short[] bg = (short[])blankImage.getPixels();
		for (int i=0; i<width*height; i++)
 			img[i] = (short)(((img[i]&0xffff)*(mean/(bg[i]&0xffff)))+0.5);
  	}

	void error(String msg) {
 		IJ.showMessage("Shading_Corrector",msg);
 		canceled = true;
	}
	
	public void showAbout() {
		IJ.showMessage("Shading-Corrector...", ABOUT);
	}
    
}


