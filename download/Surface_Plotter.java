import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.image.*;
import java.math.*;

public class Surface_Plotter implements PlugIn {

	static int plotWidth =400;
	static double angleInDegrees =  40;
	static double angle = (angleInDegrees/360.0)*2.0*Math.PI;
	static int polygonMultiplier = 100;
	static boolean oneToOne;

	ImagePlus img;
	int[] x,y;
		
	public void run(String arg) {
		img = WindowManager.getCurrentImage();
		if (img==null)
			{IJ.noImage(); return;}
		if (!showDialog())
			return;
		if (img.getType()!=ImagePlus.GRAY8) {
			ImageProcessor ip = img.getProcessor();
			ip = ip.crop(); // duplicate
			img = new ImagePlus("temp", ip);
			 new ImageConverter(img).convertToGray8();
		}
		ImageProcessor plot = makeSurfacePlot(img.getProcessor());	
		new ImagePlus("Surface Plot", plot).show();
		IJ.register(Surface_Plotter.class);
	}

	boolean showDialog() {
		GenericDialog gd = new GenericDialog("Surface Plotter");
		gd.addNumericField("Width (pixels):", plotWidth, 0);
		gd.addNumericField("Angle (-90-90 degrees):", angleInDegrees, 0);
		gd.addNumericField("Polygon Multiplier (10-200%):", polygonMultiplier, 0);
		gd.addCheckbox("One Polygon Per Line", oneToOne);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		plotWidth = (int) gd.getNextNumber();
		angleInDegrees = gd.getNextNumber();
		polygonMultiplier = (int)gd.getNextNumber();
		oneToOne = gd.getNextBoolean();
		if (polygonMultiplier>400) polygonMultiplier = 400;
		if (polygonMultiplier<10) polygonMultiplier = 10;
		return true;
	}

	public ImageProcessor makeSurfacePlot(ImageProcessor ip) {
		double angle = (angleInDegrees/360.0)*2.0*Math.PI;
		int polygons = (int)(plotWidth*(polygonMultiplier/100.0)/4);
		if (oneToOne)
			polygons = ip.getHeight();
		double xinc = 0.8*plotWidth*Math.sin(angle)/polygons;
		double yinc = 0.8*plotWidth*Math.cos(angle)/polygons;
		boolean smooth = true;
		IJ.showProgress(0.01);
		ip.setInterpolate(true);
		ip = ip.resize(plotWidth, polygons);
		int width = ip.getWidth();
		int height = ip.getHeight();
		double min = ip.getMin();
		double max = ip.getMax();

		if (smooth)
			ip.smooth();
		//new ImagePlus("Image", ip).show();

		int windowWidth =(int)(plotWidth+polygons*Math.abs(xinc) + 20.0);
		int windowHeight = (int)(255+polygons*yinc + 10.0);
		Image plot =IJ.getInstance().createImage(windowWidth, windowHeight);
		Graphics g = plot.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, windowWidth, windowHeight);

		x = new int[width+2];
		y = new int[width+2];
		double xstart = 10.0;
		if (xinc<0.0)
			xstart += Math.abs(xinc)*polygons;
		double ystart = 0.0;
		for (int row=0; row<height; row++) {
			double[] profile = ip.getLine(0, row, width-1, row);
			Polygon p = makePolygon(profile, xstart, ystart);
			g.setColor(Color.white);
			g.fillPolygon(p);
			g.setColor(Color.black);
			g.drawPolygon(p);
			xstart += xinc;
			ystart += yinc;
			if ((row%5)==0) IJ.showProgress((double)row/height);
		}
		IJ.showProgress(1.0);

		ip = new ColorProcessor(plot);
		byte[] bytes = new byte[windowWidth*windowHeight];
		int[] ints =(int[]) ip.getPixels();
		for (int i=0; i<windowWidth*windowHeight; i++)
			bytes[i] = (byte)ints[i];
		ip = new ByteProcessor(windowWidth,windowHeight, bytes, null);
		return ip;
	}

	Polygon makePolygon(double[] profile, double xstart, double ystart) {
		int width = profile.length;
		for (int i=0; i<width; i++) 
			x[i] =(int)( xstart+i);
		for (int i=0; i<width; i++) 
			y[i] =(int)(ystart+255.0-profile[i]);
		x[width] =(int)xstart+width-1;
		y[width] = (int)ystart+255;
		x[width+1] =(int)xstart;
		y[width+1] = (int)ystart+255;

		//for (int i=0; i<width; i++)
		//	IJ.write("dbg: "+i+"  "+profile[i]+"  "+x[i]+"  "+y[i]);

		return new Polygon(x, y, width+2);
	}

}

