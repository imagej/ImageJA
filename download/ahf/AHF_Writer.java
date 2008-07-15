import ij.*;
import ij.plugin.PlugIn;
import ij.io.*;
import java.awt.*;
import java.io.*;

public class AHF_Writer implements PlugIn {

  private ImagePlus imp;
  private static String defaultDirectory = null;
  
  public void run(String arg) {
    imp = WindowManager.getCurrentImage();
    if (imp==null)
      {IJ.noImage(); return;}
    saveAsAHF();
  }
  
  /** Save the image in AHF format using a save file
      dialog. Returns false if the user selects cancel. */
  public boolean saveAsAHF() {
    SaveDialog sd = new SaveDialog("Save as ASCII Header Format", imp.getTitle(), ".ahf");
    String directory = sd.getDirectory();
    String name = sd.getFileName();
    if (name==null)
      return false;
    else
      return saveAsAHF(directory+name);
  }
	
	
  /** Save the image in AHF format using the specified path. Returns
      false if there is an I/O error. */

  /* RAK new bit 19/7/99 RGB_PLANAR and GRAY32* not tested */

  public boolean saveAsAHF(String path) {
    FileInfo fi = imp.getFileInfo();
    fi.nImages = 1;
    try {
      ImageWriter file = new ImageWriter(fi);
      OutputStream out = new BufferedOutputStream(new FileOutputStream(path));
      OutputStreamWriter osw = new OutputStreamWriter(out);

      osw.write("AHF{\n");

      switch( fi.fileType ) {
      case fi.RGB:
	osw.write("values {3}\n"); 
	break;
      case fi.RGB_PLANAR:
	osw.write("frames {3}\n"); 
	break;
      case fi.GRAY16_SIGNED: 
	osw.write("bits {16}\n"); 
	break;
      case fi.GRAY16_UNSIGNED: 
	osw.write("format {unsigned}\nbits {16}\n"); 
	break;
      case fi.GRAY32_FLOAT: 
	osw.write("format {float}\nbits {32}\n"); 
	break;
      case fi.GRAY32_INT: 
	osw.write("bits {32}\n"); 
	break;
      case fi.COLOR8: 
      case fi.GRAY8: 
	break;
      default:
	throw new IOException("Unknown data type");
       }

      osw.write("pixels {" + fi.width + "}\n" +
		"lines {" + fi.height + "}\n" );

      // Get rest of header if it exists...
      String restOfHeader = null;
      if ( imp.getProperties() != null ) {
	restOfHeader = imp.getProperties().getProperty("ahfHeader");
      }
      if ( restOfHeader != null ) {
	osw.write( restOfHeader + "\n" );
      }

      osw.write( "}" );

      osw.flush();
      file.write(out);
      osw.close();
      out.close();
    } catch (IOException e) {
      IJ.error("Error writing AHF file header");
      return false;
    }
    imp.changes = false;
    IJ.showStatus("");
    return true;
  }
  
}
