import ij.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

/**	This plugin is an extended version of  ImageJ's Measure command that calculates
	object circularity using the formula circularity = 4pi(area/perimeter^2).
*/
public class Circularity_ implements PlugInFilter, Measurements {
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		if (IJ.versionLessThan("1.18o"))
			return DONE;
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		int measurements = Analyzer.getMeasurements(); // defined in Set Measurements dialog
		measurements |= AREA+PERIMETER; //make sure area and perimeter are measured
		Analyzer.setMeasurements(measurements);
		Analyzer a = new Analyzer();
		ImageStatistics stats = imp.getStatistics(measurements);
		Roi roi = imp.getRoi();
		a.saveResults(stats, roi); // store in system results table
		ResultsTable rt =Analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();
		double area = rt.getValue(ResultsTable.AREA, counter-1);
		double perimeter = rt.getValue(ResultsTable.PERIMETER, counter-1);
		rt.addValue("Circularity", perimeter==0.0?0.0:4.0*Math.PI*(area/(perimeter*perimeter)));
		a.displayResults(); //display the results in the worksheet
		a.updateHeadings(); // update the worksheet headings
	}

}

