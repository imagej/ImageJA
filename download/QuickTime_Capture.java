import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.util.*;
import ij.text.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;
import quicktime.*;
import quicktime.std.sg.*;
import quicktime.std.*;
import quicktime.qd.*;
import quicktime.util.*;
import quicktime.io.*;
import quicktime.std.image.*;

/**
Previews and captures a single video frame using QuickTime for Java.
Press the space bar to stop previewing. Press the alt key to 
capture a frame and continue previewing.
While previewing, type "+" to zoom in, "-" to zoom out,
and "h" to display a histogram. Captures and
displays a single frame if called with the argument "grab".
Based on the LiveCam example posted to the QuickTime for Java mailing list
by Jochen Broz.
http://lists.apple.com/archives/quicktime-java/2005/Feb/msg00062.html
*/
public class QuickTime_Capture implements PlugIn {

	SequenceGrabber grabber;
	SGVideoChannel channel;
	QDRect cameraSize;
	QDGraphics gWorld;
	public int[] pixelData;
	ImagePlus imp;
	int intsPerRow;
	int width, height;
	boolean grabbing = true;
	int frame;
	boolean grabMode;
	

	public void run(String arg) {
		String options = Macro.getOptions();
		if (options!=null && options.indexOf("grab")!=-1)
			arg = "grab";
		grabMode = arg.equals("grab");
		try {
			QTSession.open();
			initSequenceGrabber();
			width = cameraSize.getWidth();
			height = cameraSize.getHeight();
			intsPerRow = gWorld.getPixMap().getPixelData().getRowBytes()/4;
			ImageProcessor ip = new ColorProcessor(width, height);
			imp = new ImagePlus("Live (press space bar to stop)", ip);
			imp.show();
			pixelData = new int[intsPerRow*height];
			IJ.setKeyUp(KeyEvent.VK_ALT);
			IJ.setKeyUp(KeyEvent.VK_SPACE);
			if (IJ.debugMode) {
				IJ.log("Size: "+width+"x"+height);
				IJ.log("intsPerRow: "+intsPerRow);
			}
			preview();
		} catch(Exception e) {
			printStackTrace(e);
		} finally {
			QTSession.close();
		}
	}

	/**
	* Initializes the SequenceGrabber. Gets it's source video bounds, creates a gWorld with that size.
	* Configures the video channel for grabbing, previewing and playing during recording.
	*/
	private void initSequenceGrabber() throws Exception{
		grabber = new SequenceGrabber();
		SGVideoChannel channel = new SGVideoChannel(grabber);
		cameraSize = channel.getSrcVideoBounds();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		if (cameraSize.getHeight()>screen.height-40) // iSight camera claims to 1600x1200!
			cameraSize.resize(640, 480);
		gWorld =new QDGraphics(cameraSize);
		grabber.setGWorld(gWorld, null);
		channel.setBounds(cameraSize);
		channel.setUsage(quicktime.std.StdQTConstants.seqGrabRecord |
			quicktime.std.StdQTConstants.seqGrabPreview |
			quicktime.std.StdQTConstants.seqGrabPlayDuringRecord);
		channel.setFrameRate(0);
		channel.setCompressorType( quicktime.std.StdQTConstants.kComponentVideoCodecType);
	}

	private void initSequenceGrabber2() throws Exception{
		grabber = new SequenceGrabber();
		// added by Jeff Hardin to account for change in byte order on Intel Macs
		if (quicktime.util.EndianOrder.isNativeLittleEndian())
			gWorld = new QDGraphics(QDConstants.k32BGRAPixelFormat, cameraSize);
		else
			gWorld = new QDGraphics(QDGraphics.kDefaultPixelFormat, cameraSize);
		SGVideoChannel channel = new SGVideoChannel(grabber);
		cameraSize = channel.getSrcVideoBounds();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		if (cameraSize.getHeight()>screen.height-40) // iSight camera claims to 1600x1200!
			cameraSize.resize(640, 480);
		gWorld =new QDGraphics(cameraSize);
		grabber.setGWorld(gWorld, null);
		channel.setBounds(cameraSize);
		channel.setUsage(quicktime.std.StdQTConstants.seqGrabPreview);
		//channel.setFrameRate(0);
		//channel.setCompressorType( quicktime.std.StdQTConstants.kComponentVideoCodecType);
	}

	/**
	* This is a bit tricky. We do not start Previewing, but recording. By setting the output to a
	* dummy file (which will never be created (hope so)) with the
	* quicktime.std.StdQTConstants.seqGrabDontMakeMovie flag set. This seems to be equivalent to
	* preview mode with the advantage, that it refreshes correctly.
	*/
	void preview() throws Exception {
		QTFile movieFile = new QTFile(new java.io.File("NoFile"));
		grabber.setDataOutput( null, quicktime.std.StdQTConstants.seqGrabDontMakeMovie);
		grabber.prepare(true, true);
		grabber.startRecord();
		while(grabbing) {
			grabber.idle();
			grabber.update(null);
			displayFrame();
		}
	}

	void displayFrame() {
 		gWorld.getPixMap().getPixelData().copyToArray(0, pixelData, 0, pixelData.length);
		ImageProcessor ip = imp.getProcessor();
		int[] pixels = ip!=null?(int[])ip.getPixels():null;
		ImageWindow win = imp.getWindow();
		if (pixels==null || win==null || IJ.spaceBarDown()) {
			grabbing = false; 
			imp.setTitle("Untitled"); 
			return;
		}
		if (IJ.altKeyDown()) {
			IJ.setKeyUp(KeyEvent.VK_ALT);
			IJ.run("Add Slice");
		}
		if (intsPerRow!=width) {
			for (int i=0; i<height; i++)
			System.arraycopy(pixelData, i*intsPerRow, pixels, i*width, width);
		} else
			ip.setPixels(pixelData);
		imp.updateAndDraw();
		if (grabMode&&pixelData[0]!=0)
			{grabbing = false; imp.setTitle("Untitled"); return;}
		frame++;
		IJ.wait(10);
	}

	void printStackTrace(Exception e) {
		String msg = e.getMessage();
		if (msg!=null && msg.indexOf("-9405")>=0)			IJ.error("QT Capture", "QuickTime compatible camera not found");
		else {
			CharArrayWriter caw = new CharArrayWriter();
			PrintWriter pw = new PrintWriter(caw);
			e.printStackTrace(pw);
			String s = caw.toString();
			if (IJ.isMacintosh())
				s = Tools.fixNewLines(s);
			new TextWindow("Exception", s, 500, 300);
		}
	}

}

