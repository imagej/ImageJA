package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/** This plugin implements the commands in the Image/Overlay menu. */
public class OverlayLabels implements PlugIn {
	private static Overlay defaultOverlay = new Overlay();
	
	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		Overlay overlay = imp!=null?imp.getOverlay():null;
		if (overlay==null)
			overlay = defaultOverlay;
		if (showDialog(imp, overlay)) {
			defaultOverlay.drawLabels(overlay.getDrawLabels());
			defaultOverlay.drawNames(overlay.getDrawNames());
			defaultOverlay.drawBackgrounds(overlay.getDrawBackgrounds());
			defaultOverlay.setLabelColor(overlay.getLabelColor());
		}
	}
	
	public boolean showDialog(ImagePlus imp, Overlay overlay) {
		boolean showLabels = overlay.getDrawLabels();
		boolean showNames = overlay.getDrawNames();
		boolean drawBackgrounds = overlay.getDrawBackgrounds();
		String labelColor = decodeColor(overlay.getLabelColor(), Color.white);
		GenericDialog gd = new GenericDialog("Labels");
		gd.addStringField("Label color:", labelColor, 6);
		gd.addCheckbox("Show labels", showLabels);
		gd.addCheckbox("Use names as labels", showNames);
		gd.addCheckbox("Draw backgrounds", drawBackgrounds);
		//checkboxes = gd.getCheckboxes();
		//((Checkbox)checkboxes.elementAt(existingOverlay?3:2)).addItemListener(this);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		boolean showLabels2 = showLabels;
		boolean showNames2 = showNames;
		boolean drawBackgrounds2 = drawBackgrounds;
		String labelColor2 = labelColor;
		boolean sl =showLabels;
		boolean sn = showNames;
		boolean db = drawBackgrounds;
		String lcolor = labelColor;
		labelColor = gd.getNextString();
		showLabels = gd.getNextBoolean();
		showNames = gd.getNextBoolean();
		drawBackgrounds = gd.getNextBoolean();
		Color color = Colors.decode(labelColor, Color.black);
		if (showNames) showLabels = true;
		boolean changes = showLabels!=sl || sn!=showNames
			|| drawBackgrounds!=db || !labelColor.equals(lcolor);
		if (changes) {
			overlay.drawLabels(showLabels);
			overlay.drawNames(showNames);
			overlay.drawBackgrounds(drawBackgrounds);
			overlay.setLabelColor(color);
			if (imp!=null && imp.getOverlay()!=null)
				imp.draw();
		}
		return true;
	}
	
	private String decodeColor(Color color, Color defaultColor) {
		if (color==null)
			color = defaultColor;
		String str = "#"+Integer.toHexString(color.getRGB());
		if (str.length()==9 && str.startsWith("#ff"))
			str = "#"+str.substring(3);
		String lc = Colors.hexToColor(str);
		if (lc!=null) str = lc;
		return str;
	}
	
	static Overlay getDefaultOverlay() {
		return defaultOverlay.duplicate();
	}

}
