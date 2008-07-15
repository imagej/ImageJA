import ij.*;
import ij.io.*;
import ij.plugin.*;
import java.io.*;
import java.util.*;

/**
Opens multi-image 8-bits tiff files created by Leica confocal microscope
systems using each channels own LUT.  Modified by Nico Stuurman June 2000
 */
public class Leica_SP_Reader  implements PlugIn {

	int nr_channels = 1; 

	public void run(String arg) {
		if (IJ.versionLessThan("1.18h"))
			return;	
		OpenDialog od = new OpenDialog("Open Leica SP...", arg);
		String dir = od.getDirectory();
		String name = od.getFileName();
		if (name==null)
			return;
		IJ.showStatus("Opening: " + dir + name);
		try {
		
			FileInfo[] fi =  getFileInfo(dir, name);	
			//ij.IJ.write ("Leica_Reader: " + nr_channels + "\n");
			openStacks(fi);
		} catch (IOException e) {
			String msg = e.getMessage();
			if (msg==null || msg.equals(""))
				msg = ""+e;
			IJ.showMessage("Leica SP Reader", msg);
		}
	}

	FileInfo[] getFileInfo(String directory, String name) throws IOException {
		LeicaTiffDecoder td = new LeicaTiffDecoder(directory, name);
		if (IJ.debugMode) td.enableDebugging();
		FileInfo[] info = td.getTiffInfo();
		nr_channels = td.nr_channels;
		//ij.IJ.write ("in getFileInfo: " + nr_channels + "\n");
		if (info==null)
			throw new IOException("This file does not appear to be in TIFF format.");
		if (IJ.debugMode) // dump tiff tags
			IJ.write(info[0].info);
		return info;
	}
	
	void openStacks(FileInfo[] fi) throws IOException {
		if (fi[0].fileType!=FileInfo.COLOR8)
			throw new IOException("This does not appear to be  a stack of 8-bit color images.");
		int maxStacks = nr_channels;
		ImageStack[] stacks = new ImageStack[maxStacks];
		int width = fi[0].width;
		int height = fi[0].height;
		String name = fi[0].fileName;
		int length_per_channel = fi.length/nr_channels;
		//ij.IJ.write ("Leica_Reader: " + length_per_channel + "\n");

		for (int j=0; j<nr_channels; j++){
    			if (stacks[j]==null)
					 stacks[j] = new ImageStack(width,height);
			for (int i=0; i<length_per_channel; i++) {
				int k = i + j * length_per_channel;
				if (fi[k].width!=width || fi[k].height!=height)
					break;
				FileOpener fo = new FileOpener(fi[k]);
				ImagePlus imp = fo.open(false);
				if (imp!=null)
					stacks[j].addSlice("", imp.getProcessor());
			}
			if (stacks[j]!=null){
				int l = j+1;
				new ImagePlus(name+"(channel "+l+")", stacks[j]).show ();
			}
		}
	}
}


/* This class inherits ImageJ's TiffDecoder and overrides the decodeImageDescription method.
The Leica SP files start their image description with "[GLOBAL]".  The number
of channels in a Leica SP TIFF file is given within the image description as "NumOfVisualisations"=x.

*/ 
class LeicaTiffDecoder extends TiffDecoder{

	public int nr_channels = 1;

	public  LeicaTiffDecoder(String directory, String name) {
		super(directory, name);
	}

	public void decodeImageDescription(byte[] description, FileInfo fi) {	
		if (new String (description,0,8).equals("[GLOBAL]")) {
			if (debugMode) ij.IJ.write ("Leica file detected..." + "\n");
			String file_specs = new String (description);
			if (debugMode) ij.IJ.write(file_specs);
			StringTokenizer st = new StringTokenizer(file_specs, "\n= ");
    			while (st.hasMoreTokens()) {
				if(st.hasMoreTokens()) {
      					String s = st.nextToken();
      					if (s.equals ("NumOfVisualisations")) {
						//ij.IJ.write ("found" +"\n");
						String temp = new String (st.nextToken());
						//ij.IJ.write ("temp: " + temp + "l" + "\n");
						temp = temp.trim();
						try {Integer a = new Integer (temp);
						nr_channels = a.intValue();}
						catch (NumberFormatException e) {return;}
						if (debugMode) ij.IJ.write ("channels detected: " +nr_channels+ "\n");
					break; //out of while loop
					}
				}
			}         
		}
	}

} //class Leica_TiffDecoder
