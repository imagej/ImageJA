package ij;

import ij.gui.MenuCanvas;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.MenuBar;
import java.awt.Panel;
import java.awt.ScrollPane;

	/** Runs ImageJ as an applet and optionally opens images 
		using URLs that are passed as a parameters. */
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
    }

    public void stop() {
	ImageJ ij = IJ.getInstance();
	ij.dispose();
    }
}

