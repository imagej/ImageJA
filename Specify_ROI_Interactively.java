import ij.plugin.PlugIn;
import java.awt.*;
import java.util.Vector;
import ij.*;
import ij.gui.*;

public class Specify_ROI_Interactively implements PlugIn, DialogListener {
    private double xRoi, yRoi, width, height;
    private boolean  oval;
    private boolean  centered;
    private Rectangle prevRoi;
    private boolean bAbort;
    private ImagePlus imp;

    public void run(String arg) {
        imp = IJ.getImage();
        Roi roi = imp.getRoi();
        if (roi!=null && roi.getBounds().equals(prevRoi))
            roi = null;
        if (roi!=null) {
            boolean rectOrOval = roi!=null
                && (roi.getType()==Roi.RECTANGLE||roi.getType()==Roi.OVAL);
            oval = rectOrOval && (roi.getType()==Roi.OVAL);
            Rectangle r = roi.getBounds();
            width = r.width;
            height = r.height;
            xRoi = r.x;
            yRoi = r.y;
        } else {
            width = imp.getWidth()/2;
            height = imp.getHeight()/2;
            xRoi = width/2;
            yRoi = height/2; 
        }
        if (centered) { 
            xRoi += width/2.0;
            yRoi += height/2.0; 
        }
        showDialog();
    }
    
    void showDialog() {
        Roi roi = imp.getRoi();
        if (roi==null)
            drawRoi();
        int w = imp.getWidth();
        int h = imp.getHeight();
        GenericDialog gd = new GenericDialog("Specify ROI");
        gd.addSlider("X:", 0, w, xRoi);
        gd.addSlider("Y:", 0, h, yRoi);
        gd.addSlider("Width:", 0, w, width);
        gd.addSlider("Height:", 0, h, height);
        gd.addCheckbox("Oval", oval);
        gd.addCheckbox("Centered",centered);
        gd.addDialogListener(this);
        gd.showDialog();
        if (gd.wasCanceled()) {
             if (roi==null)
                imp.deleteRoi();
             else // restore initial ROI when cancelled
                imp.setRoi(roi);
        }
    }
    
    void drawRoi() {
        int iX = (int)xRoi;
        int iY = (int)yRoi;
        if (centered) {
            iX = (int)(xRoi - (width/2));
            iY = (int)(yRoi - (height/2));
        }
        int iWidth = (int)width;
        int iHeight = (int)height;
        Roi roi;
        if (oval)
            roi = new OvalRoi(iX, iY, iWidth, iHeight);
        else
            roi = new Roi(iX, iY, iWidth, iHeight);
        imp.setRoi(roi);
        prevRoi = roi.getBounds();
    }
        
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        xRoi = gd.getNextNumber();  
        yRoi = gd.getNextNumber();
        width = gd.getNextNumber();
        height = gd.getNextNumber();
        oval = gd.getNextBoolean();
        centered = gd.getNextBoolean();
        if (gd.invalidNumber())
            return false;
        else {
            drawRoi();
            return true;
        }
    }

}
