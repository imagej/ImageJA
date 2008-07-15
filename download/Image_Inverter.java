import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;

/** This sample ImageJ plugin filter inverts images.

A few things to note:
	1) Filter plugins must implement the PlugInFilter interface.
	2) User plugins do not use the package statement;
	3) Plugins residing in the "plugins" folder, and with at
	least one underscore in their name, are automatically
	installed in the PlugIns menu.
	4) Plugins can be installed in other menus by 
	packaging them as JAR files.
	5) The class name ("Image_Inverter") and file name 
	("Image_Inverter.java") must be the same.
	6) This filter works with selections, including non-rectangular selections.
	7) It will be called repeatedly to process all the slices in a stack.
	8) It supports Undo for single images.
	9) "~" is the bitwise complement operator.
*/

public class Image_Inverter implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		if (IJ.versionLessThan("1.37j"))
			return DONE;
		else	
			return DOES_ALL+DOES_STACKS+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		Rectangle r = ip.getRoi();
		for (int y=r.y; y<(r.y+r.height); y++)
			for (int x=r.x; x<(r.x+r.width); x++)
				ip.set(x, y, ~ip.get(x,y));
	}

}

