import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/* 	Author: Julian Cooper
	Contact: Julian.Cooper [at] uhb.nhs.uk
	First version: 2009/02/22
	Licence: Public Domain	*/

/* This plugin creates a montage that best fits a desired grid
*/
public class RC_Montage implements PlugIn {

	int first, last, rows, cols, border;
	double scale;
	boolean labels;

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		ImagePlus imp2 = imp.createImagePlus();
		ImageStack stack = imp.getStack();
		ImageStack stack2 = imp.createEmptyStack();
		if (stack.getSize()==1)
			{IJ.error("Stack Required"); return;}
		if (!showDialog(stack))
			return;
		if (last>stack.getSize())
			last = stack.getSize();
		double n = last - first;
		double rc = rows*cols;
		if (rc < 2) {
			cols = 2;
			rc = 2;
		}
		double inc = n/(rc-1);

		if (rc > n+1){
			inc = 1;
			rc = n+1;
		}

		int size = (int) rc;
		int slice;
		for(int i=0; i<size; i++) {
			slice = (int) Math.rint(first + i * inc);
			stack2.addSlice(stack.getSliceLabel(slice), stack.getProcessor(slice));
		}
		imp2.setStack(null, stack2);

		MontageMaker mont = new MontageMaker();
		mont.makeMontage(imp2, cols, rows, scale, 1, size, 1, border, labels);
	}

	public boolean showDialog(ImageStack stack) {
		int n = stack.getSize();
		GenericDialog gd = new GenericDialog("RC Montage");
		gd.addNumericField("First Slice:", 1, 0);
		gd.addNumericField("Last Slice:", n, 0);
		gd.addNumericField("Rows:", 5, 0);
		gd.addNumericField("Columns:", 5, 0);
		gd.addNumericField("Scale:", 1.0, 2);
		gd.addNumericField("Border width:", 0, 0);
		gd.addCheckbox("Add slice labels?", labels);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		first = (int) gd.getNextNumber();
		last = (int) gd.getNextNumber();
		rows = (int) gd.getNextNumber();
		cols = (int) gd.getNextNumber();
		scale = (double) gd.getNextNumber();
		border = (int) gd.getNextNumber();
		labels = gd.getNextBoolean();
		return true;
	}



}
