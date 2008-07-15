import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;
import java.text.NumberFormat;

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
This plugin takes the Red and Green channel values from an RGB image or stack of images, determines 
whether or not there is colocalization for a given pixel, and stores the result in the Blue channel 
of the image or slice.  This method of storage for the colocalization data allows for the easy 
appreciation of the presence of colocalized pixels in a sample by superimposing the colocalization 
data without corrupting or modifying the original data.  To alter the sensitivity of the selection 
algorithm, the user can specify the minimum ratio for the pixels in question, as well as the threshold 
for the red and green channels.  Alternatively, the user can choose to have the threshold values determined 
automatically (for each slice if a stack) via the getAutoThreshold() method in the ImageProcessor Class.  
The colocalization data can be expressed as the average, max, or min of the corresponding red and green 
pixels, or as a saturated pixel (255).  The colocalized data can also be displayed in a separate window 
as an 8bit or RGB image.  If any data was present in the Blue channel of the original image, it will 
be replaced by the colocalization data.

					Christopher Philip Mauer 
					cpmauer@northwestern.edu
*/


public class RG2B_Colocalization implements PlugIn{ 
    private ImagePlus imp, imp2, imp3;
    private ImageProcessor ip;
    private ByteProcessor bp;
    private ColorProcessor cp,cp2,cp3;
    private ImageStack stack,stack2,stack3;
    private byte rpix[],gpix[],bpix[];
    private double ratio = 0.0;
    private double redthresh = 0;
    private double greenthresh = 0;
    private boolean atebit = true;
    private boolean auto = true;
    private int choice = 0;       
    private int display = 2;       
    private NumberFormat nf = NumberFormat.getInstance();
    private double prognum;
    private String progstr;
    private String otitle;
    private String titlestring;
    private ImageProcessor rip,gip;
    
    public void run(String arg){
	nf.setMaximumFractionDigits(2);
	nf.setMinimumFractionDigits(2);
	String errormessage;
	String choices[] = {"the average of the red and green","the max of the red and green",
			    "the min of the red and green","a saturated pixel (255)"};
	String colocdisplay[] = {"Do not display",
				 "an 8bit image","as an RGB image"};
	GenericDialog d = new GenericDialog("RG2B Colocalization", IJ.getInstance());
	d.addCheckbox("Use auto thresholding", auto);
	d.addMessage("or Manually Specify the following:");
	d.addNumericField("Minimum ratio between channels (0.0 to 1.0):", ratio, 2);
	d.addNumericField("Red channel lower threshold (0 to 255):", redthresh, 0);
	d.addNumericField("Green channel lower threshold (0 to 255):", greenthresh, 0);
	d.addMessage("Specify the output format:");
	d.addChoice("Set the colocalized pixels to:",choices,choices[1]);
	d.addChoice("Display the colocalization data separately as:",colocdisplay,colocdisplay[2]);
	d.showDialog();
	if(d.wasCanceled())return;
	auto = d.getNextBoolean();
	ratio = d.getNextNumber();
	if(auto)ratio=0;
	if(d.invalidNumber()) {
	    errormessage = new String("Invalid value for ratio.\nAutomatic values will be calculated.");
	    ratio = 0;
	    IJ.showMessage("Error", errormessage);
	}
	redthresh = d.getNextNumber();	
	if(d.invalidNumber()) {
	    errormessage = new String("Invalid value for red channel threshold.\nAutomatic values will be calculated.");
	    auto=true;
	    ratio=0;
	    IJ.showMessage("Error", errormessage);
	}
	greenthresh = d.getNextNumber();	
	if(d.invalidNumber()) {
	    errormessage = new String("Invalid value for green channel threshold.\nAutomatic values will be calculated.");
	    auto=true;
	    ratio=0;
	    IJ.showMessage("Error", errormessage);
	}

	choice = d.getNextChoiceIndex();
	display = d.getNextChoiceIndex();
	if(ratio<0.0||ratio>1.0){
	    ratio = 0;
	    errormessage = new String("Invalid value for ratio.\nAutomatic values will be calculated.");
	    IJ.showMessage(errormessage);
	}
	if(redthresh>255||redthresh<0){
	    auto=true;
	    errormessage = new String("Invalid value for red channel threshold.\nAutomatic values will be calculated.");
	    IJ.showMessage(errormessage);
	}
	if(greenthresh>255||greenthresh<0){
	    auto=true;
	    errormessage = new String("Invalid value for green channel threshold.\nAutomatic values will be calculated.");
	    IJ.showMessage(errormessage);
	}
	imp = IJ.getImage();
	otitle = imp.getTitle();
	if (imp.getBitDepth()!=24){
	    IJ.showMessage("RGB Image required");
	    return;
	}
	imp2 = new ImagePlus("RG2B Colocalization", imp.getStack());
	stack = imp.getStack();
	int m = imp.getWidth();
	int n = imp.getHeight();	
	stack2 = imp2.createEmptyStack();
	if(display==1||display==2){
	    imp3 = new ImagePlus("Colocalization Data", imp.getStack());
	    stack3 = new ImageStack(m,n);	
	}
	int dimension = m*n;
	int stacksize = stack.getSize();

	cp = (ColorProcessor)imp.getProcessor();
	rpix = new byte[dimension];
	gpix = new byte[dimension];
	bpix = new byte[dimension];

	for(int i=1;i<=stacksize;++i){
	    cp = (ColorProcessor)stack.getProcessor(i);
	    cp.getRGB(rpix,gpix,bpix);
	    if(auto){
		rip = new ByteProcessor(m,n);
		rip.setPixels(rpix);
		redthresh = rip.getAutoThreshold();
		gip = new ByteProcessor(m,n);
		gip.setPixels(gpix);
		greenthresh = gip.getAutoThreshold();
	    }
	    detectRGcolocalization(rpix,gpix,bpix);
	    cp2 = new ColorProcessor(m,n);
	    cp2.setRGB(rpix,gpix,bpix);
	    stack2.addSlice(String.valueOf(i), cp2);
	    if(display==1||display==2){
		cp3 = new ColorProcessor(m,n);
		cp3.setRGB(bpix,bpix,bpix);
		if(display==1){
		    ip = (new TypeConverter(cp3,true)).convertToByte();
		    stack3.addSlice(String.valueOf(i), ip);
		}
		else stack3.addSlice(String.valueOf(i), cp3);
	    }
	    prognum = (double)i/stacksize;
	    progstr = nf.format(100*prognum); 
	    IJ.showStatus("RG2B Colocalization "+progstr+"% Done");
	}
	imp2.setStack("RG2B Colocalization - "+otitle, stack2);
	imp2.show();
	if(display==1||display==2){
	    imp3.setStack("Colocalization Data - "+otitle, stack3);
	    imp3.show();
	}
    }
    private void detectRGcolocalization(byte redarray[], byte greenarray[], byte bluearray[]){
	int red, green;
	double ratio1, ratio2;
	for(int i=0;i<redarray.length;++i){
	    red = 0xff & redarray[i];
	    green = 0xff & greenarray[i];
	    ratio1 = (double)green/(double)red;
	    ratio2 = 1.0/ratio1;
	    if(red>redthresh&&green>greenthresh&&ratio1>ratio&&ratio2>ratio){
		switch(choice){
		case 0: bluearray[i]=(byte)((red+green)/2); break;
		case 1: if(redarray[i]>greenarray[i])bluearray[i]=redarray[i];else bluearray[i]=greenarray[i];break;
		case 2: if(redarray[i]<greenarray[i])bluearray[i]=redarray[i];else bluearray[i]=greenarray[i];break;
		case 3: bluearray[i]=(byte)255;
		}
	    }
	    else bluearray[i]=0;
	}
    }
}

