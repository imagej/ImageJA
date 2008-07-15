import java.io.*;
import java.util.*;
import java.awt.*;

import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.io.*;
import ij.measure.*;

//Jim Hull, The University of Washington Chemical Engineering
//Authored 01/04

public class Open_NII implements PlugIn {

	FileInfo nIIFileInfo;
	FileOpener nIIFileOpener;
	ImagePlus nIIImage;
	ImageProcessor nProc;
	Calibration nCal;


	String buf;

	BufferedReader fileInputStream;
	File file;

	public double[] nIIProps = new double[3];

	public void run(String arg) {

		IJ.log("Entering Plugin");
		if (arg.equals("")) {
			OpenDialog od = new OpenDialog("Open Nano II...","");
			String fileName = od.getFileName();

			if (fileName==null)
				return;

			String directory = od.getDirectory();
	        IJ.showStatus("Opening: " + directory + fileName);
	        file = new File(directory+fileName);
		}
		else {
			file = new File(arg);
		}

		//Open the STP file
		openNII(file);

	}	//end run


	public void openNII(File nIIFileName) {

		IJ.log("Entering openNII");

		//Create the file input stream
   		try {
			fileInputStream = new BufferedReader( new FileReader(nIIFileName));
			IJ.showStatus("Opening Buffered Reader");
		}
		catch (FileNotFoundException exception) {
			IJ.showStatus("Buffered Reader Exception");
		}

		//Read the Header
		readHeader(fileInputStream);

		//Close the input stream
		try {
			fileInputStream.close();
			IJ.showStatus("Closing Buffered Reader");
		}
		catch (IOException exception) {
			IJ.showStatus("Buffered Reader Exception");
		}

		//Set Parameters and open each buffer
		nIIFileInfo = new FileInfo();
		nIIFileInfo.fileType = nIIFileInfo.GRAY16_SIGNED;
		nIIFileInfo.fileName = nIIFileName.toString();
		nIIFileInfo.width = (int)nIIProps[0];
		nIIFileInfo.height = (int)nIIProps[0];
		nIIFileInfo.offset = 2048;
		nIIFileInfo.intelByteOrder = true;

		//Open the Image
		nIIFileOpener = new FileOpener(nIIFileInfo);
		nIIImage = nIIFileOpener.open(false);

		nIIImage.setProperty("zV2A",new Double(nIIProps[2]));
		nIIImage.setProperty("lengthX",new Double(nIIProps[1]));
		nIIImage.setProperty("lengthY",new Double(nIIProps[1]));
		nIIImage.setProperty("iname",nIIFileName.getName());

		//convert to value
		nProc = nIIImage.getProcessor().convertToFloat();
		nProc.multiply(nIIProps[2]/65536.0);

		nCal = nIIImage.getCalibration();
		nCal.setUnit("nm");

		nCal.pixelWidth  = nIIProps[1]/nIIProps[0];
		nCal.pixelHeight = nIIProps[1]/nIIProps[0];

		nIIImage.setProcessor(getTitle(nIIImage),nProc);

		nCal.setValueUnit("nm");

		nIIImage.show();

	}	//end nIIOpener


	public void readHeader(BufferedReader in) {

		//Scan through header for file info
		for(int line = 0 ; line<20 ; line++) {

			try {
				buf = new String(in.readLine());}
			catch (IOException exception) {
				IJ.showStatus("IO Exception");
			}

			//Get the buffer info

			if (buf.startsWith("num_samp")) {
				nIIProps[0] = new Double(buf.substring(11).trim()).doubleValue();
				IJ.log(new Double(nIIProps[0]).toString());
				continue;
			}
			if (buf.startsWith("scan_sz")) {
				nIIProps[1] = new Double(buf.substring(10).trim()).doubleValue();
				IJ.log(new Double(nIIProps[1]).toString());
				continue;
			}
			if (buf.startsWith("z_scale")) {
				nIIProps[2] = new Double(buf.substring(9).trim()).doubleValue();
				IJ.log(new Double(nIIProps[2]).toString());
				break;
			}

		}	//end for

	}	//end readHeader

	public String getTitle(ImagePlus img) {
		String title=new String("");
		String sizeX=IJ.d2s(new Double(img.getProperty("lengthX").toString()).doubleValue(),2);
		String sizeY=IJ.d2s(new Double(img.getProperty("lengthY").toString()).doubleValue(),2);
		String imgName = img.getProperty("iname").toString();
		String unit = img.getCalibration().getUnit();

		title=imgName+" "+sizeX+unit+" x "+sizeY+unit;

		return title;
	}	//end title

}	//end class







