//--v1.0: the born of the program
//--v1.1: Put the shift procedure to a separated function

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class X_Shifter implements PlugInFilter {
	ImagePlus imp;
	double time;
	static int offset1 = 0;
	boolean canceled;
	int frame, first, last;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if (imp!=null) {
			first = 1;
			last = imp.getStackSize();
		}
		return IJ.setupDialog(imp, DOES_ALL+SUPPORTS_MASKING);
	}

	public void run(ImageProcessor ip) {
		frame++;
		if (frame==1) showDialog(ip);
		if (canceled || frame<first || frame>last) return;
		shift_offset(ip);
		if (frame==last) imp.updateAndDraw();
	}
	

	void showDialog(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("X Shifter");
		gd.addNumericField("Shift Pixel Amount:", offset1, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			{canceled = true; return;}
		offset1 = (int) gd.getNextNumber();
	}

	void shift_offset(ImageProcessor ip){
		int ii=0;
		Rectangle r = ip.getRoi();
		int[] temp,temp2;

		temp = new int[(r.x + r.width)*(r.y +r.height)+ Math.abs(offset1)+100];
		temp2 = new int[(r.x + r.width)*(r.y +r.height)+ Math.abs(offset1)];

		for (int y=r.y; y<(r.y+r.height); y++)
			{if ((y%2)==0)
				{for (int x=r.x; x<(r.x+r.width)-1; x++)
					{temp[ii]=ip.getPixel(x,y);
					ii++;}
				}
			else
				{for (int x=(r.x+r.width)-1; x>=(r.x); x--)
					{temp[ii]=ip.getPixel(x,y);
					ii++;}
				}
			} 

		/*-----------Now starts to shift------------*/

	for (int jj=0;jj<ii;jj++)
		{if (offset1>=0)
			{if (jj<ii-offset1)
				{ temp2[jj]=temp[jj+offset1];}
			else
				{temp2[jj]=temp[jj-ii+offset1];}
			} 
		else
			{if (jj>Math.abs(offset1) )
				{ temp2[jj]=temp[jj+offset1];}
			else
				{temp2[jj]=temp[ii+offset1+jj];}
			} 
		}
		//---------------Finished Shifting, start writing-------------
		ii=0;
		for (int y=r.y; y<(r.y+r.height); y++)
			{if ((y%2)==0)
				{for (int x=r.x; x<(r.x+r.width)-1; x++)
					{ip.putPixel(x,y,temp2[ii]);//IJ.write(IJ.d2s(temp[ii]));
					ii++;}
				}
			else
				{for (int x=(r.x+r.width)-1; x>=(r.x); x--)
					{ip.putPixel(x,y,temp2[ii]);// IJ.beep();
					ii++;}
				}
			} 
	
	}

}
