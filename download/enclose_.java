
/**author A. Karperien
 * / *Charles Sturt University, Australia
 *
 * /*encloses image in a convex hull then calculates circularity and roundness
 * /*uses PolygonROI function by Thomas Roy, University of Alberta, Canada*/
/*counts pixels at foreground value so is suitable for binary images, but will automatically threshold an image for counting*/
/*
ANalyzes multiple images one by one by opening them and using ROIs.  Output to ImageJ's results window.
 */
import ij.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import java.io.*;
import ij.io.*;
import ij.plugin.*;
import ij.ImagePlus;
import ij.gui.Roi;
import java.math.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;
import ij.measure.*;
import javax.swing.*;
import javax.swing.filechooser.*;

public class enclose_ implements PlugIn, Measurements {
    
    private int width,height;  //width and height of window
    private int imgHeight, imgWidth; //dimensions of pixelated area
    private long polyWidth, polyHeight, polyLeft, polyTop;
    private float centrex, centrey;
    private int left, top, right, bottom;
    private int topxc, rightyc, leftyc, bottomxc;
    private String filename;
    private ImagePlus img;
    private int OriginNum;
    
    int foreground = 0;
    int background  = 255;
    
    //These variables can be modified to accomodate the needs of the user.
    //The higher either number is set at, the more memory the program will use.
    private static final int maxTableRows = 800;
    private static final int maxImageWidth = 5000;
    /************************************************************/
    
    /************************************************************/
    public void run(String args) {
        /*The main method.  Allows the user to select fiels and then analyzes them one by one,
        entering the data into the results table so it can be saved.*/
        
        ImageProcessor ip = null;
        ImageProcessor newIP = null;
        int DiameterIncrement = 1;
        int OriginNum=1;
        String sdir;
        ImageWindow win;
        boolean skip = false;
        JFileChooser fc = null;
        try {fc = new JFileChooser();}catch (Throwable e) {IJ.error("This plugin requires Java 2 or Swing."); return;}
        fc.setMultiSelectionEnabled(true);
        double returnVal = fc.showOpenDialog(IJ.getInstance());
        if (returnVal!=JFileChooser.APPROVE_OPTION)return;
        File[] files = fc.getSelectedFiles();
        if (files.length==0) { // getSelectedFiles does not work on some JVMs
            files = new File[1];
            files[0] = fc.getSelectedFile();
        }
        sdir = fc.getCurrentDirectory().toString();
        
        Opener opener = new Opener();
        for (int i=0; i<files.length; i++) {
            img = opener.openImage(sdir, files[i].getName());
            if (img!=null) {
                ip = img.getProcessor();
                img.show();
                win = WindowManager.getCurrentWindow();
                filename = img.getTitle();
                ImageStatistics stats = img.getStatistics();/*original image stats*/
                int allPix = 0;
                for (int k = 0; k < stats.histogram.length; k++) allPix = allPix + stats.histogram[k];
                if ((stats.histogram[0]+stats.histogram[255])!=allPix) {
                    //IJ.error("Will threshold now. " + filename + "is not an 8-bit binary image.");
                    IJ.run("Threshold");//skip = true;
	filename = "THR" + filename;
                }
                if (skip == false)
                {
                    if (stats.histogram[0]>stats.histogram[255])foreground = 255; else foreground = 0;
                    GetDimensions(ip);
                    PolygonRoi poly = this.getPolygon(ip);
                    img.setRoi(poly);
                    int ms = Analyzer.getMeasurements();
                    ms |= AREA+PERIMETER;
                    Analyzer.setMeasurements(ms);
                    Analyzer ana = new Analyzer();
                    stats = img.getStatistics(ms);
                    Roi roi = img.getRoi();
                    ana.saveResults(stats, roi);
                    ResultsTable rt =Analyzer.getResultsTable();
                    int cr = rt.getCounter();
                    double area = rt.getValue(ResultsTable.AREA, cr-1);
                    double perimeter = rt.getValue(ResultsTable.PERIMETER, cr-1);
	    double circul = (perimeter*perimeter)/area;
                    rt.addValue("Circularity", perimeter==0.0?0.0:4.0*Math.PI*(area/(perimeter*perimeter)));
	    rt.addValue("Roundness", circul);
                    rt.addLabel("FileName", filename);
                    ana.displayResults();
                    ana.updateHeadings();
                }
                
            }   	//if (i < files.length-1)this.
            img.hide();
        }
        IJ.register(enclose_.class);
    }
    
    
    
    
    private PolygonRoi getPolygon(ImageProcessor ip){
        /* This long-winded method determines the vertices of a polygon which forms a
        convex hull around the image.  It does this by first mapping the vertical contours
        of the image.  (Horizontal or vertical contours could have been used, but only one
        is necessary.)  After they are mapped, all of the points which can be enclosed by
        connecting any other points are eliminated.  This leaves only the outermost points,
        the ones that form the convex hull.  Returns a polygon created with these points.*/
        
        int polyXCoordinate[] = new int [maxImageWidth * 2];
        int polyYCoordinate[] = new int [maxImageWidth * 2];
        int[] histogram = new int[256];
        int aa=0;
        int y, x;
        int index = this.left;
        boolean change=true;
        float slope, slope2, rise, run;
        
        //Start by mapping farthest left point
        y=top-2;
        do{
            y++;
            ip.setRoi(index, y+1, 1, 1);
            histogram = ip.getHistogram();
        } while (( histogram[ this.foreground ]==0 ) && y<=bottom);
        
        polyXCoordinate[0]=index;
        polyYCoordinate[0]=y;
        
        int lastx=index;
        int lasty=y;
        float lastslope=0;
        
        //Begin mapping contours left to right
        for (index = left+1; index <= right; index++){
            
            y=top-1;
            
            do{
                y++;
                ip.setRoi(index, y+1, 1, 1);
                histogram = ip.getHistogram();
            } while (( histogram[ this.foreground ]==0 ) && y<=bottom);
            
            if (y<=bottom){
                rise = lasty-y;
                run = lastx-index;
                slope = rise/run;
                aa++;
                polyXCoordinate[aa] = index;
                polyYCoordinate[aa]=y;
                lastx=index;
                lasty=y;
                lastslope=slope;
            }
        }
        
        //Map farthest right pixel, from top
        y=top-1;
        index=right;
        
        do{
            y++;
            ip.setRoi(index, y+1, 1, 1);
            histogram = ip.getHistogram();
        } while (( histogram[ this.foreground ]==0 ) && y>=top);
        
        aa++;
        polyXCoordinate[aa]=index+1;
        polyYCoordinate[aa]=y;
        
        int ac=aa;
        
        y=bottom+1;
        index=right;
        
        //Map farthest right pixel, from bottom
        do{
            y--;
            ip.setRoi(index, y-1, 1, 1);
            histogram = ip.getHistogram();
        } while (( histogram[ this.foreground ]==0 ) && y>=top);
        
        aa++;
        polyXCoordinate[aa]=index+1;
        polyYCoordinate[aa]=y;
        
        lastx=right;
        lasty=y;
        lastslope=0;
        
        //Begin mapping bottom contours, right to left
        for (index = right-1; index >= left; index--){
            
            y=bottom+1;
            
            do{
                y--;
                ip.setRoi(index, y-1, 1, 1);
                histogram = ip.getHistogram();
            } while (( histogram[ this.foreground ]==0 ) && y>=top);
            
            if (y>=top){
                rise = lasty-y;
                run = lastx-index;
                slope = rise/run;
                aa++;
                polyXCoordinate[aa] = index;
                polyYCoordinate[aa]=y;
                lastx=index;
                lasty=y;
                lastslope=slope;
                
            }
        }
        
        y=bottom+1;
        index=left;
        
        //Map farthest left pixel, from bottom.  We've completed encircled the pixel area now
        do{
            y--;
            ip.setRoi(index, y-1, 1, 1);
            histogram = ip.getHistogram();
        } while (( histogram[ this.foreground ]==0 ) && y>=top);
        
        aa++;
        polyXCoordinate[aa]=index-1;
        polyYCoordinate[aa]=y;
        
        // This section of the method removes contours that contain concave
        // areas.  The algorithm tests each contour line to ensure that it has
        // a greater slope than all of the lines after it.  If this is not the case,
        // then it replaces the endpoint with the endpoint of the next contour
        // pixel, and checks again, until it has removed all of the contour points
        // that are concave.
        int ab=0;
        do{
            int ad=1;
            do{
                rise=polyYCoordinate[ab+1]-polyYCoordinate[ab];
                run=polyXCoordinate[ab+1]-polyXCoordinate[ab];
                if (run==0) run=1;
                slope=rise/run;
                
                rise=polyYCoordinate[ab+ad]-polyYCoordinate[ab];
                run=polyXCoordinate[ab+ad]-polyXCoordinate[ab];
                if (run==0) {
                    rise=10000;
                    run=1;
                }
                slope2=rise/run;
                
                if (slope>slope2){
                    polyXCoordinate[ab+1]=polyXCoordinate[ab];
                    polyYCoordinate[ab+1]=polyYCoordinate[ab];
                }
                ad++;
            } while (ad+ab<=ac);
            ab++;
            //IJ.showMessage(new Integer(ab).toString() + " " + new Integer(ad).toString());
            
        } while (ab<ac);
        
        ab=ac+1;
        do{
            int ad=2;
            do{
                rise=polyYCoordinate[ab+1]-polyYCoordinate[ab];
                run=polyXCoordinate[ab+1]-polyXCoordinate[ab];
                if (run==0) run=1;
                slope=rise/run;
                
                rise=polyYCoordinate[ab+ad]-polyYCoordinate[ab];
                run=polyXCoordinate[ab+ad]-polyXCoordinate[ab];
                if (run==0){
                    run=1;
                    rise=10000;
                }
                slope2=rise/run;
                
                if (slope>slope2){
                    polyXCoordinate[ab+1]=polyXCoordinate[ab];
                    polyYCoordinate[ab+1]=polyYCoordinate[ab];
                }
                ad++;
            } while (ad+ab<=aa);
            ab++;
        } while (ab<=aa);
        
        // remove countour points that are identical copies....
        index=1;
        do{
            if (polyXCoordinate[index]==polyXCoordinate[index-1] && polyYCoordinate[index]==polyYCoordinate[index-1]){
                for(int i=index; i<aa; i++){
                    polyXCoordinate[i]=polyXCoordinate[i+1];
                    polyYCoordinate[i]=polyYCoordinate[i+1];
                }
                if (index<=ac) ac--;
                aa--;
            }	else index++;
        }while (index<=aa);
        
        
        return new PolygonRoi(polyXCoordinate,polyYCoordinate, aa+1, img,Roi.POLYGON);
        
    }
    
    
    
    private void GetDimensions(ImageProcessor ip) {
        /*Determine the dimensions of the image, and sets some of the important variables.*/
        
        img = WindowManager.getCurrentImage();
        int[] histogram = new int[256];/*make an array for histogram*/
        this.width = ip.getWidth();/*record the width and height*/
        this.height = ip.getHeight();
        Rectangle roi;
        
        //start at the leftmost edge, move to the middle, and find the first coloured pixel for the left edge
        this.left = -1;
        do
        {	this.left++;
                ip.setRoi(this.left, 0, 1, this.height);
                histogram = ip.getHistogram();
        } while ( histogram[this.foreground]==0 );
        
        
        //start at the top and find the first pixel
        this.top = -1;
        do
        {	this.top++;
                ip.setRoi(this.left, this.top, this.width-this.left, 1);
                histogram = ip.getHistogram();
        } while (histogram[this.foreground]==0);
        
        //start at the right and find the first pixel
        right =width-1;
        do
        {	this.right--;
                ip.setRoi(this.right, this.top, 1, this.height-this.top);
                histogram = ip.getHistogram();
        } while (histogram[this.foreground]==0);
        
        //start at the bottom of the image and find the first pixel*/
        this.bottom =this.height;
        do
        {	this.bottom--;
                ip.setRoi(this.left, this.bottom, this.right-this.left, 1);
                histogram = ip.getHistogram();
        } while (histogram[this.foreground]==0);
        
        // This part of the methods finds the coordinates of the top, bottom, right and left pixels,
        // which is used later for the polygon.
        this.leftyc = 0;
        do
        {	this.leftyc++;
                ip.setRoi(this.left, this.leftyc, 1, 1);
                histogram = ip.getHistogram();
        } while (histogram[this.foreground]==0);
        
        this.topxc = this.left-1;
        do
        {	topxc++;
                ip.setRoi(topxc, top, 1, 1);
                histogram = ip.getHistogram();
        } while (histogram[this.foreground]==0);
        
        this.rightyc = this.top;
        do
        {	this.rightyc++;
                ip.setRoi(this.right, this.rightyc, 1, 1);
                histogram = ip.getHistogram();
        } while (histogram[this.foreground]==0);
        
        this.bottomxc = this.right;
        do
        {	this.bottomxc--;
                ip.setRoi(this.bottomxc, this.bottom, 1, 1);
                histogram = ip.getHistogram();
        } while (histogram[this.foreground]==0);
        
        this.imgHeight = bottom - top;
        this.imgWidth = right - left;
        this.centrex = (float)(left + (this.imgWidth/2));
        this.centrex = (float)(top + (this.imgHeight/2));
    }
    
    
}//end class
