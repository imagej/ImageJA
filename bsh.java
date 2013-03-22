import ij.*;
import ij.plugin.*;
import bsh.*;

/** This plugin runs BeanShell scripts. */
public class bsh extends PlugInInterpreter implements PlugIn, Runnable {
	public static String imports =
		"import ij.*;"+
		"import ij.gui.*;"+
		"import ij.process.*;"+
		"import ij.measure.*;"+
		"import ij.util.*;"+
		"import ij.plugin.*;"+
		"import ij.io.*;"+
		"import ij.plugin.filter.*;"+
		"import ij.plugin.frame.*;"+
		"import java.lang.*;"+
		"import java.awt.*;"+
		"import java.awt.image.*;"+
		"import java.awt.geom.*;"+
		"import java.util.*;"+
		"import java.io.*;"+
		"print(arg){IJ.log(\"\"+arg);} ";

	private static String defaultGetArg = "getArgument(){return \"\";} ";
	private Thread thread;
	private String script;
	private String arg;
	private Object result;

	// run script on separate thread
	public void run(String script) {
		if (script.equals(""))
			return;
		this.script = script;
		thread = new Thread(this, "BeanShell"); 
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

	public String getName() {
		return "BeanShell";
	}

	public String getVersion() {
		return "1.47n";
	}

	public String getImports() {
		return imports;
	}

	public void run() {
		try {
			Interpreter bsh = new Interpreter();
			if (arg!=null && !arg.equals("")) {
				String getArg = "getArgument(){return \""+arg+"\";} ";
				result = bsh.eval(imports+getArg+script);
			} else
				result = bsh.eval(imports+defaultGetArg+script);
		} catch(Throwable e) {
			String msg = e.getMessage();
			if (msg!=null && msg.contains("import ij.*")) {
				int index = msg.indexOf(" : ");
				if (index!=-1)
					msg = msg.substring(index+3);
				int line = ((EvalError)e).getErrorLineNumber();
				IJ.log(msg+" in line number "+line);
			}
		}
	}	

}
