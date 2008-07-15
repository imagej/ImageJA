import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;
import java.text.NumberFormat;

/* The authors of this software are Christopher Philip Mauer and Vytas Bindokas.  Copyright (c) 2004.
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
 This plugin consists of a 3D adaptation of the Hybrid Median Filter intended for XYZ or XYT data,
 which we are calling a 3d Hybrid Median Filter. In this implementation, the median
 is calculated from the medians of 1) the 3x3 2d PLUS kernel, 2) the 3x3 2d X kernel,
 3) the 3x3 3d PLUS kernel, and four 3x3 2d X kernels through the center pixel along
 Z-axis: 4) the vertical X, 5) the horizontal X, 6) the X from the upper right to the
 lower left, and 7) the X from the upper left to the lower right, and optionally 
 8) the pixel in question.  The top and bottom edge pixels are reflected outward, and the 
 side edge pixels are wrapped arpund to complete the edge bound kernels.   
 The pixels of the first and last slice are reflected outward to simulate data for the N-1
 and N+1 slices (where N is the number of images in the stack) in order to return an output
 stack which is the same size as the input stack. The filter may be aborted by hitting the escape key.

					Christopher Philip Mauer  &  Vytas Bindokas
					cpmauer@northwestern.edu     vytas@drugs.bsd.uchicago.edu
*/
public class Hybrid_3D_Median_Filter implements PlugInFilter{
    private ImagePlus imp;
    private ImagePlus imp2;
    private ImageStack stack;
    private ImageStack stack2;
    private ImageProcessor ip;
    private ImageProcessor ip2;
    private boolean atebit = false;
    private boolean include = false;
    private NumberFormat nf = NumberFormat.getInstance();
    private double prognum;
    private String progstr;
    private String otitle;
    private String titlestring;
    private ImageWindow win;
    private boolean quit = false;

    public int setup(String argv, ImagePlus imp){
	try{stack = imp.getStack();}
	catch(Exception e){
	    IJ.showMessage("A stack must be open.");
	    return DONE;
	}
	otitle = imp.getTitle();
	GenericDialog d = new GenericDialog("3d Hybrid Median Filter", IJ.getInstance());
	d.addCheckbox("Include the center pixel",include);
	d.showDialog();
	if(d.wasCanceled())return DONE;
	include = d.getNextBoolean();
	nf.setMaximumFractionDigits(2);
	nf.setMinimumFractionDigits(2);
	this.imp = imp;
	if(imp.getBitDepth()==8)atebit=true;
	win = imp.getWindow();
	if (win!=null) win.running = true;  //wsr
	return DOES_8G+DOES_16+STACK_REQUIRED+NO_UNDO;
    }
    public void run(ImageProcessor ip){
	

	imp2 = Hybrid3dMedianizer(imp);
	if(quit)return;	    
	imp2.getProcessor().resetMinAndMax();
	imp2.show();
    }
    private ImagePlus Hybrid3dMedianizer(ImagePlus imp){
	ImagePlus imp3;
	int m = stack.getWidth();
	int n = stack.getHeight();
	imp2 = new ImagePlus("3d Hybrid Median Filter", imp.getStack());
	stack2 = imp2.createEmptyStack();
	int dimension = m*n;
	int stacksize = stack.getSize();

	short beforeslice[];
	short thisslice[];
	short afterslice[];
	short newslice[];

	byte beforeslice8[];
	byte thisslice8[];
	byte afterslice8[];
	byte newslice8[];

	if(atebit){//8bit scenario
	    beforeslice8 = new byte[dimension];
	    thisslice8 = new byte[dimension];
	    afterslice8 = new byte[dimension];
	    newslice8 = new byte[dimension];
	}
	else{//16bit scenario
	    beforeslice = new short[dimension];
	    thisslice = new short[dimension];
	    afterslice = new short[dimension];
	    newslice = new short[dimension];
	}
	double dubbeforeslice[] = new double[dimension];
	double dubthisslice[] = new double[dimension];
	double dubafterslice[] = new double[dimension];
	double filteredslice[] = new double[dimension];

	double marraythisP[] = new double[5];
	double marraythisX[] = new double[5];
	double marray3P[]= new double[7];
	double marray3Xa[]= new double[5];
	double marray3Xb[]= new double[5];
	double marray3Xc[]= new double[5];
	double marray3Xd[]= new double[5];
	double medianarray[] = new double[7]; 
	if(include)medianarray = new double[8]; 

	for(int i=1;i<=stacksize;++i){
	    if(atebit){
		try{beforeslice8 = (byte[])stack.getPixels(i-1);}catch(Exception e){beforeslice8=(byte[])stack.getPixels(i);}
		thisslice8 = (byte[])stack.getPixels(i);
		try{afterslice8 = (byte[])stack.getPixels(i+1);}catch(Exception e){afterslice8=(byte[])stack.getPixels(i);}
		dubbeforeslice = byte2double(beforeslice8);
		dubthisslice = byte2double(thisslice8);
		dubafterslice = byte2double(afterslice8);
	    }
	    else{
		try{beforeslice = (short[])stack.getPixels(i-1);}catch(Exception e){beforeslice=(short[])stack.getPixels(i);}
		thisslice = (short[])stack.getPixels(i);
		try{afterslice = (short[])stack.getPixels(i+1);}catch(Exception e){afterslice=(short[])stack.getPixels(i);}
		dubbeforeslice = short2double(beforeslice);
		dubthisslice = short2double(thisslice);
		dubafterslice = short2double(afterslice);
	    }
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

		try{marray3P[0] = dubbeforeslice[j];  }catch(Exception e){marray3P[0]=dubthisslice[j];}
		try{marray3P[1] = dubthisslice[(j-m)];}catch(Exception e){marray3P[1]=dubthisslice[j];}
		try{marray3P[2] = dubthisslice[(j-1)];}catch(Exception e){marray3P[2]=dubthisslice[j];}
		try{marray3P[3] = dubthisslice[j];    }catch(Exception e){marray3P[3]=dubthisslice[j];}
		try{marray3P[4] = dubthisslice[(j+1)];}catch(Exception e){marray3P[4]=dubthisslice[j];}
		try{marray3P[5] = dubthisslice[(j+m)];}catch(Exception e){marray3P[5]=dubthisslice[j];}
		try{marray3P[6] = dubafterslice[j];   }catch(Exception e){marray3P[6]=dubthisslice[j];}

		try{marray3Xa[0] = dubbeforeslice[(j-(m+1))];}catch(Exception e){marray3Xa[0]=dubthisslice[j];}
		try{marray3Xa[1] = dubafterslice[(j+(m+1))]; }catch(Exception e){marray3Xa[1]=dubthisslice[j];}
		try{marray3Xa[2] = dubthisslice[j];          }catch(Exception e){marray3Xa[2]=dubthisslice[j];}
		try{marray3Xa[3] = dubbeforeslice[(j+(m-1))];}catch(Exception e){marray3Xa[3]=dubthisslice[j];}
		try{marray3Xa[4] = dubafterslice[(j-(m-1))]; }catch(Exception e){marray3Xa[4]=dubthisslice[j];}

		try{marray3Xb[0] = dubbeforeslice[(j-m)];}catch(Exception e){marray3Xb[0]=dubthisslice[j];}
		try{marray3Xb[1] = dubafterslice[(j+m)]; }catch(Exception e){marray3Xb[1]=dubthisslice[j];}
		try{marray3Xb[2] = dubthisslice[j];      }catch(Exception e){marray3Xb[2]=dubthisslice[j];}
		try{marray3Xb[3] = dubbeforeslice[(j+m)];}catch(Exception e){marray3Xb[3]=dubthisslice[j];}
		try{marray3Xb[4] = dubafterslice[(j-m)]; }catch(Exception e){marray3Xb[4]=dubthisslice[j];}
		
		try{marray3Xc[0] = dubbeforeslice[(j-(m-1))];}catch(Exception e){marray3Xc[0]=dubthisslice[j];}
		try{marray3Xc[1] = dubafterslice[(j+(m-1))]; }catch(Exception e){marray3Xc[1]=dubthisslice[j];}
		try{marray3Xc[2] = dubthisslice[j];          }catch(Exception e){marray3Xc[2]=dubthisslice[j];}
		try{marray3Xc[3] = dubbeforeslice[(j+(m-1))];}catch(Exception e){marray3Xc[3]=dubthisslice[j];}
		try{marray3Xc[4] = dubafterslice[(j-(m-1))]; }catch(Exception e){marray3Xc[4]=dubthisslice[j];}
		
		try{marray3Xd[0] = dubbeforeslice[(j-1)];}catch(Exception e){marray3Xd[0]=dubthisslice[j];}
		try{marray3Xd[1] = dubafterslice[(j+1)]; }catch(Exception e){marray3Xd[1]=dubthisslice[j];}
		try{marray3Xd[2] = dubthisslice[j];      }catch(Exception e){marray3Xd[2]=dubthisslice[j];}
		try{marray3Xd[3] = dubbeforeslice[(j+1)];}catch(Exception e){marray3Xd[3]=dubthisslice[j];}
		try{marray3Xd[4] = dubafterslice[(j-1)]; }catch(Exception e){marray3Xd[4]=dubthisslice[j];}
				
		medianarray[0] = median(marraythisX);
		medianarray[1] = median(marraythisP);
		medianarray[2] = median(marray3P);
		medianarray[3] = median(marray3Xa);
		medianarray[4] = median(marray3Xb);
		medianarray[5] = median(marray3Xc);
		medianarray[6] = median(marray3Xd);
	
		if(include)medianarray[7]= dubthisslice[j]; 
		filteredslice[j] = median(medianarray);
	
	    }
	    prognum = (double)(i-1)/stacksize;
	    progstr = nf.format(100*prognum); 
	    IJ.showStatus("3d Hybrid Median Filter "+progstr+"% Done");
	    
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

	    if(win!=null && !win.running){ //wsr
		IJ.beep();
		quit=true; 
		IJ.showMessage(new String("3d Hybrid Median Filter aborted.\nNo output will be generated.")); 
		return imp2;
	    }
	}
	titlestring = "3d Hybrid Median Filter - "+otitle;
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
	for(int j=0;j<array.length;++j){
	    shortarray[j] = (short)array[j];
	}
	return shortarray;
    }
    private double[] short2double(short array[]){
	double doublearray[] = new double[array.length];
	for(int j=0;j<array.length;++j){
	    doublearray[j] = (double)(0xffff & array[j]);
	}
	return doublearray;
    }
    private byte[] double2byte(double array[]){
	byte bytearray[] = new byte[array.length];
	for(int j=0;j<array.length;++j){
	    bytearray[j] = (byte)array[j];
	}
	return bytearray;
    }
    private double[] byte2double(byte array[]){
	double doublearray[] = new double[array.length];
	for(int j=0;j<array.length;++j){
	    doublearray[j] = (double)(0xff & array[j]);
	}
	return doublearray;
    }
}
