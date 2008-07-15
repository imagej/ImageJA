import ij.plugin.filter.PlugInFilter;
import java.awt.Color;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.Analyzer;
import ij.measure.*;

/**
	Uses ImageJ's particle analyzer to track the movement of
	multiple objects through a stack. 
	Based on the Object Tracker plugin filter by Wayne Rasband
*/
public class MultiTracker_ implements PlugInFilter, Measurements  {

	ImagePlus	imp;
	int		nParticles;
	float[][]	ssx;
	float[][]	ssy;

	static int	minSize = 1;
	static int	maxSize = 999999;
	static boolean bShowLabels = false;
	static boolean bShowPositions = false;
	static boolean bShowPaths = false;
	static boolean bShowPathLengths = false;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if (IJ.versionLessThan("1.17y"))
			return DONE;
		else
			return DOES_8G+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Object Tracker");
		gd.addNumericField("Minimum Object Size (pixels): ", minSize, 0);
		gd.addNumericField("Maximum Object Size (pixels): ", maxSize, 0);
		gd.addCheckbox("Show Labels", bShowLabels);
		gd.addCheckbox("Show Positions", bShowPositions);
		gd.addCheckbox("Show Paths", bShowPaths);
		gd.addCheckbox("Display Path Lengths", bShowPathLengths);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		minSize = (int)gd.getNextNumber();
		maxSize = (int)gd.getNextNumber();
		bShowLabels = gd.getNextBoolean();
		bShowPositions = gd.getNextBoolean();
		bShowPaths = gd.getNextBoolean();
		bShowPathLengths = gd.getNextBoolean();
		if (bShowPositions)
			bShowLabels =true;
		track(imp, minSize, maxSize);
	}
	
	public void track(ImagePlus imp, int minSize, int maxSize) {
		int nFrames = imp.getStackSize();
		if (nFrames<2) {
			IJ.showMessage("Tracker", "Stack required");
			return;
		}

		// See if the particles have been previously numbered
		// by surrounding them with a ROI and selecting "Measure"
		// in the desired order
		Analyzer an = new Analyzer();
		ResultsTable srt = an.getResultsTable();
		int nInitialCount = srt.getCounter();
		float[] sxInitial = srt.getColumn(ResultsTable.X_CENTROID);
		float[] syInitial = srt.getColumn(ResultsTable.Y_CENTROID);
		
		ImageStack stack = imp.getStack();
		int options = 0; // set all PA options false
		int measurements = CENTROID;

		// Get the number of particles in the first frame
		ResultsTable rt = new ResultsTable();
		rt.reset();
		ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize);
		pa.analyze(imp, stack.getProcessor(1));
		nParticles = rt.getCounter();
		
		if (nInitialCount == nParticles) {
			// check to make sure that there are exactly nParticles entries
			if (sxInitial != null && syInitial != null) {
				String str = "Sorting order found:\n";
				for (int i=0; i<nInitialCount; i++) {
					str += ""+ (i+1) +"=("+ (int)sxInitial[i] +","+ (int)syInitial[i] +")\n";
				}
				IJ.showMessage("MultiTracker", str);
				
				// reset the system measurements
				srt.reset();
			}
		} else {
			sxInitial = null;
			syInitial = null;
		}
		

		// Create the column headings based on the number of particles
		String strHeadings = "Frame";
		for (int i=1; i<=nParticles; i++) {
			strHeadings += "\tX" + i + "\tY" + i;
		}
		IJ.setColumnHeadings(strHeadings);

		// create storage for particle positions
		ssx = new float[nFrames][nParticles];
		ssy = new float[nFrames][nParticles];

		// now go through each frame and find the particle positions
		float[] sxOld = new float [nParticles];
		float[] syOld = new float [nParticles];
		float[] sx = new float [nParticles];
		float[] sy = new float [nParticles];
		float[] sxSorted = new float [nParticles];
		float[] sySorted = new float [nParticles];

		for (int iFrame=1; iFrame<=nFrames; iFrame++) {
			rt.reset();
			pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize);
			pa.analyze(imp, stack.getProcessor(iFrame));
			float[] sxRes = rt.getColumn(ResultsTable.X_CENTROID);				
			float[] syRes = rt.getColumn(ResultsTable.Y_CENTROID);
			if (sxRes==null)
				return;

			int iCount = sxRes.length;
			for (int iPart=0; iPart<nParticles; iPart++) {
				if (iPart < iCount) {
					sx[iPart] = sxRes[iPart];
					sy[iPart] = syRes[iPart];
				} else {
					sx[iPart] = 0;
					sy[iPart] = 0;
				}
			}
			int iIndex = (iFrame-1);
			if ((iIndex == 0) && (sxInitial != null) && (syInitial != null)) {
				sort(sxInitial, syInitial, sx, sy, sxSorted, sySorted, nParticles);
			} else if (iIndex > 0) {
				for (int i=0; i<nParticles; i++) {
					sxOld[i] = ssx[iIndex-1][i];
					syOld[i] = ssy[iIndex-1][i];
				}
				sort(sxOld, syOld, sx, sy, sxSorted, sySorted, nParticles);
			} else {
				for (int i=0; i<nParticles; i++) {
					sxSorted[i] = sx[i];
					sySorted[i] = sy[i];
				}
			}

			for (int iPart=0; iPart<nParticles; iPart++) {
				ssx[iIndex][iPart] = sxSorted[iPart];
				ssy[iIndex][iPart] = sySorted[iPart];
			}

			String strLine = "" + iFrame;
			for (int iPart=0; iPart<nParticles; iPart++) {
				strLine += "\t" + sxSorted[iPart] + "\t" + sySorted[iPart];
			}
			IJ.write(strLine);
			IJ.showProgress((double)iFrame/nFrames);
		}

		if (bShowLabels) {
			String strPart;
			int iIndex, iX, iY;
			for (int iFrame=1; iFrame<=nFrames; iFrame++) {
				iIndex = iFrame - 1;
				ImageProcessor ip = stack.getProcessor(iFrame);
				ip.setColor(Color.black);
				for (int i=0; i<nParticles; i++) {
					strPart = "" + (i+1);
					iX = (int)ssx[iIndex][i];
					iY = (int)ssy[iIndex][i];
					if (bShowPositions) {
						strPart += "=" + iX + "," + iY;
					}
					ip.moveTo(iX - 5, iY - 5);
					ip.drawString(strPart);
				}
				IJ.showProgress((double)iFrame/nFrames);
			}
			imp.updateAndDraw();
		}
		
		if (bShowPathLengths) {
			double[] lengths = new double[nParticles];
			int iIndex;
			double x1, y1, x2, y2;
			for (int iFrame=2; iFrame<=nFrames; iFrame++) {
				iIndex = iFrame - 1;
				for (int i=0; i<nParticles; i++) {
					x1 = ssx[iIndex-1][i];
					y1 = ssy[iIndex-1][i];
					x2 = ssx[iIndex][i];
					y2 = ssy[iIndex][i];
					lengths[i] += Math.sqrt(sqr(x2-x1)+sqr(y2-y1));
				}
			}
			IJ.write("");
			String str = "length";
			for (int i=0; i<nParticles; i++) {
				str += "\t" + (float)lengths[i] + "\t ";
			}
			IJ.write(str);
		}

		if (bShowPaths) {
			if (imp.getCalibration().scaled()) {
				IJ.showMessage("MultiTracker", "Cannot display paths if image is spatially calibrated");
				return;
			}
			ImageProcessor ip = new ByteProcessor(imp.getWidth(), imp.getHeight());
			ip.setColor(Color.white);
			ip.fill();
			int iIndex, iX1, iY1, iX2, iY2, color;
			for (int iFrame=2; iFrame<=nFrames; iFrame++) {
				iIndex = iFrame - 1;
				for (int i=0; i<nParticles; i++) {
					iX1 = (int)ssx[iIndex-1][i];
					iY1 = (int)ssy[iIndex-1][i];
					iX2 = (int)ssx[iIndex][i];
					iY2 = (int)ssy[iIndex][i];
					color =Math.min(i+1,254);
					ip.setValue(color);
					ip.moveTo(iX1, iY1);
					ip.lineTo(iX2, iY2);
				}
			}
			new ImagePlus("Paths", ip).show();
		}

	}

	double sqr(double n) {return n*n;}
	
	void sort(float[] sxOld, float[] syOld, float[] sxSrc, float[] sySrc, float[] sxDest, float[] syDest, int nLength) {
		double dDist, dMinDist;
		int iMinIndex = 0;

		for (int i=0; i<nLength; i++) {
			dMinDist = -1.0;
			// determine which new point is closest to the old point
			for (int j=0; j<nLength; j++) {
				// calculate the distance from the j'th new point to the i'th old point
				dDist = Math.sqrt(sqr(sxSrc[j] - sxOld[i]) + sqr(sySrc[j] - syOld[i]));
				if ((dMinDist < 0) || (dDist < dMinDist)) {
					// this point is the closest so far
					dMinDist = dDist;
					iMinIndex = j;
				}
			}
			// now copy this point to the destination
			sxDest[i] = sxSrc[iMinIndex];
			syDest[i] = sySrc[iMinIndex];
		}
	}

 	double s2d(String s) {
		Double d;
		try {d = new Double(s);}
		catch (NumberFormatException e) {d = null;}
		if (d!=null)
			return(d.doubleValue());
		else
			return(0.0);
	}

}


