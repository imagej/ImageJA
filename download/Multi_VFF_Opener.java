// ===============================================================================
// 
// Multi_VFF_Opener.java
//
// This ImageJ plugin can be used to open MULTIPLE VFF format files as created by the EVS 900
// microCT system. 
// It should also work for images (2D and 3D) created by GEHC eXplore Locus SP
// (tested with a few examples only).
//
// This program is free software; Therefore, you can redistribute it and/or modify it under the terms
// of the GNU General Public License as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// Michael A. Miller is highly appreciated for his pioneering effort, "VFF_Reader.java" downloadable
// from <http://php.iupui.edu/~mmiller3/ImageJ/>. 
//
// Copyright (C) Shanrong Zhang <shanrong.zhang@utsouthwestern.edu>
// Date: December 23, 2003
// 
// Edited in 2009 by Daniel Hornung <daniel.hornung@ds.mpg.de>
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

public class Multi_VFF_Opener implements PlugIn {

	static File directory;

	public void run(String arg) {
		JFileChooser fc = null;
		try {fc = new JFileChooser();}
		catch (Throwable e) {IJ.error("This plugin requires Java 2 or Swing."); return;}
		fc.setMultiSelectionEnabled(true);
		if (directory==null) {
			String sdir = OpenDialog.getDefaultDirectory();
			if (sdir!=null)
				directory = new File(sdir);
		}
		if (directory!=null)
			fc.setCurrentDirectory(directory);
		int returnVal = fc.showOpenDialog(IJ.getInstance());
		if (returnVal!=JFileChooser.APPROVE_OPTION)
			return;
		File[] files = fc.getSelectedFiles();
		if (files.length==0) {
			files = new File[1];
			files[0] = fc.getSelectedFile();
		}
		for (int i=0; i<files.length; i++) {
			String directory = fc.getCurrentDirectory().getPath()+Prefs.getFileSeparator();
			String name = files[i].getName();
			if (name==null)
				return;
			IJ.showStatus("Opening: " + directory + name);
			ImagePlus imp = load(directory, name);
			if (imp!=null) {
				ImageStack stack = imp.getStack();
				for (int j=1; j<=stack.getSize(); j++) {
					IJ.showStatus("rotating slice " + j +"/" + stack.getSize());
					ImageProcessor ip = stack.getProcessor(j);
					ip.rotate(180.0);
				}
				imp.show();
			}
		}
	}

	public ImagePlus load(String directory, String name) {
		FileInfo fi = new FileInfo(); 
		if ((name == null) || (name == "")) return null;
		IJ.showStatus("Reading vff header from " + directory + name);
		try { fi = readHeader( directory, name );}
		catch (IOException e) { IJ.log("FileLoader: "+ e.getMessage()); }
		FileOpener fo = new FileOpener(fi); 
		ImagePlus imp = fo.open(false);
		return imp; 
	}

	public FileInfo readHeader( String directory, String vfffile ) throws IOException {
		FileInfo fi = new FileInfo();
		File file =  new File( directory+vfffile );
		FileReader fr = new FileReader ( file );
		BufferedReader in = new BufferedReader( fr );
		String line;
		boolean done = false;
		int count = 0;
		String subject = "NA";
		long xdim = 0;
		long ydim = 0;
		long zdim = 1; // any image is at least one layer thick
		int rank = 0;
		long bits = 0;
		float xspacing = 0;
		float yspacing = 0;
		float zspacing = 0;
		float xsize = 0;
		float ysize = 0;
		float zsize = 0;
		float elementsize = 0;
		if ( !file.exists() || !file.canRead(  ) ) {
			IJ.showStatus( "Can't read " + file );
			return(fi);
		}
		if ( file.isDirectory(  ) ) {
			String [] files = file.list(  );
			for (int i=0; i< files.length; i++)
				IJ.showStatus( files[i] );
		}
		else try {
			IJ.log("Parameters for " + vfffile + " : "); 
			while ( ((line = in.readLine(  )) != null ) & !done ) {
				IJ.log( "      VFF header>>> " + line);
				try{
					if ( line.startsWith("subject") ) {
						StringTokenizer st = new StringTokenizer(line, " =;");
						st.nextToken();  // 1st token is 'subject'
						subject = st.nextToken();
					}
					if ( line.startsWith("rank") ) {
						StringTokenizer st = new StringTokenizer(line, " =;");
						st.nextToken();  // 1st token is 'rank'
						rank = Integer.valueOf(st.nextToken()).intValue();
					}
					if ( line.startsWith("bits") ) {
						StringTokenizer st = new StringTokenizer(line, " =;");
						st.nextToken();  // 1st token is 'bits'
						bits = Long.valueOf(st.nextToken()).longValue();
					}
					if ( line.startsWith("elementsize") ) {
						StringTokenizer st = new StringTokenizer(line, " =;");
						st.nextToken();  // 1st token is 'elementsize'
						elementsize = Float.valueOf(st.nextToken()).floatValue();
					}
					if ( line.startsWith("size") ) {
						StringTokenizer st = new StringTokenizer(line, " =;");
						st.nextToken();  // 1st token is 'size'
						xdim = Long.valueOf(st.nextToken()).longValue();  // 2nd token is xdim
						ydim = Long.valueOf(st.nextToken()).longValue();  // 3rd token is ydim
						zdim = Long.valueOf(st.nextToken()).longValue();  // 4th token is zdim
					}
					if ( line.startsWith("spacing") ) {
						StringTokenizer st = new StringTokenizer(line, " =;");
						st.nextToken();  // 1st token is 'spacing'
						xspacing = Float.valueOf(st.nextToken()).floatValue();  // 2nd token is xspacing
						yspacing = Float.valueOf(st.nextToken()).floatValue();  // 3rd token is yspacing
						zspacing = Float.valueOf(st.nextToken()).floatValue();  // 4th token is zspacing
					}
					if (line.equals("\f")) {
						// This (, form feed) seems to mark the last header
						// line for vff files from GEHC microCT devices.
						done = true;
					}
				} catch (NoSuchElementException nsee) {
					// do nothing, not necessarily a fatal error
				}
				count = count + 1;
				if ( count > 26 ) {
					done = true;
				}
			}
		}
		catch ( FileNotFoundException e ) {
			IJ.showStatus( "File Disappeared" );
		}
		xsize = elementsize * xspacing;
		ysize = elementsize * yspacing;
		zsize = elementsize * zspacing;
		IJ.log("      Subject:  " + subject);
		IJ.log("      Rank:     " + rank);
		IJ.log("      Bits:     " + bits);
		IJ.log("      xdim:    " + xdim);
		IJ.log("      ydim:    " + ydim);
		IJ.log("      zdim:    " + zdim);
		IJ.log("      xspacing: " + xspacing);
		IJ.log("      yspacing: " + yspacing);
		IJ.log("      zspacing: " + zspacing);
		IJ.log("      elementsize: " + elementsize);
		IJ.log("      xsize: " + xsize);
		IJ.log("      ysize: " + ysize);
		IJ.log("      zsize: " + zsize);
		fi.fileName = vfffile;
		fi.directory = directory;
		fi.fileFormat = fi.RAW;
		fi.width = (int)xdim;
		fi.height = (int)ydim;
		fi.nImages = (int)zdim;
		fi.pixelWidth = xsize;
		fi.pixelHeight = ysize;
		fi.pixelDepth = zsize;
		fi.intelByteOrder = false;
		fi.fileType = (bits == 8) ? FileInfo.GRAY8 : FileInfo.GRAY16_SIGNED;
		IJ.log("      fi.fileType = "
			   + ((bits == 8) ? "FileInfo.GRAY8" : "FileInfo.GRAY16_SIGNED"));
		fi.unit = "mm";
		fi.offset = (int)(file.length() - xdim*ydim*zdim*bits/8); // file size in bytes
		IJ.log("      fi.offset = " + fi.offset);
		IJ.log("");
		return (fi);
	}
}
