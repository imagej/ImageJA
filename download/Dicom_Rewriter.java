/* Walter O'Dell PhD 9/22/03
    this plugin acts to change the DICOM header information to remove identifying
    info: patient name, patient birth date, patient medical ID number
    
    This is known to some as the 'anonymous-ing' operation. 
    The patient name field will be replaced by the argument passed in by the GUI.
    This is intended so that you can replace the identifying patient name field with
    a generic, yet useful value such as 'patient 1', patient 2', ...

  Requirements(^*): 
    1. EndsWithFilter.class (*.java file included with package)
                
  Installation Procedure:
    1. put the EndsWithFilter.java and DicomRewriter_.java files into the 
        source/plugins folder (or whereever you have your plugins)
    2. run ImageJ and in the plugins menu first compile the EndsWithFilter.java
        and then the DicomRewriter_.java

  Execution Procedures:
    1. Run ImageJ and you can then directly run the DicomRewriter from the plugins menu.
    2. go to the directory holding your dicom image files and click on any of the files
        in there that has the correct extension for your dicom images 
        (note: the EndsWithFilter acts to select from the directory only the files in there 
            there with the same extension as the one first selected, in case there are
            other extraneous files that you do not want corrupted or that would give
            the program a hard time.)
    3. A GUI pops up to allow you to change the patient Name, Birth Date, and ID#
        fields.  I did not bother to make the program be able to add additional bytes to 
        the Dicom header (nor remove them), thus if the string you enter is longer 
        than the original string value for the field of interest, the trailing 
        characters of that string will be truncated to fit the space limits in the 
        original header.  Thus, if in the original images the field contained no 
        data, then putting in values into the Gui text field will have no effect. 
    4. The corrected files are rewritten over the original image files. I have not had
        any problems with the test cases I have tried these on, but to be extra cautious,
        you really want to not loss the data perhaps you should backup the files before
        writing over them.
    5. If you find that you made a mistake/typo in the name, birthdate and id# fields,
        you can always rerun the program on the same image files. Note that the field
        sizes were not changed from those of the original fields, thus if the first 
        time you used a shorter name than the original, then the second time you can go
        back to a longer name (you are still limited by the original field size though).
         
  Coding notes:
    1. the EndsWIthFilter is a good thing. On my home system I modified the FolderOpener 
        class to always use the EndsWithFilter when I read in a series of images.  
    2. This set of functions and classes obviously comes from the DicomDecoder class
        and related components. Much of this code could be omitted if in future versions 
        of ImageJ the DicomDecoder and DicomDictionary classes had as 'public' rather 
        than 'private' many of their attributes. On my home system the DicomRewriter 
        class is hardwired into the main ImageJ code (i.e. not coded as a plugin) 
        and defined as an extension of the DicomDecoder class and the code made 
        much more succinct. 

 */

import java.io.*;
import java.util.*;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.util.Tools;
import ij.measure.Calibration;
import ij.plugin.*;

public class Dicom_Rewriter implements PlugIn {
    private static final int TRANSFER_SYNTAX_UID = 0x00020010;
    private static final int PIXEL_DATA = 0x7FE00010;

    private static final int AE=0x4145, AS=0x4153, AT=0x4154, CS=0x4353, DA=0x4441, DS=0x4453, DT=0x4454,
        FD=0x4644, FL=0x464C, IS=0x4953, LO=0x4C4F, LT=0x4C54, PN=0x504E, SH=0x5348, SL=0x534C, 
        SS=0x5353, ST=0x5354, TM=0x544D, UI=0x5549, UL=0x554C, US=0x5553, UT=0x5554,
        OB=0x4F42, OW=0x4F57, SQ=0x5351, UN=0x554E, QQ=0x3F3F;

    private static Properties dictionary;

    protected String directory, fileName;  // WO was private
    protected static final int ID_OFFSET = 128;  //location of "DICM"; WO was private
    private static final String DICM = "DICM";
    
    protected BufferedInputStream f;  // WO was private
    protected int location = 0; // WO was private
    private boolean littleEndian = true;
    
    protected int elementLength; // WO was private
    private int vr;  // Value Representation
    private static final int IMPLICIT_VR = 0x2D2D; // '--' 
    private byte[] vrLetters = new byte[2];
    private int previousGroup;
    private StringBuffer dicomInfo = new StringBuffer(1000);
    private boolean dicmFound; // "DICM" found at offset 128
    protected boolean oddLocations; // WO was: private // one or more tags at odd locations
    protected boolean bigEndianTransferSyntax = false; // WO was: private

    // WO these added
    private static final int PATIENTS_NAME = 0x00100010; 
    private static final int PATIENT_ID = 0x00100020;
    private static final int PATIENTS_BIRTH_DATE = 0x00100030;
    String curName, curBD, curID;
    int patientName_loc = 0, patientID_loc = 0, patientBD_loc = 0;
    int patientName_len = 0, patientID_len = 0, patientBD_len = 0;

    public void run(String arg)  {
        // invoke open folder dialog
        OpenDialog od = new OpenDialog("Open Dicom...", arg);
        String directory = od.getDirectory();
        String fileName = od.getFileName();
        if (fileName==null)
            return;
        IJ.showStatus("Opening: " + directory + fileName);
        initDicomRewriter(directory, fileName);
    } // end run()

    public void initDicomRewriter(String directory, String fileName) {
        this.directory = directory;
        this.fileName = fileName;
        if (dictionary==null) {
            dictionary = getDictionary();
        }
        //IJ.register(DICOM.class);
        curName = new String("                    ");
        curBD = new String("                    ");
        curID = new String("                    ");
        runGui();
    }

  public void runGui()  {
        // get curName, curID, curBD values from first image header
        FileInfo localFI = getNcatchFileInfo();
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex<0) {
             IJ.showMessage("DICOM Rewriter", "The selected file does not have an extension.");
             return;
        }
        String selected_file_ext = fileName.substring(fileName.lastIndexOf(".")); 
        // file ext includes the '.', else use substring(IndexOf(".") +1);
        GenericDialog gd = new GenericDialog(
                "Dicom Header Adjuster", IJ.getInstance());
        gd.addMessage("Enter new values for patient name, birth date, and medical ID:");
        gd.addStringField("prev Name: "+curName, "patient 1");
        gd.addStringField("prev birthdate: "+curBD, "");
        gd.addStringField("prev ID: "+curID, "");
        gd.addCheckbox("change for all *."+selected_file_ext+" files ",true);
        gd.showDialog(); 
        while (gd.wasCanceled()) { 
            //IJ.write("  action canceled in dicomRewriter -- no changes written");
            return;
        }
        String newName = gd.getNextString();
        String newBD = gd.getNextString();
        String newID = gd.getNextString();
      FilenameFilter extfilter = new EndsWithFilter(selected_file_ext);
      // clever way to only list the files with the proper extension
        String[] list = new File(directory).list(extfilter);
        if (list==null) return;
        ij.util.StringSorter.sort(list);
        if (IJ.debugMode) IJ.log("FolderOpener: "+directory+" ("+list.length+" files)");
        if (gd.getNextBoolean())
            for (int i=0; i<list.length; i++) {
                // getNcatchFileInfo() determines location in buffer of tags:
                //          patientName_loc, patientID_loc, patientBD_loc
                // and length of space devoted to values of each: 
                //          patientName_len, patientID_len, patientBD_len. 
                // These all can vary from image to image, so recheck for each file
                fileName = list[i];
                localFI = getNcatchFileInfo();
                changeDicomInfo(directory, list[i], newName, newBD, newID);
            }
        else 
            changeDicomInfo(directory, fileName, newName, newBD, newID);
    } // end run()

    String getString(int length) throws IOException {
        byte[] buf = new byte[length];
        int pos = 0;
        while (pos<length) {
            int count = f.read(buf, pos, length-pos);
            pos += count;
        }
        location += length;
        return new String(buf);
    }

    int getByte() throws IOException {
        int b = f.read();
        if (b ==-1) throw new IOException("unexpected EOF");
        ++location;
        return b;
    }

    int getShort() throws IOException {
        int b0 = getByte();
        int b1 = getByte();
        if (littleEndian)
            return ((b1 << 8) + b0);
        else
            return ((b0 << 8) + b1);
    }

    final int getInt() throws IOException {
        int b0 = getByte();
        int b1 = getByte();
        int b2 = getByte();
        int b3 = getByte();
        if (littleEndian)
            return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
        else
            return ((b0<<24) + (b1<<16) + (b2<<8) + b3);
    }

    byte[] getLut(int length) throws IOException {
        if ((length&1)!=0) { // odd
            String dummy = getString(length);
            return null;
        }
        length /= 2;
        byte[] lut = new byte[length];
        for (int i=0; i<length; i++)
            lut[i] = (byte)(getShort()>>>8);
        return lut;
    }

  int getLength() throws IOException {
        int b0 = getByte();
        int b1 = getByte();
        int b2 = getByte();
        int b3 = getByte();
        
        // We cannot know whether the VR is implicit or explicit
        // without the full DICOM Data Dictionary for public and
        // private groups.
        
        // We will assume the VR is explicit if the two bytes
        // match the known codes. It is possible that these two
        // bytes are part of a 32-bit length for an implicit VR.
        
        vr = (b0<<8) + b1;
        
        switch (vr) {
            case OB: case OW: case SQ: case UN:
                // Explicit VR with 32-bit length if other two bytes are zero
                    if ( (b2 == 0) || (b3 == 0) ) return getInt();
                // Implicit VR with 32-bit length
                vr = IMPLICIT_VR;
                if (littleEndian)
                    return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
                else
                    return ((b0<<24) + (b1<<16) + (b2<<8) + b3);        
            case AE: case AS: case AT: case CS: case DA: case DS: case DT:  case FD:
            case FL: case IS: case LO: case LT: case PN: case SH: case SL: case SS:
            case ST: case TM:case UI: case UL: case US: case UT: case QQ:
                // Explicit vr with 16-bit length
                if (littleEndian)
                    return ((b3<<8) + b2);
                else
                    return ((b2<<8) + b3);
            default:
                // Implicit VR with 32-bit length...
                vr = IMPLICIT_VR;
                if (littleEndian)
                    return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
                else
                    return ((b0<<24) + (b1<<16) + (b2<<8) + b3);
        }
    }

    int getNextTag() throws IOException {
        int groupWord = getShort();
        if (groupWord==0x0800 && bigEndianTransferSyntax) {
            littleEndian = false;
            groupWord = 0x0008;
        }
        int elementWord = getShort();
        int tag = groupWord<<16 | elementWord;
        elementLength = getLength();
        
        // hack needed to read some GE files
        // The element length must be even!
        if (elementLength==13 && !oddLocations) elementLength = 10; 
        
        // "Undefined" element length.
        // This is a sort of bracket that encloses a sequence of elements.
        if (elementLength==-1)
            elementLength = 0;
        return tag;
    }

  // WO made 'public'
    public FileInfo getNcatchFileInfo() {
        FileInfo localFI = null;
    try { localFI = getFileInfo(); } 
        catch (Exception e) {
            IJ.showMessage("Dicom Rewriter", "Error opening "+fileName+"\n \n\""+e.getMessage()+"\"");
        }
        return localFI;
    }

    public FileInfo getFileInfo() throws IOException {
        long skipCount;
        location = 0; // WO reset for each pass through getFileInfo
        FileInfo fi = new FileInfo();
        int bitsAllocated = 16;
        fi.fileFormat = fi.RAW;
        fi.fileName = fileName;
        fi.directory = directory;
        fi.width = 0;
        fi.height = 0;
        fi.offset = 0;
        fi.intelByteOrder = true;
        fi.fileType = FileInfo.GRAY16_UNSIGNED;
        int samplesPerPixel = 1;
        int planarConfiguration = 0;
        String photoInterpretation = "";
                
        f = new BufferedInputStream(new FileInputStream(directory + fileName));
        if (IJ.debugMode) {
            IJ.log("");
            IJ.log("DicomDecoder: decoding "+fileName);
        }
        
        skipCount = (long)ID_OFFSET;
        while (skipCount > 0) skipCount -= f.skip( skipCount );
        location += ID_OFFSET;
        
        if (!getString(4).equals(DICM)) {
            f.close();
            f = new BufferedInputStream(new FileInputStream(directory + fileName));
            location = 0;
            if (IJ.debugMode) IJ.log(DICM + " not found at offset "+ID_OFFSET+"; reseting to offset 0");
        } else {
            dicmFound = true;
            if (IJ.debugMode) IJ.log(DICM + " found at offset " + ID_OFFSET);
        }
        
        boolean inSequence = true;
        boolean decodingTags = true;
        while (decodingTags) {
            int tag = getNextTag();
            if ((location&1)!=0) // DICOM tags must be at even locations
                oddLocations = true;
            String s;
            switch (tag) {
                case TRANSFER_SYNTAX_UID:
                    s = getString(elementLength);
                    if (s.indexOf("1.2.4")>-1||s.indexOf("1.2.5")>-1) {
                        f.close();
                        String msg = "ImageJ cannot open compressed DICOM images.\n \n";
                        msg += "Transfer Syntax UID = "+s;
                        throw new IOException(msg);
                    }
                    if (s.indexOf("1.2.840.10008.1.2.2")>=0)
                        bigEndianTransferSyntax = true;
                    break;
                case PATIENTS_NAME: 
                    // elementLength was reset in previous call to getNextTag()
                    curName = getString(elementLength);
                    patientName_len = elementLength;
                    patientName_loc = location - elementLength;
                    break;
                case PATIENT_ID: 
                    curID = getString(elementLength);
                    patientID_len = elementLength;
                    patientID_loc = location - elementLength;
                    break;
                case PATIENTS_BIRTH_DATE: 
                    curBD = getString(elementLength);
                    patientBD_len = elementLength;
                    patientBD_loc = location - elementLength;
                    break;

                case PIXEL_DATA:
                    // Start of image data...
                    if (elementLength!=0) {
                        fi.offset = location;
                        decodingTags = false;
                    }
                    break;
                default:
                    // Not used, skip over it...
                    addInfo(tag, null);

            } // end switch
        } // while(decodingTags)

        f.close();
        return fi;
    }
    
    // WO 10/28/02 made 'public'
    public String getDicomInfo() {
        return new String(dicomInfo);
    }

    void addInfo(int tag, String value) throws IOException {
        String info = getHeaderInfo(tag, value);
        if (info!=null) {
            int group = tag>>>16;
            if (group!=previousGroup) dicomInfo.append("\n");
            previousGroup = group;
            dicomInfo.append(tag2hex(tag)+info+"\n");
        }
    }

    void addInfo(int tag, int value) throws IOException {
        addInfo(tag, Integer.toString(value));
    }

    String getHeaderInfo(int tag, String value) throws IOException {
        String key = i2hex(tag);
        //while (key.length()<8)
        //  key = '0' + key;
        String id = (String)dictionary.get(key);
        if (id!=null) {
            if (vr==IMPLICIT_VR && id!=null)
                vr = (id.charAt(0)<<8) + id.charAt(1);
            id = id.substring(2);
        }
        if (value!=null)
            return id+": "+value;
        switch (vr) {
            case AE: case AS: case AT: case CS: case DA: case DS: case DT:  case IS: case LO: 
            case LT: case PN: case SH: case ST: case TM: case UI:
                value = getString(elementLength);
                break;
            case US:
                if (elementLength==2)
                    value = Integer.toString(getShort());
                else {
                    value = "";
                    int n = elementLength/2;
                    for (int i=0; i<n; i++)
                        value += Integer.toString(getShort())+" ";
                }
                break;
            default:
                long skipCount = (long)elementLength;
                while (skipCount > 0) skipCount -= f.skip(skipCount);
                location += elementLength;
                value = "";
        }
        if (id==null)
            return null;
        else
            return id+": "+value;
    }

    static char[] buf8 = new char[8];
        
    /** Converts an int to an 8 byte hex string. */
    String i2hex(int i) {
        for (int pos=7; pos>=0; pos--) {
            buf8[pos] = Tools.hexDigits[i&0xf];
            i >>>= 4;
        }
        return new String(buf8);
    }

    char[] buf10;
    
    String tag2hex(int tag) {
        if (buf10==null) {
            buf10 = new char[11];
            buf10[4] = ',';
            buf10[9] = ' ';
        }
        int pos = 8;
        while (pos>=0) {
            buf10[pos] = Tools.hexDigits[tag&0xf];
            tag >>>= 4;
            pos--;
            if (pos==4) pos--; // skip coma
        }
        return new String(buf10);
    }
    
    double s2d(String s) {
        Double d;
        try {d = new Double(s);}
        catch (NumberFormatException e) {d = null;}
        if (d!=null)
            return(d.doubleValue());
        else
            return(0.0);
    }
    
    boolean dicmFound() {
        return dicmFound;
    }
    
    void getSpatialScale(FileInfo fi, String scale) {
        double xscale=0, yscale=0;
        int i = scale.indexOf('\\');
        if (i>0) {
            xscale = s2d(scale.substring(0, i));
            yscale = s2d(scale.substring(i+1));
        }
        if (xscale!=0.0 && yscale!=0.0) {
            fi.pixelWidth = xscale;
            fi.pixelHeight = yscale;
            fi.unit = "mm";
        }
    }
        
  // WO 9/19/03 alter Dicom header fields and rewrite file
    public void changeDicomInfo(String directory, String fileName, String curName, String curBD, String curID) {
        byte[] namebuf = curName.getBytes();
        byte[] BDbuf = curBD.getBytes();
        byte[] IDbuf = curID.getBytes();
            try {
                f = new BufferedInputStream(new FileInputStream(directory + fileName));
                int totalFileLen =  f.available();
                byte[] fBufCopy = new byte[f.available()];
                f.read(fBufCopy, 0, f.available()); // get copy of entire file as byte[]
                f.close(); 
        // IJ.log("  fileName "+fileName+"  patientName_loc:"+patientName_loc);
            for (int i=0; i< patientBD_len; i++)    fBufCopy[i+patientBD_loc] = 0;
            for (int i=0; i< patientID_len; i++)    fBufCopy[i+patientID_loc] = 0;
            for (int i=0; i< patientName_len; i++)  fBufCopy[i+patientName_loc] = 0;
            for (int i=0; i< Math.min(patientName_len, curName.length()); i++)
                fBufCopy[i+patientName_loc] = namebuf[i];
            for (int i=0; i< Math.min(patientBD_len, curBD.length()); i++)
                fBufCopy[i+patientBD_loc] = BDbuf[i];
            for (int i=0; i< Math.min(patientID_len, curID.length()); i++)
                fBufCopy[i+patientID_loc] = IDbuf[i];
            // write back into same filename
                BufferedOutputStream bos = 
                    new BufferedOutputStream(new FileOutputStream(directory + fileName));
            bos.write(fBufCopy, 0, totalFileLen);
            bos.close();
            } // end try 
            catch (Exception e) {
                IJ.showMessage("Dicom Rewriter", "Error opening "+fileName+"\n \n\""+e.getMessage()+"\"");
           }
    } // end function changeDicomInfo()

    Properties getDictionary() {
        Properties p = new Properties();
        for (int i=0; i<dict.length; i++) {
            p.put(dict[i].substring(0,8), dict[i].substring(9));
        }
        return p;
    }

    String[] dict = {

        "00020010=UITransfer Syntax UID",
        "00080005=CSSpecific Character Set",
        "00080008=CSImage Type",
        "00080012=DAInstance Creation Date",
        "00080013=TMInstance Creation Time",
        "00080014=UIInstance Creator UID",
        "00080016=UISOP Class UID",
        "00080018=UISOP Instance UID",
        "00080020=DAStudy Date",
        "00080021=DASeries Date",
        "00080022=DAAcquisition Date",
        "00080023=DAImage Date",
        "00080024=DAOverlay Date",
        "00080025=DACurve Date",
        "00080030=TMStudy Time",
        "00080031=TMSeries Time",
        "00080032=TMAcquisition Time",
        "00080033=TMImage Time",
        "00080034=TMOverlay Time",
        "00080035=TMCurve Time",
        "00080042=CSNuclear Medicine Series Type",
        "00080050=SHAccession Number",
        "00080052=CSQuery/Retrieve Level",
        "00080054=AERetrieve AE Title",
        "00080058=AEFailed SOP Instance UID List",
        "00080060=CSModality",
        "00080064=CSConversion Type",
        "00080070=LOManufacturer",
        "00080080=LOInstitution Name",
        "00080081=STInstitution Address",
        "00080082=SQInstitution Code Sequence",
        "00080090=PNReferring Physician's Name",
        "00080092=STReferring Physician's Address",
        "00080094=SHReferring Physician's Telephone Numbers",
        "00080100=SHCode Value",
        "00080102=SHCoding Scheme Designator",
        "00080104=LOCode Meaning",
        "00081010=SHStation Name",
        "00081030=LOStudy Description",
        "00081032=SQProcedure Code Sequence",
        "0008103E=LOSeries Description",
        "00081040=LOInstitutional Department Name",
        "00081050=PNAttending Physician's Name",
        "00081060=PNName of Physician(s) Reading Study",
        "00081070=PNOperator's Name",
        "00081080=LOAdmitting Diagnoses Description",
        "00081084=SQAdmitting Diagnosis Code Sequence",
        "00081090=LOManufacturer's Model Name",
        "00081100=SQReferenced Results Sequence",
        "00081110=SQReferenced Study Sequence",
        "00081111=SQReferenced Study Component Sequence",
        "00081115=SQReferenced Series Sequence",
        "00081120=SQReferenced Patient Sequence",
        "00081125=SQReferenced Visit Sequence",
        "00081130=SQReferenced Overlay Sequence",
        "00081140=SQReferenced Image Sequence",
        "00081145=SQReferenced Curve Sequence",
        "00081150=UIReferenced SOP Class UID",
        "00081155=UIReferenced SOP Instance UID",
        "00082111=STDerivation Description",
        "00082112=SQSource Image Sequence",
        "00082120=SHStage Name",
        "00082122=ISStage Number",
        "00082124=ISNumber of Stages",
        "00082129=ISNumber of Event Timers",
        "00082128=ISView Number",
        "0008212A=ISNumber of Views in Stage",
        "00082130=DSEvent Elapsed Time(s)",
        "00082132=LOEvent Timer Name(s)",
        "00082142=ISStart Trim",
        "00082143=ISStop Trim",
        "00082144=ISRecommended Display Frame Rate",
        "00082200=CSTransducer Position",
        "00082204=CSTransducer Orientation",
        "00082208=CSAnatomic Structure",

        "00100010=PNPatient's Name",
        "00100020=LOPatient ID",
        "00100021=LOIssuer of Patient ID",
        "00100030=DAPatient's Birth Date",
        "00100032=TMPatient's Birth Time",
        "00100040=CSPatient's Sex",
        "00101000=LOOther Patient IDs",
        "00101001=PNOther Patient Names",
        "00101005=PNPatient's Maiden Name",
        "00101010=ASPatient's Age",
        "00101020=DSPatient's Size",
        "00101030=DSPatient's Weight",
        "00101040=LOPatient's Address",
        "00102150=LOCountry of Residence",
        "00102152=LORegion of Residence",
        "00102180=SHOccupation",
        "001021A0=CSSmoking Status",
        "001021B0=LTAdditional Patient History",
        "00104000=LTPatient Comments",

        "00180010=LOContrast/Bolus Agent",
        "00180015=CSBody Part Examined",
        "00180020=CSScanning Sequence",
        "00180021=CSSequence Variant",
        "00180022=CSScan Options",
        "00180023=CSMR Acquisition Type",
        "00180024=SHSequence Name",
        "00180025=CSAngio Flag",
        "00180030=LORadionuclide",
        "00180031=LORadiopharmaceutical",
        "00180032=DSEnergy Window Centerline",
        "00180033=DSEnergy Window Total Width",
        "00180034=LOIntervention Drug Name",
        "00180035=TMIntervention Drug Start Time",
        "00180040=ISCine Rate",
        "00180050=DSSlice Thickness",
        "00180060=DSkVp",
        "00180070=ISCounts Accumulated",
        "00180071=CSAcquisition Termination Condition",
        "00180072=DSEffective Series Duration",
        "00180080=DSRepetition Time",
        "00180081=DSEcho Time",
        "00180082=DSInversion Time",
        "00180083=DSNumber of Averages",
        "00180084=DSImaging Frequency",
        "00180085=SHImaged Nucleus",
        "00180086=ISEcho Numbers(s)",
        "00180087=DSMagnetic Field Strength",
        "00180088=DSSpacing Between Slices",
        "00180089=ISNumber of Phase Encoding Steps",
        "00180090=DSData Collection Diameter",
        "00180091=ISEcho Train Length",
        "00180093=DSPercent Sampling",
        "00180094=DSPercent Phase Field of View",
        "00180095=DSPixel Bandwidth",
        "00181000=LODevice Serial Number",
        "00181004=LOPlate ID",
        "00181010=LOSecondary Capture Device ID",
        "00181012=DADate of Secondary Capture",
        "00181014=TMTime of Secondary Capture",
        "00181016=LOSecondary Capture Device Manufacturer",
        "00181018=LOSecondary Capture Device Manufacturer's Model Name",
        "00181019=LOSecondary Capture Device Software Version(s)",
        "00181020=LOSoftware Versions(s)",
        "00181022=SHVideo Image Format Acquired",
        "00181023=LODigital Image Format Acquired",
        "00181030=LOProtocol Name",
        "00181040=LOContrast/Bolus Route",
        "00181041=DSContrast/Bolus Volume",
        "00181042=TMContrast/Bolus Start Time",
        "00181043=TMContrast/Bolus Stop Time",
        "00181044=DSContrast/Bolus Total Dose",
        "00181045=ISSyringe Counts",
        "00181050=DSSpatial Resolution",
        "00181060=DSTrigger Time",
        "00181061=LOTrigger Source or Type",
        "00181062=ISNominal Interval",
        "00181063=DSFrame Time",
        "00181064=LOFraming Type",
        "00181065=DSFrame Time Vector",
        "00181066=DSFrame Delay",
        "00181070=LORadionuclide Route",
        "00181071=DSRadionuclide Volume",
        "00181072=TMRadionuclide Start Time",
        "00181073=TMRadionuclide Stop Time",
        "00181074=DSRadionuclide Total Dose",
        "00181080=CSBeat Rejection Flag",
        "00181081=ISLow R-R Value",
        "00181082=ISHigh R-R Value",
        "00181083=ISIntervals Acquired",
        "00181084=ISIntervals Rejected",
        "00181085=LOPVC Rejection",
        "00181086=ISSkip Beats",
        "00181088=ISHeart Rate",
        "00181090=ISCardiac Number of Images",
        "00181094=ISTrigger Window",
        "00181100=DSReconstruction Diameter",
        "00181110=DSDistance Source to Detector",
        "00181111=DSDistance Source to Patient",
        "00181120=DSGantry/Detector Tilt",
        "00181130=DSTable Height",
        "00181131=DSTable Traverse",
        "00181140=CSRotation Direction",
        "00181141=DSAngular Position",
        "00181142=DSRadial Position",
        "00181143=DSScan Arc",
        "00181144=DSAngular Step",
        "00181145=DSCenter of Rotation Offset",
        "00181146=DSRotation Offset",
        "00181147=CSField of View Shape",
        "00181149=ISField of View Dimensions(s)",
        "00181150=ISExposure Time",
        "00181151=ISX-ray Tube Current",
        "00181152=ISExposure",
        "00181153=ISExposure in uAs",
        "00181154=DSAverage Pulse Width",
        "00181155=CSRadiation Setting",
        "00181156=CSRectification Type",
        "0018115A=CSRadiation Mode",
        "0018115E=DSImage Area Dose Product",
        "00181160=SHFilter Type",
        "00181161=LOType of Filters",
        "00181162=DSIntensifier Size",
        "00181164=DSImager Pixel Spacing",
        "00181166=CSGrid",
        "00181170=ISGenerator Power",
        "00181180=SHCollimator/grid Name",
        "00181181=CSCollimator Type",
        "00181182=ISFocal Distance",
        "00181183=DSX Focus Center",
        "00181184=DSY Focus Center",
        "00181190=DSFocal Spot(s)",
        "00181191=CSAnode Target Material",
        "001811A0=DSBody Part Thickness",
        "001811A2=DSCompression Force",
        "00181200=DADate of Last Calibration",
        "00181201=TMTime of Last Calibration",
        "00181210=SHConvolution Kernel",
        "00181242=ISActual Frame Duration",
        "00181243=ISCount Rate",
        "00181250=SHReceiving Coil",
        "00181251=SHTransmitting Coil",
        "00181260=SHPlate Type",
        "00181261=LOPhosphor Type",
        "00181300=ISScan Velocity",
        "00181301=CSWhole Body Technique",
        "00181302=ISScan Length",
        "00181310=USAcquisition Matrix",
        "00181312=CSPhase Encoding Direction",
        "00181314=DSFlip Angle",
        "00181315=CSVariable Flip Angle Flag",
        "00181316=DSSAR",
        "00181318=DSdB/dt",
        "00181400=LOAcquisition Device Processing Description",
        "00181401=LOAcquisition Device Processing Code",
        "00181402=CSCassette Orientation",
        "00181403=CSCassette Size",
        "00181404=USExposures on Plate",
        "00181405=ISRelative X-ray Exposure",
        "00181450=CSColumn Angulation",
        "00181500=CSPositioner Motion",
        "00181508=CSPositioner Type",
        "00181510=DSPositioner Primary Angle",
        "00181511=DSPositioner Secondary Angle",
        "00181520=DSPositioner Primary Angle Increment",
        "00181521=DSPositioner Secondary Angle Increment",
        "00181530=DSDetector Primary Angle",
        "00181531=DSDetector Secondary Angle",
        "00181600=CSShutter Shape",
        "00181602=ISShutter Left Vertical Edge",
        "00181604=ISShutter Right Vertical Edge",
        "00181606=ISShutter Upper Horizontal Edge",
        "00181608=ISShutter Lower Horizontal Edge",
        "00181610=ISCenter of Circular Shutter",
        "00181612=ISRadius of Circular Shutter",
        "00181620=ISVertices of the Polygonal Shutter",
        "00181700=ISCollimator Shape",
        "00181702=ISCollimator Left Vertical Edge",
        "00181704=ISCollimator Right Vertical Edge",
        "00181706=ISCollimator Upper Horizontal Edge",
        "00181708=ISCollimator Lower Horizontal Edge",
        "00181710=ISCenter of Circular Collimator",
        "00181712=ISRadius of Circular Collimator",
        "00181720=ISVertices of the Polygonal Collimator",
        "00185000=SHOutput Power",
        "00185010=LOTransducer Data",
        "00185012=DSFocus Depth",
        "00185020=LOPreprocessing Function",
        "00185021=LOPostprocessing Function",
        "00185022=DSMechanical Index",
        "00185024=DSThermal Index",
        "00185026=DSCranial Thermal Index",
        "00185027=DSSoft Tissue Thermal Index",
        "00185028=DSSoft Tissue-focus Thermal Index",
        "00185029=DSSoft Tissue-surface Thermal Index",
        "00185050=ISDepth of Scan Field",
        "00185100=CSPatient Position",
        "00185101=CSView Position",
        "00185104=SQProjection Eponymous Name Code Sequence",
        "00185210=DSImage Transformation Matrix",
        "00185212=DSImage Translation Vector",
        "00186000=DSSensitivity",
        "00186011=SQSequence of Ultrasound Regions",
        "00186012=USRegion Spatial Format",
        "00186014=USRegion Data Type",
        "00186016=ULRegion Flags",
        "00186018=ULRegion Location Min X0",
        "0018601A=ULRegion Location Min Y0",
        "0018601C=ULRegion Location Max X1",
        "0018601E=ULRegion Location Max Y1",
        "00186020=SLReference Pixel X0",
        "00186022=SLReference Pixel Y0",
        "00186024=USPhysical Units X Direction",
        "00186026=USPhysical Units Y Direction",
        "00181628=FDReference Pixel Physical Value X",
        "0018602A=FDReference Pixel Physical Value Y",
        "0018602C=FDPhysical Delta X",
        "0018602E=FDPhysical Delta Y",
        "00186030=ULTransducer Frequency",
        "00186031=CSTransducer Type",
        "00186032=ULPulse Repetition Frequency",
        "00186034=FDDoppler Correction Angle",
        "00186036=FDSterring Angle",
        "00186038=ULDoppler Sample Volume X Position",
        "0018603A=ULDoppler Sample Volume Y Position",
        "0018603C=ULTM-Line Position X0",
        "0018603E=ULTM-Line Position Y0",
        "00186040=ULTM-Line Position X1",
        "00186042=ULTM-Line Position Y1",
        "00186044=USPixel Component Organization",
        "00186046=ULPixel Component Mask",
        "00186048=ULPixel Component Range Start",
        "0018604A=ULPixel Component Range Stop",
        "0018604C=USPixel Component Physical Units",
        "0018604E=USPixel Component Data Type",
        "00186050=ULNumber of Table Break Points",
        "00186052=ULTable of X Break Points",
        "00186054=FDTable of Y Break Points",
        "00186056=ULNumber of Table Entries",
        "00186058=ULTable of Pixel Values",
        "0018605A=ULTable of Parameter Values",
        "00187000=CSDetector Conditions Nominal Flag",
        "00187001=DSDetector Temperature",
        "00187004=CSDetector Type",
        "00187005=CSDetector Configuration",
        "00187006=LTDetector Description",
        "00187008=LTDetector Mode",
        "0018700A=SHDetector ID",
        "0018700C=DADate of Last Detector Calibration",
        "0018700E=TMTime of Last Detector Calibration",
        "00187010=ISExposures on Detector Since Last Calibration",
        "00187011=ISExposures on Detector Since Manufactured",
        "00187012=DSDetector Time Since Last Exposure",
        "00187014=DSDetector Active Time",
        "00187016=DSDetector Activation Offset From Exposure",
        "0018701A=DSDetector Binning",
        "00187020=DSDetector Element Physical Size",
        "00187022=DSDetector Element Spacing",
        "00187024=CSDetector Active Shape",
        "00187026=DSDetector Active Dimension(s)",
        "00187028=DSDetector Active Origin",
        "00187030=DSField of View Origin",
        "00187032=DSField of View Rotation",
        "00187034=CSField of View Horizontal Flip",
        "00187040=LTGrid Absorbing Material",
        "00187041=LTGrid Spacing Material",
        "00187042=DSGrid Thickness",
        "00187044=DSGrid Pitch",
        "00187046=ISGrid Aspect Ratio",
        "00187048=DSGrid Period",
        "0018704C=DSGrid Focal Distance",
        "00187050=LTFilter Material LT",
        "00187052=DSFilter Thickness Minimum",
        "00187054=DSFilter Thickness Maximum",
        "00187060=CSExposure Control Mode",
        "00187062=LTExposure Control Mode Description",
        "00187064=CSExposure Status",
        "00187065=DSPhototimer Setting",

        "0020000D=UIStudy Instance UID",
        "0020000E=UISeries Instance UID",
        "00200010=SHStudy ID",
        "00200011=ISSeries Number",
        "00200012=ISAcquisition Number",
        "00200013=ISImage Number",
        "00200014=ISIsotope Number",
        "00200015=ISPhase Number",
        "00200016=ISInterval Number",
        "00200017=ISTime Slot Number",
        "00200018=ISAngle Number",
        "00200020=CSPatient Orientation",
        "00200022=USOverlay Number",
        "00200024=USCurve Number",
        "00200032=DSImage Position (Patient)",
        "00200037=DSImage Orientation (Patient)",
        "00200052=UIFrame of Reference UID",
        "00200060=CSLaterality",
        "00200080=UIMasking Image UID",
        "00200100=ISTemporal Position Identifier",
        "00200105=ISNumber of Temporal Positions",
        "00200110=DSTemporal Resolution",
        "00201000=ISSeries in Study",
        "00201002=ISImages in Acquisition",
        "00201004=ISAcquisition in Study",
        "00201040=LOPosition Reference Indicator",
        "00201041=DSSlice Location",
        "00201070=ISOther Study Numbers",
        "00201200=ISNumber of Patient Related Studies",
        "00201202=ISNumber of Patient Related Series",
        "00201204=ISNumber of Patient Related Images",
        "00201206=ISNumber of Study Related Series",
        "00201208=ISNumber of Study Related Images",
        "00204000=LTImage Comments",

        "00280002=USSamples per Pixel",
        "00280004=CSPhotometric Interpretation",
        "00280006=USPlanar Configuration",
        "00280008=ISNumber of Frames",
        "00280009=ATFrame Increment Pointer",
        "00280010=USRows",
        "00280011=USColumns",
        "00280030=DSPixel Spacing",
        "00280031=DSZoom Factor",
        "00280032=DSZoom Center",
        "00280034=ISPixel Aspect Ratio",
        "00280051=CSCorrected Image",
        "00280100=USBits Allocated",
        "00280101=USBits Stored",
        "00280102=USHigh Bit",
        "00280103=USPixel Representation",
        "00280106=USSmallest Image Pixel Value",
        "00280107=USLargest Image Pixel Value",
        "00280108=USSmallest Pixel Value in Series",
        "00280109=USLargest Pixel Value in Series",
        "00280120=USPixel Padding Value",
        "00281050=DSWindow Center",
        "00281051=DSWindow Width",
        "00281052=DSRescale Intercept",
        "00281053=DSRescale Slope",
        "00281054=LORescale Type",
        "00281055=LOWindow Center & Width Explanation",
        "00281101=USRed Palette Color Lookup Table Descriptor",
        "00281102=USGreen Palette Color Lookup Table Descriptor",
        "00281103=USBlue Palette Color Lookup Table Descriptor",
        "00281201=USRed Palette Color Lookup Table Data",
        "00281202=USGreen Palette Color Lookup Table Data",
        "00281203=USBlue Palette Color Lookup Table Data",
        "00283000=SQModality LUT Sequence",
        "00283002=USLUT Descriptor",
        "00283003=LOLUT Explanation",
        "00283004=LOMadality LUT Type",
        "00283006=USLUT Data",
        "00283010=SQVOI LUT Sequence",

        "7FE00010=OXPixel Data",

        "FFFEE000=DLItem",
        "FFFEE00D=DLItem Delimitation Item",
        "FFFEE0DD=DLSequence Delimitation Item"
    };

} // end class


// This class is a simple FilenameFilter.  It defines the required accept()
// method to determine whether a specified file should be listed.  A file
// will be listed if its name ends with the specified extension, or if
// it is a directory.
// WO 4/5/01 modified from FileLister ex. from version 1 of "Java in a Nutshell"
class EndsWithFilter implements FilenameFilter {
    private String extension;  
    public EndsWithFilter(String extension) {
        this.extension = extension;
    }
    public boolean accept(File dir, String name) {
        if (name.endsWith(extension)) return true;
        else return false; // (new File(dir, name)).isDirectory();
    }
}
