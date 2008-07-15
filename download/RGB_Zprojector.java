import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;
import java.util.*;
import java.lang.Math.*;
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
/*This plugin allows the user to perform a zprojection on an RGB image, specifying
which algorithm (avg, power mean, max, min, sum, median, stdev) should be used for each channel 
seperately, by splitting the image into its component channels, performing the projections,
and merging to create the resulting RGB image.  The values for the median and stdev are
rounded during conversion back to 8bit.  The values for the sum method are rescaled down
as a factor of the max value of each channel to fit in a byte.  All methods can be followed
by autoscaling to improve display.  Power mean degrees can be specified as well.
 */
public class RGB_Zprojector implements PlugIn{
    private ImagePlus imp, imp2, imp3;
    private ImageProcessor ip;
    private ByteProcessor bp;
    private ColorProcessor cp,cp2;
    private ImageStack stack;
    private ImageStack rstack,gstack,bstack;
    private byte rpix[],gpix[],bpix[];
    private byte rpixzpro[],gpixzpro[],bpixzpro[];
    private int redz=0,greenz=0,bluez=0;     
    private String zstring;
    private String titlestring;
    private String otitle;
    private double prognum;
    private String progstr;
    private double power = 2;
    private boolean auto = false;
    
 
    public void run(String arg){
	imp = IJ.getImage();
	if(imp.getStack().getSize()==1){
	    IJ.showMessage("Stack required");
	    return;
	}
	if (imp.getBitDepth()!=24){
	    IJ.showMessage("RGB Image required");
	    return;
	}
	otitle = imp.getTitle();
	String choices[] = {"Average Intensity","Power Mean (Specify order of p below)","Maximum Intensity",
			    "Minimum Intensity","Sum of Slices","Standard Deviation","Median"};
	GenericDialog d = new GenericDialog("RGB Zprojection", IJ.getInstance());
	d.addChoice("Red channel projection:",choices,choices[1]);
	d.addChoice("Green channel projection:",choices,choices[1]);
	d.addChoice("Blue channel projection:",choices,choices[1]);
	d.addCheckbox("Autoscale the data?",auto);
	d.addNumericField("Power Mean order (p):", power, 0);
	d.showDialog();
	if(d.wasCanceled())return;
	redz = d.getNextChoiceIndex();
	greenz = d.getNextChoiceIndex();
	bluez = d.getNextChoiceIndex();
	auto = d.getNextBoolean();
	power = d.getNextNumber();
	String ErrorMessage = 
	    new String("Invalid p value.\np must be greater than or equal to zero.\nThe second power mean will be used.");
	if(d.invalidNumber()) {
	    IJ.showMessage("Error", ErrorMessage);
	    power=2;
	}
	stack = imp.getStack();
	int m = imp.getWidth();
	int n = imp.getHeight();	
	rstack = imp.createEmptyStack();
	bstack = imp.createEmptyStack();
	gstack = imp.createEmptyStack();
	int dimension = m*n;
	cp = (ColorProcessor)imp.getProcessor();
	rpix = new byte[dimension];
	gpix = new byte[dimension];
	bpix = new byte[dimension];
	get8bStacks(stack);
	IJ.showStatus("RGB Zprojector is working..");
	rpixzpro = doZpro(rstack,redz);
	titlestring = "R-"+zstring+" ";
	gpixzpro = doZpro(gstack,greenz);
	titlestring += "G-"+zstring+" ";
	bpixzpro = doZpro(bstack,bluez);
	titlestring += "B-"+zstring+" "+otitle;
	IJ.showStatus("RGB Zprojector is done!");
	cp2 = new ColorProcessor(m,n);
	cp2.setRGB(rpixzpro,gpixzpro,bpixzpro);
	imp3 = new ImagePlus(titlestring,cp2);
	imp3.getProcessor().resetMinAndMax();
	imp3.show();
    }
    private byte[] doZpro(ImageStack stack, int zpro){
	switch(zpro){
	case 0: zstring = new String("Avg"); return zproAverage(stack);
	case 1: zstring = new String("PM"+String.valueOf((int)power)); return zproPowerMean(stack);
	case 2: zstring = new String("Max"); return zproMax(stack);
	case 3: zstring = new String("Min"); return zproMin(stack);
	case 4: zstring = new String("Sum"); return zproSum(stack);
	case 5: zstring = new String("Sdv"); return zproStdev(stack);
	case 6: zstring = new String("Med"); return zproMedian(stack);
	}
	return new byte[1];
    }
    private void get8bStacks(ImageStack stack){
	ImageProcessor rip,gip,bip;
	byte rpx[],gpx[],bpx[];
	int m = stack.getWidth();
	int n = stack.getHeight();
	rpx = new byte[m*n];
	gpx = new byte[m*n];
	bpx = new byte[m*n];
	for(int i=1;i<=stack.getSize();++i){
	    cp = (ColorProcessor)stack.getProcessor(i);
	    cp.getRGB(rpx,gpx,bpx);
	    rip = new ByteProcessor(m,n);
	    gip = new ByteProcessor(m,n);
	    bip = new ByteProcessor(m,n);
	    rip.setPixels(rpx);
	    rstack.addSlice(String.valueOf(i), rip.duplicate());
	    gip.setPixels(gpx);
	    gstack.addSlice(String.valueOf(i), gip.duplicate());
	    bip.setPixels(bpx);
	    bstack.addSlice(String.valueOf(i), bip.duplicate());
	}
    }
    private byte[] zproAverage(ImageStack stack){
	int m = stack.getWidth();
	int n = stack.getHeight();
	byte array[] = new byte[m*n];
	long avgvals[] = new long[m*n]; 
	for(int i=1;i<=stack.getSize();++i){
	    array = (byte[])stack.getPixels(i);
	    for(int j=0;j<m*n;++j)avgvals[j] += 0xff & array[j];
	}
	for(int j=0;j<m*n;++j)array[j] = (byte)(avgvals[j]/stack.getSize());
	if(auto)array = autoScale(array);
	return array;
    }
    private byte[] zproPowerMean(ImageStack stack){
	int m = stack.getWidth();
	int n = stack.getHeight();
	byte array[] = new byte[m*n];
	long avgvals[] = new long[m*n]; 
	for(int i=1;i<=stack.getSize();++i){
	    array = (byte[])stack.getPixels(i);
	    for(int j=0;j<m*n;++j)avgvals[j] += Math.pow((0xff & array[j]),power);
	}
	for(int j=0;j<m*n;++j)array[j] = (byte)Math.pow((avgvals[j]/stack.getSize()),(1.0/(double)power));
	if(auto)array = autoScale(array);
	return array;
    }
    private byte[] zproMax(ImageStack stack){
	int m = stack.getWidth();
	int n = stack.getHeight();
	byte array[] = new byte[m*n];
	byte maxarray[] = (byte[])stack.getPixels(1);
	int arrayj=0, maxarrayj=0;
	for(int i=2;i<=stack.getSize();++i){
	    array = (byte[])stack.getPixels(i);
	    for(int j=0;j<m*n;++j){
		arrayj = 0xff & array[j];
		maxarrayj = 0xff & maxarray[j];
		if(arrayj>maxarrayj)maxarray[j] = array[j];
	    }
	}
	if(auto)maxarray = autoScale(maxarray);
	return maxarray;
    }
    private byte[] zproMin(ImageStack stack){
	int m = stack.getWidth();
	int n = stack.getHeight();
	byte array[] = new byte[m*n];
	byte minarray[] = (byte[])stack.getPixels(1);
	int arrayj=0, minarrayj=0;
	for(int i=2;i<=stack.getSize();++i){
	    array = (byte[])stack.getPixels(i);
	    for(int j=0;j<m*n;++j){
		arrayj = 0xff & array[j];
		minarrayj = 0xff & minarray[j];
		if(arrayj<minarrayj)minarray[j] = array[j];
	    }
	}
	if(auto)minarray = autoScale(minarray);
	return minarray;
    }
    private byte[] zproSum(ImageStack stack){
	int m = stack.getWidth();
	int n = stack.getHeight();
	double max=0;
	byte array[] = new byte[m*n];
	double sumarray[] = new double[m*n];
	for(int i=1;i<=stack.getSize();++i){
	    array = (byte[])stack.getPixels(i);
	    for(int j=0;j<m*n;++j)sumarray[j] += 0xff & array[j];
	}
	max = getMax(sumarray);
	for(int j=0;j<m*n;++j) array[j] = (byte)(255*(sumarray[j]/max));
	return array;
    }
    private byte[] zproMedian(ImageStack stack){
	int m = stack.getWidth();
	int n = stack.getHeight();
	int stacksize = stack.getSize(); 
	byte array[] = new byte[m*n];
	byte imagearray[][] = new byte[stacksize][m*n];
	double medianarray[] = new double[stacksize];
	for(int i=1;i<=stacksize;++i)imagearray[i-1] = (byte[])stack.getPixels(i);
	for(int i=0;i<imagearray[0].length;++i){
	    for(int j=0;j<imagearray.length;++j)medianarray[j] = 0xff & imagearray[j][i];
	    array[i] = (byte)median(medianarray);
	}
	if(auto)array = autoScale(array);
	return array;    
    }
    private byte[] zproStdev(ImageStack stack){
	int m = stack.getWidth();
	int n = stack.getHeight();
	int stacksize = stack.getSize(); 
	byte array[] = new byte[m*n];
	double max=0;
	byte imagearray[][] = new byte[stacksize][m*n];
	double stdevarray[] = new double[stacksize];
	double stdevimage[] = new double[m*n];
	for(int i=1;i<=stacksize;++i)imagearray[i-1] = (byte[])stack.getPixels(i);
	for(int i=0;i<imagearray[0].length;++i){
	    for(int j=0;j<imagearray.length;++j)stdevarray[j] = 0xff & imagearray[j][i];
	    stdevimage[i] = stdev(stdevarray);
	}
	for(int i=0;i<m*n;++i)array[i] = (byte)stdevimage[i];
	if(auto)array = autoScale(array);
	return array;    
    }
    private static double getMax(double array[]){
	double max = array[0];
	for(int i=0;i<array.length;++i)if(array[i]>max)max=array[i];
	return max;
    }
    private static double getMax(byte array[]){
	double max = 0xff & array[0];
	for(int i=0;i<array.length;++i)if((0xff & array[i])>max)max=0xff & array[i];
	return max;
    }
    private static byte[] autoScale(byte array[]){
	double max = getMax(array);
	for(int j=0;j<array.length;++j) array[j] = (byte)(255*((0xff & array[j])/max));
	return array;
    }
    private static double stdev(double array[]){
	double stdev = 0;
	double mean = 0;
	for(int i=0;i<array.length;++i)mean+=array[i];
	mean = mean/array.length;
	for(int i=0;i<array.length;++i)stdev+=Math.pow((array[i]-mean),2);
	stdev = Math.sqrt(stdev/(array.length-1));
	return stdev;
    }
    private static double median(double array[]){
	Arrays.sort(array);
	int len = array.length;
	if(len%2==0)return((array[(len/2)-1]+array[len/2])/2);
	else return array[((len-1)/2)];
    }

}
