import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.text.TextWindow;
import java.awt.*;
import java.awt.image.*;

public class Lut_Lister implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL-DOES_RGB+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		listLut(ip);
	}
    
	void listLut(ImageProcessor ip) {
		IndexColorModel icm = (IndexColorModel)ip.getColorModel();
		int size = icm.getMapSize();
		byte[] r = new byte[size];
		byte[] g = new byte[size];
		byte[] b = new byte[size];
		icm.getReds(r); 
		icm.getGreens(g); 
		icm.getBlues(b);
		StringBuffer sb = new StringBuffer();
		String headings = "Index\tRed\tGreen\tBlue";
		for (int i=0; i<size; i++)
			sb.append(i+"\t"+(r[i]&255)+"\t"+(g[i]&255)+"\t"+(b[i]&255)+"\n");
		TextWindow tw = new TextWindow("LUT", headings, sb.toString(), 250, 400);
	}
    
 }
