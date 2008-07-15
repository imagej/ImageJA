import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import java.awt.*;
import java.io.*;
import ij.plugin.*;
import ij.measure.*;


public class Leica_TIFF_sequence implements PlugIn {
	String dir;
	String name;
	String separator;
	int count=0;
	String line="";
	String title = "";
	Calibration cal;
	String notes;
	//int rows=15;
	int rows= (int)Prefs.get("leica_rows.int",15);

public void run(String arg) 
	{
	// GJ 2006-02-23: ImagePlus.close() present from IJ 1.35m
	if (IJ.versionLessThan("1.35m"))
		return;


	if ( IJ.altKeyDown()) {
			GenericDialog gd0 = new GenericDialog("Leica TIFF series options");
			gd0.addNumericField("Set maximum number of rows in dialog", rows,0);
			gd0.showDialog();
			if (gd0.wasCanceled())	return ;
			rows = (int)gd0.getNextNumber();
			Prefs.set("leica_rows.int",rows);	
			}

	ImagePlus imp;
	int width, height, bits, slices, channels;
	double xCal, zCal, yCal, chn, sect, tpoints, tCal;
	String strXcal, strZcal, strYcal,mode ;
	String  strUnits = ""; 
	// GJ 2006-02-23: Pass arg to Leica OpenDialog in case we are given a path
	OpenDialog od = new OpenDialog("Leica List Opener", arg);
        	name = od.getFileName();
	boolean split = true;
        	if (name==null)	return;
        	dir = od.getDirectory();
	//IJ.showMessage("Dir "+dir);
        	separator = System.getProperty("file.separator");
	String[] list = new File(dir).list();
	String path = dir+separator+name;
	if ((findText("Leica",0))=="") {
				IJ.showMessage("Requires Leica TXT file"); return;}
		
	String nImagesStr = findText("Number of Images:",0);
	nImagesStr = nImagesStr.substring(18,nImagesStr.length()-1);

	int nImages = Integer.valueOf(nImagesStr).intValue(); 
	String [] titleArray = new String [nImages];
	boolean [] doOpen = new boolean[nImages];
	GenericDialog gd = new GenericDialog("Select Images to Open");
	gd.addCheckbox("Split channels?", split);
	gd.addMessage(name+" contains "+nImages+" image series");

	for (int i=0; i<nImages; i++)
		{title = "";
		IJ.showStatus("Reading "+ name);
		title = findText("Series Name:", i);	
		title = title.substring(13,title.length()-1);
		titleArray[i]=title;
		doOpen[i]=false;
		// GJ 2006-02-23: Default to having the first series selected for opening
		//gd.addCheckbox(title,i==0?true:false);
		//gd.addCheckbox(title,false);
		}

int columns = (int)Math.round(0.5+(double)nImages/(double)rows);

rows =(int)Math.round((double)nImages/(double)columns);

	gd.addCheckboxGroup(rows, columns, titleArray, doOpen);

	gd.showDialog();
		if (gd.wasCanceled())	return ;

	split = gd.getNextBoolean();
	for (int i=0; i<nImages; i++)
		{
		if (gd.getNextBoolean())
			{
			IJ.showStatus("Reading "+ name);
			strXcal = findText("Voxel-Width",i);		
			if (strXcal!="") strUnits = strXcal.substring(14, strXcal.length()-13+1);
			
			xCal= getDouble(findText("Voxel-Width",i), 17, 0);; 

			//IJ.showMessage("W "+findText("Voxel-Width",i));

			yCal= getDouble(findText("Voxel-Height",i),18, 0);
			//IJ.showMessage("H "+findText("Voxel-Height",i));
			tCal=1;zCal=1; tpoints = 1;
			chn = getDouble(findText("Channels", i),10,0);  
			
			
			//catch examples where multi-channle images have channels=1...
			if (chn ==1) chn =countLUT(i);
			
			//IJ.showMessage("channels "+chn);
			mode = findText("ScanMode",i);
			//IJ.showMessage(mode);
			tpoints=1; sect = 1; 
			String [] col = new String[(int)chn];
			String sCol="";
			int nIm = list.length;
			String nameInc = "_"+titleArray[i];

			for (int c=0; c<chn; c++)
				{sCol =findLUT(c, i); 
				col[c] =sCol;
				}
			if(mode!="")
				{
				if (mode.indexOf("z")>0)
					{sect = getDouble(findText("Sections", i),10,0);  
					
					zCal= getDouble(findText("Voxel-Depth",i), 18, 0);

					nameInc+="_";
					}
					if (mode.substring(0, mode.length()-10).indexOf("t")>0)
						{tpoints =  getDouble(findText("Iterations", i),18,0); 
						tCal =  getDouble(findText("Delay[ms]",i), 20, 0);
						if(mode.indexOf("z")<0) nameInc+="_";
						}
				}
			String description = findText("Description:",i);
			
			if (description.indexOf("Viewer")>0) 
				{nameInc = nameInc.substring(0,nameInc.length()-1);
				nIm=1;
				//IJ.showMessage("is copied");
				}
		//	IJ.showMessage(nameInc);
			notes = getNotes(i);
		//	IJ.write("    IJ.run(Image Sequence..,open=["+dir+separator+titleArray[i]+"] number="+nIm+" starting=1 increment=1 scale=100 file=["+nameInc+"]");

			IJ.run("Image Sequence...", "open=["+dir+separator+titleArray[i]+"] number="+nIm+" starting=1 increment=1 scale=100 file=["+nameInc+"]");			
			imp = WindowManager.getCurrentImage();
			imp.setTitle(titleArray[i]);
			cal = imp.getCalibration();
			cal.pixelWidth = xCal;
			cal.pixelHeight = yCal;
			cal.pixelDepth = zCal;
			cal.setUnit(strUnits);		
	//		if (mode.substring(0, mode.length()-10).indexOf("t")>0) cal.frameInterval=tCal/1000; 
			imp.setCalibration(cal);			
			if (chn*sect*tpoints == imp.getStackSize()) imp.setDimensions((int)chn, (int)sect, (int)tpoints);
			imp.setProperty("Info", notes);
			boolean dispose = false;
			if (split&&chn>1&&imp.getStackSize()>1)
				{
				for (int j=1; j<=(int)chn; j++)
					{deInterleave(imp, (int)sect, j , (int)chn, cal, titleArray[i], col[(int)j-1]);
					dispose = true;}
				}
			
			if (dispose) imp.close();		
			}
		 }
	IJ.showStatus("Done");
	System.gc();
	}


public double getDouble(String text, int start, int inFromEnd)
	{
	double d;
	if (text==""||text.indexOf("NEXT")>0) 
		{
		d=1;
		}
	else
		{
		String text2 =text.substring((start), text.length()-(inFromEnd+1));
		//IJ.showMessage(text2);
		d = Double.valueOf(text2.trim()).doubleValue(); 
		}
	return(d);
	}

public String getNotes( int imageNumber)
	{
	try
		{BufferedReader r = new BufferedReader(new FileReader(dir+name));
            		String textVal="";
		String notes="";
		line = r.readLine();
		int n=0;
		int j=0;
		while ((j<imageNumber))
			{
			line = r.readLine();
			if(line.indexOf(" NEXT IMAGE ")>0) j++;
			}
		line = r.readLine();
	            	while ((line.indexOf(" NEXT IMAGE ")<0)||(line==null) )
			{ line =  r.readLine();
			IJ.showStatus("Reading notes");	
			notes = notes+"\n"+line;			
		           	if (line==null) break;
			}
		r.close();
		return(notes);
		}	
	catch (IOException e) 
		{
		IJ.error(""+e);
		return("Findtext error!");
	       	}
	}

public String findLUT(int chn, int imageNumber)
	{
	try
		{BufferedReader r = new BufferedReader(new FileReader(dir+name));
            		String textVal="";
		line = r.readLine();
		int n=0;
		int j=0;
//find the series
		while ((j<imageNumber))
			{
			line = r.readLine();
			if(line.indexOf(" NEXT IMAGE ")>0) j++;
			if (line==null) break;
			}
	            	while ((line.indexOf("LUT_"+chn)<0))
			{
			line = r.readLine();			
			if (line==null) break;
					
			if (line.indexOf(" NEXT IMAGE")>0) {line=null; break;}
			}


		if (line!=null)
			{
			line = r.readLine();
			line = line.substring(25, line.length()-1);
			}
		else
			line = "";

		r.close();
		return(line);
		}	
	catch (IOException e) 
		{	
		IJ.error(""+e);
		return("Findtext error!");
	       	}
	}	

public int countLUT(int imageNumber)
	{
	try
		{BufferedReader r = new BufferedReader(new FileReader(dir+name));
            		String textVal="";
		line = r.readLine();
		int n=0;
		int j=0;
//skip to image j
		while ((j<imageNumber))
			{
			line = r.readLine();
			if(line.indexOf("LUT DESCRIPTION #"+imageNumber)<0) j++;
			if (line==null) break;
			}
		
	            	while ((line.indexOf(" NEXT IMAGE")<0))
			{
			line = r.readLine();	
			if (line.indexOf("LUT_"+n)!=-1)
				{ n++;
				if (line==null) break;
				//IJ.showMessage("found LUT");
				}
			}	
		r.close();
		return(n);
		}	

	catch (IOException e) 
		{
		IJ.error(""+e);
		return(1);
	       	}
	}	

public String findText(String text, int imageNumber)
	{
	try
		{BufferedReader r = new BufferedReader(new FileReader(dir+name));
            		String textVal="";
		line = r.readLine();
		int n=0;
		int j=0;
//find the series
		while ((j<imageNumber))
			{
			line = r.readLine();
			if(line.indexOf(" NEXT IMAGE ")>0) j++;
		//	IJ.write(line);
			}

	            	while ((line.indexOf(text)<0))
			{
			line = r.readLine();			
			if (line==null) break;
			if (line.indexOf(" NEXT IMAGE")>0) break;
			}
		r.close();
		if(line==null||line.indexOf("NEXT IMAGE")>0) line = "";
		return(line);
		}	
	catch (IOException e) 
		{
		IJ.error(""+e);
		return("Findtext error!");
	       	}
	}	

public ImagePlus deInterleave(ImagePlus imp, int sections, int chn, int tCh, Calibration cal, String name, String col)
	{
	ImagePlus imp2;
	ImageStack img1 = imp.getStack();
	ImageStack img2 = new ImageStack(imp.getWidth(), imp.getHeight());
	int nSlices = (int)((int)imp.getStackSize()/(int)tCh);
	ImageProcessor ip = imp.getProcessor();
	for (int s=0; s<nSlices; s++)
		{
		ip = img1.getProcessor(chn+(tCh*s));
		img2.addSlice(null, ip);	
		}
	new ImagePlus(name+ " "+col,img2).show();

	imp2 = WindowManager.getCurrentImage();

	if((col.indexOf("Red")<0)&& (col.indexOf("Green")<0)&&(col.indexOf("Blue")<0)){
					//IJ.showMessage(col);				
					col = "Grays";

					}
	
	if(imp2.getType()!=imp2.COLOR_RGB)	 IJ.run(col);
	imp2.setCalibration(cal);	
	imp2.updateAndDraw();	
	imp2.getWindow().repaint();
	return (imp2);
	}

}
