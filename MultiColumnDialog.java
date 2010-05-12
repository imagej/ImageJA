import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/** This plugin demonstrates how to display checkbox groups in GenericDialogs.
     It requires ImageJ 1.44a or later. The dialog box displayed by this plugin is
     modeled after the LOCI Bio-Formats Import Options dialog box.
 */
public class MultiColumnDialog implements PlugIn {

	public void run(String arg) {
		if (IJ.versionLessThan("1.44a"))
			return;
		GenericDialog gd = new GenericDialog("Dialog With Many Options");
		String[] labels = new String[8];
		boolean[] states = new boolean[8];
		String[] headings = new String[2];
		headings[0] = "Dataset Organization";
		headings[1] = "Memory Management";
		labels[0]="Group files with similar names"; states[0]=false;
		labels[1]="Use virtual stack"; states[1]=false;
		labels[2]="Open files individually"; states[2]=true;
		labels[3]="Record modifications"; states[3]=false;
		labels[4]="Swap dimensions"; states[4]=false;
		labels[5]="Crop on import"; states[5]=true;
		labels[6]="Open all series"; states[6]=false;
		labels[7]="Specify range for series"; states[7]=true;
		gd.addCheckboxGroup(4, 2, labels, states, headings);
		String[] labels2 = new String[12];
		boolean[] states2 = new boolean[12];
		String[] headings2 = new String[3];
		headings2[0] = "Color Options";
		headings2[1] = "Split";
		headings2[2] = "Display";
		labels2[0]="Merge RGB"; states2[0]=false;
		labels2[1]="Channels"; states2[1]=false;
		labels2[2]="Metadata"; states2[2]=true;
		labels2[3]="Colorize"; states2[3]=true;
		labels2[4]="Focal planes"; states2[4]=false;
		labels2[5]="OME-XML"; states2[5]=false;
		labels2[6]="Swap channels"; states2[6]=false;
		labels2[7]="Timepoints"; states2[7]=true;
		labels2[8]="ROIs"; states2[8]=false;
		labels2[9]="Autoscale"; states2[9]=false;
		gd.setInsets(20, 0, 0 );
		gd.addCheckboxGroup(4, 3, labels2, states2, headings2);
		gd.showDialog();
	}

}
