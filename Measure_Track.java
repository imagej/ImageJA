import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.text.*;
import java.awt.*;
import ij.plugin.filter.*;


/**
       Measures the z-profile of the ROI of a stack,
       except for each slice the ROI is moved by the displacement
           (x[i], y[i]) - (x[orig], y[orig])
       with x and y taken from selected columnns of the Results table.
       
       Intended for use with the output of Tracker_.class
           http://rsbweb.nih.gov/ij/plugins/tracker.html
	   
       (c) 2009 Chris Nicolai
           http://www.qub.buffalo.edu
*/

public class Measure_Track implements PlugInFilter {
    ImagePlus imp;
    
    // it remembers column names (until quit)
    static String last_xname = "X1";
    static String last_yname = "Y1";
    
    public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
	return DOES_ALL; // TELLS_LIES
    }
    
    public void run(ImageProcessor ip) {
	// prompt for column names
	GenericDialog gd = new GenericDialog("Measure Track");
	gd.addStringField("X column: ", last_xname);
	gd.addStringField("Y column: ", last_yname);
	gd.showDialog();
	if (gd.wasCanceled())
	    return;
	String xname = last_xname = gd.getNextString();
	String yname = last_yname = gd.getNextString();

	int nFrames = imp.getStackSize();
	
	// find the Results window with the named columns
	// (Tracker_ appears to use a results panel other than the main one.)
	TextPanel results = null;
	boolean use_x = ! xname.equals("");
	boolean use_y = ! yname.equals("");
	int col_x = 0;
	int col_y = 0;
	Frame[] windows = WindowManager.getNonImageWindows();
	for (int w=0; w<windows.length; w++) {
	    if ( windows[w] instanceof TextWindow ) {
		TextPanel text_panel = ((TextWindow) windows[w]).getTextPanel();
		String[] col_heads = text_panel.getColumnHeadings().split("\t");
		col_x = col_y = -1;
		for (int i=0; i<col_heads.length; i++) {
		    if ( use_x && col_heads[i].equals(xname) )
			col_x = i;
		    else if ( use_y && col_heads[i].equals(yname) )
			col_y = i;
		}
		if ( ((!use_x) || (col_x >= 0)) && ((!use_y) || (col_y >= 0)) ) {
		    results = text_panel;
		    break;
		}
	    }
	}
	if ( results == null ) {
	    IJ.error("Can't find Tracker Results window");
	    return;
	}
	
	// read all the x and y before they are cleared
	int[] xx = new int[nFrames];
	int[] yy = new int[nFrames];
	for (int j=0; j<nFrames; j++) {
	    String[] tokens = results.getLine(j).split("\t");
	    if ( use_x )
		xx[j] = (int) Float.parseFloat(tokens[col_x]);
	    else
		xx[j] = 0;
	    if ( use_y )
		yy[j] = (int) Float.parseFloat(tokens[col_y]);
	    else
		yy[j] = 0;
	}

	// prepare for new results, starting with 1
	results = IJ.getTextPanel();
	results.clear();
	Analyzer a = new Analyzer();
	a.resetCounter();

	// outputs for plotting
	float [] frame = new float[nFrames];
	float [] mean = new float[nFrames];
	// reference points: displacement from what's onscreen (also restoring what's onscreen)
	int saved_frame = imp.getCurrentSlice();
	int x0 = xx[saved_frame];
	int y0 = yy[saved_frame];
	Roi roi_orig = imp.getRoi();
	if ( roi_orig == null )
	    roi_orig = new Roi(new Rectangle(0, 0, imp.getWidth(), imp.getHeight()));
	// the "location" of roi is roi.getBounds().x, .y
	Rectangle roi_orect = roi_orig.getBounds();
	for (int j=0; j<nFrames; j++) {
	    imp.setSlice(j);
	    int dx = xx[j] - x0;
	    int dy = yy[j] - y0;
	    // don't want to mess up roi_orig; move a clone and select it:
	    Roi roi = (Roi) roi_orig.clone();
	    roi.setLocation(roi_orect.x + dx, roi_orect.y + dy);
	    imp.setRoi(roi);
	    
	    // get all stats from the Measurement dialog:
	    ImageStatistics stats = imp.getStatistics(a.getMeasurements());
	    a.saveResults(stats, roi);
	    // and add some extras:
	    ResultsTable rt = a.getResultsTable();
	    rt.addValue("X displacement", dx);
	    rt.addValue("Y displacement", dy);
	    // replicate input columns so user can repeat with different ROI
	    rt.addValue(xname, xx[j]);
	    rt.addValue(yname, yy[j]);
	    // put invisible results table into a row of visible results table:
	    a.displayResults();

	    // plotted x,y
	    frame[j] = j+1;
	    mean[j] = (float) stats.mean;
	}

	new PlotWindow("Measure Track", "Frame", "Mean", frame, mean).draw();

	// restore
	imp.setRoi(roi_orig);
	imp.setSlice(saved_frame);
    }
    
}
