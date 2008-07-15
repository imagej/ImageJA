import ij.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;


/**	Calculates the mean, min, max and standard deviation of the 
	pixel values along a straight line selection. */
public class Line_Analyzer implements PlugIn {

		static final String headings = " \tL-Mean\tL-Min\tL-Max\tL-StdDev\tL-Length\tL-Angle";
		static int count;

		public void run(String arg) {
		if (IJ.versionLessThan("1.18o"))
			return;
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		Roi roi = imp.getRoi();
		if (!(roi!=null && roi.getType()==roi.LINE) )
			{IJ.error("Straight line selection required."); return;}

		double angle = 0.0;
		if (roi.getType()==Roi.LINE) {
			Line line = (Line)roi;
			angle = roi.getAngle(line.x1, line.y1, line.x2, line.y2);
		} else if (roi.getType()==Roi.POLYLINE)
			angle = ((PolygonRoi)roi).getAngle();
			
		double[] ddata = ((Line)roi).getPixels();
		float[] fdata = new float[ddata.length];
		for (int i=0; i<fdata.length; i++)
			fdata[i] = (float)ddata[i];
		ImageProcessor ip = new FloatProcessor(fdata.length, 1, fdata, null);
		ImageStatistics s = new FloatStatistics(ip);
		
		if (!headings.equals(IJ.getTextPanel().getColumnHeadings())) {
			IJ.setColumnHeadings(headings);
			count = 0;
		}
		
		IJ.write(++count+"\t"+IJ.d2s(s.mean)+"\t"+IJ.d2s(s.min)+"\t"+IJ.d2s(s.max)+"\t"+IJ.d2s(s.stdDev)+"\t"+IJ.d2s(roi.getLength())+"\t"+IJ.d2s(angle));
		IJ.register(Line_Analyzer.class);

	}
}
