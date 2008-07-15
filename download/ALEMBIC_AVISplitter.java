import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.Animator;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

//
// ALEMBIC_AVISplitter v. 0.2:
// imports avi sections into stacks
// based on AVI_Reader plugin
// Copyright 2003 Marco Caimi for ALEMBIC
//
// History:
// v. 0.1 first version
// v. 0.2 added swing slider, reorganized classes to support quasi-editing
// added delay in play mode, contrain to segment play, repeat and ping-pong play.
//
public class ALEMBIC_AVISplitter implements PlugIn {

	class AVIStreamHeader
	{
	  private String fccStreamHandler;
	  private int dwStreamFlags;
	  private int dwStreamReserved1;
	  private int dwStreamInitialFrames;
	  private int dwStreamScale;
	  private int dwStreamRate;
	  private int dwStreamStart;
	  private int dwStreamLength;
	  private int dwStreamSuggestedBufferSize;
	  private int dwStreamQuality;
	  private int dwStreamSampleSize;

	  private void readData() throws Exception, IOException
	  {
		  	  try
			  {
				fccStreamHandler = readStringBytes();
				dwStreamFlags = readInt();
				dwStreamReserved1 = readInt();
				dwStreamInitialFrames = readInt();
				dwStreamScale = readInt();
				dwStreamRate = readInt();
				dwStreamStart = readInt();
				dwStreamLength = readInt();
				dwStreamSuggestedBufferSize = readInt();
				dwStreamQuality = readInt();
				dwStreamSampleSize = readInt();
				// log read info
				//logData();
			  }
			  catch (Exception e)
			  {
				String msg = e.getMessage();
				if (msg == null || msg.equals("")) msg = "" + e;
				IJ.showMessage("ALEMBIC AVI Splitter", "AVIStreamHeader::readData() " + msg);
			  }
		  }

			private void logData()
			{
				//log("      fccStreamType=" + fccStreamType);
				log("      fccStreamHandler=" + fccStreamHandler);
				log("      dwStreamFlags=" + dwStreamFlags);
				log("      dwStreamReserved1=" + dwStreamReserved1);
				log("      dwStreamInitialFrames=" + dwStreamInitialFrames);
				log("      dwStreamScale=" + dwStreamScale);
				log("      dwStreamRate=" + dwStreamRate);
				log("      dwStreamStart=" + dwStreamStart);
				log("      dwStreamLength=" + dwStreamLength);
				log("      dwStreamSuggestedBufferSize=" + dwStreamSuggestedBufferSize);
				log("      dwStreamQuality=" + dwStreamQuality);
				log("      dwStreamSampleSize=" + dwStreamSampleSize);
			}
	}

	class AVIHeader
	{
		 private int dwMicroSecPerFrame;
		  private int dwMaxBytesPerSec;
		  private int dwReserved1;
		  private int dwFlags;
		  private int dwTotalFrames;
		  private int dwInitialFrames;
		  private int dwStreams;
		  private int dwSuggestedBufferSize;
		  private int dwWidth;
		  private int dwHeight;
		  private int dwScale;
		  private int dwRate;
		  private int dwStart;
		  private int dwLength;

		  private int getFrameNum() { return dwTotalFrames; }
		  private int getWidth() { return dwWidth; }

		  // read AVI header
		  void readData() throws Exception, IOException
		  {
			try
			{
				readTypeAndSize();
				if (type.equals("avih"))
				{
				  //log("   AVI header chunk (avih) detected...");
				  long pos = raFile.getFilePointer();
				  dwMicroSecPerFrame = readInt();
				  dwMaxBytesPerSec = readInt();
				  dwReserved1 = readInt();
				  dwFlags = readInt();
				  dwTotalFrames = readInt();
				  dwInitialFrames = readInt();
				  dwStreams = readInt();
				  dwSuggestedBufferSize = readInt();
				  dwWidth = readInt();
				  dwHeight = readInt();
				  dwScale = readInt();
				  dwRate = readInt();
				  dwStart = readInt();
				  dwLength = readInt();
				  // log read data
				  //logData();
				  // read what we needed, skip header
				  raFile.seek(pos + size);
				}
			}
			catch (Exception e)
			{
				String msg = e.getMessage();
				if (msg == null || msg.equals("")) msg = "" + e;
				IJ.showMessage("ALEMBIC AVI Splitter", "AVIHeader::readData() " + msg);
			}
		}

		private void logData()
		{
		  log("      dwMicroSecPerFrame=" + dwMicroSecPerFrame);
		  log("      dwMaxBytesPerSec=" + dwMaxBytesPerSec);
		  log("      dwReserved1=" + dwReserved1);
		  log("      dwFlags=" + dwFlags);
		  log("      dwTotalFrames=" + dwTotalFrames);
		  log("      dwInitialFrames=" + dwInitialFrames);
		  log("      dwStreams=" + dwStreams);
		  log("      dwSuggestedBufferSize=" + dwSuggestedBufferSize);
		  log("      dwWidth=" + dwWidth);
		  log("      dwHeight=" + dwHeight);
		  log("      dwScale=" + dwScale);
		  log("      dwRate=" + dwRate);
		  log("      dwStart=" + dwStart);
		  log("      dwLength=" + dwLength);
		}
	}

	class AVIBitmap
	{
		private int BMPsize;                       // size of this header in bytes
		private short BMPplanes;              // no. of color planes: always 1
		private int BMPsizeOfBitmap;        // size of bitmap in bytes (may be 0: if so, calculate)
		private int BMPhorzResolution;    // horizontal resolution, pixels/meter (may be 0)
		private int BMPvertResolution;    // vertical resolution, pixels/meter (may be 0)
		private int BMPcolorsUsed;          // no. of colors in palette (if 0, calculate)
		private int BMPcolorsImportant; // no. of important colors (appear first in palette) (0 means all are important)
		private boolean BMPtopDown;
		private int BMPnoOfPixels;
		private int BMPwidth;
		private int BMPheight;
		private short BMPbitsPerPixel;
		private int BMPcompression;
		private int BMPactualSizeOfBitmap;
		private int BMPscanLineSize;
		private int BMPactualColorsUsed;

		// color mode object (holds palette data)
		private ColorModel cm = null;

		// returns true if bitmap has palette
		private boolean hasPalette() { return (BMPactualColorsUsed != 0); }

		// returns height/width
		private int getHeight() { return BMPheight; }
		private int getWidth() { return BMPwidth; }

		// returns color depth
		private int getBitsPerPixel() { return BMPbitsPerPixel; }

		// returns Color model object
		private ColorModel getColorModel() { return cm; }

		// read bitmap header information. If palette is present, read it and construct
		// a ColorModel object
		private void readData() throws Exception, IOException
		{
			try
			{
				BMPsize = readInt();
				BMPwidth = readInt();
				BMPheight = readInt();
				BMPplanes = readShort();
				BMPbitsPerPixel = readShort();
				BMPcompression = readInt();
				BMPsizeOfBitmap = readInt();
				BMPhorzResolution = readInt();
				BMPvertResolution = readInt();
				BMPcolorsUsed = readInt();
				BMPcolorsImportant = readInt();
				BMPtopDown = (BMPheight < 0);
				BMPnoOfPixels = BMPwidth * BMPheight;
				// Scan line is padded with zeroes to be a multiple of four bytes
				BMPscanLineSize = ( (BMPwidth * BMPbitsPerPixel + 31) / 32) * 4;
				// bitmap size: a value of 0 doesn't mean zero - it means we have to calculate it
				if (BMPsizeOfBitmap != 0) BMPactualSizeOfBitmap = BMPsizeOfBitmap;
				else BMPactualSizeOfBitmap = BMPscanLineSize * BMPheight;
				// colors: a value of 0 means we determine this based on the bits per pixel
				if (BMPcolorsUsed != 0) BMPactualColorsUsed = BMPcolorsUsed;
				else if (BMPbitsPerPixel < 16) BMPactualColorsUsed = 1 << BMPbitsPerPixel;
					else BMPactualColorsUsed = 0; // no palette
				// log read info
				//logData();
				// compressed data (is not handled for now)
				if (BMPcompression != 0)
					throw new Exception("AVIBitmap::readInfo(), AVI file must be uncompressed.");
				// unsupported color depth
				if (BMPbitsPerPixel != 8 && BMPbitsPerPixel != 24)
					throw new Exception("AVIBitmap::readInfo(), unsupported bits-per-pixel value (8 or 24 bits required)");
				// read palette
				if (BMPactualColorsUsed != 0)
				{
					//log("      Now reading palette...");

					byte[] pr = new byte[BMPcolorsUsed];
					byte[] pg = new byte[BMPcolorsUsed];
					byte[] pb = new byte[BMPcolorsUsed];

					for (int i = 0; i < BMPcolorsUsed; i++)
					{
						pb[i] = raFile.readByte();
						pg[i] = raFile.readByte();
						pr[i] = raFile.readByte();
						raFile.readByte();
					}
					//log("      Palette was " + (raFile.getFilePointer() - pos1) +  " bytes long");
					//log("      Palette ended at " + (raFile.getFilePointer()) +  " and was expected to end at " + (pos + size));

					// create color model object
					cm = new IndexColorModel(BMPbitsPerPixel, BMPcolorsUsed, pr, pg, pb);
				}
			}
			catch (Exception e)
			{
				String msg = e.getMessage();
				if (msg == null || msg.equals("")) msg = "" + e;
				IJ.showMessage("ALEMBIC AVI Splitter", "AVIBitmap::readData() " + msg);
			}
		}

		private void logData() {
			log("      BMPsize=" + BMPsize);
			log("      BMPwidth=" + BMPwidth);
			log("      BMPheight=" + BMPheight);
			log("      BMPplanes=" + BMPplanes);
			log("      BMPbitsPerPixel=" + BMPbitsPerPixel);
			log("      BMPcompression=" + BMPcompression);
			log("      BMPsizeOfBitmap=" + BMPsizeOfBitmap);
			log("      BMPhorzResolution=" + BMPhorzResolution);
			log("      BMPvertResolution=" + BMPvertResolution);
			log("      BMPcolorsUsed=" + BMPcolorsUsed);
			log("      BMPcolorsImportant=" + BMPcolorsImportant);
			log("      >BMPnoOfPixels=" + BMPnoOfPixels);
			log("      >BMPscanLineSize=" + BMPscanLineSize);
			log("      >BMPactualSizeOfBitmap=" + BMPactualSizeOfBitmap);
			log("      >BMPactualColorsUsed=" + BMPscanLineSize);
			//log("      Read up to " + raFile.getFilePointer());
			//log("      Format ends at " + (pos + size));
		}
	}

	class AVIMovie
	{
		// AVI header object (holds avi heder info)
		 private AVIHeader aviHdr = new AVIHeader();
		// AVI stream header (holds stream header info)
		private AVIStreamHeader aviStrHdr = new AVIStreamHeader();
		// bitmap class (holf frame info + palette if present)
		private AVIBitmap bmp= new AVIBitmap();
		 // image processor used to hold a frame read from avi file
		private ImageProcessor ip = null;
		// image stack to keep frames read from file
		private ImageStack stack = null; // = new ImageStack(0, 0);

		// members to hold image data
		private int[] intData;
		private byte[] byteData;
		private byte[] rawData;

		// get/set methods
		// get stack (to pass to ImagePlus object)
		private ImageStack getStack() { return stack; }
		// get image processor
		private ImageProcessor getImageProcessor() { return ip; }
		// get frame width/height
		private int getHeight() { return  bmp.getHeight(); }
		private int getWidth() { return aviHdr.getWidth(); }
		// get frame number
		private int getFrameNum() { return aviHdr.getFrameNum(); }
		// is empty
		private boolean isEmpty() { return ((stack == null) || (stack.getSize() == 0)); }

		// read avi header data
		private void readAVIHeader()  throws Exception, IOException { aviHdr.readData(); }
		// read stream header data
		private void readStreamHeader() throws Exception, IOException { aviStrHdr.readData(); }
	    // read bitmap data
		private void readBitmap() throws Exception, IOException { bmp.readData(); }

        // return bitmap actual size
        private int getActualBitmapSize() { return bmp.BMPactualSizeOfBitmap; }

        private void createStack()
        {
            // create stack
			if (bmp.hasPalette())
				stack = new ImageStack(getWidth(), getHeight(), bmp.getColorModel());
			else
				stack = new ImageStack(getWidth(), getHeight());
        }

        private void createProcessor()
        {
        	// create image processor
			if (bmp.getBitsPerPixel() <= 8)
				ip = new ByteProcessor(getWidth(), getHeight());
			else
				ip = new ColorProcessor(getWidth(), getHeight());
        }

		private void create()
		{
			createStack();
            createProcessor();
		}

		// frees objects
		private void close()
		{
			aviHdr = null;
			aviStrHdr = null;
			bmp = null;
			ip = null;
			stack = null;
			intData = null;
			byteData = null;
			rawData = null;
		}

		private void openFile(String sFileName) throws Exception, IOException
		{
			 try
			 {
				// open file
				log("Open raFile " + sFileName);

				raFile = new RandomAccessFile(new File(sFileName), "r");
			 }
			 catch(Exception e)
			 {
				String msg = e.getMessage();
				if (msg == null || msg.equals("")) msg = "" + e;
				IJ.showMessage("ALEMBIC AVI Splitter", "openFile: " + msg);
			}
		  }

	  // close AVI file
	  private void closeFile() throws Exception, IOException
	  {
		try
		{
		  raFile.close();
		  raFile = null;
		  log("file closed");
		}
		catch (Exception e)
		{
		  String msg = e.getMessage();
		  if (msg == null || msg.equals("")) msg = "" + e;
		  IJ.showMessage("ALEMBIC AVI Splitter", "CloseFile " + msg);
		}
	  }

	  // export AVI file segment to stack
	  private void displayStack(String sTitle)
	  {
		// display stack
		if (!isEmpty())
		{
			new ImagePlus(sTitle, getStack()).show();

            // create new stack for next export
            aviMovie.createStack();
  		}
	  }

	  private void exportToStack(long lMinFrame, long lMaxFrame)
	  {
		  try
		  {
            long lseek = 0;
			// rewind
			raFile.seek(0);
			//aviMovie.readAVIFile(lMinFrame, lMaxFrame, bBuildStack);
			for (int i=(int)lMinFrame; i<=(int)lMaxFrame; i++)
			{
              if (bPrescan)
              {
                lseek = lFramesPos[i - 1];
              }
              else
              {
                lseek = lPosStartFrames + ((i - 1) * (aviMovie.getActualBitmapSize() + 8));
              }
              //Long iFr = new Long(i);
              //Long lP = new Long(lseek);
              //log("Exporting frame " + iFr.toString() + " lseek " + lP.toString());
              aviMovie.readFrameData(lseek);
			  aviMovie.addFrameData(true);
			}
		  }
		  catch (Exception e)
		  {
			  String msg = e.getMessage();
			  if (msg == null || msg.equals("")) msg = "" + e;
			  IJ.showMessage("ALEMBIC AVI Splitter", "export" + msg);
		  }
	  }

		private boolean readAVIFile() throws Exception, IOException
		{
		  //if (lMinFrame < lMaxFrame)
		  //{
			byte[] list = new byte[4];
			String listString;

			// show progress bar
			//IJ.showProgress(.01);

			// first read file header, if not an avi file exit
			try
			{
				readFileHeader();
			}
			catch (Exception e)
			{
				String msg = e.getMessage();
				if (msg == null || msg.equals("")) msg = "" + e;
				IJ.showMessage("ALEMBIC AVI Splitter", "ReadAVIFile: " + msg);
				return false;
			}

			// now read file lists
			while (raFile.read(list) == 4)
			{
				// read type
				listString = new String(list);
				// backtrack a little...
				raFile.seek(raFile.getFilePointer() - 4);
				// update progress bar
				//updateProgress();
				// skipping alignment junk chunks
				if (listString.equals("JUNK"))
					skipBlock();
				else if (listString.equals("LIST"))
				{
					// list type detected, good road ahead...
					readTypeAndSizeAndFcc();
					if (fcc.equals("hdrl")) // avi heder found
					{
						// avi header list found
						readAVIHeader();
					}
					else if (fcc.equals("strl")) // video stream found
					{
						long startPos = raFile.getFilePointer();
						long streamSize = size;
						readVideoStream();
						raFile.seek(startPos + streamSize - 4); // - fcc length
					}
					else if (fcc.equals("movi"))  // movie data found
					{
						// about to read frames... create and init this object
						aviMovie.create();
						readMovieData();
						return true;
				}
				else
				{
					// unknown fcc
					raFile.seek(raFile.getFilePointer() - 12);
					skipBlock();
					// skip fcc 4byte fields as well (?)
					raFile.seek(raFile.getFilePointer() + 4);
				}
			}
			else
			{
				// skip unknow block
			  skipBlock();
			}
		  }
		  return true;
	  }

		private void readFileHeader() throws Exception, IOException
		{
			readTypeAndSizeAndFcc();
			if (type.equals("RIFF"))
			{
			  if (!fcc.equals("AVI "))
			  {
				throw new Exception("The file does not appear to be in AVI format.");
			  }
			}
			else
			{
				throw new Exception("The file does not appear to be in AVI format.");
			}
		  }

		// read video stream
		  void readVideoStream() throws Exception, IOException
		  {
			//
			// strl is followed by strh and strf chunks
			//
			readTypeAndSize();

			if (type.equals("strh"))  // strh chunk
			{
			  //log("   Stream header chunk (strh) detected...");
			  pos = raFile.getFilePointer();
			  String fccStreamTypeOld;
			  fccStreamTypeOld = fccStreamType;
			  fccStreamType = readStringBytes();
			  if (!fccStreamType.equals("vids"))
			  {
				//log("      Not video stream (fcc '" + fccStreamType + "')");
				fccStreamType = fccStreamTypeOld;
				return;
			  }
			  readStreamHeader();
			  raFile.seek(pos + size);
			}
			else
			{
			  //log("**Expected fcc 'strh', found fcc '" + fcc + "'");
			  return;
			}

			readTypeAndSize();

			if (type.equals("strf"))  // strf chunk (bitmap size and palette)
			{
			  //log("   Stream format chunk (strf) detected...");
			  pos = raFile.getFilePointer();
			  readBitmap();
			  raFile.seek(pos + size);
			}
			else
			{
			  //log("**Expected fcc 'strf', found fcc '" + fcc + "'");
			  return;
			}
			// audio (?) strd strn chunks
			readTypeAndSize();

			if (type.equals("strd"))
			{
			  //log("   Stream 'strd' chunk detected and skipped");
			  raFile.seek(raFile.getFilePointer() + size);
			}
			else
			{
			  //log("   Type '" + type + "' detected.  Backing up.");
			  raFile.seek(raFile.getFilePointer() - 8);
			}

			readTypeAndSize();
			if (type.equals("strn"))
			{
			  //log("   Stream 'strn' chunk detected and skipped");
			  raFile.seek(raFile.getFilePointer() + size);
			}
			else
			{
			  //log("   Type '" + type + "' detected.  Backing up.");
			  raFile.seek(raFile.getFilePointer() - 8);
			}
		  }

			void readMovieData() throws Exception,
				  IOException
			{
				readTypeAndSizeAndFcc();

				if (type.equals("LIST") && fcc.equals("rec "))
				{
				  //log("   Movie record detected and skipped");
				}
				else
				{
				  //log("   Type '" + type + "' and fcc '" + fcc + "' detected.  Backing up.");
				  raFile.seek(raFile.getFilePointer() - 12);
				}

				readTypeAndSize();

				long startPos = raFile.getFilePointer();

				//log("  Entering while-loop to read chunks");
				// init counter and array position
				int lFrameNumber = 1;
				lFramesPos = new long[aviMovie.getFrameNum()];
				Long lposiz;

				while (type.substring(2).equals("db") || type.substring(2).equals("dc") ||
					   type.substring(2).equals("wb")) {
				  //updateProgress();
				  pos = raFile.getFilePointer();
				  if (type.substring(2).equals("db") || type.substring(2).equals("dc")) {
					//log("   Video data chunk (" + type + ") detected...");
					//log("      size=" + size);

					// setting starting position of frames in files
					if (lPosStartFrames < 0) {
					  lPosStartFrames = raFile.getFilePointer();
					  // save file position for frame lFrameNumber
                      if (!bPrescan) return;
					}
					lFramesPos[lFrameNumber - 1] = raFile.getFilePointer();
					// log
					lposiz = new Long(lFramesPos[lFrameNumber - 1]);
					log("frame pos " + lposiz.toString());
					//read frame
					aviMovie.readFrameData(-1);

					// get next frame
					lFrameNumber++;
				  }
				  else if (type.substring(2).equals("wb"))
				  {
					//log("   audio data chunk (" + type + ") detected.  Skipping...");
					//log("   size=" + size);
				  }
				  else
				  {
					//log("   unknown data chunk (" + type + ") detected.  Skipping...");
					//log("   size=" + size);
				  }

				  readTypeAndSize();
				  if (type.equals("JUNK"))
				  {
					raFile.seek(raFile.getFilePointer() + size);
					readTypeAndSize();
				  }
				}

				//log("End of video data reached with type '" + type + "'.  Backing up.");
				raFile.seek(raFile.getFilePointer() - 8);
			}

			void skipBlock() throws IOException
			{
				readTypeAndSize();
				if (type.equals("JUNK"))
				{
					//log(type + " block detected and skipped");
					raFile.seek(raFile.getFilePointer() + size);
				}
		  }

			void rewind()
			{
				try
				{
					if (raFile != null) raFile.seek(0);
				}
				catch(Exception e)
				{
				}
			}
		// reads frame data into internal objects: file ptr must have already been
		// positioned at the beginning of a valid frame
		public void readFrameData(long lPos) throws Exception, IOException
		{
			if (lPos >= 0) raFile.seek(lPos);

			int len = bmp.BMPscanLineSize;

			if (bmp.getBitsPerPixel() > 8)
				intData = new int[getWidth() * getHeight()];
			else
				byteData = new byte[getWidth() * getHeight()];

			rawData = new byte[bmp.BMPactualSizeOfBitmap];

			int rawOffset = 0;

			int offset = (getHeight() - 1) * getWidth();

			for (int i = getHeight() - 1; i >= 0; i--)
			{
				int n = raFile.read(rawData, rawOffset, len);
				if (n < len)
				{
					throw new Exception("Scan line ended prematurely after " + n + " bytes");
				}

				if (bmp.getBitsPerPixel() > 8)
					unpack(rawData, rawOffset, intData, offset, aviHdr.getWidth());
			  else
					unpack(rawData, rawOffset, bmp.getBitsPerPixel(), byteData, offset, aviHdr.getWidth());
			  rawOffset += len;
			  offset -= aviHdr.getWidth();
			}
		  }

		  // add last frame data read to image processor and to stack if bAddToStack is true
		  public void addFrameData(boolean bAddToStack) throws Exception, IOException
		  {
			  if ((bmp != null) && (ip != null))
			  {
				if (bmp.getBitsPerPixel() <= 8) ip.setPixels(byteData);
				else ip.setPixels(intData);

				if (bmp.hasPalette())  ip.setColorModel(bmp.getColorModel());
				if ((stack != null) && (bAddToStack)) stack.addSlice("", ip);
			  }
		  }
	}



  // file object
  private RandomAccessFile raFile;

   // image plus
  private ImagePlus imp;

  // object to hold image data and stack read from file
  private AVIMovie aviMovie = new AVIMovie();

  private String type = "error";
  private String fcc = "error";
  private int size = -1;

  private boolean verbose = IJ.debugMode;

  private String fccStreamType;

  private long pos;

  private long lPosStartFrames = -1;
  private String sFName = "";
  private long[] lFramesPos;

  // if true, prescan file to get frame starting positions (slower)
  boolean bPrescan = false;

  public void run(String args)
  {
    try
	{
      IJ.showProgress(.01);
      sFName = GetAVIFileNameFromDialog();
	  // open and prescan file in order to read number of frames and get file pointer to beginning of frames
      if (sFName != null)
      {
        aviMovie.openFile(sFName);

		if (aviMovie.readAVIFile())
        {
          // create review window
		  CreatePreviewWindow(aviMovie.getWidth(), aviMovie.getHeight(), aviMovie.getFrameNum(), 1, aviMovie.getFrameNum());
        }
      }
      else IJ.showMessage("ALEMBIC AVI Splitter", "Nothing to do.");
    }
    catch (OutOfMemoryError e)
    {
        IJ.showMessage("ALEMBIC AVI Splitter", "Out of memory.");
    }
    catch (Exception e)
    {
      String msg = e.getMessage();
      if (msg == null || msg.equals("")) msg = "" + e;
      IJ.showMessage("ALEMBIC AVI Splitter", "run: " + msg);
    }
    finally
    {
      IJ.showProgress(1);
      IJ.showProgress(0);
    }
  }

  // get avi filename from common file dialog
  private String GetAVIFileNameFromDialog() throws Exception, IOException
  {
	  String sFileName = "";

	  try
	  {
			OpenDialog sd = new OpenDialog("Select AVI File", "");
			sFileName = sd.getFileName();
			if (sFileName != null) sFileName = sd.getDirectory() + sFileName;
	  }
	catch (Exception e)
    {
      String msg = e.getMessage();
      if (msg == null || msg.equals("")) msg = "" + e;
      IJ.showMessage("ALEMBIC AVI Splitter", "GetAVIFileNameFromDialog: " + msg);
    }
	  return sFileName;
  }
  // read avi frame frame lFrameNum, AVI file must have been opened first
  public boolean readAVIFrame(int lFrameNum) throws Exception, IOException {
    try {
      long lseek;
      if (bPrescan)
        lseek = lFramesPos[lFrameNum - 1];
      else
        lseek = lPosStartFrames + ((lFrameNum - 1) * (aviMovie.getActualBitmapSize() + 8));

      aviMovie.readFrameData(lseek);
	  aviMovie.addFrameData(false); // do not build stack
    }
    catch (Exception e)
	{
      String msg = e.getMessage();
      if (msg == null || msg.equals("")) {
        msg = "" + e;
      }
      IJ.showMessage("ALEMBIC AVI Splitter",
                     "readAVIFrame: An error occurred reading the frame.\n \n" +
                     msg);
      return false;
    }
    return true;
  }

  void unpack(byte[] rawData, int rawOffset, int bpp, byte[] byteData, int byteOffset, int w) throws Exception
  {
    for (int i = 0; i < w; i++) {
      byteData[byteOffset + i] = rawData[rawOffset + i];
    }
  }

  void unpack(byte[] rawData, int rawOffset, int[] intData, int intOffset, int w) throws Exception
  {
    int j = intOffset;
    int k = rawOffset;
    int mask = 0xff;
    for (int i = 0; i < w; i++) {
      int b0 = ( ( (int) (rawData[k++])) & mask);
      int b1 = ( ( (int) (rawData[k++])) & mask) << 8;
      int b2 = ( ( (int) (rawData[k++])) & mask) << 16;
      intData[j] = 0xff000000 | b0 | b1 | b2;
      j++;
    }
  }

  String readStringBytes() throws IOException {
    //reads next 4 bytes and returns them as a string
    byte[] list;
    list = new byte[4];
    raFile.read(list);
    return new String(list);
  }

  int readInt() throws IOException {
    // 4 bytes, http://mindprod.com/endian.html
    int accum = 0;
    int shiftBy;
    for (shiftBy = 0; shiftBy < 32; shiftBy += 8) {
      accum |= (raFile.readByte() & 0xff) << shiftBy;
    }
    return accum;
  }

  short readShort() throws IOException {
    // 2 bytes
    int low = raFile.readByte() & 0xff;
    int high = raFile.readByte() & 0xff;
    return (short) (high << 8 | low);
  }

  void readTypeAndSize() throws IOException {
    type = readStringBytes();
    size = readInt();
  }

  void readTypeAndSizeAndFcc() throws IOException {
    type = readStringBytes();
    size = readInt();
    fcc = readStringBytes();
  }

  private int getFieldCount(String sLine, char cDelimiter)
  {
    int iCount = 0;

    sLine = sLine.trim();
    for (int i=0; i<sLine.length(); i++)
      if (sLine.charAt(i) == cDelimiter) iCount++;

    if (sLine == "") return 0;
    else return iCount+1;
  }

  // iFieldPos starts from 1
  private String getField(String sLine, int iFieldPos, char cDelimiter)
  {
    String sField = "";

    int i = 1, iPos = sLine.indexOf(cDelimiter);
//    log ("0: " + sLine);
    while (iPos != -1)
    {
//      log ("enter while");
      if (i == iFieldPos)
      {
        sField = sLine.substring(0, iPos);
        sField = sField.trim();
        return sField;
      }
      i++;
      sLine = sLine.substring(iPos+1, sLine.length());
//      log("2: " + sLine);
      iPos = sLine.indexOf(cDelimiter);
//      log ("loop");
    }

    if (i == iFieldPos)
    {
      sField = sLine.trim();
  //    log("3: " + sField);
      return sField;
    }

    return sField;
  }

  private void CreatePreviewWindow(int iWidth, int iHeight, int iMaxFrames, int iStartFrame, int iStopFrame) throws Exception
  {
    //PreviewWindow pw;
    try {
      // rewind file first
	  aviMovie.rewind();
      ColorProcessor cProc = new ColorProcessor(3*iWidth+5, iHeight+2);
      // border color
      cProc.setColor(new Color(255, 0, 0));
      cProc.fill();
      ImagePlus iPlus = new ImagePlus("Preview Window", cProc);
      PreviewWindow pw = new PreviewWindow(iPlus, new PreviewCanvas(iPlus));
      pw.SetMaxFrames(iMaxFrames);
      pw.SetStartFrame(iStartFrame);
      pw.SetStopFrame(iStopFrame);
      pw.SetPreviewWidth(iWidth);
      pw.InitGUI();
    }
    catch (Exception e)
    {
      String msg = e.getMessage();
      if (msg == null || msg.equals("")) msg = "" + e;
      IJ.showMessage("ALEMBIC AVI Splitter", "CreatePreviewWindow: " + msg);
    }
  }

  class PreviewCanvas extends ImageCanvas
  {
    PreviewCanvas(ImagePlus imp)
    {
      super(imp);
    }

    public void mousePressed(MouseEvent e)
    {
      super.mousePressed(e);
    }
  }

  class PreviewWindow extends ImageWindow
    implements ActionListener, ItemListener, WindowListener, ChangeListener
  {
    // gui members
    private Button btnPrev, btnNext, btnPlay, btnClose, btnSetCurr, btnSetStart, btnSetStop, btnExport, btnFirst, btnLast;
    private TextField txtCurrFrame, txtDelay, txtExport;
    private Checkbox cbConstraint, cbRepeat, cbPingPong;
    private JSlider slider;
    // max frame number
    private Integer iMaxFrame = new Integer(1);
    // start frame number
    private Integer iStartFrame = new Integer(1);
    // end frame number
    private Integer iStopFrame = new Integer(1);
    // frame width
    private Integer iFrameWidth = new Integer(100);
    // if true, movie is being played
	private boolean bStop = false;
    // delay in millesec between frames diplay in play function
    private Integer iDelay = new Integer("10");
    // if GUI is being initialized
    private boolean bInitGUI = true;
    PreviewWindow(ImagePlus imp, ImageCanvas ic) {
      super(imp, ic);
      addPanel();
    }

    void SetMaxFrames(int iFrameNum) {
      iMaxFrame = new Integer(iFrameNum);
    }
    void SetStartFrame(int iFrameNum) {
      iStartFrame = new Integer(iFrameNum);
    }
    void SetStopFrame(int iFrameNum) {
      iStopFrame = new Integer(iFrameNum);
    }
    void SetPreviewWidth(int iWidth) {
      iFrameWidth = new Integer(iWidth);
    }
    void addPanel() {
      Panel panelSlider = new Panel();
      panelSlider.setLayout(new GridLayout(0, 1, 4, 4));
      slider = new JSlider(JSlider.HORIZONTAL);
      slider.setPreferredSize(new Dimension(400, 20));
      slider.setMajorTickSpacing(10);
      slider.setMinorTickSpacing(5);
//      slider.setPaintTicks(true);
//      slider.setPaintLabels(true);
      slider.addChangeListener(this);
      panelSlider.add(slider);
      add(panelSlider);
      Panel panel = new Panel();
      panel.setLayout(new GridLayout(0, 5, 4, 4));
      btnFirst = new Button(" << ");
      btnFirst.addActionListener(this);
      panel.add(btnFirst);
      btnPrev = new Button(" < ");
      btnPrev.addActionListener(this);
      panel.add(btnPrev);
      txtCurrFrame = new TextField();
      txtCurrFrame.setText("1");
      txtCurrFrame.addActionListener(this);
      panel.add(txtCurrFrame);
      btnNext = new Button(" > ");
      btnNext.addActionListener(this);
      panel.add(btnNext);
      btnLast = new Button(" >> ");
      btnLast.addActionListener(this);
      panel.add(btnLast);
      btnPlay = new Button(" Play ");
      btnPlay.addActionListener(this);
      panel.add(btnPlay);
      cbConstraint = new Checkbox(" Constrain ");
      cbConstraint.addItemListener(this);
      panel.add(cbConstraint);
      cbRepeat = new Checkbox(" Repeat ");
      cbRepeat.addItemListener(this);
      panel.add(cbRepeat);
      cbPingPong = new Checkbox(" Ping-pong ");
      cbPingPong.addItemListener(this);
      panel.add(cbPingPong);
      txtDelay = new TextField();
      txtDelay.setText(iDelay.toString());
      txtDelay.addActionListener(this);
      panel.add(txtDelay);
      btnSetCurr = new Button(" Set Curr ");
      btnSetCurr.addActionListener(this);
      panel.add(btnSetCurr);
      btnSetStart = new Button(" Set Start ");
      btnSetStart.addActionListener(this);
      panel.add(btnSetStart);
      btnSetStop = new Button(" Set End ");
      btnSetStop.addActionListener(this);
      panel.add(btnSetStop);
      txtExport = new TextField();
      txtExport.addActionListener(this);
      panel.add(txtExport);
      btnExport = new Button(" Export ");
      btnExport.addActionListener(this);
      panel.add(btnExport);
      btnClose = new Button(" Exit ");
      btnClose.addActionListener(this);
      panel.add(btnClose);
      add(panel);
      pack();
    }

    private void UpdateTitle() {
     // set title
     String sTitle;
	 sTitle = "Preview Window: current frame " + slider.getValue() +" of " + iMaxFrame.toString() + " (start frame = " + iStartFrame.toString() + ", end frame = " + iStopFrame.toString() + ")";
     this.getImagePlus().setTitle(sTitle);
     txtExport.setText(iStartFrame.toString() + "-" + iStopFrame.toString());
    }

    private void InitGUI() {
      // init GUI with current image, start image, stop image
      try {
        slider.setMinimum(iStartFrame.intValue());
        slider.setMaximum(iStopFrame.intValue());
        slider.setValue(1);
//        slider.setMajorTickSpacing(9);
//        slider.setMinorTickSpacing(3);
//        slider.setPaintTicks(true);
//        slider.setPaintLabels(true);
        // current Frame
        if (readAVIFrame(slider.getValue()))
          this.imp.getProcessor().copyBits(aviMovie.getImageProcessor(), 1, 1, Blitter.COPY);

        if (readAVIFrame(iStartFrame.intValue()))
          this.imp.getProcessor().copyBits(aviMovie.getImageProcessor(), iFrameWidth.intValue() + 2, 1, Blitter.COPY);

        if (readAVIFrame(iStopFrame.intValue()))
          this.imp.getProcessor().copyBits(aviMovie.getImageProcessor(), 2*iFrameWidth.intValue() + 3, 1, Blitter.COPY);

        this.imp.updateAndDraw();
        UpdateTitle();
      }
      catch (Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.equals("")) {
          msg = "" + e;
        }
        IJ.showMessage("ALEMBIC AVI Splitter", "InitGUI: " + msg);
      }
      bInitGUI = false;
    }

    private void UpdateCurrGUI() {
      try
      {
        Integer iCurr = new Integer(slider.getValue());
        txtCurrFrame.setText(iCurr.toString());
        // update current image
        if (readAVIFrame(slider.getValue()))
        {
          this.imp.getProcessor().copyBits(aviMovie.getImageProcessor(), 1, 1, Blitter.COPY);
		  this.imp.updateAndDraw();
          UpdateTitle();
        }
      }
      catch (Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.equals("")) {
          msg = "" + e;
        }
        IJ.showMessage("ALEMBIC AVI Splitter", "UpdateCurrGUI(): " + msg);
      }
    }

    private void UpdateStartGUI() {
      // update start image
      try
      {
        if (readAVIFrame(iStartFrame.intValue()))
        {
          this.imp.getProcessor().copyBits(aviMovie.getImageProcessor(), iFrameWidth.intValue() + 2, 1, Blitter.COPY);
		  this.imp.updateAndDraw();
          UpdateTitle();
        }
      }
      catch (Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.equals("")) {
          msg = "" + e;
        }
        IJ.showMessage("ALEMBIC AVI Splitter", "UpdateStartGUI(): " + msg);
      }
    }

    private void UpdateStopGUI() {
      try
      {
        if (readAVIFrame(iStopFrame.intValue()))
        {
          this.imp.getProcessor().copyBits(aviMovie.getImageProcessor(), 2*iFrameWidth.intValue() + 3, 1, Blitter.COPY);
		  this.imp.updateAndDraw();
          UpdateTitle();
        }
      }
      catch (Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.equals("")) {
          msg = "" + e;
        }
        IJ.showMessage("ALEMBIC AVI Splitter", "UpdateStopGUI(): " + msg);
      }
    }
    public void itemStateChanged(ItemEvent e)
    {
      Object cb = e.getSource();

      if (cb == cbConstraint)
      {
        if (cbConstraint.getState())
        {
          if (slider.getValue() < iStartFrame.intValue())
            slider.setValue(iStartFrame.intValue());
          else if (slider.getValue() > iStopFrame.intValue())
            slider.setValue(iStopFrame.intValue());
        }
      }
    }
    public void actionPerformed(ActionEvent e)
    {
      Object b = e.getSource();

      if (b == btnPrev)
      {
        // prev
        int iCurr = slider.getValue() - 1;
        if (cbConstraint.getState())
        {
          if (iCurr <= iStartFrame.intValue()) slider.setValue(iStartFrame.intValue());
          else slider.setValue(iCurr);
        }
        else
        {
          if (iCurr <= 0) slider.setValue(1);
          else slider.setValue(iCurr);
        }
      }
      else if (b == btnFirst)
      {
        // first
        if (cbConstraint.getState())
          slider.setValue(iStartFrame.intValue());
        else
          slider.setValue(1);
      }
      else if (b == btnLast)
      {
         // first
        if (cbConstraint.getState())
          slider.setValue(iStopFrame.intValue());
        else
          slider.setValue(iMaxFrame.intValue());
      }
      else if (b == btnNext)
      {
        // next
        int iCurr = slider.getValue() + 1;
        if (cbConstraint.getState())
        {
          if (iCurr > iStopFrame.intValue()) slider.setValue(iStopFrame.intValue());
          else slider.setValue(iCurr);
        }
        else
        {
          if (iCurr > iMaxFrame.intValue()) slider.setValue(iMaxFrame.intValue());
          else slider.setValue(iCurr);
        }
      }
      else if (b == btnPlay)
      {
		if (btnPlay.getLabel() == " Play ")
		{
			new ALEMBIC_Thread("PLAY", this);
			btnPlay.setLabel(" Stop ");
		}
		else
		{
			bStop = true;
		}
      }
      else if (b == btnSetCurr)
      {
        String sFrame = txtCurrFrame.getText();
        Integer iFrame = new Integer(sFrame);
        if ((iFrame.intValue() > 0) && (iFrame.intValue() <= iMaxFrame.intValue()))
          slider.setValue(iFrame.intValue());
      }
      else if (b == btnSetStart)
      {
        // set Start Frame
        String sFrame = txtCurrFrame.getText();
        Integer iFrame = new Integer(sFrame);
        if ((iFrame.intValue() > 0) && (iFrame.intValue() <= iMaxFrame.intValue()))
        {
          if (iFrame.intValue() > iStopFrame.intValue())
            iStartFrame = iStopFrame;
          else
            iStartFrame = iFrame;
          UpdateStartGUI();
        }
      }
      else if (b == btnSetStop)
      {
        // set Stop Frame
        String sFrame = txtCurrFrame.getText();
        Integer iFrame = new Integer(sFrame);
        if ((iFrame.intValue() > 0) && (iFrame.intValue() <= iMaxFrame.intValue()))
        {
          if (iFrame.intValue() < iStartFrame.intValue())
            iStopFrame = iStartFrame;
          else
            iStopFrame = iFrame;
          UpdateStopGUI();
        }
      }
      else if (b == btnExport)
      {
        // export segment
   		String sTitle = "", sSection = txtExport.getText();
        sSection = sSection.trim();
        if (sSection.length() == 0)
        {
          txtExport.setText(iStartFrame.toString() + "-" + iStopFrame.toString());
          // export all segment AVI
          aviMovie.exportToStack(iStartFrame.intValue(), iStopFrame.intValue());
          // title
          if ((sFName.lastIndexOf('\\') > 0) && (sFName.lastIndexOf('.') > 0))
			sTitle = sFName.substring(sFName.lastIndexOf('\\')+1, sFName.lastIndexOf('.')) + ": "  + iStartFrame.toString() + "-" + iStopFrame.toString();
          // display stack
          aviMovie.displayStack(sTitle);
        }
        else
        {
          boolean bFound = false;
          String sField = "", sMin = "", sMax = "";
          int iCount = getFieldCount(sSection, ',');
          if ((sFName.lastIndexOf('\\') > 0) && (sFName.lastIndexOf('.') > 0))
			sTitle = sFName.substring(sFName.lastIndexOf('\\')+1, sFName.lastIndexOf('.')) + ": ";

          for (int i=1; i<=iCount; i++)
          {
            sField = getField(sSection, i, ',');
            if (sField != "")
            {
              log(sField);
              sMin = getField(sField, 1, '-');
              if (sMin != "")
              {
                sMax = getField(sField, 2, '-');
                if (sMax != "")
                {
                  //
                  Integer iMin = new Integer(sMin);
                  Integer iMax = new Integer(sMax);
                  if (iMin.intValue() < 1) iMin = new Integer(1);
                  if (iMax.intValue() > iMaxFrame.intValue()) iMax = iMaxFrame;
                  if (iMin.intValue() < iMax.intValue())
                  {
                    log("min " + iMin.toString() + ", iMax " + iMax.toString());
                    aviMovie.exportToStack(iMin.intValue(), iMax.intValue());
                    sTitle += (iMin.toString() + "-" + iMax.toString());
                    if (i < iCount) sTitle += ", ";
                    bFound = true;
                  }
                }
              }
            }
          }
          if (bFound)
          {
            // display stack
            aviMovie.displayStack(sTitle);
          }
        }
      }
      else if (b == txtDelay) // return pressed
      {
        iDelay = new Integer(txtDelay.getText());
      }
      else if (b == txtCurrFrame) // return pressed
      {
        Integer iCurr = new Integer(txtCurrFrame.getText());
        slider.setValue(iCurr.intValue());
      }
      else if (b == btnClose) {
        try {
          bStop = true;
		  if (aviMovie != null)
		  {
			aviMovie.closeFile();
			aviMovie.close();
			aviMovie = null;
		  }
          this.close();
        }
        catch (Exception ex) {
          String msg = ex.getMessage();
          if (msg == null || msg.equals("")) {
            msg = "" + ex;
          }
          IJ.showMessage("ALEMBIC AVI Splitter",
                         "An error occurred closing the file.\n \n" + msg);
        }

      }
    }
    public void stateChanged(ChangeEvent e)
    {
      Object sl = e.getSource();

      if (sl == slider)
      {
        if (!bInitGUI) UpdateCurrGUI();
      }
    }
	public void windowClosed(WindowEvent e)
	{
	try {
      bStop = true;
	  if (aviMovie != null)
	  {
		  aviMovie.closeFile();
		  aviMovie.close();
		  aviMovie = null;
	  }
	}
	catch (Exception ex) {
	}
	}
	}

  private void log(String sLog){
    if (verbose) IJ.log(sLog);
  }

  class ALEMBIC_Thread extends Thread
  {
		String command;
		private PreviewWindow pw;

		ALEMBIC_Thread(String command, PreviewWindow pw) {
			this.command = command;
			this.pw = pw;
			setPriority(Math.max(getPriority()-2, MIN_PRIORITY));
			start();
		}

		public void run()
		{
			try
			{
				execCmd(command);
			}
			catch(OutOfMemoryError e)
			{
				IJ.outOfMemory(command);
			}
			catch(Exception e)
			{
			}
		}

		public void execCmd(String command)
		{
			if (command == "PLAY")
			{
				// play whole video
                int iStart, iStop, i;
                if (pw.cbConstraint.getState())
                {
                  iStart = pw.iStartFrame.intValue();
                  iStop = pw.iStopFrame.intValue();
                }
                else
                {
                  iStart = 1;
                  iStop = pw.iMaxFrame.intValue();
                }

                boolean bExit = false;
                int iWait = pw.iDelay.intValue();
                do
                {
                  for (i=iStart; i<=iStop; i++)
                  {
                      pw.slider.setValue(i);
                      pw.UpdateCurrGUI();
                      IJ.wait(iWait);
                      if (pw.bStop == true)
                      {
                        bExit = true;
                        break;
                      }
                  }
                  if (bExit) break;

                  if (pw.cbPingPong.getState())
                  {
                    for (i=iStop; i>=iStart; i--)
                    {
                        pw.slider.setValue(i);
                        pw.UpdateCurrGUI();
                        IJ.wait(iWait);
                        if (pw.bStop == true) break;
                    }
                    if (bExit) break;
                  }
                }
                while (pw.cbRepeat.getState());
				pw.btnPlay.setLabel(" Play ");
				pw.bStop = false;
                bExit = false;
			}
		}
	}
}
