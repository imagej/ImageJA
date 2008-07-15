import ij.*;
import ij.plugin.PlugIn;

/** This plugin closes all the image windows
 *  @author Hajime Hirase
 *  @version 1.0
*/

public class Window_Closer implements PlugIn {
    public void run(String arg) {
	WindowManager.closeAllWindows();
    }
}
