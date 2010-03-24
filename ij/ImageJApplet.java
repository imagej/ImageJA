package ij;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.MenuCanvas;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.MenuBar;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
	Runs ImageJ as an applet and optionally opens up to 
	nine images using URLs passed as a parameters.
	<p>
	Here is an example applet tag that launches ImageJ as an applet
	and passes it the URLs of two images:
	<pre>
	&lt;applet archive="../ij.jar" code="ij.ImageJApplet.class" width=0 height=0&gt;
	&lt;param name=url1 value="http://rsb.info.nih.gov/ij/images/FluorescentCells.jpg"&gt;
	&lt;param name=url2 value="http://rsb.info.nih.gov/ij/images/blobs.gif"&gt;
	&lt;/applet&gt;
	</pre>
	To use plugins, add them to ij.jar and add entries to IJ_Props.txt file (in ij.jar) that will  
	create commands for them in the Plugins menu, or a submenu. There are examples 
	of such entries in IJ.Props.txt, in the "Plugins installed in the Plugins menu" section.
	<p>
	Macros contained in a file named "StartupMacros.txt", in the same directory as the HTML file
	containing the applet tag, will be installed on startup.
*/
public class ImageJApplet extends Applet {
    ScrollPane imagePane;
    Scrollbar scrollC, scrollZ, scrollT;
    int heightWithoutImage;
    Panel north;
    Panel scrollPane;
    MenuCanvas menu;
    ImagePlus image;

    public ImageJApplet() {
	setLayout(new BorderLayout());

	menu = new MenuCanvas();
	north = new Panel();
	north.setLayout(new BorderLayout());
	north.add(menu, BorderLayout.NORTH);
	add(north, BorderLayout.NORTH);

	imagePane = new ScrollPane();
	add(imagePane, BorderLayout.CENTER);

	AdjustmentListener listener = new AdjustmentListener() {
                public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
			if (image == null)
				return;
			int channel = scrollC.getValue();
			int slice = scrollZ.getValue();
			int frame = scrollT.getValue();
			image.setPosition(channel, slice, frame);
			imagePane.repaint();
		}
	};

	scrollPane = new Panel();
	scrollPane.setLayout(new GridBagLayout());
	GridBagConstraints c = new GridBagConstraints();
	scrollC = new Scrollbar(Scrollbar.HORIZONTAL);
	scrollC.addAdjustmentListener(listener);
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1;
	c.gridy = 0;
	scrollPane.add(scrollC, c);
	scrollZ = new Scrollbar(Scrollbar.HORIZONTAL);
	scrollZ.addAdjustmentListener(listener);
	c.gridy = 1;
	scrollPane.add(scrollZ, c);
        scrollPane.validate();
	scrollT = new Scrollbar(Scrollbar.HORIZONTAL);
	scrollT.addAdjustmentListener(listener);
	c.gridy = 2;
	scrollPane.add(scrollT, c);

	add(scrollPane, BorderLayout.SOUTH);
}

    public Component add(Component c) {
	if (north.getComponentCount() < 2)
		north.add(c, BorderLayout.SOUTH);
	else if (getComponentCount() < 3)
		add(c, BorderLayout.CENTER);
	else if (getComponentCount() < 4)
		add(c, BorderLayout.SOUTH);
	else
		IJ.error("Too many components!");
	return null;
    }

    public void pack() {
	north.doLayout();
	imagePane.doLayout();
	scrollPane.doLayout();
	doLayout();
    }

    public void setImageCanvas(Component c) {
	if (c != null) {
		imagePane.removeAll();
		imagePane.add(c);
		c.requestFocus();
		imagePane.repaint();
		if (c instanceof ImageCanvas) {
			image = ((ImageCanvas)c).getImage();

			scrollC.setMinimum(1);
			scrollC.setMaximum(image.getNChannels()+1);
			scrollC.setVisible(image.getNChannels() > 1);

			scrollZ.setMinimum(1);
			scrollZ.setMaximum(image.getNSlices()+1);
			scrollZ.setVisible(image.getNSlices() > 1);

			scrollT.setMinimum(1);
			scrollT.setMaximum(image.getNFrames()+1);
			scrollT.setVisible(image.getNFrames() > 1);
			pack();
		}
	}
    }

    public String getURLParameter(String key) {
	    String url = getParameter(key);
	    if (url==null)
			return null;
	    if (url.indexOf(":/") < 0)
		    url = getCodeBase().toString() + url;
	    if (url.indexOf("://") < 0) {
		    int index = url.indexOf(":/");
		    if (index > 0)
			    url = url.substring(0, index) + ":///"
				    + url.substring(index + 2);
	    }
		return url;
    }

	/** Starts ImageJ if it's not already running. */
    public void init() {
    	ImageJ ij = IJ.getInstance();
     	if (ij==null || (ij!=null && !ij.isShowing()))
			new ImageJ(this);
		for (int i=1; i<=9; i++) {
			String url = getURLParameter("url"+i);
			if (url==null) break;
			ImagePlus imp = new ImagePlus(url);
			if (imp!=null) imp.show();
		}
		/** Also look for up to 9 macros to run. */
		for (int i=1; i<=9; i++) {
			String url = getURLParameter("macro"+i);
			if (url==null) break;
			try {
				InputStream in = new URL(url).openStream();
				BufferedReader br = new BufferedReader(new
						InputStreamReader(in));
				StringBuffer sb = new StringBuffer() ;
				String line;
				while ((line=br.readLine()) != null)
					sb.append (line + "\n");
				in.close ();
				IJ.runMacro(sb.toString());
			} catch (Exception e) {
				IJ.write("warning: " + e);
			}
		}
		/** Also look for up to 9 expressions to evaluate. */
		for (int i=1; i<=9; i++) {
			String macroExpression = getParameter("eval"+i);
			if (macroExpression==null) break;
			IJ.runMacro(macroExpression);
		}
    }
    
    public void destroy() {
    	ImageJ ij = IJ.getInstance();
    	if (ij!=null) ij.quit();
    }

    public void stop() {
	ImageJ ij = IJ.getInstance();
	ij.dispose();
    }

    public void open(String url) {
        IJ.open(url);
    }

    public void eval(String expression) {
        IJ.runMacro(expression);
    }
}
