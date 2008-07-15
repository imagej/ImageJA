import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.PlugIn;

import com.sun.jimi.core.*;


/**

Uses the Jimi library from http://java.sun.com/products/jimi/
to save in JPEG, PNG, PICT, BMP, XBM, XPM or PCX format. The
JimiProClasses.zip (aka jimi.jar) must be in ImageJ's class path.
*/

public class Jimi_Writer implements PlugIn {

	String[] types = {"png","bmp","pict","jpeg","xbm","tga","psd","xpm","pcx","ico"};
	static String type = "png";

	public void run(String arg) {
		try {Class.forName("com.sun.jimi.core.Jimi");}
		catch(Exception e)  {
			IJ.showMessage("Jimi Writer", "This plugin requires jimi.jar, available from\n"
				+"\"http://rsb.info.nih.gov/ij/plugins/jimi.html\".");
			return;
		}
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Save As (Jimi)", "No images are open.");
			return;
		}

		GenericDialog gd = new GenericDialog("Jimi Writer", IJ.getInstance());
		gd.addChoice("Format:", types, type);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		type = gd.getNextChoice();
		IJ.register(Jimi_Writer.class);

		SaveDialog sd = new SaveDialog("Save as (Jimi)"+type, imp.getTitle(), "."+type);
		String file = sd.getFileName();
		if (file == null) return;
		String directory = sd.getDirectory();
		//String[] types2 = Jimi.getDecoderTypes();
		//for (int i=0; i<types2.length; i++)
		//	IJ.write(types2[i]); 

		try {Jimi.putImage("image/"+type, imp.getImage(), directory+file);}
		catch(Exception e)  {IJ.showMessage("Save as "+type, ""+e);}
	}
}
