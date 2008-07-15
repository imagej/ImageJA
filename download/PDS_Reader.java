import ij.plugin.*;
import java.awt.*;
import java.io.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.measure.*;

/**	Opens and displays PDS images.  Does not work with compressed images */
public class PDS_Reader extends ImagePlus implements PlugIn {

	private static final String TITLE = "PDS Reader";
	private String directory, fileName;
	private DataInputStream f;
	private StringBuffer info = new StringBuffer(512);
	private double bscale, bzero;
	String keyword, value, line = "", sampleType, encodingType="",mapScale="";
	private int recordBytes,bitsPerPixel;

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Open PDS...", arg);
		directory = od.getDirectory();
		fileName = od.getFileName();
		if (fileName==null)
			return;
		IJ.showStatus("Opening: " + directory + fileName);
		FileInfo fi = null;
		try {
			if (!checkFileType(directory+fileName)) {
				IJ.showMessage(TITLE, "This does not appear to be a PDS image file.");
				return;
			}
			fi = getInfo();
		} catch (IOException e) {
			IJ.showMessage(TITLE, ""+e);
			return;
		}
		if (fi!=null && fi.width>0 && fi.height>0) {
			FileOpener fo = new FileOpener(fi);
			ImagePlus imp = fo.open(false);
			ImageProcessor ip = imp.getProcessor();
			setProcessor(fileName, ip);
			setCalibration(imp.getCalibration());
			setProperty("Info", getHeaderInfo());
			if (arg.equals("")) show();
		} //else 
			//IJ.error("This does not appear to be a PDS file.");
		IJ.showStatus("");
	}
	
	FileInfo getInfo() throws IOException {
		FileInfo fi = new FileInfo();
		fi.fileName = fileName;
		fi.directory = directory;
		fi.width = 0;
		fi.height = 0;
		fi.offset = 0;
		
		BufferedReader f = new BufferedReader(new FileReader(directory+fileName));
		while (!line.trim().equals("END")) {//read label until we get to the END statement
			    
		line = f.readLine();
		//System.out.println(line);
		//IJ.write(line);
		if (line == null) break;
		line = line.replace('"',' ');
		if (line.trim().equals("")) continue;
		if (line.indexOf("=") > 0) {
			keyword = line.substring(0,line.indexOf("=")).trim();
			value = line.substring(line.indexOf("=")+1,line.length()).trim().toUpperCase();
			}
		else continue;
		if (value.length() == 0) continue; 
		if (keyword.equals("LINES")) {
			fi.height = getInteger(value);
			continue;
			}
		if (keyword.equals("LINE_SAMPLES")) {
			fi.width = getInteger(value);
			continue;
			}		   
		if (keyword.equals("RECORD_BYTES")) {
			recordBytes = getInteger(value);
			continue;
			}			
		if (keyword.equals("^IMAGE")) {  //will only work if image pointer follows record bytes keyword
			//System.out.println("imagepointer="+value);
			try {fi.offset = Integer.parseInt(value);}
			catch (NumberFormatException e) {
			  if (value.indexOf("(") >= 0) {
			  	fi.fileName = value.trim().substring(value.indexOf("(")+2,value.lastIndexOf(","));
			  	fi.offset = Integer.parseInt(value.substring(value.indexOf(",")+1,value.lastIndexOf(")")));
			  	}
			  else {
			  	fi.fileName = value; 
			  	fi.offset = 0;
			  	} 
			  }
			 // fi.fileName = fi.fileName.substring(0,fi.fileName.lastIndexOf(".")) +".IMG"; } 
			fi.offset = (fi.offset-1) * recordBytes;
			//System.out.println("offset="+String.valueOf(fi.offset));
			continue;
			}			
		if (keyword.equals("SAMPLE_BITS")) {
			bitsPerPixel = getInteger(value);
			//System.out.println("samplebits="+value);
			continue;
			}		
		if (keyword.equals("SAMPLE_TYPE")) {
			sampleType = value;
			//System.out.println("sampletype="+value);
			continue;
			}	
		if (keyword.equals("SCALING_FACTOR")) {
			bscale = getFloat(value);
			continue;
			}		
		if (keyword.equals("OFFSET")) {
			bzero = getFloat(value);
			continue;
			}
		if (keyword.equals("ENCODING_TYPE")) {
			encodingType = value;
			continue;
			}
		if (keyword.equals("MAP_SCALE") 			||
			keyword.equals("MINIMUM_LATITUDE")		||
			keyword.equals("MAXIMUM_LATITUDE")		||
			keyword.equals("MINIMUM_LONGITUDE")		||
			keyword.equals("MAXIMUM_LONGITUDE")		||
			keyword.equals("EASTERNMOST_LONGITUDE")	||
			keyword.equals("WESTERNMOST_LONGITUDE")	||
			keyword.equals("MAP_PROJECTION_TYPE")  
		    ) {
			IJ.write(keyword +" = " + value); // display map scale value
			continue;
			}
		} // end of label parsing
		
		if (bitsPerPixel==8) fi.fileType = FileInfo.GRAY8;
		else if (bitsPerPixel==16) {
			fi.fileType = FileInfo.GRAY16_UNSIGNED;
			if (sampleType.equals("VAX_INTEGER")
			   || sampleType.equals("PC_INTEGER")) 
			   fi.intelByteOrder = true;
			}
		else if (bitsPerPixel==32 && sampleType.equals("UNSIGNED_INTEGER"))
			fi.fileType = FileInfo.GRAY32_INT; 
		else if (bitsPerPixel==-32 && sampleType.equals("REAL"))
			fi.fileType = FileInfo.GRAY32_FLOAT;
		else {
			IJ.showMessage(TITLE, "SAMPLE_BITS must be 8, 16, 32 or -32 (float).");
			f.close();
			return null;
			}
			line = f.readLine();
		if (encodingType.length() > 0 && !encodingType.equals("NONE")) {
			IJ.showMessage(TITLE, "Cannot open PDS compressed images.");
			f.close();
			return null;
			}
		f.close();
		if (fi.fileType==FileInfo.GRAY16_SIGNED && !(bscale==1.0&&bzero==32768.0)) {
			double[] coeff = new double[2];
			coeff[0] = -32768.0;
			coeff[1] = 1.0;
    		fi.calibrationFunction = Calibration.STRAIGHT_LINE;
     		fi.coefficients = coeff;
    		fi.valueUnit = "gray value";
		}
		return fi;
	}

	String getString(int length) throws IOException {
		byte[] b = new byte[length];
		f.read(b);
		return new String(b);
	}

	int getInteger(String s) {
		//s = s.substring(10, 30);
		//s = s.trim();
		return Integer.parseInt(s);
	}

	double getFloat(String s) {
		//s = s.substring(10, 30);
		//s = s.trim();
		Double d;
		try {d = new Double(s);}
		catch (NumberFormatException e){d = null;}
		if (d!=null)
			return(d.doubleValue());
		else
			return 0.0;
	}

	String getHeaderInfo() {
		return new String(info);
	}
	boolean checkFileType(String path) throws IOException {
		InputStream is;
		byte[] buf = new byte[132];
		is = new FileInputStream(path);
		is.read(buf, 0, 132);
		is.close();
		int b0=buf[0]&255, b1=buf[1]&255, b2=buf[2]&255, b3=buf[3]&255;

		// PDS ("CCSD", XXCD, NJPL, XXNJ, PDSX)
		if ((b0==67 && b1==67 && b2==83 && b3==68) || 
		    (b2==67 && b3==67)  ||
		    (b0==78 && b1==74 && b2==80 && b3==76) ||
		    (b2==78 && b3==74)  ||
		    (b0==80 && b1==68 && b2==83))
			return true;
		else
			return false;
	}

}

