import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import java.awt.*;
import java.io.*;
import ij.plugin.*;

/** This plugin opens images from a text file list. The list can contain full paths
	or just file names. The default directory is the one containing the text file list. */
public class List_Opener implements PlugIn {

    public void run(String arg) {
        OpenDialog od = new OpenDialog("List Opener", null);
        String name = od.getFileName();
        if (name==null)
            return;
        String dir = od.getDirectory();
        Opener o = new Opener();
        int count = 0;
        String separator = System.getProperty("file.separator");
        try {
            BufferedReader r = new BufferedReader(new FileReader(dir+name));
            while (true) {
                String path = r.readLine();
                if (path==null)
                    break;
                else {
                    if (path.indexOf(separator)<0)
                        path = dir + path;
                    File f = new File(path);
                    if (f==null || !f.exists()) {
                        IJ.log("File does not exist: \""+path+"\"");
                        continue;
                    }
                    ImagePlus imp = o.openImage(path);
                    count++;
                    if (imp!=null)
                        imp.show();
                    else
                        IJ.log("Error opening: \""+path+"\"");
                }
            }
            r.close();
        } catch (IOException e) {
            IJ.error(""+e);
            return;
        }
    }

}
