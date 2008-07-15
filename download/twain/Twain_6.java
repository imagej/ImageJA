import ij.*;
import ij.process.*;
import ij.measure.*;
import ij.gui.*;
import java.awt.*;
import java.awt.image.*;
import ij.plugin.*;
import SK.gnome.twain.*;

/**
This plugin acquires images from TWAIN sources such as scanners,
digital cameras and frame grabbers. n.getUnits() does not work.
*/

public class Twain_6 implements PlugIn {

    public void run(String arg) {
        ImagePlus imp = null;
        imp  = scan(arg);
        if (imp!=null)
            imp.show();
    }

    public ImagePlus scan(String arg) {
        ImagePlus imp = null;
        TwainSource source = null;
        try {
            if (arg.equals("default"))
                 source = TwainManager.getDefaultSource();
            else
                source = TwainManager.selectSource(null);
             if (source==null) return null;
             Image image=Toolkit.getDefaultToolkit().createImage(new TwainImage(source));
             imp = new ImagePlus("Untitled", image);
             getUnits(imp, source);
         } catch(Exception e) {
             //e.printStackTrace();
             String msg = ""+e;
             if (msg.indexOf("Invalid capability")==-1)
                  IJ.showMessage("Twain", ""+e);
         } finally {
              try {TwainManager.close();}
              catch (TwainException e) {}
         }
         return imp;
    }

    void getUnits(ImagePlus imp, TwainSource source) throws TwainException {
        String unit;
        int code = source.getUnits();
        if (code==TwainSource.TWUN_INCHES)
            unit = "inch";
        else if (code==TwainSource.TWUN_CENTIMETERS)
            unit = "cm";
        else
            unit = "";
        double xResolution = source.getXResolution();
        double yResolution = source.getYResolution();
        if (xResolution!=0.0 && yResolution!=0.0) {
            Calibration cal = imp.getCalibration();
            cal.pixelWidth = 1/xResolution;
            cal.pixelHeight = 1/yResolution;
            cal.setUnit(unit);
        }
    }

}
