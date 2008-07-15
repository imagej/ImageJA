import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.measure.*;
import ij.util.*;

/**
 *     Granulometry of images
 *     @version 1.1.1 31 May, 2003
 *
 *     @author	Dimiter Prodanov
 *     @author  University of Leiden
 *      This plugin performs granulometry of grayscale images
 *      I use circular structure elements - the RankFilter object provided in the ImageJ
 *
 *      Copyright (C) 2003 Dimiter Prodanov
 *
 *      This library is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *       Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public
 *      License along with this library; if not, write to the Free Software
 *      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

public class Granulometry_  implements PlugInFilter {
    ImagePlus imp, copy1;
    private  float MinRadius=0;
    private  float MaxRadius=1;
    private float step=0;
    private static int options=0;
    private static int showoptions=0;
    private static String[] items={"radius", "area","nothing"};
    private static String[] showitems={"no", "yes"};
    
  /*------------------------------------------------------------------*/ 
    /**
     *  Overloaded method of PlugInFilter.
     *
     * @param  arg  Optional argument, not used by this plugin.
     * @param  imp  Optional argument, not used by this plugin.
     * @return   Flag word that specifies the filters capabilities.
     */
    public int setup(String arg, ImagePlus imp){
        this.imp=imp;
        IJ.register(Granulometry_.class);
        if (arg.equals("about")){
            showAbout();
            return DONE;
        }
        if(IJ.versionLessThan("1.23") || !showDialog(imp)) {
            return DONE;
        }
        else {
            return DOES_8G;
        }
    } /* setup */
    
  /*------------------------------------------------------------------*/ 
   /**
     *  The main class of the plugin
     *
     * @param  ip  The current ImageProcessor.
     */
    public void  run(ImageProcessor ip) {
		Runtime.getRuntime().gc();
        final int ratio=(int)((MaxRadius-MinRadius)/step);
        int[] pixcounts= new int[ratio+1];
        float[] res= new float[ratio+1];
        float[] radii=new float[ratio+1];
        ImageProcessor ip1;
        double content=0;
        ImageStatistics stats1 = imp.getStatistics();
        pixcounts[0] =  getPixelSum(stats1.histogram,0,255);
        //float mean=(float) stats1.mean;
        float cal=pixcounts[0];
        IJ.setColumnHeadings("radius"+"\t"+"area*mean"+"\t"+"sum of pixels"+"\t"+"density");
        int counter=0;
        float r=MinRadius;
        while   (r<=MaxRadius) {
            IJ.showStatus("Please wait ...  Iteration  " + r);
            copy1=duplicateImage(ip);
            ip1=copy1.getProcessor();
            //stats1 = ip1.getStatistics();
            ImgGrayOpen(ip1, r);
            
            pixcounts[counter] =  getPixelSum(ip1.getHistogram(),0,255);
            radii[counter]=r;
            if (counter>0) res[counter]=-(pixcounts[counter] - pixcounts[counter-1])/cal;
            content=Math.PI*r*r*255;
            IJ.write(r+"\t"+IJ.d2s(content)+"\t"+IJ.d2s(pixcounts[counter])+"\t"+IJ.d2s(res[counter],4));
            
            
            if (showoptions==1){
                copy1.setProcessor("Image "+r, ip1);
                copy1.show();
                
                copy1.updateAndDraw();
            }
            
            if (pixcounts[counter]==0) break;
            copy1=null;
            ip1=null;
            r+=step;
            counter++;
        }
        if (options!=2) {
            plot(res, radii, "Density distribution",items[options]+ " of opening","Rel. Density",options);
        }
        
    } /* run */
	
   /*------------------------------------------------------------------*/ 
    public  int getPixelSum(int[] histogram, int minThreshold, int maxThreshold) {
        int count;
        int sum = 0;
        for (int i=minThreshold; i<=maxThreshold; i++) {
            count = histogram[i];
            sum += i*count;
        }
        return sum;
    } /* getPixelSum */


   /*------------------------------------------------------------------*/ 
    private void   plot(float[] arr, float[] radii, String Plabel, String Xlabel, String Ylabel, int option ) {
        float[] px=new float[arr.length];
        float[] py=new float[arr.length];
        String note="";
        
        for (int i=0; i<arr.length;i++){
            switch (option) {
                case 0:{  //radius
                    px[i]=radii[i];
                    note="2*radius+1";
                    break;
                }
                case 1: { //area
                    px[i]=(float)Math.PI*(radii[i])*(radii[i]);
                    note="2*sqrt(area/Pi)+1";
                    break;
                }
            }
            py[i]=arr[i];
        }
        
        PlotWindow pw = new PlotWindow(Plabel,Xlabel,Ylabel, px, py);
        
        double[] a = Tools.getMinMax(px);
        double xmin=a[0], xmax=a[1];
        a = Tools.getMinMax(py);
        double ymin=a[0], ymax=a[1];
        pw.setLimits(xmin,xmax,ymin,ymax);
        pw.addPoints(px,py, PlotWindow.CIRCLE);
        
        pw.addLabel(0.6, 0.2, "Note: particle size="+note);
        
        pw.draw();
        
    } /* plot */

 /*------------------------------------------------------------------*/ 
    private ImagePlus duplicateImage(ImageProcessor iProcessor){
        int w=iProcessor.getWidth();
        int h=iProcessor.getHeight();
        ImagePlus iPlus=NewImage.createByteImage("Image", w, h, 1, NewImage.FILL_BLACK);
        ImageProcessor imageProcessor=iPlus.getProcessor();
        imageProcessor.copyBits(iProcessor, 0,0, Blitter.COPY);
        return iPlus;
    } /* duplicateImage */
    
/*------------------------------------------------------------------*/ 
    public void ImgGrayOpen(ImageProcessor ip, float radius){
        /** We try to perform graylevel erosion
         *  followed by graylevel dilation
         *  with radius=radius
         **/
        RankFilters rf=new RankFilters();
        rf.rank(ip, radius, rf.MIN); //erosion
        rf.rank(ip, radius, rf.MAX);//dilation
    } /* ImgGrayOpen */

   /*------------------------------------------------------------------*/ 
    boolean showDialog(ImagePlus imp)   {
        
        if (imp==null) return true;
        GenericDialog gd=new GenericDialog("Parameters");
        
        // Dialog box for user input
        gd.addMessage("This plugin performs granulometry\n");
        // radius=size/2-1;
        gd.addNumericField("Minimal radius of opening (pixels):", 0, 1);
        
        gd.addNumericField("Maximal radius of opening (pixels):", 10, 1);
        gd.addNumericField("Step of increase (pixels):", 1, 1);
        
        gd.addChoice("Plot Intensity vs.", items, items[0]);
        
        gd.addChoice("Show images", showitems, showitems[0]);
        gd.showDialog();
        
        MinRadius=(float)gd.getNextNumber();
        MaxRadius=(float)gd.getNextNumber();
        step=(float) gd.getNextNumber();
        //      IJ.write(IJ.d2s(step));
        options=gd.getNextChoiceIndex();
        showoptions=gd.getNextChoiceIndex();
        
        // IJ.write(MinRadius+"\t"+MaxRadius);
        if (gd.wasCanceled())
            return false;
        
        
        if (!validate(MinRadius) || !validate(MaxRadius) || (MaxRadius<=MinRadius)){
            IJ.showMessage("Invalid Numbers!\n" +
            "Enter Integers equal or greater than 2");
            return false;
        }
        
        if (!validate(step)){
            IJ.showMessage("Invalid Numbers!\n" +
            "Enter floats 0.5 or 1");
            return false;
        }
        
        return true;
    } /* showDialog */

   /*------------------------------------------------------------------*/ 
    private boolean validate ( float var){
        float a=2*var;
        int b=(int) (2* var);
        // IJ.log(IJ.d2s(a-b));
        if ((a-b==0)||(var<0))
            return true;
        else return false;
    } /* validate */

    /*------------------------------------------------------------------*/
    void showAbout() {
        IJ.showMessage("About Granulometry...",
        "This plug-in filter performs granulometry on images"
        );
    } /* showAbout */
}
