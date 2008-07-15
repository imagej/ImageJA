import ij.plugin.*;
import ij.*;
import ij.io.*;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;

/**	Uses the JFileChooser from Swing to open one or more raw images.
	 The "Open All Files in Folder" check box in the dialog is ignored. */
public class Raw_File_Opener  implements PlugIn {
	static File dir;

	public void run(String arg) {
		if (!IJ.versionLessThan("1.21e"))
			openFiles();
		IJ.register( Raw_File_Opener.class);
	}

	public void openFiles() {
		JFileChooser fc = null;
		try {fc = new JFileChooser();}
		catch (Throwable e) {IJ.error("This plugin requires Java 2 or Swing."); return;}
		fc.setMultiSelectionEnabled(true);
		if (dir==null) {
			String sdir = OpenDialog.getDefaultDirectory();
			if (sdir!=null)
				dir = new File(sdir);
		}
		if (dir!=null)
			fc.setCurrentDirectory(dir);
		int returnVal = fc.showOpenDialog(IJ.getInstance());
		if (returnVal!=JFileChooser.APPROVE_OPTION)
			return;
		File[] files = fc.getSelectedFiles();
		if (files.length==0) { // getSelectedFiles does not work on some JVMs
			files = new File[1];
			files[0] = fc.getSelectedFile();
		}
		String path = fc.getCurrentDirectory().getPath()+Prefs.getFileSeparator();
		dir = fc.getCurrentDirectory();
		ImportDialog d = new ImportDialog();
		FileInfo fi = d.getFileInfo();
		if (fi==null)
			return;
		fi.directory = path;
		for (int i=0; i<files.length; i++) {
			fi.fileName = files[i].getName();
			FileOpener fo = new FileOpener(fi);
			fo.open();
		}        
	}

}
