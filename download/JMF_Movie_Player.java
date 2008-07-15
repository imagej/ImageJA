//JMF_Movie_Player plugin
//Version 0.1, May 1, 2004 by Jeff Hardin, Dept. of Zoology, Univ. of Wisconsin
//Questions? Contact the author at jdhardin@wisc.edu
//Code based on the JMF_Movie_Reader plugin by Wayne Rasband, NIH
//Note: JMF is not very robust in terms of the codecs it supports; so caveat emptor!


import ij.*;
import ij.plugin.frame.*;
import ij.process.*;
import ij.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import javax.media.*;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.*;
import javax.media.util.BufferToImage;

public class JMF_Movie_Player extends PlugInFrame implements ControllerListener,  ActionListener {
	Player p;
	FramePositioningControl fpc;
	FrameGrabbingControl fgc;
	Object waitSync = new Object();
	boolean stateTransitionOK = true;
	int totalFrames = FramePositioningControl.FRAME_UNKNOWN;
	BufferToImage frameConverter;
	String name;
	boolean grayscale = false;
	ImageStack stack;
  
	public JMF_Movie_Player() {
		super("JMF Movie Player");
	}
    
	public void run(String arg) {
		IJ.showStatus("");
		OpenDialog od = new OpenDialog("Open Movie...", "");
		String dir = od.getDirectory();
		name = od.getFileName();
		if (name==null)
			return;
		String path = dir + name;
		String url = encodeURL("file://"+path);
		//IJ.write("path: "+path);
		//IJ.write("url: "+url);
		//url = "http://rsb.info.nih.gov/movie.avi";
		//url = "file:///Macintosh HD/Desktop%20Folder/test.mov";
		MediaLocator ml;
		if ((ml = new MediaLocator(url)) == null) {
			IJ.write("Cannot build media locator from: ");
			return;
		}
		DataSource ds = null;
		// Create a DataSource given the media locator.
		IJ.showStatus("creating JMF data source");
		try {
		ds = Manager.createDataSource(ml);
		} catch (Exception e) {
			IJ.write("Cannot create DataSource from: " + ml);
			return;
		}
 		openMovie(ds);
	}
	
	public String encodeURL(String url) {
		int index = 0;
		while (index>-1) {
			index = url.indexOf(' ');
			if (index>-1)
				url = url.substring(0,index)+"%20"+url.substring(index+1,url.length());
		}
		return url;
	}

	public boolean openMovie(DataSource ds) {

		IJ.showStatus("opening: "+ds.getContentType());
		try {
			p = Manager.createPlayer(ds);
		} catch (Exception e) {
			error("Failed to create a player from the given DataSource:\n \n" + e.getMessage());
			return false;
		}

		p.addControllerListener(this);
		p.realize();
		if (!waitForState(p.Realized)) {
			error("Failed to realize the JMF player.");
			return false;
		}

		// Try to retrieve a FramePositioningControl from the player.
		fpc = (FramePositioningControl)p.getControl("javax.media.control.FramePositioningControl");
		if (fpc == null) {
			error("The player does not support FramePositioningControl.");
			return false;
		}

		// Try to retrieve a FrameGrabbingControl from the player.
		fgc = (FrameGrabbingControl)p.getControl("javax.media.control.FrameGrabbingControl");
		if (fgc == null) {
			error("The player does not support FrameGrabbingControl.");
			return false;
		}

		Time duration = p.getDuration();
		if (duration != Duration.DURATION_UNKNOWN) {
			//IJ.write("Movie duration: " + duration.getSeconds());
			totalFrames = fpc.mapTimeToFrame(duration)+1;
			if (totalFrames==FramePositioningControl.FRAME_UNKNOWN)
			IJ.write("The FramePositioningControl does not support mapTimeToFrame.");
		} else {
			IJ.write("Movie duration: unknown");
		}

		// Prefetch the player.
		p.prefetch();
		if (!waitForState(p.Prefetched)) {
			error("Failed to prefetch the player.");
			return false;
		}

		// Display the visual & control component if there's one.
		setLayout(new BorderLayout());
		Component vc;
		Component cc;
		if ((vc = p.getVisualComponent()) != null) {
			add(vc, BorderLayout.CENTER);
		}
		
		//show controller for movie
		if ((cc = p.getControlPanelComponent()) != null) {
             add (cc, BorderLayout.SOUTH);
		}
		
		pack(); 
		//setVisible(true);
		//following gets the first frame to show up!
		p.start();
		p.stop();
		p.setMediaTime(new Time(0));
		setVisible(true);
		return true;

	}

	public void addNotify() {
		super.addNotify();
		pack();
	}

	/** Block until the player has transitioned to the given state. */
	boolean waitForState(int state) {
		synchronized (waitSync) {
			try {
				while (p.getState() < state && stateTransitionOK)
				waitSync.wait();
 			} catch (Exception e) {}
		}
		return stateTransitionOK;
	}

	public void actionPerformed(ActionEvent ae) {
		int currentFrame = fpc.mapTimeToFrame(p.getMediaTime());
		if (currentFrame != FramePositioningControl.FRAME_UNKNOWN)
		IJ.write("Current frame: " + currentFrame);
	}

	private void error(String msg) {
		IJ.showMessage("JMF Movie Reader", msg);
		IJ.showStatus("");
	}
    

	/**  Controller Listener */
	public void controllerUpdate(ControllerEvent evt) {

		if (evt instanceof ConfigureCompleteEvent ||
		evt instanceof RealizeCompleteEvent ||
		evt instanceof PrefetchCompleteEvent) {
			synchronized (waitSync) {
			stateTransitionOK = true;
			waitSync.notifyAll();
		}
		} else if (evt instanceof ResourceUnavailableEvent) {
			synchronized (waitSync) {
				stateTransitionOK = false;
				waitSync.notifyAll();
			}
		} else if (evt instanceof EndOfMediaEvent) {
			//p.setMediaTime(new Time(0));
			//p.start();
			//p.close();
			//System.exit(0);
		} else if (evt instanceof SizeChangeEvent) {
		}
	}
 
}
