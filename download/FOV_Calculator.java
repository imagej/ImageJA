//=====================================================
//      Name:        FOV_Calculator.java
//      Project:     PINHOLE CAMERA MODEL FOR IMAGEJ
//      Version:     0.1
//
//      Author:      Joshua Gulick, Orphan Technologies, Inc.
//      Date:        5/30/2000
//      Comment:     Determines the field of view for a particular camera setup
//=====================================================

import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;

public class FOV_Calculator implements PlugIn {

  public void run(String arg) {
        double sx=0.0, sy=0.0, sz=0.0;
        double px=17.5, py=12.0, pz=15.0;
        double sw=35.0, sh=24.0;
        double leftSensorWidth, rightSensorWidth;
        double lowerSensorHeight, upperSensorHeight;
        double rightFOV, leftFOV, upperFOV, lowerFOV;
        double pd;

        // The underscores are required for the plugin to work with the
        // command recorder when the first word of the prompt is not unique. 
        // They are not displayed in the dialog in ImageJ 1.30r ot later.

        GenericDialog gd = new GenericDialog("FOV Calculator");
        gd.addNumericField("Sensor_Origin_X:", sx, 1);
        gd.addNumericField("Sensor_Origin_Y:", sy, 1);
        gd.addNumericField("Sensor_Origin_Z:", sz, 1);
        gd.addNumericField("Pinhole_X:", px, 1);
        gd.addNumericField("Pinhole_Y:", py, 1);
        gd.addNumericField("Pinhole_Z:", pz, 1);
        gd.addNumericField("Sensor_Width:", sw, 1);
        gd.addNumericField("Sensor_Height:", sh, 1);
        gd.addMessage("Please enter camera parameters.");
        gd.showDialog();

        if (gd.wasCanceled())
               return;

        sx = gd.getNextNumber();
        sy = gd.getNextNumber();
        sz = gd.getNextNumber();
        px = gd.getNextNumber();
        py = gd.getNextNumber();
        pz = gd.getNextNumber();
        sw = gd.getNextNumber();
        sh = gd.getNextNumber();
        pd = pz - sz;
        leftSensorWidth = sw - sx - px;
        rightFOV = Math.atan(leftSensorWidth/pd);
        rightSensorWidth = sw - leftSensorWidth;
        leftFOV = Math.atan(rightSensorWidth/pd);
        lowerSensorHeight = sh - sy - py;
        upperFOV = Math.atan(lowerSensorHeight/pd);
        upperSensorHeight = sh - lowerSensorHeight;
        lowerFOV = Math.atan(upperSensorHeight/pd);

        int places = 4;
        IJ.log("");
        IJ.log("The left view field is " + IJ.d2s(leftFOV,places) + " radians.");
        IJ.log("The right view field is " + IJ.d2s(rightFOV,places) + " radians.");
        IJ.log("The upper view field is " + IJ.d2s(upperFOV,places) + " radians.");
        IJ.log("The lower view field is " + IJ.d2s(lowerFOV,places) + " radians.");
        IJ.log("The total view field is " + IJ.d2s((leftFOV+rightFOV)*(upperFOV+lowerFOV),places) + " radians^2.");
        IJ.log("The left-right view angle is " + IJ.d2s((leftFOV+rightFOV)*180.0/Math.PI,places) + " degrees.");
        IJ.log("The upper-lower view angle is " + IJ.d2s((upperFOV+lowerFOV)*180.0/Math.PI,places) + " degrees.");
   } 
}
