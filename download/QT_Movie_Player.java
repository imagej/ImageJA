import ij.*; 
import ij.plugin.frame.*;
import ij.util.Tools;
import ij.text.TextWindow;
import ij.gui.GUI;
import ij.process.*;
import ij.io.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*; 
import java.io.*;

import quicktime.*;
import quicktime.io.*;
import quicktime.std.movies.*;
import quicktime.std.movies.media.*;
import quicktime.app.view.*;
import quicktime.qd.*;

//Revised code to display a QuickTime movie in a window under Java 1.4.x and QuickTime 6.4, aka QTJ 6.1
//Version 1.0 beta 1, November 22, 2003 by Jeff Hardin, Dept. of Zoology, Univ. of Wisconsin

//Adapted from basic code courtesy of Chris Adamson, c/o the O'Reilly Network
//Window resize detection appears broken under Mac OS X 10.3, JavaVM 1.4.1 for the moment

public class QT_Movie_Player extends PlugInFrame implements KeyEventDispatcher {

	public Movie m;
	public Component c;
	public MovieController mc;
	boolean done;
	double timeScale;
	double previousTime = -1;
	int n;

	public QT_Movie_Player() {
		super("Player");
		OpenDialog od = new OpenDialog("Open QuickTime", "");
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name==null) return;
		try {
			QTSession.open();
			QTFile f = new QTFile(directory+name);
			if (f==null) 
				{QTSession.close(); return;}
			OpenMovieFile omf = OpenMovieFile.asRead (f);
			m = Movie.fromFile(omf);
			mc = new MovieController (m);
			mc.setVisible(true);
			//need the next line to send key events to the movie controller so we can simulate the arrow key functions Apple broke
			mc.setKeysEnabled(true);
			c = QTFactory.makeQTComponent(mc).asComponent();
			c.setName("Controller");
			setTitle(f.getName());
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);			add(c);
			pack();
			setResizable(false);
			GUI.center(this);
			setVisible(true);
			monitor();
		} catch (Exception e) {
			QTSession.close();
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);  
			printStackTrace(e);
		}
	}

	void monitor() throws Exception {  
		timeScale = m.getTimeScale();
		double time;
		while (!done) {
			time = m.getTime()/timeScale;
			if (time!=previousTime) {
				IJ.showStatus( getTimeString(time));
				previousTime = time;
			}
			IJ.wait(10);
		}
	}

	String getTimeString(double time) throws Exception {  
		int minutes = (int)time/60;
		String s1 = minutes + ":";
		if (s1.length()==2)
			s1 = "0" + s1;
		String s2 = IJ.d2s(time-minutes*60,2);
		if (s2.charAt(1)=='.')
			s2 = "0" + s2;
		return s1 + s2;
	}
    
	public void close() {
		done = true;
		QTSession.close();
		setVisible(false);
		dispose();
		WindowManager.removeWindow(this);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);  
	}
				
	void keyPressed (KeyEvent e) {
		//IJ.log("keyPressed: "+e);
		try {
			//need to pass MovieController.key() an int corresponding to standard Java key code; initialize it here
			int myInt = 0;
			switch (e.getKeyCode()) {
				case KeyEvent.VK_SPACE:
					if (m.getRate() != 0) 
						m.setRate (0);
					else
						m.setRate (1);
					break;
				case KeyEvent.VK_LEFT:
					myInt = KeyEvent.VK_LEFT;
					mc.key(myInt,e.getModifiers());                                  
					break;
				case KeyEvent.VK_RIGHT:
					myInt = KeyEvent.VK_RIGHT;
					mc.key (myInt,e.getModifiers());
					break;
				case KeyEvent.VK_G:
					grab();
					break;
				default:
					break;
			}
		} catch (Exception ee) {
			printStackTrace(ee);
		}
	}
	
	void grab() {
		Point loc = getLocation();
		Dimension size = getSize();
		try {
			Robot robot = new Robot();
			Rectangle r = new Rectangle(loc.x, loc.y+22, size.width, size.height-22-16);
			Image img = robot.createScreenCapture(r);
			if (img!=null)
				new ImagePlus(getTimeString(m.getTime()/timeScale), img).show();
		} catch(Exception e) { }
	}
 
	void grab2() throws Exception {
		Point loc = getLocation();
		Dimension size = getSize();
		int width = size.width;
		int height = size.height-38;
		QDGraphics gWorld = m.getGWorld();
		int intsPerRow = gWorld.getPixMap().getPixelData().getRowBytes()/4;
		int[] pixelData = new int[intsPerRow*height];
 		gWorld.getPixMap().getPixelData().copyToArray(0, pixelData, 0, pixelData.length);
		//IJ.log("Grab: "+gWorld+"   "+gWorld.getPixMap());
		int[] pixels = new int[width*height];
		for (int i=0; i<height; i++)
			System.arraycopy(pixelData, i*intsPerRow+loc.x, pixels, i*width, width);
		pixelData = pixels;
		ImageProcessor ip= new ColorProcessor(width, height, pixelData);
		new ImagePlus("Frame", ip).show();
	}
                    
	void printStackTrace(Exception e) {
		if (e.toString().indexOf("userCanceledErr")>=0)
			return;
		CharArrayWriter caw = new CharArrayWriter();
		PrintWriter pw = new PrintWriter(caw);
		e.printStackTrace(pw);
		String s = caw.toString();
		if (IJ.isMacintosh())
		s = Tools.fixNewLines(s);
		new TextWindow("Exception", s, 500, 300);
	}

	public boolean dispatchKeyEvent(KeyEvent e) {
		//IJ.log("dispatchKeyEvent: "+e);
		if (!done && e.getID()==KeyEvent.KEY_PRESSED)
			keyPressed(e);
		return false;
	}

}






