import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;
import java.text.NumberFormat;
import java.lang.Math.*;

/* NOTE:
   Bug in 7x7 filter fixed by Morten Larsen <ml@life.ku.dk>, May 2007. */

/* The author of this software is Christopher Philip Mauer.  Copyright (c) 2004.
   Permission to use, copy, modify, and distribute this software for any purpose 
   without fee is hereby granted, provided that this entire notice is included in 
   all copies of any software which is or includes a copy or modification of this 
   software and in all copies of the supporting documentation for such software.
   Any for profit use of this software is expressly forbidden without first
   obtaining the explicit consent of the author. 
   THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED WARRANTY. 
   IN PARTICULAR, THE AUTHOR DOES NOT MAKE ANY REPRESENTATION OR WARRANTY 
   OF ANY KIND CONCERNING THE MERCHANTABILITY OF THIS SOFTWARE OR ITS FITNESS FOR ANY 
   PARTICULAR PURPOSE. 
*/
/* 
   This plugin consists of 3 versions of the standard hybrid median filter: a 3x3, 
   5x5, and 7x7 kernel. In these implementations, the median of 1) the median of the NxN PLUS
   kernel, 2) the median of the NxN X kernel, and 3) the pixel in question replaces
   the original pixel value.  Multiple repetitions can automatically be run by specifying a repeat 
   value greater than 1.  The top and bottom edge pixels are reflected outward, and the side
   edge pixels are wrapped around to complete the edge bound kernels.  This plugin works both 
   with stacks and individual slices.

   Christopher Philip Mauer 
   cpmauer@northwestern.edu
*/
public class Hybrid_2D_Median_Filter implements PlugInFilter{
    private ImagePlus imp;
    private ImagePlus imp2;
    private ImagePlus impcopy;
    private ImageStack stack;
    private ImageStack stack2;
    private ImageStack tempstack;
    private ImageProcessor ip;
    private ImageProcessor ip2;
    private boolean atebit = false;
    private boolean astack = true;
    private NumberFormat nf = NumberFormat.getInstance();
    private double prognum;
    private String progstr;
    private double nsize = 1;
    private double times = 1;
    private String otitle;
    private String titlestring;
    private String sizes[] = {"3x3","5x5","7x7"}; 

    public int setup(String argv, ImagePlus imp){
	try{stack = imp.getStack();}
	catch(Exception e){
	    IJ.showMessage("An image or stack must be open.");
	    return DONE;
	}
	GenericDialog d = new GenericDialog("2D Hybrid Median Filter", IJ.getInstance());
	d.addChoice("Hybrid Median Kernel Size:",sizes,sizes[0]);
	d.addNumericField("Number of Repetitions:", times, 0);
	d.showDialog();
	if(d.wasCanceled())return DONE;
	nsize = Math.floor(d.getNextChoiceIndex());
	times = d.getNextNumber();
	String ErrorMessage = 
	    new String("Invalid repetition value.\nThe value must be greater than or equal to one.\nOne repetition will be performed.");
	if(d.invalidNumber()||times<1) {
	    IJ.showMessage("Error", ErrorMessage);
	    times=1;
	}
	nf.setMaximumFractionDigits(2);
	nf.setMinimumFractionDigits(2);
	this.imp = imp;
	if(imp.getBitDepth()==8)atebit=true;
	if(imp.getStack().getSize()==1)astack=false;
	otitle = imp.getTitle();
	return DOES_8G+DOES_16+NO_UNDO;
    }
    public void run(ImageProcessor ip){
	imp2 = Hybrid2dMedianizer(imp, nsize);
	imp2.getProcessor().resetMinAndMax();
	imp2.show();
    }
    private ImagePlus Hybrid2dMedianizer(ImagePlus imp, double size){
	int m = stack.getWidth();
	int n = stack.getHeight();
	imp2 = new ImagePlus("2d Hybrid Median Filter", imp.getStack());
	stack2 = imp2.createEmptyStack();
	int dimension = m*n;
	int stacksize = stack.getSize();
	short thisslice[];
	short newslice[];

	byte thisslice8[];
	byte newslice8[];

	if(atebit){//8bit scenario
	    thisslice8 = new byte[dimension];
	    newslice8 = new byte[dimension];
	}
	else{//16bit scenario
	    thisslice = new short[dimension];
	    newslice = new short[dimension];
	}
	double dubthisslice[] = new double[dimension];
	double filteredslice[] = new double[dimension];

	double marraythisP[] = new double[5];
	double marraythisX[] = new double[5];
	titlestring = "2d "+sizes[(int)nsize]+" Hybrid Median Filter x"+String.valueOf((int)times)+" - "+otitle;
	if(nsize==1){
	    marraythisP = new double[9];
	    marraythisX = new double[9];
	}
	if(nsize==2){
	    marraythisP = new double[13];
	    marraythisX = new double[13];
	}
	double medianarray[] = new double[3]; 
	
	for(int i=1;i<=stacksize;++i){
	    
	    if(atebit){
		thisslice8 = (byte[])stack.getPixels(i);
		dubthisslice = byte2double(thisslice8);
	    }
	    else{
		thisslice = (short[])stack.getPixels(i);
		dubthisslice = short2double(thisslice);
	    }
	    
	    if(nsize==0){
		for(int k=0;k<times;++k){
		    for(int j=0;j<dimension;++j){
			try{marraythisP[0] = dubthisslice[(j-m)];}catch(Exception e){marraythisP[0]=dubthisslice[j];}
			try{marraythisP[1] = dubthisslice[(j-1)];}catch(Exception e){marraythisP[1]=dubthisslice[j];}
			try{marraythisP[2] = dubthisslice[j];    }catch(Exception e){marraythisP[2]=dubthisslice[j];}
			try{marraythisP[3] = dubthisslice[(j+1)];}catch(Exception e){marraythisP[3]=dubthisslice[j];}
			try{marraythisP[4] = dubthisslice[(j+m)];}catch(Exception e){marraythisP[4]=dubthisslice[j];}
		
			try{marraythisX[0] = dubthisslice[(j-(m+1))];}catch(Exception e){marraythisX[0]=dubthisslice[j];}
			try{marraythisX[1] = dubthisslice[(j-(m-1))];}catch(Exception e){marraythisX[1]=dubthisslice[j];}
			try{marraythisX[2] = dubthisslice[j];        }catch(Exception e){marraythisX[2]=dubthisslice[j];}
			try{marraythisX[3] = dubthisslice[(j+(m-1))];}catch(Exception e){marraythisX[3]=dubthisslice[j];}
			try{marraythisX[4] = dubthisslice[(j+(m+1))];}catch(Exception e){marraythisX[4]=dubthisslice[j];}
		
			medianarray[0] = median(marraythisX);
			medianarray[1] = median(marraythisP);
			medianarray[2] = dubthisslice[j];
			filteredslice[j] = median(medianarray);
		    }
		    for(int h=0;h<dimension;++h)dubthisslice[h]=filteredslice[h];
		}
	    }
	    if(nsize==1){
		for(int k=0;k<times;++k){
		    for(int j=0;j<dimension;++j){
		
			try{marraythisP[0] = dubthisslice[(j-m)];}catch(Exception e){marraythisP[0]=dubthisslice[j];}
			try{marraythisP[1] = dubthisslice[(j-1)];}catch(Exception e){marraythisP[1]=dubthisslice[j];}
			try{marraythisP[2] = dubthisslice[j];    }catch(Exception e){marraythisP[2]=dubthisslice[j];}
			try{marraythisP[3] = dubthisslice[(j+1)];}catch(Exception e){marraythisP[3]=dubthisslice[j];}
			try{marraythisP[4] = dubthisslice[(j+m)];}catch(Exception e){marraythisP[4]=dubthisslice[j];}
		
			try{marraythisP[5] = dubthisslice[(j-2*m)];}catch(Exception e){
			    try{marraythisP[5]=dubthisslice[j-m];}catch(Exception ee){marraythisP[5]=dubthisslice[j];}}

			try{marraythisP[6] = dubthisslice[(j-2)];}catch(Exception e){
			    try{marraythisP[6]=dubthisslice[j-1];}catch(Exception ee){marraythisP[6]=dubthisslice[j];}}

			try{marraythisP[7] = dubthisslice[(j+2)];}catch(Exception e){
			    try{marraythisP[7]=dubthisslice[j+1];}catch(Exception ee){marraythisP[7]=dubthisslice[j];}}

			try{marraythisP[8] = dubthisslice[(j+2*m)];}catch(Exception e){
			    try{marraythisP[8]=dubthisslice[j+m];}catch(Exception ee){marraythisP[8]=dubthisslice[j];}}
		
					
			try{marraythisX[0] = dubthisslice[(j-(m+1))];}catch(Exception e){marraythisX[0]=dubthisslice[j];}
			try{marraythisX[1] = dubthisslice[(j-(m-1))];}catch(Exception e){marraythisX[1]=dubthisslice[j];}
			try{marraythisX[2] = dubthisslice[j];        }catch(Exception e){marraythisX[2]=dubthisslice[j];}
			try{marraythisX[3] = dubthisslice[(j+(m-1))];}catch(Exception e){marraythisX[3]=dubthisslice[j];}
			try{marraythisX[4] = dubthisslice[(j+(m+1))];}catch(Exception e){marraythisX[4]=dubthisslice[j];}

			try{marraythisX[5] = dubthisslice[(j-(2*m+2))];}catch(Exception e){
			    try{marraythisX[5]=dubthisslice[j-(m+1)];}catch(Exception ee){marraythisX[5]=dubthisslice[j];}}

			try{marraythisX[6] = dubthisslice[(j-(2*m-2))];}catch(Exception e){
			    try{marraythisX[6]=dubthisslice[j-(m-1)];}catch(Exception ee){marraythisX[6]=dubthisslice[j];}}

			try{marraythisX[7] = dubthisslice[(j+(2*m-2))];}catch(Exception e){
			    try{marraythisX[7]=dubthisslice[j+(m-1)];}catch(Exception ee){marraythisX[7]=dubthisslice[j];}}

			try{marraythisX[8] = dubthisslice[(j+(2*m+2))];}catch(Exception e){
			    try{marraythisX[8]=dubthisslice[j+(m+1)];}catch(Exception ee){marraythisX[8]=dubthisslice[j];}}

			medianarray[0] = median(marraythisX);
			medianarray[1] = median(marraythisP);
			medianarray[2] = dubthisslice[j];
			filteredslice[j] = median(medianarray);
		    }
		    for(int h=0;h<dimension;++h)dubthisslice[h]=filteredslice[h];
		}
	    
	    }
	    if(nsize==2){
		for(int k=0;k<times;++k){
		    for(int j=0;j<dimension;++j){
		
			try{marraythisP[0] = dubthisslice[(j-m)];}catch(Exception e){marraythisP[0]=dubthisslice[j];}
			try{marraythisP[1] = dubthisslice[(j-1)];}catch(Exception e){marraythisP[1]=dubthisslice[j];}
			try{marraythisP[2] = dubthisslice[j];    }catch(Exception e){marraythisP[2]=dubthisslice[j];}
			try{marraythisP[3] = dubthisslice[(j+1)];}catch(Exception e){marraythisP[3]=dubthisslice[j];}
			try{marraythisP[4] = dubthisslice[(j+m)];}catch(Exception e){marraythisP[4]=dubthisslice[j];}
		
			try{marraythisP[5] = dubthisslice[(j-2*m)];}catch(Exception e){
			    try{marraythisP[5]=dubthisslice[j-m];}catch(Exception ee){marraythisP[5]=dubthisslice[j];}}

			try{marraythisP[6] = dubthisslice[(j-2)];}catch(Exception e){
			    try{marraythisP[6]=dubthisslice[j-1];}catch(Exception ee){marraythisP[6]=dubthisslice[j];}}

			try{marraythisP[7] = dubthisslice[(j+2)];}catch(Exception e){
			    try{marraythisP[7]=dubthisslice[j+1];}catch(Exception ee){marraythisP[7]=dubthisslice[j];}}

			try{marraythisP[8] = dubthisslice[(j+2*m)];}catch(Exception e){
			    try{marraythisP[8]=dubthisslice[j+m];}catch(Exception ee){marraythisP[8]=dubthisslice[j];}}

			try{marraythisP[9] = dubthisslice[(j-3*m)];}catch(Exception e){
			    try{marraythisP[9] = dubthisslice[(j-2*m)];}catch(Exception ee){
				try{marraythisP[9]=dubthisslice[j-m];}catch(Exception eee){marraythisP[9]=dubthisslice[j];}}}

			try{marraythisP[10] = dubthisslice[(j-3)];}catch(Exception e){
			    try{marraythisP[10] = dubthisslice[(j-2)];}catch(Exception ee){
				try{marraythisP[10]=dubthisslice[j-1];}catch(Exception eee){marraythisP[10]=dubthisslice[j];}}}

			try{marraythisP[11] = dubthisslice[(j+3)];}catch(Exception e){
			    try{marraythisP[11] = dubthisslice[(j+2)];}catch(Exception ee){
				try{marraythisP[11]=dubthisslice[j+1];}catch(Exception eee){marraythisP[11]=dubthisslice[j];}}}

			try{marraythisP[12] = dubthisslice[(j+3*m)];}catch(Exception e){
			    try{marraythisP[12] = dubthisslice[(j+2*m)];}catch(Exception ee){
				try{marraythisP[12]=dubthisslice[j+m];}catch(Exception eee){marraythisP[12]=dubthisslice[j];}}}
		
					
			try{marraythisX[0] = dubthisslice[(j-(m+1))];}catch(Exception e){marraythisX[0]=dubthisslice[j];}
			try{marraythisX[1] = dubthisslice[(j-(m-1))];}catch(Exception e){marraythisX[1]=dubthisslice[j];}
			try{marraythisX[2] = dubthisslice[j];        }catch(Exception e){marraythisX[2]=dubthisslice[j];}
			try{marraythisX[3] = dubthisslice[(j+(m-1))];}catch(Exception e){marraythisX[3]=dubthisslice[j];}
			try{marraythisX[4] = dubthisslice[(j+(m+1))];}catch(Exception e){marraythisX[4]=dubthisslice[j];}


			try{marraythisX[5] = dubthisslice[(j-(2*m+2))];}catch(Exception e){
			    try{marraythisX[5]=dubthisslice[j-(m+1)];}catch(Exception ee){marraythisX[5]=dubthisslice[j];}}
			try{marraythisX[6] = dubthisslice[(j-(2*m-2))];}catch(Exception e){
			    try{marraythisP[6]=dubthisslice[j-(m-1)];}catch(Exception ee){marraythisX[6]=dubthisslice[j];}}
			try{marraythisX[7] = dubthisslice[(j+(2*m-2))];}catch(Exception e){
			    try{marraythisX[7]=dubthisslice[j+(m-1)];}catch(Exception ee){marraythisX[7]=dubthisslice[j];}}
			try{marraythisX[8] = dubthisslice[(j+(2*m+2))];}catch(Exception e){
			    try{marraythisX[8]=dubthisslice[j+(m+1)];}catch(Exception ee){marraythisX[8]=dubthisslice[j];}}

			try{marraythisX[9] = dubthisslice[(j-(3*m+3))];}catch(Exception e){
			    try{marraythisX[9] = dubthisslice[(j-(2*m+2))];}catch(Exception ee){
				try{marraythisX[9]=dubthisslice[j-(m+1)];}catch(Exception eee){marraythisX[9]=dubthisslice[j];}}}
			try{marraythisX[10] = dubthisslice[(j-(3*m-3))];}catch(Exception e){
			    try{marraythisX[10] = dubthisslice[(j-(2*m-2))];}catch(Exception ee){
				try{marraythisX[10]=dubthisslice[j-(m-1)];}catch(Exception eee){marraythisX[10]=dubthisslice[j];}}}
			try{marraythisX[11] = dubthisslice[(j+(3*m-3))];}catch(Exception e){
			    try{marraythisX[11] = dubthisslice[(j+(3*m-3))];}catch(Exception ee){
				try{marraythisX[11]=dubthisslice[j+(m-1)];}catch(Exception eee){marraythisX[11]=dubthisslice[j];}}}
			try{marraythisX[12] = dubthisslice[(j+(3*m+3))];}catch(Exception e){
			    try{marraythisX[12] = dubthisslice[(j+(2*m+2))];}catch(Exception ee){
				try{marraythisX[12]=dubthisslice[j+(m+1)];}catch(Exception eee){marraythisX[12]=dubthisslice[j];}}}
		
			medianarray[0] = median(marraythisX);
			medianarray[1] = median(marraythisP);
			medianarray[2] = dubthisslice[j];
			filteredslice[j] = median(medianarray);
		    }
		    for(int h=0;h<dimension;++h)dubthisslice[h]=filteredslice[h];
		}
	    }
	    if(atebit){
		newslice8 = double2byte(filteredslice);
		ip2 = imp2.getProcessor();
		ip2.setPixels(newslice8);
	    }	
	    else{ 
		newslice = double2short(filteredslice);
		ip2 = imp2.getProcessor();
		ip2.setPixels(newslice);	
	    }
	    prognum = (double)(i-1)/(stacksize);
	    progstr = nf.format(100*prognum); 
	    IJ.showStatus("2d Hybrid Median Filter "+progstr+"% Done");
	    
	    if(atebit){
		newslice8 = double2byte(filteredslice);
		ip2 = imp2.getProcessor();
		ip2.setPixels(newslice8);
	    }	
	    else{ 
		newslice = double2short(filteredslice);
		ip2 = imp2.getProcessor();
		ip2.setPixels(newslice);	
	    }
	    stack2.addSlice(String.valueOf(i), ip2);
	}
	imp2.setStack(titlestring, stack2);
	return imp2;
    } 
    private static double median(double array[]){
	Arrays.sort(array);
	int len = array.length;
	if(len%2==0)return((array[(len/2)-1]+array[len/2])/2);
	else return array[((len-1)/2)];
    }
    private short[] double2short(double array[]){
	short shortarray[] = new short[array.length];
	for(int j=0;j<array.length;++j)shortarray[j] = (short)array[j];
	return shortarray;
    }
    private double[] short2double(short array[]){
	double doublearray[] = new double[array.length];
	for(int j=0;j<array.length;++j)doublearray[j] = (double)(0xffff & array[j]);
	return doublearray;
    }
    private byte[] double2byte(double array[]){
	byte bytearray[] = new byte[array.length];
	for(int j=0;j<array.length;++j)bytearray[j] = (byte)array[j];
	return bytearray;
    }
    private double[] byte2double(byte array[]){
	double doublearray[] = new double[array.length];
	for(int j=0;j<array.length;++j)doublearray[j] = (double)(0xff & array[j]);
	return doublearray;
    }
}
