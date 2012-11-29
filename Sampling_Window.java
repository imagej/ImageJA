import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import ij.plugin.*;
import ij.measure.*;

import ij.util.Tools;
import java.awt.image.*;

public class Sampling_Window implements PlugIn, DialogListener {
	private static String[] colors = {"Red","Green","Blue","Magenta","Cyan","Yellow","Orange","Black","White"};
	private static String color = "Cyan";
	private static String[] linewidths= {"0","1","2","3","5","10"};
	private static String linew = "0";
	private static String[] linestrokes= {"2","5","10","20","50"};
	private static String lines = "10";
	private static double offX;
	private static double offY;
	private static double widthX;
	private static double widthY;
	private static int numstr;
	private static int linewidth;
	private ImagePlus imp;
	private int x1, x2, y1, y2;
	private int imageWidth, imageHeight;
	private double pixelWidth=1.0, pixelHeight=1.0;
	private String units = "pixels";

	public void run(String arg) {
		if (IJ.versionLessThan("1.43u"))	 		return;
		imp = IJ.getImage();
		showDialog();
	}
	
	void drawDotted(GeneralPath path, int x1, int y1, float dx, float dy, int n) {
		for(int i=0; i<n; i++) {
			float x = (float)(x1+2*i*dx);
			float y = (float)(y1+2*i*dy);
			path.moveTo(x,y);
			path.lineTo(x+dx,y+dy);
		}
	}

		
	void drawRect() {
		GeneralPath path = new GeneralPath();

		path.moveTo(x1, 0); 
    path.lineTo(x1, y2);
    path.lineTo(x2, y2);
          
 	  float dx= (float)((float)(x2-x1)/(2.0*numstr));
	  float dy= (float)((float)(y2-y1)/(2.0*numstr));
  	drawDotted(path,x2,y1,-dx,0,numstr);
    int numstr2=(int)(numstr*(float)(imageHeight-y1)/(y2-y1));
   	drawDotted(path,x2,y1,0,dy,numstr2);
   
//   	path.moveTo(x1, y1); 
//    path.lineTo(x2, y1);
//    path.lineTo(x2, imageHeight);
	
	  BasicStroke stroke= new BasicStroke((float)linewidth);
    imp.setOverlay(path,getColor(),stroke); 
   
 	}

	void showDialog() {
		imageWidth = imp.getWidth();
		imageHeight = imp.getHeight();
		Calibration cal = imp.getCalibration();
		int places;
		if (cal.scaled()) {
			pixelWidth = cal.pixelWidth;
			pixelHeight = cal.pixelHeight;
			units = cal.getUnits();
			places = 2;
		} else {
			pixelWidth = 1.0;
			pixelHeight = 1.0;
			units = "pixels";
			places = 0;
		}
		
		if (offX==0.0)
			offX= 0.25*imageWidth*pixelWidth;
		if (offY==0.0)
			offY= 0.25*imageHeight*pixelHeight;
		if (widthX==0.0)
			widthX= 0.5*imageWidth*pixelWidth;
		if (widthY==0.0)
			widthY= 0.5*imageHeight*pixelHeight;
	
  	ImageWindow win = imp.getWindow();
		GenericDialog gd = new GenericDialog("SampWin...");
		gd.addNumericField("Horiz. offset:", offX, places, 6, units);
		gd.addNumericField("Vert. offset:", offY, places, 6, units);
		gd.addNumericField("Width:", widthX, places, 6, units);
		gd.addNumericField("Height:", widthY, places, 6, units);
		gd.addChoice("Color:", colors, color);
		gd.addChoice("Line width:", linewidths, linew);
		gd.addChoice("Line strokes:", linestrokes, lines);
		gd.addDialogListener(this);
		gd.showDialog();
		if (gd.wasCanceled()) 
			imp.setOverlay(null);
	}

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		imageWidth = imp.getWidth();
		imageHeight = imp.getHeight();

		offX = gd.getNextNumber();
		x1= (int)(offX/pixelWidth);
		offY = gd.getNextNumber();
		y1= (int)(offY/pixelHeight);
		widthX = gd.getNextNumber();
		x2= (int)((offX+widthX)/pixelWidth);
		widthY = gd.getNextNumber();
		y2= (int)((offY+widthY)/pixelHeight);
		color = gd.getNextChoice();
		linew = gd.getNextChoice();
		linewidth= getLinew();
		lines = gd.getNextChoice();
		numstr= getLines();
		
		if (gd.invalidNumber())
			return true;
		drawRect();
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

	int getLinew() {
		int lw = 0;
		if (linew.equals(linewidths[0])) lw = 0;
		else if (linew.equals(linewidths[1])) lw = 1;
		else if (linew.equals(linewidths[2])) lw = 2;
		else if (linew.equals(linewidths[3])) lw = 3;
		else if (linew.equals(linewidths[4])) lw = 5;
		else if (linew.equals(linewidths[5])) lw = 10;
		return lw;
	}

	int getLines() {
		int ls = 10;
		if (lines.equals(linestrokes[0])) ls = 2;
		else if (lines.equals(linestrokes[1])) ls = 5;
		else if (lines.equals(linestrokes[2])) ls = 10;
		else if (lines.equals(linestrokes[3])) ls = 20;
		else if (lines.equals(linestrokes[4])) ls = 50;
		return ls;
	}
	
}
