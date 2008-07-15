import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.filter.*; 

public class Measure_And_Label implements PlugIn { 

public void run(String arg) {
        IJ.run("Measure");
        ImagePlus imp = IJ.getImage();
        Roi roi = imp.getRoi();
        if (roi!=null) {
            IJ.setForegroundColor(255, 255, 255);
            IJ.run("Line Width...", "line=1");
            IJ.run("Draw");
            drawLabel(imp, roi);
        }
    } 

void drawLabel(ImagePlus imp, Roi roi) {
            if (roi==null) return;
            Rectangle r = roi.getBoundingRect();
            ImageProcessor ip = imp.getProcessor();
            String count = "" + Analyzer.getCounter();
            int x = r.x + r.width/2 - ip.getStringWidth(count)/2;
            int y = r.y + r.height/2 + 6;
            ip.setFont(new Font("SansSerif", Font.PLAIN, 9));
            ip.drawString(count, x, y);
            imp.updateAndDraw();
    } 

}



