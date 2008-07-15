import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import java.awt.*;

/** This plugin demonstrates how to add a tool to the toolbar. */
public class Tool_Demo implements PlugIn {

	public void run(String arg) {
		if (IJ.versionLessThan("1.37c")) return;
		String macro =
			"macro 'Example Tool-C090T0f15EC00aT7f15T' {\n" +
			"  getCursorLoc(x, y, z, flags);\n" +
			"  call('Tool_Demo.mousePressed', x, y, z, flags);\n"+
			"}";
		new MacroInstaller().install(macro);
  	}

	public static void mousePressed(String xs, String ys, String zs, String flags) {
		int x = Integer.parseInt(xs);
		int y = Integer.parseInt(ys);
		ImagePlus img = WindowManager.getCurrentImage();
		if (img!=null)
			IJ.log("User clicked at ("+x+","+y+") on "+img.getTitle());
	}

}
