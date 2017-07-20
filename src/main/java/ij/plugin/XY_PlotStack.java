//Jérôme Parent 20.07.2017
//This plugin generates a stack of plots with the same vertical scale
//Source image is a stack or hyperstack
//Line or rectangle ROI is required 

package ij.plugin;
import ij.*;
import ij.gui.*;
import java.awt.*;

public class XY_PlotStack implements PlugIn {
	
	int channel = 1;
	int slice = 1;
	int frame = 1;
	
	public void run(String arg){
		ImagePlus imp = IJ.getImage();
		//Check if Roi is defined
		if (imp.getRoi() == null) {
			IJ.error("XY_PlotStack", "Line or rectangular selection required");
			return;
		}
		//Check if Image is a Stack
		int dim = imp.getNDimensions();
		if (dim < 3) {
			IJ.error("XY_PlotStack","stack required");
			return;
		}
		//Get Stack size
		int length = 0;
		if(dim == 3) length = imp.getImageStackSize();
		// Plot stack over frames information, improvement will be to select the dimension to plot over
		if(dim >3) {
			channel = imp.getChannel();
			slice = imp.getSlice();
			length = imp.getNFrames();
		}
		
		//Get a profile plot for each frame in the stack
		//Store min and max value of all Profile across the stack
		ProfilePlot[] pPlot = new ProfilePlot[length];
		double ymin = 0;
		double ymax = 0;
		for (int i=0; i<length; i++) {
			if (dim == 3) imp.setPosition(i+1);
			if (dim > 3) imp.setPosition(channel,slice,i+1);
			pPlot[i] =  new ProfilePlot(imp);
			if(pPlot[i] == null) return;
			if (pPlot[i].getMin() < ymin) ymin = pPlot[i].getMin();
			if (pPlot[i].getMax() > ymax) ymax = pPlot[i].getMax();
		}
		//Save current Min and Max values of profile plot
		double pp_min = ProfilePlot.getFixedMin();
		double pp_max = ProfilePlot.getFixedMax();
		//Set same Min Max values for all plots
		ProfilePlot.setMinAndMax(ymin,ymax);
		
		//Make a profile stack
		Plot plot = pPlot[0].getPlot();
		Dimension size = plot.getSize();
		ImageStack stack = new ImageStack(size.width,size.height);
		for (int i=0; i< length; i++) {
			plot = pPlot[i].getPlot();
			stack.addSlice(plot.getProcessor());
		}
		ImagePlus output = new ImagePlus("profile plot",stack);
		output.show();
		//reset profile plot Min and May
		ProfilePlot.setMinAndMax(pp_min,pp_max);
	}
	

}
