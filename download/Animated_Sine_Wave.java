import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;

/** This plugin uses the new Plot class to create an animated sine wave. */
public class Animated_Sine_Wave implements PlugIn {

    public void run(String arg) {
        if (IJ.versionLessThan("1.31h"))
            return;
        int frames = 50;
        int points = 100;
        double start = Math.PI*1.5;
        ImageStack stack = null;
        float[] x = new float[points];
        float[] y = new float[points];
        for (int i=0; i<frames; i++) {
            IJ.showProgress(i, frames-1);
            for (int j=0; j<points; j++) {
                double angle = j*(Math.PI*4/points);
                x[j] = (float)angle;
                y[j] = (float)Math.sin(start+angle);
            }
            start += Math.PI*4/points;
            Plot plot = new Plot("Sine Wave", "", "", x, y);
            ImageProcessor ip = plot.getProcessor();
            if (i==0)
                stack = new ImageStack(ip.getWidth(), ip.getHeight());
            stack.addSlice(null, ip);
        }
        new ImagePlus("Animated Sine Wave", stack).show();
        //IJ.run("Animation Options...", "speed=10 loop start");
    }
}
