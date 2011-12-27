import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.tool.PlugInTool;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.*;

/**
 * User: Tom Larkworthy
 * Date: 08-Jun-2006
 */
public class ROI_Brush_Tool extends PlugInTool {
	int brushWidth = 20;

	public void mousePressed(ImagePlus imp, MouseEvent e) {
		update(imp, e);
	}

	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		update(imp, e);
	}

	public void update(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		int flags = e.getModifiers();
		if ((flags&Event.ALT_MASK)==0)
			label(imp, x, y);
		else
			unlabel(imp, x, y);
	}

	void label(ImagePlus imp, int x, int y) {
		Roi roi = imp.getRoi();
		if (roi != null) {
			if (!(roi instanceof ShapeRoi))
				roi = new ShapeRoi(roi);
			ShapeRoi roiShape = (ShapeRoi) roi;
			roiShape.or(getBrushRoi(x, y, brushWidth));
		} else
			roi = getBrushRoi(x, y, brushWidth);
		imp.setRoi(roi);
	}

	void unlabel(ImagePlus imp, int x, int y) {
		Roi roi = imp.getRoi();
		if (roi != null) {
			if (!(roi instanceof ShapeRoi))
				roi = new ShapeRoi(roi);
			ShapeRoi roiShape = (ShapeRoi) roi;
			roiShape.not(getBrushRoi(x, y, brushWidth));
			imp.setRoi(roi);
		}
	}

	public void showOptionsDialog() {
		brushWidth = (int)IJ.getNumber("Brush width (pixels):", brushWidth);	}

    ShapeRoi getBrushRoi(int x, int y, int width) {
        return new ShapeRoi(new OvalRoi(x - width / 2, y - width / 2, width, width));
    }

	public String getToolIcon() {
		return "Cf800O11ff";
	}

}


