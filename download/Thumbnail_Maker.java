import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.plugin.TextReader;

/** This plugin creates a 100 pixel wide 8-bit indexed color version of the active image. */
public class Thumbnail_Maker implements PlugInFilter {

	static final int THUMBNAIL_WIDTH = 100;
	ImagePlus imp;
	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		ImagePlus imp2 = makeThumbnail(imp, THUMBNAIL_WIDTH);
		//ImagePlus imp2 = addText(imp, "This is some text");
		if (imp2!=null) {
			imp2.setTitle(imp.getShortTitle()+" Thumbnail");
			imp2.show();
		}
	}
	
	/* Converts the specified image to an image 8-bit indexed color image of the specied width. */
	ImagePlus makeThumbnail(ImagePlus imp, int thumbnailWidth) {
		if (imp==null)
			return null;
		ImageProcessor ip = imp.getProcessor();
		int width = ip.getWidth();
		int height = ip.getHeight();
		if (imp.getType()==ImagePlus.COLOR_256)
			ip = ip.convertToRGB();
		ip.smooth();
		ip.setInterpolate(true);
		ImageProcessor ip2 = ip.resize(thumbnailWidth, thumbnailWidth*height/width);
		ip.reset();
		if (ip2 instanceof ShortProcessor || ip2 instanceof FloatProcessor)
			ip2 = ip2.convertToByte(true);
		ip2 = reduceColors(ip2, 256);
		return new ImagePlus("Thumbnail", ip2);
	}
	
	ImageProcessor reduceColors(ImageProcessor ip, int nColors) {
		if (ip instanceof ByteProcessor && nColors==256)
			return ip;
		ip = ip.convertToRGB();
		MedianCut mc = new MedianCut((int[])ip.getPixels(), ip.getWidth(), ip.getHeight());
		Image img = mc.convert(nColors);
		return(new ByteProcessor(img));
	}

	/*
	You can also run Thumbnail_Maker using a command something like
	
		java -cp ij.jar;. Thumbnail_Maker input-image output-image
		
	The input-image will be opened as a text image if the file names ends in ".txt".
		
	With Java 1.4 and ImageJ 1.27i or later, it can be used on "headless" (no display) servers.
	This example compiles the plugin and runs it in headless mode. 

		c:\jdk1.4\bin\javac -classpath ij.jar Thumbnail_Maker.java
		c:\jdk1.4\bin\java -cp ij.jar;. -Djava.awt.headless=true Thumbnail_Maker c:\images\peppers.jpg c:\images\thumbnail.jpg 
	*/
	public static void main(String args[]) {
		if (args.length<2)
			IJ.write("usage: javaThumbnail_Maker input-image output-image");
		else {
			 Thumbnail_Maker tm = new Thumbnail_Maker();
			 IJ.write("opening: "+args[0]);
			 ImagePlus imp1;
			 if (args[0].endsWith(".txt"))
			 	imp1 = tm.openTextImage(args[0]);
			 else
			 	 imp1 = new Opener().openImage(args[0]);
			 if (imp1==null){
				IJ.write("error: openImage returned null");
				return;
			 }
			 IJ.write("making thumbnail");
			 ImagePlus imp2 = tm.makeThumbnail(imp1, 100);
			 IJ.write("saving thumbnail: "+args[1]+" ");
			 if (imp2!=null)
				new FileSaver(imp2).saveAsJpeg(args[1]);
		}
	}
	
	//Throws a HeadlessException with Java 1.4  in headless mode
	ImagePlus addText(ImagePlus imp) {
		if (imp==null)
			return null;
		ImageProcessor ip2 = imp.getProcessor();
		ip2 = ip2.duplicate();
		ip2.setFont(new Font("SansSerif", Font.PLAIN, 18));
		ip2.moveTo(20, 30);
		ip2.drawString("This is 18-point, 'SansSerif', PLAIN.");		
		return new ImagePlus("Image with text", ip2);
	}
	
	ImagePlus openTextImage(String path) {
		TextReader tr = new TextReader();
		ImageProcessor ip = tr.open(path);
		if (ip!=null)
			return new ImagePlus(tr.getName(), ip);
		else
			return null;
	}

}
