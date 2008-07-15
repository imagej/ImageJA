import ij.plugin.filter.PlugInFilter;
import java.awt.Color;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.measure.*;

/**
	Uses ImageJ's particle analyzer to track the movement of
	two objects through a stack.
*/
public class Tracker_ implements PlugInFilter, Measurements  {

	ImagePlus imp;
	float[] sx1,sy1,sx2,sy2;
	double x1,y1,x2,y2;
	static int minSize = 10;
	static int maxSize = 999999;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Object Tracker");
		gd.addNumericField("Minimum Object Size (pixels): ", minSize, 0);
		gd.addNumericField("Maximum Object Size (pixels): ", maxSize, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		minSize = (int)gd.getNextNumber();
		maxSize = (int)gd.getNextNumber();
		track(imp, minSize, maxSize);
	}
	
	public void track(ImagePlus imp, int minSize, int maxSize) {
		int nFrames = imp.getStackSize();
		if (nFrames<2) {
			IJ.showMessage("Tracker", "Stack required");
			return;
		}
		ImageStack stack = imp.getStack();
		int options = 0; // set all PA options false
		int measurements = CENTROID;
		IJ.setColumnHeadings(" \tCount\tX1\tY1\tX2\tY2\tDistance");
		float[] distance = new float[nFrames];
		sx1 = new float[nFrames];
		sy1 = new float[nFrames];
		sx2 = new float[nFrames];
		sy2 = new float[nFrames];
		ResultsTable rt = new ResultsTable();
		for (int i=1; i<=nFrames; i++) {
			rt.reset();
			ParticleAnalyzer pa = new ParticleAnalyzer(options,measurements,rt, minSize,maxSize);
			pa.analyze(imp, stack.getProcessor(i));
			float[] x = rt.getColumn(ResultsTable.X_CENTROID);				
			float[] y = rt.getColumn(ResultsTable.Y_CENTROID);
			if (x==null)
				return;
			int count = x.length;
			x1=0.0;y1=0.0;x2=0.0;y2=0.0;
			if (count>=1) {
				x1 = x[0]; y1 = y[0];
				x2 = x1; y2 = y1;
			}
			if (count>=2) {
				x2 = x[1]; y2 = y[1];
				distance[i-1] = (float)Math.sqrt(sqr(x2-x1)+sqr(y2-y1));
			}
			if (count>=3) 
				distance[i-1] = 0f;
			track(i);
			IJ.write(i+"\t"+count+"\t"+IJ.d2s(x1,1)+"\t"+IJ.d2s(y1,1)+"\t"+IJ.d2s(x2,1)+"\t"+IJ.d2s(y2,1)
				+"\t"+IJ.d2s(distance[i-1],2));
			IJ.showProgress((double)i/nFrames);
		}				
		float[] frame = new float[nFrames];
		for (int i=0; i<nFrames; i++)
			frame[i] = i+1;
		new PlotWindow("Tracker", "Frame", "Distance", frame, distance).draw();
		//new PlotWindow("Tracker", "X1", "Y1", sx1, sy1);
	}

	double sqr(double n) {return n*n;}
	
	void track(int i) {
		i--;
		if (i>0) {
			double distance1 = Math.sqrt(sqr(x1-sx1[i-1])+sqr(y1-sy1[i-1]));
			double distance2 = Math.sqrt(sqr(x2-sx1[i-1])+sqr(y2-sy1[i-1]));
			if (distance2<distance1) { //swap
				double tx = x1;
				double ty = y1;
				x1 = x2;
				y1 = y2;
				x2 = tx;
				y2 = ty;
				
			}
		}
		sx1[i] = (float)x1;
		sy1[i] = (float)y1;
		sx2[i] = (float)x2;
		sy2[i] = (float)y2;
	}
	
}


