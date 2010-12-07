import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.algorithm.math.MathLib;

public class Imglib_Find_Max<T extends RealType<T>> implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
  		long start = System.currentTimeMillis();
		final Image<T> img = ImagePlusAdapter.wrap( imp );  
		final LocalizableCursor<T> loc_cursor = img.createLocalizableCursor();
		final T max = img.createType();
		max.setReal(max.getMinValue());
		final int[] max_position = img.createPositionArray();
		for (final T t : loc_cursor) {
			if (max.compareTo(t) < 0) {
				loc_cursor.getPosition(max_position);
				max.set(t);
			}
		}
		loc_cursor.close();
		showTime(imp, start);
		IJ.log("Maximum of "+max+" found at "+position(max_position));
		IJ.makePoint(max_position[0], max_position[1]);
		if (max_position.length==3)
			imp.setSlice(max_position[2]+1);
	}

	void showTime(ImagePlus imp, long start) {
		int images = imp.getStackSize();
		double time = (System.currentTimeMillis()-start)/1000.0;
		IJ.showTime(imp, start, "", images);
		double mpixels = (double)(imp.getWidth())*imp.getHeight()*images/1000000;
		IJ.log("\n"+imp);
		IJ.log(IJ.d2s(mpixels/time,1)+" million pixels/second");
	}

	String position(int[] coords) {
		String pos = coords[0]+","+coords[1];
		for (int i=2; i<coords.length; i++)
			pos += ","+coords[i];
		return pos;
	}

}
