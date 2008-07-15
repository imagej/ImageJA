import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.text.*;
import ij.io.*;
import java.awt.*;
import java.util.*;
import java.lang.Math;

/* Implementation of windowed-sinc low-pass filter appeared in Chapter 13 of */
/* "The Scientist and Engineer's Guide to Digital Signal Processing" */
/* by Steven W. Smith, Ph.D. California Technical Publishing */
/* ISBN 0-9660176-3-3 (1997) */
/* http://www.dspguide.com/  */
/* Perform low-pass filtering against image stack regarding time axis */
/* written by k.yoshida at mark imperial.ac.uk */

public class WindowedSinc_Filter implements PlugIn {
    static int winSize=24;
    static double fc=0.5;
    static boolean showFilter;
    int width, height;
    
    public void run(String arg) {
	ImagePlus imp=WindowManager.getCurrentImage();
	if (imp==null || imp.getStackSize()==1 || imp.getBitDepth()!=8) {
		IJ.error("This plugin requires an 8-bit stack");
		return;
	}
	GenericDialog tgd = new GenericDialog("Lowering_SampleRate", IJ.getInstance());
	tgd.addNumericField("Window size:", winSize, 0);
	tgd.addNumericField("Cutoff frequency (0-1.0)x¹:", fc, 3);
	tgd.addCheckbox("Show Filter", showFilter);
	tgd.showDialog();
	if (tgd.wasCanceled()) 
	    return;
	winSize=(int)tgd.getNextNumber();
	if (winSize>=imp.getStackSize()) {
		IJ.error("Window size must be less than the stack size");
		return;
	}
	fc=(double)tgd.getNextNumber();
	showFilter = tgd.getNextBoolean();
	convolveFrames(imp, winSize, 0.5*fc);
    }
    
    void convolveFrames (ImagePlus imp, int winSize, double fc) {
	double PI=3.14159265;
	double[] h=new double[(winSize+1)];
	double sumh=0;
	int i;
	for (i=0;i<=winSize;i++) {
	    if (i==winSize/2) {
		h[i]=2*PI*fc;
	    } else {
		h[i]=Math.sin(2*PI*fc*(i-winSize/2))/(i-winSize/2);
	    }
	    h[i]=h[i]*(0.54-0.46*Math.cos(2*PI*i/winSize));
	    //IJ.log(i+"\t"+h[(int)i]);
	    sumh+=h[i];
	}

	if (showFilter) {
		double[] xValues=new double[winSize+1];
		for (i=0;i<=winSize;i++)
			xValues[i] = i;
		PlotWindow plot = new PlotWindow("Filter Kernel","Time","", xValues, h);
		plot.draw();
	}

	width=imp.getWidth();
	height=imp.getHeight();
	int nSlices = imp.getStackSize();
	ImageStack bis=imp.getStack();

	ByteProcessor bip=(ByteProcessor)bis.getProcessor(1);
	FloatProcessor fip=new FloatProcessor(width, height);
	float[] fpixels=(float[])fip.getPixels();
	ImageStack ois = new ImageStack(width, height);

	for (int j=winSize; j<nSlices; j++) {
	   IJ.showStatus("Processing "+j+"/"+nSlices+"");
	   IJ.showProgress(j, nSlices-1);
	   for (i=0; i<=winSize; i++) {
		bip=(ByteProcessor)bis.getProcessor(j-i+1);
		byte[] bpixels = (byte[]) bip.getPixels();
		for (int x=0;x<width;x++) {
		    for (int y=0;y<height;y++) {
			int k=width*y+x;
			if (i==0)
			    fpixels[k]=0;
		       fpixels[k]+=(float)(bpixels[k]&0xff)*h[i]/sumh;
		    }
		}
	    }
	    ImageProcessor tip=fip.convertToByte(false);
	    ois.addSlice("", tip); 
	    //ois.addSlice("", fip.duplicate()); 
	}
	ImagePlus oimp=new ImagePlus("WS_"+imp.getShortTitle(),ois);
	oimp.show();
	return;
    }
}

