import ij.*;
import ij.plugin.PlugIn;
import imagescience.image.*;

public class ImageScience_Find_Max implements PlugIn {
   
   public void run(String arg) {
      ImagePlus imp = IJ.getImage();
      long start = System.currentTimeMillis();
      Image img = Image.wrap(imp);
      Dimensions dims = img.dimensions();
      final Coordinates c = new Coordinates();
      final Coordinates c2 = new Coordinates();
      img.axes(Axes.X);
      final double[] v = new double[dims.x];
      double max = -Double.MAX_VALUE;
      for (c.c=0; c.c<dims.c; ++c.c)
         for (c.t=0; c.t<dims.t; ++c.t)
            for (c.z=0; c.z<dims.z; ++c.z)
               for (c.y=0; c.y<dims.y; ++c.y) {
                  img.get(c,v);
                  for (int x=0; x<dims.x; ++x)
                     if (v[x]>max) {
                        max = v[x];
                        c2.set(c);
                        c2.x = x;
                     }
               }
      showTime(imp, start);
      imp.setPosition(c2.c+1, c2.z+1, c2.t+1);
      IJ.makePoint(c2.x, c2.y);
      IJ.log("Maximum of "+max+" found at "
         +c2.x+","+c2.y+","+c2.c+","+c2.z+","+c2.t);
   }

   void showTime(ImagePlus imp, long start) {
      int images = imp.getStackSize();
      double time = (System.currentTimeMillis()-start)/1000.0;
      IJ.showTime(imp, start, "", images);
      double mpixels = (double)(imp.getWidth())*imp.getHeight()*images/1000000;
      IJ.log("\n"+imp);
      IJ.log(IJ.d2s(mpixels/time,1)+" million pixels/second");
   }
   
}
