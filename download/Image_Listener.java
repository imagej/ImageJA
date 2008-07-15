import ij.*;
import ij.plugin.*;

// This plugin demonstrates how to use the ImageListener interface, 
// which enables a plugin to be called when an image window is opened,
// closed or updated. It terminates when the "Log" window is closed.
public class Image_Listener implements PlugIn, ImageListener {

	public void run(String arg) {
		if (IJ.versionLessThan("1.35l")) return;
		IJ.log("Image_Listener: starting");
		ImagePlus.addImageListener(this);
	}

	// called when an image is opened
	public void imageOpened(ImagePlus imp) {
		checkLog();
		IJ.log(imp.getTitle() + " opened");
	}

	// Called when an image is closed
	public void imageClosed(ImagePlus imp) {
		checkLog();
		IJ.log(imp.getTitle() + " closed");
	}

	// Called when an image's pixel data is updated
	public void imageUpdated(ImagePlus imp) {
		checkLog();
		IJ.log(imp.getTitle() + " updated");
	}

	// Quit if the "Log" window is not open
	void checkLog() {
		if (WindowManager.getFrame("Log")==null) {
			IJ.log("Image_Listener: stopping");
			ImagePlus.removeImageListener(this);
		}
	}
	
}
