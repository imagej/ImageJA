//=====================================================
//      Name:            Circle_Test.java
//      Project:         ImageJ PlugIns for image manipulation
//      Version:         0.1
//
//      Author:           Joshua Gulick, Orphan Technologies, Inc.
//      Date:               6/2/2000
//      Comment:        Draws a circle  :)
//=====================================================


//===========imports===================================
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;


public class Circle_Test implements PlugIn {

	public void run(String arg) {
		int w = 324, h = 200;
		ImageProcessor ip = new ColorProcessor(w, h);
		//int[] pixels = (int[])ip.getPixels();
		int centerx = 162, centery = 100;
		double radius = 60;
		for (double counter = 0; counter < 10; counter = counter + 0.001) {
			double x = Math.sin(counter) * radius + centerx;
			double y = Math.cos(counter) * radius + centery;
			ip.putPixel((int)x, (int)y, -1);
			//int i = ((int) y)*w+((int) x);
			//pixels[i] = (255 << 16) | 255 << 8 | 255;
		}
		new ImagePlus("Circle Test", ip).show();
	 }
}


