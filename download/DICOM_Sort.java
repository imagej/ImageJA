import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.text.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;
import java.awt.image.*;

/**
 *      First version: 2003/04/22.        
 *      This plugin produces different image sequences of DICOM files 
 *      by exploring the DICOM header information via several tags. 
 *      Images can be sorted by image number, image time 
 *      or acquisition time before importing. The plugin displays 
 *      the tag information with full paths and file names in the log window.
 *
 *      Modified version: 2004/05/13.
 *      Modified with the help of Wayne Rasband. Many exceptions has been 
 *      catched and code was added to enable display of the DICOM tags 
 *      when the "Show Info" command on the stack is used.
 *
 *      This program is distributed in the hope that it will be useful, but
 *      WITHOUT ANY WARRANTY.
 *
 *      @author Thomas Stamm
 *      @author University of Munster, Department of Orthodontics
 *      @author stammt@uni-muenster.de
 *      
 */


public class DICOM_Sort implements PlugIn {
       
    private static boolean grayscale;
    private static boolean halfSize;
    public final int FILE_SIZE = 520000; // explore
    private static String pick = "AcquisitionTime";
    private  static String prefix1 = "LastFirstname";
    private  static String prefix2 = "Age";    
    private  static String prefix3 = "Sex";  
    private int countdir;
    private String tempp = "dummy"; /**    for sorting */ 
    private String whichcase = "none";
    private String whichprefix1 = "none";
    private String whichprefix2 = "none";
    private String whichprefix3 = "none";
    private static boolean statussorta;
    private static boolean statussortd;
    private static boolean statustext;
    private static boolean open_as_stack = true;
    private static boolean explore;
    private static boolean rdicomdir;
    private static boolean only_images;
    
 
public void run(String arg) {

		String[] sequences =   {"AcquisitionTime", "ImageNumber", "ImageTime"};
        String[] prefixlist =  {"None", "LastFirstname", "Age", "Sex", "StudyDescription", "ProtocolName"};
        String[] prefixlist2 = {"None", "LastFirstname", "Age", "Sex", "StudyDescription", "ProtocolName"};
		String[] prefixlist3 = {"None", "LastFirstname", "Age", "Sex", "StudyDescription", "ProtocolName"};
	    
	    GenericDialog gd = new GenericDialog("DICOM Sort");
		gd.addMessage("This plugin explores DICOM files via selected tags below.");
		gd.addMessage("          comments to: stammt@uni-muenster.de            ");
		gd.addMessage("  ");
		gd.addChoice("Image Sequence by: ",sequences, pick);
		gd.addChoice("1st Prefix: ", prefixlist, prefix1);
		gd.addChoice("2nd Prefix: ", prefixlist2, prefix2);
		gd.addChoice("3rd Prefix: ", prefixlist3, prefix3);
		gd.addCheckbox("Show sorted Image Sequence (asc)", statussorta);
		gd.addCheckbox("Sort Image Sequence (desc)", statussortd);
		gd.addCheckbox("Make Sorted Text File List (only path + file name)", statustext);
		gd.addCheckbox("Open sorted Images", only_images);
		gd.addCheckbox("Open sorted Images as a Stack", open_as_stack);
		gd.addCheckbox("Convert to 8-bits", grayscale);

		gd.showDialog();
	    if (gd.wasCanceled())
			return;

		pick   = gd.getNextChoice();
		prefix1 = gd.getNextChoice();
		prefix2 = gd.getNextChoice();
		prefix3 = gd.getNextChoice();
		statussorta = gd.getNextBoolean();
		statussortd = gd.getNextBoolean();
		statustext = gd.getNextBoolean();
		only_images = gd.getNextBoolean();
		open_as_stack = gd.getNextBoolean();
		grayscale = gd.getNextBoolean();
		OpenDialog od = new OpenDialog("Select a file in source folder...", "");
		String dir = od.getDirectory();
		if (od.getFileName()==null)
			return;
		process(od.getDirectory());
		
    } // of void run

public void process(String dir) {
	    
	    String[] list = new File(dir).list();
		if (list==null)
		   return;
	    int n = list.length;
	    long[] bubblesort = new long[list.length+1]; // length+1 otherwise array-exception
		if (!statustext) {
		    IJ.log("Current Directory is: " + dir);
		    IJ.log(" ");
		    IJ.log("DICOM File Name / " + prefix1 + " / " + prefix2 + " / " + prefix3 + " / " + pick);
		    IJ.log(" ");
		}
		for (int i=0; i<n; i++) {
			IJ.showStatus(i+"/"+n);
			File f = new File(dir+list[i]);
			if (!f.isDirectory()) {
				ImagePlus img = new Opener().openImage(dir, list[i]);
				if (img!=null && img.getStackSize()==1) {
				    if (!scoutengine(img))
					return;
				    if (!statustext) {
					IJ.log(list[i] + "/" + whichprefix1 + "/" + whichprefix2 + "/" + whichprefix3 + "/" + whichcase);
				    }
                    int lastDigit = whichcase.length()-1;
                    while (lastDigit>0) {
                        if (!Character.isDigit(whichcase.charAt(lastDigit)))
                            lastDigit -= 1;
					    else
						break;
                    }
				    if (lastDigit<whichcase.length()-1)
				           whichcase = whichcase.substring(0, lastDigit+1);
                        // IJ.log("\""+whichcase+"\"");				    
				    bubblesort[i] = Long.parseLong(whichcase); // Parsing to long for sorting
				}
			}
		}
	
/**	sort   -----------------------------------------------------      */
		
		if (statussorta || statussortd || statustext) {          
		    boolean sorted = false;  // sort the array
		    while (!sorted) {
			sorted = true;
			for (int i=0; i < n-1; i++) {   // n-1 otherwise array-exception
		            if (statussorta) {
				if (bubblesort[i] > bubblesort[i+1]) // sorting asc.
				{
				long temp = bubblesort[i];
				tempp = list[i];
				bubblesort[i] = bubblesort[i+1];
				list[i] = list[i+1];
				bubblesort[i+1] = temp;
				list[i+1] = tempp;
				sorted = false;
		    		}
			    }
			     else {
				if (bubblesort[i] < bubblesort[i+1]) // sorting desc.
		    		{
				long temp = bubblesort[i];
				tempp = list[i];
				bubblesort[i] = bubblesort[i+1];
				list[i] = list[i+1];
				bubblesort[i+1] = temp;
				list[i+1] = tempp;
				sorted = false;
		    		}
			     } // of else
			} // of for
		    }
		    IJ.log(" ");
		    for (int i=0; i<n; i++) {
			if (!statustext) {
			    IJ.log(list[i] + " / " + bubblesort[i]);
			}
			else {
			    IJ.log(dir + list[i]);
			}
		    }
		}
		
/**	sort  end -------------------------------------------------      */
		
		if (open_as_stack || only_images) {
		    boolean sorted = false; // sort the array
		    while (!sorted) {
			sorted = true;
			for (int i=0; i < n-1; i++) {   // n-1 otherwise array-exception
			    if (bubblesort[i] > bubblesort[i+1]) 
		    		{
				long temp = bubblesort[i];
				tempp = list[i];
				bubblesort[i] = bubblesort[i+1];
				list[i] = list[i+1];
				bubblesort[i+1] = temp;
				list[i+1] = tempp;
				sorted = false;
		    		}
			}
		    }// of while
		    
		    if (only_images) {
		         Opener o = new Opener();
		         int counter = 0;
		         IJ.log(" ");
		         for (int i=0; i<n; i++) {
		             String path = (dir + list[i]);
		             if (path==null)
		                 break;
		             else {
		                 ImagePlus imp = o.openImage(path);
		                 counter++;
		                 if (imp!=null) {
		                     IJ.log(counter+" + "+path);
		                     imp.show();
		                 } else
		                     IJ.log(counter+" - "+path);
		             }
		             
		         } // for
			 
			 return; // if singe images are required
			 
		     } // of if
		    
/** Stack ------------------------------------------------------  */
		    		    
		int width=0,height=0,type=0;
		ImageStack stack = null;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		int k = 0;
		try {
			for (int i=0; i<n; i++) {
			    String path = (dir + list[i]);
				if (path==null)
				     break;
				if (list[i].endsWith(".txt"))
					continue;
				ImagePlus imp = new Opener().openImage(path);
				if (imp!=null && stack==null) {
					width = imp.getWidth();
					height = imp.getHeight();
					type = imp.getType();
					ColorModel cm = imp.getProcessor().getColorModel();
					if (halfSize)
						stack = new ImageStack(width/2, height/2, cm);
					else
						stack = new ImageStack(width, height, cm);
				}
				if (stack!=null)
					k = stack.getSize()+1;
				IJ.showStatus(k + "/" + n);
				IJ.showProgress((double) k / n);
				if (imp==null)
					IJ.log(list[i] + ": unable to open");
				else if (imp.getWidth()!=width || imp.getHeight()!=height)
					IJ.log(list[i] + ": wrong dimensions");
				else if (imp.getType()!=type)
					IJ.log(list[i] + ": wrong type");
				else {
					ImageProcessor ip = imp.getProcessor();
					if (grayscale)
						ip = ip.convertToByte(true);
					if (halfSize)
						ip = ip.resize(width/2, height/2);
					if (ip.getMin()<min) min = ip.getMin();
					if (ip.getMax()>max) max = ip.getMax();
					String label = imp.getTitle();
					String info = (String)imp.getProperty("Info");
					if (info!=null)
						label += "\n" + info;
					stack.addSlice(label, ip);
				}
				System.gc();
			}
		} catch(OutOfMemoryError e) {
			IJ.outOfMemory("FolderOpener");
			stack.trim();
		}
		if (stack!=null && stack.getSize()>0) {
			ImagePlus imp2 = new ImagePlus("Stack", stack);
			if (imp2.getType()==ImagePlus.GRAY16 || imp2.getType()==ImagePlus.GRAY32)
				imp2.getProcessor().setMinAndMax(min, max);
			imp2.show();
		}
		IJ.showProgress(1.0);		    
		} 		    
	}
	
/** Stack ------------------------------------------------------  */		
	
	public boolean scoutengine(ImagePlus img) {
	    String infoProperty = (String)img.getProperty("Info");
		if (infoProperty!=null) {
		    
		      { /** the first IF-Block  */
		      if (pick.equals("AcquisitionTime")) {
			     int acqtimestart = infoProperty.indexOf("0008,0032");				
			         if (acqtimestart ==-1) 
					   {IJ.log("AcquisitionTime tag not found in "+img.getTitle()); return false;}
			     int acqtimeend = infoProperty.indexOf("0008,00",(acqtimestart+2));
			     String acqtime = infoProperty.substring(acqtimestart+29,acqtimeend);
			     int dot = acqtime.indexOf('.');
                 if (dot==-1)
                    {IJ.log("Unable to parse AcquisitionTime tag in "+img.getTitle()); return false;}
/**			  extraction of the dot */
			     String beforedot = acqtime.substring(0, dot);
			     String afterdot = acqtime.substring(dot+1, acqtime.length());
			     acqtime = beforedot.concat(afterdot);
			     whichcase = acqtime.trim();
			  }
		      else if (pick.equals("ImageNumber")) {
			     int imnumstart = infoProperty.indexOf("0020,0013"); 
                 if (imnumstart==-1)  
                    {IJ.log("ImageNumber tag not found in "+img.getTitle()); return false;}
			     int imnumend = infoProperty.indexOf("0020,00",(imnumstart+2));
                 if (imnumend==-1)
                    {IJ.log("Unable to parse ImageNumber tag in "+img.getTitle()); return false;}
			     String imnum = infoProperty.substring(imnumstart+24,imnumend);
				 whichcase = imnum.trim();
			  }
			  else if (pick.equals("ImageTime")) {
			     int imtimestart = infoProperty.indexOf("0008,0033");
                 if (imtimestart==-1)
                    {IJ.log("ImageTime tag not found in "+img.getTitle()); return false;}
			     int imtimeend = infoProperty.indexOf("0008,00",(imtimestart+2));
			     String imtime = infoProperty.substring(imtimestart+22,imtimeend);
			     int dot = imtime.indexOf('.');
/**			  extraction of the dot */
			     String beforedot = imtime.substring(0, dot);
			     String afterdot = imtime.substring(dot+1, imtime.length());
			     imtime = beforedot.concat(afterdot);
			     whichcase = imtime.trim();
			  }
		        } /** of the first IF-Block ------------------------------------------*/
			
		       
			{ /** the second IF-Block  ---------------------------*/
			if (prefix1.equals("None")) {
			  return true;
			  }

	          else if (prefix1.equals("LastFirstname")) {
			     int namestart = infoProperty.indexOf("0010,0010");	
			     int nameend = infoProperty.indexOf("0010,00",(namestart+2));
			     String name = infoProperty.substring(namestart+26,nameend);
			     whichprefix1 = name.trim(); 
			  }
			  
			  else if(prefix1.equals("Age")) {
			     int agestart = infoProperty.indexOf("0010,1010");
			     int ageend = agestart + 30;
			     String age = infoProperty.substring(agestart+25,ageend);
			     whichprefix1 = age.trim();
			  }
			  
			  else if(prefix1.equals("Sex")) {
			     int sexstart = infoProperty.indexOf("0010,0040");	
			     int sexend = sexstart + 27;
			     String sex = infoProperty.substring(sexstart+25,sexend);
			     whichprefix1 = sex.trim();
			  }

			  else if (prefix1.equals("StudyDescription")) {
			     int studystart = infoProperty.indexOf("0008,1030");	
			     int studyend = infoProperty.indexOf("0008,10",(studystart+2));
			     String study = infoProperty.substring(studystart+29,studyend);
			     whichprefix1 = study.trim(); 
			  }
			  
			  else if (prefix1.equals("ProtocolName")) {
			     int protstart = infoProperty.indexOf("0018,1030");	
			     int protend = infoProperty.indexOf("0018,1",(protstart+2));
			     String prot = infoProperty.substring(protstart+25,protend);
			     whichprefix1 = prot.trim(); 
			  }
			  
		        } /** of the second IF-Block ----------------------------------------*/

			
			{ /** the third IF-Block ---------------------------*/
			if (prefix2.equals("None")) {
			  return true;
			  }
	          else if (prefix2.equals("LastFirstname")) {
			     int namestart = infoProperty.indexOf("0010,0010");	
			     int nameend = infoProperty.indexOf("0010,00",(namestart+2));
			     String name = infoProperty.substring(namestart+26,nameend);
			     whichprefix2 = name.trim(); 
			  }
			  
			  else if(prefix2.equals("Age")) {
			     int agestart = infoProperty.indexOf("0010,1010");
			     int ageend = agestart + 30;
			     String age = infoProperty.substring(agestart+25,ageend);
			     whichprefix2 = age.trim();
			  }
			  			  
              else if(prefix2.equals("Sex")) {
                 int sexstart = infoProperty.indexOf("0010,0040");	
                 int sexend = sexstart + 27;
                 String sex = infoProperty.substring(sexstart+25,sexend);
                 whichprefix2 = sex.trim();
			  }
			  
            else if (prefix2.equals("StudyDescription")) {
              int studystart = infoProperty.indexOf("0008,1030");	
              int studyend = infoProperty.indexOf("0008,10",(studystart+2));
              String study = infoProperty.substring(studystart+29,studyend);
              whichprefix2 = study.trim(); 
              }
              
            else if (prefix2.equals("ProtocolName")) {
              int protstart = infoProperty.indexOf("0018,1030");	
              int protend = infoProperty.indexOf("0018,1",(protstart+2));
              String prot = infoProperty.substring(protstart+25,protend);
              whichprefix2 = prot.trim(); 
              }
			  
		        } /** of the third IF-Block ----------------------------------------*/

			
			{ /** the fourth IF-Block ---------------------------*/
			if (prefix3.equals("None")) {
			  return true;
			  }

	        else if (prefix3.equals("LastFirstname")) {
			  int namestart = infoProperty.indexOf("0010,0010");	
			  int nameend = infoProperty.indexOf("0010,00",(namestart+2));
			  String name = infoProperty.substring(namestart+26,nameend);
			  whichprefix3 = name.trim(); 
			  }
			  
			else if(prefix3.equals("Age")) {
			  int agestart = infoProperty.indexOf("0010,1010");
			  int ageend = agestart + 30;
			  String age = infoProperty.substring(agestart+25,ageend);
			  whichprefix3 = age.trim();
			  }
			  
			else if(prefix3.equals("Sex")) {
			  int sexstart = infoProperty.indexOf("0010,0040");	
			  int sexend = sexstart + 27;
			  String sex = infoProperty.substring(sexstart+25,sexend);
			  whichprefix3 = sex.trim();
			  }
			  
			else if (prefix3.equals("StudyDescription")) {
			  int studystart = infoProperty.indexOf("0008,1030");	
			  int studyend = infoProperty.indexOf("0008,10",(studystart+2));
			  String study = infoProperty.substring(studystart+29,studyend);
			  whichprefix3 = study.trim(); 
			  }
			  
			else if (prefix3.equals("ProtocolName")) {
			  int protstart = infoProperty.indexOf("0018,1030");	
			  int protend = infoProperty.indexOf("0018,1",(protstart+2));
			  String prot = infoProperty.substring(protstart+25,protend);
			  whichprefix3 = prot.trim(); 
			  }
			  
		        } /** of the fourth IF-Block ----------------------------------------*/

		    
			} 
			else {
			     IJ.log("No DICOM");
				 return false;
			}	
		return true;
	}
}
