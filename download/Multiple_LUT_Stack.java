import ij.*;
import ij.process.*;
import ij.plugin.*;

public class Multiple_LUT_Stack implements PlugIn {

   public void run(String arg) {
      int w=500, h=500, images=10;
      ImagePlus imp = IJ.createImage("Multiple LUT Stack", "32-bit", w, h, images);
      imp.setDimensions(images, 1, 1);
      imp = new CompositeImage(imp, CompositeImage.GRAYSCALE);
      for (int s=1; s<=images; s++) {
         imp.setSlice(s);
         ImageProcessor ip = imp.getProcessor();
         assignPixels(ip, s);
         ip.resetMinAndMax();
         imp.setDisplayRange(ip.getMin(), ip.getMax());
      }
      imp.show();
   }
   
   void assignPixels(ImageProcessor ip, int s) {
       for (int y=0; y<ip.getHeight(); y++)
         for (int x=0; x<ip.getWidth(); x++)
            ip.putPixelValue(x, y, x*s);
   }
   
}
