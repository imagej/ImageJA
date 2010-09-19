import java.util.ArrayList;
import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.gui.*;

public class GHT_ implements PlugIn {
	
	private byte edge_colour = 0;		//search-/edge-colour (0= black); edit for other colours
	private int xref = 0, yref = 0;		// middlepoint for RTable-Calculation
	private ArrayList<Integer> rtable = new ArrayList<Integer>();		// RTable part I (vectors)
	private ArrayList<Float> params = new ArrayList<Float>();		// RTable part II (scaling factor and delta angle)
	private Thread[] threads;
	private byte [] pixels;		// temp pic-array
	private int [] delta;		// array for one-dimensional rtable pixel-offsets for a searhc-picture
	private ArrayList<Integer> hits = new ArrayList<Integer>();		// dynamic array for hits
	private int wref = 0, href = 0;		// width and height of reference-pic
	private int wsch = 0, hsch = 0;		// width and height of search-pics
	
	// Input variables
	private int rtable_simple_size = 0, rtable_scaled_size = 0, rtable_full_size = 0;		// size of simple and scaled RTable
	private int pixel_offset = 0;		// only use every pixel_offset ref-pixel for RTable
	private float treshold = (float)0;		// treshold-factor for object-finding in HoughSpace
	private float treshold_hits = (float)0;			// treshold in hits per pixel
	private float size_min = 0, size_max = 0, size_delta = 0;		// Used for object-scaling
	private float angle_min = 0, angle_max = 0, angle_delta = 0;		// Used for object-rotation
	private float acc_smooth_sum = 0;		// summand for smooth accumulator updating
	private int non_max_suppr_size = 1;		// 1 = non-max-suppression off; > 1 && odd size for non-max-suppression-square
	private boolean hspace_output = false;		// output of houghspace only (without objects)
	private boolean show_hits_in_log = true;		// output of hits in ImageJ-Log
	private boolean disable_pic_out = false;		// disable ouput of any resultp-images (spare ressources)
	private int threads_input = 1;		// number of threads
	
	public void run(String arg) {
		System.gc();
		//hits = null;
		
		// Choose pics-source-dir
		String picDir = IJ.getDirectory("Select Image Source Folder...");
		if (picDir==null) return;
		String [] picDirFileList = new File(picDir).list();
		if (picDirFileList==null) return;
		IJ.log("The source image folder chosen was " +picDir+ ". It contains " +picDirFileList.length+ " objects.");
		
		//Get parameters from user
		GenericDialog gd = new GenericDialog ("Please insert GHT-parameters", IJ.getInstance());
		gd.addNumericField("Threshold: ", 0.75, 2);
		gd.addNumericField("Only use every n edge pixel. n: ", 1, 0);
		gd.addNumericField("Threads: ", 2, 0);
		gd.addNumericField("Minimum size (factor): ", 1, 2);
		gd.addNumericField("Maximum size (factor): ", 1, 2);
		gd.addNumericField("Size delta (factor): ", 1, 2);
		gd.addNumericField("Minimum angle (deg): ", 360, 2);
		gd.addNumericField("Maximum angle (deg): ", 360, 2);
		gd.addNumericField("Angle delta (deg): ", 360, 2);
		gd.addNumericField("Summand for accumulator smoothing(0 = off): ", 0, 2);
		gd.addNumericField("Non-Max-Supression-quad-pixels(1=off, only odd): ",1,2);
		gd.addCheckbox("Show hits in Log", true);
		gd.addCheckbox("HoughSpace-output", false);
		gd.addCheckbox("Disable pic-output",false);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("GHT-Plugin", "Plugin was canceled!");
		};
		treshold = (float)gd.getNextNumber();
		pixel_offset = (int)gd.getNextNumber();
		threads_input = (int)gd.getNextNumber();
		size_min = (float)gd.getNextNumber();
		size_max = (float)gd.getNextNumber();
		size_delta = (float)gd.getNextNumber();
		angle_min = (float)gd.getNextNumber();
		angle_max = (float)gd.getNextNumber();
		angle_delta = (float)gd.getNextNumber();
		acc_smooth_sum = (float)gd.getNextNumber();
		non_max_suppr_size = (int)gd.getNextNumber();
		show_hits_in_log = gd.getNextBoolean();
		hspace_output = gd.getNextBoolean();
		disable_pic_out = gd.getNextBoolean();
		// input validation
		if (treshold <= 0) {
			IJ.error("Threshold <= 0. Threshold deactivated!");
			treshold = (float)0.75;
		}
		if (pixel_offset < 0) {
			IJ.error("Negative pixel-offset not allowed. Pixel-offset deactivated.");
			pixel_offset = 1;
		}
		if (size_min<=0 || size_max<=0 || size_delta<=0 || size_min>size_max) {
			IJ.error("Wrong scaling parameters. Scaling deactivated.");
			size_min = (float) 1;
			size_max = (float) 1;
			size_delta = (float) 1;
		}
		if (angle_min<0 || angle_max<=0 || angle_delta<=0 || angle_min>angle_max || angle_max>360) {
			IJ.error("Wrong rotation parameters. Rotation deactivated.");
			angle_min = (float) 360;
			angle_max = (float) 360;
			angle_delta = (float) 360;
		}
		if (acc_smooth_sum < 0) {
			IJ.error("GHT-Plugin", "Accumulator smoothing summand may not be negative! Smooth accumulator updating ist deactivated now.");
			acc_smooth_sum = 0;
		}
		if (non_max_suppr_size < 1 || non_max_suppr_size%2 == 0) {
			IJ.error("GHT-Plugin", "Value for non-max-suppression-quad-pixel-lengt must be odd and positive. Functionality is now disabled!");
			non_max_suppr_size = 1;
		};
		if (threads_input <= 0) {
			IJ.error("GHT-Plugin", "At least one thread needed. Threads set to 1!");
			threads_input = 1;
		};
		// Output of parameters
		IJ.log("Parameters:   threshold: " +treshold+ ", pixel_offset: " +pixel_offset+ ", acc_smooth_sum: " +acc_smooth_sum+ ", non-max-suppr-size: " +non_max_suppr_size);
		IJ.log("                      min. size: " +size_min+ ", max. size: " +size_max+ ", size delta: " +size_delta);
		IJ.log("                      min. angle: " +angle_min+ ", max. angle: " +angle_max+ ", angle delta: " +angle_delta);
		
		// Find ref-image and analyze ref-object
		for (int i=0; i<picDirFileList.length; i++) {
			
			File f = new File (picDir+picDirFileList[i]);
			if (f.isFile() && picDirFileList[i].toLowerCase().contains("reference_object.")) {
				
				IJ.log("Processing file: " +picDirFileList[i]);
				long time = -System.currentTimeMillis();
				
				analyze_ref_image (f.getPath());
				f = null;
				
				System.gc();
				
				rtable_simple_size = rtable.size() / 2;
				do_scaling();
				rtable_scaled_size = rtable.size() / 2;
				do_rotation();
				rtable_full_size = rtable.size() / 2;
				
				IJ.log("Reference-object analyzed! Time needed: " +(time + System.currentTimeMillis())+ "ms");
			} // f.isFile() && picDirFileList[i].equals("reference_object.gif"
		} // for (int i=0; i<picDirFileList.length; i++)
		
		System.gc();
		
		/*// Output of Rtable
		System.out.println("RTable: ");
		System.out.println("size: " +rtable.size());
		for (int j = 0; j < rtable.size(); j++) {
			if (j%2 == 0)
				System.out.println("x: " +rtable.get(j));
			else
				System.out.println("y: " +rtable.get(j));
		}
		
		// Output of params-table
		System.out.println("Params-Table: ");
		System.out.println("size: " +params.size());
		for (int j = 0; j < params.size(); j++) {
			if (j%2 == 0)
				System.out.println("scale: " +params.get(j));
			else
				System.out.println("rotation: " +params.get(j));
		}*/
		
		// Analyze images
		for (int i=0; i<picDirFileList.length; i++) {
			File f = new File (picDir+picDirFileList[i]);
			if (f.isFile() && !picDirFileList[i].equals("reference_object.gif")) {
				String fname = f.getPath();
				f = null;
				
				search_for_object(fname, treshold);
				System.gc();
				IJ.log(hits.size()/2+ " objects found:");
				
				// Output of hits
				if (show_hits_in_log)
					for (int j=0; j<hits.size()-1; j++) {
						IJ.log("x: " +hits.get(j)+ ", y: " +hits.get(j+1));
						j++;
					} // for (int j=0; j<hits.size()-1; j++)
				
				// Output of HoughSpace
				if (hspace_output && !disable_pic_out) {
					ImagePlus img_hspace = NewImage.createByteImage("HoughSpace for " + fname, wsch, hsch, 0, NewImage.FILL_WHITE);
					ImageProcessor ip_hspace = img_hspace.getProcessor();
					byte [] pixels_hspace = (byte [])ip_hspace.getPixels();

					for (int j=0; j<hits.size()-1; j++) {
						pixels_hspace[hits.get(j) + hits.get(j+1)*wsch] = 0;
						j++;
					} // for (int j=0; j<hits.size()-1; j++)
					
					img_hspace.show();
					pixels_hspace = null;
				} // if (hspace_output && !disable_pic_out)
				
				hits.clear();
				
			} // if (f.isFile() && !picDirFileList[i].equals("reference_object.gif"))
		} // for (int i=0; i<picDirFileList.length; i++)
		IJ.log("Number of threads used for object searching: " +threads_input);
	} // public void run(ImageProcessor ip)
	
	
	private void analyze_ref_image (String imagefile) {
		ImagePlus imp_ref = new ImagePlus(imagefile);
		ImageProcessor ip_ref = imp_ref.getProcessor();
		
		wref = ip_ref.getWidth();
		href = ip_ref.getHeight();
		pixels = ((byte []) ip_ref.getPixels());
		
		ip_ref = null;
		imp_ref.flush();
		imp_ref.close();		
		
		build_xy_rtable_n(pixel_offset);
		
		pixels = null;
	} // public void analyze_ref_image (String imagefile)
	
	
	private void build_xy_rtable_n (int n) {
		int i = 0, j = n;
		int xmin = wref, xmax = 0;
		int ymin = href, ymax = 0;
		
		//System.out.println("Points:");
		if (n==0 || n==1) {
			for (i=0; i<pixels.length; i++)
				if (pixels [i] == edge_colour) {
					//System.out.println("x: " +(i-(((int)i/wref)*wref))+ ", y: " +((int)i/wref)+ ", p: " +i);
					rtable.add( i-(((int)(i/wref))*wref) );		// delta x
					rtable.add( (int)(i/wref) );		// delta y
				} // if (pixels [i] == edge_colour)
		} else {
			for (i=0; i<pixels.length; i++)
				if (pixels [i] == edge_colour)
					if (j < n)
						j++;
					else {
						//System.out.println("x: " +(i-(((int)(i/wref))*wref))+ ", y: " +((int)i/wref)+ ", p: " +i);
						rtable.add( i-(((int)(i/wref))*wref) );		// delta x
						rtable.add( (int)(i/wref) );		// detla y
						j = 1;
					}; // if (j < n) else 
		}; // if (n==0 || n==1) else
		
		// find min-, max- and middle-coordinates of ref-object
		for (i = 0; i < rtable.size(); i++) {
			int x = rtable.get(i);
			int y = rtable.get(i+1);
			if (x < xmin)
				xmin = x;
			if (x > xmax)
				xmax = x;
			if (y < ymin)
				ymin = y;
			if (y > ymax)
				ymax = y;
			i++;
		} // for (i = 0; i < rtable.size()-1; i++)
		xref = (int)((xmax+xmin)/2);
		yref = (int)((ymax+ymin)/2);
		
		/*System.out.println("xref: " +xref+ ", yref: " +yref);
		System.out.println("xmax: " +xmax+ ", ymax: " +ymax);
		System.out.println("xmin: " +xmin+ ", ymin: " +ymin);**/
		
		// invert vector & move ref-point: adapt rtable according to "middle of object"
		for (i = 0; i < rtable.size(); i++) {
			rtable.set(i, -(rtable.get(i)-xref));
			rtable.set(i+1, -(rtable.get(i+1)-yref));
			i++;
		} // for (i = 0; i < rtable.size(); i++)
		
		// store parameters for original size (rtable is already built)
		params.add((float)1);												// scale-factor
		params.add((float)0);												// angular
		
	} // public void build_xy_rtable_n (int n)
	
	
	private void do_scaling () {
		// scaling
		for (float size_factor = size_min; size_factor<=size_max; size_factor = size_factor + size_delta) {
			if (size_factor != 1) {
				// store parameters
				params.add((float)size_factor);								// scale-factor
				params.add((float)0);										// angular
				// scale
				for (int i=0; i<rtable_simple_size; i++) {
					rtable.add(Math.round(rtable.get(i*2)*size_factor));		// scale x
					rtable.add(Math.round(rtable.get(i*2+1)*size_factor));		// scale y
					//rtable.add((int)(rtable.get(i*2)*size_factor));		// scale x
					//rtable.add((int)(rtable.get(i*2+1)*size_factor));		// scale y
				} // for (int i=0; i<rtable_simple_size; i++)
			} // if (size_factor != 1)
		} // for (float size_factor = size_min; size_factor<=size_max; size_factor = size_factor + size_delta)
	} // private void do_scaling ()
	
	
	private void do_rotation () {
		float delta_rad = (float)Math.toRadians(angle_delta);
		float min_rad = (float)Math.toRadians(angle_min);
		float twoPI = (float)Math.PI * 2;
		int nr_of_passes = (int) ((angle_max-angle_min)/angle_delta);
		double phi = 0;
		int x = 0;
		int y = 0;
		int i = 0;
		double norm = 0;
		
		if (((angle_min + nr_of_passes * angle_delta) >= 360) || ((angle_min + nr_of_passes * angle_delta) > angle_max))
			nr_of_passes = nr_of_passes - 1;
		if (angle_min == 0)
			i = 1;
		else
			i = 0;
		
		for (; i<=nr_of_passes; i++) {
			//System.out.println("i: " +i+",");+
			for (int j = 0; j<rtable_scaled_size; j++) {
				//System.out.print(j+",");
				x = -rtable.get(j*2);
				y = -rtable.get(j*2+1);
				
				norm = Math.sqrt(x*x + y*y);
				phi = Math.atan2(y,x);
				if (phi>0)
					phi = phi + i*delta_rad + min_rad;
				else
					phi = twoPI + phi + i*delta_rad + min_rad;
				rtable.add(-Math.round((float)(norm*Math.cos(phi))));	// rotated x-value
				rtable.add(-Math.round((float)(norm*Math.sin(phi))));	// rotated y-value
				
				if (j % rtable_simple_size == 0) {
					// store parameters
					params.add(params.get((int)(j/rtable_simple_size)*2));	// scale
					params.add(i*angle_delta + angle_min);	// rotation
				} // if (j % rtable_simple_size == 0)
				//System.out.println("x_org: " +x+ ", y_org: " +y+ ", x_rot: " +Math.round((float)norm*Math.cos(phi))+ ", y_rot: " +Math.round((float)norm*Math.sin(phi)));
				//System.out.println("ang_org: " +Math.toDegrees(Math.atan2(y, x))+ ", ang_rot: " +Math.toDegrees(phi));
			} // for (int j = 0; j<rtable_scaled_size; j++)
		} // (int i = 1; i<=nr_of_passes; i++)
			
	} // private void do_rotation ()
	
	// start threads and wait till finished
	private static void startandjoin (Thread[] threads) {  
		for (int i = 0; i < threads.length; ++i) {  
			threads[i].setPriority(Thread.MAX_PRIORITY);  
			threads[i].start();  
		}  

		try {     
			for (int i = 0; i < threads.length; ++i)  
				threads[i].join();  
		} catch (InterruptedException ie) {  
			throw new RuntimeException(ie);  
		}  
	} // private static void startandjoin (Thread[] threads)
	
	// store hits, synchronized because of multithreading
	private synchronized void storehit (int x, int y) {
		hits.add(x);
		hits.add(y);
	}
	
	private void search_for_object(String imagefile, float treshold) {
		IJ.log("Searching for objects in file: " +imagefile);
		long time = -System.currentTimeMillis();
		ImagePlus imp_sch = new ImagePlus(imagefile);
		ImageProcessor ip_sch = imp_sch.getProcessor();
		
		wsch = ip_sch.getWidth();
		hsch = ip_sch.getHeight();
		pixels = ((byte []) ip_sch.getPixels());
		if (disable_pic_out) {
			ip_sch = null;
			imp_sch.flush();
			imp_sch.close();
			System.gc();
		}	// if (disable_pic_out)	
		
		//calc rtable pixel-deltas for search-pic only once
		delta = new int [rtable.size()/2];
		for (int i=0; i<rtable.size()-1; i++) {
			delta[i/2] = rtable.get(i) + rtable.get(i+1)*wsch;
			i++;
		} // for (int i=0; i<rtable.size()-1; i++)
		// IJ.log("Auflösung umgerechnet: " +(time + System.currentTimeMillis())+ "ms");
		
		// treshold
		treshold_hits = (int)(rtable_simple_size*treshold);
		// how many threads?
		int calcs = rtable_full_size/rtable_simple_size;
		if ((calcs/threads_input) < 1) {
			threads = new Thread [1];
			threads_input = 1;
		} else
			threads = new Thread [threads_input];
		int calcs_per_thread = calcs / threads.length;
		
		// prepare threads for object searching
		for (int i=0; i<threads.length; i++) {
			int tmp = 0;
			if (((i+1) * calcs_per_thread) > calcs) {
				tmp = calcs_per_thread;
			} else {
				tmp = ((i+1) * calcs_per_thread);
			} // if (((i+1) * calcs_per_thread) > calcs) else
			final int to = tmp;
			final int from = i * calcs_per_thread;
			
			// prepare thread
			threads [i] = new Thread () {
				private float [] acc_array;
				private float [] tmp_array;
				
				public void run () {
					int idx_tmp = 0, idx_hlp = 0;
					
					for (int i=from; i<to; i++) {
						//IJ.log("Durchgang: " +i+ ", scale: " +params.get(i*2)+ ", rotation: " +params.get(i*2+1));
						acc_array = new float [pixels.length];
						idx_hlp = (i+1)*(rtable_simple_size)-1;
						
						// fill accumulator
						for (int j=0; j<pixels.length; j++) {
							if (pixels[j] == edge_colour) {
								for (int k=i*rtable_simple_size; k<idx_hlp; k++) {
									idx_tmp = j+delta[k];
									if (idx_tmp>=0 && idx_tmp<acc_array.length)
										acc_array[idx_tmp]++;
								} // for (int k=i*rtable_simple_size; k<idx_hlp; k++)
							} // if (pixels[j] == edge_colour)
						} // for (int j=0; j<pixels.length; j++)
						
						// accumulator smoothing
						if (acc_smooth_sum > 0) {
							tmp_array = new float [acc_array.length];
							int lu = -wsch-1; //left up
							int ru = -wsch+1; // right up
							int ld = wsch-1; //left down
							int rd = wsch+1; // right down
							
							//edge points
							tmp_array[0] = acc_array[0] + acc_smooth_sum*(acc_array[1] + acc_array[wsch] + acc_array[wsch+1]);
							tmp_array[wsch-1] = acc_array[wsch-1] + acc_smooth_sum*(acc_array[wsch-2] + acc_array[2*wsch-1] + acc_array[2*wsch-2]);
							tmp_array[wsch*(hsch-1)] = acc_array[wsch*(hsch-1)] + acc_smooth_sum*(acc_array[wsch*(hsch-2)] + acc_array[wsch*(hsch-2)+1] + acc_array[wsch*(hsch-1)+1]);
							tmp_array[acc_array.length-1] = acc_array[acc_array.length-1] + acc_smooth_sum*(acc_array[acc_array.length-wsch-2] + acc_array[acc_array.length-wsch-1] + acc_array[acc_array.length-2]);
							
							//upper border
							for (int j=1; j<wsch-1; j++)
								tmp_array[j] = acc_array[j] + acc_smooth_sum*(acc_array[j-1]+acc_array[j+1]+acc_array[j+ld]+acc_array[j+wsch]+acc_array[j+rd]);
							//lower border
							for (int j=wsch*(hsch-1)+1; j<acc_array.length-1; j++)
								tmp_array[j] = acc_array[j] + acc_smooth_sum*(acc_array[j-1]+acc_array[j+1]+acc_array[j+lu]+acc_array[j-wsch]+acc_array[j+ru]);
							//left border
							for (int j=wsch; j<wsch*(hsch-1); j = j+wsch)
								tmp_array[j] = acc_array[j] + acc_smooth_sum*(acc_array[j-wsch]+acc_array[j+wsch]+acc_array[j+ru]+acc_array[j+1]+acc_array[j+rd]);
							//right border
							for (int j=2*wsch-1; j<acc_array.length-1; j = j+wsch)
								tmp_array[j] = acc_array[j] + acc_smooth_sum*(acc_array[j-wsch]+acc_array[j+wsch]+acc_array[j+lu]+acc_array[j-1]+acc_array[j+ld]);
						
							//other pixels
							int p = 0;
							for(int j=1; j<wsch-1; j++) {
								for(int k=1; k<hsch-1; k++) {
									p = k*wsch+j;
									tmp_array[p] = acc_array[p] + acc_smooth_sum*(acc_array[p+lu]+acc_array[p-wsch]+acc_array[p+ru]+acc_array[p-1]+acc_array[p+1]+acc_array[p+ld]+acc_array[p+wsch]+acc_array[p+rd]);
								} // for(int k=1; k<hsch-1; k++)
							} // for(int j=1; j<wsch-1; j++)
								
							acc_array = tmp_array;
							
						} // if (acc_smooth_sum > 0)
						
						// non-max-suppression with quad-pixel-size (= non_max_suppr_size)
						if (non_max_suppr_size > 1) {
							int minimum = -(int)(non_max_suppr_size/2);
							int maximum = (int)(non_max_suppr_size/2);
							int tmp = 0;
							for (int p = 0; p < acc_array.length; p++) {
								if (acc_array[p]!=0)
									for (int h = minimum; h <= maximum; h++) {
										for (int j = minimum; j <= maximum; j++) {
											tmp = p + h + j * wsch;
											if (tmp >= 0 && tmp < acc_array.length)
												if (acc_array[p] > acc_array[tmp])
													acc_array[tmp] = 0;
										} // for (int j = minimum; j <= maximum; j++)
									} //for (int i = minimum; i <= maximum; i++)
							} // for (int i = 0; i < acc_array.length; i++)
						} // if (non_max_suppr_size > 1)
						
						// search for hits with treshold depending on ref-obj-pixel-amount
						if (treshold_hits > 0) {
							for (int j=0; j<acc_array.length; j++)
								if (acc_array[j] >= treshold_hits) {
									storehit(j-(((int)(j/wsch))*wsch), (int)(j/wsch));
									pixels[j]=edge_colour;
									//IJ.log("Found object at x = " +hits.get(hits.size()-2)+ " and y = " +hits.get(hits.size()-1));
								}	// if (acc_array[j] >= treshold_hits)
						} // if (treshold_hits > 0)
					} // for (int i=from; i<to; i++)
					
					
				} // public void run ()
			}; // threads [i] = new Thread ()
			
		} // for (int i=0; i<threads.length; i++)
		
		// IJ.log("Threads werden gestartet: " +(time + System.currentTimeMillis())+ "ms");
		// start threads
		startandjoin(threads);
		
		IJ.log("Finished! Time needed: " +(time + System.currentTimeMillis())+ "ms");
		threads = null;
		delta = null;
		pixels = null;
		if (!disable_pic_out)
			imp_sch.show();
		
		//ip_sch = null;
		//imp_sch.close();
	} // private void search_for_object()
	
} // public class GHT_ implements PlugIn