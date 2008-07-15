import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import java.awt.*;
import ij.gui.*;

/**
This plugin illustrates a problem  that could occur in ImageJ 1.20i and earlier when rapidly
opening a series of  windows. ImageJ could get in a state where the  focus was rapidy
bouncing between two or more  windows in an endless cycle until the user clicked on close boxes
a few times. The problem was most likely to occur with Sun's Java 2 JVM (JDK 1.2 and JDK 1.3)
when using shift-R (Process/Repeat command) to rapidy run multiple copies of this plugin.
This problem was more likely to occur in versions of ImageJ prior to 1.14.
*/
public class Focus_Bouncer implements PlugIn {

	public void run(String arg) {
		ByteProcessor bp1 = new ByteProcessor(300, 300);
		ByteProcessor bp2 = new ByteProcessor(300, 300);
		ByteProcessor bp3 = new ByteProcessor(300, 300);                
		ByteProcessor bp4 = new ByteProcessor(300, 300);                            
		ImagePlus imp1 = new ImagePlus("Window 1", bp1);
		ImagePlus imp2 = new ImagePlus("Window 2", bp2);
		ImagePlus imp3 = new ImagePlus("Window 3", bp3);
		ImagePlus imp4 = new ImagePlus("Window 4", bp4);                
            
		imp1.show();
		imp1.setRoi(1,1,10,10);           
            
		ProfilePlot a = new ProfilePlot(imp1);          
		imp2.show();            

		a.createWindow();
            
		imp3.show();		                
		imp4.show();	    
	}
}

