import ij.*;
import ij.gui.*;
import ij.plugin.frame.PlugInFrame;
import java.awt.Label;

/** FrameDemo
  *
  * Demonstrates the use of PlugInFrame and GenericDialog.
  *
  * This is an example plugin from the ImageJ plugin writing tutorial
  * The tutorial can be downloaded at 
  * http://www.fhs-hagenberg.ac.at/staff/burger/ImageJ/tutorial
  */
public class FrameDemo_ extends PlugInFrame {

	public FrameDemo_() {
		super("FrameDemo");
	}
	
	public void run(String arg) {
		// create a dialog with two numeric input fields
		GenericDialog gd = new GenericDialog("FrameDemo settings");
		gd.addNumericField("Frame width:",200.0,3);
		gd.addNumericField("Frame height:",200.0,3);
		
		// show the dialog and quit, if the user clicks "cancel"
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("PlugIn canceled!");
			return;
		}
	
		// set the size of this frame to the values specified by the user,
		// add label and show the frame
		this.setSize((int) gd.getNextNumber(),(int) gd.getNextNumber());
		this.add(new Label("PlugInFrame demo",Label.CENTER));
		this.show();
	}

}

