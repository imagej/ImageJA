import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import java.awt.image.*;

public class Transparent_Image_Overlay implements PlugIn {

    public void run(String arg) {
        ImagePlus imp = IJ.openImage("http://imagej.nih.gov/ij/images/cardio.dcm.zip");
        ImageProcessor ip = imp.getProcessor();
        ip = ip.crop();
        ip.setRoi(142, 132, 654, 616);
        int width = ip.getWidth()/2;
        int height = ip.getHeight()/2;
        ip = ip.resize(width, height, true);
        ip.setColor(Color.red);
        ip.setFont(new Font("SansSerif",Font.PLAIN,28));
        ip.drawString("Transparent\nImage\nOverlay", 0, 40);
        ip.setColorModel(new DirectColorModel(32,0x00ff0000,0x0000ff00,0x000000ff,0xff000000));
        for (int x=0; x<width; x++) {
           for (int y=0; y<height; y++) {
                double v = ip.getPixelValue(x, y);
                if (v>1) ip.set(x, y, ip.get(x,y)|0xff000000);
            }
        }
        Roi imageOverlay = new ImageRoi(100, 25, ip);
        ImagePlus boats = IJ.openImage("http://imagej.nih.gov/ij/images/boats.gif");
        Overlay overlay = new Overlay(imageOverlay);
        boats.setOverlay(overlay);
        //boats.setRoi(imageOverlay);
        boats.show();
        
    }

}
