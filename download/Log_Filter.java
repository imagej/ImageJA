/*************************************************************************\
 * LoG Filtering Plug-in for Image/J					 	 *
 * Written by Lokesh Taxali, Email: lkt@cse.unsw.edu.au		 	 *
 * based on algorithm by Dr. Jesse Jin, Email: jesse@cse.unsw.edu.au	 *
 * This plug-in is designed to perform LoG Filtering on an 8-bit    	 *
 * grayscale image.									 *
 *												 *
 \************************************************************************/


/* Importing standard Java API Files and Image/J packages */
import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;
import java.io.*;
import java.lang.*;
import ij.gui.*;

/*
 * "Log_Filter" is the main class that implements the plug-in
 * 'PlugInFilter' is the Imgage/J interface that has to be
 * implemented by this LoG Filtering plug-in
 *
 * Notes on significance of user input (via GUI) and suggested values:
 * Sigma (cfsize) : The width of filter centre 				(3,5,9,17,35)
 * FilterWidth 'w': The width of filter, aka support			(2,3,4)
 * Threshold 'thr': The threshold for 1 D DoG Filtering
 * Delta          : The delta level for adjusting zero crossings	(-1 .. 1)
 * Mode of output : 0: Filtering results in intensity 0 .. 255
 *			  1: absolute value of filtering
 *			  2: results representing values -1, 0 or 1
 *			  3: Zero crossings overlaid on original image
 *			  4: Zero crossings only
 *
 */

public class Log_Filter implements PlugInFilter {

		// The following are the input parameters, with default values assigned to them
		int cfsize=3;
	      float w=2 ,delta=0 , thr=0;
		int mode=0;
		boolean answer;
		boolean flag;
//-----------------------------------------------------------------------------------
	private boolean GUI()
	{

		GenericDialog gd = new GenericDialog("Enter Values", IJ.getInstance());

		String[] mode_option = {"0","1","2","3","4"};
		gd.addNumericField("Sigma (3,5,9,17,35)", cfsize, 0);
		gd.addNumericField("FilterWidth (2,3,4)", w, 0);
		gd.addNumericField("Threshold (0..0.5)", thr, 1);
		gd.addNumericField("Delta (-1..1)", delta, 1);
		gd.addChoice("Mode:", mode_option, mode_option[4]);
		gd.addMessage("NOTE: Incorrect values entered will be replaced by Default values");

		/* Cancel pressed , 0
		   one or more invalid values , 1
		   all correct values 2
		 */

/*		answer =
		if(answer == true)
		{
			return true;
		}
		else
		{
			//answer == 2;
			return false;
		}
*/
	return getUserParams(gd);

	}

	private boolean getUserParams(GenericDialog gd)
	{
		answer = false;
		flag = false;
		gd.showDialog();

		// The user presses the Cancel button
		if (gd.wasCanceled())
		{
			return false;
		}

//-------------------------------------------
		cfsize = (int) gd.getNextNumber();
		if (gd.invalidNumber())
		{
			flag = true;
			IJ.error("Sigma is invalid.");
		}
		w = (float) gd.getNextNumber();
		if (gd.invalidNumber())
		{
			flag = true;
			IJ.error("Width is invalid.");
		}
		thr   = (float) gd.getNextNumber();
		if (gd.invalidNumber())
		{
			flag = true;
			IJ.error("Threshold is invalid.");
		}
		delta = (float)gd.getNextNumber();
		if (gd.invalidNumber())
		{
			flag = true;
			IJ.error("Delta is invalid.");
		}
		mode  =  gd.getNextChoiceIndex();
		if (gd.invalidNumber())
		{
			flag = true;
			IJ.error("Mode is invalid.");
		}

		if (flag == true)
		{
			answer = getUserParams(gd);
		}
		else
		{
			answer = true;
		}
		return answer;
	}// end of method getUserParams

//--------------------------------------------------------------------------------------------------

		/* This method is used to process the image. Since the
               SUPPORTS_STACKS flag was set, it is called for each slice in
               a stack. Image/J will lock the image before calling
               this method and unlock it when the filter is finished.

		   Abstract Superclass 'ImageProcessor' represents the type of
		   image the current image is and provides utility methods for it
		 */

public void run(ImageProcessor ip)
{

	if(GUI() == false)
	{
		return;
	}
	else
	{

		// 'csize' is the height of the image
		int csize = ip.getHeight();

		// 'rsize' is the width of the image
		int rsize = ip.getWidth();

    		// 'Rectangle' represents the co-ordinates of the smallest
		// enclosing rectangle for the user selected region (ROI)
		Rectangle rect = ip.getRoi();

		// General use variables
		int d0,a0,acr,dow,it;
    		int  i,x,y;
    		double h12, h21, ft, h1h2, h2h1, fmu, dh, dv;
		double r, dt, dmx, dmn;

		// 1D LoG function
   		float logaus[] = new float[(rect.width>rect.height)? rect.width : rect.height];

		// 1D Gaussian function
    		float   gaus[] = new float[(rect.width>rect.height)? rect.width : rect.height];

		// 1 D DoG function (Difference of Gaussian)
		float  dgaus[] = new float[(rect.width>rect.height)? rect.width : rect.height];

		// represents the number of ZCs found
    		long zcn =0;

		// 'ip.getPixels()' returns the image's pixels in an array
		// Since this filter only deals with 8-bit grayscale images, the array
		// type is 'byte'
    		byte pixels[] = (byte[])ip.getPixels();

		// The array 'img_in' stores the pixel values for input image in
		// decimal format, in the range 0 - 255
		int img_in[]  = new int[rect.width*rect.height];

		// The following will replace the user values which are out of an allowable range
    		if (cfsize<0) cfsize=3;
    		if (cfsize>35) cfsize=35;
		if(w<0) w=0;
		// Can add more conditions here on the input parameters to restrict parameters' range


		IJ.write("");
		IJ.write("cfise is= "	+ cfsize);
    		IJ.write("w is= "		+ w);
    		IJ.write("thr is= "		+ thr);
    		IJ.write("delta is= "	+ delta);
    		IJ.write("mode is= "	+ mode );

		// Calculating the Filter's size
		int fsize = (int)(cfsize*w);
		if (fsize%2 == 0)
		{
      		fsize += 1;
		}

		IJ.write("The filter size = "+ fsize);

		// These two are temporary storage arrays for the image during processing
		// They are only required to be as big as the ROI, because only the ROI is being processed
		double dimg[] = new double[rect.height*rect.width];
		double dr[] = new double[rect.height*rect.width];

		i=0;
		// The following loop converts the byte values in the 'pixels' array to int values in the
		// range 0 to 255, and copies them to the array 'img_in'
		// Note that only the pixels in the ROI are copied to the destination array, because
		// they are the only ones that need to be processed
		for(y=rect.y;y<(rect.y+rect.height);y++)
		{
      		for(x=rect.x;x<(rect.x+rect.width);x++)
			{
				img_in[i] = (pixels[(y*rsize)+x]&0xff);
				i++;
      		}
    		}

		IJ.write("The dimensions of smallest enclosing rectangle are: ");
		IJ.write("rect.y= "+rect.y+"; rect.x= "+rect.x);
		IJ.write("rect.width= "+rect.width+"; rect.height= "+rect.height);

		// 'size' is the extended row size (by fsize/2)
		int size = rect.width + fsize -1;

		// The array 'image' is an array extended by (fsize/2) in both dimensions, to hold
		// the processed image (during convolution)
		int image[] = new int[(rect.width+fsize-1)*(rect.height+fsize-1)];
		int extension= (fsize/2);

		// The pixel values from the 'img_in' array are copied to the centre of the extended
		// array 'image'
    		for( i=0; i<rect.height;i++)
		{
      		System.arraycopy(img_in,(i*rect.width),image,( ((i+extension)*(rect.width+fsize-1))+ extension ),rect.width);
		}

		// Initialise variables
		h1h2= h2h1 = h12 =0.0;

		// The following sets the filter into 'logaus[]' and sum(h12+h21)
		for(i=1; i<( (fsize+1) /2);i++)
		{
			w = (float)cfsize/(float)2.0/(float)1.414;
      		IJ.write("w="+w);
      		ft = i/w;
      		IJ.write("ft="+ft);
			gaus[i] = (float)Math.exp(-ft*ft/2);
			IJ.write("gaus[i]="+gaus[i]);
			h1h2 += 2.0 *(gaus[i]);
			logaus[i] =(float)(1-ft*ft)*(float)Math.exp(-ft*ft/2);
			IJ.write("logaus[i]="+logaus[i]);
			h2h1 += 2.0*(logaus[i]);
			dgaus[i] =(float)ft*(float)Math.exp(-ft*ft/2);
			IJ.write("dgaus[i]="+dgaus[i]);
    		}
		// 'gaus[0]' and 'logaus[0]' = 1

		fmu = (h2h1 + 1)* (h1h2+1);
		IJ.write("sum(h12)+sum(h21) = "+ fmu);

		// Initialise Zero crossing search array to zeros and double the size to avoid circulation
		int prel[] = new int[rect.width+1];

		// Calculate max and min in order to get ZC level
		dmx = -99999.9;
		dmn =  99999.9;

		int limit = ((rect.width+fsize-1)*(rect.height+fsize-1));

		// The convolution of the filter and image starts here
		// 'do' traverses the hieght of the image and 'a0' traverses the width of the image
    		for(d0=0;d0<rect.height;d0++)
		{
      		for(a0=0;a0<rect.width;a0++)
			{
				// 'acr' and 'dow' are the X and Y co-ordinates of the image pixels in the
				// extended array
				acr = a0 + fsize/2;
				dow = d0 + fsize/2;

				// Initial 1D Threshold
				dh = dv = 0.0;
				h1h2 = h2h1 = 0.0;

				// Log Filtering
				for (int j=1; j<(fsize+1)/2; j++)
				{
					int a0d0, a0d1, a1d0, a1d1;
					h12=h21=0.0;

					for(i=1;i<(fsize+1)/2;i++)
					{
						a0d0 = acr-i+((dow-j)*size);
						a0d1 = acr-i+((dow+j)*size);
						a1d0 = acr+i+((dow-j)*size);
						a1d1 = acr+i+((dow+j)*size);
						//IJ.write("a0d0="+a0d0+" a0d1="+a0d1+" a1d0="+a1d0+" a1d1="+a1d1);
						h12 += logaus[i]*(image[a0d0] + image[a0d1]+
									image[a1d0] + image[a1d1]);
						h21 += gaus[i]*  (image[a0d0] + image[a0d1] +
									image[a1d0] + image[a1d1]);
          				}
					a0d0 = acr-j+dow*size;
					a0d1 = acr+(dow-j)*size;
					a1d0 = acr+j+dow*size;
					a1d1 = acr+(dow+j)*size;

					h1h2 += gaus[j] * (h12+ image[a0d0]+image[a0d1]+
								image[a1d0]+image[a1d1]);
					h2h1 += logaus[j]*(h21+ image[a0d0]+ image[a0d1] +
								image[a1d0] + image[a1d1] );

					// 1D Derivative
					if(thr != 0.0)
					{
						dh += dgaus[j] * ( image[a1d0] - image[a0d0] );
						dv += dgaus[j] * ( image[a1d1] - image[a0d1] );
					}
				}
				// logaus[0], gaus[0]=1 and max and min for scaling
				dt = dimg[d0*rect.width+a0] = h1h2 + h2h1 + (2*image[dow*size+acr]) ;
				if (dt > dmx) dmx = dt;
				if (dt < dmn) dmn = dt;

				// store the 1 D value for thresholding
				if( thr!= 0.0)
				{
					dr[(d0*rect.width)+a0] = Math.abs(dh) + Math.abs(dv);
				}
			}
		}
		IJ.write("filtering max = "+dmx+" min = "+dmn);

		// Set dmx to level range and dmn to central level
		// Set dmx to half bandwidth
		dmx = (dmx-dmn) / 2;
		// Set dmn to middle level
		dmn += dmx;

		int row=0, column=0;

		// Detecting the zero-crossings starts here
		for(d0=0;d0<rect.height;d0++)
		{
			for(a0=0;a0<rect.width;a0++)
			{
				int id = (d0*rect.width) +a0;
				int index = rsize*(rect.y+d0) + (a0+rect.x);
				// k is for the display
				int k = 15;
				// 'it' is the final intensity of the pixel after processing
        			it = (int)(dt = (dimg[id] - (dmn-delta*dmx))*255 / (dmx*(1+Math.abs(delta))));
                                //IJ.write("Value of it=" + it);
                                //IJ.write("Value of dt=" + dt);
                                //IJ.write("Value of dmx=" + dmx);
                                //IJ.write("Value of dmn=" + dmn);
                                switch(mode){
					case 0:
						// Filtering results appear in the intensity range 0 - 255
						pixels[index] = (byte)((dt-dmn+dmx)/dmx*127);
						break;
					case 1:
						// Absolute value of filtering
						pixels[index] = (byte)Math.abs(it);
						break;
					case 2:
						// Results as values -1, 0 or 1
						pixels[index] = (byte)( ((dt!=0)?((dt>0) ? 1: -1) : 0) * 192);
						break;
					case 3:
						// ZCs overlaid on original image
					default:
						// ZCs only
						r = dr[id];
						it = ( (dt!=0) ? ((dt>0) ? 1: -1) : 0);
						if( it==0 && r>=thr)
						{
							k = 255;
     		         				zcn++;
						}
						else
						{
							if( (it*prel[a0]<0 || it*prel[a0+1]<0) && r>=thr)
							{
								k = 255;
								zcn++;
							}
						}
						prel[a0+1] = it;
						if(k==255 || mode!=3)
						pixels[index] = (byte)k;
						break;
				}//end of switch statement
			}
		}
		IJ.write("The number of zcn is = "+zcn);

	}//end of loop GUI
} // End of 'run()' method

	/* The 'setup' function is called when the filter is loaded. 'arg' which may be blank,
	   is the argument specified for this plug-in
 	   in "ij.properties". 'imp' is the currently active image.
 	   This method returns a flag word that specifies the
 	   filters capabilities.
	*/

	public int setup(String arg, ImagePlus imp)
	{
		if (arg.equals("about"))
		{
      		showAbout();
      		return DONE;  /* If 'arg' was "about", dont have to do anything else now*/
      	}

		/* 'DOES_8G' means the filter handles 8-bit grayscale images only */
		/* 'DOES_STACKS' means the 'run()' method will be called for all the slices in a stack */
		/* 'SUPPORTS_MASKING' means the filter wants Image/J, for non-rectangular
		    ROIs, to restore that part of the image that's inside the boun
		    rectangle but outside of the ROI.
		*/
                return DOES_8G+SUPPORTS_MASKING+DOES_STACKS;
                    
  	}

	/* This method displays a dialog box with a text message on it */
	void showAbout() {

		// 'showMessage' brings up a dialog box, with the Title and Content as specified below
		IJ.showMessage(" About LoG_Filter","This plugin is designed to filter images\n"+
                   " using LoG Filtering and extract zero crossings.\n" +
			 " Written by Lokesh Taxali,\n" +
                   " based on algorithm originally by Dr. Jesse Jin\n" );
	}// End of method 'showAbout()'

} // End of class LoG_Filter

