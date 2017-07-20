package ij.plugin;
import ij.*;
//import ij.gui.*;

public class XY_PlotStack implements PlugIn {
	
	ImagePlus imp;
	
	public void run(String arg){
		imp = IJ.getImage();
		return;
	}
	

}
