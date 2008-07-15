/*
  Author: George H. Silva <George.Silva@chemie.bio.uni-giessen.de>
  Version History:
    v1.00 - 2005/8/23 - initial release
          - InstantImager_Reader opens a Packard InstantImager image (*.img, 16-bit unsigned image).
          - Format information for images was hacked from various data files, so this may not
            work 100% - feedback welcome.
          - Incorporated into HandleExtraFileTypes.java to work seamlessly with ImageJ.
          - See EOF for hacked file format (as Pascal types ;-)
    v1.01 - 2005/8/26 - fixed code so that "File|Revert" function works
          - added code for extracting/displaying additional relevant header information
    v1.02 - 2005/8/29 - removed "public" modifiers from class variables (no need to be global; the
                        original idea was that this class would be useful to just parse image headers)
*/

import java.io.*;
import java.text.*;
import ij.*;
import ij.io.*;
import ij.plugin.*;
import ij.process.*;
import ij.text.*;

public class InstantImager_Reader extends ImagePlus implements PlugIn {

  // Image information variables - call readHeader to initialize
  short OffsetParams = 0;
  short OffsetImage = 0;
  short ImageWidth = 0;
  short ImageHeight = 0;
  short MaxCountsPixel = 0;
  int ElapsedTime = 0; // Seconds
  String DetectorSerial = "";
  String UserName = "";
  String Project = "";
  String AcquisitionDate = "";
  //

  public void run(String arg) {
    OpenDialog od = new OpenDialog("Select an InstantImager File", arg);
    String directory = od.getDirectory();
    String filename = od.getFileName();

    if ((filename == null) || (directory == null)) 
      return;
    IJ.showStatus("Opening: "+directory+filename);
    open(directory, filename, arg);
  }

  void open(String directory, String filename, String arg) {
    if (!readHeader(directory+filename)) {
      IJ.log("Failed to open "+directory+filename+" as an InstantImager data file.");
      return;
    }
    // Report details from image header
    TextWindow tw = new TextWindow("Parameters for "+filename,"InstantImager Parameters",500,300);
    tw.append("------------------------");
    tw.append("Image File: "+directory+filename);
    tw.append("Detector Serial#: "+DetectorSerial);
    tw.append("User: "+UserName);
    tw.append("Project: "+Project);
    tw.append("Image Dimensions: "+ImageWidth+" x "+ImageHeight);
    tw.append("Maximum Counts/Pixel: "+MaxCountsPixel);
    tw.append("Acquisition Date/Time: "+AcquisitionDate);
    tw.append("Elapsed Time: "+ElapsedTime+" seconds");
    // Now open the image file
    FileInfo fi = new FileInfo();
    fi.fileFormat = fi.RAW;
    fi.directory  = directory;
    fi.fileName = filename;
    fi.width = ImageWidth;
    fi.height = ImageHeight;
    fi.nImages = 1;
    fi.fileType = FileInfo.GRAY16_UNSIGNED;
    fi.offset = OffsetImage+1; // File read is zero-based
    fi.whiteIsZero = true;
    //
    // For compatibility with both HandleExtraFileTypes.java and the Plugins|Input-Output menu,
    // use the following method to open the image...
    //
    setFileInfo(fi); // Set properties for "this"
    FileOpener fo = new FileOpener(fi);
    // Do this so that File|Revert works - is there a better way?
    fo.revertToSaved(this);
    if (arg.equals("")) fo.open(); // Selected from Plugins menu
  }

  public boolean readHeader(String filepath) {
    FileInputStream fis;
    byte[] numBuf = new byte[4];
    byte[] strBuf = new byte[33]; // Max text item size + null byte
    SimpleDateFormat sdf;

    // See notes at EOF for file format details
    try {
      fis = new FileInputStream(filepath);
      // Read/verify image header
      fis.read(numBuf,0,4);
      if (numBuf[0] != 75 || numBuf[1] != 65 || numBuf[2] != 74 || numBuf[3] != 0) {
        IJ.showMessage("InstantImager Reader","Missing image data signature.");
        return false;
      }
      fis.skip(5);
      fis.read(numBuf,0,2);
      OffsetParams = shortBuf(numBuf);
      fis.skip(15);
      fis.read(numBuf,0,2);
      OffsetImage = shortBuf(numBuf);
      fis.close();
      // Get image parameters; see Pascal structures below
      fis = new FileInputStream(filepath);
      fis.skip(OffsetParams);
      fis.read(strBuf,0,17);
      UserName = stringBuf(strBuf);
      fis.read(strBuf,0,33);
      Project = stringBuf(strBuf);
      fis.read(numBuf,0,2);
      ImageWidth = shortBuf(numBuf);
      fis.read(numBuf,0,2);
      ImageHeight = shortBuf(numBuf);
      fis.skip(10);
      fis.read(numBuf,0,2);
      MaxCountsPixel = shortBuf(numBuf);
      fis.skip(195);
      fis.read(strBuf,0,11);
      DetectorSerial = stringBuf(strBuf);
      // Date/Time stored as single bytes in order: sec,min,hour,day,month,year
      fis.read(strBuf,0,6);
      sdf = new SimpleDateFormat("EEEE MMMM d, yyyy 'at' H:mm:ss");
      sdf.getCalendar().set(strBuf[5]+1900,strBuf[4]-1,strBuf[3],strBuf[2],strBuf[1],strBuf[0]);
      AcquisitionDate = sdf.format(sdf.getCalendar().getTime());
      fis.skip(1);
      fis.read(numBuf,0,4);
      ElapsedTime = intBuf(numBuf);
      fis.close();
    } catch (IOException e) {
      IJ.showMessage("InstantImager Reader","Problem parsing input file for image data parameters.");
      return false;
    }
    return true;
  }

  // Helper functions for little-endian conversion
  // If using JDK 1.5+, could replace this with "reverseBytes" methods
  
  short shortBuf(byte[] buf) { // Assumes buf is 2 bytes
    return (short)((int)(buf[0] & 0xFF) | ((int)(buf[1] & 0xFF) << 8));
  }

  int intBuf(byte[] buf) { // Assumes buf is 4 bytes
    int result = 0;
    
    for (int i=0; i<4; i++) {
      result |= ((int)(buf[i] & 0xFF) << (i*8));
    }
    return result;
  }
  
  // Convert buffer array to string
  String stringBuf(byte[] buf) {
    String result = "";
    int i = 0;
    
    while (buf[i] != 0) {
      result += (char)buf[i];
      i++;
    }
    return result;
  }
}
/*
// Hacked-out InstantImager file format
// Much of this is guess work and/or unknown!!!

CONST
  IMG_SIG = $004A414B; // JAK#0

TYPE
  TInstantImageHeader = packed record  // 72 bytes (minimum?) is fixed
   dwSignature: DWORD;                 // Signature for image file
   byUnknown1: Array[1..5] of Byte;    // Unknown
   wOffsetParams: Word;                // Offset from start of file to image parameters
   byUnknown2: Array[1..15] of Byte;   // Unknown
   wOffsetImage: Word;                 // Offset from start of file to image data
   byUnknown3: Array[1..44] of Byte;   // Unknown
  end;

  // Preset region values are saved as TWICE the actual value, meaning you can
  // specify values as whole or half numbers only!
  TImagePresetRegion = packed record
    wLeft: Word;
    wTop: Word;
    wWidth: Word;
    wHeight: Word;
  end;

  // Date/Time stored as simple byte values
  TImageAcquireDate = packed record
    bySec: Byte;
    byMin: Byte;
    byHour: Byte;
    byDay: Byte;
    byMonth: Byte;
    byYear: Byte; // TWO-DIGIT YEAR! Add 1900 to get actual four-digit year ;-)
  end;

  // This starts at wOffsetParams from start of file
  TInstantImagerParams = packed record // 283 bytes (minimum?) fixed
    szUser: Array[0..16] of Char;      // User name, null terminated
    szProject: Array[0..32] of Char;   // Project comment, null terminated
    wImageX: Word;                     // Image width in pixels
    wImageY: Word;                     // Image height in pixels
    byUnknown1: Array[1..10] of Byte;  // Unknown
    wMaxCountsPixel: Word;             // Actual maximum counts per pixel collected
    byUnknown2: Array[1..42] of Byte;  // Unknown
    wPresetMaxCounts: Word;            // Preset maximum counts per pixel
    wUnknown1: Word;                   // Unknown
    iprRegion: TImagePresetRegion;     // Preset region for data collection; see notes above
    byUnknown: Array[1..134] of Byte;  // Unknown
    dwPresetTime: DWORD;               // Preset data collection time in seconds
    byUnknown3: Array[1..3] of Byte;   // Unknown
    szSerial: Array[0..10] of Char;    // Detector serial number, null terminated
    iadDateTime: TImageAcquireDate;    // Date/Time image was acquired; see above
    byUnknown4: Byte;                  // Unknown
    dwElapsedTime: DWORD;              // Actual data collection (elapsed) time in seconds
  end;
*/
