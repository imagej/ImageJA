import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
* @version 1.0	8 Nov 2012
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
	private String version="1.0";
	private int nPasses=1;
	private int pass;
	private static int staticSZ = 5;
	private int sz = staticSZ;
	private float[] kernel=null;
	private Convolver con;
	private boolean debug = IJ.debugMode;
	private Checkbox debugCheckbox;
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		return  flags;
	}

	@Override
	public void run(ImageProcessor ip) {
		pass++;
		int r = (sz-1)/2;
		computeKernel(r);
		if (pass==1) {
			con = new Convolver();
			con.setNPasses(nPasses);
		}
		con.convolveFloat(ip, kernel, sz, sz);
		if (debug) {
			FloatProcessor fp=new FloatProcessor(sz,sz);
			fp.setPixels(kernel);
			new ImagePlus("kernel",fp).show();
			debug = false;
			debugCheckbox.setState(false);
		}
	}
	
	public float[] getKernel() {
		return kernel;
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.pfr = pfr;
		int r = (sz-1)/2;
		GenericDialog gd=new GenericDialog("Input");
		gd.addNumericField("Radius", r, 1);
		gd.addCheckbox("Show kernel", debug);
		Vector v = gd.getCheckboxes();
		debugCheckbox = (Checkbox)v.firstElement();
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
		sz = 2*r+1;
		if (gd.wasCanceled() || r==0)
			return false;
		pass = 0;
		return true;
    }

	/**
	 * @param r
	 */
	public void computeKernel(int r) {
		sz=2*r+1;
		//final double sigma2=2*((double)r/3.5 +1/7.0)*((double)r/3.5 +1/7.0);
		final double sigma2=2*((double)r/3.0+1/6)*((double)r/3.0 +1/6.0);
		kernel=new float[sz*sz];

		float sum=0;
		for (int u=-r; u<=r; u++) {
			for (int w=-r; w<=r; w++) {
				final double x2=u*u+w*w;
				final int idx=u+r + sz*(w+r);
				kernel[idx]=(float)((x2 -sigma2)*Math.exp(-x2/sigma2));
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
	}

	@Override
	public void setNPasses (int nPasses) {
		this.nPasses = nPasses;
	}
	
}
