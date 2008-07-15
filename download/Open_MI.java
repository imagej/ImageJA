import java.io.*;
import java.util.*;
import java.awt.*;

import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.io.*;
import ij.measure.*;

// Jim Hull, The University of Washington Chemical Engineering
// Authored 01/04
// Modified and extended for the PicoView Software on 03/2008 by Philippe Carl
// E-mail: philippe.carl@tiscali.fr

public class Open_MI implements PlugIn
{

	FileInfo nFileInfo;
	FileOpener nFileOpener;
	ImagePlus nImage;
	ImageProcessor nProc;
	Calibration nCal;


	String buf;

	BufferedReader fileInputStream;
	File file;

	public void run(String arg)
	{

		//IJ.log("Entering Plugin");
		if (arg.equals(""))
		{
			OpenDialog od = new OpenDialog("Open STP...","");
			String fileName = od.getFileName();

			if (fileName==null)
				return;

			String directory = od.getDirectory();
	        	IJ.showStatus("Opening: " + directory + fileName);
		        file = new File(directory + fileName);
		}
		else
		{
			file = new File(arg);
		}

		//Open the STP file
		openSTP(file);

	}	//end run


	public void openSTP(File stpFileName)
	{
		//IJ.log("Entering openStp");

		int        buffers     = 0;
		double[]   fileData    = new double[4];
		double[]   bufferRange = new double[6];
		double[]   fileProps   = new double[10];
		double[][] bufferProps = new double[10][5];
		String     fileUnit    = "";
		String[]   bufferLabel = new String[6];
		String[]   bufferUnit  = new String[6];

		//Create the file input stream
		try
		{
			fileInputStream = new BufferedReader( new FileReader(stpFileName));
			IJ.showStatus("Opening Buffered Reader");
		}
		catch (FileNotFoundException exception)
		{
			IJ.showStatus("Buffered Reader Exception");
		}

		//Figure out version of software (PicoScan or PicoView)
		try
		{
			buf = new String(fileInputStream.readLine());
		}
		catch (IOException exception)
		{
			IJ.showStatus("IO Exception");
		}

		if (buf.startsWith("fileType      "))
		{
			//Read the Header
			buffers = readHeaderPicoView(fileInputStream, fileData, bufferLabel, bufferUnit, bufferRange);

			//Close the input stream
			try
			{
				fileInputStream.close();
				IJ.showStatus("Closing Buffered Reader");
			}
			catch (IOException exception)
			{
				IJ.showStatus("Buffered Reader Exception");
			}

			//Set up the units
			if (fileData[2] <= 100.0)
				fileUnit = "Å";
			if (fileData[2] > 100.0 && fileData[2] <= 10000.0 )
			{
				fileUnit = "nm";
				fileData[2] = fileData[2] / 10.0;
				fileData[3] = fileData[3] / 10.0;
			}
			if (fileData[2] > 10000.00)
			{
				fileUnit = "µm";
				fileData[2] = fileData[2] / 10000.0;
				fileData[3] = fileData[3] / 10000.0;
			}

			//Set up the images
			for(int i = 0; i != buffers; i++)
			{
				//Set Parameters and open each buffer
				nFileInfo = new FileInfo();
				nFileInfo.fileType = nFileInfo.GRAY16_SIGNED;
				nFileInfo.fileName = stpFileName.toString();
				nFileInfo.width    = (int) fileData[0];
				nFileInfo.height   = (int) fileData[1];
				nFileInfo.offset   = (int) (stpFileName.length() - fileData[0] * fileData[1] * 2 * (buffers - i));
				nFileInfo.intelByteOrder = true;

				//Open the Image
				nFileOpener = new FileOpener(nFileInfo);
				nImage = nFileOpener.open(false);

				//set the image Properties
				nImage.setProperty("iname", stpFileName.getName());

				//convert to value
				nProc = nImage.getProcessor().convertToFloat();
				nProc.add     (-32768.0);
				nProc.multiply(bufferRange[i] / 65535.0);
				nCal  = nImage.getCalibration();

				//Set length scale
				nCal.setUnit(fileUnit);
				nImage.setProperty("lengthX", new Double(fileData[2]));
				nImage.setProperty("lengthY", new Double(fileData[3]));
				nImage.setProperty("label"  , new String(bufferLabel[i]));
				nImage.setProperty("unit"   , new String(bufferUnit [i]));
				nCal.pixelWidth  = (fileData[2] / fileData[0]);
				nCal.pixelHeight = (fileData[3] / fileData[1]);
				nImage.setProcessor(getTitle(nImage, true), nProc);
				nImage.getProcessor().flipVertical();
				nImage.show();
			}	//end for
		}	// end choice picoView
		else
		{
			//Read the Header
			buffers=readHeaderPicoScan(fileInputStream,fileProps,bufferProps);

			//Close the input stream
			try
			{
				fileInputStream.close();
				IJ.showStatus("Closing Buffered Reader");
			}
			catch (IOException exception)
			{
				IJ.showStatus("Buffered Reader Exception");
			}

			//Set up the images
			for(int i=0;i<buffers;i++)
			{
				//Set Parameters and open each buffer
				nFileInfo = new FileInfo();
				nFileInfo.fileType = nFileInfo.GRAY16_UNSIGNED;
				nFileInfo.fileName = stpFileName.toString();
				nFileInfo.width    = (int) bufferProps[5][i];
				nFileInfo.height   = (int) bufferProps[6][i];
				nFileInfo.offset   = (int) (stpFileName.length() - bufferProps[5][i] * bufferProps[6][i] * 2 * (buffers - i));
				nFileInfo.intelByteOrder = true;

				//Open the Image
				nFileOpener = new FileOpener(nFileInfo);
				nImage = nFileOpener.open(false);

				//set the image Properties
				nImage.setProperty("mode", new Double(fileProps[0]));
				nImage.setProperty("zV2A", new Double(fileProps[1]));
				nImage.setProperty("maxZVolt", new Double(fileProps[2]));
				nImage.setProperty("spt", new Double(fileProps[3]));
				nImage.setProperty("tau_i", new Double(fileProps[4]));
				nImage.setProperty("Kp", new Double(fileProps[5]));
				nImage.setProperty("deltaVz", new Double(fileProps[6]));
				nImage.setProperty("lineFreq", new Double(fileProps[7]));
				nImage.setProperty("iname", stpFileName.getName());
				nImage.setProperty("source", new Double(bufferProps[0][i]));
				nImage.setProperty("scan_dir", new Double(bufferProps[1][i]));
				nImage.setProperty("collect_mode", new Double(bufferProps[2][i]));

				//convert to value
				nProc = nImage.getProcessor().convertToFloat();

				if (bufferProps[0][i] == 1 || bufferProps[0][i] == 9 || bufferProps[0][i] == 10)
					nProc.multiply(2.0 * fileProps[1] * fileProps[6] / 655350.0);
				else
				{
					nProc.add(-1.0 * 32768.0);
					nProc.multiply(10.0 / 32768.0);
				}

				nCal = nImage.getCalibration();
				if (bufferProps[3][i] <= 100.0)
					nCal.setUnit("Å");
				if (bufferProps[3][i] > 100.0 && bufferProps[3][i] <=10000.0 )
				{
					nCal.setUnit("nm");
					bufferProps[3][i] = bufferProps[3][i]/10.0;
					bufferProps[4][i] = bufferProps[4][i]/10.0;
				}
				if (bufferProps[3][i] > 10000.00)
				{
					nCal.setUnit("µm");
					bufferProps[3][i] = bufferProps[3][i]/10000.0;
					bufferProps[4][i] = bufferProps[4][i]/10000.0;
				}

				//Set length scale
				nImage.setProperty("lengthX", new Double(bufferProps[3][i]));
				nImage.setProperty("lengthY", new Double(bufferProps[4][i]));
				nCal.pixelWidth  = bufferProps[3][i] / bufferProps[5][i];
				nCal.pixelHeight = bufferProps[4][i] / bufferProps[6][i];
				nImage.setProcessor(getTitle(nImage, false), nProc);
	
				//setup the calibration
				if (bufferProps[0][i] == 1 || bufferProps[0][i] == 9 || bufferProps[0][i] == 10)
					nCal.setValueUnit("nm");
				else
					nCal.setValueUnit("V");
	
				nImage.show();
			}	//end for
		}	// end choice PicoScan
	}	//end openSTP

	public int readHeaderPicoView(BufferedReader in, double[] fileData, String[] bufferLabel, String[] bufferUnit, double[] bufferRange)
	{

		int bufferCount = 0;
		String mode;
		//Scan through header for file info
		for(int line = 0 ; line != 300 ; line++)
		{
			try
			{
				buf = new String(in.readLine());
			}
			catch (IOException exception)
			{
				IJ.showStatus("IO Exception");
			}

			//Get the buffer info

			if (buf.startsWith("xPixels       "))
			{
				fileData[0] = new Double(buf.substring(14)).doubleValue();
				continue;
			}
			if (buf.startsWith("yPixels       "))
			{
				fileData[1] = new Double(buf.substring(14)).doubleValue();
				continue;
			}
			if (buf.startsWith("xLength       "))
			{
				fileData[2] = (new Double(buf.substring(14)).doubleValue()) * 1e10;
				continue;
			}
			if (buf.startsWith("yLength       "))
			{
				fileData[3] = (new Double(buf.substring(14)).doubleValue()) * 1e10;
				continue;
			}
			if (buf.startsWith("bufferLabel   "))
			{
				bufferCount ++;
				bufferLabel[bufferCount - 1] = buf.substring(14);
				continue;
			}
			if (buf.startsWith("bufferUnit    "))
			{
				bufferUnit[bufferCount - 1] = buf.substring(14);
				continue;
			}
			if (buf.startsWith("bufferRange   "))
			{
				bufferRange[bufferCount - 1] = new Double(buf.substring(14)).doubleValue();
				continue;
			}
			if (buf.startsWith("data          "))
			{
				break;
			}
		}	//end for

		return bufferCount;

	}	//end readHeaderPicoView

	public int readHeaderPicoScan(BufferedReader in,double[] stpProps,double[][] buffProps) {

		int bufferCount=0;
		String mode;
		stpProps[0] = 0.0;
		//Scan through header for file info
		for(int line = 0 ; line<300 ; line++)
		{
			try
			{
				buf = new String(in.readLine());
			}
			catch (IOException exception)
			{
				IJ.showStatus("IO Exception");
			}

			//Get the buffer info
			if (buf.startsWith("sub_mode"))
			{
				mode = buf.substring(14);
				if (mode.equals("M")) {stpProps[0] = 2.0;}
				if (mode.equals("0")) {stpProps[0] = 1.0;}
			}
			if (buf.startsWith("z_v_to_angs"))
			{
				stpProps[1] = new Double(buf.substring(13)).doubleValue();
				continue;
			}
			if (buf.startsWith("max_z_volt"))
			{
				stpProps[2] = new Double(buf.substring(13)).doubleValue();
				continue;
			}
			if (buf.startsWith("buffer_id"))
			{
				bufferCount ++;
				continue;
			}
			if (bufferCount == 1 && buf.startsWith("tip_bias"))
			{
				stpProps[3] = new Double(buf.substring(13)).doubleValue();
				continue;
			}
			if (bufferCount == 1 && buf.startsWith("i_servo_gain"))
			{
				stpProps[4] = new Double(buf.substring(13)).doubleValue();
				continue;
			}
			if (bufferCount == 1 && buf.startsWith("p_servo_gain"))
			{
				stpProps[5] = new Double(buf.substring(13)).doubleValue();
				continue;
			}
			if (bufferCount == 1 && buf.startsWith("servo_range"))
			{
				stpProps[6] = new Double(buf.substring(13)).doubleValue();
				continue;
			}
			if (bufferCount == 1 && buf.startsWith("line_freq"))
			{
				stpProps[7] = new Double(buf.substring(13)).doubleValue();
				continue;
			}
			if (buf.startsWith("source_mode"))
			{
				buffProps[0][bufferCount-1] = new Double(buf.substring(14)).doubleValue();
				if (buffProps[0][bufferCount-1] == 14.0)
					stpProps[0] = 1.0;
				continue;
			}
			if (buf.startsWith("scan_dir"))
			{
				buffProps[1][bufferCount-1] = new Double(buf.substring(buf.length()-1)).doubleValue();
				continue;
			}
			if (buf.startsWith("collect_mode"))
			{
				buffProps[2][bufferCount-1] = new Double(buf.substring(buf.length()-1)).doubleValue();
				continue;
			}
			if (buf.startsWith("length_x"))
			{
				buffProps[3][bufferCount-1] = new Double(buf.substring(13)).doubleValue();
				continue;
			}
			if (buf.startsWith("length_y"))
			{
				buffProps[4][bufferCount-1] = new Double(buf.substring(13)).doubleValue();
				continue;
			}
			if (buf.startsWith("samples_x"))
			{
				buffProps[5][bufferCount-1] = new Double(buf.substring(buf.length()-4)).doubleValue();
				continue;
			}
			if (buf.startsWith("samples_y"))
			{
				buffProps[6][bufferCount-1] = new Double(buf.substring(buf.length()-4)).doubleValue();
				continue;
			}
			if (buf.startsWith("Data_section"))
			{
				break;
			}

		}	//end for

		return bufferCount;

	}	//end readHeaderPicoScan

	public String getTitle(ImagePlus img, boolean picoView)
	{

		String title   = new String("");
		String sizeX   = IJ.d2s(new Double(img.getProperty("lengthX").toString()).doubleValue(),2);
		String sizeY   = IJ.d2s(new Double(img.getProperty("lengthY").toString()).doubleValue(),2);
		String imgName = img.getProperty("iname").toString();
		String unit    = img.getCalibration().getUnit();

		if(picoView)
		{
			String mode    = img.getProperty("label").toString();
			String unit2   = img.getProperty("unit").toString();

			title = imgName + " - " + sizeX + " " + unit + " x " + sizeY + " " + unit + " - " + mode + "(" + unit2 + ")";
		}
		else
		{
			String mode = "";
			String source = "";
			double m = new Double(img.getProperty("mode").toString()).doubleValue();
			double s = new Double(img.getProperty("source").toString()).doubleValue();

			mode=(m==0.0)?"CNT":((m==1.0)?"PFM":"MAC");

			if(mode.equals("CNT")) {source=(s==1.0)?"TOPO":"DEF";}
			else if(mode.equals("PFM")) {source=(s==1.0)?"TOPO":"ADH";}
			else {source=(s==1.0)?"TOPO":((s==2.0)?"AMP":"PHASE");}

			title = imgName + " " + sizeX + unit + " x " + sizeY + unit + " " + mode + " " + source;
		}

		return title;
	}

}	//end class
