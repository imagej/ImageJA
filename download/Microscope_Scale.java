import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import ij.measure.Calibration;
import java.awt.*;
import ij.plugin.*;

public class Microscope_Scale implements  PlugInFilter {

/* DESCRIPTION & CUSTOMIZATION INSTRUCTIONS
        This plugin lets you calibrate images spatially using hard-coded arrays of magnifications,
        calibration values and length units. The calibration can optionally be set as global. After
        spatial calibration the plugin will also optionally run Wayne's new scale-bar plugin, available
        in version 1.28h. To customize the plugin for your specific microscope, edit the arrays
        between the "START EDIT" and "END EDIT" comments below. Save anywhere in the
        "plugins" directory, then use "Compile and Run" option under the "plugins" menu to create
        a java class. Restart ImageJ.
**/
        ImagePlus imp;
        private static boolean addScaleBar = false;  //  if true run scalebar plugin - available in version 1.28h
        private static boolean isGlobalCal = false; // if true, set selected calibration as the global calibration
        private static int magIndex = 4; // index of initial selected magnification in dropdown menu

/* START EDIT
        Edit the following arrays using your microscope's nominal magnification steps, the
        corresponding spatial calibration and the length units of the spatial calibration.
        Make sure the arrays are of equal length.
**/
        // nominal magnification settings of the microscope
        private static String[] mags =  { "49k", "60k", "82k", "105k","135k", "175k", "230k", "300k", "380k", "490k", "635k", "820k"};
        // spatial calibration for the nominal magnification settings - width of one pixel (pixelWidth)
        private static double[] xscales = {0.88463, 0.75118, 0.53233,0.42808, 0.34537, 0.25849, 0.19856, 0.15225, 0.12203, 0.10324, 0.08748, 0.05939};
        // units for the spacial calibrations given in xscales array above
        private static String[] units =  { "nm",  "nm",  "nm",  "nm",  "nm", "nm",  "nm",  "nm",  "nm",  "nm",  "nm",  "nm"};
/* END EDIT **/

        public int setup(String arg, ImagePlus imp) {
                this.imp = imp;
                if (imp==null)
                        {IJ.noImage(); return DONE;}
                return DOES_ALL;
        }

        public void run(ImageProcessor ip) {
                if (doDialog()) {
                        Calibration oc = imp.getCalibration().copy();
                        oc.setUnit(units[magIndex]);
                        oc.pixelWidth=xscales[magIndex];
                        oc.pixelHeight=oc.pixelWidth;
                        if (isGlobalCal) {
                                imp.setGlobalCalibration(oc);
                                int[] list = WindowManager.getIDList();
                                if (list==null) return;
                                for (int i=0; i<list.length; i++) {
                                        ImagePlus imp2 = WindowManager.getImage(list[i]);
                                        if (imp2!=null) imp2.getWindow().repaint();
                                }
                                } else {
                                imp.setGlobalCalibration(null);
                                imp.setCalibration(oc);
                                imp.getWindow().repaint();
                        }
                        if (addScaleBar){
                                IJ.run("Scale Bar...");
                        }
                }
        }

        private boolean doDialog() {
                GenericDialog gd = new GenericDialog("Scale Microscope Image...");
                gd.addChoice("Nominal Magnification", mags, mags[magIndex]);
                gd.addCheckbox("Global Calibration", isGlobalCal);
                gd.addCheckbox("Add Scale Bar", addScaleBar);
                gd.showDialog();
                if (gd.wasCanceled()) {return false;}
                magIndex=gd.getNextChoiceIndex();
                isGlobalCal = gd.getNextBoolean();
                addScaleBar = gd.getNextBoolean();
                return true;
        }
}
