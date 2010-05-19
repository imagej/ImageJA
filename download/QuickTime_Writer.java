import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.io.*;
import ij.gui.*;
import ij.text.TextWindow;
import ij.measure.Calibration;

import quicktime.*;
import quicktime.io.*;
import quicktime.qd.*;
import quicktime.std.*;
import quicktime.std.movies.*;
import quicktime.std.movies.media.*;
import quicktime.std.image.*;
import quicktime.util.*;

import java.io.*;

// This plugin uses QuickTime for Java to save the current stack as a QuickTime movie.
// It is based on the VideoSampleBuilder example from chapter 8 of "QuickTime for Java: 
// A Developer's Notebook" by Chris Adamson (www.oreilly.com/catalog/quicktimejvaadn/).
public class QuickTime_Writer implements PlugIn, StdQTConstants {
	static final int TIME_SCALE = 600;
	String[] codecs = {"Cinepak", "Animation", "H.263", "Sorenson", "Sorenson 3", "MPEG-4"};
	int[] codecTypes = {kCinepakCodecType, kAnimationCodecType, kH263CodecType, kSorensonCodecType, 0x53565133, 0x6d703476};
	static String codec = "Sorenson";
	String[] qualityStrings = {"Low", "Normal", "High", "Maximum"};
	int[] qualityConstants = {codecLowQuality, codecNormalQuality, codecHighQuality, codecMaxQuality};
	int keyFrameRate = 15;
	static String quality = "Normal";

	public void run(String arg) {
		if (IJ.is64Bit() && IJ.isMacintosh()) {
			IJ.error("This plugin requires a 32-bit version of Java");
			return;
		}
		ImagePlus imp = IJ.getImage();
		if (imp==null) return;
		if (imp.getStackSize()==1) {
			IJ.showMessage("QuickTime Writer", "This plugin requires a stack");
			return;
		}

		Calibration cal = imp.getCalibration();
		double fps = 7.0;
		if (cal.frameInterval!=0.0)
			fps = 1.0/cal.frameInterval;
		int decimalPlaces = (int) fps == fps?0:3;
		GenericDialog gd = new GenericDialog("QuickTime Options");
		gd.addChoice("Compression:", codecs, codec);
		gd.addChoice("Quality:", qualityStrings, quality);
		gd.addNumericField("Frame Rate:", fps, decimalPlaces, 4, "fps");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		codec = gd.getNextChoice();
		quality = gd.getNextChoice();
		int codecType = kSorensonCodecType;
		for (int i=0; i<codecs.length; i++) {
			if (codec.equals(codecs[i]))
				codecType = codecTypes[i];
		}
		int codecQuality = codecNormalQuality;
		for (int i=0; i<qualityStrings.length; i++) {
			if (quality.equals(qualityStrings[i]))
				codecQuality = qualityConstants[i];
		}
		switch (codecQuality) {
			case codecLowQuality: keyFrameRate=30; break;
			case codecNormalQuality: keyFrameRate=15; break;
			case codecHighQuality: keyFrameRate=7; break;
			case codecMaxQuality: keyFrameRate=1; break;
		}
		fps = gd.getNextNumber();
		if (fps<0.0016666667) fps = 0.0016666667; // 10 minutes/frame
		if (fps>100.0) fps = 100.0;
		int rate = (int)(TIME_SCALE/fps);
		cal.frameInterval = 1.0/fps;

		SaveDialog sd = new SaveDialog("Save as QuickTime...", imp.getTitle(), ".mov");
		String name = sd.getFileName();
		if (name==null) return;
		if (name.length()>32) {
			IJ.error("QuickTime Writer", "File name cannot be longer than 32 characters");
			return;
		}
		String dir = sd.getDirectory();
		String path = dir+name;

		long start = System.currentTimeMillis();
		try {
			QTSession.open();
			//getCodecSettings(null);
			writeMovie(imp, path, codecType, codecQuality, rate);
		} catch (Exception e) {
			IJ.showProgress(1.0);
			printStackTrace(e);
		} finally {
			QTSession.close();
		}

		File f = new java.io.File(path);
		double fsize = f.length();
		int bitsPerPixel = imp.getBitDepth();
		if (bitsPerPixel ==24) bitsPerPixel = 32;
		int bytesPerPixel = bitsPerPixel/8;
		int isize = imp.getWidth()*imp.getHeight()*imp.getStackSize()*bytesPerPixel;
		IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 1)+ " seconds, "+IJ.d2s(isize/fsize,0)+":1 compression");
	}

	public void writeMovie(ImagePlus imp, String path, int codecType, int codecQuality, int rate) throws QTException, IOException {
		int width = imp.getWidth();
		int height = imp.getHeight();
		ImageStack stack = imp.getStack();
		int images = stack.getSize();
		QTFile movFile = new QTFile (new java.io.File(path));
		Movie movie = Movie.createMovieFile(movFile, kMoviePlayer, createMovieFileDeleteCurFile|createMovieFileDontCreateResFile);
		int timeScale = TIME_SCALE; // 100 units per second
		Track videoTrack = movie.addTrack (width, height, 0);
		VideoMedia videoMedia = new VideoMedia(videoTrack, timeScale);
		videoMedia.beginEdits();
		ImageDescription imgDesc2 = new ImageDescription(QDConstants.k32ARGBPixelFormat);
		imgDesc2.setWidth(width);
		imgDesc2.setHeight(height);
		QDGraphics gw = new QDGraphics(imgDesc2, 0);
		QDRect bounds = new QDRect (0, 0, width, height);
		int rawImageSize = QTImage.getMaxCompressionSize(gw, bounds, gw.getPixMap().getPixelSize(), 
			codecQuality, codecType, CodecComponent.anyCodec);
		QTHandle imageHandle = new QTHandle (rawImageSize, true);
		imageHandle.lock();
		RawEncodedImage compressedImage = RawEncodedImage.fromQTHandle(imageHandle);
		CSequence seq = new CSequence(gw, bounds, gw.getPixMap().getPixelSize(), codecType, CodecComponent.bestFidelityCodec, 
			codecQuality, codecQuality, keyFrameRate, null, 0);
		ImageDescription imgDesc = seq.getDescription();
		int[] pixels2 = null;
		boolean hyperstack = imp.isHyperStack() || imp.isComposite();
		boolean overlay = imp.getOverlay()!=null && !imp.getHideOverlay();
		boolean saveFrames=false, saveSlices=false, saveChannels=false;
		int channels = imp.getNChannels();
		int slices = imp.getNSlices();
		int frames = imp.getNFrames();
		int c = imp.getChannel();
		int z = imp.getSlice();
		int t = imp.getFrame();
		if (hyperstack) {
			if (frames>1) {
				saveFrames = true;
				images = frames;
			} else if (slices>1) {
				saveSlices = true;
				images = slices;
			} else if (channels>1) {
				saveChannels = true;
				images = channels;
			} else
				hyperstack = false;
		}
		for (int image=1; image<=images; image++) {
			IJ.showProgress(image+1, images);
			IJ.showStatus(image+"/"+images + " (" +IJ.d2s(image*100.0/images,0)+"%)");
			ImageProcessor ip = null;
			if (hyperstack || overlay) {
				if (saveFrames)
					imp.setPositionWithoutUpdate(c, z, image);
				else if (saveSlices)
					imp.setPositionWithoutUpdate(c, image, t);
				else if (saveChannels)
					imp.setPositionWithoutUpdate(image, z, t);
				ImagePlus imp2 = imp;
				if (overlay) {
					if (!(saveFrames||saveSlices||saveChannels))
						imp.setPositionWithoutUpdate(c, image, t);
					imp2 = imp.flatten();
				}
				ip = new ColorProcessor(imp2.getImage());
			} else {
				ip = stack.getProcessor(image);
				ip = ip.convertToRGB();
			}
			int[] pixels = (int[])ip.getPixels();
			RawEncodedImage pixelData = gw.getPixMap().getPixelData();
			int intsPerRow = pixelData.getRowBytes()/4;
			if (pixels2==null) pixels2 = new int[intsPerRow*height];
			if (EndianOrder.isNativeLittleEndian()) {
				//EndianOrder.flipBigEndianToNative(pixels, 0, EndianDescriptor.flipAll32);
				int offset1, offset2;
				for (int y=0; y<height; y++) {
					offset1 = y*width;
					offset2 = y* intsPerRow;
					for (int x=0; x<width; x++)
						pixels2[offset2++] = EndianOrder.flipBigEndianToNative32(pixels[offset1++]);
				}
			} else {
				for (int i=0; i<height; i++)
					System.arraycopy(pixels, i*width, pixels2, i*intsPerRow, width);
			}
			pixelData.copyFromArray(0, pixels2, 0, intsPerRow*height);
			CompressedFrameInfo cfInfo = seq.compressFrame (gw, bounds, codecFlagUpdatePrevious, compressedImage);
			boolean syncSample = cfInfo.getSimilarity()==0; // see developer.apple.com/qa/qtmcc/qtmcc20.html
			videoMedia.addSample (imageHandle, 0, cfInfo.getDataSize(), rate, imgDesc, 1, syncSample?0:mediaSampleNotSync);
		}
		videoMedia.endEdits();
		videoTrack.insertMedia (0, 0, videoMedia.getDuration(), 1);
		OpenMovieFile omf = OpenMovieFile.asWrite (movFile);
		movie.addResource (omf, movieInDataForkResID, movFile.getName());
		if (hyperstack) imp.setPosition(c, z, t);
	}

	void printStackTrace(Exception e) {
		CharArrayWriter caw = new CharArrayWriter();
		PrintWriter pw = new PrintWriter(caw);
		e.printStackTrace(pw);
		String s = caw.toString();
		if (s.indexOf("fBsyErr")!=-1) {
			String msg =
				"NOTE: Saving to a folder with a name longer than\n"+
				"31 characters can cause this error. Overwriting\n"+
				"an existing file can also cause it.\n \n";
			s = msg+s;
		}
		new TextWindow("Exception", s, 500, 300);
	}

}
