import ij.measure.*;
import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

/** This plugin implement the algorithm to direct
measurement of the fractal dimension of fracture surface 
from height measurements obtained by projective covering 
method described in "Direct fractal measurement and 
multifractal properties of fracture surfaces",Heping Xie a,
Jin-an Wang a, E. Stein, Physics Letters A 242 (1998) 41-50.
 */
public class Fractal_Surface_Measurement implements PlugIn {

    public void run(String arg) {
        ImagePlus imp = IJ.getImage();
        GenericDialog gd = new GenericDialog("Parameters of fractal measurement surfaces");
        gd.addNumericField("Scale:", 51, 3);
        gd.showDialog();
        int scale = (int) gd.getNextNumber();
        ResultsTable rt = fractal2D(imp, scale);
        rt.show("Results");
    }

    public ResultsTable fractal2D(ImagePlus imp, int scale) {
        ImageProcessor ip = imp.getProcessor();
        ip = ip.convertToFloat();
        float[] pixels = (float[]) (ip.getPixels());//(float[])
        int scaleD = (scale / 2) + 1;
        int count = 0;
        int slice = 0;
        double A = 0;
        double[] logarea = new double[scaleD];
        double[] logscaleM = new double[scaleD];
        int width = ip.getWidth();
        int height = ip.getHeight();

        for (int s = 1; s <= scale; s = s + 2) {
            IJ.showProgress((double) s / (scale));
            int xc = scale / 2;
            int yc = xc;
            for (int y = yc; y < height - yc; y = y + s) {
                for (int x = xc; x < width - xc; x = x + s) {
                    double h1 = (double) pixels[-xc + x + (-yc + y) * width];
                    double h2 = (double) pixels[xc + x + (-yc + y) * width];
                    double h3 = (double) pixels[-xc + x + (yc + y) * width];
                    double h4 = (double) pixels[xc + x + (yc + y) * width];
                    //projective covering method
                    double A1 = (double) (Math.sqrt(Math.pow((s), 2) + Math.pow((h2 - h1), 2)) * Math.sqrt(Math.pow(s, 2) + Math.pow((h4 - h1), 2))) / 2;//(float)
                    double A2 = (double) (Math.sqrt(Math.pow((s), 2) + Math.pow((h2 - h3), 2)) * Math.sqrt(Math.pow(s, 2) + Math.pow((h4 - h3), 2))) / 2;//(float)
                    A += A1 + A2;
               }
            }

            logarea[count] = (Math.log(A)) / Math.log(10);
            /**log (A(delta)) from equation 6 of "Direct fractal measurement and
            multifractal properties of fracture surfaces",Heping Xie a,
            Jin-an Wang a, E. Stein, Physics Letters A 242 (1998) 41-50.*/
            logscaleM[count] = (Math.log(s)) / Math.log(10);
            /**log (delta) from equation 6 of "Direct fractal measurement and
            multifractal properties of fracture surfaces",Heping Xie a,
            Jin-an Wang a, E. Stein, Physics Letters A 242 (1998) 41-50.*/
            count++;
            A = 0;
        }

        ResultsTable rt = new ResultsTable();
        for (int c = 0; c < scaleD; c++) {
            rt.incrementCounter();
            rt.addValue("logScale", logscaleM[c]);
            rt.addValue("logArea", logarea[c]);
            rt.show("Results");
        }
        return rt;
    }

}





        









