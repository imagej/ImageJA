import ij.*;
import ij.process.*;
import ij.plugin.*;
import ij.plugin.filter.Analyzer;
import ij.measure.*;
import ij.gui.*;
import java.awt.*;

/**    Counts the thresholded voxels in a stack and displays the count, average count per slice and the volume fraction.
*/
public class Voxel_Counter implements PlugIn {

    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp==null) {
            IJ.noImage();
            return;
        }
        if (!(imp.getProcessor() instanceof ByteProcessor)) {
            IJ.showMessage("Voxel Counter", "8-bit image or stack required.");
            return;
        }
        if (imp.getProcessor().getMinThreshold()==ImageProcessor.NO_THRESHOLD && !binaryImage(imp)) {
            IJ.showMessage("Voxel Counter", "Thresolded or binary stack required.");
            return;
        }
        Calibration cal = imp.getCalibration();
        double pw = cal.pixelWidth;
        double ph = cal.pixelHeight;
        double pd = cal.pixelDepth;
        cal.pixelWidth = 1.0;
        cal.pixelHeight = 1.0;

        int nslices = imp.getStackSize();
        Roi roi = imp.getRoi();
        int roiCount = 0;
        ResultsTable rt;
        int volumeCount = imp.getWidth()*imp.getHeight()*nslices;
        if (roi==null)
            roiCount = volumeCount;
        else if (roi.getType()==Roi.RECTANGLE) {
            Rectangle r = roi.getBoundingRect();
            roiCount = r.width*r.height*nslices;           
        } else {
            IJ.run("Clear Results");
            IJ.run("Set Measurements...", "area decimal=2");
            for (int i=1; i<=nslices; i++) {
                imp.setSlice(i);
                IJ.run("Measure");
            }
            rt = Analyzer.getResultsTable();
            roiCount = 0;
            for (int i=0; i<rt.getCounter(); i++)
                roiCount += rt.getValue(ResultsTable.AREA, i);
       }

        IJ.run("Clear Results");
        IJ.run("Set Measurements...", "area limit decimal=2");
        for (int i=1; i<=nslices; i++) {
            imp.setSlice(i);
            IJ.run("Measure");
        }
        cal.pixelWidth = pw;
        cal.pixelHeight = ph;
        rt = Analyzer.getResultsTable();
        double sum = 0;
        for (int i=0; i<rt.getCounter(); i++)
            sum += rt.getValue(ResultsTable.AREA, i);
        IJ.write("");
        IJ.write("Thresholded voxels: "+(int)sum);
        IJ.write("Average voxels per slice:  "+IJ.d2s(sum/nslices,2));
        IJ.write("Total ROI Voxels: "+roiCount);
        IJ.write("Volume fraction:  "+IJ.d2s((sum*100)/roiCount,2)+"%");
        IJ.write("Voxels in stack: "+volumeCount);
        if (cal.scaled()) {
		int digits = Analyzer.getPrecision();
		String units = cal.getUnits();	
       	double scale = pw*ph*pd;
		IJ.write("");
		IJ.write("Voxel size: "+IJ.d2s(cal.pixelWidth, digits) + "x" + IJ.d2s(cal.pixelHeight, digits)+"x"+IJ.d2s(cal.pixelDepth, digits)+" "+units);
		IJ.write("Thresholded volume: "+IJ.d2s(sum*scale,digits)+" "+units+"^3");
		IJ.write("Average volume per slice:  "+IJ.d2s(sum*scale/nslices,digits)+" "+cal.getUnits()+"^3");
		IJ.write("Total ROI volume: "+IJ.d2s(roiCount*scale,digits)+" "+units+"^3");
		IJ.write("Volume of stack: "+IJ.d2s(volumeCount*scale,digits)+" "+units+"^3");
        }
    }

    boolean binaryImage(ImagePlus imp) {
        ImageStatistics stats = imp.getStatistics();
        boolean isBinary = stats.histogram[0]+stats.histogram[255]==stats.pixelCount;
        if (isBinary) {
            boolean invertedLut = imp.isInvertedLut();
            ImageProcessor ip = imp.getProcessor();
            if (invertedLut)
                ip.setThreshold(255, 255, ImageProcessor.RED_LUT);
            else
                ip.setThreshold(0, 0, ImageProcessor.RED_LUT);
        }
        return isBinary;
    }

}
