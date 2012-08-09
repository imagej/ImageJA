import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.plugin.tool.PlugInTool;
import java.awt.event.MouseEvent;

/* This tool displays, in the status bar, the location and pixel value
 * as the cursor is moved over an image, overriding the built in
 * function that does this. Double click on the tool icon to set 
 * the number of decimal places (default is 3).
*/
public class N_Digits_Tool extends PlugInTool {
	protected int digits = 3;

	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int sx = e.getX();
		int sy = e.getY();
		int ox = ic.offScreenX(sx);
		int oy = ic.offScreenY(sy);

		String message;
		if (ox < 0 || oy < 0 || ox >= imp.getWidth() || oy >= imp.getHeight())
			return;
		message = "x=" + ox + ", y=" + oy + " value=";
		if (imp.getType() == ImagePlus.COLOR_RGB) {
			int value = imp.getProcessor().getPixel(ox, oy);
			message += "" + ((value >> 16) & 0xff) + "," + ((value >> 8) & 0xff) + "," + (value & 0xff);
		}
		else
			message += IJ.d2s(imp.getProcessor().getf(ox, oy), digits);
		IJ.showStatus(message);

		// Tell ImageJ to not to bother with the status
		e.consume();
	}

	public void showOptionsDialog() {
		GenericDialog gd = new GenericDialog("N Digits Options");
		gd.addNumericField("Decimal places:", digits, 0);
		gd.showDialog();
		if (!gd.wasCanceled())
			digits = (int)gd.getNextNumber();
	}
}
