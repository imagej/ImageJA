import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.tool.PlugInTool;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.*;


/** This plugin demonstrates how to add a tool to the toolbar.
	It requires ImageJ 1.46d or later. */
public class Example_Tool extends PlugInTool {

	public void mousePressed(ImagePlus imp, MouseEvent e) {
		show(imp, e, "clicked");
	}

	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		show(imp, e, "dragged");
	}

	public void showOptionsDialog() {
		IJ.log("User double clicked on the tool icon");
	}

	void show(ImagePlus imp, MouseEvent e, String msg) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		IJ.log("User "+msg+" at ("+x+","+y+") on "+imp.getTitle());
	}

	public String getToolIcon() {
		return "C00fO22dd"; // blue circle
	}

}


