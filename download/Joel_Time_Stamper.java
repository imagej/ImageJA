import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import java.awt.*;
import java.io.*;
import ij.plugin.*;

/**
 *  This plug-in is a custom jobber for Joel.
 * 
 * 1) User generates a simple file list in some desired order (creation order?).
 * 2) Sequentially open each file.
 * 3) Then *destructively* insert a time annotation to each image (a number of increasing value).
 * 4) Save the file with its original name in a new directory.
 *
 *  Plan B.  i.e. (1) use the antiquated DOS system
 * to generate a file list in creation order.  (2) Then, since the file
 * comes up with all sorts of info, edit the file in Excell to strip
 * out the irrelevant
 * material and leave a simple list of file names.[I can do this]  (3)
 * in ImageJ, sequentially open each file, then
 * insert a number of increasing value, and then to save the result in
 * a new directory.
 * This plugin opens images from a text file list. 
 * The list can contain full paths or just file names. 
 * The default directory is the one containing the text file.  
 *
 * Notes:
 *  The times are drawn in the current foreground color.
 *  X and Y are the image lcoation where the user wants the time stamp.
 *  Supports Tiff, Jpeg, Gif, bmp, DICOM, FITS, raw images.
 *  Default output format is TIF.
 *  The destination folder, directory must exist. 
 *
 *  It bastardizes the List Opener (http://rsb.info.nih.gov/ij/plugins/list-opener.html),
 *  Time Stamper (http://rsb.info.nih.gov/ij/plugins/stamper.html), and
 *  Batch Converter (http://rsb.info.nih.gov/ij/plugins/batch-converter.html) 
 *  by Wayne Rasband (wayne@codon.nih.gov). 
 *  
 *	Requested by Joel B. Sheffield, Ph.D (jbs@astro.temple.edu)
 *	@author	keesh
 *	@author	keesh@ieee.org
 *
 *  Installation: Download Joel_Time_Stamper.class to the plugins folder and restart ImageJ.  
 *                Download Joel_Time_Stamper.java to the plugins folder and compile it using the Plugin/Compile and Run command
 *                Similar directions to Command Line Example http://rsb.info.nih.gov/ij/plugins/command-line.html
 *  
 *  03/03/2003: First version Requested by Joel B. Sheffield, Ph.D  jbs@temple.edu
 *  03/12/2003: Additional tweaking, error handling, and command line interface
 *  03/22/2003: Additional error handling + cleanup  
 *
 *  Known Issues
 *  .cannot get foreground user preference color while running from the command line
 *  .jdk 1.4.0+ caused an OutOfMemoryError while running from the command line
 *   using a big 100 image filelist?
 *
 *  Things To Do    
 *
 *  Testing
 *  From command line-- Main Class: Joel_Time_Stamper
 *  Program Arguments: F:\source\java\imagej\data\joel_time_stamper\joel_ts_file_list  F:\source\java\imagej\data\joel_time_stamper\out5 1 10 50 50 100 2 "sec"
 */
public class Joel_Time_Stamper implements PlugIn {
	double time;
    static int x = 2;
	static int y = 15;
    static int size = 24;
	int maxWidth;
	Font font;
	static double start = 0;
	static double interval = 1;
	static String suffix = "sec";  // "hh:mm:sec"
	static int decimalPlaces = 0;
    boolean canceled;
    static final int MAX_NUM_IMAGES = 10000;  // only used for max label sizing
	String filelist, input_dir, output_dir;   
	static final String fin_mess = "Joel Time Stamper fin";   
	static final String err_mess = "Joel Time Stamper error";
	static Color annotColor = Color.red;  // default annotation colour is Red
	   
    public void run(String arg) {   
        if(!getParmsFromUser())
            return;
        if(!process()){
            IJ.error("Could not process images");
            return;
        }
    }  // run

    protected boolean process() {   
            font = new Font("SansSerif", Font.PLAIN, size);
            time = start;
		    if (y<size)
			    y = size;
		    //imp.startTiming();
            String separator = System.getProperty("file.separator");
            Opener o = new Opener();
            try {
            BufferedReader r = new BufferedReader(new FileReader(input_dir+filelist));
            while (true) {
                String path = r.readLine();
                if (path==null)
                    break;
                else {
                    if (path.indexOf(separator)<0)
                        path = input_dir + path;
                    ImagePlus imp = o.openImage(path);
                    if (imp == null)
                        return false;
                    ImageProcessor ip = imp.getProcessor();
                    if(ip == null) 
                        return false;
                    //
                    ip.setFont(font);
		            ip.setColor(annotColor);
		            // this is the point where we can create "hh:mm:sec" string
		            // foreach image
		            String s = getString(time);
		            // 'cept for the ip, the follow is a loop invariant
		            maxWidth = ip.getStringWidth(getString(start+interval*MAX_NUM_IMAGES));
		            ip.moveTo(x+maxWidth-ip.getStringWidth(s), y);
		            ip.drawString(s);
		            save(imp, output_dir);
		            time += interval;	
		            // loop cleanup
                    imp.trimProcessor();     
                }  // else path
            }  // while
            r.close();
        } catch (IOException e) {
            IJ.error(""+e);
            return false;
        }
        // 888-- jdk 1.4.0+ caused an OutOfMemoryError while running from the command line
        // using a big 100 image filelist?
        /*
        catch (OutOfMemoryError e) {
            IJ.error(""+e + "time= " + time + " " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
        */
        IJ.showStatus(fin_mess); 
        return true;
     }  // process

  
	protected void save(ImagePlus img, String dir) {
	    String dir2 = dir;
	    if (!dir2.endsWith(File.separator))
		    dir2 += File.separator;
	    // 
	    String name = img.getTitle();
		// try to infer format from suffix-- a cheap, but not great approach
		String format = "Tiff";  // default
		String base_name = "";
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex>=0) {
			base_name    = name.substring(0, dotIndex);
		    format  = name.substring(dotIndex, name.length());
		    format = format.replace('.',' ');
		    format = format.trim();
		}
		String path = dir2 + base_name;
	    //
	    if (format.equalsIgnoreCase("Tiff") || format.equalsIgnoreCase("Tif"))
			new FileSaver(img).saveAsTiff(path+".tif");
		else if (format.equalsIgnoreCase("Zip"))
			new FileSaver(img).saveAsZip(path+".zip");
		else if (format.equalsIgnoreCase("Raw"))
			new FileSaver(img).saveAsRaw(path+".raw");
		else if (format.equalsIgnoreCase("Jpeg") || format.equalsIgnoreCase("Jpg"))
			new FileSaver(img).saveAsJpeg(path+".jpg");
		else  // default
		    new FileSaver(img).saveAsTiff(path+".tif");
	}  // save
	
	protected void showDialog() {
		GenericDialog gd = new GenericDialog(getClass().getName());
		gd.addNumericField("Starting Time:", start, 2);
		gd.addNumericField("Time Between Frames:", interval, 2);
		gd.addNumericField("X Location:", x, 0);
		gd.addNumericField("Y Location:", y, 0);
		gd.addNumericField("Font Size:", size, 0);
		gd.addNumericField("Decimal Places:", decimalPlaces, 0);
		gd.addStringField("Suffix:", suffix);
		gd.showDialog();
		if (gd.wasCanceled())
			{canceled = true; return;}
		start = gd.getNextNumber();
 		interval = gd.getNextNumber();
		x = (int)gd.getNextNumber();
		y = (int)gd.getNextNumber();
		size = (int)gd.getNextNumber();
		decimalPlaces = (int)gd.getNextNumber();
		suffix = gd.getNextString();
	}
	
	String showOutDirDialog() {
	    String dir="";
        SaveDialog sd = new SaveDialog("Open destination folder...", "dummy name (required)", "");
		if (sd.getFileName()!=null) {
		    dir = sd.getDirectory();
		} 
	    return dir;
	}
	
	protected boolean getParmsFromUser() {
        // get a file list "file"
        // getClass should be OK for public members
		String my_class_name = getClass().getName() ;
        String fl_prompt = ":  Select a file list";
        OpenDialog od = new OpenDialog(my_class_name + fl_prompt, null);
        filelist = od.getFileName();
        input_dir = od.getDirectory();
        if(!checkDir(input_dir)) {
            IJ.error("Invalid input file list directory:  " + input_dir);
            return false;
        }
        if(!checkFile(input_dir+filelist)) { 
            IJ.error("Invalid input file list:  " + filelist);
            return false;
        }
        // get one-time user prefs
        output_dir = showOutDirDialog();
        if(!checkOutputDir(input_dir, output_dir)) {
            IJ.error("Invalid output directory:  " + output_dir); 
            return false;
        }
        //
        showDialog();
	    if (canceled)
	        return false;
	    
	    // if getting parms from user, get foreground color from UI toolbar
	    annotColor = Toolbar.getForegroundColor();
	    
	    return true;
     }
	
	protected boolean getParmsFromCommandLine(String args[]) {
	    // filelist
        String idir = getDir(args[0]); 
	    if(idir == null) {
	        IJ.error("Invalid input file list directory:  " + args[0]);
            return false;
	    }
        String fl = getFile(args[0]);
        if(fl == null) {
            IJ.error("Invalid input file list:  " + args[0]);
            return false;
        }  
	    input_dir = idir;
	    if (!input_dir.endsWith(File.separator))
		    input_dir += File.separator;
	    filelist = fl;  
	  
	    String odir = args[1];
	    if (!odir.endsWith(File.separator))
		    odir += File.separator;
	    if(!checkOutputDir(input_dir, odir)) {
            IJ.error("Invalid output directory:  " + odir); 
            return false;
        }
	    output_dir = odir;
	    
	    // get starting time
	    try {
			start = Double.valueOf(args[2]).doubleValue();
		} catch (NumberFormatException e) {
		    IJ.error("Invalid Starting Time: " + args[2]); 
		    return false;
		}
	    // get time between frames
	    try {
			interval = Double.valueOf(args[3]).doubleValue();
		} catch (NumberFormatException e) {
		    IJ.error("Invalid Time Between Frames: " + args[3]);
		    return false;
		}
	    // get x location
	    try {
			x = Integer.valueOf(args[4]).intValue();
		} catch (NumberFormatException e) {
		    IJ.error("Invalid X Location: " + args[4]); 
		    return false;
		}
        // get y location
	    try {
			y = Integer.valueOf(args[5]).intValue();
		} catch (NumberFormatException e) {
		    IJ.error("Invalid Y Location: " + args[5]); 
		    return false;
		}
        // get font size
        try {
			size = Integer.valueOf(args[6]).intValue();
		} catch (NumberFormatException e) {
		    IJ.error("Invalid Font Size: " + args[6]); 
		    return false;
		}
		// get decimal places
		try {
			decimalPlaces = Integer.valueOf(args[7]).intValue();
		} catch (NumberFormatException e) {
		    IJ.error("Decimal Places: " + args[7]); 
		    return false;
		}
		// get suffix
		suffix = args[8];
		
		// 888-- Most methods in the Prefs class will not unless Prefs.load() has been 
        // called, and Prefs.load() currently requires an ImageJ object as a parameter.
		// if getting parms from the command line, try getting foreground color from ImageJ Preferences
		annotColor = Prefs.getColor(Prefs.FCOLOR, annotColor); 
		
	    return true;
	}  // getParmsFromCommandLine
	
	String getString(double time) {
		if (interval==0.0)
			return suffix;
		else
			return (decimalPlaces==0?""+(int)time:IJ.d2s(time, decimalPlaces))+" "+suffix;
	}


    protected boolean checkOutputDir(String input_dir, String output_dir) {
        if(!checkDir(output_dir)) 
            return false;
        // do not allow the output dir to be the same as the input dir (data loss)
        if(output_dir.compareTo(input_dir)==0) {
            return false;
        }
        return true;
    }

    boolean checkFile(String sfile) {
        if (sfile==null || sfile=="")
            return false;
        File file = new File(sfile);
        try
        {
            if(!file.isFile()) 
                return false;
        } 
        catch (SecurityException e) {}  // eat exception
        return true;
    }

    boolean checkDir(String sdir) {
        if (sdir==null || sdir=="")
            return false;
        File file = new File(sdir);
        try
        {
            if(!file.isDirectory()) 
                return false;
        } 
        catch (SecurityException e) {}  // eat exception
        return true;
    }

    // returns null on errors
    String getDir(String sfile) {
        if( checkDir(sfile) ) return sfile;
        if(!checkFile(sfile)) return null;         
        File file = new File(sfile);
        return file.getParent();
    }
    // return null on errors
    String getFile(String sfile) {
        if(!checkFile(sfile)) return null;         
        File file = new File(sfile);
        String name = file.getName();
        if(name == "") return null;
        else return name;
    }

     // command-line application interface
     /* Run from the command line using:
     *      java -cp ij.jar;. Joel_Time_Stamper
     *      <my_filelist> <output_dir> <start_time> <interval_time> <x_loc> <y_loc> 
     *      <font_size> <decinal_places> <suffix>
     *  Replace "java" with "C:\ImageJ\jre\bin\java"
     *  to use the Java distributed with ImageJ.
     *  On Unix, replace the ";" with ":".
     */
     public static void main(String args[]) {
        if (args.length<9)
             IJ.write("usage: java Joel_Time_Stamper filelist output_dir\n" + 
             "start_time interval_time x_loc y_loc font_size\n" +
             "decinal_places suffix");
         else {
            Joel_Time_Stamper jts = new Joel_Time_Stamper(); 
            if(jts.getParmsFromCommandLine(args)) {
                if( jts.process() )
                    IJ.showMessage(fin_mess);
                else
                    IJ.showMessage(err_mess);
            } else {
                IJ.showMessage(err_mess);
            }
            //
            System.exit(0);
         }
     }

}  // Joel_Time_Stamper
