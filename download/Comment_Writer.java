import ij.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import java.util.Date;

/** This plugin writes a comment line in the main IJ window. 
    author: Francois Richard <richard@science.uottawa.ca>
*/
public class Comment_Writer implements PlugInFilter, Measurements {
        String comment;
        boolean bAbort;

        ImagePlus imp;

        public int setup(String arg, ImagePlus imp) {
                this.imp = imp;
                if (arg.equals("about"))
                        {showAbout(); return DONE;}
                return NO_IMAGE_REQUIRED;
        }

        public void run(ImageProcessor ip) {
                Date date = new Date();    //gets today's date
                comment = " ["+date+"] ";     //sets default comment line
                if (imp != null) {
                        String imgTitle = imp.getTitle();
                        comment =" ["+date+"] "+imgTitle;   // add image title to default comment
                }
                getComment();
                if (bAbort)
                        return;
                IJ.write(comment);
        }

        void getComment() {

                GenericDialog gd = new GenericDialog("Comment Writer ");
                gd.addStringField("Enter comment:", comment, 32);
                gd.showDialog();
                if (gd.wasCanceled()) {
                        bAbort = true;
                        return;
                }
                comment = gd.getNextString();
        }

        /**
         *      Displays a short message describing the plugin (in Help | About Plugins)
         */
        void showAbout() {
                IJ.showMessage("About Comment_Writer...",
                        "This plugin displays a user-specified comment in the main IJ window.\n"
                );
        }

}

