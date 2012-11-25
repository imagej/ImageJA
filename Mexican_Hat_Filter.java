import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.gui.Roi;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * @version 	2.0	18 Nov 2012
 * 				semi-separable implementation
 * 				1.0	 8 Nov 2012
 *   
 * 
 * @author Dimiter Prodanov
 * 		  IMEC
 *
 *
 * @contents
 * This pluign convolves an image with a Mexican Hat filter
 * 
 * 
 * @license This library is free software; you can redistribute it and/or
 *      modify it under the terms of the GNU Lesser General Public
 *      License as published by the Free Software Foundation; either
 *      version 2.1 of the License, or (at your option) any later version.
 *
 *      This library is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *       Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public
 *      License along with this library; if not, write to the Free Software
 *      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
public class Mexican_Hat_Filter implements ExtendedPlugInFilter, DialogListener {

	private PlugInFilterRunner pfr=null;

	final int flags=DOES_ALL+CONVERT_TO_FLOAT+SUPPORTS_MASKING+KEEP_PREVIEW;
	private String version="2.0";
	private int nPasses=1;
	private int pass;

	private static int staticSZ = 5;
	private int sz = staticSZ;
	private float[][] kernel=null;
	public static boolean debug=IJ.debugMode;

	private Roi roi;

	public static boolean sep=false;

	@Override
	public int setup(String arg, ImagePlus imp) {
		if (IJ.versionLessThan("1.47g"))
			return DONE;
		return  flags;
	}

	@Override
	public void run(ImageProcessor ip) {
		pass++;
		int r = (sz-1)/2;
		float[] kernx= gauss1D(r);
		float[] kern_diff= diff2Gauss1D(r);
		kernel=new float[3][];
		kernel[0]=kernx;
		kernel[1]=kern_diff;
		
		float[] kernel2=computeKernel2D(r);
		kernel[2]=kernel2;
		
		if (debug && pass==1) {
			FloatProcessor fp=new FloatProcessor(sz,sz);
			
			float[][] disp= new float[2][];
			
			disp[0]=joinXY(kernel, 0, 1);
			disp[1]=joinXY(kernel, 1, 0);
			
			for (int i=0; i<sz*sz; i++)
				fp.setf(i, disp[0][i]+ disp[1][i]);
			 
			new ImagePlus("kernel",fp).show();
			if (!sep) {
				
				FloatProcessor fp2=new FloatProcessor(sz,sz, kernel2);
				new ImagePlus("kernel 2",fp2).show();
			}
		}
		long time=-System.nanoTime();	
		Convolver con=new Convolver();
		if (sep) {
			FloatProcessor ipx=(FloatProcessor)ip.duplicate();
			con.convolveFloat1D( ipx, kern_diff, sz, 1); // x direction
			con.convolveFloat1D( ipx, kernx, 1, sz); // y direction
			con.convolveFloat1D( (FloatProcessor)ip, kernx, sz, 1); // x direction
			con.convolveFloat1D( (FloatProcessor)ip, kern_diff, 1, sz); // y direction
			ip.copyBits(ipx, 0, 0, Blitter.ADD);
		} else {
			con.convolveFloat(ip, kernel2, sz, sz);
		}
		double sigma2=(sz-1)/6.0;
		sigma2*=sigma2;
		ip.multiply(sigma2);
		time+=System.nanoTime();
		time/=1000.0f;
		//System.out.println("elapsed time: " + time +" us");
		//ip.resetMinAndMax();
	}

	public float[] getKernel(int i) {
		return kernel[i];
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.pfr = pfr;
		int r = (sz-1)/2;
		GenericDialog gd=new GenericDialog("Mex. Hat " + version);
		gd.addNumericField("Radius", r, 1);
		gd.addCheckbox("Show kernel", debug);
		gd.addCheckbox("Separable", sep);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;
		if (!IJ.isMacro())
			staticSZ = sz;
		return IJ.setupDialog(imp, flags);
	}

	// Called after modifications to the dialog. Returns true if valid input.
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		int r = (int)(gd.getNextNumber());
		debug = gd.getNextBoolean();
		sep = gd.getNextBoolean();
		sz = 2*r+1;
		if (gd.wasCanceled())
			return false;
		return r>0;
	}

	/**
	 * @param r
	 */
	public float[] computeKernel2D(int r) {
		sz=2*r+1;
		final double sigma2=2*((double)r/3.0+1/6)*((double)r/3.0 +1/6.0);
		float[] kernel=new float[sz*sz];
		final double PIs=4/Math.sqrt(Math.PI*sigma2)/sigma2/sigma2;
		float sum=0;
		for (int u=-r; u<=r; u++) {
			for (int w=-r; w<=r; w++) {
				final double x2=u*u+w*w;
				final int idx=u+r + sz*(w+r);
				kernel[idx]=(float)((x2 -sigma2)*Math.exp(-x2/sigma2)*PIs);
				///System.out.print(kernel[c] +" ");		
				sum+=kernel[idx];

			}
		}
		sum=Math.abs(sum);
		if (sum<1e-5) sum=1;
		if (sum!=1) {
			for (int i=0; i<kernel.length; i++) {
				kernel[i]/=sum;
				//System.out.print(kernel[i] +" ");
			}
		}
		return kernel;
	}

	public float[] gauss1D(int r) {
		sz=2*r+1;
		//final double sigma2=2*((double)r/3.5 +1/7.0)*((double)r/3.5 +1/7.0);
		final double sigma2=((double)r/3.0+1/6)*((double)r/3.0 +1/6.0);
		float[] kernel=new float[sz];
		float sum=0;
		final double PIs=1/Math.sqrt(2*Math.PI*sigma2);
		for (int u=-r; u<=r; u++) {
			final double x2=u*u;
			final int idx=u+r ;
			kernel[idx]=(float)(Math.exp(-0.5*x2/sigma2)*PIs);

		}
		sum=Math.abs(sum);
		if (sum<1e-5) sum=1;
		if (sum!=1) {
			for (int i=0; i<kernel.length; i++) {
				kernel[i]/=sum;
				//System.out.print(kernel[i] +" ");
			}
		}
		return kernel;
	}

	public float[] diff2Gauss1D(int r) {
		sz=2*r+1;
		final double sigma2=((double)r/3.0+1/6)*((double)r/3.0 +1/6.0);
		float[] kernel=new float[sz];
		//((w^2-r^2)*%e^(-r^2/(2*w^2)))/(2^(3/2)*sqrt(%pi)*w^4*abs(w))
		float sum=0;
		final double PIs=1/Math.sqrt(2*Math.PI*sigma2);
		for (int u=-r; u<=r; u++) {
			final double x2=u*u;
			final int idx=u+r ;
			kernel[idx]=(float)((x2-sigma2)*Math.exp(-0.5*x2/sigma2)*PIs);

		}
		sum=Math.abs(sum);
		if (sum<1e-5) sum=1;
		if (sum!=1) {
			for (int i=0; i<kernel.length; i++) {
				kernel[i]/=sum;
				//System.out.print(kernel[i] +" ");
			}
		}
		
		return kernel;
	}

	private float[] joinXY(float[][] kernel, int a, int b) {
	 
		int sz=kernel[0].length;
		float[] jkernel=new float[sz*sz];
		
		for (int i=0; i<jkernel.length; i++) {
			jkernel[i]=1.0f;
		}
		
		for (int m=0; m<sz; m++) { // row
			for (int n=0; n<sz; n++) { // col
				final int idx=n + m *sz;
				jkernel[idx]*=kernel[a][n];
			}
		}
		
		for (int m=0; m<sz; m++) { // row
			for (int n=0; n<sz; n++) { // col
				final int idx=n + m *sz;
				jkernel[idx]*=kernel[b][m];
			}
		}
		return jkernel;
		
	}
	@Override
	public void setNPasses (int nPasses) {
		this.nPasses = nPasses;
	}

}
