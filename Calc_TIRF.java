import ij.*;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.gui.*;
import java.awt.*;

/*Sebastian Rhode
Plugin calculates the penetration depth for TIRF microscopy for 3 laser lines.
Additionally it calculates the required incident angles for a desired penetration
depth and displays the evanescent field intensity.

Version 1.2	2008-04-29
*/

public class Calc_TIRF implements PlugIn {

    public void run(String arg) {
            if (IJ.versionLessThan("1.39t"))
                    return;

            /*
            n1	-->	refractive index of immersion fluid, usually immersion oil = 1.518
            n2	-->	refractive index of medium, usually water-like = 1.330
                            for the cytosol a value of n2 = 1.360 should be used
            NA	-->	numerical aperture of TIRF objective lens

            ---------------------------------------------------------------------------------------

            Equation: d= wl /(4*pi) * 1/sqrt(n1^2*sin(iAngle*pi/180)*sin(iAngle*pi/180)-n2^2);														
             */

            double wl1	=  491;		// laser wavelength 1 in [nm]
            double wl2	=  561;		// laser wavelength 2 in [nm]
            double wl3 	=  647;		// laser wavelength 3 in [nm]
            double n1	=  1.518;	// refractive index of immersion fluid
            double n2	=  1.330;	// refractive index of medium
            double NA	=  1.45;   	// numerical aperture of TIRF objective lens
            double pd	=  150;   	// desired penetration depth in [nm]

            GenericDialog gd = new GenericDialog("Specify Parameters for TIRF", IJ.getInstance());
            gd.addMessage("Laser Wavelengths [nm]");
            gd.addNumericField("LaserLine 1", wl1, 0);
            gd.addNumericField("LaserLine 2", wl2, 0);
            gd.addNumericField("LaserLine 3", wl3, 0);
            gd.addMessage("-------------------------------------");
            gd.addNumericField("refractive index of immersion oil", n1, 3);
            gd.addNumericField("refractive index of medium", n2, 3);
            gd.addNumericField("Numerical Aperture of TIRF objective", NA, 2);
            gd.addMessage("-------------------------------------");
            gd.addNumericField("Desired Penetration Depth [nm]", pd, 0);
            gd.showDialog();
            if (gd.wasCanceled())
                    return;

            wl1		= gd.getNextNumber();
            wl2		= gd.getNextNumber();
            wl3		= gd.getNextNumber();
            n1		= gd.getNextNumber();
            n2		= gd.getNextNumber();
            NA		= gd.getNextNumber();
            pd		= gd.getNextNumber();

            double maxAngle = Math.ceil(Math.asin(NA/n1)* 180/Math.PI * 10);
            double minAngle = Math.ceil(Math.asin(n2/n1)* 180/Math.PI * 10);
            double maxRange = 800;
            double[] iAngle = new double [(int) (maxRange-minAngle+1)]; 
            for (int i = 0; i < (maxRange-minAngle+1); i++) {
                    iAngle[i] = (minAngle +i) * 0.1;
            }

            double [] d1 = new double [(int) iAngle.length];
            double [] d2 = new double [(int) iAngle.length];
            double [] d3 = new double [(int) iAngle.length];
            for (int i = 0; i < iAngle.length; i++) {
                    d1[i]= wl1 /(4*Math.PI)*1/Math.sqrt(n1*n1*Math.sin(iAngle[i]*Math.PI/180)*Math.sin(iAngle[i]*Math.PI/180)-n2*n2);
                    d2[i]= wl2 /(4*Math.PI)*1/Math.sqrt(n1*n1*Math.sin(iAngle[i]*Math.PI/180)*Math.sin(iAngle[i]*Math.PI/180)-n2*n2);
                    d3[i]= wl3 /(4*Math.PI)*1/Math.sqrt(n1*n1*Math.sin(iAngle[i]*Math.PI/180)*Math.sin(iAngle[i]*Math.PI/180)-n2*n2);
            }
            // TIR working range of objective in [grad]*10
            double[] wr0 = {minAngle/10, minAngle/10}; // minimum incident angle
            double[] wr1 = {maxAngle/10, maxAngle/10}; // maximum incident angle
            double[] wr2 = {0, 400};

            PlotWindow plot1 = new PlotWindow("TIRF Calculation","Incident Angle [grad]","Penetration depth [nm]",iAngle,d1);
            //---------------------------------------------------------
            plot1.setLimits(60, (maxRange/10), 0, 300);
            plot1.setColor(Color.black);
            plot1.addLabel(0.27,0.4,"maxAngle : "+IJ.d2s(maxAngle/10,2));
            plot1.addLabel(0.27,0.5,"critical Angle : "+IJ.d2s(minAngle/10,2));
            plot1.addLabel(0.7,0.1,"n1 = "+IJ.d2s(n1,2));
            plot1.addLabel(0.7,0.2,"n2 = "+IJ.d2s(n2,2));
            plot1.addLabel(0.7,0.3,"NA = "+IJ.d2s(NA,2));
            plot1.addPoints(wr0,wr2,PlotWindow.LINE);
            plot1.addPoints(wr1,wr2,PlotWindow.LINE);
            //---------------------------------------------------------
            plot1.setColor(Color.green);
            plot1.addLabel(0.27,0.2,"d2 @"+IJ.d2s(wl2,2));        
            plot1.addPoints(iAngle,d2,PlotWindow.LINE);
            //---------------------------------------------------------
            plot1.setColor(Color.red);
            plot1.addLabel(0.27,0.3,"d3 @"+IJ.d2s(wl3,2));        
            plot1.addPoints(iAngle,d3,PlotWindow.LINE);
            //---------------------------------------------------------
            plot1.setColor(Color.blue);        
            plot1.addLabel(0.27,0.1,"d1 @"+IJ.d2s(wl1,2));  
            //---------------------------------------------------------
            plot1.draw();
            
            double []z	=  new double [750]; // initialization of z range --> 0-750nm
            for (int i = 0; i < 750; i++) {
                    z[i] = i;
            }
            double [] I = new double [(int) z.length]; // calculation of intensity curve for pd
            for (int i = 0; i < I.length; i++) {
                    I[i] = 100 * Math.exp(-z[i]/pd);
            }
            
            PlotWindow plot2 = new PlotWindow("Evanescent Field Intensity","z-position [nm]","I / I(0) [%]",z,I);
            //---------------------------------------------------------
            plot2.setLimits(0, 750, 0, 100);
            plot2.setColor(Color.red);
            //plot2.addLabel(0.7,0.7,"Intensity I");
            plot2.draw();
            
            double [] wl = {405, 442, 473, 491, 514, 532, 561, 633, 647, 660};
            double []angle = new double [(int) wl.length];
            for (int i =0; i < angle.length; i++) {
                    angle[i] = Math.asin(Math.sqrt( (wl[i]/(4*Math.PI*pd*n1))*(wl[i]/(4*Math.PI*pd*n1))+(n2*n2/n1/n1)))*180/Math.PI;
                    angle[i] = Math.ceil(angle[i]*100)/100;
            }
            ResultsTable rt = new ResultsTable();
            rt.reset();
            for (int n=0; n <  wl.length; n ++) {
                    rt.incrementCounter();
                    rt.addValue("WL [nm]", wl[n]);
                    rt.addValue("angle [grad]", angle[n]);
            }
            rt.show("Required Angles for PD ="+ pd+"nm");
            
    }
}
