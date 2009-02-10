import ij.plugin.*;
import java.awt.*;
import java.io.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;

/*  Converts a folder of images in any format supported by ImageJ's 
File>Open command into TIFF, 8-bit TIFF, JPEG, GIF, PNG, PGM,
BMP, FITS, Text Image, ZIP or Raw. The plugin displays three dialogs. 
In the first, select the source folder. In the second, select the format
you want to convert to. In the third, select the destination folder.
*/
public class Batch_Converter implements PlugIn {

	private static String[] choices = {"TIFF", "8-bit Color TIFF", "JPEG", "GIF", "PNG", "PGM", "BMP", "FITS", "Text Image", "ZIP", "Raw"};
	private static String format = "TIFF";
	private static boolean convertToGrayscale;
	
	public void run(String arg) {
		String dir1 = IJ.getDirectory("Select source folder...");
		if (dir1==null) return;
		if (!showDialog()) return;
		String dir2 = IJ.getDirectory("Select destination folder...");
		if (dir2==null) return;
		convert(dir1, dir2, format);
	}

	boolean showDialog() {
		GenericDialog gd = new GenericDialog("Batch Converter");
		gd.addChoice("Convert to: ", choices, format);
		gd.addCheckbox("Make Grayscale", convertToGrayscale);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		format = gd.getNextChoice();
		convertToGrayscale = gd.getNextBoolean();
		return true;
	}

	public void convert(String dir1, String dir2, String format) {
		IJ.log("\\Clear");
		IJ.log("Converting to "+format);
		IJ.log("dir1: "+dir1);
		IJ.log("dir2: "+dir2);
		String[] list = new File(dir1).list();
		if (list==null) return;
		for (int i=0; i<list.length; i++) {
			IJ.showProgress(i, list.length);
			IJ.log((i+1)+": "+list[i]+"  "+WindowManager.getImageCount());
			IJ.showStatus(i+"/"+list.length);
			boolean isDir = (new File(dir1+list[i])).isDirectory();
			if (!isDir && !list[i].startsWith(".")) {
				ImagePlus img = IJ.openImage(dir1+list[i]);
				if (img==null) continue;
				img = process(img);
				if (img==null) continue;
				if (img.isComposite())
					img = convertToRGB(img);
				if (img.getStackSize()>1)
					img = getFirstSlice(img);
				if (convertToGrayscale)
					img = convertToGrayscale(img);
				if (format.equals("8-bit Color TIFF")||format.equals("GIF"))
					img = convertTo8Bits(img);
				WindowManager.setTempCurrentImage(img);
				IJ.saveAs(format, dir2+list[i]);
			}
		}
		IJ.showProgress(1.0);
		IJ.showStatus("");
	}

	/** This is the place to add code to process each image. The image 
		is not written if this method returns null. */
	public ImagePlus process(ImagePlus img) {
		double scale = 0.5;
		int width = img.getWidth();
		int height = img.getHeight();
		//ImageProcessor ip = img.getProcessor();
		//ip.setInterpolate(true);
		//ip = ip.resize((int)(width*scale), (int)(height*scale));
		//img.setProcessor(null, ip);
		return img;
	}

	ImagePlus convertTo8Bits(ImagePlus img) {
		ImageProcessor ip = img.getProcessor();
		if (ip instanceof ColorProcessor) {
			MedianCut mc = new MedianCut((int[])ip.getPixels(), ip.getWidth(), ip.getHeight());
			img.setProcessor(null, mc.convertToByte(256));
		} else {
			ip = ip.convertToByte(true);
			img.setProcessor(null, ip);
		}
		return img;
	}

	ImagePlus convertToRGB(ImagePlus img) {
		ImagePlus img2 = img.createImagePlus();
		img.updateImage();
		img2.setProcessor(img.getTitle(), new ColorProcessor(img.getImage()));
		return img2;
	}

	ImagePlus getFirstSlice(ImagePlus img) {
		ImagePlus img2 = img.createImagePlus();
		img2.setProcessor(img.getTitle(), img.getProcessor());
		return img2;
	}

	ImagePlus convertToGrayscale(ImagePlus img) {
		ImagePlus img2 = img.createImagePlus();
		img2.setProcessor(img.getTitle(), img.getProcessor().convertToByte(true));
		return img2;
	}

	/**	Run Batch_Converter using a command something like
			"java -cp ij.jar;. Batch_Converter c:\dir1\ c:\dir2\"
		or (Unix)
			"java -cp ij.jar:. Batch_Converter /users/wayne/dir1 /users/wayne/dir2/"
	*/
	public static void main(String args[]) {
		if (args.length<2)
			IJ.log("usage: java Batch_Converter srcdir dstdir");
		else {
			new Batch_Converter().convert(args[0], args[1], "Jpeg");
			System.exit(0);
		}
	}

}


