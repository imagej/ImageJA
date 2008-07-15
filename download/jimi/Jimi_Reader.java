import java.awt.*;
import ij.*;
import ij.io.*;
import ij.plugin.PlugIn;
import com.sun.jimi.core.*;

/**
Uses the Jimi library from http://java.sun.com/products/jimi/
to open a PNG image. The image file must have a ".png" extension and 
jimi.jar must be added to the -cp option of the ImageJ command line.
Also opens other image formats supported by Jini including gif, jpeg, tiff,
Pict, Photoshop, bmp, Targa, ico, cur, Sunraster, xbm, XPM, and pcx.
*/
public class Jimi_Reader implements PlugIn {

	public void run(String arg) {
		try {Class.forName("com.sun.jimi.core.Jimi");}
		catch(Exception e)  {
			IJ.showMessage("Jimi Reader", "This plugin requires jimi.jar, available from\n"
				+"\"http://rsb.info.nih.gov/ij/plugins/jimi.html\".");
			return;
		}
		OpenDialog od = new OpenDialog("Open Jimi", arg);
		String file = od.getFileName();
		if (file == null)
			return;
		String directory = od.getDirectory();

		try {
			Image img = Jimi.getImage(directory+file);
			new ImagePlus(file, img).show();
		}
		catch(Exception e)  {IJ.showMessage("Open Jimi", ""+e);}
	}

}
