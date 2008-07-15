import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

public class RGB_Gray_Merge implements PlugIn {

	private ImagePlus imp; ImagePlus[] image = new ImagePlus[4];

	ImageStack rgb; int w,h,d; boolean delete;

        static int  G=0;
	static int  r=1;
	static int  g=2;
	static int  b=3;
	
	/* Merges one, two or three 8-bit stacks into an RGB stack. */
	public void run(String arg) {
		imp = WindowManager.getCurrentImage();
		if (GetStacks() && CheckStacks()) {
	                CombineStacks(delete);
		 	DisplayResult();
			CloseUsedStacks(delete);
		}
	}

	/** Combines four grayscale stacks into one RGB stack. */
	public boolean GetStacks() {
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.error("No images are open.");
			return false;
		}

		String[] titles = new String[wList.length+1];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		String none = "*None*";
		titles[wList.length] = none;

		GenericDialog gd = new GenericDialog("Gray-RGB Stack Merge");
		gd.addChoice("Gray Stack:", titles, titles[0]);
		gd.addChoice("Red Stack:", titles, titles[1]);
		String title3 = titles.length>2?titles[2]:none;
		gd.addChoice("Green Stack:", titles, title3);
		String title4 = titles.length>3?titles[3]:none;
		gd.addChoice("Blue Stack:", titles, title4);
		gd.addCheckbox("Keep source stacks", true);

		gd.showDialog();
		if (gd.wasCanceled()) 
			return false;
		for(int ii=0;ii<4;ii++) {
			int FILE=gd.getNextChoiceIndex();
			image[ii] = (FILE<wList.length)
					? WindowManager.getImage(wList[FILE])
					: null;
		}
		delete = !gd.getNextBoolean();
		return true;
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
				GS= (byte[])((image[G]!=null) ? image[G].getStack().getPixels(remove?1:ss) : null) ;
				rS= (byte[])((image[r]!=null) ? image[r].getStack().getPixels(remove?1:ss) : null) ;
				gS= (byte[])((image[g]!=null) ? image[g].getStack().getPixels(remove?1:ss) : null) ;
				bS= (byte[])((image[b]!=null) ? image[b].getStack().getPixels(remove?1:ss) : null) ;
				if (GS!=null){
					for(int pp=0; pp<numpels; pp++){
						rPels[pp] = (byte)((rS!=null) ? (GS[pp])|(rS[pp]) : GS[pp]);
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
		new ImagePlus("Color", rgb).show();
	}
}



