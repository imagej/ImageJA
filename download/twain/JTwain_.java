import ij.*;
import ij.process.*;
import ij.measure.*;
import ij.gui.*;
import java.awt.*;
import java.awt.image.*;
import ij.plugin.*;
import com.asprise.util.jtwain.*;

/**
This plugin acquires images from TWAIN sources such as scanners,
digital cameras and frame grabbers. Requires the JTwain package from
"http://asprise.com/product/jtwain/".
*/

public class JTwain_ implements PlugIn {

    public void run(String arg) {
        ImagePlus imp = null;
        imp  = scan(arg);
        if (imp!=null)
            imp.show();
    }

    public ImagePlus scan(String arg) {
        ImagePlus imp = null;
        Source source = null;
        try {
             if (arg.equals("default"))
                 source = SourceManager.instance().getDefaultSource();
             else
                 source = SourceManager.instance().selectSourceUI();
             if (source==null) return null;
             source.open();
             Image image = source.acquireImage();
             imp = new ImagePlus("Untitled", image);
             try {getUnits(imp, source);}
             catch(Exception e) {}
         } catch(Exception e) {
             //e.printStackTrace();
             SourceManager.closeSourceManager();
             IJ.showMessage("JTwain", ""+e);
         } finally {
              SourceManager.closeSourceManager();
         }
        return imp;
    }

  void getUnits(ImagePlus imp, Source source)  throws Exception{
        String unit;
        int[] code = source.getCurrentUnits();
        if (code==null || code.length==0) return;
        if (code[0]==Source.TWUN_INCHES)
            unit = "inch";
        else if (code[0]==Source.TWUN_CENTIMETERS)
            unit = "cm";
        else
            unit = "";
        double xResolution = source.getCurrentXResolution();
        double yResolution = source.getCurrentYResolution();
        if (xResolution!=0.0 && yResolution!=0.0) {
            Calibration cal = imp.getCalibration();
            cal.pixelWidth = 1/xResolution;
            cal.pixelHeight = 1/yResolution;
            cal.setUnit(unit);
        }
    }
}
