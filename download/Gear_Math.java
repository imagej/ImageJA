//=====================================================
//      Name:            Gear_Math.java
//      Project:         Math Stuff
//      Version:         0.1
//
//      Author:           Joshua Gulick, Orphan Technologies, Inc.
//      Date:               6/09/2000
//      Comment:       Calculates rpm and torque of some simple gear designs
//=====================================================


//===========imports===================================
import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;
import ij.plugin.PlugIn;
import ij.text.*;

//===========source====================================
public class Gear_Math implements PlugIn {

public void run(String arg) {
    double g1 = 12, g2 = 34560, g3 = 144, s1rpm = 2880, s1torque = 34560;
    GenericDialog gd = new GenericDialog("Enter Gear Info", IJ.getInstance());
    gd.addMessage("All gears have equally spaced teeth. The screw\n"
		+"threads are spaced equal to the gear teeth");
    gd.addNumericField("RPM of drive screw:", s1rpm, 0);
    gd.addNumericField("Torque (ft./lbs.) of drive screw:", s1torque, 1);
    gd.addNumericField("1st gear tooth count:", g1, 0);
    gd.addNumericField("2nd gear tooth count:", g2, 0);
    gd.addNumericField("3rd gear tooth count:", g3, 0);

    gd.showDialog();
        if (gd.wasCanceled())
              return;
    s1rpm =  (int)gd.getNextNumber();
    s1torque =  gd.getNextNumber();
    g1 =  (int)gd.getNextNumber();
    g2 =  (int)gd.getNextNumber();
    g3 =  (int)gd.getNextNumber();

    double g1rpm, g1torque, g2rpm, g2torque, g3rpm, g3torque;
    g1rpm = s1rpm/g1;
    g1torque = s1torque*(g1/s1rpm);
    g2rpm = g1rpm*(g1/g2);
    g3rpm = g2rpm*(g2/g3);
    g2torque = g1torque*(g2/g1);
    g3torque = g2torque*(g3/g2);

    TextWindow tw = new TextWindow("Gear Math", "", 700, 200);
    tw.append("A screw gear rotating at " + (int)s1rpm + " rpm and having a torque of " + s1torque + " ft./lbs.");
    tw.append("drives a gear with " + (int)g1 + " teeth, causing it to rotate at " + IJ.d2s(g1rpm,1) + " rpm with a torque of " + g1torque + " ft./lbs.");
    tw.append("This drives a second gear with " + (int)g2 + " teeth, causing it to rotate at " + IJ.d2s(g2rpm,1) + " rpm with a torque of " + g2torque + " ft./lbs.");
    tw.append("The second gear, in turn, drives a third gear with " + (int)g3 + " teeth which rotates at " + IJ.d2s(g3rpm,1) + " rpm and has a torque of " + g3torque + " ft./lbs.");
    }
}
