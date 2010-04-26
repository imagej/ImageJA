//  Diameter plugin, version 1.0, 2010_04_23
//  Michael J.M. Fischer

import ij.plugin.frame.*;
import java.awt.datatransfer.*;
import ij.plugin.PlugIn;
import java.awt.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

public class Diameter_ implements PlugIn {

    public void run(String arg) {
        double diff;
        double dxmin;
        double dxmax;
        double left;
        double right;
        int fall;
        int rise;
        double intensity1;
        double intensity2;
        double intensity3;
        double magnification;
        double xInc;
        int dx;
        int dy;
        double[] xValues;

        ImagePlus img = IJ.getImage();
        Roi roi = img.getRoi();
        if (roi==null || !roi.isLine()) {
            IJ.error("Line selection required.");
            return;
        }
		Calibration cal = img.getCalibration(); 
		ImageProcessor ip = img.getProcessor();
		ip.setCalibrationTable(cal.getCTable());
		ip.setInterpolate(true);
		
		// Transfer of the user-chosen line to profile
        Line line = (Line)roi;
        double[] profile = line.getPixels();
        
        if (!(cal==null) && !(cal.pixelWidth==cal.pixelHeight)) {
            double d_x = cal.pixelWidth*(line.x2 - line.x1);
            double d_y = cal.pixelHeight*(line.y2 - line.y1);
            double length = Math.round(Math.sqrt(d_x*d_x + d_y*d_y)); 
        }

        ImageStack stack = img.getStack();
        int size = stack.getSize(); 
        float[][] vesselDiameter = new float[10][size+1];

        String buffer = "Diam1\tDiam2\tDiam3\tDiam4\tDiam5\t\tStart1\tEnd1\tStart2\tEnd2\tStart3\tEnd3\tStart4\tEnd4\tStart5\tEnd5\n";

        // Shift line selection by one pixel orthogonal to the selection
        // y shift if abs(dx)/(abs(dx)+abs(dy)) > 0.293, x shift if abs(dy)/(abs(dx)+abs(dy)) > 0.293
        dy = (int)(Math.signum(line.x2 - line.x1)*Math.round(1.7*Math.abs(line.x2 - line.x1)/(Math.abs(line.x2 - line.x1)+Math.abs(line.y2 - line.y1))));
        dx = (int)(-1*Math.signum(line.y2 - line.y1)*Math.round(1.7*Math.abs(line.y2 - line.y1)/(Math.abs(line.x2 - line.x1)+Math.abs(line.y2 - line.y1))));

        // Loop for 5 repetitive measurements
        for(int no_of_measure = -2; no_of_measure <=2; no_of_measure++)
        {
            // Parallel shifted line selctions
			img.setRoi(new Line(line.x1+dx*no_of_measure, line.y1+dy*no_of_measure, line.x2+dx*no_of_measure, line.y2+dy*no_of_measure));
            Roi roi2 = img.getRoi();

			// Loop to go throuhg the image stack
			for(int mySliceNumber = 1; mySliceNumber <= size; mySliceNumber++) {
				// Get Profile from selected Stack image
				img.setSlice(mySliceNumber);
				ip.setRoi(roi2);
				Line line2 = (Line)roi2;
				profile = line2.getPixels();

				// Find maximum inentisy change, uses the average of three adjacent datapoints
				fall = 0;
				rise = 0;
				dxmin = 0;
				dxmax = 0;
				for (int i = 0; i <= profile.length-6; i++) {
					diff = profile[i]+profile[i+1]+profile[i+2] - profile[i+3]-profile[i+4]-profile[i+5];
					if (diff > dxmin) {
						dxmin = diff;
						fall = i + 2;
					}
					if (diff < dxmax) {
						dxmax = diff;
						rise = i + 2;
					}
				}
				
				// Calculate reference intensities, 1 before, 2 within and 3 after the vessel
				intensity1 = profile[0];
				for (int i = 1; i <= profile.length/2 ; i++) {
					if (((profile[i-1]+profile[i]+profile[i+1])/3) > intensity1) { intensity1 = ((profile[i-1]+profile[i]+profile[i+1])/3); }
				}
				intensity2 = profile[fall];
				for (int i = (int)Math.floor(profile.length*0.25); i <= Math.floor(profile.length*0.75); i++) {
					if (((profile[i-1]+profile[i]+profile[i+1])/3) < intensity2) { intensity2 = ((profile[i-1]+profile[i]+profile[i+1])/3); }
				}
				intensity3 = profile[rise+1];
				for (int i = (int)Math.floor(profile.length/2); i <= profile.length-2; i++) {
					if (((profile[i-1]+profile[i]+profile[i+1])/3) > intensity3) {intensity3 = ((profile[i-1]+profile[i]+profile[i+1])/3); }
				}

				// Intersection of Linear Trend and Threshold, threshold is the average of intensity 1&2 or 2&3
				left  =  0.5 * ( profile[fall-1] + profile[fall] );
				right = 0.5 * ( profile[fall+1] + profile[fall+2] );
				vesselDiameter[no_of_measure*2+4][mySliceNumber] = (float)(((fall - 0.5) + 2*( (float)((left-(intensity1+intensity2)/2)/(left-right))))*1.1547) ;// 1.1547=sqrt(4/3)

				left  =  0.5 * ( profile[rise-1] + profile[rise] );
				right = 0.5 * ( profile[rise+1] + profile[rise+2] );
				vesselDiameter[no_of_measure*2+5][mySliceNumber] = (float)((rise - 0.5 + 2*( ((left-(intensity2+intensity3)/2)/(left-right))))*1.1547) ;// 1.1547=sqrt(4/3)
			}    
        }
        // Resets the original selection
		img.setRoi(roi);

		// Generates a string from the results
        for(int mySliceNumber = 1; mySliceNumber <= size; mySliceNumber++) {
			for(int no_of_measure = -2; no_of_measure <=2; no_of_measure++) {
                buffer += vesselDiameter[no_of_measure*2+5][mySliceNumber]-vesselDiameter[no_of_measure*2+4][mySliceNumber];
                buffer += "\t";
			}
			buffer += "\t";
			for(int no_of_measure = -2; no_of_measure <=2; no_of_measure++) {			
				buffer += vesselDiameter[no_of_measure*2+4][mySliceNumber];
                buffer += "\t";
                buffer += vesselDiameter[no_of_measure*2+5][mySliceNumber];
				if (no_of_measure < 2) {
                    buffer += "\t";
                }
            }
            buffer += "\n";
        }

		// Copies string to system clipboard
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection cont = new StringSelection(buffer);
        systemClipboard.setContents(cont, cont);
    }
}

