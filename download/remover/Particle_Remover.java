import ij.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;

/**	This is an extended ParticleAnalyzer that creates a copy of the
	image that has each measured particle erased to the current foreground color.
	For stacks, the particles are erased in place.
*/
public class Particle_Remover implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null || imp.getType()!=ImagePlus.GRAY8)
			{IJ.error("8-bit grayscale image required"); return;}
		Roi roi=imp.getRoi();
		ImageStack stack = imp.getStack();
		int stackSize = imp.getStackSize();
		int currentSlice = imp.getCurrentSlice();
		CustomAnalyzer pa = new CustomAnalyzer();
		int code = pa.setup("", imp);
		if (code==PlugInFilter.DONE)
			return;
		if ((code&PlugInFilter.DOES_STACKS)==0)
			stackSize = 1;
		for (int i=1; i<=stackSize; i++) {
			IJ.showProgress((double)i/stackSize);
			IJ.showStatus("a: "+i+"/"+stackSize);
			if (stackSize>1)
				imp.setSlice(i);
			ImageProcessor ip = imp.getProcessor();
			pa.run2(imp, ip);
			if (pa.error)
				break;
			if (stackSize==1)
				new ImagePlus(imp.getTitle()+"2", pa.ip2).show();
			else
				ip.setPixels(pa.ip2.getPixels());
			if(roi!=null)imp.setRoi(roi);
		}
		IJ.showProgress(1.0);
		ImageWindow win = imp.getWindow();
		if (win!=null)
			win.running = false;
		if (stackSize>1) {
			imp.setSlice(currentSlice);
			imp.getProcessor().resetThreshold();
			imp.updateAndDraw();
		}
	}
	
}

class CustomAnalyzer extends ParticleAnalyzer {
	ImageProcessor ip2;
	boolean error;
	
	public void run2(ImagePlus imp, ImageProcessor ip) {
		ip2 = ip.duplicate(); // make a copy of this image
		ip2.setColor(Toolbar.getForegroundColor());
		slice++;
		error = !analyze(imp, ip); 
	}
	
	protected void saveResults(ImageStatistics stats, Roi roi) {
		super.saveResults(stats, roi);
		ip2.setRoi(roi.getBoundingRect());
		ip2.fill(roi.getMask());
	}
}
