import ij.*;
import ij.process.*;
import ij.plugin.*;

/** This plugin separately measures the red, green and blue channels of an RGB image. */
public class RGB_Measure implements PlugIn {

	public void run(String arg) {
		if (IJ.versionLessThan("1.32g"))
			return;
		if (IJ.getImage().getBitDepth()!=24)
			{IJ.showMessage("RGB Measure", "RGB Image required");}
		double[] wf = ColorProcessor.getWeightingFactors();
		//IJ.log("Weighting Factors: "+wf[0]+" "+wf[1]+" "+wf[2]);
		ColorProcessor.setWeightingFactors(1.0, 0.0, 0.0);
		IJ.run("Measure");
		ColorProcessor.setWeightingFactors(0.0, 1.0, 0.0);
		IJ.run("Measure");
		ColorProcessor.setWeightingFactors(0.0, 0.0, 1.0);
		IJ.run("Measure");
		ColorProcessor.setWeightingFactors(wf[0], wf[1], wf[2]);
	}

}
