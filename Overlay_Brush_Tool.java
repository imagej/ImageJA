import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.tool.*;
import java.awt.event.*;
import java.awt.BasicStroke;
import java.awt.geom.*;

public class Overlay_Brush_Tool extends PlugInTool {
	private float width = 5f;
	private BasicStroke stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
	private GeneralPath path;
	private boolean newPath;

	public void mousePressed(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		float x = (float)ic.offScreenXD(e.getX());
		float y = (float)ic.offScreenYD(e.getY());
		path = new GeneralPath();
		path.moveTo(x, y);
		newPath = true;
	}

	public void mouseReleased(ImagePlus imp, MouseEvent e) {
	}

	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		double x = ic.offScreenXD(e.getX());
		double y = ic.offScreenYD(e.getY());
		path.lineTo(x, y);
		Overlay overlay = imp.getOverlay();
		if (overlay==null)
			overlay = new Overlay();
		ShapeRoi roi = new ShapeRoi(path);
		roi.setStrokeColor(Toolbar.getForegroundColor());
		roi.setStroke(stroke);
		if (newPath) {
			overlay.add(roi);
			newPath = false;
		} else {
			overlay.remove(overlay.size()-1);
			overlay.add(roi);
		}
		imp.setOverlay(overlay);
	}

	public void showOptionsDialog() {
		GenericDialog gd = new GenericDialog("Overlay Brush");
		gd.addNumericField("Brush width: ", width, 1);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		else {
			width = (float)gd.getNextNumber();
			stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
		}
	}

	public String getToolIcon() {
		return "C037La077Ld098L6859L4a2fL2f4fL3f99L5e9bL9b98L6888L5e8dL888cCc00P2f7f9ebdcaf70P2e7e9dbcc9f60";
	}

}
