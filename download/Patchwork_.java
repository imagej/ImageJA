/*----------------------------------------------------------------------------------------------------------------------------------
  Program created in the European Molecular Biology Laboratory in Heidelberg, Germany.
  Author: Kavagiou Zaharoula (Kavagiouzaharoula@yahoo.com)
  Supervisor / Group Leader: Dr Ernst Stelzer (stelzer@embl-heidelberg.de)
  ----------------------------------------------------------------------------------------------------------------------------------*/

/************************************************************************************************************
 *Class patchwork_  includes methods that create a complete image from a mosaic of images that overlap.
 *
 *The program can handle 8-bit, 16-bit and color images.
 *1) The user should provide the number of the images he/she wants to combine, in the beginning.
 *2)Then, the user should open the images in the correct order (from the one in the right bottom to the one
 *   on the left top, moving upwards. Imagine the mosaic as a matrix of images and start from element (n,m),
 *   then (n-1,m)...,(n,m-1),(n-1,m-1)...to (0,0). The File Opener plugin is used (as found on the webpage)
 *   in order to open the images. The user can also open the images before running the program and
 *   choose <cancel> when the File Opener runs.
 *3)The user should provide the number of the "rows" of the mosaic, the percentage of overlap vertically
 *    and the percentage of overlap horizontically.
 *4)The option of eliminating background abnormalities is given. The images are divided by a "background
 *    image"  that is nothing but an image with no elements (of interest) in it (e.g. no cells). When the user
 *    opens the background image, the program starts.
 *
 *Class patchwork_  uses classes:
 *     ...correl2_
 *     ...correl3_
 *     ...Final_proc_
 *     ...File Opener_
 ****************************************************************************************************************/

import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;
import ij.io.FileSaver;
import ij.plugin.filter.*;
import ij.plugin.*;
import ij.io.*;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;

public class Patchwork_ implements PlugIn {
/** im_num: total number of images*/
  private int im_num=0;
/** mcol: number of columns of images*/
  private int mcol;
/** mrow: number of rows*/
  private int mrows;
/**  list of open images*/
  private int[] wList;
/** list of created images*/
  private int[] cList;
/** final image*/
  public ImagePlus complete;
/**background image*/
  public ImagePlus back;
  public float overlap_ver, overlap_hor;
  private boolean error=false,backcor=false;
  public correl2_  cor2;
  public correl3_  cor3;
  public Final_proc_  div;



     public void run(String arg) {
      	if (showdialog()) {
	   if (mrows>1) create_column();
	   if (!error)  create_row();
	   if (!error) {
                        imshow();
                        new FileSaver( complete).saveAsTiff();
                      }
                   WindowManager.closeAllWindows();
                }
       }


/** Method that implements the interface of the program
 */

     public boolean showdialog() {
                       GenericDialog gd1= new GenericDialog("Opening the images...");                  // requests the images from the user
                        gd1.addMessage(  " Please open the images one after another.\n"+
                        		      " Start from the bottom right image of the mosaic\n"+
                        		      " and move towards the top one, as shown in \n"+
	     		      " the example of the 12-images mosaic with 3 columns: \n"+
                        		      " 12  8  4\n"+
                        		      " 11  7  3\n"+
                        		      " 10  6  2\n"+
                        		       "  9   5  1");
                        gd1.addNumericField(" Give the number of images of the mosaic :",0,0);
                        gd1.showDialog();
                        if (gd1.wasCanceled())
                           return false;
                        im_num=(int) gd1.getNextNumber();
                        IJ.runPlugIn("File_Opener"," ");
 	     wList=WindowManager.getIDList();

                       GenericDialog gd2 = new GenericDialog("Image Patchwork!");                // requests for the characteristics of the images
                        gd2.addNumericField("Give the number of rows of the mosaic :",0,0);
	     gd2.addNumericField("percentage (%) of overlap of columns: ",40,0);
	     gd2.addNumericField("percentage (%) of overlap of rows: ",27,0);
                        gd2.showDialog();
                        if (gd2.wasCanceled())
                           return false;
                        mrows=(int)gd2.getNextNumber();
  	     overlap_ver=(float) gd2.getNextNumber();                               // the percentage of vertical ovelap
      	     overlap_ver=(float)overlap_ver/100;
	     overlap_hor=(float) gd2.getNextNumber();	// the percentage of horizontal overlap
      	     overlap_hor=(float)overlap_hor/100;
                        for (int i=0; i<wList.length; i++) {
                              ImagePlus imp = WindowManager.getImage(wList[i]);
                        }

	     GenericDialog gd3= new GenericDialog("Background Correction");      // offers the option of background correction
                        gd3.addCheckbox(" Implement Background correction",true);
                        gd3. showDialog();
                        backcor= gd3.getNextBoolean();

	     if (backcor) {
                        GenericDialog gd4= new GenericDialog(" ");
                        gd4.addMessage(" Open the image you want as a background");
                        gd4.showDialog();
                        while (wList.length==im_num) {
                                 IJ.wait(1000);
                                wList =WindowManager.getIDList();
                         }
                        back=WindowManager.getCurrentImage();
                        }
                        return true;
                      }


/** Method that creates columns of images.
 * Create_column method combines all the images that belong in the same column of tha mosaic.
 * When it finishes, there are n images open, where n is the number of columns of the mosaic.
 */

     public void create_column() {
                   cor2= new correl2_();
                   div= new Final_proc_();
	int j=0,id;
                   ImagePlus imp1;
                   ImagePlus imp2;
                   cList= new int[im_num];
                   for (int l=0;l<=im_num-mrows;l+=mrows) {
                     imp1=WindowManager.getImage(wList[l]);                    //  the first image of each column
 	  if (backcor) {
                         ImageProcessor backip=back.getProcessor();          // background correction
                         ImageProcessor ip=imp1.getProcessor();
	      div.divide(ip,backip);
                      }
                     for (int i=l+1;i<l+mrows;i++) {
                        imp2= WindowManager.getImage(wList[i]);
                        if (backcor) {
                            ImageProcessor backip=back.getProcessor();
                            ImageProcessor ip2=imp2.getProcessor();
                            div.divide(ip2,backip);
                          }
     	     cor2.im_out(imp1,imp2,overlap_ver,backcor);         // correlation function for images that belong to the same column
                        if (cor2.fnum==0) {                                                          // no overlap found
                            message(cor2.mes);
                            IJ.write(" Correct the overlap of the columns!");
	         error=true;
                            break;
                            }
                        ImageWindow w1;
                        if (i!=l+1) w1=WindowManager.getCurrentWindow();          // when image1 is an original and not a created (combined) one
                        else w1=imp1.getWindow();                                                     // closes the windows of the images we used
                        w1.close();
                        ImageWindow w2=imp2.getWindow();
                        w2.close();
                        imp1=cor2.impf;
                        ImageProcessor ip1=imp1.getProcessor();
 	     new ImagePlus("new column",ip1).show();                        // displays the combined image
	     }
                   if (error) break;
                   imp1=WindowManager.getCurrentImage();                           //  final image of the column
                   cList[j] =imp1.getID();                                                                  // the final image (column) is saved in cList
  	imp1=WindowManager.getImage(cList[j]);
 	j++;
                  }
     }


/** Method that creates a row.
 * Create_row method combines the columns that were created by create_column method.
 * It produces the final image
 */

     public void create_row() {
	cor3=new correl3_();
                   ImagePlus imp1;
	ImagePlus imp2;
	mcol=im_num / mrows;
	if (mrows>1) imp1=WindowManager.getImage(cList[0]);      // mrow==1, when the user asks for a row of original images
                   else imp1=WindowManager.getImage(wList[0]);
                   for (int i=1;i<mcol;i++) {
	  if (mrows>1) imp2=WindowManager.getImage(cList[i]);
                     else imp2=WindowManager.getImage(wList[i]);
	  cor3.im_out(imp1,imp2,overlap_hor);
                     if (cor3.fnum==0) {                                                                    // matching could not be found
                            message(cor3.mes);
                            IJ.write("Correct the overlap of the rows!");
	         error=true;
                            break;
                         }
  	     ImageWindow w1;
                        if (i!=1) w1=WindowManager.getCurrentWindow();
                        else w1=imp1.getWindow();
                        w1.close();
                        ImageWindow w2=imp2.getWindow();
                        w2.close();
                        imp1=cor3.impf;
                        ImageProcessor ip=imp1.getProcessor();
                        new ImagePlus(" new row",ip).show();
	}
	if (!error)
	complete=imp1;
     }


/** Method that helps the user to correct wrong input.
 */

      public void message(String mes) {
	if (mes== "smaller")  IJ.write("wrong percentage of overlap.Try again with a smaller one");
	else if (mes== "larger") IJ.write("wrong percentage of overlap.Try again with a larger one");
                   else  IJ.write("wrong percentage of overlap.Try again with another one");
      }



/** Method that shows the final image
 */

   public void imshow() {
                   ImageProcessor c_ip;
	c_ip=complete.getProcessor();
 	new ImagePlus("Complete Image",c_ip).show();
    }




/********************************************************************************************************
 * Correl2_  is a class with methods to create a column of vertically overlapping (aligned) images
 ********************************************************************************************************/


class correl2_ implements PlugIn {
    private float ratio;
/** width and height of the 2 images that will be combined*/
    private int width1,width2,height1,height2;
    private int num,excounter=0;
/**  percentage of overlap between the two images*/
    private float overlap;
    private boolean doScaling = true,backcor;
    private  ImageProcessor final_image;
    public ImagePlus impf;
    public String mes=" gap ";
    public int fnum,loop;

    public void run(String arg) {
         ImagePlus imp1=null;                                                                     // the 2 images and the image we use to correct the background abnormalities
         ImagePlus imp2=null;
         ImagePlus back=null;
         im_out(imp1,imp2,overlap,backcor);
     }


/** Method that returns the final image.
 * Im_out calls the basic methods to produce the combined image
 */

    public void im_out (ImagePlus imp1, ImagePlus imp2, float overlap,boolean backcor) {
         this.overlap=overlap;
         fnum  = corloops(imp1,imp2,backcor);
         if (fnum!=0)  {
           imcreate(imp1,imp2);
           impf= new ImagePlus("final image",final_image);
         }    
      }


/** Method that finds the best match of two images.
 * Corloops calls the correlation method for two images, checks within a small range
 * of rows from the user's input and returns the row with the biggest 
 * similarity level 
 */

    public int corloops(ImagePlus imp1, ImagePlus imp2,boolean backcor) {  
        int ex1=0;			                        // number of pixels of same value on the row of merging                                                              
        width1 = imp1.getWidth();
        width2=imp2.getWidth();
        height1 = imp1.getHeight();
        height2 = imp2.getHeight();
        ImageProcessor ip1=imp1.getProcessor();
        ImageProcessor ip2=imp2.getProcessor();
        ImageProcessor  ip11,ip22;

        ip11=ip1.duplicate();                                                                          // making copy of ip1 to ip11 and convert ip11 to 8-bit grayscale IP
        ip11=ip11.convertToByte(doScaling);                                            // 2 IPs of imp1; ip1 & ip11; ip1 changes imp1,ip11 doesn't 
        if (!backcor)                                                                                          // implementing background correction on the images
           ip11=background(ip11,1,2);
        ip22=ip2.duplicate();
        ip22=ip22.convertToByte(doScaling);
        if (!backcor) 
           ip22=background(ip22,1,2);    
        ip11.autoThreshold();                                             		       // produce binary images	
        ip22.autoThreshold();

        num=(int) (overlap*(float)height2);  		                         //  number of rows overlapping
        float lowrange=0;
        if (overlap>=0.02) lowrange=overlap-(float)0.02;                         // range of overlap  (lowrange to highrange) !!! u can adjust the percentage to your needs!!!
        float highrange=1;
        if (overlap<1) highrange=overlap+(float)0.02;
        int numlow= (int)(lowrange*(float)height2);		//  lower limit of comparison  (in pixels)                  
        int numhigh=(int)(highrange*(float)height2);       	//  upper limit of comparison   
        float res=0,comp=0;   				           //  similarity levels
        loop=0;  			                                     	           // final number of overlapping rows	         
        for (int i=numlow; i<=numhigh;i++)  {
             res=correlate(ip11,ip22,i);
             if (res>comp) {                                                                                    // saving the best (biggest) level
                comp=res;
                loop=i;
                ex1=excounter;
               }
            }
        if (comp==0) loop=-1;
        if ((loop==numlow)||(loop==0)) {	                             	      // we didn't find a turning point (maximum) or the ratio was small
           mes= "smaller";         				      // should search towards the direction that the level increases 
           return (0);
          } 
        else if (loop==numhigh) {
           if ((ex1==width1)&&(ex1==width2))  {                                                            //comparing identical images!
              mes="per_match";
              return(loop);
             }
           mes="larger";
           return (0);
          }
        else if ((loop==-1)||((float)ex1/width1<(float)0.65)) {  		      // turning point was found but it is local maximum !!!adjust the percentage to your needs!!!
           mes="another"; 
           return (0);
          }
        else {
           mes="match";
           return (loop);
          }
      }


/** Method that returns the percentage of similarity of 2 images.
 * Correlate compares parts of two images- (w,h1) & (w,h2)-
 * defined by the user's input of overlapping rows 
 * and returns the level of their similarity
 */ 

    public float correlate(ImageProcessor ip11, ImageProcessor ip22,int num) {
        int z1, z2, count=0;
        int y2= 0, x2=0, x1=0, y1=0,counter=0;
        int w1=width1;
        int h2=height2-num; 		//  number of rows that don't overlap in imp2                                               
        int h1=num;                                             //  number of rows that overlap in imp1

        for ( x1=0; x1<w1; x1++,x2++)  {
             y2=h2;
             for ( y1=0; y1<h1; y1++,y2++) {
                 z1 = ip11.getPixel(x1,y1);		//  z-value (intensity) of pixel (x,y) in image1
                 z2 = ip22.getPixel(x2,y2); 		// z-value of pixel (x,y) in image2
                 counter++;
                 if (z1==z2) {
                   count++;  			                   //  number of pixels of imp1 & imp2 that have the same value

                   if ((y2==h2) && (y1==0))
                              excounter++; 		                          // pixels with the same value on the row/column where the overlap will begin
                  }
               } 
            }
        if (counter==0) counter=1;
        ratio= (float) count/counter;    	             //  percentage of similarity                           
        if (ratio<0.64)                                                             // adjust the percentage to your needs
           return(0);
        else
           return (ratio);
     }


/** Method that creates the new image (same type as the original) combining the two overlapping ones 
 */

   public void imcreate(ImagePlus imp1,ImagePlus imp2) {
       int final_width=width1;
       int final_height=height1+height2-fnum;
       ImageProcessor ip1=imp1.getProcessor();
       ImageProcessor ip2=imp2.getProcessor();
       if ((imp1.getType()==imp1.GRAY8) || (imp1.getType()==imp1.COLOR_256))
           final_image=new ByteProcessor(final_width,final_height); 	   
       else if (imp1.getType()==imp1.GRAY16)
           final_image=new ShortProcessor(final_width,final_height);	     
       else if (imp1.getType()==imp1.GRAY32)
           final_image=new FloatProcessor(final_width,final_height);
       else final_image=new ColorProcessor(final_width,final_height);
       final_image.insert(ip2,0,0);
       final_image.insert(ip1,0,height2-fnum); 
       if (final_image instanceof ShortProcessor)
           final_image.setMinAndMax(0.0,65355.0);                                                   // normalizing the 16-bit image
}


/** Method that intensifies-corrects the background opposition to the region of interest.
  * Uses rank filters and is used only when no background correction takes place 
 */

   public ImageProcessor background(ImageProcessor ip,int iteration,int radius) { 
       int  wb = ip.getWidth();
       int hb = ip.getHeight();      
       ImageProcessor ipcopy=new ByteProcessor(wb,hb);    
       ipcopy.insert(ip,0,0);
       RankFilters rankFilter=new RankFilters();  		             // use of rank filters on the copy of the image
       for(int i=1; i<=iteration; i++)  {
           rankFilter.rank(ipcopy, radius, 2);
          }
       ip.copyBits(ipcopy,0,0,ByteBlitter.SUBTRACT);       	 // subtract ipcopy from ip; eliminate background noise
       return(ip);
   }


}


/********************************************************************************************************
 * Correl3_  is a class with methods to create a row of horizontically overlaping (aligned) images 
 ********************************************************************************************************/

class correl3_ implements PlugIn {

    private double rows,columns; 
/**  width and height of the images that will be combined*/	
    private int width1,width2,height1,height2;
    private int num,excounter=0;
    private int shift;                                                                                                         //  shift one of the images by  "shift " rows to overlap it correctly
    private float overlap,ratio;
    private boolean doScaling = true;
    public ImagePlus impf;
    private ImageProcessor final_image;
    private int dif=0,minh=0,loop;
    public int fnum;
    public String mes=" gap ";

    public void run(String arg) {
          ImagePlus imp1=null;
          ImagePlus imp2=null;
          im_out(imp1,imp2,overlap);
}


/** Method that returns the final image.
 * Im_out calls the basic methods to produce the combined image
 */

   public void im_out (ImagePlus imp1, ImagePlus imp2, float overlap) {
        this.overlap=overlap;
        fnum  = corloops(imp1,imp2);
        if (fnum!=0)  {
          imcreate(imp1,imp2);
          impf= new ImagePlus("final image",final_image);
        }
   }


/** Method that finds the best match of two images.
 * Corloops calls the correlation method for two images, checks within a small range
 * of columns from the user's input and returns the column with the biggest
 * similarity level.
 */

    public int corloops(ImagePlus imp1,ImagePlus imp2) {
        int ex1=0;				// number of pixels of same value on the column of merging
        width1 = imp1.getWidth();
        width2 = imp2.getWidth();
        height1 = imp1.getHeight();
        height2 = imp2.getHeight();
        ImageProcessor ip1=imp1.getProcessor();
        ImageProcessor ip2 = imp2.getProcessor(); 
        ImageProcessor ip11,ip22;
        ip11=ip1.duplicate();
        ip11=ip11.convertToByte(doScaling);         
        ip11=background(ip11,1,2);
        ip22=ip2.duplicate();
        ip22=ip22.convertToByte(doScaling);
        ip22=background(ip22,1,2);
        ip11.autoThreshold();                                             		             // produce binary images	
        ip22.autoThreshold();

        num=(int) (overlap*(float)width2);              		       // number of columns overlapping         
        float lowrange=0;
        if (overlap>=0.02) lowrange=overlap-(float)0.02;                             //  range of overlap  (lowrange to highrange) !!!adjust this percentage to your needs
        float highrange=1;
        if (overlap<1) highrange=overlap+(float)0.02;    
        int numlow= (int)(lowrange*(float)width2);                              // lower limit of comparison  (in pixels)                                         
        int numhigh=(int)(highrange*(float)width2);                      // upper limit of comparison             	       
        float res=0,comp=0;                             //  similarity levels
        loop=0;                // number of overlapping rows 		          	    

        dif=Math.max(height1,height2)-Math.min(height1,height2);      // difference of size between the 2 images

        for (int j=0;j<=dif;j++) {                                                                          // loop that defines the shift of one image compared to the other ; the shift is defined by the difference |height1-height2| 
         for (int i=numlow; i<=numhigh;i++) {                                                // loop that checks for overlap from numlow column to numhigh
             res=correlate(ip11,ip22,i,j);
             if (res>comp) {                                                                                 // saving the best (biggest) level
                 comp=res;
                 loop=i;
                 shift=j;
                 ex1=excounter;
               }
             } 
            if (comp==0) loop=-1;
           }
          if ((loop==numlow)||(loop==0)) { 	                             	      // we didn't find a turning point (maximum) or the ratio was small
              mes="smaller";				      // should search towards the direction that the level increases 
              return (0);
             } 
        else if (loop==numhigh) {
          if ((height1==height2)&&(ex1==height1)) {                                //comparing identical images!
           mes="per_match";
           return(loop);
          }
          mes="larger";
          return (0);
        }
   else if ((loop==-1)||((float)ex1/minh<(float)0.65)) {                        // turning point was found but it's a local maximum !!!adjust this percentage to your needs!!!
       mes="another";
       return (0);
      }
   else {
      mes="match";   
      return (loop);
    } 
}



/** Method that returns the percentage of similarity of 2 images.
 * Correlate compares parts of two images- (w1,h1) & (w2,h2)-
 * defined by the user's input of overlapping columns
 * and returns the level of their similarity. The shift is taken into account.
 */ 

    public float correlate(ImageProcessor ip11, ImageProcessor ip22,int num,int offset) {
        int z1, z2, count=0;
        int y2= 0, x2=0, x1=0, y1=0,counter=0;
        int offy1=0,offy2=0;                             // starting points of comparisons for y coordinate
        excounter=0;
        int w1=num;       // number of columns that overlap in imp1
        int w2=width2-num;                            // number of columns that don't overlap in imp2                                         
        int h2=height2;
        int h1=height1;

        minh=Math.min(h1,h2);                      // the smallest height of the two images
        if (minh==h1) offy2=offset;
        else offy1=offset;
        y2=offy2;

        for ( y1=offy1; y1<minh; y1++,y2++) {
            x2=w2;
            for ( x1=0; x1<w1; x1++,x2++) {
                z1 = ip11.getPixel(x1,y1);                                // z-value (intensity) of pixel (x,y) in image1
                z2 = ip22.getPixel(x2,y2);                      //  z-value of pixel (x,y) in image2 		
                counter++;
                if (z1==z2) {
                   count++;                                                        // number of pixels of imp1 & imp2 that have the same value
                   if ((x2==w2) && (x1==0))
                            excounter++;                                    //comparing the column where the merging will begin
                 }
                }
            }
        if (counter==0) counter=1;
       ratio= (float) count/counter;                                                //  percentage of similarity   
       if (ratio<0.7) return(0);                                                       // !!!adjust this percentage to your needs!!!
       else
       return (ratio);
   }


/** Method that creates the new image (same type as the original) combining the two overlapping ones 
 */

   public void imcreate(ImagePlus imp1,ImagePlus imp2) {
      int final_width=width1+width2-fnum;		//  width of the final-combined image
      int final_height=Math.max(height1,height2);                    // height of the final-combined image                                      
      ImageProcessor ip1 = imp1.getProcessor();
      ImageProcessor ip2 = imp2.getProcessor();
      if ((imp1.getType()==imp1.GRAY8) || (imp1.getType()==imp1.COLOR_256))
             final_image=new ByteProcessor(final_width,final_height); 	     
      else if (imp1.getType()==imp1.GRAY16)
            final_image=new ShortProcessor(final_width,final_height);       
      else if (imp1.getType()==imp1.GRAY32)
           final_image=new FloatProcessor(final_width,final_height);
       else
           final_image=new ColorProcessor(final_width,final_height);
       if (final_height==height1) {                                                                               // creating final image taking shift into consideration
            final_image.insert(ip1,width2-fnum,0);
            final_image.insert(ip2,0,final_height-height2-shift);
         }
       if (final_height==height2) {   
           final_image.insert(ip2,0,0);
           final_image.insert(ip1,width2-fnum,final_height-height1-shift);
         }
        if (final_image instanceof ShortProcessor)
           final_image.setMinAndMax(0.0,65355.0);                                                   // normalizing the 16-bit image
  }


/** Method that intensifies-corrects the background opposition to the region of interest.
  * Uses rank filters and is used only when no background correction takes place
  */

   public ImageProcessor background(ImageProcessor ip,int iteration,int radius) { 
       int  wb = ip.getWidth();
       int hb = ip.getHeight();      
       ImageProcessor ipcopy=new ByteProcessor(wb,hb);
       ipcopy.insert(ip,0,0);
       RankFilters rankFilter=new RankFilters();               // use of rank filters on the copy of the image
       for (int i=1; i<=iteration; i++)
          {
           rankFilter.rank(ipcopy, radius, 2);
          }
       ip.copyBits(ipcopy,0,0,ByteBlitter.SUBTRACT);        // subtract ipcopy from ip; eliminate background noise
       return(ip);  
   }

}

/*************************************************************************************************
 * Final_proc_  class produces the result of the division between images of the same type.
 * In the case of the patchwork_ program, it is used to divide the background image by the original,
 * so that the noise and background abnormalities (e.g. caused by the incoherent illumination
 * of the microscope lamp) are eliminated
 */

class Final_proc_ implements PlugIn {

public void run(String arg) {
                   ImageProcessor ip1=null;
	ImageProcessor ip2=null;
                  divide(ip1,ip2);
          }


/** Method that divides 2 images using another method depending on their type
 */

public void divide(ImageProcessor ip1,ImageProcessor ip2) {
 	if (ip1 instanceof ByteProcessor) divide8bit(ip1,ip2);
                   else if (ip1 instanceof ShortProcessor) divide16bit(ip1,ip2);
	else dividecolor(ip1,ip2);
 }


/** Dividing 8-bit images
 */

public void divide8bit(ImageProcessor ip1,ImageProcessor ip2) {
	byte[] srcpixels;
	byte[] pixels;
                   int src,dstIndex=0,srcIndex=0;
                   float dst;
	int w= ip1.getWidth();
	int h=ip1.getHeight();
                   pixels=(byte[])ip1.getPixels();
	srcpixels=(byte[])ip2.getPixels();
                   for (int i=w*h;--i>=0;) {
                        src = (int)(srcpixels[srcIndex++]&0xff);                // src= image that we want to divide by the dst image
                        if (src==0)
                            dst = (float)256.0;
                        else
                            dst =(float)(210.0*( (float) (pixels[dstIndex]&0xff)) / ((float)src));
                        pixels[dstIndex++] = (byte)dst;
                       }
     }


/** Dividing 16-bit images
 */

public void divide16bit(ImageProcessor ip1,ImageProcessor ip2) {
	short[] srcpixels;
	short[] pixels;
                   int src,dstIndex=0,srcIndex=0;
                   float dst;
	int w= ip1.getWidth();
	int h=ip1.getHeight();
                   pixels=(short[])ip1.getPixels();
	srcpixels=(short[])ip2.getPixels();
                   for (int i=w*h;--i>=0;) {
                        src = srcpixels[srcIndex++]&0xffff;
                        if (src==0)
                            dst = (float)65535.0;
                        else
                            dst =(float)(62000.0*( (float) (pixels[dstIndex]&0xffff)) / ((float)src));
                        pixels[dstIndex++] = (short)dst;
                       }
     }


/** Dividing color images
 */

public void dividecolor(ImageProcessor ip1,ImageProcessor ip2) {
	int[] srcpixels;
	int[] pixels;
                   int pixr,pixg,pixb;
                   int srcr,srcb,srcg,src,dstIndex=0,srcIndex=0;
                   float dst,dstr,dstg,dstb;
	int w= ip1.getWidth();
	int h=ip1.getHeight();
                   pixels=(int[])ip1.getPixels();
	srcpixels=(int[])ip2.getPixels();
                   for (int i=w*h;--i>=0;) {
                        srcr =(int) (srcpixels[srcIndex]&0xff0000)>>16;                   // seperate division for each color (red, green, blue)
                        srcg =(int) (srcpixels[srcIndex]&0x00ff00)>>8;
 	     srcb =(int) (srcpixels[srcIndex]&0x0000ff);
                        src=((srcr & 0xff) <<16) + ((srcg & 0xff) << 8) + (srcb & 0xff);
                        if (src==0) {
                            dstr = (float) 256.0;
	         dstg = (float) 256.0;
	         dstb = (float) 256.0;
	        }
                        else {
                            pixr =(int) (pixels[srcIndex]&0xff0000)>>16;
                            pixg =(int) (pixels[srcIndex]&0x00ff00)>>8;
                            pixb=(int) (pixels[srcIndex]&0x0000ff);

                            dstr =(float)(210.0*( (float) (pixr&0xff)) / ((float)srcr));
	         dstg =(float)(210.0*( (float) (pixg&0xff)) / ((float)srcg));
	         dstb =(float)(210.0*( (float) (pixb&0xff)) / ((float)srcb));
                            }
	         dst=(float)((((int)dstr) & 0xff) <<16) + ((((int)dstg) & 0xff) << 8) + (((int)dstb) & 0xff);
                        pixels[dstIndex++] = (int)dst;
                        srcIndex++;
                       }
     }

}



/** Uses the JFileChooser from Swing to open one or more images.
*/

class File_Opener  implements PlugIn {
	private File dir;

	public void run(String arg) {
		openFiles();
		IJ.register( File_Opener .class);
	}

	public void openFiles() {
		JFileChooser fc = null;
		try {fc = new JFileChooser();}
		catch (Throwable e) {IJ.error("This plugin requires Java 2 or Swing."); return;}
		fc.setMultiSelectionEnabled(true);
		if (dir==null) {
			String sdir = OpenDialog.getDefaultDirectory();
			if (sdir!=null)
				dir = new File(sdir);
		}
		if (dir!=null)
			fc.setCurrentDirectory(dir);
		int returnVal = fc.showOpenDialog(IJ.getInstance());
		if (returnVal!=JFileChooser.APPROVE_OPTION)
			return;
		File[] files = fc.getSelectedFiles();
		if (files.length==0) { // getSelectedFiles does not work on some JVMs
			files = new File[1];
			files[0] = fc.getSelectedFile();
		}
		String path = fc.getCurrentDirectory().getPath()+Prefs.getFileSeparator();
		dir = fc.getCurrentDirectory();
		Opener opener = new Opener();
		for (int i=0; i<files.length; i++) {
			ImagePlus img = opener.openImage(path, files[i].getName());
			if (img!=null)
				img.show();
		}
	}

}
}
