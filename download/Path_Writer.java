import ij.plugin.filter.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;
import java.io.*;
import ij.*;
import ij.process.*;
import ij.io.*;
import ij.gui.*;
import ij.measure.*;

/** Saves evenly spaced (one pixel) X-Y coordinates along the current ROI boundary. */
public class Path_Writer implements PlugInFilter, Measurements {
    ImagePlus imp;
    double[] xpath;
    double[] ypath;
    int pathLength;
    int arrayLength;
    boolean invertYCoordinates;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL+ROI_REQUIRED+NO_CHANGES;
    }

    public void run(ImageProcessor ip) {
        try {
            saveXYCoordinates(imp);
        } catch (IllegalArgumentException e) {
            IJ.showMessage("Path Writer", e.getMessage());
        }
    }

    public void saveXYCoordinates(ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi==null)
            throw new IllegalArgumentException("ROI required");
        if (!(roi instanceof PolygonRoi))
            throw new IllegalArgumentException("Irregular area or line selection required");

        SaveDialog sd = new SaveDialog("Save Coordinates as Text...", imp.getTitle(), ".txt");
        String name = sd.getFileName();
        if (name == null)
            return;
        String directory = sd.getDirectory();
        PrintWriter pw = null;
        try {
            FileOutputStream fos = new FileOutputStream(directory+name);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            pw = new PrintWriter(bos);
        } catch (IOException e) {
            IJ.showMessage("XYWriter", ""+e);
            return;
        }

        PolygonRoi p = (PolygonRoi)roi;
        getPath(p);
        //drawPath(imp);
        Calibration cal = imp.getCalibration();
        String ls = System.getProperty("line.separator");
        boolean scaled = cal.scaled();
        int maxy = imp.getHeight()-1;
        double w = cal.pixelWidth;
        double h = cal.pixelHeight;
        int digits = roi.getType()==Roi.TRACED_ROI&&w==1.0&&h==1.0?0:4;
        int measurements = Analyzer.getMeasurements();
        invertYCoordinates = (measurements&INVERT_Y)!=0;
        if (invertYCoordinates) {
            for (int i=0; i<pathLength; i++)
                pw.print(IJ.d2s((xpath[i])*w,digits) + "\t" + IJ.d2s((maxy-ypath[i])*h,digits) + ls);
        } else {
            for (int i=0; i<pathLength; i++)
                pw.print(IJ.d2s((xpath[i])*w,digits) + "\t" + IJ.d2s(ypath[i]*h,digits) + ls);
        }
        pw.close();
    }

    void getPath(Roi roi) {
        int n = ((PolygonRoi)roi).getNCoordinates();
        int[] x = ((PolygonRoi)roi).getXCoordinates();
        int[] y = ((PolygonRoi)roi).getYCoordinates();
        Rectangle r = roi.getBoundingRect();
        int xbase = r.x;
        int ybase = r.y;
        boolean areaPath = roi.getType()<=Roi.TRACED_ROI;
        double length = 0.0;
        double segmentLength;
        int xdelta, ydelta, iLength;
        double[] segmentLengths = new double[n];
        int[] dx = new int[n];
        int[] dy = new int[n];
        for (int i=0; i<(n-1); i++) {
            xdelta = x[i+1] - x[i];
            ydelta = y[i+1] - y[i];
            segmentLength = Math.sqrt(xdelta*xdelta+ydelta*ydelta);
            length += segmentLength;
            segmentLengths[i] = segmentLength;
            dx[i] = xdelta;
            dy[i] = ydelta;
        }
        if (areaPath) {
            xdelta = x[0] - x[n-1];
            ydelta = y[0] - y[n-1];
            segmentLength = Math.sqrt(xdelta*xdelta+ydelta*ydelta);
            length += segmentLength;
            segmentLengths[n-1] = segmentLength;
            dx[n-1] = xdelta;
            dy[n-1] = ydelta;
       }

        int size = (int)(1.1*length);
        arrayLength = size;
        xpath = new double[size];
        ypath = new double[size];
        double leftOver = 1.0;
        double distance = 0.0;
        int index = -1;
        for (int i=0; i<n; i++) {
            double len = segmentLengths[i];
            if (len==0.0)
                continue;
            double xinc = dx[i]/len;
            double yinc = dy[i]/len;
            double start = 1.0-leftOver;
            double rx = xbase+x[i]+start*xinc;
            double ry = ybase+y[i]+start*yinc;
            double len2 = len - start;
            int n2 = (int)len2;
            for (int j=0; j<=n2; j++) {
                index++;
                if (index<xpath.length) {
                    xpath[index] = rx;
                    ypath[index] = ry;
                    pathLength = index+1;
                }
                rx += xinc;
                ry += yinc;
            }
            distance += len;
            leftOver = len2 - n2;
        }

    }

    void drawPath(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        ip.setColor(Color.black);
        for (int i=0; i<pathLength; i++)
             ip.drawDot((int)(xpath[i]+0.5), (int)(ypath[i]+0.5));
        imp.updateAndDraw();
    }

}
