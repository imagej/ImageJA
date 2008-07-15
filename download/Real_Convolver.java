import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;

/** This plugin does convolutions on real images using user user defined kernels. */

public class Real_Convolver implements PlugInFilter {

	ImagePlus imp;
	boolean canceled;
	static String kernel = ".0625  .125  .0625\n.125    .25    .125\n.0625  .125  .0625";
	static boolean autoScale = true;
	static boolean createSelection = true;
	int kw, kh;

	public int setup(String arg, ImagePlus imp) {
		if (IJ.versionLessThan("1.17j"))
			return DONE;
 		// Registration may help static fields from being reset but may
 		// also prevent 'Compile and Run' from reloading this class.
 		IJ.register(Real_Convolver.class);
		this.imp = imp;
		return DOES_32+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		float[] kernel = getKernel();
		imp.startTiming();
		if (kernel!=null) {
			if ((kw&1) ==0) {
				IJ.error("The kernel must be square and have an\n"
					+"odd width. This kernel is "+kw+"x"+kh+".");
				return;
			}
			IJ.showStatus("Real Convolver: convolving with "+kw+"x"+kh+" kernel");
			convolve(ip, kernel, kw, kh);
			ip.setMinAndMax(0,0);
			if (createSelection)
				imp.setRoi(kw/2,kh/2,imp.getWidth()-(kw/2)*2, imp.getHeight()-(kh/2)*2);
		} ;
	}
	
	float[] getKernel() {
		GenericDialog gd = new GenericDialog("Real Convolver...", IJ.getInstance());
		gd.addTextAreas(kernel, null, 10, 30);
		gd.addCheckbox("Scale", autoScale);
		gd.addCheckbox("Create Selection", createSelection);
		gd.showDialog();
		if (gd.wasCanceled()) {
			canceled = true;
			return null;
		}
		kernel = gd.getNextText();
		autoScale = gd.getNextBoolean();
		createSelection = gd.getNextBoolean();
		StringTokenizer st = new StringTokenizer(kernel);
		int n = st.countTokens();
		kw = (int)Math.sqrt(n);
		kh = kw;
		n = kw*kh;
		float[] k = new float[n];
		for (int i=0; i<n; i++)
			k[i] = (float)getNum(st);
		//IJ.write("kw: "+kw);
		return k;
	}

	double getNum(StringTokenizer st) {
		Double d;
		String token = st.nextToken();
		try {d = new Double(token);}
		catch (NumberFormatException e){d = null;}
		if (d!=null)
			return(d.doubleValue());
		else
			return 0.0;
	}

	public void convolve(ImageProcessor ip, float[] kernel, int kw, int kh) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		int uc = kw/2;    
		int vc = kh/2;
		float[] pixels = (float[])ip.getPixels();
		float[] pixels2 = (float[])ip.getPixelsCopy();
		for (int i=0; i<width*height; i++)
			pixels[i] = 0f;

		double scale = 1.0;
		if (autoScale) {
			double sum = 0.0;
			for (int i=0; i<kernel.length; i++)
				sum += kernel[i];
			if (sum!=0.0)
				scale = (float)(1.0/sum);
		}

 		int progress = Math.max(height/25,1);
		double sum;
		int offset, i;  
		for(int y=vc; y<height-vc; y++) {
			if (y%progress ==0) IJ.showProgress((double)y/height);
			//IJ.write(""+y);
			for(int x=uc; x<width-uc; x++) {
				sum = 0.0;
				i = 0;
				for(int v=-vc; v <= vc; v++) {
					offset = x+(y+v)*width;
					for(int u = -uc; u <= uc; u++) {
    						sum +=pixels2[offset+u] * kernel[i++];
	    				}
	    			}
				pixels[x+y*width] = (float)(sum*scale);
					
			}
    		}
   		IJ.showProgress(1.0);
   	 }

}


