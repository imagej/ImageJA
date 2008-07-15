import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import ij.plugin.*;
import ij.measure.*;

public class Grid_Overlay implements PlugIn {
	static String[] colors = {"Red","Green","Blue","Magenta","Cyan","Yellow","Orange","Black","White"};
	static String color = "Cyan";
	static float tileWidth=16, tileHeight=16;
	int xstart, ystart;

	public void run(String arg) {
		if (IJ.versionLessThan("1.38u"))
			return;
		ImagePlus imp = IJ.getImage();
		if (showDialog())
			drawGrid(imp);
		else
			 imp.getCanvas().setDisplayList(null);
	}
		
	void drawGrid(ImagePlus imp) {
		GeneralPath path = new GeneralPath();
		int width = imp.getWidth();
		int height = imp.getHeight();
		float xoff=0;
		while (true) { // draw vertical lines
			if (xoff>=width) break;
			path.moveTo(xoff, 0f);
			path.lineTo(xoff, height);
			xoff += tileWidth;
		}
		float yoff=0.0001f;
		while (true) { // draw horizonal lines
			if (yoff>=height) break;
			path.moveTo(0f, yoff);
			path.lineTo(width, yoff);
			yoff += tileHeight;
		}
		ImageCanvas ic = imp.getCanvas();
		if (ic==null) return;
		if (path==null)
			ic.setDisplayList(null);
		else
			ic.setDisplayList(path, getColor(), null);
	}

	boolean showDialog() {
		GenericDialog gd = new GenericDialog("Grid");
		gd.addNumericField("Tile Width:", tileWidth, 0);
		gd.addNumericField("Tile Height:", tileHeight, 0);
		gd.addChoice("Color:", colors, color);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		tileWidth = (float)gd.getNextNumber();
		tileHeight = (float)gd.getNextNumber();
		color = gd.getNextChoice();
		return true;
	}

	Color getColor() {
		Color c = Color.cyan;
		if (color.equals(colors[0])) c = Color.red;
		else if (color.equals(colors[1])) c = Color.green;
		else if (color.equals(colors[2])) c = Color.blue;
		else if (color.equals(colors[3])) c = Color.magenta;
		else if (color.equals(colors[4])) c = Color.cyan;
		else if (color.equals(colors[5])) c = Color.yellow;
		else if (color.equals(colors[6])) c = Color.orange;
		else if (color.equals(colors[7])) c = Color.black;
		else if (color.equals(colors[8])) c = Color.white;
		return c;
	}
}
