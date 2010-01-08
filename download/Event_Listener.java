import ij.IJ;
import ij.IJEventListener;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.gui.Toolbar;

// This plugin demonstrates how to use the IJEventListener interface.
// Plugins that implement this interface are notified when the user
// changes the foreground color, changes the background color,
// closes the color picker, closes the Log window or switches tools.
public class Event_Listener implements PlugIn, IJEventListener {

	public void run(String arg) {
		if (IJ.versionLessThan("1.43o")) return;
		IJ.addEventListener(this);
		IJ.log("Event_Listener started");
	}
	
	public void eventOccurred(int eventID) {
		switch (eventID) {
			case IJEventListener.FOREGROUND_COLOR_CHANGED:
				String c = Integer.toHexString(Toolbar.getForegroundColor().getRGB());
				c = "#"+c.substring(2);
				IJ.log("The user changed the foreground color to "+c);
				break;
			case IJEventListener.BACKGROUND_COLOR_CHANGED:
				c = Integer.toHexString(Toolbar.getBackgroundColor().getRGB());
				c = "#"+c.substring(2);
				IJ.log("The user changed the background color to "+c);
				break;
			case IJEventListener.TOOL_CHANGED:
				String name = IJ.getToolName();
				IJ.log("The user switched to the "+name+(name.endsWith("Tool")?"":" tool"));
				break;
			case IJEventListener.COLOR_PICKER_CLOSED:
				IJ.removeEventListener(this);
				IJ.log("Color picker closed; Event_Listener stopped");
				break;
			case IJEventListener.LOG_WINDOW_CLOSED:
				IJ.removeEventListener(this);
				IJ.log("Log window closed; Event_Listener stopped");
				break;
		}
	}

}
