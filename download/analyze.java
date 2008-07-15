import ij.plugin.*;
import java.awt.*;
import java.io.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

/**
This is an example that shows how to write a command line utility that uses ImageJ
as a library. Use the "Compile and Run" command to compile and test, then run
from the command line. It requires ImageJ 1.29v or later.
*/
public class analyze implements PlugIn {

    public void run(String arg) {
        OpenDialog od = new OpenDialog("Open...", "");
        if (od.getFileName()==null) return;
        String dir = od.getDirectory();
        if (dir==null)
            return;
        String name = od.getFileName();
        process(dir+name);
    }

    void process(String path) {
    	if (IJ.versionLessThan("1.33b")) return;
        IJ.log("opening "+path);
        IJ.run("Open...", "path='"+path+"'");
        IJ.run("Set Measurements...", "area mean circularity decimal=3");
        IJ.log("analyzing particles");
        IJ.run("Threshold", "stack");
        IJ.run("Analyze Particles...", "minimum=1 maximum=999999 bins=20 show=Nothing display clear stack");
        
        // Caluculate mean area
        ResultsTable rt = ResultsTable.getResultsTable();
        double sum = 0;
        int n = rt.getCounter();
        for (int i=0; i<n; i++)
        	sum += rt.getValue("Area", i);
        IJ.log("mean area: "+sum/n);        
        IJ.run("Close");
        IJ.log("done");
   }

    /* To run from the command line: 
    *    java -cp ij.jar:. analyze blobs.tif > results.txt (Unix)
    *    java -cp ij.jar;. analyze blobs.tif > results.txt (Windows)
    *
    *  To use plugins, either change to the ImageJ directory or assign the
    *  plugins.dir property to the directory containing the plugins directory:
    *    java -Dplugins.dir=/usr/local/ImageJ -cp /usr/local/ij.jar:. analyze blobs.tif > results.txt (Unix)
    *    java -Dplugins.dir=C:\ImageJ -cp C:\ImageJ\ij.jar;. analyze blobs.tif > results.txt (Windows)  
    */
    public static void main(String args[]) {
        if (args.length<1)
            IJ.log("usage: analyze image");
        else {
            // new ImageJ(); // open the ImageJ window to see images and results
            new analyze().process(args[0]);
            System.exit(0);
        }
    }

}


