import java.awt.*;
import ij.*;
import ij.process.*;
import ij.plugin.PlugIn;

/** This is an example plugin that shows how to add text to an image. */
public class Text_Demo implements PlugIn {

	public void run(String arg) {
		int w = 400, h = 200;
		if (IJ.versionLessThan("1.17s"))
			return;
		ImageProcessor ip = new ColorProcessor(w, h);
		//ImageProcessor ip = new ByteProcessor(w, h);
		//ImageProcessor ip = new ShortProcessor(w, h,true);
		//ImageProcessor ip = new ShortProcessor(w, h,false);
		//ImageProcessor ip = new FloatProcessor(w, h);
		ip.setColor(Color.white);
		ip.fill();
	
		ip.setColor(Color.black);
		int x=10, y=20;
		ip.moveTo(x,y);
		ip.drawString("This is the default font.");

		ip.setFont(new Font("Monospaced", Font.PLAIN, 12));
		y += 20; ip.moveTo(x,y);
		ip.drawString("This is 12-point, 'Monospaced', PLAIN.");

		ip.setFont(new Font("SansSerif", Font.PLAIN, 18));
		y += 30; ip.moveTo(x,y);
		ip.drawString("This is 18-point, 'SansSerif', PLAIN.");
		
		ip.setFont(new Font("Serif", Font.BOLD+Font.ITALIC, 18));
		y += 30; ip.moveTo(x,y);
		ip.setColor(Color.blue);
		ip.drawString("This is 18-point, 'Serif', BOLD+ITALIC.");

		ip.setFont(new Font("SansSerif", Font.PLAIN, 24));
		y += 40; ip.moveTo(x,y);
		ip.setColor(Color.red);
		ip.drawString("24-point, 'SansSerif', PLAIN.");

		new ImagePlus("Text Demo", ip).show();
	 }

}


