import ij.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/**
	Changes the canvas size of an image or stack without resizing the image.
	The border is filled with the current background color.
*/
public class Resize_Canvas implements PlugInFilter {

	ImagePlus imp;
		
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if (IJ.versionLessThan("1.17y"))
			return DONE;
		else
			return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		int wOld, hOld, wNew, hNew;
		boolean fIsStack = false;

		wOld = ip.getWidth();
		hOld = ip.getHeight();

		ImageStack stackOld = imp.getStack();
		if ((stackOld != null) && (stackOld.getSize() > 1))
			fIsStack = true;

		String[] sPositions = {
			"Top-Left", "Top-Center", "Top-Right", 
			"Center-Left", "Center", "Center-Right",
			"Bottom-Left", "Bottom-Center", "Bottom-Right"
		};
			
		String strTitle = fIsStack ? "Resize Stack Canvas" : "Resize Image Canvas";
		GenericDialog gd = new GenericDialog(strTitle);
		gd.addNumericField("Width:", wOld, 0);
		gd.addNumericField("Height:", hOld, 0);
		gd.addChoice("Position:", sPositions, sPositions[4]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
			
		wNew = (int)gd.getNextNumber();
		hNew = (int)gd.getNextNumber();
		int iPos = gd.getNextChoiceIndex();
		
		int xOff, yOff;
		int xC = (wNew - wOld)/2;	// offset for centered
		int xR = (wNew - wOld);		// offset for right
		int yC = (hNew - hOld)/2;	// offset for centered
		int yB = (hNew - hOld);		// offset for bottom
		
		switch(iPos) {
		case 0:	// TL
			xOff=0;	yOff=0; break;
		case 1:	// TC
			xOff=xC; yOff=0; break;
		case 2:	// TR
			xOff=xR; yOff=0; break;
		case 3: // CL
			xOff=0; yOff=yC; break;
		case 4: // C
			xOff=xC; yOff=yC; break;
		case 5:	// CR
			xOff=xR; yOff=yC; break;
		case 6: // BL
			xOff=0; yOff=yB; break;
		case 7: // BC
			xOff=xC; yOff=yB; break;
		case 8: // BR
			xOff=xR; yOff=yB; break;
		default: // center
			xOff=xC; yOff=yC; break;
		}
		
		String strOldTitle = imp.getWindow().getTitle();
		
		if (fIsStack) {
			ImageStack stackNew = expandStack(stackOld, wNew, hNew, xOff, yOff);
			new ImagePlus(strOldTitle + " copy", stackNew).show();
		} else {
			ImageProcessor newIP = expandImage(ip, wNew, hNew, xOff, yOff);
			new ImagePlus(strOldTitle + " copy", newIP).show();
		}
	}
	
	public ImageStack expandStack(ImageStack stackOld, int wNew, int hNew, int xOff, int yOff) {
		int nFrames = stackOld.getSize();
		ImageProcessor ipOld = stackOld.getProcessor(1);
		java.awt.Color colorBack = Toolbar.getBackgroundColor();
		
		ImageStack stackNew = new ImageStack(wNew, hNew, stackOld.getColorModel());
		ImageProcessor ipNew;
		
		for (int i=1; i<=nFrames; i++) {
			IJ.showProgress((double)i/nFrames);
			ipNew = ipOld.createProcessor(wNew, hNew);
			ipNew.setColor(colorBack);
			ipNew.fill();
			ipNew.insert(stackOld.getProcessor(i), xOff, yOff);
			stackNew.addSlice(null, ipNew);
		}
		return stackNew;
	}
	
	public ImageProcessor expandImage(ImageProcessor ipOld, int wNew, int hNew, int xOff, int yOff) {
		ImageProcessor ipNew = ipOld.createProcessor(wNew, hNew);
		ipNew.setColor(Toolbar.getBackgroundColor());
		ipNew.fill();
		ipNew.insert(ipOld, xOff, yOff);
		return ipNew;
	}

}

