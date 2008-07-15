import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

/** Autocorrelation Filter */
/* (c) Markus Hasselblatt 1999 */
public class Auto_Corr implements PlugInFilter {
	
      ImagePlus imp;
	ImagePlus imp2;
	ImagePlus imp3;
	ImagePlus imp4;
	ImagePlus imp5;
	ImagePlus imp6;
	ImagePlus imp7;
	ImagePlus imp8;
	Window win;
	Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
	Dimension winorg;
	Dimension winatc;
	Dimension winkrn;
	private static int thresh = 0;
	private static int rothresh = 0;
	private static int dummy = 1;
	private static int operator = 9;
	private static String srcTitle = "";
	private static String dstTitle = "";
	private static boolean finished = false;
	private static boolean abort = false;
	private static boolean printedresults = false;
	private static boolean rotated = false;
	private static boolean savedots = false;
	private static boolean coarse = true;
	private static boolean useAveKern = false;
	private static boolean knowRotAveKern = false;
	private static boolean showrotation = true;
	private static int KernelX;
	private static int KernelY;
	private static int KernelWidth;
	private static int KernelHeight;
	private static int ACWidth;
	private static int ACHeight;
	private static int disp = 3;				// Displacement in atcRotWiggle MUST be an odd number!
	

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G+NO_CHANGES+ROI_REQUIRED;
;
	}

	public void run(ImageProcessor ip) {

//
///	Declare loop booleans
//
	finished = false;
	rotated = false;
	abort = false;
//
///	Find out dimensions of data window and region of interest; define respective constants
//
	win = imp.getWindow();
	Dimension winorg = win.getSize();
	win.setLocation(0,screen.height-winorg.height);
	Rectangle r = ip.getRoi();
	int KernelX = (int)(r.x);
	int KernelY = (int)(r.y);
	int KernelWidth = (int)(r.width);
	int KernelHeight = (int)(r.height);
	int ACWidth = ip.getWidth() - KernelWidth;
	int ACHeight = ip.getHeight() - KernelHeight;
	ColorModel cm = LookUpTable.createGrayscaleColorModel(false);
//
///	define image windows resulting from this procedure
//
	ImagePlus imp2 = new ImagePlus("Kernel", ip.resize(KernelWidth, KernelHeight));
	ImagePlus imp3 = new ImagePlus("Autocorrelation", ip.resize(ACWidth, ACHeight));
	ImagePlus imp5 = new ImagePlus("Kernel Average", ip.resize(KernelWidth, KernelHeight));
	ImagePlus imp6 = new ImagePlus("Kernel Rotation Average", ip.resize(KernelWidth, KernelHeight));
	ImagePlus imp7 = new ImagePlus("Random Difference Kernel", ip.resize(KernelWidth, KernelHeight));
	ImagePlus imp8 = new ImagePlus("Random Diff. Rot. Kernel", ip.resize(KernelWidth, KernelHeight));
	ImageProcessor krn = imp2.getProcessor();
	ip.snapshot();
	krn.snapshot();
//
///	define arrays for data-juggling
//
	byte[] maxima = new byte[ACWidth*ACHeight];
	byte[] original = (byte[])ip.getPixels();
	byte[] kernel = (byte[])krn.getPixels();
	byte[] autocorr = new byte[ACWidth*ACHeight];
	byte[] byteaverage = new byte[KernelWidth*KernelHeight];
//
///	show kernel, i.e. the region of interest or sub-bitmap; place on top of image
//
	imp2.setProcessor("Kernel",krn);
	imp2.show();
	win = imp2.getWindow();
	Dimension winkrn = win.getSize();
	win.setLocation(0,screen.height-winorg.height-winkrn.height);
	imp2.show();
	imp2.copyScale(imp);
//
///	calculate initial linear correlation bitmap, or autocorrelation bitmap (ATC-bitmap)
//
	ImageProcessor atc = new ByteProcessor(ACWidth,ACHeight,autocorr, cm);
	imp3.setProcessor("Autocorrelation",atc);
	IJ.write("");
	IJ.showStatus("Calculating autocorrelation...");
	long startTime = System.currentTimeMillis();
	autoCorrCalc(ip,atc);
	long time = System.currentTimeMillis()-startTime;
	double seconds = time/1000.0;
	IJ.write("Took "+IJ.d2s(seconds)+" seconds to calculate autocorrelation.");
	atc.snapshot();
	imp.killRoi();
	imp3.show();
//
///	prepare image to store maxima
//
	ImagePlus imp4 = new ImagePlus("Maxima",atc.resize(ACWidth,ACHeight));
	ImageProcessor tmx = imp4.getProcessor();
	for (int x=0; x < ACWidth; x++) {
		for (int y=0; y < ACHeight; y++) {
			tmx.putPixel(x,y,255);
		}
	}
	imp4.setProcessor("Maxima",tmx);
	tmx.snapshot();
//
/// begin interactive loop for data analysis
//
	while (!finished) {
		ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
//
///		ask for parameters
//
		getThresh();
//
///		select maxima and perhaps recalculate ATC-bitmap using average kernel
//
		if (!rotated) knowRotAveKern = false;	// in case rotated switch is set back!
		if (!abort) {
		if (!useAveKern) {
			//autoCorrCalc(ip,atc);
			atc.snapshot();
			findMaxima(ip,atc,tmx);
			displayWin(imp3,"Autocorrelation",atc,winorg.width,screen.height-winorg.height);
			displayWin(imp5,"Kernel Average",imp5.getProcessor(),winkrn.width,screen.height-winorg.height-winkrn.height);
			tmx.snapshot();
		}
		if (useAveKern) {
			findMaxima(ip,atc,tmx);
			ip.reset();
			ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
			calcAverage(ip,tmx,imp5,imp7,dummy);
			generateAverageKernel(ip,imp5,atc,tmx,dummy);
			atc.snapshot();
			findMaxima(ip,atc,tmx);
			displayWin(imp3,"Autocorrelation",atc,winorg.width,screen.height-winorg.height);
			displayWin(imp5,"Kernel Average",imp5.getProcessor(),winkrn.width,screen.height-winorg.height-winkrn.height);
			tmx.snapshot();
		}
		if (rotated) {
			getRotThresh();
		}
		if (rotated) IJ.write("Rotation Threshold = "+Math.round(rothresh/2.55));
		atc.reset();
		tmx.reset();
		ip.reset();
		findMaxima(ip,atc,tmx);
		imp.updateAndDraw();
		imp3.updateAndDraw();
		ip.reset();
//		atc.reset();
		ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
		if (knowRotAveKern) IJ.write("Now using Rotation Average as Kernel!");
//
///		generate averages depending on selected parameters
//
		if (!savedots) {
			if (!rotated)
				calcAverage(ip,tmx,imp5,imp7,dummy);
			else {
				startTime = System.currentTimeMillis();
				calcAverageRot(ip,krn,tmx,imp3,imp5,imp6,imp7,imp8,dummy);
				time = System.currentTimeMillis()-startTime;
				seconds = time/1000.0;
				IJ.write("Took "+IJ.d2s(seconds)+" seconds to calculate rotation.");
			}
		}
		else {
			if (!rotated){
				calcAverage(ip,tmx,imp5,imp7,dummy);
				ip.reset();
				atc.reset();
				printedresults = true;
				findMaxima(ip,atc,tmx);
				imp.updateAndDraw();
				printedresults = false;
			}
			else {
				startTime = System.currentTimeMillis();
				calcAverageRot(ip,krn,tmx,imp3,imp5,imp6,imp7,imp8,dummy);
				time = System.currentTimeMillis()-startTime;
				seconds = time/1000.0;
				IJ.write("Took "+IJ.d2s(seconds)+" seconds to calculate rotation.");
			}
		}
//
///		now that the work has been done, draw all images/windows
//
		displayWin(imp3,"Autocorrelation",atc,winorg.width,screen.height-winorg.height);
		if (!savedots) atc.reset();
//
///		omit the maxima, information is marked in original image and ATC-bitmap
//
//		win.setLocation(imp4,"Maxima",winorg.width+winatc.width,screen.height-winorg.height);
//
		displayWin(imp5,"Kernel Average",imp5.getProcessor(),winkrn.width,screen.height-winorg.height-winkrn.height);
		if (rotated)
			displayWin(imp6,"Rotation Kernel Average",imp6.getProcessor(),
				winkrn.width+winkrn.width,screen.height-winorg.height-winkrn.height);
		displayWin(imp7,"Random Difference",imp7.getProcessor(),3*winkrn.width,screen.height-winorg.height-winkrn.height);
		if (rotated) 
			displayWin(imp8,"Rot. Random Difference",imp8.getProcessor(),
				4*winkrn.width,screen.height-winorg.height-winkrn.height);
		}
	}
//
///	try to finish in a clean manner!
//
	if (!savedots) ip.reset();
	ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
	imp.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
	displayWin(imp,"Original Bitmap",imp.getProcessor(),0,screen.height-winorg.height);
	displayWin(imp2,"Kernel",imp2.getProcessor(),0,screen.height-winorg.height-winkrn.height);
}

////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////            Here come the procedures !         /////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////

//
/// findMaxima identifies local maxima of the ATC-bitmap for a given threshold
/// marking the maxima in both the original image and the ATC-bitmap
//
	void findMaxima(ImageProcessor ip, ImageProcessor atc, ImageProcessor tmx) {
		boolean neighbour = true;
		int KernelWidth = ip.getWidth()-atc.getWidth();
		int KernelHeight = ip.getHeight()-atc.getHeight();
		int ACWidth = atc.getWidth();
		int ACHeight = atc.getHeight();
		int localmax = 0;
		int kw = (int)(KernelWidth/2);
		int kh = (int)(KernelHeight/2); 

		for (int x=0; x < ACWidth; x++) {
			for (int y=0; y < ACHeight; y++) {
				tmx.putPixel(x,y,255);
			}
		}
		for (int x=1; x < (ACWidth-1); x++) {
			for (int y=1; y < (ACHeight-1); y++) {
				if (atc.getPixel(x,y)>=thresh) {
					int nbox = 1;
					int mina = Math.min(x,y);
					int minb = Math.min(ACWidth-x,ACHeight-y);
					if (Math.min(mina,minb)<=10) nbox = Math.max(Math.min(mina,minb),1);
					else nbox = 10; 
					localmax = atc.getPixel(x,y);
					int localx = nbox;
					int localy = nbox;
					for (int i=0; i<2*nbox; i++) {
						for (int j=0; j<2*nbox; j++) {
							if (atc.getPixel(x-nbox+i,y-nbox+j)>localmax) {
								localmax=(int)(atc.getPixel(x-nbox+i,y-nbox+j));
								localx=i;
								localy=j;
							}
						}
					}
					tmx.putPixel(x-nbox+localx,  y-nbox+localy,  0);
				}
			}
		}
		for (int x=0; x < ACWidth; x++) {
			for (int y=0; y < ACHeight; y++) {
				if (tmx.getPixel(x,y)!=0) {
					tmx.putPixel(x,y,255);
				} 
			}
		}
		dummy = 0;
		while (neighbour) {
			for (int x=1; x < (ACWidth-1); x++) {
				for (int y=1; y < (ACHeight-1); y++) {
					if (tmx.getPixel(x,y)==0 && (atc.getPixel(x,y)>=thresh)) {
						int nbox = 1;
						int mina = Math.min(x,y);
						int minb = Math.min(ACWidth-x,ACHeight-y);
						if (Math.min(mina,minb)<=3) nbox = Math.max(Math.min(mina,minb),1);
						else nbox = 3; 
						localmax = atc.getPixel(x,y);
						int localx = nbox;
						int localy = nbox;
						for (int i=0; i<2*nbox; i++) {
							for (int j=0; j<2*nbox; j++) {
								if (atc.getPixel(x-nbox+i,y-nbox+j)>localmax) {
									localmax=(int)(atc.getPixel(x-nbox+i,y-nbox+j));
									localx=i;
									localy=j;
								}
							}
						}
						for (int i=0; i<2*nbox; i++) {
							for (int j=0; j<2*nbox; j++) {
								tmx.putPixel(x-nbox+i,y-nbox+j,255);
							}
						}
						tmx.putPixel(x-nbox+localx,  y-nbox+localy,  0);
					}
				}
			}
			neighbour = false;
			dummy += 1;
			for (int x=0; x < ACWidth; x++) {
				for (int y=0; y < ACHeight; y++) {
					if (tmx.getPixel(x,y) != 0) tmx.putPixel(x,y,255);
				}
			}
			for (int x=1; x < (ACWidth-1); x++) {
				for (int y=1; y < (ACHeight-1); y++) {
					if (tmx.getPixel(x,y)==0) {
						if (tmx.getPixel(x-1,y-1) == 0) neighbour = true;
						if (tmx.getPixel(x-1,y  ) == 0) neighbour = true;
						if (tmx.getPixel(x-1,y+1) == 0) neighbour = true;
						if (tmx.getPixel(x  ,y-1) == 0) neighbour = true;
						if (tmx.getPixel(x  ,y+1) == 0) neighbour = true;
						if (tmx.getPixel(x+1,y-1) == 0) neighbour = true;
						if (tmx.getPixel(x+1,y  ) == 0) neighbour = true;
						if (tmx.getPixel(x+1,y+1) == 0) neighbour = true;
					}
				}
			}
			if (dummy>7) neighbour = false;
		}
		if (neighbour==false) {
			dummy = 0;
			for (int x=1; x < (ACWidth-1); x++) {
				for (int y=1; y < (ACHeight-1); y++) {
					if (tmx.getPixel(x,y)==0) {
//						markPixel(atc,x,y,ACWidth,ACHeight);
						markPixel(ip,kw+x,kh+y,ip.getWidth(),ip.getHeight());
						dummy += 1;
					}
				}
			}
			if (!printedresults) IJ.write("Autocorrelation found "+dummy
				+" maxima using threshold "+IJ.d2s(Math.round(thresh/2.55))+"%");
		}
	}

//
///	autoCorrCalc calculates the linear correlation of the roi in the original bitmap with
///	each sub-bitmap of the same size in the original image writing the values into the
///	image processor atc
//

	void autoCorrCalc(ImageProcessor ip, ImageProcessor atc){

	Rectangle r = ip.getRoi();
	int KernelX = (int)(r.x);
	int KernelY = (int)(r.y);
	int KernelWidth = (int)(r.width);
	int KernelHeight = (int)(r.height);
	int ACWidth = atc.getWidth();
	int ACHeight = atc.getHeight();
	int rmin = 255;
	int rmax = 0;
	byte[] sample = new byte[KernelWidth*KernelHeight];

	ip.reset();
	ImageProcessor krnIP = ip.crop();
	krnIP.smooth();
	byte[] kernel = (byte[])krnIP.getPixels();
	for (int x=0; x < ACWidth; x++) {
		for (int y=0; y < ACHeight; y++) {
		double kmean = 0;
		double smean = 0;
		double tk = 0;
		double ts = 0;
		double skk = 0;
		double sss = 0;
		double sks = 0;
		double rfactor = 0;
		double tiny = 1.0e-20;
		ip.setRoi(new Rectangle(x,y,KernelWidth,KernelHeight));
		ImageProcessor ip3 = ip.crop();
		ip3.smooth();
		sample = (byte[])ip3.getPixels();
			for (int i=0; i < KernelWidth*KernelHeight; i++) {
				//IJ.write(kernel[i] + " kernel");
				kmean += (double)(kernel[i]);
				smean += (double)(sample[i]);				
			}
			kmean /= KernelWidth*KernelHeight;
			smean /= KernelWidth*KernelHeight;
			for (int i=0; i < KernelWidth*KernelHeight; i++) {
				tk = kernel[i] - kmean;
				ts = sample[i] - smean;
				skk += tk*tk;
				sss += ts*ts;
				sks += tk*ts;				
			}
			rfactor = sks/(Math.sqrt(skk*sss)+tiny);
			int value = (int)(Math.round(128.0*(rfactor+1)-1.0));
			if (value!=255) {
				if (rmax < value) rmax =  value;
			}
			if (value!=0) {
				if (rmin > value) rmin =  value;
			}
			atc.putPixel(x,y,value);
		}
		IJ.showProgress((double)x/ACWidth);
	}
	((ByteProcessor)atc).applyLut();
	ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
	}

//
///	calcAverage calculates the average for found maxima, writes this into image imp5
///	and also writes the difference of odd-average and even-average matches into image imp7
//

	void calcAverage(ImageProcessor ip, ImageProcessor tmx, ImagePlus imp5, ImagePlus imp7, int maxima) {
	
	Rectangle r = ip.getRoi();
	int KernelX = (int)(r.x);
	int KernelY = (int)(r.y);
	int KernelWidth = ip.getWidth()-tmx.getWidth();
	int KernelHeight = ip.getHeight()-tmx.getHeight();
	int ACWidth = tmx.getWidth();
	int ACHeight = tmx.getHeight();
	int aveMax = ip.getPixel(KernelX,KernelY);
	int aveMin = ip.getPixel(KernelX,KernelY)*maxima;
	int[] xloc = new int[2*maxima];
	int[] yloc = new int[2*maxima];
	int n = 0;
	int[] average = new int[KernelWidth*KernelHeight];
	byte[] sample = new byte[ACWidth*ACHeight];
	ColorModel cm = LookUpTable.createGrayscaleColorModel(false);

	ImageProcessor imp5IP = imp5.getProcessor();
	ImageProcessor imp7IP = imp7.getProcessor();
	for (int x=0; x < ACWidth; x++) {
		for (int y=0; y < ACHeight; y++) {
			if (tmx.getPixel(x,y) == 0) {
				xloc[n] = x;
				yloc[n] = y;
			n += 1;
			}
		}
	}
	for (int x=0; x<KernelWidth; x++) {
		for (int y=0; y<KernelHeight; y++) {
			int ave = 0;
			int evenave = 0;
			int oddave = 0;
			for (int i=0; i<n; i++) {
				ave += ip.getPixel(xloc[i]+x,yloc[i]+y); 
			}
			for (int i=0; i<n/2; i++) {
				evenave += ip.getPixel(xloc[i+1]+x,yloc[i+1]+y); 
				oddave += ip.getPixel(xloc[i]+x,yloc[i]+y); 
			}
			double temp = (double)ave/n;
			ave = (int)Math.round(temp);
			imp5IP.putPixel(x,y,ave);
			temp = ((evenave-oddave)/(n/2.0)+255.0)/2.0;
			ave = (int)Math.round(temp);
			imp7IP.putPixel(x,y,ave);
		}
	}
	imp5.setProcessor("Kernel Average",imp5IP);
	imp5.updateAndDraw();
	imp5.show();	
	imp7.setProcessor("Random Difference",imp7IP);
	imp7.updateAndDraw();	
	}
	
//
///	this procedure will mark a given pixel in image processor ip with a cross
//
	void markPixel(ImageProcessor ip, int x, int y, int limx, int limy) {
	
	int tone = 0;
	int n = 1;

	tone = ip.getPixel(x-1,  y);					// find average tone of line segment
	if (x>1) {tone = tone + ip.getPixel(x-2,  y); n++;}
	if (x>2) {tone = tone + ip.getPixel(x-3,  y); n++;}
	if (x>3) {tone = tone + ip.getPixel(x-4,  y); n++;}
	if (x>4) {tone = tone + ip.getPixel(x-5,  y); n++;}
	if (x>5) {tone = tone + ip.getPixel(x-6,  y); n++;}
	if (x>6) {tone = tone + ip.getPixel(x-7,  y); n++;}
	tone /= n;
	if (tone < 128) tone = 254;
	else tone = 1;
	ip.putPixel(x-1,  y,tone);
	if (x>1) ip.putPixel(x-2,  y,tone);				// put new line segement according to tone
	if (x>2) ip.putPixel(x-3,  y,tone);
	if (x>3) ip.putPixel(x-4,  y,tone);
	if (x>4) ip.putPixel(x-5,  y,tone);
	if (x>5) ip.putPixel(x-6,  y,tone);
	if (x>6) ip.putPixel(x-7,  y,tone);
	tone = 0;
	n = 1;

	tone = ip.getPixel(x+1,  y);
	if (x<limx-1) {tone = tone + ip.getPixel(x+2,  y); n++;}
	if (x<limx-2) {tone = tone + ip.getPixel(x+3,  y); n++;}
	if (x<limx-3) {tone = tone + ip.getPixel(x+4,  y); n++;}
	if (x<limx-4) {tone = tone + ip.getPixel(x+5,  y); n++;}
	if (x<limx-5) {tone = tone + ip.getPixel(x+6,  y); n++;}
	if (x<limx-6) {tone = tone + ip.getPixel(x+7,  y); n++;}
	tone /= n;
	if (tone < 128) tone = 254;
	else tone = 1;
	ip.putPixel(x+1,  y,tone);
	if (x<limx-1) ip.putPixel(x+2,  y,tone);	
	if (x<limx-2) ip.putPixel(x+3,  y,tone);
	if (x<limx-3) ip.putPixel(x+4,  y,tone);
	if (x<limx-4) ip.putPixel(x+5,  y,tone);
	if (x<limx-5) ip.putPixel(x+6,  y,tone);
	if (x<limx-6) ip.putPixel(x+7,  y,tone);
	tone = 0;
	n = 1;

	tone = ip.getPixel(x,y-1);
	if (y>1) {tone = tone + ip.getPixel(x,y-2); n++;}
	if (y>2) {tone = tone + ip.getPixel(x,y-3); n++;}
	if (y>3) {tone = tone + ip.getPixel(x,y-4); n++;}
	if (y>4) {tone = tone + ip.getPixel(x,y-5); n++;}
	if (y>5) {tone = tone + ip.getPixel(x,y-6); n++;}
	if (y>6) {tone = tone + ip.getPixel(x,y-7); n++;}
	tone /= n;
	if (tone < 128) tone = 254;
	else tone = 1;
	ip.putPixel(x,y-1,tone);
	if (y>1) ip.putPixel(x,y-2,tone);	
	if (y>2) ip.putPixel(x,y-3,tone);
	if (y>3) ip.putPixel(x,y-4,tone);
	if (y>4) ip.putPixel(x,y-5,tone);
	if (y>5) ip.putPixel(x,y-6,tone);
	if (y>6) ip.putPixel(x,y-7,tone);
	tone = 0;
	n = 1;

	tone = ip.getPixel(x,y+1);
	if (y<limy-1) {tone = tone + ip.getPixel(x,y+2); n++;}
	if (y<limy-2) {tone = tone + ip.getPixel(x,y+3); n++;}
	if (y<limy-3) {tone = tone + ip.getPixel(x,y+4); n++;}
	if (y<limy-4) {tone = tone + ip.getPixel(x,y+5); n++;}
	if (y<limy-5) {tone = tone + ip.getPixel(x,y+6); n++;}
	if (y<limy-6) {tone = tone + ip.getPixel(x,y+7); n++;}
	tone /= n;
	if (tone < 128) tone = 254;
	else tone = 1;
	ip.putPixel(x,y+1,tone);
	if (y<limy-1) ip.putPixel(x,y+2,tone);	
	if (y<limy-2) ip.putPixel(x,y+3,tone);
	if (y<limy-3) ip.putPixel(x,y+4,tone);
	if (y<limy-4) ip.putPixel(x,y+5,tone);
	if (y<limy-5) ip.putPixel(x,y+6,tone);
	if (y<limy-6) ip.putPixel(x,y+7,tone);
	tone = 0;
	n = 1;
	}

//
///	dialog box to get user parameters
//

	void getThresh() {

	GenericDialog gd = new GenericDialog("Set Cutoff", IJ.getInstance());
	gd.addNumericField("Select Threshold (0%-100%):", Math.round(thresh/2.55), 0);
	gd.addCheckbox("Use Average Kernel", useAveKern);
	gd.addCheckbox("Finished", finished);
	gd.addCheckbox("Rotate Kernel", rotated);
	gd.addCheckbox("Save Crosses", savedots);
	gd.setLocation(0,screen.height-gd.getSize().height);
	gd.showDialog();
	if (gd.wasCanceled()) {
		abort = true;
		finished = true;
		return;
	}
	thresh = (int) Math.round(2.55*(double)(gd.getNextNumber()));
	useAveKern = gd.getNextBoolean();
	finished = gd.getNextBoolean();
	rotated = gd.getNextBoolean();
	savedots = gd.getNextBoolean();
	if (gd.invalidNumber()) {
		IJ.error("Invalid Threshold!");
		return;
	}
	if (thresh >= 255) thresh = 255;
	if (thresh <= 0) thresh = 0;
	}

//
///	dialog box to get additional user parameters in case the user chose rotation
//

	void getRotThresh() {

	GenericDialog gdrot = new GenericDialog("Set Rotation Threshold", IJ.getInstance());
	gdrot.addNumericField("Select Rotation Threshold (0%-100%):", Math.round(rothresh/2.55), 0);
	gdrot.addNumericField("Select ODD displacement constant (3,5,7,...):",disp, 0);
	gdrot.addCheckbox("Check finely spaced set of angles", (!coarse));
	gdrot.addCheckbox("Actually, do NOT rotate", (!rotated));
	gdrot.addCheckbox("Save time, show NO rotation", (true));
	gdrot.addCheckbox("Use Rotation Average as Kernel", (false));
	gdrot.setLocation(0,screen.height-gdrot.getSize().height);
	gdrot.showDialog();
	if (gdrot.wasCanceled()) {
		coarse = true;
		rotated = false;
		finished = true;
		abort = true;
		return;
	}
	rothresh = (int) Math.round(2.55*(double)(gdrot.getNextNumber()));
	disp = (int)gdrot.getNextNumber();
	coarse = (!gdrot.getNextBoolean());
	rotated = (!gdrot.getNextBoolean());
	showrotation = (!gdrot.getNextBoolean());
	knowRotAveKern = (gdrot.getNextBoolean());
	if (gdrot.invalidNumber()) {
		IJ.error("Invalid Threshold!");
		return;
	}
	if (rothresh >= 255) rothresh = 255;
	if (rothresh <= 0) rothresh = 0;
	}

//
///	calculate average in case the user chose rotation
//

	void calcAverageRot(ImageProcessor ip, ImageProcessor krn, ImageProcessor tmx, ImagePlus imp3, ImagePlus imp5, ImagePlus
imp6, ImagePlus imp7, ImagePlus imp8, int maxima) {
	
	this.imp = imp;
	Rectangle r = ip.getRoi();
	int KernelX = (int)(r.x);
	int KernelY = (int)(r.y);
	int KernelWidth = ip.getWidth()-tmx.getWidth();
	int KernelHeight = ip.getHeight()-tmx.getHeight();
	int ACWidth = tmx.getWidth();
	int ACHeight = tmx.getHeight();
	int aveMax = ip.getPixel(KernelX,KernelY);
	int aveMin = ip.getPixel(KernelX,KernelY)*maxima;
	int[] xloc = new int[2*maxima];
	int[] yloc = new int[2*maxima];
	int[] dxloc = new int[2*maxima];
	int[] dyloc = new int[2*maxima];
	int[] finaldxloc = new int[2*maxima];
	int[] finaldyloc = new int[2*maxima];
	double[] angle = new double[2*maxima];
	int[] ATCvalue = new int[2*maxima];
	int[] dislocate = new int[9];
	int n = 0;
	int[] average = new int[KernelWidth*KernelHeight];
	int[] oddaverage = new int[KernelWidth*KernelHeight];
	int[] evenaverage = new int[KernelWidth*KernelHeight];
	byte[] sample = new byte[ACWidth*ACHeight];
	ColorModel cm = LookUpTable.createGrayscaleColorModel(false);

	ImageProcessor atc = imp3.getProcessor();
	ImageProcessor imp5IP = imp5.getProcessor();
	ImageProcessor imp6IP = imp6.getProcessor();
	ImageProcessor imp7IP = imp7.getProcessor();
	ImageProcessor imp8IP = imp8.getProcessor();
//
///	Initialize the maxima
//
	for (int x=0; x < ACWidth; x++) {
		for (int y=0; y < ACHeight; y++) {
			if (tmx.getPixel(x,y) == 0) {
				xloc[n] = x;
				yloc[n] = y;
				n += 1;
			}
		}
	}
//
///	Initialize the Maximum of ATC as a function of rotation angle for all maxima
//
	ip.reset();
	for (int i=0; i<n; i++) {
		int writeATC = (int)Math.round(atc.getPixel(xloc[i],yloc[i])/2.55); 
		IJ.write("("+xloc[i]+","+yloc[i]+") = point of rotation @ "+writeATC+" % ATC");
		IJ.wait(50);  // give system time to redraw ImageJ window
		markPixel(ip,KernelWidth/2+xloc[i],KernelWidth/2+yloc[i],ip.getWidth(),ip.getHeight());
		imp.updateAndDraw();
		IJ.wait(50);  // give system time to redraw ImageJ window
		ip.reset();
		double ang = 0.0;
		int temp = 0;
		int presentATCvalue = 0;
		if (coarse) {
			for (int j=0; j<90; j++) {
				ip.reset();
				ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
//				temp = autoCorrRot(ip,krn,xloc[i],yloc[i],ang);
				temp = atcRotWiggle(ip,imp6,xloc,yloc,dxloc,dyloc,i,ang,ACWidth,ACHeight,disp);
				if (showrotation) {
					ip.setRoi(new Rectangle(xloc[i],yloc[i],KernelWidth,KernelHeight));
					ip.rotate(ang);
					markPixel(ip,KernelWidth/2+xloc[i],KernelWidth/2+yloc[i],ip.getWidth(),ip.getHeight());
					imp.updateAndDraw();
					ip.reset();
				}
				if (presentATCvalue<temp) {
					presentATCvalue = temp;
					angle[i] = (double) ang;
					ATCvalue[i] = temp;
					finaldxloc[i] = dxloc[i];
					finaldyloc[i] = dyloc[i];
				}
				ang += 4.0;
				IJ.showProgress((double)(i*360+ang)/(360*n));
			}
		}
		else {
			for (int j=0; j<720; j++) {
				ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
//				temp = autoCorrRot(ip,krn,xloc[i],yloc[i],ang);
				temp = atcRotWiggle(ip,imp6,xloc,yloc,dxloc,dyloc,i,ang,ACWidth,ACHeight,disp);
				if (showrotation) {
					ip.setRoi(new Rectangle(xloc[i],yloc[i],KernelWidth,KernelHeight));
					ip.setInterpolate(true);
					ip.rotate(ang);
				}
				markPixel(ip,KernelWidth/2+xloc[i],KernelWidth/2+yloc[i],ip.getWidth(),ip.getHeight());
				if (showrotation) imp.updateAndDraw();
				ip.reset();
				if (presentATCvalue<temp) {
					presentATCvalue = temp;
					angle[i] = (double) ang;
					ATCvalue[i] = temp;
					finaldxloc[i] = dxloc[i];
					finaldyloc[i] = dyloc[i];
				}
				ang += 0.5;
				IJ.showProgress((double)(i*360+ang)/(360*n));
			}			
		}
		imp.updateAndDraw();
	}
//
///	Calculate average including image rotation
//
//
///	First the normal kernel average without rotation
//
	for (int x=0; x<KernelWidth; x++) {
		for (int y=0; y<KernelHeight; y++) {
			int ave = 0;
			int evenave = 0;
			int oddave = 0;
			for (int i=0; i<n; i++) {
				ave += ip.getPixel(xloc[i]+x,yloc[i]+y); 
			}
			for (int i=0; i<n/2; i++) {
				evenave += ip.getPixel(xloc[i+1]+x,yloc[i+1]+y); 
				oddave += ip.getPixel(xloc[i]+x,yloc[i]+y); 
			}
			double temp = ave/n;
			ave = (int)temp;
			imp5IP.putPixel(x,y,ave);
			temp = (evenave-oddave)/(n/2)+127;
			ave = (int)Math.round(temp);
			imp7IP.putPixel(x,y,ave);
		}
	}
	imp5.setProcessor("Kernel Average",imp5IP);
	imp5.updateAndDraw();
	imp5.show();	
	imp7.setProcessor("Random Difference",imp7IP);
	imp7.updateAndDraw();	
//
///	Now the kernel average using rotation and the rotation threshold
//
	int index = 0;
	int number = 0;
	int onumber = 0;
	int enumber = 0;
	boolean thisodd = true;
	for (int i=0; i<KernelWidth*KernelHeight; i++) {
		average[i] = 0;
		oddaverage[i] = 0;
		evenaverage[i] = 0;
	}
	IJ.write("Correlation for match:");
	for (int i=0; i<n; i++) {
		int cta = (int)Math.round(ATCvalue[i]/2.55); 
		int ctb = (int)(angle[i]); 
		int ctc = (int)Math.round(rothresh/2.55); 
		int ctd = (int)Math.round(atc.getPixel(xloc[i],yloc[i])/2.55); 
		IJ.write(" #"+(int)(i+1)+": "+cta+"% at "+IJ.d2s(angle[i])+"deg and "+ctc+"% threshold."); // ("+ctd+"% at 0deg).");
		if (ATCvalue[i] >= rothresh) {
			thisodd = (!thisodd);
			number += 1;
			if (thisodd) onumber += 1;
			if (!thisodd) enumber += 1;
			IJ.write("     averaging number "+number+" at displacement("
				+(finaldxloc[i]-(disp-1)/2)+","+(finaldyloc[i]-(disp-1)/2)+").");
			ip.setRoi(new
Rectangle(xloc[i]+finaldxloc[i]-(disp-1)/2,yloc[i]+finaldyloc[i]-(disp-1)/2,KernelWidth,KernelHeight));
			ip.setInterpolate(true);
			ip.rotate(angle[i]);
			index = 0;
			for (int x=0; x<KernelWidth; x++) {
				for (int y=0; y<KernelHeight; y++) {
				average[y*KernelWidth+x] += ip.getPixel(xloc[i]+x,yloc[i]+y);
				if (thisodd) oddaverage[y*KernelWidth+x] += ip.getPixel(xloc[i]+x,yloc[i]+y);
				if (!thisodd) evenaverage[y*KernelWidth+x] += ip.getPixel(xloc[i]+x,yloc[i]+y);
				}
			}
			ip.reset();
		}
		else IJ.write("     omitting this match.");
	}
	if (number > 0) {
	for (int x=0; x<KernelWidth; x++) {
		for (int y=0; y<KernelHeight; y++) {
			imp6IP.putPixel(x,y,(int)(average[y*KernelWidth+x]/number));
			int odda = (int)Math.round(oddaverage[y*KernelWidth+x]/(1.0*onumber+0.0001));
			int evena = (int)Math.round(evenaverage[y*KernelWidth+x]/(1.0*enumber+0.0001));
			imp8IP.putPixel(x,y,(int)Math.round(0.5*(odda-evena+255)));
		}
	}}
	IJ.write("Kernel location=("+KernelX+","+KernelY+"), width="+KernelWidth+", height="+KernelHeight);
	IJ.write("Added "+n+" cells to make the kernel average.");
	IJ.write("Added "+number+" cells to make the kernel rotation average.");
	IJ.write("Tried "+disp+"x"+disp+" neighboring cells to find best rotation average.");
	for (int i=0; i<n; i++) {
		if (ATCvalue[i] > rothresh)
			markPixel(ip,KernelWidth/2+xloc[i],KernelWidth/2+yloc[i],ip.getWidth(),ip.getHeight());
//			markPixel(atc,xloc[i],yloc[i],atc.getWidth(),atc.getHeight());	
	}
	knowRotAveKern = true;
	imp.updateAndDraw();
	imp3.updateAndDraw();
	imp8.setProcessor("Random Diff. Rot.",imp8IP);
	imp8.updateAndDraw();	
}

//
///	calculate single ATC-value including S rotation of the chosen sub-bitmap
//

	int autoCorrRot(ImageProcessor ip, ImageProcessor krn, int xloc, int yloc, double angle){

		Rectangle r = ip.getRoi();
		int KernelX = (int)(r.x);
		int KernelY = (int)(r.y);
		int KernelWidth = (int)(r.width);
		int KernelHeight = (int)(r.height);
		int rmin = 255;
		int rmax = 0;
		byte[] atcRotSample = new byte[KernelWidth*KernelHeight];
		byte[] atcRotKernel = new byte[KernelWidth*KernelHeight];
		ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
		ImageProcessor atcRotKernelIP = krn;
		atcRotKernelIP.smooth();
		atcRotKernel = (byte[])atcRotKernelIP.getPixels();
		double kmean = 0;
		double smean = 0;
		double tk = 0;
		double ts = 0;
		double skk = 0;
		double sss = 0;
		double sks = 0;
		double rfactor = 0;
		double tiny = 1.0e-20;
		ip.setRoi(new Rectangle(xloc,yloc,KernelWidth,KernelHeight));
		if (angle != 0.0) {
			ip.setInterpolate(true);
			ip.rotate(angle);
		}
		ImageProcessor atcRotSampleIP = ip.crop();
		ip.reset();
		atcRotSampleIP.smooth();
		atcRotSample = (byte[])atcRotSampleIP.getPixels();
		for (int i=0; i < KernelWidth*KernelHeight; i++) {
			kmean += (double)(atcRotKernel[i]);
			smean += (double)(atcRotSample[i]);				
		}
		kmean /= KernelWidth*KernelHeight;
		smean /= KernelWidth*KernelHeight;
		for (int i=0; i < KernelWidth*KernelHeight; i++) {
			tk = atcRotKernel[i] - kmean;
			ts = atcRotSample[i] - smean;
			skk += tk*tk;
			sss += ts*ts;
			sks += tk*ts;				
		}
		rfactor = sks/(Math.sqrt(skk*sss)+tiny);
		int value = (int)(Math.round(128.0*(rfactor+1)-1.0));
		ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
		return value;
	}

//
///	find the maximum ATC-value in a (disp x disp) array of locations around a maximum including rotation
//

	int atcRotWiggle(ImageProcessor ip, ImagePlus imp6, int[] xloc , int[] yloc,int[] dxloc , int[] dyloc, int j, double angle,
int ACWidth, int ACHeight, int disp){

		boolean atcincrease = true;
		Rectangle r = ip.getRoi();
		int KernelX = (int)(r.x);
		int KernelY = (int)(r.y);
		int KernelWidth = (int)(r.width);
		int KernelHeight = (int)(r.height);
		int rmin = 255;
		int rmax = 0;
		int atcvalue = 0;
		int tempatcvalue = 0;
		int nfin = 0;
		int mfin = 0;
		byte[] atcRotSample = new byte[KernelWidth*KernelHeight];
		byte[] atcRotKernel = new byte[KernelWidth*KernelHeight];

		ImageProcessor atcRotKernelIPave = imp6.getProcessor();
		imp6.updateAndDraw();
		ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
		ImageProcessor atcRotKernelIP = ip.crop();
		atcRotKernelIP.smooth();
		if (knowRotAveKern) atcRotKernel = (byte[])atcRotKernelIPave.getPixels();
		if (!knowRotAveKern) atcRotKernel = (byte[])atcRotKernelIP.getPixels();
		while (atcincrease) {
		atcincrease = false;
		for (int n=0; n<disp; n++) {
		for (int m=0; m<disp; m++) {
		if ((xloc[j]+(n-1))>=0 && (xloc[j]+(n-1))<ACWidth && (yloc[j]+(m-1))>=0 && (yloc[j]+(m-1))<ACHeight) {
			double kmean = 0;
			double smean = 0;
			double tk = 0;
			double ts = 0;
			double skk = 0;
			double sss = 0;
			double sks = 0;
			double rfactor = 0;
			double tiny = 1.0e-20;
			ip.setRoi(new Rectangle(xloc[j]+(n-(disp-1)/2),yloc[j]+(m-(disp-1)/2),KernelWidth,KernelHeight));
			if (angle != 0.0) {
				ip.setInterpolate(true);
				ip.rotate(angle);
			}
			ImageProcessor atcRotSampleIP = ip.crop();
			ip.reset();
			atcRotSampleIP.smooth();
			atcRotSample = (byte[])atcRotSampleIP.getPixels();
			for (int i=0; i < KernelWidth*KernelHeight; i++) {
				kmean += (double)(atcRotKernel[i]);
				smean += (double)(atcRotSample[i]);				
			}
			kmean /= KernelWidth*KernelHeight;
			smean /= KernelWidth*KernelHeight;
			for (int i=0; i < KernelWidth*KernelHeight; i++) {
				tk = atcRotKernel[i] - kmean;
				ts = atcRotSample[i] - smean;
				skk += tk*tk;
				sss += ts*ts;
				sks += tk*ts;				
			}
			rfactor = sks/(Math.sqrt(skk*sss)+tiny);
			tempatcvalue = (int)(Math.round(128.0*(rfactor+1)-1.0));
			if (atcvalue<tempatcvalue) {
					atcincrease = true;
					atcvalue = tempatcvalue;
					nfin = n;
 					mfin = m;
			} 			
			ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
		}
		}
		}}
		dxloc[j] = nfin;
		dyloc[j] = mfin;
		return atcvalue;
	}

//
///	break for debugging purposes.... I have yet to find a good java development kit
//

	void addBreak() {

	GenericDialog gdbreak = new GenericDialog("Break", IJ.getInstance());
	gdbreak.addCheckbox("Actually, do NOT rotate", (!rotated));
	gdbreak.setLocation(0,screen.height-gdbreak.getSize().height);
	gdbreak.showDialog();
	gdbreak.getNextBoolean();
	if (gdbreak.wasCanceled()) {
		return;
	}
	}

//
///	use the kernel average to recalculate the ATC-bitmap
//

	void 	generateAverageKernel(ImageProcessor ip,ImagePlus imp5,ImageProcessor atc,ImageProcessor tmx, int maxima){

	Rectangle r = ip.getRoi();
	int KernelX = (int)(r.x);
	int KernelY = (int)(r.y);
	int KernelWidth = ip.getWidth()-tmx.getWidth();
	int KernelHeight = ip.getHeight()-tmx.getHeight();
	int ACWidth = tmx.getWidth();
	int ACHeight = tmx.getHeight();
	int aveMax = ip.getPixel(KernelX,KernelY);
	int aveMin = ip.getPixel(KernelX,KernelY)*maxima;
	int[] xloc = new int[2*maxima];
	int[] yloc = new int[2*maxima];
	int n = 0;
	int[] average = new int[KernelWidth*KernelHeight];
	ColorModel cm = LookUpTable.createGrayscaleColorModel(false);
	int rmin = 255;
	int rmax = 0;
	byte[] sample = new byte[KernelWidth*KernelHeight];

	ip.reset();
	ImageProcessor avekrn = imp5.getProcessor();
	for (int x=0; x < ACWidth; x++) {
		for (int y=0; y < ACHeight; y++) {
			if (tmx.getPixel(x,y) == 0) {
				xloc[n] = x;
				yloc[n] = y;
			n += 1;
			}
		}
	}
//
///	Calculate kernel average to use as new kernel if rotated average is not yet known
//
	for (int x=0; x<KernelWidth; x++) {
		for (int y=0; y<KernelHeight; y++) {
			int ave = 0;
			for (int i=0; i<n; i++) {
				ave += ip.getPixel(xloc[i]+x,yloc[i]+y); 
			}
			double temp = (double)ave/n;
			ave = (int)temp;
			avekrn.putPixel(x,y,ave);
		}
	}
//
///	Recalculate linear correlation using the kernel average as kernel. 
//
//	avekrn.smooth();
	byte[] kernel = (byte[])avekrn.getPixels();
	for (int x=0; x < ACWidth; x++) {
		for (int y=0; y < ACHeight; y++) {
		double kmean = 0;
		double smean = 0;
		double tk = 0;
		double ts = 0;
		double skk = 0;
		double sss = 0;
		double sks = 0;
		double rfactor = 0;
		double tiny = 1.0e-20;
		ip.setRoi(new Rectangle(x,y,KernelWidth,KernelHeight));
		ImageProcessor ip3 = ip.crop();
		ip3.smooth();
		sample = (byte[])ip3.getPixels();
			for (int i=0; i < KernelWidth*KernelHeight; i++) {
				//IJ.write(kernel[i] + " kernel");
				kmean += (double)(kernel[i]);
				smean += (double)(sample[i]);				
			}
			kmean /= KernelWidth*KernelHeight;
			smean /= KernelWidth*KernelHeight;
			for (int i=0; i < KernelWidth*KernelHeight; i++) {
				tk = kernel[i] - kmean;
				ts = sample[i] - smean;
				skk += tk*tk;
				sss += ts*ts;
				sks += tk*ts;				
			}
			rfactor = sks/(Math.sqrt(skk*sss)+tiny);
			int value = (int)(Math.round(128.0*(rfactor+1)-1.0));
			if (value!=255) {
				if (rmax < value) rmax =  value;
			}
			if (value!=0) {
				if (rmin > value) rmin =  value;
			}
			atc.putPixel(x,y,value);
		}
		IJ.showProgress((double)x/ACWidth);
	}
	((ByteProcessor)atc).applyLut();
	imp5.setProcessor("Kernel Average",avekrn);
	imp5.updateAndDraw();
	imp5.show();
	ip.setRoi(new Rectangle(KernelX,KernelY,KernelWidth,KernelHeight));
	IJ.write("Recalculated ATC-function using average kernel!");
	}
	
//
///	VERY ugly way of getting images updated throughout my procedure, but I lost
///	patience in finding out how to do it "right"; suggestions very welcome.
///	How do they say: "There is nothing more successfull than success."
//

	void displayWin(ImagePlus imp, String name, ImageProcessor ip, int xpos, int ypos) {
	
		imp.setProcessor(name,ip);
		imp.updateAndDraw();
		imp.show();
		win = imp.getWindow();
		win.setLocation((xpos+1),(ypos+1));
		win = imp.getWindow();
		win.setLocation(xpos,ypos);
		imp.show();
	}

}



