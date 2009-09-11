import ij.*;
import ij.plugin.*;
import ij.io.FileInfo;
import java.awt.Frame;
import java.io.IOException;
import loci.common.RandomAccessInputStream;
import loci.formats.tiff.*;

/** Parses and outputs all TIFF tags for the current image. */
public class TIFF_Dumper implements PlugIn {

	public void run(String arg) {
		if (Menus.getCommands().get("Bio-Formats Importer")==null) {
			IJ.error("TIFF Dumper", "Bio-Formats plugin required");
			return;
		}
		ImagePlus imp = IJ.getImage();
		FileInfo fi = imp.getOriginalFileInfo();
		if (fi.directory==null || fi.fileFormat!=FileInfo.TIFF) {
			IJ.error("TIFF Dumper", "File path not available or not TIFF file");
			return;
		}
		String path = fi.directory + fi.fileName;
		IJ.log("\\Clear");
		IJ.log("PATH = "+path);
		try {
			dumpIFDs(path);
		} catch(IOException e) {
			IJ.error("Tiff Dumper", ""+e);
		}
		Frame log = WindowManager.getFrame("Log");
		if (log!=null) log.toFront();
	}

	public static void dumpIFDs(String path) throws IOException {
		RandomAccessInputStream in = new RandomAccessInputStream(path);
		TiffParser parser = new TiffParser(in);
		IFDList ifdList = parser.getIFDs();
		for (IFD ifd : ifdList) {
			for (Integer key : ifd.keySet()) {
				int k = key.intValue();
				String name = IFD.getIFDTagName(k);
				Object value = ifd.getIFDValue(k);
				IJ.log(name + " = " + value);
			}
		}
	}

}
