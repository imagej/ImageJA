// ===============================================================================
// 
// Multi_FDF_Opener.java
//
// This ImageJ plugin can be used to open MULTIPLE Varian imaging files in FDF format
// (FDF = Flexible Data Format). 

// This program is free distributed in the hope that it will be useful to those who are interested
// in processing MRI images (Varian) by ImageJ, but WITHOUT ANY WARRANTY; 
// without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE.  See the GNU General Public License for more details.
//
// This program is free software; Therefore, you can redistribute it and/or modify it under the terms
// of the GNU General Public License as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// Michael A. Miller is highly appreciated for his pioneering effort, "FDF_Reader.java" downloadable
// from <http://php.iupui.edu/~mmiller3/ImageJ/>. 
//
// Copyright (C) Shanrong Zhang <shanrong.zhang@utsouthwestern.edu>
// Date: December 23, 2003
//
// ================================================================================

import java.io.*;
import java.util.*;
import java.lang.Math.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.filechooser.*;

import ij.*; 
import ij.plugin.*;
import ij.process.*;
import ij.io.*;
import ij.measure.*;

public class Multi_FDF_Opener implements PlugIn {

	static File directory;

	public void run(String arg) {
		JFileChooser fc = null;
		try {fc = new JFileChooser();}
		catch (Throwable e) {IJ.error("This plugin requires Java 2 or Swing."); return;}
		fc.setMultiSelectionEnabled(true);
		if (directory == null) {
			String sdir = OpenDialog.getDefaultDirectory();
			if (sdir != null)
				directory = new File(sdir);
		}
		if (directory != null)
			fc.setCurrentDirectory(directory);
		int returnVal = fc.showOpenDialog(IJ.getInstance());
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return;
		File[] files = fc.getSelectedFiles();
		if (files.length == 0) {
			files = new File[1];
			files[0] = fc.getSelectedFile();
		}
		for (int i=0; i<files.length; i++) {
			String directory = fc.getCurrentDirectory().getPath()+Prefs.getFileSeparator();
			String name = files[i].getName();
			if (name == null)
				return;
			IJ.showStatus("Opening: " + directory + name);
			ImagePlus imp = load(directory, name);
			if (imp != null) {
				ImageProcessor ip = imp.getProcessor();
				ip.rotate(180);	// Usually, Varian image should be rotated 180 on PC versus Unix.
				imp.show();
			}
		}
	}
    
	public ImagePlus load(String directory, String name) {
		FileInfo fi = new FileInfo(); 
		if ((name == null) || (name == "")) return null;
		IJ.showStatus("Reading fdf header from " + directory + name);
		try {fi = readHeader( directory, name );}
		catch (IOException e) {IJ.log("FileLoader: "+ e.getMessage());}
		FileOpener fo = new FileOpener(fi);  
 		ImagePlus imp = fo.open(false);
		return imp; 
	}

	public FileInfo readHeader(String directory, String fdffile) throws IOException { 
		FileInfo fi = new FileInfo();
		File file =  new File( directory + fdffile );
		FileReader fr = new FileReader ( file );
		BufferedReader in = new BufferedReader( fr );
		String line;
		boolean done = false;
		int count = 0;
		String subject = "NA";
		String spatial_rank = "NA";
		long xdim = 0;
		long ydim = 0;
		long zdim = 1;
		int rank = 0;
		long bits = 0;
		float xspan = 0;
		float yspan = 0;
		float zspan = 0;
		float xsize = 0;
		float ysize = 0;
		float zsize = 0;
		long bigEndian = 1;
		float elementsize = 0;
		
		String type;
		String name;
		
		
		
		
		long arrayIndex;
		if ( !file.exists() || !file.canRead() ) {
			IJ.showStatus( "Can't read " + file );
			return(fi);
		}
		if ( file.isDirectory() ) {
			String [] files = file.list();
			for (int i=0; i< files.length; i++)
				IJ.showStatus( files[i] );
		}
		else try {
			while ( ((line = in.readLine()) != null ) & !done ) {
				IJ.log( "      FDF header>>> " + line);
				if( line.length() == 0 ) {
					IJ.log("line is empty");
				} else {
					StringTokenizer st = new StringTokenizer( line, " *=;,{}[]\"");
					type = st.nextToken();
					if( type.equals("int") ) {
					// process ints
						name = st.nextToken();
						if( name.equals("bigendian") ) {
							bigEndian = Long.valueOf(st.nextToken()).longValue();
						} else	if( name.equals("checksum") ) {
							done = true;
						}
					} else if( type.equals("float") ) {
					// process float
						name = st.nextToken();
						if( name.equals("matrix") ) {
							if( spatial_rank.equals("2dfov") ) {
								xdim = Long.valueOf(st.nextToken()).longValue();
								ydim = Long.valueOf(st.nextToken()).longValue();
							} else if( spatial_rank.equals("3dfov") ) {
								xdim = Long.valueOf(st.nextToken()).longValue();
								ydim = Long.valueOf(st.nextToken()).longValue();
								zdim = Long.valueOf(st.nextToken()).longValue();
							}
						} else if( name.equals("span") ) { 
							if( spatial_rank.equals("2dfov") ) {
								xspan = Float.valueOf(st.nextToken()).floatValue();
								yspan = Float.valueOf(st.nextToken()).floatValue();
							} else if( spatial_rank.equals("3dfov") ) {
								xspan = Float.valueOf(st.nextToken()).floatValue();
								yspan = Float.valueOf(st.nextToken()).floatValue();
								zspan = Float.valueOf(st.nextToken()).floatValue();
							}
						} else if( name.equals("bigendian") ) {
							bigEndian = Long.valueOf(st.nextToken()).longValue();
						}
					} else if( type.equals("char") ) {
						name = st.nextToken();
						if( name.equals("spatial_rank") ) {
							spatial_rank = st.nextToken();
						}
					}
					count = count + 1;
				}
			}
		}
		catch ( FileNotFoundException e ) {
			IJ.showStatus( "File Disappeared" );
		}
		xsize = 10 * xspan / xdim;
		ysize = 10 * yspan / ydim;
		zsize = 10 * zspan / zdim;
			fi.fileName = fdffile;
		fi.directory = directory;
		fi.fileFormat = fi.RAW;
		fi.width = (int)xdim;
		fi.height = (int)ydim;
		fi.nImages = (int)zdim;
		fi.pixelWidth = xsize;
		fi.pixelHeight = ysize;
		fi.pixelDepth = zsize;
		fi.intelByteOrder = bigEndian == 0 ? true: false;
		fi.fileType = FileInfo.GRAY32_FLOAT; 
		fi.unit = "mm";
		fi.offset = (int)(file.length() - xdim*ydim*zdim*4); 

		IJ.log(" intelByteOrder: " + fi.intelByteOrder);
		IJ.log("      xdim:  " + xdim);
		IJ.log("      ydim:  " + ydim);
		IJ.log("      zdim:  " + zdim);
		IJ.log("      xspan: " + xspan);
		IJ.log("      yspan: " + yspan);
		IJ.log("      zspan: " + zspan);
		IJ.log("      xsize: " + xsize);
		IJ.log("      ysize: " + ysize);
		IJ.log("      zsize: " + zsize);
		IJ.log(" file length: " + file.length() );
		IJ.log(" data offset: " + fi.offset );

//		IJ.log("      fi.offset = " + fi.offset);
//		IJ.log("");
		return (fi);
	}
}
