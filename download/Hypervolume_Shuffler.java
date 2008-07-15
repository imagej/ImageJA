import java.io.*;
import java.awt.image.*;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;
import ij.measure.*;

/**
		Does various reconstruction operations on a hypervolume
		(a series of 3d volumes) in a ImageJ stack.
*/
public class Hypervolume_Shuffler implements PlugIn
{
		static int 			choice = 0;
		protected Calibration	c;
		static int			depth = 1;

		public void run(String arg)
		{
				ImagePlus imp = WindowManager.getCurrentImage();
				if (imp instanceof ImagePlus && imp.getStackSize() > 1)
				{
					ImageStack is = imp.getStack();
					c = imp.getCalibration();
					GenericDialog gd = new GenericDialog("Hypervolume shuffler ("
							+is.getWidth()+"x"+is.getHeight()+"x"+is.getSize()+")", ij.IJ.getInstance());
					String [] sc = {
						"shuffle  (xytz -> xyzt)", 	// 0
						"unshuffle (xyzt -> xytz)",	// 1
						"pad (v -> ppvvvpp)",      	// 2
						"inflate",                 	// 3
						"split (xyzt->xyz/xyz/...)",// 4
						"split (xytz->xyz/xyz/...)", // 5
						"split (xyzt->xyt/xyt/...)" // 6
			};
					gd.addChoice("Select operation", sc, sc[choice]);
					gd.showDialog();
					if (gd.wasCanceled())
						return;
					choice = gd.getNextChoiceIndex();
					switch (choice)
					{
						case 0: shuffle(imp); break;
						case 1: shufflerev(imp); break;
						case 2: pad(imp); break;
						case 3: inflate(imp); break;
						case 4: split(imp); break;
						case 5: splitxytz(imp); break;
						case 6: split2(imp); break;					}
				}
		}
		private void shuffle(ImagePlus imp)
		/*
				Change the order from xytz to xyzt.
		*/
		{
						ImageStack is = imp.getStack();
						GenericDialog gd = new GenericDialog("Shuffle hypervolume stack ("
							+is.getWidth()+"x"+is.getHeight()+"x"+is.getSize()+")", ij.IJ.getInstance());
						gd.addNumericField("volume depth:", depth, 0);
						gd.showDialog();
						if (gd.wasCanceled())
							return;
						depth = (int) gd.getNextNumber();
						// calculate number of volumes in hypervolume.
						int length = is.getSize() / depth;
						ij.IJ.write("hypervolume will be "+is.getWidth()+"x"+
								is.getHeight()+"x"+depth+"x"+length);
						ImageStack isn = new ImageStack(is.getWidth(), is.getHeight());
						for (int t = 0; t < length; t++)
								for (int z = 0; z < depth; z++)
										isn.addSlice(""+ (z * length + t + 1),
												is.getProcessor(z * length + t + 1));
						ImagePlus impn = new ImagePlus("Shuffled "+imp.getTitle(), isn);
						impn.setCalibration(c);
						impn.show();
		}
		private void shufflerev(ImagePlus imp)
		/*
				Change the order from xyzt to xytz.
		*/
		{
						ImageStack is = imp.getStack();
						GenericDialog gd = new GenericDialog("Unshuffle hypervolume stack ("
							+is.getWidth()+"x"+is.getHeight()+"x"+is.getSize()+")", ij.IJ.getInstance());
						gd.addNumericField("volume depth:", depth, 0);
						gd.showDialog();
						if (gd.wasCanceled())
							return;
						depth = (int) gd.getNextNumber();
						// calculate number of volumes in hypervolume.
						int length = is.getSize() / depth;
						ij.IJ.write("hypervolume will be "+is.getWidth()+"x"+
								is.getHeight()+"x"+depth+"x"+length);
						ImageStack isn = new ImageStack(is.getWidth(), is.getHeight());
						for (int z = 0; z < depth; z++)
								for (int t = 0; t < length; t++)
										isn.addSlice(""+ (t * depth + z + 1),
												is.getProcessor(t * depth + z + 1));
						ImagePlus impn = new ImagePlus("Unshuffled "+imp.getTitle(), isn);
						impn.setCalibration(c);
						impn.show();
		}
		private void pad(ImagePlus imp)
		/*
				Pads front and back of volume with empty slices.
				Also triples each slice.
		*/
		{
						ImageStack is = imp.getStack();
						GenericDialog gd = new GenericDialog("Pad hypervolume stack ("
							+is.getWidth()+"x"+is.getHeight()+"x"+is.getSize()+")", ij.IJ.getInstance());
						gd.addNumericField("original volume depth:", depth, 0);
						gd.addNumericField("padding (volumes):", 2, 0);
						gd.showDialog();
						if (gd.wasCanceled())
							return;
						depth = (int) gd.getNextNumber();
						int padding = (int) gd.getNextNumber();
						// calculate number of volumes in hypervolume.
						int length = is.getSize() / depth;
						ij.IJ.write("padded hypervolume "+is.getWidth()+"x"+
								is.getHeight()+"x"+(depth*3+padding*2)+"x"+length);
						ImageStack isn = new ImageStack(is.getWidth(), is.getHeight());
						// create an empty image.
						ImageProcessor ipblank;
						if (is.getProcessor(1).getPixels() instanceof byte[])
								ipblank = new ByteProcessor(is.getWidth(), is.getHeight());
						else if (is.getProcessor(1).getPixels() instanceof short[])
								ipblank = new ShortProcessor(is.getWidth(), is.getHeight(), false);
						else if (is.getProcessor(1).getPixels() instanceof int[])
								ipblank = new ColorProcessor(is.getWidth(), is.getHeight());
						else
								ipblank = new FloatProcessor(is.getWidth(), is.getHeight());
						for (int t = 0; t < length; t++)
						{
								// Put in empty slices in front.
								for (int i = 0; i < padding; i++)
								{
										isn.addSlice("", ipblank);
								}
								for (int z = 0; z < depth; z++)
								{
										isn.addSlice("", is.getProcessor(z + t * depth + 1));
										isn.addSlice("", is.getProcessor(z + t * depth + 1));
										isn.addSlice("", is.getProcessor(z + t * depth + 1));
								}
								// Put in two empty slices in back.
								for (int i = 0; i < padding; i++)
								{
										isn.addSlice("", ipblank);
								}
                        }
						ImagePlus impn = new ImagePlus("Padded "+imp.getTitle(), isn);
						impn.setCalibration(c);
						impn.show();
		}
		private void inflate(ImagePlus imp)
		{
						ImageStack is = imp.getStack();
						GenericDialog gd = new GenericDialog("Inflate hypervolume stack ("
							+is.getWidth()+"x"+is.getHeight()+"x"+is.getSize()+")", ij.IJ.getInstance());
						gd.addNumericField("original volume depth:", depth, 0);
						gd.addNumericField("inflation to (volumes):", 5, 0);
						gd.showDialog();
						if (gd.wasCanceled())
							return;
						depth = (int) gd.getNextNumber();
						int inflation = (int) gd.getNextNumber();
						// calculate number of volumes in hypervolume.
						int length = is.getSize() / depth;
						ij.IJ.write("inflated hypervolume "+is.getWidth()+"x"+
								is.getHeight()+"x"+(inflation)+"x"+length);
						ImageStack isn = new ImageStack(is.getWidth(), is.getHeight());
						for (int t = 0; t < length; t++)
						{
								int z = depth / 2;
								{
										for (int j = 0; j < inflation; j++)
										{
												isn.addSlice("", is.getProcessor(z + t * depth + 1));
										}
								}
                        }
						ImagePlus impn = new ImagePlus("Inflated "+imp.getTitle(), isn);
						impn.setCalibration(c);
						impn.show();
		}

		private void split(ImagePlus imp)
		// Split a 4d volume ordered as xyzt into separate 3d volume stacks.
		{
						ImageStack is = imp.getStack();
						GenericDialog gd = new GenericDialog("Split hypervolume stack ("
							+is.getWidth()+"x"+is.getHeight()+"x"+is.getSize()+")", ij.IJ.getInstance());
						gd.addNumericField("depth:", depth, 0);
						gd.showDialog();
						if (gd.wasCanceled())
							return;
						depth = (int) gd.getNextNumber();
						// calculate number of volumes in hypervolume.
						int length = is.getSize() / depth;
						ij.IJ.write("creating "+length+" xyz volume stacks");
						for (int t = 0; t < length; t++)
						{
								ImageStack isn = new ImageStack(is.getWidth(), is.getHeight());
								for (int z = 0; z < depth; z++)
								{
										isn.addSlice("", is.getProcessor(z + t * depth + 1));
								}
								ImagePlus impn = new ImagePlus("v["+t+"]"+imp.getTitle(), isn);
								impn.setCalibration(c);
								impn.show();
						}
		}

		private void splitxytz(ImagePlus imp)
		// Split a 4d volume ordered as xytz into separate 3d volume stacks.
		{
						ImageStack is = imp.getStack();
						GenericDialog gd = new GenericDialog("Split hypervolume stack ("
							+is.getWidth()+"x"+is.getHeight()+"x"+is.getSize()+")", ij.IJ.getInstance());
						gd.addNumericField("depth:", depth, 0);
						gd.showDialog();
						if (gd.wasCanceled())
							return;
						depth = (int) gd.getNextNumber();
						// calculate number of volumes in hypervolume.
						int length = is.getSize() / depth;
						ij.IJ.write("creating "+length+" xyz volume stacks");
						for (int t = 0; t < length; t++)
						{
								ImageStack isn = new ImageStack(is.getWidth(), is.getHeight());
								for (int z = 0; z < depth; z++)
								{
										isn.addSlice(""+ (z * length + t + 1),
												is.getProcessor(z * length + t + 1));
								}
								ImagePlus impn = new ImagePlus("v["+t+"]"+imp.getTitle(), isn);
								impn.setCalibration(c);
								impn.show();
						}
		}

		private void split2(ImagePlus imp) {
		// Split a 4d volume ordered as xyzt into separate xyt time series stacks.
			ImageStack is = imp.getStack();
			GenericDialog gd = new GenericDialog("Split hypervolume stack ("
				+is.getWidth()+"x"+is.getHeight()+"x"+is.getSize()+")");
			gd.addNumericField("depth:", depth, 0);
			gd.showDialog();
			if (gd.wasCanceled())
				return;
			depth = (int) gd.getNextNumber();																			
			// calculate number of volumes in hypervolume.
			int length = is.getSize() / depth;
			ij.IJ.write("creating "+depth+" xyt time series stacks");
			for (int z = 0; z <depth;z++) {
				ImageStack isn = new ImageStack(is.getWidth(), is.getHeight());
				for (int t = 0; t < length; t++)
					isn.addSlice("", is.getProcessor(z + t * depth + 1));
				ImagePlus impn = new ImagePlus("v["+z+"]"+imp.getTitle(), isn);
				impn.setCalibration(c);
				impn.show();
			}
		}

		private void images(ImagePlus imp)
		{
						ImageStack is = imp.getStack();
						for (int t = 0; t < is.getSize(); t++)
						{
								ImagePlus impn = new ImagePlus(imp.getTitle()+":"+t, is.getProcessor(t+1));
								impn.show();
						}
		}
}


