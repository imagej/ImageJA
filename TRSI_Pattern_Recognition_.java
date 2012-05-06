/** TRSI Pattern Recognition
  * Version May 1, 2012
  * A Plugin for Translation, Rotation, and Scale Invariant Character/Pattern Recognition using Modified Ring Projection
  * By Gholam Reza Kaka and Kaiser Niknam
  *
  * Reference: Niknam, Kaiser and Gholam Reza Kaka. Translation, Rotation, and Scale Invariant Character Recognition using Modified Ring Projection.� International Journal of Imaging & Robotics 7.1 (2012): 1-10.
  * Research Center of Neuroscience, Baqiyatallah Medical Sciences University, Tehran, Iran
  * Email: gh_kaka@yahoo.com
  * Email: niknam.kaiser@gmail.com
**/

import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.*;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.lang.Math.*;
import java.util.Properties;

public class TRSI_Pattern_Recognition_ implements ExtendedPlugInFilter, DialogListener {

	ImagePlus imp = null;
	private static int N=100; // Sampling number
	private static float rho[]; // Rho variable
	private static float ref[]; // Reference modified ring projections (modified ring projections of pattern image)
	private static int data[][]; // Binary matrix of binary image
	private static int intError=30; // Acceptable error (in percent)
	private static int intOtsu=127; // Grayscale image is converted to binary image using this threshold value
	private static boolean blbg = true; // Black background (true) or white background (false)
	private static boolean sciv = false; // Write in percent of similarity (true) or not (false)
	
	public int setup(String arg, ImagePlus imp) {
		if (imp == null) {
			IJ.noImage();
			return DONE;
		}
		if (imp.getType() != ImagePlus.GRAY8) {
			IJ.error("TRSI_Pattern_Recognition_", "This command requires an image of type: 8-bit grayscale");
			return DONE;
		}
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}
		this.imp = imp;
		return DOES_8G+ROI_REQUIRED;
	}

	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog("Pattern Recognition", IJ.getInstance());
		ImageProcessor tp = imp.getProcessor(); intOtsu = Otsu(tp.duplicate());
		gd.addSlider(" Acceptable Error (%) ", 1., 100., intError);
		gd.addSlider(" Binarization Threshold ", 0., 255., intOtsu);
		gd.addCheckbox(" Black Background", blbg);
		gd.addCheckbox(" Write in Similarity (%)", sciv);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.showDialog();
		if (gd.wasCanceled()) return DONE;
		IJ.register(this.getClass());
		return IJ.setupDialog(imp, DOES_8G);
	}

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		Vector numericFields = gd.getNumericFields();
		intError = (int)gd.getNextNumber();
		intOtsu = (int)gd.getNextNumber();
		blbg = gd.getNextBoolean();
		sciv = gd.getNextBoolean();
		return true;
	}

	public void run(ImageProcessor ip) {
		try {
			ImageProcessor icopy = ip.duplicate();
			Rectangle roi = ip.getRoi(); // Pattern region
			ip.setRoi(0, 0, 0, 0); // Kill roi
			
			int H = icopy.getHeight(); // Image Height
			int W = icopy.getWidth(); // Image Width
			icopy.threshold(intOtsu); // Image binarization
			if(!blbg) icopy.invert(); // Set background color to black
			
			data = new int[W][H]; // Matrix of binary image
			for(int i=0; i<W; i++) {
				for(int j=0; j<H; j++) {
					if(icopy.getPixel(i,j)>0) data[i][j] = 1; else data[i][j] = 0;
				}
			}
			
			rho = new float[N];
			for(int i=0; i<N; i++) rho[i] = ((float)i)/((float)N)*((float)Math.sqrt(2.)); // rho = [0..sqrt(2)]
			ref = new float[N];
			ref = modified_ring(1, roi.x, roi.y, roi.width+roi.x-1, roi.height+roi.y-1, icopy.duplicate()); // Reference modified ring projections
			// Areas identification
			int m = 1;
			for(int i=0; i<W; i++) {
				for(int j=0; j<H; j++) {
					if(data[i][j]==1) {
						m++;
						FloodFill(i, j, W, H, m);
					}
				}
			}
			// Comparison modified ring projection of all identified areas with reference modified ring projection
			for(int indm=2; indm<=m; indm++) {
				int xmin=-1; int xmax=-1; int ymin=-1; int ymax=-1;
				for(int i=0; i<W; i++) {
					for(int j=0; j<H; j++) {
						if(data[i][j]==indm) {
							if(xmin==-1) xmin=i; else xmin=Math.min(xmin, i);
							if(xmax==-1) xmax=i; else xmax=Math.max(xmax, i);
							if(ymin==-1) ymin=j; else ymin=Math.min(ymin, j);
							if(ymax==-1) ymax=j; else ymax=Math.max(ymax, j);
						}
					}
				}
				float sum=0, error=0, ringm[];
				ringm = modified_ring(indm, xmin, ymin, xmax, ymax, icopy.duplicate());
				for(int index=0; index<N; index++) {
					sum += ref[index];
					error += Math.abs(ringm[index]-ref[index]);
				}
				error=error/sum*100;
				if(error<(float)intError) { // Draw an oval around the similar patterns
					if(blbg) ip.setColor(Color.white); else ip.setColor(Color.black); ip.setLineWidth(2); ip.drawOval((3*xmin-xmax)/2, (3*ymin-ymax)/2, 2*(xmax-xmin+1), 2*(ymax-ymin+1));
				}
				if(sciv) { // Write in percent of similarity
					if(blbg) ip.setColor(Color.white); else ip.setColor(Color.black); ip.setLineWidth(2); ip.drawString(Float.toString((float)Math.floor(100*error)/100)+"%", xmin, ymin);
				}
			}
			// Restore region of pattern and re-draw image
			imp.setRoi(roi); imp.updateAndDraw();
		}
		catch(Exception e) {
			IJ.error("Runtime Error", e.getMessage());
		}
	}

	public void setNPasses(int nPasses) {
	}

	public void showAbout() {
		IJ.showMessage("About TRSI Pattern Recognition Plugin",
		"A Plugin for Translation, Rotation, and Scale Invariant Object/Character Recognition using Modified Ring Projection\n" +
		"By: Gholam Reza Kaka and Kaiser Niknam\n" +
		"At: Research Center of Neuroscience, Baqiyatallah Medical Sciences University, Tehran, Iran");
	}

    public void FloodFill(int x, int y, int w, int h, int label) {
		Stack<Node> s = new Stack<Node>();
		s.push(new Node(x,y));
		while (!s.isEmpty()) {
			Node n = s.pop();
			if ((n.x>=0) && (n.x<w) && (n.y>=0) && (n.y<h) && data[n.x][n.y]==1) {
				data[n.x][n.y]=label;
				s.push(new Node(n.x+1,n.y));
				s.push(new Node(n.x,n.y+1));
				s.push(new Node(n.x,n.y-1));
				s.push(new Node(n.x-1,n.y));
			}
		}
	}
	
	float[] modified_ring(int m, int xmin, int ymin, int xmax, int ymax, ImageProcessor ip0) {
		int S=0, xCenterOfMass=0, yCenterOfMass=0;
		float mdr[] = new float[N];
		try {
			for(int i=xmin; i<=xmax; i++) {
				for(int j=ymin; j<=ymax; j++) {
					if(data[i][j]==m) {
						xCenterOfMass += i;
						yCenterOfMass += j;
						S++ ;
					}
				}
			}
			xCenterOfMass = (int)Math.round((float)xCenterOfMass/(float)S) - xmin;
			yCenterOfMass = (int)Math.round((float)yCenterOfMass/(float)S) - ymin;
			
			ip0.setRoi(xmin, ymin, (xmax-xmin)+1, (ymax-ymin)+1); ip0 = ip0.crop(); ImagePlus imp0 = new ImagePlus("cropped", ip0);
			
			for(int index=0; index<N; index++) {
				int r = (int)Math.floor((double)rho[index] * Math.sqrt(S));
				ImagePlus imp1 = NewImage.createByteImage("circle", ip0.getWidth(), ip0.getHeight(), 1, NewImage.FILL_BLACK);
				ImageProcessor ip1 = imp1.getProcessor(); ip1.setColor(Color.white); ip1.setLineWidth(1); ip1.drawOval(xCenterOfMass-r, yCenterOfMass-r, 2*r, 2*r);
				ImageStatistics stats_T = imp1.getStatistics(Measurements.AREA_FRACTION); double area_T = stats_T.areaFraction;
				ip1.copyBits(ip0, 0, 0, Blitter.MIN);
				ImageStatistics stats_F = imp1.getStatistics(Measurements.AREA_FRACTION); double area_F = stats_F.areaFraction;
				if(area_T==0) mdr[index] = 0; else mdr[index] = (float)(area_F/area_T);
			}
		}
		catch(Exception e) {
			IJ.error("Pattern Recognition Error", "Where is the pattern? it seems an empty page only!\n" + e.getMessage());
		}
		return mdr;
	}

    int Otsu(ImageProcessor ip) {
		int hist[] = ip.getHistogram();
		double BCV=0, BCVmax=0, num, denom;
		int k, kStar=0, N=0, Nk, S=0, Sk=0, L=256;
		for (k=0; k<L; k++) {
			S += k * hist[k];
			N += hist[k];
		}
		Nk = hist[0];
		for (k=1; k<L-1; k++) {
			Sk += k * hist[k];
			Nk += hist[k];
			denom = (double)(Nk) * (N - Nk);
			if(denom != 0 ) {
				num = ((double)Nk/N) * S - Sk;
				BCV = (num * num)/denom;
			}
			else {
				BCV = 0;
			}
			if(BCV >= BCVmax) {
				BCVmax = BCV;
				kStar = k;
			}
		}
		return kStar;
	}

	class Node {
		int x, y;
		Node (int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
}