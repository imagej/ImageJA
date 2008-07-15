import ij.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/**
* "AnalyzeParticles" for each slice of stack
*    record:
*        items checked in Set Measurements
*        slice# in "Slice" column
*         topLeft x,y and ncoords to allow re_autoOutline of particles
*
*/
public class Custom_Particle_Analyzer implements PlugIn {

	public void run(String arg) {
		if (IJ.versionLessThan("1.26i"))
			return;
		ImagePlus imp = IJ.getImage();
		analyzeStackParticles(imp);
	}

	public void analyzeStackParticles(ImagePlus imp) {
		if (imp.getBitDepth()==24)
			{IJ.error("Grayscale image required"); return;}
		CustomParticleAnalyzer pa = new CustomParticleAnalyzer();
		int flags = pa.setup("", imp);
		if (flags==PlugInFilter.DONE)
			return;
		if ((flags&PlugInFilter.DOES_STACKS)!=0) {
			for (int i=1; i<=imp.getStackSize(); i++) {
				imp.setSlice(i);
				pa.run(imp.getProcessor());
			}
		} else
			pa.run(imp.getProcessor());
	}
}

class CustomParticleAnalyzer extends ParticleAnalyzer {
	
	// Overrides method with the same in AnalyzeParticles that's called once for each particle
	protected void saveResults(ImageStatistics stats, Roi roi) {
		int coordinates = ((PolygonRoi)roi).getNCoordinates();
		Rectangle r = roi.getBoundingRect();
		int x = r.x+((PolygonRoi)roi).getXCoordinates()[coordinates-1];
		int y = r.y+((PolygonRoi)roi).getYCoordinates()[coordinates-1];
		analyzer.saveResults(stats, roi);
		rt.addValue("Slice", imp.getCurrentSlice());
		rt.addValue("Xtopl", x);
		rt.addValue("Ytopl", y);
		rt.addValue("nCoord", coordinates);
		if (showResults)
			analyzer.displayResults();
	}
	    
}

