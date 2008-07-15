import ij.plugin.*;
import java.io.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.Analyzer;
import ij.measure.*;

/**	This plugin displays statistics on all the images and stacks in a folder. 
	Use Analyze/Set Measurements to specify what gets measured.  */
public class Batch_Statistics implements PlugIn {

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Select a file in source folder...", "");
		if (od.getFileName()==null)
			return;
		process(od.getDirectory());
	}

	public void process(String dir) {
		String[] list = new File(dir).list();
		if (list==null)
			return;
		int n = list.length;
		Analyzer analyzer = new Analyzer();
		if (!analyzer.resetCounter())
			return;
		ResultsTable rt = analyzer.getResultsTable();
		for (int i=0; i<n; i++) {
			IJ.showProgress((double)i/n);
			IJ.showStatus(i+"/"+n);
			File f = new File(dir+list[i]);
			if (!f.isDirectory()) {
				ImagePlus img = new Opener().openImage(dir, list[i]);
				if (img==null) continue;
				int nSlices = img.getStackSize();
 				for (int j=1; j<=nSlices; j++) {
					img.setSlice(j);
					ImageStatistics stats = img.getStatistics(Analyzer.getMeasurements());
					analyzer.saveResults(stats, null);
					String name = img.getTitle();
					if (nSlices>1) name += "-"+j;
					rt.addLabel("Name", name);
					analyzer.displayResults();
				}
			}
		}
		IJ.showProgress(1.0);
		IJ.showStatus("");
	}

}


