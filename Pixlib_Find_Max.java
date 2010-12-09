import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import ijaux.*;
import ijaux.compat.ImagePlusCube;
import ijaux.compat.ImagePlusIndex;
import ijaux.datatype.Pair;
import ijaux.datatype.access.*;
import ijaux.hypergeom.BaseIndex;
import ijaux.hypergeom.PixelCube;
import ijaux.iter.IndexedIterator;
 
public class Pixlib_Find_Max implements PlugIn {
	//private PixLib plib=new PixLib();
 
	public void run(String arg) {
			
		ImagePlus imp = IJ.getImage();
		long start = System.currentTimeMillis();
		
		// wraps the ImagePlus to a HyperCube
		ImagePlusCube ipc=new ImagePlusCube(imp);
		long time1 = System.currentTimeMillis();
		
		int size=Util.cumprod(imp.getDimensions());
		
		int idx= 0;
		Access<?> access=ipc.getAccess();
		
	
		float max=access.elementFloat(idx);
		int ind=idx;
		
		//iterates over the image and finds the 1st global maximum
		for (idx=0; idx<size; idx++) {
			final float c=access.elementFloat(idx);
			if (c>max) {
				max=c;
				ind=idx;
			}
		}
		
		ImagePlusIndex bi=ipc.getIndex();
		//calculates the image coordinates
		bi.setIndex(ind);
		int[] coords=bi.getCoordinates();
		showTime(imp, start);
	 
		IJ.makePoint(coords[0],coords[1]);
		IJ.log("Maximum of "+  access.elementFloat(ind)  +" found at "+bi);

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
