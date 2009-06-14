import ij.*;
import ij.process.*;
import ij.measure.*;
import imagescience.image.Image;
import imagescience.transform.Scale;
import ij.plugin.filter.*;

/* 	Author: Julian Cooper
	Contact: Julian.Cooper [at] uhb.nhs.uk
	First version: 2009/05/22
	Licence: Public Domain	*/

/*  Acknowledgements: Erik Meijering, author of TransformJ*/

/*  This plugin scales an anisotropically calibrated stack of images so that the
    calibration is isotropic (the pixel depth becomes the same as the pixel
    width). The plugin will not rescale XY anisotropy.
 
    Note: If the pixel depth of the source stack is less than then the width
    there will be fewer slices in the output stack resulting in a potential loss
    of information.
*/

/* Requires imagescience.jar to be installed in plugins folder
 * Available as part of TransformJ written by Erik Meijering
 * http://www.imagescience.org/meijering/software/transformj/
 */



public class Make_Isotropic implements PlugInFilter{
    ImagePlus imp;
    
    public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

    public void run(ImageProcessor ip) {

        if(imp.getStackSize()==1) {
            IJ.error("Stack required");
            return;
        }

        ImagePlus sca=null;
        Calibration cal = imp.getCalibration();
        double scaling = cal.pixelDepth/cal.pixelWidth;
        if(scaling!=1) {
            sca = scale_TJ(imp, scaling);
            sca.setTitle(imp.getTitle()+ " - isotropic");
            sca.show();
            sca.setCalibration(cal);
            Calibration cal_sca = sca.getCalibration();
            cal_sca.pixelDepth = cal_sca.pixelWidth;
        } else {
            IJ.showMessage("Make Isotropic", "Stack already isotropic");
        }
    }

    ImagePlus scale_TJ(ImagePlus imp, double zscale) {
        Image img = Image.wrap(imp);
        final Scale scaler = new Scale();
        final Image newimg = scaler.run(img, 1, 1, zscale,1,1, Scale.LINEAR);
        ImagePlus newimp = newimg.imageplus();
        return newimp;
    }
}
