import ij.*;
import ij.plugin.*;
import ij.io.LogStream;
import java.io.File;
import java.util.Properties;
import org.python.util.PythonInterpreter;
import org.python.core.PyObject;

/** This plugin runs Jython scripts. */
public class Jython extends PlugInInterpreter implements PlugIn, Runnable {
	public static final String imports =
		"from ij import *\n"+
		"from ij.gui import *\n"+
		"from ij.process import *\n"+
		"from ij.measure import *\n"+
		"from ij.util import *\n"+
		"from ij.plugin import *\n"+
		"from ij.plugin.filter import *\n"+
		"from ij.plugin.frame import *\n"+
		"from ij.io import *\n"+
		"from java.lang import *\n"+
		"from java.awt import *\n"+
		"from java.awt.image import *\n"+
		"from java.awt.geom import *\n"+
		"from java.util import *\n"+
		"from java.io import *\n";

	public static final String name = "Jython";
	private Thread thread;
	private String script;
	private String arg;
	private String output;
	private static int counter;
	private PyObject result;

	// run script on separate thread
	public void run(String script) {
		if (script.equals(""))
			return;
		this.script = script;
		thread = new Thread(this, name); 
		thread.setPriority(Math.max(thread.getPriority()-2, Thread.MIN_PRIORITY));
		thread.start();
	}
	
	// run script on current thread
	public String run(String script, String arg) {
		this.script = script;
		this.arg = arg;
		run();
		return null;
	}

	public String getReturnValue() {
		return result!=null?""+result:"";
	}

	public String getImports() {
		return imports;
	}

	public String getVersion() {
		return "1.47n";
	}

	public String getName() {
		return "Jython";
	}

	public void run() {
		if (counter==0)
			IJ.showStatus("Starting Jython...");
		String cacheDir = System.getProperty("user.home")+File.separator+".imagej";
		File f = new File(cacheDir);
		if (!f.exists())
			f.mkdir();
		Properties props = System.getProperties();
		props.put("python.home", cacheDir);
		props.put("python.cachedir.skip", "false");
		PythonInterpreter py = new PythonInterpreter();
		LogStream stream = new LogStream();
		py.setOut(stream);
		py.setErr(stream);
		try {
			py.exec(imports);
			if (arg==null) arg="";
			py.exec("def getArgument():\n   return \""+arg+"\"");
			py.exec(script);
			result = py.get("result");
		} catch(Throwable e) {
			String msg = ""+e;
			if (!msg.contains(Macro.MACRO_CANCELED))
				IJ.log(msg);
		}
		stream.close();
		if (counter++==0)
			IJ.showStatus("");
	}	

}
