import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;

/**
	This plugin highlights the colocalizated points of two 8-bits 
	images (or stacks) . The two images (or stacks) will be affected 
	to the two Red and Green channels of an RGB image (or stack).
	The colocalizated points will appear white by default (Display 
	value = 255).
	The plugin initially generates an 8bits image with only the 
	colocalizated points (image available by validating Colocalizated 
	points 8-bit), then it combines the three 8-bits images in
	an RGB image. Two points are considered as colocalizated if 
	their respective intensities are strictly higher than the threshold of 
	their channels (which are 50 by default: Threshold
	channel 1 (0-255)), and if their ratio (of intensity) is strictly 
	higher than the ratio setting value (which is 50% by defect: ratio 
	(0-100%)).



	Pierre Bourdoncle 11/03/2003
	Institut Jacques Monod 
	Service Imagerie
	2, place Jussieu Tour 43 
	75005 Paris
	bourdoncle@ijm.jussieu.fr
	bourdoncle@wanadoo.fr

*/


public class Colocalization_  implements PlugIn {

    static String title = "Colocalization";
    static final int SCALE=0;
    static int operation = SCALE;
    static double f = 255;
    static double k1 = 50;
    static double k2 = 50;
    static double r = 50;
    static boolean createWindow = false;
    int[] wList;
    private String[] titles;
    int i1Index;
    int i2Index;
    ImagePlus i1;
    ImagePlus i2;
    private ImagePlus imp; ImagePlus[] image = new ImagePlus[4];
    ImageStack rgb; int w,h,d; boolean delete;
    static int  R=0;
    static int  G=1;
    static int  B=2;
    static int  g=3;

    public void run(String arg) {
    if (IJ.versionLessThan("1.27w"))
        return;
        wList = WindowManager.getIDList();
        if (wList==null || wList.length<2) {
            IJ.showMessage(title, "There must be at least two windows open");
            return;
        }
        titles = new String[wList.length];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null)
                titles[i] = imp.getTitle();
            else
                titles[i] = "";
        }

           if (!showDialog())
            return; 
     
        long start = System.currentTimeMillis();
        boolean calibrated = i1.getCalibration().calibrated() || i2.getCalibration().calibrated();
        if (calibrated)
           createWindow = true;
            i2 = duplicateImage(i2, calibrated);
        if (createWindow) {
            i2.show();
            if (i2==null)
                {IJ.showMessage(title, "Out of memory"); return;}
        
        } 
        calculate(i1, i2, k1, k2 , r, f );
        IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");
		if (CheckStacks()) {
	                CombineStacks(delete);
		 	DisplayResult();
			CloseUsedStacks(delete);

		}    

    }
    
    public boolean showDialog() {
        GenericDialog gd = new GenericDialog(title);
        gd.addChoice("Channel_1 (red):", titles, titles[0]);
        gd.addChoice("Channel_2 (green):", titles, titles[1]);
        gd.addMessage(" ");
        gd.addNumericField("Ratio (0-100%):", r, 1);
        gd.addNumericField("Threshold_channel_1 (0-255):", k1, 1);
        gd.addNumericField("Threshold_channel_2 (0-255):", k2, 1);
        gd.addMessage(" ");
        gd.addNumericField("Display value (0-255):", f, 1);
        gd.addCheckbox("Colocalizated points 8-bit", createWindow);
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        int i1Index = gd.getNextChoiceIndex();
        int i2Index = gd.getNextChoiceIndex();
        r = gd.getNextNumber();
        k1 = gd.getNextNumber();
        k2 = gd.getNextNumber();
        f = gd.getNextNumber();
        createWindow = gd.getNextBoolean();
        i1 = WindowManager.getImage(wList[i1Index]);
        i2 = WindowManager.getImage(wList[i2Index]);
        image[R] =  WindowManager.getImage(wList[i1Index]);
        image[G] =  WindowManager.getImage(wList[i2Index]);
        int d1 = i1.getStackSize();
        int d2 = i2.getStackSize();
        if (d2==1 && d1>1) {
            IJ.showMessage(title, "If i2 is not a stack then i1 must also not be a stack.");
            return false;
        }
        return true;
    }

    public void calculate(ImagePlus i1, ImagePlus i2, double k1, double k2 , double r ,double f) {
        double v1, v2;
        int width  = i1.getWidth();
        int height = i1.getHeight();
        ImageProcessor ip1, ip2;
        int slices1 = i1.getStackSize();
        int slices2 = i2.getStackSize();
        float[] ctable1 = i1.getCalibration().getCTable();
        float[] ctable2 = i2.getCalibration().getCTable();
        ImageStack stack1 = i1.getStack();
        ImageStack stack2 = i2.getStack();
        int currentSlice = i2.getCurrentSlice();

        for (int n=1; n<=slices2; n++) {
            ip1 = stack1.getProcessor(n<=slices1?n:slices1);
            ip2 = stack2.getProcessor(n);
            ip1.setCalibrationTable(ctable1);
            ip2.setCalibrationTable(ctable2);
            for (int x=0; x<width; x++) {
                for (int y=0; y<height; y++) {
                    v1 = ip1.getPixelValue(x,y);
                    v2 = ip2.getPixelValue(x,y);
                    switch (operation) {
                              case SCALE: v2 =v1>k1&v2>k2&v1/v2*100>r&v2/v1*100>r?f:0.0 ;
                    }
            
             ip2.putPixelValue(x, y, v2);
                }   
            }  
            if (n==currentSlice) {
                i2.getProcessor().resetMinAndMax();
                i2.updateAndDraw();
            }     
            IJ.showProgress((double)n/slices2);
            IJ.showStatus(n+"/"+slices2);
        }
    }

   ImagePlus duplicateImage(ImagePlus img1, boolean calibrated) {
        ImageStack stack1 = img1.getStack();
        int width = stack1.getWidth();
        int height = stack1.getHeight();
        int n = stack1.getSize();
        ImageStack stack2 = img1.createEmptyStack();
        float[] ctable = img1.getCalibration().getCTable();
        try {
            for (int i=1; i<=n; i++) {
                ImageProcessor ip1 = stack1.getProcessor(i);
                ImageProcessor ip2 = ip1.duplicate(); 
                if (calibrated) {
                    ip2.setCalibrationTable(ctable);
                    ip2 = ip2.convertToFloat();
                }
                stack2.addSlice(stack1.getSliceLabel(i), ip2);
            }
        }
        catch(OutOfMemoryError e) {
            stack2.trim();
            stack2 = null;
            return null;
        }
        ImagePlus img2 =  new ImagePlus("Colocalizated points (8-bit) ", stack2);
image[3] = img2;
        return img2;
    }

	public boolean CheckStacks(){
		int stackSize, width, height, type, img=0;
		while (image[img]==null) img++;
		if(img>=4) IJ.error("an image must exist");
		d=stackSize = image[img].getStackSize();
		h=height = image[img].getHeight();
		w=width = image[img].getWidth();
		type = image[img].getType();
		if (stackSize <1 ) {
			IJ.error("require stackSize>0");
			return false;
		}
		if (type != ImagePlus.GRAY8) {
			IJ.error("require 8-bit grayscale");
			return false;
		}
		if (width <1 ) {
			IJ.error("require width>0");
			return false;
		}
		if (height <1 ) {
			IJ.error("require height>0");
			return false;
		}
		for (int ii=0; ii<4; ii++){
			if(image[ii]!=null){
				if(stackSize!=image[ii].getStackSize()) {
					IJ.error("stackSize mismatch");
					return false;
				}
				if(height!=image[ii].getHeight()){
					IJ.error("height mismatch");
					return false;
				}
				if(width!=image[ii].getWidth()) {
					IJ.error("width mismatch");
					return false;
				}
				if(type!=image[ii].getType()) {
					IJ.error("type mismatch");
					return false;
				}
			}
		}
	   return true;
	}
	public void CombineStacks(boolean remove){
		rgb = new ImageStack(w, h);
		int inc = d/10; /* for showing progress */
		int numpels = w*h;
		if (inc<1) inc = 1;
		ColorProcessor cp;
		byte[] GS,rS,gS,bS; /* source pointers */
		byte[] rPels=new byte[w*h];	
		byte[] gPels=new byte[w*h];
		byte[] bPels=new byte[w*h];
		byte[] blank=new byte[w*h];
		try{

			for (int ss=1; ss<=d; ss++) { /* i is the image slice among d slices per stack */
				cp = new ColorProcessor(w, h); /* MAY NEED TO DO THIS INSIDE STACK BUILDING LOOP */		
				GS= (byte[])((image[g]!=null) ? image[g].getStack().getPixels(remove?1:ss) : null) ;
				rS= (byte[])((image[R]!=null) ? image[R].getStack().getPixels(remove?1:ss) : null) ;
				gS= (byte[])((image[G]!=null) ? image[G].getStack().getPixels(remove?1:ss) : null) ;
				bS= (byte[])((image[B]!=null) ? image[B].getStack().getPixels(remove?1:ss) : null) ;
				if (GS!=null){
					for(int pp=0; pp<numpels; pp++){
						rPels[pp] = (byte)((rS!=null) ?(GS[pp])|(rS[pp]) : GS[pp]);
						gPels[pp] = (byte)((gS!=null) ? (GS[pp])|(gS[pp]) : GS[pp]);
						bPels[pp] = (byte)((bS!=null) ? (GS[pp])|(bS[pp]) : GS[pp]);
					}
		    			cp.setRGB(rPels, gPels, bPels);
				}
				else{
					cp.setRGB(rS=(rS!=null)?rS:blank, gS=(gS!=null)?gS:blank, bS=(bS!=null)?bS:blank);	
				}
				rgb.addSlice(null, cp);
				if(remove)
				for(int ii=0; ii<4;ii++){
					if(image[ii]!=null)
						image[ii].getStack().deleteSlice(1);
				}
				if ((ss%inc) == 0) IJ.showProgress((double)ss/d);
			}
			IJ.showProgress(1.0);
		} catch(OutOfMemoryError o) {
			IJ.outOfMemory("MergeStacks");
			IJ.showProgress(1.0);
		}
	}

	public void CloseUsedStacks(boolean close){
	if (close)
		for (int i=0; i<4; i++) {
			if (image[i]!=null) {
				image[i].changes = false;
				ImageWindow win = image[i].getWindow();
				if (win!=null)
					win.close();
			}
		}
	}
	
	public void DisplayResult(){
          		new ImagePlus("Colocalizated points (RGB) ", rgb).show();
	}
}

