package ij;
import ij.util.Tools;
import ij.text.TextWindow;
import ij.plugin.MacroInstaller;
import ij.plugin.frame.Recorder;
import java.io.*;
import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

/** Runs ImageJ menu commands in a separate thread.*/
public class Executer implements Runnable {

	private static String previousCommand;
	private static CommandListener listener;
	private static Vector listeners = new Vector();
	
	private String command;
	private Thread thread;
	
	/** Create an Executer to run the specified menu command
		in this thread using the active image. */
	public Executer(String cmd) {
		command = cmd;
	}

	/** Create an Executer that runs the specified menu command
		in a separate thread using the active image image. */
	public Executer(String cmd, ImagePlus ignored) {
		if (cmd.startsWith("Repeat")) {
			command = previousCommand;
			IJ.setKeyUp(KeyEvent.VK_SHIFT);		
		} else {
			command = cmd;
			if (!(cmd.equals("Undo")||cmd.equals("Close")))
				previousCommand = cmd;
		}
		IJ.resetEscape();
		thread = new Thread(this, cmd);
		thread.setPriority(Math.max(thread.getPriority()-2, Thread.MIN_PRIORITY));
		thread.start();
	}

	void notifyCommandListeners(Command cmd, int action) {
		if (listeners.size()>0) synchronized (listeners) {
			for (int i=0; i<listeners.size(); i++) {
				CommandListener listener = (CommandListener)listeners.elementAt(i);
				if (listener instanceof CommandListenerPlus)
					((CommandListenerPlus)listener).stateChanged(cmd, action);
			}
		}
	}

	public void run() {
		if (command==null) return;
		Command cmd = new Command(command);
		if (listeners.size()>0) synchronized (listeners) {
			for (int i=0; i<listeners.size(); i++) {
				CommandListener listener = (CommandListener)listeners.elementAt(i);
				cmd.command = listener.commandExecuting(cmd.command);
				if (listener instanceof CommandListenerPlus) {
					((CommandListenerPlus)listener).stateChanged(cmd, CommandListenerPlus.CMD_REQUESTED);
					if (cmd.isConsumed()) return;
				}
				if (cmd.command==null) return;
			}
		}
		cmd.modifiers = (IJ.altKeyDown()?ActionEvent.ALT_MASK:0)|(IJ.shiftKeyDown()?ActionEvent.SHIFT_MASK:0);
		try {
			if (Recorder.record) {
				Recorder.setCommand(cmd.command);
				runCommand(cmd);
				Recorder.saveCommand();
			} else
				runCommand(cmd);
		} catch(Throwable e) {
			IJ.showStatus("");
			IJ.showProgress(1.0);
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp!=null) imp.unlock();
			String msg = e.getMessage();
			if (e instanceof OutOfMemoryError) {
				IJ.outOfMemory(command);
				notifyCommandListeners(cmd, CommandListenerPlus.CMD_ERROR);
			} else if (e instanceof RuntimeException && msg!=null && msg.equals(Macro.MACRO_CANCELED)) {
				notifyCommandListeners(cmd, CommandListenerPlus.CMD_CANCELED);
				; //do nothing
			} else {
				CharArrayWriter caw = new CharArrayWriter();
				PrintWriter pw = new PrintWriter(caw);
				e.printStackTrace(pw);
				String s = caw.toString();
				if (IJ.isMacintosh()) {
					if (s.indexOf("ThreadDeath")>0)
						return;
					s = Tools.fixNewLines(s);
				}
				if (IJ.getInstance()!=null)
					new TextWindow("Exception", s, 350, 250);
				else
					IJ.log(s);
				notifyCommandListeners(cmd, CommandListenerPlus.CMD_ERROR);
			}
			IJ.abort();
		}
	}
	
    void runCommand(Command cmd) {
		Hashtable table = Menus.getCommands();
		cmd.className = (String)table.get(cmd.command);
		if (cmd.className!=null) {
			cmd.arg = "";
			if (cmd.className.endsWith("\")")) {
				// extract string argument (e.g. className("arg"))
				int argStart = cmd.className.lastIndexOf("(\"");
				if (argStart>0) {
					cmd.arg = cmd.className.substring(argStart+2, cmd.className.length()-2);
					cmd.className = cmd.className.substring(0, argStart);
				}
			}
			notifyCommandListeners(cmd, CommandListenerPlus.CMD_READY);
			if (cmd.isConsumed()) return; // last chance to interrupt
			if (IJ.shiftKeyDown() && cmd.className.startsWith("ij.plugin.Macro_Runner")) {
    				IJ.open(IJ.getDirectory("plugins")+cmd.arg);
				IJ.setKeyUp(KeyEvent.VK_SHIFT);		
    			} else {
				cmd.plugin = IJ.runPlugIn(cmd.command, cmd.className, cmd.arg);
			}
			notifyCommandListeners(cmd, CommandListenerPlus.CMD_STARTED);
		} else {
			notifyCommandListeners(cmd, CommandListenerPlus.CMD_READY);
			// Is this command in Plugins>Macros?
			if (MacroInstaller.runMacroCommand(cmd.command)) {
				notifyCommandListeners(cmd, CommandListenerPlus.CMD_MACRO);
				return;
			}
			// Is this command a LUT name?
			String path = Prefs.getHomeDir()+File.separator+"luts"+File.separator+cmd.command+".lut";
			File f = new File(path);
			if (f.exists()) {
				IJ.open(path);
				notifyCommandListeners(cmd, CommandListenerPlus.CMD_LUT);
			}
			else
				IJ.error("Unrecognized command: " + cmd.command);
	 	}
		notifyCommandListeners(cmd, CommandListenerPlus.CMD_FINISHED);
	}

	/** Returns the last command executed. Returns null
		if no command has been executed. */
	public static String getCommand() {
		return previousCommand;
	}
	
	/** Adds the specified command listener. */
	public static void addCommandListener(CommandListener listener) {
		listeners.addElement(listener);
	}
	
	/** Removes the specified command listener. */
	public static void removeCommandListener(CommandListener listener) {
		listeners.removeElement(listener);
	}

}


