import ij.*;
import ij.process.*;
import ij.plugin.PlugIn;

/**
This is a plugin version of the BeanShell script by Stephan Saalfeld at
    http://pacific.mpi-cbg.de/wiki/index.php/RGB_to_CMYK
which converts an uncalibrated linear RGB image into an
uncalibrated linear 32-bit CMYK stack.
*/
public class RGB_to_CMYK implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		if (imp.getBitDepth()!=24) {
			IJ.error("RGB to CMYK", "RGB image required");
			return;
		}
		ImagePlus cmykStack = rgbToCmyk(imp);
		cmykStack.show();
	}
	
	/** Converts an RGB image into a 32-bit CMYK stack. */
	public ImagePlus rgbToCmyk(ImagePlus imp) {
		
		ImageProcessor ipRGB = imp.getProcessor();
		int width = ipRGB.getWidth();
		int height = ipRGB.getHeight();
		
		/* CMYK */
		ImageProcessor ipC = new FloatProcessor(width, height);
		ImageProcessor ipM = new FloatProcessor(width, height);
		ImageProcessor ipY = new FloatProcessor(width, height);
		ImageProcessor ipK = new FloatProcessor(width, height);
				
		int[] pixels = ( int[] )ipRGB.getPixels();
		float[] cPixels = ( float[] )ipC.getPixels();
		float[] mPixels = ( float[] )ipM.getPixels();
		float[] yPixels = ( float[] )ipY.getPixels();
		float[] kPixels = ( float[] )ipK.getPixels();
		
		for ( int i = 0; i < pixels.length; ++i ){
			int argb = pixels[ i ];
			float r = ( argb >> 16 ) & 0xff;
			float g = ( argb >> 8 ) & 0xff;
			float b = argb & 0xff;
			float c = 1.0f - r / 255.0f;
			float m = 1.0f - g / 255.0f;
			float y = 1.0f - b / 255.0f;
			float k = Math.min( c, Math.min( m, y ) );
			if ( k >= 1.0f )
				cPixels[ i ] = mPixels[ i ] = yPixels[ i ] = 0;
			else {
				float s = 1.0f - k;
				cPixels[ i ] = ( c - k ) / s;
				mPixels[ i ] = ( m - k ) / s;
				yPixels[ i ] = ( y - k ) / s;
			}
			kPixels[ i ] = k;
		}
		
		ipC.setMinAndMax( 0.0, 1.0 );
		ipM.setMinAndMax( 0.0, 1.0 );
		ipY.setMinAndMax( 0.0, 1.0 );
		ipK.setMinAndMax( 0.0, 1.0 );
		
		ImageStack stack = new ImageStack(width, height);
		stack.addSlice("C", ipM);
		stack.addSlice("M", ipM);
		stack.addSlice("Y", ipY);
		stack.addSlice("K", ipK);
		ImagePlus cmyk = new ImagePlus("CMYK_"+imp.getTitle(), stack);
		cmyk = new CompositeImage(cmyk, CompositeImage.COLOR);
		cmyk.setSlice(1); IJ.run(cmyk,"Cyan",""); IJ.run(cmyk,"Invert LUT","");
		cmyk.setSlice(2); IJ.run(cmyk,"Magenta",""); IJ.run(cmyk,"Invert LUT","");
		cmyk.setSlice(3); IJ.run(cmyk,"Yellow",""); IJ.run(cmyk,"Invert LUT","");
		cmyk.setSlice(4); IJ.run(cmyk,"Invert LUT","");
		cmyk.setSlice(1);
		return cmyk;
	}
	
}
