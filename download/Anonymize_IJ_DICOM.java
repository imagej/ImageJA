import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;

/* 	Author: Julian Cooper
	Contact: Julian.Cooper [at] uhb.nhs.uk
	First version: 2009/07/01
	Second version: 2010/07/08 Bugfix for first slice anonymizing and others not in some situations
	Licence: Public Domain	*/


/** This plugin allows data in the DICOM header revealed by Image > Show Info to be anonymized.
 *
 * The text used to substitute for the patient's name etc. is customisable, as is the choice of tags to be anonymized
*/



public class Anonymize_IJ_DICOM implements PlugInFilter {
	ImagePlus imp;    
	static final String dcmINST="0008,0080", dcmINSTADDR="0008,0081", dcmPHYS="0008,0090", dcmNAME="0010,0010", dcmID="0010,0020", dcmDOB="0010,0030", dcmOTHID="0010,1000", dcmOTHNAME="0010,1001", dcmMAID="0010,1005", dcmADDR="0010,1040";
	static final String[] dcmTags = {dcmINST, dcmINSTADDR, dcmPHYS, dcmNAME, dcmID, dcmDOB, dcmOTHID, dcmOTHNAME, dcmMAID, dcmADDR};
	static final String[] dcmLabels = {"Institute", "Institute Address", "Physician", "Patient's name", "ID Number", "Date of birth", "Other ID", "Other name", "Maiden name", "Address"};
	static final boolean[] dcmDefaults = {true, true, true, true, true, true, true, true, true, true};
	boolean[] dcmChosen = {true, true, true, true, true, true, true, true, true, true};
	
	static final String anonTextdefault="Anonymized";
	String header, anonText="";
	
	public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
	
	return DOES_ALL;
	}
	
	public void run(ImageProcessor ip) {
		if (!showDialog()) return;
		ImageStack stack = imp.getStack();
		int stk_size=stack.getSize();
		for(int s=1; s<=stk_size; s++) {
      if(stk_size>1) header = stack.getSliceLabel(s);
			else header = (String)imp.getProperty("Info");
			StringBuffer hdrBuff = new StringBuffer(header);
			for(int n=0; n<dcmChosen.length; n++){
				if(dcmChosen[n]) {
					hdrBuff = anonymizeTag(hdrBuff, dcmTags[n]);
				}
			}			
			String new_header = new String(hdrBuff);
			if(stk_size>1) stack.setSliceLabel(new_header, s);
			else imp.setProperty("Info", new_header);
		}
	}

	boolean showDialog(){
		GenericDialog gd = new GenericDialog("Anonymize IJ DICOM");
		gd.addStringField("Substitute text:", anonTextdefault, 10);
		gd.setInsets(10, 40, 0);
		gd.addCheckboxGroup(dcmLabels.length, 1, dcmLabels, dcmDefaults);
		gd.showDialog();
		if (gd.wasCanceled()){
			return false;
		}
		anonText=" "+gd.getNextString();
		for(int n=0; n<dcmDefaults.length; n++){
			dcmChosen[n]=gd.getNextBoolean();
		}
		return true;
	}

	StringBuffer anonymizeTag(StringBuffer hdrBuff, String tag) {
		int iTag = hdrBuff.indexOf(tag);
		int iColon = hdrBuff.indexOf(":",iTag);
		int iNewline = hdrBuff.indexOf("\n",iColon);
		if(iTag>=0 && iColon>=0 && iNewline>=0){
			hdrBuff.replace(iColon+1, iNewline, anonText);
		}
		return hdrBuff;
	}
}
