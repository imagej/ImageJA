import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.Editor;

public class Editor_Demo implements PlugIn {

	public void run(String arg) {
		String text =
			"This plugin demonstrates how to open an \n" +
			"editor window containing some text.\n" +
			"\n" +
			"This could be used to display online help";

		Editor ed = new Editor();
		ed.setSize(350, 300);
		ed.create("My Editor", text);
	}

}
