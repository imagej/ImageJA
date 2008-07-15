import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.event.*;
import java.awt.Cursor;

	/**
	 * This plugin implements a Paintbrush tool that can be toggled on and
	 * off in each image window. The brush size depends on the current
	 * line width.
	 */
	public class Toggle_Paintbrush_ implements PlugInFilter, MouseListener, MouseMotionListener {
		ImageWindow		imageWin;
		ImagePlus		currentImagePlus;
		ImageProcessor	currentProcessor;
		ImageCanvas		imageCanvas;

		int				iLastX;
		int				iLastY;
		MouseListener[]			oldML;
		MouseMotionListener[]	oldMML;
		String			oldTitle;
		protected static Cursor paintCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);

	public int setup(String arg, ImagePlus imp) {
		if (System.getProperty("java.version").startsWith("1.1")) {
			IJ.showMessage("Toggle_Paintbrush", "This plugin requires Java 2 (e.g. JDK 1.3).");
			return DONE;
		}
		if (imp!=null) {
			imageWin = imp.getWindow();
			imageCanvas = imageWin.getCanvas();
		}
		currentImagePlus = null;
		currentProcessor = null;

		return DOES_ALL;
	}


	public void run(ImageProcessor ip) {
		// See if there is already a Toggle_Paintbrush_ mouse listener
		// associated with this imageCanvas
		MouseListener[] newML = (MouseListener[])(imageCanvas.getListeners(MouseListener.class));
		boolean fOldFound = false;
		for (int i=0; i<newML.length; i++) {
			if (newML[i].getClass() == Toggle_Paintbrush_ .class) {
				// we found one
				((Toggle_Paintbrush_ )newML[i]).removeYourself();
				fOldFound = true;
			}
		}

		if (fOldFound) {
			// we just toggled Toggle_Paintbrush_ off for this imageCanvas.
			// don't toggle it back on
			return;
		} 

		oldTitle = imageWin.getTitle();
		imageWin.setTitle(oldTitle+" PAINTING");


		// toggle the drawing on

		// save the old listeners
		oldML = (MouseListener[])(imageCanvas.getListeners(MouseListener.class));
		oldMML = (MouseMotionListener[])(imageCanvas.getListeners(MouseMotionListener.class));
		// remove the old listeners
		for (int i=0; i<oldML.length; i++) {
			imageCanvas.removeMouseListener(oldML[i]);
		}
		for (int i=0; i<oldMML.length; i++) {
			imageCanvas.removeMouseMotionListener(oldMML[i]);
		}
		// I will be the only mouse listener
		imageCanvas.addMouseListener(this);
		imageCanvas.addMouseMotionListener(this);

	}

	public void removeYourself() {
		imageWin.setTitle(oldTitle);

		// toggle the drawing off
		imageCanvas.removeMouseListener(this);
		imageCanvas.removeMouseMotionListener(this);

		// restore the old listeners
		for (int i=0; i<oldML.length; i++) {
			imageCanvas.addMouseListener(oldML[i]);
		}
		for (int i=0; i<oldMML.length; i++) {
			imageCanvas.addMouseMotionListener(oldMML[i]);
		}
	}
	
	public void mousePressed(MouseEvent e) {
		currentImagePlus = imageWin.getImagePlus();
		currentProcessor = currentImagePlus.getProcessor();
		currentProcessor.snapshot();
		Undo.setup(Undo.FILTER, currentImagePlus);
		iLastX = imageCanvas.offScreenX(e.getX());
		iLastY = imageCanvas.offScreenY(e.getY());

		if (IJ.altKeyDown()) {
			currentImagePlus.setColor(Toolbar.getBackgroundColor());
		} else {
			currentImagePlus.setColor(Toolbar.getForegroundColor());
		}
		currentProcessor.moveTo(iLastX, iLastY);
	}

	public void mouseDragged(MouseEvent e) {
		iLastX = imageCanvas.offScreenX(e.getX());
		iLastY = imageCanvas.offScreenY(e.getY());
		currentProcessor.lineTo(iLastX, iLastY);
		currentImagePlus.updateAndDraw();
	}

	public void mouseMoved(MouseEvent e) {
		imageCanvas.setCursor(paintCursor);
	}

	public void mouseReleased(MouseEvent e) {
		currentImagePlus = null;
		currentProcessor = null;
	}
	
	public void mouseExited(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}
	
	public void mouseEntered(MouseEvent e) {}
}


