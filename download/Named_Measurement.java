import ij.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

/** This plugin is an extended version of the Measure command 
      that allows naming of individual measurements.
      @Author: Audrey Karperien (akarpe01@postoffice.csu.edu.au)
*/
public class Named_Measurement implements PlugIn, Measurements {
    static String name = "";

    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp==null)
            {IJ.noImage(); return;}
        Roi roi = imp.getRoi();
        if (roi==null )
            {IJ.error("Selection required"); return;}                
        GenericDialog gd = new GenericDialog("Measurement Name:");
        gd.addStringField("Name:", name, 25);
        gd.showDialog();
        if (gd.wasCanceled())
            return;
        int measurements = Analyzer.getMeasurements();
        Analyzer.setMeasurements(measurements |= LABELS);
        name = gd.getNextString();
        String title = imp.getTitle();
        imp.setTitle(name);     
        IJ.run("Measure");
        imp.setTitle(title);
     }

}

