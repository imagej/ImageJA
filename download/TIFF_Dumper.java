import ij.*;
import ij.plugin.*;
import ij.io.FileInfo;
import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
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
   IJ.showStatus("Parsing IFDs");
   RandomAccessInputStream in = new RandomAccessInputStream(path);
   TiffParser parser = new TiffParser(in);
   IFDList ifdList = parser.getIFDs();
   IJ.showStatus("");
   for (IFD ifd : ifdList) {
     for (Integer key : ifd.keySet()) {
       int k = key.intValue();
       String name = IFD.getIFDTagName(k);
       String value = prettyValue(ifd.getIFDValue(k), 0);
       IJ.log(name + " = " + value);
     }
   }
   in.close();
 }

 private static String prettyValue(Object value, int indent) {
   if (!value.getClass().isArray()) return value.toString();
   char[] spaceChars = new char[indent];
   Arrays.fill(spaceChars, ' ');
   String spaces = new String(spaceChars);
   StringBuilder sb = new StringBuilder();
   sb.append("{\n");
   for (int i=0; i<Array.getLength(value); i++) {
     sb.append(spaces);
     sb.append(" ");
     Object component = Array.get(value, i);
     sb.append(prettyValue(component, indent + 2));
     sb.append("\n");
   }
   sb.append(spaces);
   sb.append("}");
   return sb.toString();
 }

}
