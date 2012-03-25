import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.filter.*;

public class Gaussian_Blur_3D implements PlugIn {
	private static double xsigma=2, ysigma=2, zsigma=2;

	public void run(String arg) {
		if (IJ.versionLessThan("1.46j"))
			return;
		ImagePlus img = IJ.getImage();
		GenericDialog gd = new GenericDialog("3D Gaussian Blur");
		gd.addNumericField("X sigma", xsigma, 1);
		gd.addNumericField("Y sigma", ysigma, 1);
		gd.addNumericField("Z sigma", zsigma, 1);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		xsigma = gd.getNextNumber();
		ysigma = gd.getNextNumber();
		zsigma = gd.getNextNumber();
		GaussianBlur.blur3D(img, xsigma, ysigma, zsigma);
	}

}
