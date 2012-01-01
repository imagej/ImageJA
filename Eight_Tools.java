import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.tool.PlugInTool;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;


/** This plugin demonstrates how to add multiple tools to the ImageJ toolbar.
	It requires ImageJ 1.46d or later. */
public class Eight_Tools extends PlugInFrame implements ActionListener, IJEventListener {
	private static Frame instance;

	public Eight_Tools() {
		super("Eight_Tools");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		instance = this;
		addKeyListener(IJ.getInstance());
		IJ.addEventListener(this);
		setLayout(new FlowLayout());
		Panel panel = new Panel();
		panel.setLayout(new GridLayout(2, 1, 10, 10));
		Button b = new Button("Add Custom Tools");
		b.addActionListener(this);
		panel.add(b);
		b = new Button("Restore Default Tools");
		b.addActionListener(this);
		panel.add(b);
		add(panel);
		pack();
		GUI.center(this);
		setVisible(true);
	}

	private void addTools() {
		Toolbar.removeMacroTools();
		for (int n=1; n<=8; n++)
			new Tool(n);
	}

	private void restoreTools() {
		String path = IJ.getDirectory("macros")+"StartupMacros.txt";
		File f = new File(path);
		if (!f.exists() && path.contains("Fiji"))
			path = IJ.getDirectory("macros")+"StartupMacros.fiji.ijm";
		IJ.run("Install...", "install="+path);
	}
	
	public Insets getInsets() {
    		Insets i= super.getInsets();
    		return new Insets(i.top+10, i.left+10, i.bottom+10, i.right+10);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().startsWith("Add"))
			addTools();
		else
			restoreTools();
	}

	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) {
			instance = null;	
			IJ.removeEventListener(this);
			restoreTools();
		}
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
		}
	}

	class Tool extends PlugInTool {
		int toolNumber;
	
		Tool(int toolNumber) {
			this.toolNumber = toolNumber;
			run("");
		}
	
		public void mousePressed(ImagePlus imp, MouseEvent e) {
			show(imp, e, "clicked");
		}
	
		public void mouseDragged(ImagePlus imp, MouseEvent e) {
			show(imp, e, "dragged");
		}
	
		public void showOptionsDialog() {
			IJ.log("User double clicked on the tool icon");
		}
	
		void show(ImagePlus imp, MouseEvent e, String msg) {
			ImageCanvas ic = imp.getCanvas();
			int x = ic.offScreenX(e.getX());
			int y = ic.offScreenY(e.getY());
			IJ.log("Tool "+toolNumber+" "+msg+" at ("+x+","+y+") on "+imp.getTitle());
		}
	
		public String getToolName() {
			return "Custom Tool "+toolNumber;
		}
	
		public String getToolIcon() {
			return "C00aT0f18"+toolNumber;
		}
	
	}

}




