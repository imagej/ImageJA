import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.Random;
import ij.plugin.*;

public class Random_Ovals implements PlugIn {

	public void run(String arg) {
		IJ.run("New...", "name='Random Ovals' type='32-bit RGB' width=400 height=400");
		if (IJ.altKeyDown())
			drawOvalsFaster();
		else
			drawOvals();
	}

	void drawOvals() {
		ImagePlus imp = WindowManager.getCurrentImage();
		Random ran = new Random();
		int width = imp.getWidth();
		int height = imp.getHeight();
		for (int i=0; i<1000; i++) {
			int w = (int)(ran.nextDouble()*width/2+1);
			int h = (int)(ran.nextDouble()*width/2+1);
			int x = (int)(ran.nextDouble()*width-w/2);
			int y = (int)(ran.nextDouble()*height-h/2);
			IJ.setForegroundColor((int)(ran.nextDouble()*255),
				 (int)(ran.nextDouble()*255), (int)(ran.nextDouble()*255));
			IJ.makeOval(x, y, w, h);
			IJ.run("Fill");
		}
	}

	void drawOvalsFaster() {
		ImagePlus imp = WindowManager.getCurrentImage();
		Random ran = new Random();
		int width = imp.getWidth();
		int height = imp.getHeight();
		ImageProcessor ip = imp.getProcessor();
		for (int i=0; i<1000; i++) {
			int w = (int)(ran.nextDouble()*width/2+1);
			int h = (int)(ran.nextDouble()*width/2+1);
			int x = (int)(ran.nextDouble()*width-w/2);
			int y = (int)(ran.nextDouble()*height-h/2);
			ip.setColor(new Color((int)(ran.nextDouble()*255),
				 (int)(ran.nextDouble()*255), (int)(ran.nextDouble()*255)));
			Roi roi=new OvalRoi(x, y, w, h, null);
			ip.setMask(roi.getMask());
			ip.setRoi(roi.getBoundingRect());
			ip.fill(ip.getMask());
			if (i%10==0)
				imp.updateAndDraw();
		}
	}

}

