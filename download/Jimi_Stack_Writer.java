import java.awt.*;
import java.awt.image.*;
import java.io.*;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.PlugIn;

import com.sun.jimi.core.*;


/**

Uses the Jimi library from http://java.sun.com/products/jimi/
to save a stack in JPEG, PNG, PICT, BMP, XBM, XPM or PCX format.
The JimiProClasses.zip (aka jimi.jar) must be in ImageJ's class path.

NOTE: This plugin is the lovechild offspring of the ImageJ 
StackWriter class and the Jimi_Writer plugin.

*/

public class Jimi_Stack_Writer implements PlugIn {

	String[] types = {"png","bmp","pict","jpeg","xbm","tga","psd","xpm","pcx"};
	static String type = "png";
	private static int ndigits = 4;

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();

		if (imp == null) {
			IJ.showMessage("Save As (Jimi)", "No images are open.");
			return;
		}

		String name = imp.getTitle();
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex>=0)
			name = name.substring(0, dotIndex);

		GenericDialog gd = new GenericDialog("Jimi Writer", IJ.getInstance());
		gd.addChoice("Save As:", types, type);
		gd.addStringField("Name:", name, 12);
		gd.addNumericField("Digits (1-8):", ndigits, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		type = gd.getNextChoice();
		name = gd.getNextString();
		ndigits = (int)gd.getNextNumber();

		int number = 0;
		//if (startAtZero)
		//	number = 0;
		if (ndigits<1) ndigits = 1;
		if (ndigits>8) ndigits = 8;

		IJ.register(Jimi_Stack_Writer.class);

		String digits = getDigits(number);
		SaveDialog sd = new SaveDialog("Save as (Jimi)"+type, name+digits+"."+type, "."+type);
		String file = sd.getFileName();
		if (file == null) return;
		String directory = sd.getDirectory();

		ImageStack stack = imp.getStack();
		ImagePlus tmp = new ImagePlus();
		int nSlices = stack.getSize();
		String path = directory+name;
		for (int i=1; i<=nSlices; i++) {
			IJ.showStatus("writing: "+i+"/"+nSlices);
			IJ.showProgress((double)i/nSlices);
			tmp.setProcessor(null, stack.getProcessor(i));
			digits = getDigits(number++);

			try {
				Jimi.putImage("image/"+type, tmp.getImage(), path+digits+"."+type);
			} catch(Exception e)  {
				IJ.showMessage("Save as "+type, ""+e);
			}

			System.gc();
		}
		IJ.showStatus("");
		IJ.showProgress(1.0);

	}

	String getDigits(int n) {
		String digits = "00000000"+n;
		return digits.substring(digits.length()-ndigits,digits.length());
	}
}
