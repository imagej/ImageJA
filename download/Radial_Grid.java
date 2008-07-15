import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.util.*;

public class Radial_Grid implements PlugInFilter {
	private ImagePlus imp;
	private int lineWidth = 1;
	private int degrees, xorg, yorg, length;
	private int color, red, green, blue;
	double angle;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		getData();
		drawGrid(ip);
	}

	public void drawGrid(ImageProcessor ip)
	{
		int newx, newy;

		ip.setLineWidth(lineWidth);
		ip.setColor(color);

		for(int i=0;(i*degrees)<96;i++)
		{
			newx =  xorg - (int)(length*Math.cos(angle));
			newy = yorg + (int)(length*Math.sin(angle));
			ip.moveTo(xorg,yorg);
			ip.lineTo(newx,newy);
			angle = angle - Math.toRadians(degrees);
		}
	}

	public void getData()
	{
		GenericDialog gd = new GenericDialog("Output");
		gd.addNumericField("Degrees between radii",8,0);
		gd.addMessage("Color of Radii");
		gd.addNumericField("Red",255,0);
		gd.addNumericField("Green",255,0);
		gd.addNumericField("Blue",128,0);
		gd.addMessage("Length of Radii");
		gd.addNumericField("",375,0);
		gd.addMessage("Origin of Radii");
		gd.addNumericField("x",390,0);
		gd.addNumericField("y",424,0);
		gd.addMessage("Starting Position");
		gd.addNumericField("Degrees from vertical",-41.7724532,8);
		gd.showDialog();

		degrees = (int)gd.getNextNumber();
		red = (int)gd.getNextNumber();
		green = (int)gd.getNextNumber();
		blue = (int)gd.getNextNumber();
		length = (int)gd.getNextNumber();
		xorg = (int)gd.getNextNumber();
		yorg = (int)gd.getNextNumber();
		angle = (double)gd.getNextNumber();
		angle = Math.toRadians(angle);

		color = ((red & 0xff) << 16) + ((green & 0xff) << 8) + (blue & 0xff);
	}
}
