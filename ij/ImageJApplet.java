package ij;

import ij.gui.MenuCanvas;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.MenuBar;
import java.awt.Panel;
import java.awt.ScrollPane;
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
    int heightWithoutImage;
    Panel north;
    MenuCanvas menu;

    public ImageJApplet() {
	setLayout(new BorderLayout());

	menu = new MenuCanvas();
	north = new Panel();
	north.setLayout(new BorderLayout());
	north.add(menu, BorderLayout.NORTH);
	add(north, BorderLayout.NORTH);

	imagePane = new ScrollPane();
	add(imagePane, BorderLayout.CENTER);
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
	doLayout();
    }

    public void setImageCanvas(Component c) {
	if (c != null) {
		imagePane.removeAll();
		imagePane.add(c);
		c.requestFocus();
		imagePane.repaint();
	}
    }

	/** Starts ImageJ if it's not already running. */
    public void init() {
    	ImageJ ij = IJ.getInstance();
     	if (ij==null || (ij!=null && !ij.isShowing()))
			new ImageJ(this);
		for (int i=1; i<=9; i++) {
			String url = getParameter("url"+i);
			if (url==null) break;
			if (url.indexOf(":/") < 0)
				url = getCodeBase().toString() + url;
			if (url.indexOf("://") < 0) {
				int index = url.indexOf(":/");
				if (index > 0)
					url = url.substring(0, index) + ":///"
						+ url.substring(index + 2);
			}
			ImagePlus imp = new ImagePlus(url);
			if (imp!=null) imp.show();
		}
		/** Also look for up to 9 macros to run. */
		for (int i=1; i<=9; i++) {
			String url = getParameter("macro"+i);
			if (url==null) break;
			if (url.indexOf(":/") < 0)
				url = getCodeBase().toString() + url;
			if (url.indexOf("://") < 0) {
				int index = url.indexOf(":/");
				if (index > 0)
					url = url.substring(0, index) + ":///"
						+ url.substring(index + 2);
			}
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
