import ij.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import sms.Unimotion;

// MacBook_Position.java
// mutterer@ibmp.fr
//
// Reads MacBook 'sudden motion sensors' 
// sms.jar and libUnimotion.jnilib are from http://www.shiffman.net/p5/sms/
//   

public class MacBook_Position implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = IJ.createImage("Position", "8-bit Ramp", 400, 400, 1);
		IJ.run(imp, "Rotate 90 Degrees Left", "");
		IJ.run("Line Width...", "line=30");
		imp.show();
		IJ.run("In");
		while (WindowManager.getFrame("Position")!=null) {
			int[] vals = Unimotion.getSMSArray();
			double a= Math.PI/2*vals[0]/255;
			double x1= 200+150*Math.cos(a);
			double y1= 200+vals[1]+200*Math.sin(a);
			double x2= 200-150*Math.cos(a);
			double y2= 200+vals[1]-200*Math.sin(a);
			imp.setRoi(new Line(x1, y1, x2, y2));
			if (Math.abs(vals[2]-255)>30) Roi.setColor(Color.red);
			else if (Math.abs(vals[2]-255)>15) Roi.setColor(Color.yellow);
			else 	 Roi.setColor(Color.green);
		}
	}
}
