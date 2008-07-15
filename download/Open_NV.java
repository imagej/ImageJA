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
// Modified on 04/2007 by Philippe Carl, Life science project engineer Veeco Instruments GmbH
// E-mail: pcarl@veeco.de

public class Open_NV implements PlugIn
{
	FileInfo nFileInfo;
	FileOpener nFileOpener;
	ImagePlus nImage;
	ImageProcessor nProc;
	Calibration nCal;
	private int bufferCount=0;

	String buf = "";

	BufferedReader fileInputStream;
	File file;

	private double[]   nVProps   = new double[3];
	private double[][] buffProps = new double[5][8];
	private int colonIndex, unitSkip;

	public void run(String arg)
	{

		//IJ.log("Entering Plugin");
		if (arg.equals(""))
		{
			OpenDialog od = new OpenDialog("Open NV...", "C:\\Philippe\\registration", "");
			String fileName = od.getFileName();

			if (fileName==null)
				return;

			String directory = od.getDirectory();
	        	IJ.showStatus("Opening: " + directory + fileName);
		        file = new File(directory + fileName);
		}
		else
			file = new File(arg);

		// Open the STP file
		openNV(file);

	}	//end run


	public void openNV(File fileName)
	{
		//IJ.log("Entering openStp");

		//int buffers=0;
		double[]   fileProps   = new double[10];
		double[][] bufferProps = new double[10][5];

		//Create the file input stream
   		try
		{
			fileInputStream = new BufferedReader(new FileReader(fileName));
			IJ.showStatus("Opening Buffered Reader");
		}
		catch (FileNotFoundException exception)
		{
			IJ.showStatus("Buffered Reader Exception");
		}

		//Read the Header
		readHeader(fileInputStream);

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
		for(int i = 0; i < bufferCount; i++)
		{

			//Set Parameters and open each buffer
			nFileInfo = new FileInfo();
			nFileInfo.fileType = nFileInfo.GRAY16_SIGNED;
			nFileInfo.fileName = fileName.toString();
			nFileInfo.width  = (int) nVProps[1];
			nFileInfo.height = (int) nVProps[1];
			nFileInfo.offset = (int)buffProps[0][i];
			nFileInfo.intelByteOrder = true;

			//Open the Image
			nFileOpener = new FileOpener(nFileInfo);
			nImage = nFileOpener.open(true);

		}	//end for

	}	//end openNV


	public void readHeader(BufferedReader in)
	{
		//Scan through header for file info
		while(!buf.endsWith("File list end"))
		{
			//IJ.log("buffer count " + new Integer(bufferCount).toString());
			try
			{
				buf = new String(in.readLine());
			}
			catch (IOException exception)
			{
				IJ.showStatus("IO Exception");
			}

			buf=buf.substring(1);
			//IJ.log(buf);
			colonIndex=buf.indexOf(":");
			//IJ.log(new Integer(colonIndex).toString());
			unitSkip=buf.length()-2;
			//IJ.log(new Integer(unitSkip).toString());

			//Get the buffer info
			//Get the header size
			if(buf.startsWith("Data length:") && bufferCount == 0)
			{
				//IJ.log(new Double(buf.substring(colonIndex + 1).trim()).toString());
				nVProps[0] = new Double(buf.substring(colonIndex + 1).trim()).doubleValue();
				IJ.log("Header Length " + new Integer((int) nVProps[0]).toString());
				//bufferCount++;
				continue;
			}

			//Get the number if pixels
			if (buf.startsWith("Lines:") && bufferCount == 0)
			{
				nVProps[1] = new Double(buf.substring(colonIndex + 1).trim()).doubleValue();
				IJ.log("Pixels " + new Integer((int) nVProps[1]).toString());
				continue;
			}

			if (buf.startsWith("Scan size:") && bufferCount == 0)
			{
				nVProps[2] = new Double(buf.substring(colonIndex + 1, unitSkip).trim()).doubleValue();
				IJ.log("Scan Size " + new Double(nVProps[2]).toString());
				continue;
			}

			//Get the image size and offset
			if (buf.startsWith("Data offset:"))
			{
				buffProps[0][bufferCount] = new Integer(buf.substring(colonIndex + 1).trim()).intValue();
				bufferCount += 1;
				IJ.log("Image Offset " + new Integer((int) buffProps[0][bufferCount - 1]).toString());
				continue;
			}
			if (buf.startsWith("Data length:") && bufferCount>0)
			{
				buffProps[1][bufferCount - 1] = new Integer(buf.substring(colonIndex + 1).trim()).intValue();
				IJ.log("Image Sizez " + new Integer((int) buffProps[1][bufferCount - 1]).toString());
				//bufferCount++;
				continue;
			}

		}	//end for

	}	//end readHeader


}	//end class
