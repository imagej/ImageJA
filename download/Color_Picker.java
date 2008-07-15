// Color_Picker 1.0 by Kas Thomas
// 27 Nov 2001
//
// Creates a spectral color window. Use the eyedropper tool to pick
// any color.
//
// Freely distribute!


import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
 

public class Color_Picker  implements PlugIn {
   private  ImagePlus imp;
 

public void run(String arg) {
   imp = createWindow("ColorPicker 1.0 by Kas Thomas",400,60); // 400x60 default size
   imp.show();
        swatch((int[])  imp.getProcessor().getPixels(), imp.getWidth(), imp.getHeight() );
 }
 
 public ImagePlus createWindow(String title,int w, int h) { 
    return NewImage.createRGBImage(title,w,h,1,0);
 }

                                           // Fill the pixel array with colors
 public void swatch( int[] pixels, int  width,int  height )  {
         int offset = 0;
         for (int y = 0;  y < height; y++, offset += width,incrementalRefresh(y%40==0) ) 
                for (int  x = 0; x < width; x++)
                     pixels[x+offset] = rainbowPixel(  (double)x/(double)width,(double)y/(double)height);
 
     incrementalRefresh(true);
 }
 
                      	 // Refresh the screen every so often
 public void  incrementalRefresh(boolean needToDo) {
                             if (needToDo)
                                    imp.updateAndRepaintWindow();
 }
 		// Generate a pixel color based on screen position
 private int rainbowPixel(double xspan,double  yspan) {
 
          double red =255. -  yspan*255. * ( 1.0+  Math.sin( 6.3*xspan ) )/2.;
          double green = 255. - yspan*255. * ( 1.0+Math.cos( 6.3*xspan ) )/2.;
          double blue =  255. - yspan*255. * ( 1.0-Math.sin( 6.3*xspan ) )/2.;
 
         return ((int)red << 16) + ((int)green<<8) + (int)blue;
 }
 
}


