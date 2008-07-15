import ij.*;
import ij.process.*;
import ij.gui.*;
import java.util.*;
import java.awt.*;
import ij.plugin.filter.*;


/**
 *
 * @version 1.0;  23 Dec 2004
 * @author Dimiter Prodanov
 * @author University of Leiden
 * @contents This plugin separately measures the red, green and blue channels of an RGB image
 *  between user-definable threshold levels per channel. It is best used together with the
 *  Threshold_Colour plugin (by Bob Dougherty  and Gabriel Landini) or my ColorHistogram.
 *  The idea and original realization of this plugin belong to	Wayne Rasband, NIH
 *
 * @license This library is free software; you can redistribute it and/or
 *      modify it under the terms of the GNU Lesser General Public
 *      License as published by the Free Software Foundation; either
 *      version 2.1 of the License, or (at your option) any later version.
 *
 *      This library is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *       Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public
 *      License along with this library; if not, write to the Free Software
 *      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

public class RGB_Measure_Plus implements PlugInFilter {
    private ImagePlus imp;
    private static final String RTMIN="rTmin", RTMAX="rTmax", 
    GTMIN="gTmin", GTMAX="gTmax", BTMIN="bTmin", BTMAX="bTmax";
       
    private static int rtmin=Prefs.getInt(RTMIN,0);
    private static int rtmax=Prefs.getInt(RTMAX,255);
    private static int gtmin=Prefs.getInt(GTMIN,0);
    private static int gtmax=Prefs.getInt(GTMAX,255);
    private static int btmin=Prefs.getInt(BTMIN,0);
    private static int btmax=Prefs.getInt(BTMAX,255);
    private int[] tamin=new int[3];
    private int[] tamax=new int[3];
    private byte[] mask;
    private int area=1;
    private int width, height=1;
    /**
     * stores the histogram means
     */    
    protected double[] histMean=new double[3];
    /**
     * stores the histogram area per channel
     */    
    protected double[] histArea=new double[3];
    /**
     * stores the histogram standard deviation
     */    
    protected double[] histStdev=new double[3];
    /**
     * stores the 3-channel ROI histogram
     */    
    protected int[][] histogram=new int [3][256];
    
    boolean showDialog(ImagePlus imp)   {
        
        if (imp==null) return true;
        GenericDialog gd=new GenericDialog("Thresholds");
        
        gd.addMessage("This plugin performs channel measurements\n");
        gd.addNumericField("red_threshold_min",rtmin,0);
        gd.addNumericField("red_threshold_max",rtmax,0);
        gd.addNumericField("green_threshold_min",gtmin,0);
        gd.addNumericField("green_threshold_max",gtmax,0);
        gd.addNumericField("blue_threshold_min",btmin,0);
        gd.addNumericField("blue_threshold_max",btmax,0);
        gd.showDialog();

        if (gd.wasCanceled())
            return false;
        
        rtmin=(int)gd.getNextNumber();
        rtmax=(int)gd.getNextNumber();
        gtmin=(int)gd.getNextNumber();
        gtmax=(int)gd.getNextNumber();
        btmin=(int)gd.getNextNumber();
        btmax=(int)gd.getNextNumber();
              
        return true;
    } /* showDialog */
    
    /*------------------------------------------------------------------*/
    void showAbout() {
        IJ.showMessage("About RGB Measure...",
        "This plug-in separately measures the red, green and blue channels"
        );
    }
    
    /**
     * runs the plugin
     */    
    public void run(ImageProcessor ip) {
      //  IJ.log("rmin - "+rtmin+ " rmax - "+rtmax+" gmin - "+gtmin+" gmax -"+gtmax+" bmin - "+btmin+" btmax - "+btmax);
        tamin[0]=rtmin; tamin[1]=gtmin; tamin[2]=btmin;
        tamax[0]=rtmax; tamax[1]=gtmax; tamax[2]=btmax;
        
      //  imp.unlock();
        ImageProcessor ipmask = imp.getMask();
        mask=ipmask!=null?(byte[])ipmask.getPixels():null;
        width=imp.getWidth();
        height=imp.getHeight();
     //   if (ip instanceof ColorProcessor) {
          
        Rectangle rect;
        try {
            rect=imp.getRoi(). getBoundingRect();
            area=rect.width*rect.height;
        }
        catch (NullPointerException e) {
            rect=new Rectangle(width,height);
            area=width*height;
        }
       //  IJ.log("area "+area);
        int [] pixels=(int[])imp.getProcessor().getPixels();
       //  IJ.log(" rect h: "+rect.height+" rect w: "+rect.width);
        if (mask!=null) {
           
            histogram= getHistogram(width, pixels,mask, rect);
        }
        else  {
            histogram = getHistogram(width,pixels, rect);
          //  IJ.log("mask null");
        }
        calculateStatistics(histogram,tamin,tamax);
   
            IJ.setColumnHeadings("parameter\tred\tgreen\tblue");
            IJ.write("mean\t"+IJ.d2s(histMean[0],2)+ "\t"+IJ.d2s(histMean[1],2)+"\t"+IJ.d2s(histMean[2],2));
            IJ.write("st. dev\t"+IJ.d2s(histStdev[0],2)+ "\t"+IJ.d2s(histStdev[1],2)+"\t"+IJ.d2s(histStdev[2],2));
            IJ.write("area fraction\t"+IJ.d2s(histArea[0],4)+ "\t"+IJ.d2s(histArea[1],4)+"\t"+IJ.d2s(histArea[2],4));
       // }
    }
    
    /**
     * calculates the histogram
     */    
    public int[][] getHistogram(int width, int[] pixels, Rectangle roi) {
        
        int c, r, g, b, v;
        int roiY=roi.y;
        int roiX=roi.y;
        int roiWidth=roi.width;
        int roiHeight=roi.height;
        int[][] histogram = new int[3][256];
        for (int y=roiY; y<(roiY+roiHeight); y++) {
            int i = y * width + roiX;
            for (int x=roiX; x<(roiX+roiWidth); x++) {
                c = pixels[i++];
                r = (c&0xff0000)>>16;
                g = (c&0xff00)>>8;
                b = c&0xff;
    
                histogram[0][r]++;
                histogram[1][g]++;
                histogram[2][b]++;
            }
            //if (y%20==0)
            //showProgress((double)(y-roiY)/roiHeight);
        }
        //hideProgress();
        return histogram;
    }
   
    /** Value of pixels included in masks. */
    public static final int BLACK = 0xFF000000;
    
    /**
     * calculates the histogram
     */    
    public int[][] getHistogram(int width, int[]pixels, byte[] mask, Rectangle roi) {
        int c, r, g, b, v;
        int[][] histogram = new int[3][256];
        int roiY=roi.y;
        int roiX=roi.y;
        int roiWidth=roi.width;
        int roiHeight=roi.height;
        for (int y=roiY, my=0; y<(roiY+roiHeight); y++, my++) {
            int i = y * width + roiX;
            int mi = my * roiWidth;
            for (int x=roiX; x<(roiX+roiWidth); x++) {
                if (mask[mi++]!=BLACK) {
                    c = pixels[i];
                    r = (c&0xff0000)>>16;
                    g = (c&0xff00)>>8;
                    b = c&0xff;
                    //v = (int)(r*0.299 + g*0.587 + b*0.114 + 0.5);
                    histogram[0][r]++;
                    histogram[1][g]++;
                    histogram[2][b]++;
                }
                i++;
            }
            //if (y%20==0)
            //showProgress((double)(y-roiY)/roiHeight);
        }
        //hideProgress();
        return histogram;
    }
    
    /**
     * calculates the statistics histogram between Tmin and Tmax
     */    
    protected void calculateStatistics(int[][]histogram,int[]tmin, int[]tmax) {
        for (int col=0;col<3;col++) {
           double cumsum=0;
            double cumsum2=0;
            double aux=0;
            for (int i=tmin[col]; i<=tmax[col];i++){
                cumsum+=i*histogram[col][i];
                aux+=histogram[col][i];
                cumsum2+=i*i*histogram[col][i];
            }
            //IJ.log("cumsum: "+cumsum);
            // IJ.log("cumsum2: "+cumsum2);
            histMean[col]=cumsum/area;
            histArea[col]=aux/area;
            double stdDev = (area*cumsum2-cumsum*cumsum)/area;
            histStdev[col] = Math.sqrt(stdDev/(area-1.0));
            
        }
        
    }
    
    /**
     * sets up the plugin parameters
     */    
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        IJ.register(RGB_Measure_Plus.class);
        if (arg.equals("about")){
            showAbout();
            return DONE;
        }
        if(IJ.versionLessThan("1.32") || !showDialog(imp)) {
            return DONE;
        }
        else {
            return DOES_RGB+NO_UNDO;
        }
    }
    
    
    /**
     * saves parameters
     */    
     public static void savePreferences(Properties prefs) {
              
        prefs.put(RTMIN, Integer.toString(rtmin));
        prefs.put(RTMAX, Integer.toString(rtmax));
        prefs.put(GTMIN, Integer.toString(gtmin));
        prefs.put(GTMAX, Integer.toString(gtmax));
        prefs.put(BTMIN, Integer.toString(btmin));
        prefs.put(BTMAX, Integer.toString(btmax));

    }
}
