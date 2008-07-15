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

public class JMF_Movie_Reader extends PlugInFrame implements ControllerListener,  ActionListener {
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
  
	public JMF_Movie_Reader() {
		super("JMF Movie Reader");
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
		setLayout(new FlowLayout());
		Component vc;
		if ((vc = p.getVisualComponent()) != null) {
			add(vc);
		}

		Buffer frame = fgc.grabFrame();
		VideoFormat format = (VideoFormat)frame.getFormat();
		frameConverter = new BufferToImage(format);
		setVisible(true);
		//saveImage(1, frame);

		for (int i=0;i<totalFrames; i++) {
			int currentFrame = fpc.mapTimeToFrame(p.getMediaTime());
			IJ.showStatus((currentFrame+1)+"/"+totalFrames);
			IJ.showProgress((double)(currentFrame+1)/totalFrames);
			if (!saveImage(i+1, fgc.grabFrame()))
				break;
			fpc.skip(1);
		}
		if (stack!=null)
			new ImagePlus(name, stack).show();
		setVisible(false);
		dispose();
		IJ.showStatus("");

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
    
	private boolean saveImage(int count, Buffer frame) {
		Image img = frameConverter.createImage(frame);
		ImageProcessor ip = new ColorProcessor(img);
		if (count==1) {
			int width = ip.getWidth();
			int height = ip.getHeight();
			int size = (width*height*totalFrames*4)/(1024*1024);
			IJ.showStatus("Allocating "+width+"x"+height+"x"+totalFrames+" stack ("+size+"MB)");
			stack = allocateStack(width, height, totalFrames);
			if (stack==null) {
				IJ.outOfMemory("JMF Movie Opener");
				return false;
			}
			grayscale = isGrayscale(ip);
		}
		if (grayscale)
			ip = ip.convertToByte(false);
		stack.setPixels(ip.getPixels(), count);
		return true;
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
			p.setMediaTime(new Time(0));
			//p.start();
			//p.close();
			//System.exit(0);
		} else if (evt instanceof SizeChangeEvent) {
		}
	}
 
	ImageStack allocateStack(int width, int height, int size) {
		ImageStack stack=null;
		byte[] temp;
		try {
			stack = new ImageStack(width, height);
			for (int i=0; i<size; i++) {
				if (grayscale)
					stack.addSlice(null, new byte[width*height]);
				else
					stack.addSlice(null, new int[width*height]);
			}
			temp = new byte[width*height*4*5+1000000];
	 	}
		catch(OutOfMemoryError e) {
			if (stack!=null) {
				Object[] arrays = stack.getImageArray();
				if (arrays!=null)
					for (int i=0; i<arrays.length; i++)
				arrays[i] = null;
			}
			stack = null;
		}
		temp = null;
		System.gc();
		System.gc();
		return stack;
	}
	
	boolean isGrayscale(ImageProcessor ip) {
		int[] pixels = (int[])ip.getPixels();
		boolean grayscale = true;
		int c, r, g, b;
		for (int i=0; i<pixels.length; i++) {
			c = pixels[i];
			r = (c&0xff0000)>>16;
			g = (c&0xff00)>>8;
			b = c&0xff;
			if (r!=g || r!=b || g!=b) {
				grayscale = false;
				break;
			}
		}
		return grayscale;
	}

}
