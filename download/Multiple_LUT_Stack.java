import ij.*;
import ij.process.*;
import ij.plugin.*;

public class Multiple_LUT_Stack implements PlugIn {

   public void run(String arg) {
      int w=500, h=500, images=10;
      ImagePlus imp = IJ.createImage("Multiple LUT Stack", "32-bit ramp", w, h, images);
      imp.setDimensions(images, 1, 1);
      imp = new CompositeImage(imp, CompositeImage.GRAYSCALE);
      for (int i=1; i<=images; i++) {
         imp.setSlice(i);
         ImageProcessor ip = imp.getProcessor();
         ip.multiply(100); ip.add(i*100);
         ip.resetMinAndMax();
         imp.setDisplayRange(ip.getMin(), ip.getMax());
      }
      imp.show();
      for (int i=1; i<=images; i++) {
         imp.setSlice(i);
         ImageProcessor ip = imp.getProcessor();
         IJ.log(i+": "+(int)ip.getMin()+"-"+(int)ip.getMax());
      }
  }
      
}
