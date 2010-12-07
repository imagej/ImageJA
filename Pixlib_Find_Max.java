import java.awt.BasicStroke;
import java.awt.geom.GeneralPath;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import ijaux.*;
import ijaux.datatype.Pair;
import ijaux.datatype.access.*;
import ijaux.hypergeom.BaseIndex;
import ijaux.hypergeom.PixelCube;
import ijaux.iter.IndexedIterator;
 
public class Pixlib_Find_Max implements PlugIn {
	private PixLib plib=new PixLib();
 
	public void run(String arg) {
			
		ImagePlus imp = IJ.getImage();
		long start = System.currentTimeMillis();
		// converts the ImagePlus to a PixelCube
		PixelCube<Number,BaseIndex>  pc=plib.cubeFrom(imp, PixLib.BASE_INDEXING);
		int idx= pc.index();
		final Access<?> access=pc.getAccess();
		
		Pair<Number,Integer> max=  (Pair<Number,Integer>)Pair.of((Number)access.elementFloat(idx),idx);
		//iterates over the image and finds the 1st global maximum
		int imgtype=imp.getType();
		
		
		for (IndexedIterator<Number> iter= pc.iterator();iter.hasNext(); iter.next()) {
			switch (imgtype) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256: 
			case ImagePlus.GRAY16: {
				final int c=access.elementInt(idx);
				if (c> max.first.intValue()  )
					max=(Pair<Number, Integer>)Pair.of((Number)c,idx);
			}
			case ImagePlus.GRAY32: {
				final float c=access.elementFloat(idx);
				if (c > max.first.floatValue()  )
					max=(Pair<Number, Integer>)Pair.of((Number)c,idx);
			}
			
			}
			
			idx= iter.index();
		}
	 
		BaseIndex bi=pc.getIndex();
		//calculates the image coordinates
		bi.setIndex(max.second);
		int[] coords=bi.getCoordinates();
		long time2=System.currentTimeMillis();
		showTime(imp, start);
	 
		//IJ.makePoint(coords[0],coords[1]);
		drawCross(  coords,   imp);
		IJ.log("Maximum of "+  access.elementFloat(max.second)  +" found at "+bi);
		long duration=time2-start;
		int size=pc.getSize();
		System.out.println("run time: "+ duration+" ms\n speed " + (float)size/duration +" pix/ms" );

	}

	void showTime(ImagePlus imp, long start) {
		int images = imp.getStackSize();
		double time = (System.currentTimeMillis()-start)/1000.0;
		IJ.showTime(imp, start, "", images);
		double mpixels = (double)(imp.getWidth())*imp.getHeight()*images/1000000;
		IJ.log("\n"+imp);
		IJ.log(IJ.d2s(mpixels/time,1)+" million pixels/second");
	}

	void drawCross(int[] coords, ImagePlus imp) {
		int centerx=coords[0];
		int centery=coords[1];
		GeneralPath path = new GeneralPath();
		final float arm  = 5;
		float x = (centerx);
		float y = (centery);
		path.moveTo(x-arm, y);
		path.lineTo(x+arm, y);
		path.moveTo(x, y-arm);
		path.lineTo(x, y+arm);
		imp.setOverlay(path,Toolbar.getForegroundColor(), new BasicStroke(2));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ImageJ();
		new Pixlib_Find_Max().run(null);
		

	}


 }
