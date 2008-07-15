import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;


/**
 * 
 * @author Mikhail Umorin <mikeumo@sbcglobal.net>
 * 
 * 09.10.02 (i.e. October 9th, 2002) modified 10.07.03 -- added generation of
 * height map; 4.08.03 -- changed interface for k_size and yes/no height map;
 * 7.08.03 -- Plugin can be initialized with arg string to setup method and then
 * runs non-interactively; sometime in 2004 -- added color support; 
 * 
 * Inspired by the mentioning of the capability of "flattening" a set of images
 * of different focal planes in ImagePro 4.0. The author, however came up with
 * the algorithm idea (and implementation) totally on his own without any
 * reference to any similar algorithms, printed, digital, or otherwise. The
 * author is open to any suggestions regarding algorithm(s), implementation,
 * and/or features of the program. The program may be distributed and modified
 * freely under GPL with the reference of the original source. No implicit or
 * explicit warranty or suitabiluty for a particular purpose is given. See license.txt 
 * for detailed conditions on use, modification, and distribution of the source and binary code
 * of the plugin.
 * 
 * Contains a very simple algorythm of patching a *focused * image from a stack
 * of images corresponding to different focal planes. It is very important that
 * images in the stack are of the same brightness; otherwise pasting together
 * pieces of different images will create artificial edges.
 * 
 * The principle: 1) find edges for each image in a stack by running a Sobel
 * filter (after noise-reducing 3x3 median filter); 2) create a "map" of how far
 * the influence of edges extends; i.e. how far from a focused edge we still
 * think that the image is focused by taking the maximum value in the
 * neighborhood of the specified size; 3) for every pixel (x, y) based on the
 * choice of the maximum "edge" value among different slices in the "map" stack
 * copy the pixel value from the appropriate original image to the new image.
 * 
 * Program works on 8-bit and 16-bit grayscale stacks, and accepts rectangular
 * ROIs; if no ROI is given, plugin works on the whole image; For RGB stack the
 * plugin decomposes the stack into three 8-bit R, G, and B component stacks and
 * applies the above described algorithm to each one. The resulting *pasted" RGB
 * image is created from the 8-bit pasted images from each component stack. The
 * height map is an average of individual height maps for RGB case.
 * 
 * If the option "R, G, and B come from same objects/structures" is set, the plugin 
 * determines focussed areas for green colour component only and pastes R and B components
 * from the corresponding green-focussed areas. 
 * 
 * plugin converts stacks to 32-bit float to preserve precision before any
 * manipulation is performed. The size in pixels (odd integer > 1) of the
 * Maximum square filter is requested; trial and error would be the fastest way
 * to optimize the result. The final image is written to "Focused"+<original
 * stack title> window, but not saved on the disk (the user can do that him/her
 * self). Optionally, the plugin generates a "height map", i.e. an image of the
 * heights of focused parts of the image. The home-grown maximum filter
 * maxFilter has a square kernel and is MUCH faster then available in Process ->
 * Filters -> Maximum menu. The sacrifice in quality is believed negligible for
 * this kind of application even though the squareness makes it anisotropic (?)
 * 
 * For a short but good reference on image analysis see
 * http://www.cee.hw.ac.uk/hipr/html/hipr_top.html
 */
public class Stack_Focuser_ implements PlugInFilter
{
	/**
	 * ImageStacl object of the original image
	 */
	private ImageStack i_stack;
	
	
	private static final int BYTE=0;
	private static final int SHORT=1; 
	private static final int FLOAT=2;
	private static final int RGB=3;
	
	/**
	 * Focusing kernel size, implies square kernel
	 */
	protected int k_size;
	
	/**
	 * Index of the image type, {@link #BYTE}, {@link #SHORT}, {@link #FLOAT}, {@link #RGB}
	 */
	protected int type;
	
	/**
	 * Width of the original image
	 */
	protected int o_width;
	
	/**
	 * Height of the original image
	 */
	protected int o_height; 
	
	
	/**
	 * Number of slices in the original stack
	 */
	protected int n_slices;
	
	/**
	 * Total number of pixels in the original image,  = {@link #o_height} x {@link #o_width}
	 */
	protected int o_dim;
	
	/**
	 * Rectangle object for image's ROI where to perform focusing. If ROI is not set,
	 * focusing is performed on the whole image.
	 */
	private Rectangle r;
	
	/**
	 * Width of ROI
	 */
	protected int n_width;
	
	/**
	 * Height of ROI
	 */
	protected int n_height;
	
	/**
	 * Total number of pixels in ROI, = {@link #n_height } x {@link #n_width}
	 */
	protected int n_dim;

	/**
	 * Filename of the original image
	 */
	private String o_title;
	
	
	private boolean create_map = false;
	private boolean onefocus = false;
	private GenericDialog input_dialog;
	private boolean interact = true;
	private ImageProcessor focused_ip = null, height_ip = null;
	private ImageStack focused_stack, height_stack;
	private static final int redMask = 0xff0000, greenMask = 0x00ff00, blueMask = 0x0000ff;
	private static final int redShift = 16, greenShift = 8, blueShift = 0;

	/**
	 * Setup routine. Checks for any initialization parameters.
	 * @param arg Initialization string. either empty or should contain parameter values in
	 * the form "name=value" space-separated list. Avaliable parameters are "ksize" -- kernel size, 
	 * positive odd integer; "hmap" -- whether to create a height map, "true/false"; "rgbone" --
	 * whether R, G, and B come from same objects/structures, "true/false".
	 * @param imp image stack to work on.
	 */
	public int setup(String arg, ImagePlus imp) {
		k_size = 11;
		if (arg.equalsIgnoreCase("about")) {
			showAbout();
			return DONE;
		}
		// check if the arg string has parameters to set
		if (arg.indexOf("ksize=") >= 0) {
			interact = false;
			int pos = arg.indexOf("ksize=") + 6;
			if (pos != arg.length()) {
				if (arg.charAt(pos) != ' ') {
					String kss;
					int posn = arg.indexOf(' ', pos + 1);
					if (posn > 0) {
						kss = arg.substring(pos, posn);
					} else {
						kss = arg.substring(pos);
					}
					k_size = Integer.parseInt(kss);
				}
			}
		}
		if (arg.indexOf("hmap=") >= 0) {
			interact = false;
			int pos = arg.indexOf("hmap=") + 5;
			if (pos != arg.length()) {
				if (arg.charAt(pos) != ' ') {
					String hms;
					int posn = arg.indexOf(' ', pos + 1);
					if (posn > 0) {
						hms = arg.substring(pos, posn);
					} else {
						hms = arg.substring(pos);
					}
					create_map = hms.equalsIgnoreCase("true");
				}
			}
		}
		if (arg.indexOf("rgbone=") >= 0) {
			interact = false;
			int pos = arg.indexOf("rgbone=") + 7;
			if (pos != arg.length()) {
				if (arg.charAt(pos) != ' ') {
					String hms;
					int posn = arg.indexOf(' ', pos + 1);
					if (posn > 0) {
						hms = arg.substring(pos, posn);
					} else {
						hms = arg.substring(pos);
					}
						onefocus = hms.equalsIgnoreCase("true");
				}
			}
		}
		//
		if (imp == null) {
			IJ.noImage();
			return DONE;
		}
		//
		ImageProcessor ip_p = imp.getProcessor();
		o_title = imp.getTitle();
		int dot_i = o_title.indexOf(".");
		if (dot_i > 0)
			o_title = o_title.substring(0, dot_i);
		// determine the type of the image; getType() does not work for stacks
		if (ip_p instanceof ByteProcessor)
			type = BYTE;
		else if (ip_p instanceof ShortProcessor)
			type = SHORT;
		else if (ip_p instanceof FloatProcessor)
			type = FLOAT;
		else
			type = RGB;
		i_stack = imp.getStack();
		o_width = imp.getWidth();
		o_height = imp.getHeight();
		o_dim = o_width * o_height;
		n_slices = imp.getStackSize();
		// obtain ROI and if ROI not set set ROI to the whole image
		r = i_stack.getRoi();
		if ((r == null) || (r.width < 2) || (r.height < 2)) {
			r = new Rectangle(0, 0, o_width, o_height);
			i_stack.setRoi(r);
		}
		n_width = r.width;
		n_height = r.height;
		n_dim = n_width * n_height;
		return DOES_8G + DOES_16 + DOES_RGB + STACK_REQUIRED + NO_CHANGES
				+ NO_UNDO;
	}


	public void run(ImageProcessor ip)
	{
		// read options
		// TODO allow for different x and y kern_size later
                if (interact) {
                    input_dialog = new GenericDialog("Options");
                    input_dialog.addNumericField("Enter the n (>2) for n x n kernel:", k_size, 0);
                    input_dialog.addCheckbox("Generate height map", create_map);
                    input_dialog.addCheckbox("R, G, and B come from same objects/structures", onefocus);
                    input_dialog.showDialog();
                    if (input_dialog.wasCanceled()) return;
                    k_size = (int)input_dialog.getNextNumber();
                    create_map = input_dialog.getNextBoolean();
                    onefocus = input_dialog.getNextBoolean();
                    if ( input_dialog.invalidNumber() || k_size<3 ) {
                        IJ.error("Invalid number or " +k_size+" is incorrect! ");
                        return;
                    }
                }
		switch(type)
		{
			case BYTE: focused_ip = focusGreyStack(i_stack, BYTE); break;
			case SHORT: focused_ip = focusGreyStack(i_stack, SHORT); break;
			case FLOAT: focused_ip = focusGreyStack(i_stack, FLOAT); break;
			case RGB: 
				if (onefocus) {
					focusRGBStackOne(i_stack);
				} else {
					focusRGBStack(i_stack);
				}
				break;
			default: break;
		}
		// construct the title of the new window
		ImagePlus focused = null;
		ImagePlus height_map = null;
		String n_title = "Focused_"+o_title;
		focused = new ImagePlus(n_title, focused_ip);
		focused.show();
		focused.updateAndDraw();
		if (create_map) {
			String nm_title = "HeightMap_" + o_title;
			height_map = new ImagePlus(nm_title, height_ip);
			height_map.show();
			height_map.updateAndDraw();
		}
	}

	/**
	 * Focuses an RGB stack. All colors are focused independently.
	 * @param rgb_stack
	 */
	void focusRGBStack (ImageStack rgb_stack) {
		IJ.showStatus("Processing RGB stack");
		focused_stack = new ImageStack(n_width, n_height);
		height_stack = new ImageStack(n_width, n_height);
		// split RGB stack into R, G, and B components
		// and then run FocusGreyStack() on each independently
		
		
		// Red
		ImageStack colored_stack = extractColor(rgb_stack, redMask, redShift);
		IJ.showStatus("Extracted red color");
		focused_ip = focusGreyStack(colored_stack, BYTE);
		IJ.showStatus("Focused red color stack");
		focused_stack.addSlice("Red", focused_ip);
		height_stack.addSlice("Red", height_ip);
		colored_stack = null;
		// Green
		colored_stack = extractColor(rgb_stack, greenMask, greenShift);
		IJ.showStatus("Extracted green color");
		focused_ip = focusGreyStack(colored_stack, BYTE);
		IJ.showStatus("Focused green color stack");
		focused_stack.addSlice("Green", focused_ip);
		height_stack.addSlice("Green", height_ip);
		colored_stack = null;
		// Blue
		colored_stack = extractColor(rgb_stack, blueMask, blueShift);
		IJ.showStatus("Extracted blue color");
		focused_ip = focusGreyStack(colored_stack, BYTE);
		IJ.showStatus("Focused blue color stack");
		focused_stack.addSlice("Blue", focused_ip);
		height_stack.addSlice("Blue", height_ip);
		colored_stack = null;
		//
		ImagePlus fs_image = new ImagePlus("Focused stack", focused_stack);
		fs_image.show();
		fs_image.updateAndDraw();
		// ImageWindow win = fs_image.getWindow();
		IJ.run("RGB Color");
		focused_ip = new ColorProcessor(n_width, n_height);
		focused_ip.copyBits(WindowManager.getCurrentWindow().getImagePlus().getProcessor(), 
				0, 0, Blitter.COPY);
		IJ.run("Close");
		// ImagePlus hs_image = new ImagePlus("Focus height stack", height_stack);
		// hs_image.show();
		// hs_image.updateAndDraw();
		long[] sum = new long[n_dim];
		// int hs_size = height_stack.getSize();
		for (int i=1; i<=height_stack.getSize(); i++)
		{
			byte[] pixels = (byte[]) height_stack.getPixels(i);
			// add the value of each pixel an the corresponding position of the sum array
			for (int j=0; j<n_dim; j++)
			{
				sum[j]+=0xff & pixels[j];
			}
		}
		byte[] average = new byte[n_dim];
		// divide each entry by the number of slices
		for (int j=0; j<n_dim; j++)
		{
			average[j] = (byte) ((sum[j]/height_stack.getSize()) & 0xff);
		}
		height_ip = new ByteProcessor(n_width, n_height, average, null);
		sum = null;
		// height_ip.copyBits(hs_image.getProcessor(), 0, 0, Blitter.COPY);
	}
	
	/**
	 * Focuses an RGB stack when all three color come from the same objects/structures.
	 * Uses GREEN as the focusing color.
	 * @param rgb_stack
	 */
	void focusRGBStackOne (ImageStack rgb_stack) {
		IJ.showStatus("Processing RGB stack");
		focused_stack = new ImageStack(n_width, n_height);
		height_stack = new ImageStack(n_width, n_height);
		// split RGB stack into R, G, and B components
		// and then focus each of the colors with focusing stack set for green color.
		
		
		// Green
		ImageStack colored_stack = extractColor(rgb_stack, greenMask, greenShift);
		IJ.showStatus("Extracted green color");
		// create max stack
		ImageStack max_stack = makeMaxStack(colored_stack, BYTE);
		//paste the image
		ImageProcessor g_ip = pasteGreyImage(colored_stack, max_stack, BYTE);
		IJ.showStatus("Focused green color stack");
		// height_stack.addSlice("Green", height_ip);
		colored_stack = null;
		// Red
		colored_stack = extractColor(rgb_stack, redMask, redShift);
		IJ.showStatus("Extracted red color");
		//paste the image
		ImageProcessor r_ip = pasteGreyImage(colored_stack, max_stack, BYTE);
		IJ.showStatus("Focused red color stack");
		colored_stack = null;
		// Blue
		colored_stack = extractColor(rgb_stack, blueMask, blueShift);
		IJ.showStatus("Extracted blue color");
		//paste the image
		ImageProcessor b_ip = pasteGreyImage(colored_stack, max_stack, BYTE);
		IJ.showStatus("Focused blue color stack");

		focused_stack.addSlice("Red", r_ip);
		focused_stack.addSlice("Green", g_ip);
		focused_stack.addSlice("Blue", b_ip);
		colored_stack = null;
		//
		ImagePlus fs_image = new ImagePlus("Focused stack", focused_stack);
		fs_image.show();
		fs_image.updateAndDraw();
		// ImageWindow win = fs_image.getWindow();
		IJ.run("RGB Color");
		focused_ip = new ColorProcessor(n_width, n_height);
		focused_ip.copyBits(WindowManager.getCurrentWindow().getImagePlus().getProcessor(), 
				0, 0, Blitter.COPY);
		IJ.run("Close");
		// ImagePlus hs_image = new ImagePlus("Focus height stack", height_stack);
		// hs_image.show();
		// hs_image.updateAndDraw();
		// height_ip.copyBits(hs_image.getProcessor(), 0, 0, Blitter.COPY);
	}

	/**
	 * Focuses a grey-color stack, i.e. 8-bit integer, 16-bit integer, and 32-bit float.
	 * @param grey_stack
	 * @param stackType
	 */
	ImageProcessor focusGreyStack(ImageStack g_stack, int stackType)
	{
		ImageStack max_stack;

		// create max stack
		max_stack = makeMaxStack(g_stack, stackType);
		
		//paste the image
		return pasteGreyImage(g_stack, max_stack, stackType);
		// max_stack = null;

	}
	
	/**
	 * Create a stack for max in the neighbourhood.
	 * match input stack and the new one slice by slice.
	 * @param g_stack
	 * @param m_stack
	 * @param stackType
	 */
	private ImageStack makeMaxStack(ImageStack g_stack, int stackType){
		ImageProcessor i_ip, dfloat_ip;
		ImageProcessor m_ip;
		ImageStack m_stack = new ImageStack(n_width, n_height);
		float[] m_slice;
		IJ.showProgress(0.0f);
		IJ.showStatus("Converting...");
		for (int i=1; i<=n_slices; i++)
		{
			// Convert to float
			IJ.showStatus("Converting to float...");
			i_ip = g_stack.getProcessor(i);
			float[] dfloat_array = new float[n_dim];
			dfloat_array = convertGreyToFloat(i_ip, stackType);
			dfloat_ip = new FloatProcessor(n_width, n_height, dfloat_array, i_ip.getColorModel());
			
			// run median filter on the new one to get rid of some noise
			IJ.showStatus("Running median filter...");
			dfloat_ip.medianFilter();
			
			// Smooth image
			IJ.showStatus("Smoothing image....");
			dfloat_ip.smooth();
			
			// run Sobel edge detecting filter
			IJ.showStatus("Finding edges....");
			dfloat_ip.findEdges();
			
			// run Max filter
			m_slice = new float[n_dim];
			m_ip = new FloatProcessor(n_width, n_height, m_slice, null);
			//  a dialog with user input at the beginning of run specifies k_size.
			IJ.showStatus("Applying "+k_size+"x"+k_size+" filter...");
			maxFilter(dfloat_ip, m_ip, k_size);
			dfloat_ip = null;
			
			// and add to the new stack
			m_stack.addSlice(null, m_ip);
			IJ.showProgress(1.0*i/n_slices);
		}
		return m_stack;
	}
	
	/**
	 * By comparing max values for the same point in different slices we decide which
	 * original slice to use to paste into the new image at that location.
	 * 
	 * @param g_stack
	 * @param m_stack
	 * @param f_ip
	 * @param stackType
	 */
	private ImageProcessor pasteGreyImage(ImageStack g_stack, ImageStack m_stack, int stackType) {
		ImageProcessor f_ip = null;
		byte[] orig_pixels8 = null;
		short[] orig_pixels16 = null;
		float[] orig_pixels32 = null;
		byte[] dest_pixels8 = null;
		short[] dest_pixels16 = null;
		float[] dest_pixels32 = null;
		//
		int scale = 0;
		// prepare height map
		// if (create_map)
		{
			height_ip = new ByteProcessor(n_width, n_height);
			scale = 255/n_slices;
		}
		//
		switch (stackType)
		{
			case BYTE:
				dest_pixels8 = new byte[n_dim];
				break;
			case SHORT:
				dest_pixels16 = new short[n_dim];
				break;
			case FLOAT:
				dest_pixels32 = new float[n_dim];
				break;
			default:
				break;
		}
		int offset, i;
		int copy_i, copy_x, copy_y;
		int pix;
		IJ.showStatus("Pasting the new image...");
		IJ.showProgress(0.0f);
		int max_slice = 1;
		float[] curr_pixels;
		for (int y=0; y<n_height; y++)
		{
			offset = n_width*y;
			for (int x=0; x<n_width; x++)
			{
				i = offset + x;
				float max_e = 0.0f;
				// find the slice to copy from
				for (int z=1; z<=m_stack.getSize(); z++)
				{
					curr_pixels = (float[]) m_stack.getPixels(z);
					if (curr_pixels[i]>max_e)
					{
						max_e = curr_pixels[i];
						max_slice = z;
					}
				}
				copy_x = r.x+x;
				copy_y = r.y+y;
				copy_i = copy_x+copy_y*o_width;
				// if (create_map)
				{
					height_ip.putPixel(x, y, max_slice*scale);}
				switch (stackType)
				{
					case BYTE:
						orig_pixels8 = (byte[]) g_stack.getPixels(max_slice);
						dest_pixels8[i] = orig_pixels8[copy_i];
						break;
					case SHORT:
						orig_pixels16 = (short[]) g_stack.getPixels(max_slice);
						dest_pixels16[i] = orig_pixels16[copy_i];
						break;
					case FLOAT:
						orig_pixels32 = (float[]) g_stack.getPixels(max_slice);
						dest_pixels32[i] = orig_pixels32[copy_i];
						break;
					default:
						break;
				}
				IJ.showStatus("Pasting the new image...");
				IJ.showProgress(1.0*i/n_dim);
			}
		}
		switch (stackType)
		{
			case BYTE:
				f_ip = new ByteProcessor(n_width, n_height, dest_pixels8, null);
				break;
			case SHORT:
				f_ip = new ShortProcessor(n_width, n_height, dest_pixels16, null);
				break;
			case FLOAT:
				f_ip = new FloatProcessor(n_width, n_height, dest_pixels32, null);
				break;
			default:
				break;
		}
		return f_ip;
	}
	
	/**
	 * Copy from input stack to the newer one slice by slice and
	 * convert to floating point grayscale to avoid precision loss later
	 *
	 * @param ip
	 * @param float_array
	 * @param stackType
	 */
	private float[] convertGreyToFloat(ImageProcessor ip, int stackType) {
		int o_offset, o_i, ii;
		// int dim = ip.getHeight()*ip.getWidth();
		float[] float_array = new float[n_dim];
		switch (stackType)
		{
			case BYTE:
				ii = 0;
				byte[] bi_pixels = (byte[])ip.getPixels();
				for (int y=r.y; y<(r.y+r.height); y++)
				{
					o_offset = y*o_width;
					for(int x=r.x; x<(r.x+r.width); x++)
					{
						o_i = o_offset+x;
						float_array[ii] = bi_pixels[o_i]&0xff;
						ii++;
					}
				}
				break;
			case SHORT:
				ii = 0;
				short[] si_pixels = (short[])ip.getPixels();
				for (int y=r.y; y<(r.y+r.height); y++)
				{
					o_offset = y*o_width;
					for(int x=r.x; x<(r.x+r.width); x++)
					{
						o_i = o_offset+x;
						float_array[ii] = si_pixels[o_i]&0xffff;
						ii++;
					}
				}
				break;
			case FLOAT:
				ii = 0;
				float[] fi_pixels = (float[])ip.getPixels();
				for (int y=r.y; y<(r.y+r.height); y++)
				{
					o_offset = y*o_width;
					for(int x=r.x; x<(r.x+r.width); x++)
					{
						o_i = o_offset+x;
						float_array[ii] = fi_pixels[o_i];
						ii++;
					}
				}
				break;
			default:
				break;
		}// switch
		return float_array;
	}
	
	/**
	 * 
	 * @param source_ip Source ImageProcessor
	 * @param dest_ip Destination ImageProcessor
	 * @param kern_size odd number for kernel size
	 */
	private void maxFilter(ImageProcessor source_ip, ImageProcessor dest_ip, int kern_size)
	{
			// float[] dest_pixels = (float[]) dest_ip.getPixels();
			float[] dest_pixels = (float[]) dest_ip.getPixels();
			int width = source_ip.getWidth();
			int height = source_ip.getHeight();
			int offset, i;
			for (int y=0; y<height; y++)
			{
				offset = width*y;
				for (int x=0; x<width; x++)
				{
					i = offset + x;
					dest_pixels[i] = findMaxInNeigh(source_ip, x, y, kern_size, kern_size);
				}
			}
	}

	/**
	 * Returns maximum pixel value (for grey scale only) in the neighbourhood centered 
	 * at (center_x, center_y) and of height and with = size_y x size_x.
	 * size>1; odd and even do not matter, i.e. size=2 is same as size=3
	 * @param ip_
	 * @param center_x
	 * @param center_y
	 * @param size_x
	 * @param size_y
	 * @return
	 */
	private float findMaxInNeigh(ImageProcessor ip_, int center_x, int center_y, int size_x, int size_y)
	{
		float maxVal = 0.0f;
		int width_ = ip_.getWidth();
		int height_ = ip_.getHeight();
		float[] pixels_ = (float[]) ip_.getPixels();
		int half_x= size_x / 2;
		int half_y= size_y / 2; 
		int start_x = center_x-half_x;
		int start_y = center_y-half_y;
		if (start_x<0) {start_x = 0;}
		if (start_y<0) {start_y = 0;}
		int end_x = center_x+half_x;
		int end_y = center_y+half_y;
		if (end_x>width_) {end_x = width_;}
		if (end_y>height_) {end_y = height_;}
		int offset_, i_;
		for (int y=start_y; y<end_y; y++)
		{
			offset_ = width_*y;
			for (int x=start_x; x<end_x; x++)
			{
				i_ = offset_ + x;
				if (pixels_[i_]>maxVal) {maxVal = pixels_[i_];}
			}
		}
		return maxVal;
	}

	private ImageStack extractColor(ImageStack rgbStack, int mask, int shift)
	{
		ImageProcessor sliceProcessor;
		ImageStack g_stack = new ImageStack(n_width, n_height);
		int offset, pos;
		int w = rgbStack.getWidth();
		int h = rgbStack.getHeight();
		// gretStack = new ImageStack(w, h);
		// match input stack and the new one slice by slice
		for (int i=1; i<=rgbStack.getSize(); i++)
		{
			sliceProcessor = rgbStack.getProcessor(i);
			int[] colorPixels = (int[]) sliceProcessor.getPixels();
			byte[] greyPixels = new byte[w*h];
			for (int y=0; y<h; y++)
			{
				offset = y*w;
				for(int x=0; x<w; x++)
				{
					pos = offset+x;
					greyPixels[pos] = (byte)((colorPixels[pos]&mask)>>shift);
				}
			}
			g_stack.addSlice("", greyPixels);
		}
		return g_stack;
	}

	public void showAbout()
	{
		IJ.showMessage("About Stack Focuser...",
					"Patches a *focused* image\n"+
					" from a stack of images \n"+
					"corresponding to different focal planes\n"+
					"\n Mikhail Umorin <mikeumo@sbcglobal.net>");
	}
}
