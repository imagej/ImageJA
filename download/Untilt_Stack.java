import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.text.*;
import imagescience.image.Image;
import imagescience.transform.Affine;
import java.awt.*;
import java.awt.geom.*;
import ij.plugin.filter.*;

/* 	Author: Julian Cooper
	Contact: Julian.Cooper [at] uhb.nhs.uk
	First version: 2009/05/22
	Licence: Public Domain	*/

/*  Acknowledgements: Erik Meijering, author of TransformJ*/

/*  This plugin rotates a stack of images so that a linear structure of interest
    becomes parallel to the z-axis
*/

/* Requires imagescience.jar to be installed in plugins folder
 * Available as part of TransformJ written by Erik Meijering
 * http://www.imagescience.org/meijering/software/transformj/
 */

public class Untilt_Stack implements PlugInFilter {
	ImagePlus imp;
    Point2D.Double pt1, pt2;
    
    
    public Calibration cal;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
        double x, y, z, theta_x, theta_y, sz;
		
        double pt1_z, pt2_z = 0.0;
        ImagePlus rot;
		Point wfuloc=null;
        boolean looped = false;
		boolean show_log = false;
		boolean export_matrix = false;
        String looped_msg = "";
		String mtx_string = "";

		if (imp.getStackSize()<2) {
			IJ.error("Untilt Stack...", "Stack required");
			return;
		}

		Roi roi = imp.getRoi();
        Roi old_roi = null;
        if(roi!=null) old_roi = roi;
        if (roi == null) {
            Dialog wfud1 = new WaitForUserDialog("Untilt Stack","Scroll stack to top of object and select area for first point");
			wfud1.setLocationRelativeTo(null);
            wfud1.setVisible(true);
			wfuloc = wfud1.getLocation();
            if(IJ.escapePressed()) return;
        }
        cal = imp.getCalibration();

        pt1 = getPoint(imp.getRoi(), ip);
        pt1_z = imp.getSlice();

        while (pt2_z <= pt1_z){
            if(looped) looped_msg = "(Slice number must be at least that of first point)";
            Dialog wfud2 = new WaitForUserDialog("Untilt Stack","Scroll stack to base of object and select area for second point\n" + looped_msg);
			Checkbox cb1 = new Checkbox("Log angular corrections", false);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=0; gbc.gridy=2;
			gbc.insets=new Insets(0, 4, 4, 0);
			gbc.anchor=GridBagConstraints.WEST;
			wfud2.add(cb1, gbc);
			Checkbox cb2 = new Checkbox("Display matrix as text", false);
			gbc.gridx=0; gbc.gridy=2;
			gbc.insets=new Insets(0, 100, 4, 0);
			gbc.anchor=GridBagConstraints.CENTER;
			wfud2.add(cb2, gbc);
			if(wfuloc!=null) wfud2.setLocation(wfuloc);
			else wfud2.setLocationRelativeTo(null);

            wfud2.setVisible(true);
			
            
			show_log=cb1.getState();
			export_matrix=cb2.getState();
			
			if(IJ.escapePressed()) return;
			
            pt2 = getPoint(imp.getRoi(), ip);
            pt2_z = imp.getSlice();
            looped = true;
        }
		
       
        z = (pt2_z - pt1_z) * cal.pixelDepth;
        
        x = pt2.x - pt1.x;
        y = pt2.y - pt1.y;

		//Calculate angles for the rotation matrix
		//NOTE: These angles are for rotation of the point not the coordinate axes
		//ensures sign of rotation is correct
		theta_y = -(Math.abs(Math.acos(z/(Math.sqrt(x*x + z*z)))))*Math.signum(x);
        theta_x = -(Math.abs(Math.acos(z/(Math.sqrt(y*y + z*z)))))*Math.signum(-y);
        
        //Calculate the z-scaling factor (reciprocal of axis scaling factor)
		sz = cal.pixelWidth/cal.pixelDepth;

		//Rotation and scaling matrix. Rotates the axes through angles theta_x and theta_y
		/*double matrix[][]={
			{Math.cos(theta_y),							0,						-sz*Math.sin(theta_y),					0},
			{Math.sin(theta_x)*Math.sin(theta_y),		Math.cos(theta_x),		sz*Math.cos(theta_y)*Math.sin(theta_x), 0},
			{(Math.cos(theta_x)*Math.sin(theta_y)/sz),	-Math.sin(theta_x)/sz,	Math.cos(theta_x)*Math.cos(theta_y),	0},
			{0,											0,						0,										1}
		};*/
		
		double matrix[][]={
			{Math.cos(theta_y),			Math.sin(theta_x)*Math.sin(theta_y),	Math.cos(theta_x)*Math.sin(theta_y)/sz,	0},
			{0,							Math.cos(theta_x),						-Math.sin(theta_x)/sz,					0},
			{-sz*Math.sin(theta_y),		sz*Math.cos(theta_y)*Math.sin(theta_x),	Math.cos(theta_x)*Math.cos(theta_y),	0},
			{0,											0,						0,										1}
		};
		
		if(export_matrix) {
			mtx_string = 
				IJ.d2s(matrix[0][0],5)+"   "+IJ.d2s(matrix[0][1],5)+"   "+IJ.d2s(matrix[0][2],5)+"   0.00000\n"+
				IJ.d2s(matrix[1][0],5)+"   "+IJ.d2s(matrix[1][1],5)+"   "+IJ.d2s(matrix[1][2],5)+"   0.00000\n"+
				IJ.d2s(matrix[2][0],5)+"   "+IJ.d2s(matrix[2][1],5)+"   "+IJ.d2s(matrix[2][2],5)+"   0.00000\n"+
				"0.00000   0.00000   0.00000   1.00000";
				new TextWindow(imp.getTitle()+" matrix", mtx_string, 350, 250);
		}

		rot= affine_TJ(imp, matrix);
		rot.setCalibration(cal);
		if(show_log){
			IJ.log(imp.getTitle());
			IJ.log("Rotation about x-axis (degrees): "+IJ.d2s(Math.toDegrees(theta_x),5));	//Report the angles of rotation of
			IJ.log("Rotation about y-axis (degrees): "+IJ.d2s(Math.toDegrees(theta_y),5));	//the image data. Negative of the axis rotation.
		}
		
        rot.setTitle(imp.getTitle()+" - Untilted");
        rot.show();
        if(old_roi != null) imp.setRoi(old_roi);
        else imp.killRoi();
	}
    
    Point2D.Double getPoint(Roi roi, ImageProcessor ip) {
        double rx, ry, roiX, roiY;
        
        if (roi != null) {
           	rx = roi.getBounds().getCenterX();
            ry = roi.getBounds().getCenterY();
        } else {
            rx = 0;
            ry = 0;
        }

		roiX = cal != null ? cal.getX(rx) : rx;
		roiY = cal != null ? cal.getY(ry, ip.getHeight()) : ry;
        
        Point2D.Double pt = new Point2D.Double(roiX, roiY);
        return pt;
    }


	ImagePlus affine_TJ(ImagePlus imp, double[][]matrix ) {
        Image img = Image.wrap(imp);
        Affine affiner = new Affine();
        Image newimg = affiner.run(img, matrix, Affine.LINEAR, true, false);
        ImagePlus newimp = newimg.imageplus();
        return newimp;
    }
        
}
