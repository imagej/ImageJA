import ij.*;
import ij.plugin.PlugIn;

/** Message Test
  *
  * Demonstrates the use of some of the utility methods in class IJ.
  *
  * This is an example plugin from the ImageJ plugin writing tutorial.
  * The tutorial can be downloaded at 
  * http://www.fhs-hagenberg.ac.at/staff/burger/ImageJ/tutorial
  */
public class Message_Test implements PlugIn {

	public void run(String arg) {
		// show text in status bar
		IJ.showStatus("Plug-in Message Test started.");
		// set value of progress bar
		IJ.showProgress(0.0);
		// display error message
		IJ.error("need user input");
		// display text input dialog
		String name = IJ.getString("Please enter your name: ","me");
		IJ.showProgress(0.5);
		// write text to the ImageJ window
		IJ.write("now starting sample plugion RedAndBlue ... ");
		// run another plugin
		IJ.runPlugIn("RedAndBlue_","");
		IJ.showProgress(1);
		// display custom message dialog
		IJ.showMessage("Finished.",name+", thank you for running this plug-in");
	}

}

