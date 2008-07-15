import ij.*;

/**
 * This simple class can be called from an ImageJ macro via the "call" mechanism to
 * get and set the (string) properties of the active image (ImagePlus). 
 *
 * call("ImpProps.setProperty", "<key>", "<value>");
 *	set property <key> to <value>, returns <value> if successful, 
 *	"" otherwise (no active image)
 *
 * call("ImpProps.getProperty", "<key>");
 *	returns value of property <key> if set, "" otherwise (not found or no active image)
 *
 * @see    ij.ImagePlus#setProperty
 * @see    ij.ImagePlus#getProperty
 *
 * @author Joachim Wesner
 * @author Leica Microsystems CMS GmbH
 * @author joachim.wesner@leica-microsystems.com
 * @version 2008-3-15
 *
 * This class can be compiled with ImageJ's Plugins>Compile and Run command.
 */

public class ImpProps {

	public static String getProperty(String arg1) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return "";
		Object prop = imp.getProperty(arg1);
		if (prop != null && prop instanceof String)
			return (String)prop;
		else
			return "";
	}

	public static String setProperty(String arg1, String arg2) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return "";
		imp.setProperty(arg1, arg2);
		return arg2;
	}

}
