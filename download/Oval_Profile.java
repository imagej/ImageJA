import java.awt.*;
import java.text.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;
import ij.text.*;
import ij.measure.*;

/*
 *  Oval_Profile  takes the image region bounded by an oval region and samples the oval
 *  at N equal angles around the oval. The program generates a ProfilePlot of either:
  *    0) maximum intensity values along N radii from the oval center to oval points.
 *      This measure is called the circumferential profile and is used in Nuclear Medicine
 *      cardiac blood perfusion studies, in which short axis slices of the left
 *      ventricle are roughly doughnut shaped.
 *    1) Sum of pixel intensities along each radius, from the center to the oval
 *       boundary.
 *    2) Sum of intensities along each radius.
 *    3) Pixel intensities at equiangular sample points along the oval profile.
 *    4) Pixel intensities at equidistant sample points along the oval profile.
*/

public class Oval_Profile implements PlugInFilter {
	protected ImagePlus imp;
	//  protected double width, height;
	protected static int npoints = 30;
	private static String[] modes = {"Maximum Intensity", "Radial Sums",
                                          "Along Oval", "EquiCircumference"};
	public static int CIRCUMFERENTIAL = 0;
	protected static int mode = CIRCUMFERENTIAL;
	protected static boolean hotspots = true;
        protected int imType = 0;
        protected int fgColor = 255;
        protected double circumference;
	public int setup(String args, ImagePlus imp) {
		if(args.equals("about")) {
			showAbout();
			return DONE;
		}
		if (IJ.versionLessThan("1.27e"))
			return DONE;
		if(imp != null) {
			this.imp = imp;
		}
		return DOES_8G+DOES_16;
	}

	public void run(ImageProcessor ip) {
		Roi roi = imp.getRoi();
		if (roi==null || roi.getType()!=Roi.OVAL) {
			IJ.error("Oval selection required.");
			return;
		}
		if (!getParameters())
			return;
                imType = imp.getType();
                if(imType == ImagePlus.GRAY16) fgColor = 65535;
                else fgColor = 255;
                OvalProfilePlot p = new OvalProfilePlot(imp, npoints, mode,
                modes, hotspots, fgColor);
                Calibration oldCal = imp.getCalibration();
                Calibration cal = oldCal.copy();
                if(mode == 3)
                  cal.setUnit("Distance");
                else
                  cal.setUnit("Degrees");
                cal.pixelWidth = 360.0 / npoints;
                cal.pixelHeight = 360.0 / npoints;
                imp.setCalibration(cal);
//		OvalProfilePlot p = new OvalProfilePlot(imp, npoints, mode, hotspots, fgColor);
//               p.createWindow();
                p.createPlot();
                p.createWindow();
                imp.setCalibration(oldCal);
	}

	boolean getParameters() {
		GenericDialog param = new GenericDialog("Circumferential Profile", IJ.getInstance());
		param.addNumericField("Number of Points", npoints, 0);
		param.addChoice("Analysis mode", modes, modes[mode]);
		param.addCheckbox("Show Hotspots", hotspots);
		param.showDialog();
		if(!param.wasCanceled()) {
			npoints = (int)param.getNextNumber();
			mode = param.getNextChoiceIndex();
			hotspots = param.getNextBoolean();
			return true;
		} else
			return false;
	}

	void showAbout() {
        IJ.showMessage(
 "Oval_Profile  takes the image region bounded by an oval region and samples the oval" +
 "at N equal angles around the oval. The program generates a ProfilePlot of either:" +
 "    0) maximum intensity values along N radii from the oval center to oval points."  +
 "      This measure is called the circumferential profile and is used in Nuclear Medicine" +
 "      cardiac blood perfusion studies, in which short axis slices of the left" +
 "      ventricle are roughly doughnut shaped." +
 "    1) Sum of pixel intensities along each radius, from the center to the oval" +
 "      boundary. " +
 "    2) pixel intensities at equiangular points along the oval." +
 "    3) pixel intensities at equidistant points along the oval."                      );
	}

}


class OvalProfilePlot {
	int npoints = 20;
	int mode = 0;  // Circ. profile, radial sums, or oval points
        int TESTRADII = 2048;
        protected static int EQUICIRCUMFERENCE = 3;
        double[] profile = null;
        double[] xValues = null;
        String[] modes = null;
        boolean hotspots = false; // Show graphed points in image
        ImagePlus imp;
	ImageProcessor ip2;
        int fgColor = 255;

/*
	public OvalProfilePlot(ImagePlus imp, int npoints) {
		// assume CIRCUMFERENTIAL PROFILE and HOTSPOTS are false.
		this(imp, npoints, 0);
        }
*/
	public OvalProfilePlot(ImagePlus imp, int npoints, int mode, String[] modes) {
		// assume HOTSPOTS is false.
		this(imp, npoints, mode, modes, false, 255);
	}

//	public OvalProfilePlot(ImagePlus imp, int npoints, boolean circ, boolean hotspots) {
	public OvalProfilePlot(ImagePlus imp, int npoints, int mode, String[] modes,
               boolean hotspots, int fgColor) {
		super();
		this.npoints = npoints;
		this.imp = imp;
		this.mode = mode;
                this.modes = modes;
		this.hotspots = hotspots;
                this.fgColor = fgColor;
                xValues = new double[npoints];
                profile = new double[npoints];
		Roi roi = imp.getRoi();
		if (roi==null) {
			IJ.error("Selection required.");
			return;
		}
		int roiType = roi.getType();
		if(roiType != Roi.OVAL) {
			IJ.error("Oval selection required.");
			return;
		}
		// RoiType is Oval
		imp.getProcessor().setValue((double)fgColor);
/*
                  Calibration cal = imp.getCalibration();
		pixelSize = cal.pixelWidth;
		units = cal.getUnits();
		yLabel = cal.getValueUnit();
*/
              ImageProcessor ip = imp.getProcessor();
		if (hotspots) {
			ip2 = ip.duplicate();
			ip2.setValue((double)fgColor);
			ip2.snapshot();
		}
//		ip.setCalibrationTable(cal.getCTable());
		profile = getOvalProfile(roi, ip);

		ip.setCalibrationTable(null);
		ImageWindow win = imp.getWindow();
/*
                  if (win!=null)
			magnification = win.getCanvas().getMagnification();
		else
			magnification = 1.0;
*/
	}

	double[] getOvalProfile(Roi roi, ImageProcessor ip) {
		Rectangle b = roi.getBounds();
		double width = b.width;
		double height = b.height;
                double w2 = width*width / 4.0;
                double h2 = height*height / 4.0;
		// get radii from oval center
		double cx = b.x+width/2.0 + .5;
		double cy = b.y+height/2.0 + .5;
		// double theta0 = -Math.PI/2;
		double theta0 = 0;
		double tink = 2*Math.PI / npoints;

                double circumInc = 0,
                       circumSum = 0;
                       int radius = 0;
                double[] manyRadii = null;
                EllipsePoint e = null;

                if(mode == EQUICIRCUMFERENCE)
                {
                  e = new EllipsePoint(b, TESTRADII, 0);
                  manyRadii = getCircumference(e);
                  IJ.log("circumference=" + manyRadii[TESTRADII]);
                  circumInc = manyRadii[TESTRADII] / npoints;
                }
		int i = 0;
                double theta = theta0;
             	for(; (theta < theta0+2*Math.PI) && (i<npoints); theta += tink) {
			double dx = Math.cos(theta);
			double dy = Math.sin(theta);
			double x = cx;
			double y = cy;
                        double sum = 0;

			double hotval = Double.MIN_VALUE;
			double hotx=0, hoty=0;
			while(roi.contains((int)x, (int)y)) {
				double val = ip.getInterpolatedPixel(x, y);
				switch(mode)
                                {
                                case 0:
                                        if((val > hotval)) { // Max intensity
					hotval = val;
					hotx = x;
					hoty = y;
				        }
                                        break;
                                case 1:
                                        sum += val; // radial sums
 			                if(hotspots)ip2.drawPixel((int)x, (int)y);
                                        break;
/*
                                case 2:
                                        hotval = val; // oval points
					hotx = x;
					hoty = y;
                                        break;
*/
                                }
				x += dx;
				y += dy;
			}
//			if(mode == 1) profile[i] = sum;
//                        else          profile[i] = hotval;
                        switch(mode)
                        {
                        case 0: profile[i] = hotval;
                                break;
                        case 1: profile[i] = sum;
                                break;
                        case 2: double m = Math.sqrt(w2*h2 / (dx*dx*h2 + dy*dy*w2));
                                hotx = cx + dx*m;  hoty = cy + dy*m;
                                profile[i] = ip.getInterpolatedPixel(hotx, hoty);
                                break;
                        case 3:  // equiCircumferential
                                radius = getNextCircumPoint(e, circumSum, manyRadii, radius);
                                e.init(b, TESTRADII, radius);
                                hotx = e.getX(); hoty = e.getY();
                                profile[i] = ip.getInterpolatedPixel(hotx, hoty);

                                break;
                        }
			if(hotspots)ip2.drawPixel((int)hotx, (int)hoty);
                        if(mode == 3)
                         { xValues[i] = circumSum;
                           circumSum += circumInc;
                         }
                        else xValues[i] = Math.toDegrees(theta);
			i++;
		}

		return profile;
	}

        double[] getCircumference(EllipsePoint e)
        {
           double[] distances = new double[TESTRADII + 1];

           for(int i=0; i<TESTRADII; i++)
           {
               distances[i] = distance(e, i);
           }
 //          distances[TESTRADII] = distance(e, TESTRADII);
           // Sum the distances
           for(int i= 1; i<=TESTRADII; i++) distances[i] += distances[i-1];
           return distances;
        }

        double distance(EllipsePoint e, int i)
        {
         e.init(e.getBoundingRect(), e.getNRadii(), i);
         double x1 = e.getX();
         double y1 = e.getY();

         int nextRadius;
         if(i<TESTRADII) nextRadius = i+1;
         else nextRadius = 0;
         e.init(e.getBoundingRect(), e.getNRadii(), nextRadius);

         double x2 = e.getX();
         double y2 = e.getY();;
         double diffx = x2 - x1;
         double diffy = y2 - y1;
         return Math.sqrt(diffx*diffx + diffy*diffy);
       }

       int getNextCircumPoint(EllipsePoint e, double circumSum, double[] radii, int radius)
       {
         int i = 0;
         for(i=0; radii[i] <= circumSum; i++) {};
//         IJ.write("i=" + i + " sum=" + circumSum);
         if(i==0) return 0;
         if((radii[i] - circumSum) <= (circumSum - radii[i-1]))
           return i;
         else return i-1;
       }


           public void createWindow() {
//		super.createWindow();
		if (hotspots && ip2!=null) {
			ImagePlus imp2 = new ImagePlus("Hotspots of "+imp.getShortTitle(), ip2);
//			imp2.setRoi((Roi)imp.getRoi().clone());
			imp2.show();
		}
	}

        void createPlot()
        {
          String xLabel = "";
          if(mode == 3) xLabel = "Circumference";
          else xLabel = "Degrees";
          Plot plot = new Plot(modes[mode], xLabel, "Gray Value", xValues, profile);
          plot.setLimits(getMin(xValues), getMax(xValues), getMin(profile), getMax(profile));
          plot.setColor(Color.black);
          plot.show();
        }

        double getMax(double[] v)
        {
          double max = Double.MIN_VALUE;
          for(int i=0; i < v.length; i++)
            if(v[i] > max) max = v[i];
          return max;
        }

        double getMin(double[] v)
        {
          double min = Double.MAX_VALUE;
          for(int i=0; i < v.length; i++)
            if(v[i] < min) min = v[i];
          return min;
        }

        class EllipsePoint
        {
          int nRadii, radius;
          double width, height, w2, h2, cx, cy, dx, dy, m, x, y;
          double theta = 0;
          Rectangle b;

          EllipsePoint() {};
          EllipsePoint(Rectangle b, int nRadii, int radius)
          {
            init(b, nRadii, radius);
          }

          void init(Rectangle b, int nRadii, int radius) {
            this.b = b;
            double width = b.width;
            height = b.height;
            w2 = width * width / 4.0;
            h2 = height * height / 4.0;
      // get radii from oval center
            cx = b.x + width / 2.0 + .5;
            cy = b.y + height / 2.0 + .5;
            this.nRadii = nRadii;
            this.radius = radius;
            theta = 2 * Math.PI * radius / nRadii;
            dx = Math.cos(theta);
            dy = Math.sin(theta);

            w2 = b.width * b.width / 4;
            h2 = b.height * b.height / 4;
            m = Math.sqrt(w2 * h2 / (dx * dx * h2 + dy * dy * w2));
            //hotx = cx + dx*m;  hoty = cy + dy*m;
            x = cx + m * dx;
            y = cy + m * dy;
          }

          double getX() {return x;}
          double getY() {return y;}
          double getAngle() {return theta;}
          Rectangle getBoundingRect() {return b;}
          int getNRadii() {return nRadii;}
        }
}


