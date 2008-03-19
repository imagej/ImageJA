package ij.plugin.filter;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.io.FileSaver;
import ij.plugin.*;


/** This plugin saves an image in tiff, gif, jpeg, bmp, png, text or raw format. */
public class Writer implements PlugInFilter {
	private String arg;
    private ImagePlus imp;
    
	public int setup(String arg, ImagePlus imp) {
		this.arg = arg;
		this.imp = imp;
		return DOES_ALL+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		if (arg.equals("tiff"))
			new FileSaver(imp).saveAsTiff();
		else if (arg.equals("gif"))
			new FileSaver(imp).saveAsGif();
		else if (arg.equals("jpeg"))
			new FileSaver(imp).saveAsJpeg();
		else if (arg.equals("text"))
			new FileSaver(imp).saveAsText();
		else if (arg.equals("lut"))
			new FileSaver(imp).saveAsLut();
		else if (arg.equals("raw"))
			new FileSaver(imp).saveAsRaw();
		else if (arg.equals("zip"))
			new FileSaver(imp).saveAsZip();
		else if (arg.equals("bmp"))
			new FileSaver(imp).saveAsBmp();
		else if (arg.equals("png"))
			new FileSaver(imp).saveAsPng();
		else if (arg.equals("pgm"))
			new FileSaver(imp).saveAsPgm();
		else if (arg.equals("fits"))
			new FileSaver(imp).saveAsFits();
	}
	

	/**Reasonably thread-safe writers. */
	static public void write(ImagePlus imp, String arg, String path) {
		if (arg.equals("tiff"))
			if (imp.getStackSize() > 1)
				new FileSaver(imp).saveAsTiffStack(path);
			else
				new FileSaver(imp).saveAsTiff(path);
		else if (arg.equals("gif"))
			GifWriter.write(imp, path, GifWriter.getTransparentIndex());
		else if (arg.equals("jpeg"))
			JpegWriter.write(imp, path, JpegWriter.getQuality());
		else if (arg.equals("text"))
			new FileSaver(imp).saveAsText();
		else if (arg.equals("lut"))
			new FileSaver(imp).saveAsLut();
		else if (arg.equals("raw"))
			new FileSaver(imp).saveAsRaw();
		else if (arg.equals("zip"))
			new FileSaver(imp).saveAsZip();
		else if (arg.equals("bmp"))
			BMP_Writer.write(imp, path);
		else if (arg.equals("png"))
			PNG_Writer.write(imp, path);
		else if (arg.equals("pgm"))
			PNM_Writer.write(imp, path);
		else if (arg.equals("fits"))
			FITS_Writer.write(imp, path);
	}
}
