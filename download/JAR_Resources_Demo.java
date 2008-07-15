import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.text.TextWindow;
import ij.io.Opener;
import java.awt.*;
import java.io.*;
import java.net.*;

/** This plugin shows how files required for plugins can be packaged and
 *  placed in a jar file. It loads and displays a text file, two image and a 
 *  macro from a JAR file which also contains the plugin. Note that
 *  the text file, images and macro are in a directory
 *  named "demo-resources" inside the jar file.
 * 
 * @author  Daniel Tyreus, CircuSoft Instrumentation LLC
 */
public class JAR_Resources_Demo implements PlugIn {
    String path = "/demo-resources/";

    public void run(String arg) {
        displayJpeg(); 
        displayTiff(); 
        displayText();
        runMacro();  	
    }   

    //  Loads and displays a JPEG image from a JAR using the getResource() method. 
    // Should also work for GIF and PNG images.
    void displayJpeg() {
        URL url = null;
        try {
            url = getClass().getResource(path+"buenavista.jpg");
            Image image = Toolkit.getDefaultToolkit().getImage(url);            
            // display the image
            new ImagePlus("San Francisco (JPEG)", image).show();
        }
        catch (Exception e) {
            String msg = e.getMessage();
            if (msg==null || msg.equals(""))
                msg = "" + e;	
            IJ.showMessage("JAR Demo", msg + "\n \n" + url);
        }
    }

    //  Loads and displays a TIFF from within a JAR file using getResourceAsStream().
     void displayTiff() {
            InputStream is = getClass().getResourceAsStream(path+"buenavista.tif");
            if (is!=null) {
                Opener opener = new Opener();
                ImagePlus imp = opener.openTiff(is, "San Francisco (TIFF)");
                if (imp!=null) imp.show();
            }
    }

    //  Loads and displays a text file from within a JAR file.
    void displayText() {
            new TextWindow("JAR Demo", getText(path+"text.txt"), 450, 450);
    }

    //  Loads and runs a macro from within a JAR file.
    void runMacro() {
            IJ.runMacro(getText(path+"macro.txt"), "This is the argument");
    }

    //  Loads a text file from within a JAR file using getResourceAsStream().
    String getText(String path) {
        String text = "";
        try {
            // get the text resource as a stream
            InputStream is = getClass().getResourceAsStream(path);
            if (is==null) {
                IJ.showMessage("JAR Demo", "File not found in JAR at "+path);
                return "";
            }
            InputStreamReader isr = new InputStreamReader(is);
            StringBuffer sb = new StringBuffer();
            char [] b = new char [8192];
            int n;
            //read a block and append any characters
            while ((n = isr.read(b)) > 0)
                sb.append(b,0, n);
            // display the text in a TextWindow
            text = sb.toString();
        }
        catch (IOException e) {
            String msg = e.getMessage();
            if (msg==null || msg.equals(""))
                msg = "" + e;	
            IJ.showMessage("JAR Demo", msg);
        }
        return text;
    }

}

