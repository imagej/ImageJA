import ij.*;
import ij.macro.Interpreter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.*;
import java.awt.event.*;
import java.awt.image.ColorModel;
import java.util.*;
import java.lang.*;
import ij.plugin.filter.*;

/** Concatenates two or more images.
 *  Gives the option of viewing the concatenated stack as a 4D image (requires 'Image5D' plugin)
 *  @author Jon Jackson - jjackson at familyjackson dot net
 *  Version 1.0
 *    First release
 *  Version 1.1
 *    Support for 'Channels' added
 *  Version 1.2
 *    Support for 'Channels' fixed
 *    Misc. bug fixes
 *
 *  last modified April 23 2006
 */

public class Concatenate_ implements PlugIn, ItemListener{
    public String pluginName =  "Concatenate_";
    
    // Pairs of instance / static variables for user options
    boolean all_windows = false;
    static boolean all_option = false;
    boolean keep;
    static boolean keep_option = false;
    boolean assign;
    static boolean assign_option = true;
    boolean im4D = false; // determines whether or not to include the Image5D option on the dialog box
    static boolean im4D_option = false;  
    
    // Image4D options
    static final int TIME = 0;
    static final int CHANNEL = 1;    
    int dim4 = 0;
    static int dim4_option = TIME;
    final String[] dims = {"Time", "Channel"};

    Vector choices;
    Checkbox allWindows;
    Checkbox im4dCb;
    Checkbox assignCb;
    Choice dim4choice;
    
    int nImages;
    int stackSize;
    ImagePlus newImp;
    String[] imageTitles;
    ImagePlus[] images;    
    public int maxEntries = 18;  // limit number of entries to fit on screen     
    final String none = "-- None --";
    String newtitle = "Concatenated Stacks";
    
    boolean batch = false;
    boolean macro = false;
    double min = 0, max = Float.MAX_VALUE;
    
    /** Optional string argument sets the name dialog boxes if called from another plugin
     */
    public void run(String arg) {
        macro = ! arg.equals("");
        if (! setupDialog()) return;
        // setupDialog completed -> save static fields for next time
        IJ.register(Concatenate_.class); 
        
        // Save contrast settings, colormap, density calibration and titles for all images
        // for case that stacks are concatenated to channels of an Image5D.
        double[] mins = new double[images.length];
        double[] maxs = new double[images.length];
        ColorModel[] cm = new ColorModel[images.length];
        Calibration[] cals = new Calibration[images.length];
        String[] titles = new String[images.length];
        if (im4D && dim4==CHANNEL) {
            for (int i=0; i<images.length; i++) {
                mins[i] = images[i].getProcessor().getMin();
                maxs[i] = images[i].getProcessor().getMax();
                cm[i] = images[i].getProcessor().getColorModel();
                cals[i] = images[i].getCalibration();
                titles[i] = images[i].getShortTitle();
            }
        }
        
        newImp = createHypervol();
        if (newImp != null) {
            newImp.show();
            if (im4D) { // Image5D creates new Image from concat_Imp
                IJ.run("Stack to Image5D", "3rd=z 4th=ch 3rd_dimension_size=" + stackSize + " 4th_dimension_size=" + (dim4==TIME?1:newImp.getStackSize()/stackSize) + (assign?" assign":""));
                newImp = WindowManager.getImage(WindowManager.getImageCount());
                if (newImp==null) return;
                WindowManager.setCurrentWindow(newImp.getWindow());
                if (dim4==TIME) {
                    newImp.getProcessor().setMinAndMax(min, max);
                    newImp.updateAndDraw();
                } else if (dim4==CHANNEL) { // Write previously saved display settings to channels.
                    String labels = "";
                    for (int i=1; i<=newImp.getNChannels(); i++) {
                        IJ.run("Set Position", "channel="+i);
                        newImp.getProcessor().setMinAndMax(mins[i-1], maxs[i-1]);
                        if (!assign) {
                            newImp.getProcessor().setColorModel(cm[i-1]);
                        }
                        newImp.getCalibration().setFunction(cals[i-1].getFunction(),
                                cals[i-1].getCoefficients(), cals[i-1].getValueUnit(),
                                cals[i-1].zeroClip());
                        labels = labels+i+"="+titles[i-1]+" ";
                    }
                    IJ.run("Set Channel Labels", labels);
                    
                    IJ.run("Set Position", "channel=1");
                }
            }
            
        }
    }
    
    // Launch a dialog requiring user to choose images
    // returns ImagePlus of concatenated images
    public ImagePlus run() {
        if (! setupDialog()) return null;
        newImp = createHypervol();
        return newImp;
    }
    
    // concatenate two images
    public ImagePlus concatenate(ImagePlus imp1, ImagePlus imp2, boolean keep) {
        images = new ImagePlus[2];
        images[0] = imp1;
        images[1] = imp2;
        return concatenate(images, keep);
    }
    
    // concatenate more than two images
    public ImagePlus concatenate(ImagePlus[] ims, boolean keepIms) {
        images = ims;
        imageTitles = new String[ims.length];
        for (int i = 0; i < ims.length; i++) {
            if (ims[i] != null) {
                imageTitles[i] = ims[i].getTitle();
            } else {
                IJ.error(pluginName, "Null ImagePlus passed to concatenate(...) method");
                return null;
            }
        }
        keep = keepIms;
        batch = true;
        im4D = false;
        newImp = createHypervol();
        return newImp;
    }
    
    ImagePlus createHypervol() {
        boolean firstImage = true;
        boolean duplicated;
        Properties[] propertyArr = new Properties[images.length];
        ImagePlus currentImp = null;
        ImageStack concat_Stack = null;
        stackSize = 0;
        int dataType = 0, width= 0, height = 0;
        Calibration cal = null;
        int count = 0;
        for (int i = 0; i < images.length; i++) {
            if (images[i] != null) { // Should only find null imp if user has closed an image after starting plugin (unlikely...)
                currentImp = images[i];
                
                if (firstImage) { // Initialise based on first image
                    //concat_Imp = images[i];
                    cal = currentImp.getCalibration();
                    width = currentImp.getWidth();
                    height = currentImp.getHeight();
                    stackSize = currentImp.getNSlices();
                    dataType = currentImp.getType();
                    concat_Stack = currentImp.createEmptyStack();
                    min = currentImp.getProcessor().getMin();
                    max = currentImp.getProcessor().getMax();
                    firstImage = false;
                }
                
                // Safety Checks
                if (currentImp.getNSlices() != stackSize && im4D) {
                    IJ.error(pluginName, "Cannot create Image5D, stack sizes not equal");
                    return null;
                }
                if (currentImp.getType() != dataType) {
                    IJ.log("Omitting " + imageTitles[i] + " - image type not matched");
                    continue;
                }
                if (currentImp.getWidth() != width || currentImp.getHeight() != height) {
                    IJ.log("Omitting " + imageTitles[i] + " - dimensions not matched");
                    continue;
                }
                
                // concatenate
                duplicated = isDuplicated(currentImp, i);
                concat(concat_Stack, currentImp.getStack(), (keep || duplicated));
                propertyArr[count] = currentImp.getProperties();
                imageTitles[count] = currentImp.getTitle();
                if (! (keep || duplicated)) {
                    currentImp.changes = false;
                    currentImp.hide();
                }
                count++;
            }
        }
        
        // Copy across info fields
        ImagePlus imp = new ImagePlus(newtitle, concat_Stack);
        imp.setCalibration(cal);
        imp.setProperty("Number of Stacks", new Integer(count));
        imp.setProperty("Stacks Properties", propertyArr);
        imp.setProperty("Image Titles", imageTitles);
        imp.getProcessor().setMinAndMax(min, max);
        return imp;
    }
    
    // taken from WSR's Concatenator_.java
    void concat(ImageStack stack3, ImageStack stack1, boolean dup) {
        int slice = 1;
        int size = stack1.getSize();
        for (int i = 1; i <= size; i++) {
            ImageProcessor ip = stack1.getProcessor(slice);
            String label = stack1.getSliceLabel(slice);
            if (dup) {
                ip = ip.duplicate();
                slice++;
            } else
                stack1.deleteSlice(slice);
            stack3.addSlice(label, ip);
        }
    }
    
    boolean setupDialog() {
        // Setup GUI dialog (only in non-batch mode)
        batch = Interpreter.isBatchMode();
        macro = macro | Macro.getOptions() != null;
        im4D = Menus.commandInUse("Stack to Image5D") && ! batch;
        if (macro) maxEntries = 256; // screen size is not limitation in macro mode
        
        // Checks
        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.error("No windows are open.");
            return false;
        } else if (wList.length < 2) {
            IJ.error("Two or more windows must be open");
            return false;
        }
        nImages = wList.length;
        
        // Create arrays for open image names
        String[] titles = new String[nImages];
        String[] titles_none = new String[nImages + 1];
        for (int i=0; i<nImages; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null) {
                titles[i] = imp.getTitle();
                titles_none[i] = imp.getTitle();
            } else {
                titles[i] = "";
                titles_none[i] = "";
            }
        }
        titles_none[nImages] = none;
        
        GenericDialog gd = new GenericDialog(pluginName, IJ.getInstance());
        gd.setInsets(15, 5, 5);
        gd.addCheckbox("All_Open Windows", all_option);
        if (macro) {
            // Dialog box will not be displayed
            for (int i = 0; i < ((nImages+1)<maxEntries?(nImages+1):maxEntries); i++) {
                // the none string is used in macro mode so that images will not be selected by default
                gd.addChoice("Image_" + (i+1), titles_none, none);
            }
        } else {
            // Dialog will be displayed - minimum two entries
            gd.setInsets(0, 15, 3);
            gd.addChoice("Image_1", titles, titles[0]);
            gd.setInsets(0, 15, 3);
            gd.addChoice("Image_2", titles, titles[1]);
            for (int i = 2; i < ((nImages+1)<maxEntries?(nImages+1):maxEntries); i++) {
                gd.setInsets(0, 15, 3);
                gd.addChoice("Image_" + (i+1), titles_none, titles_none[i]);
            }
        }
        gd.setInsets(10, 0, 0);
        gd.addStringField("Title", newtitle, 16);
        gd.setInsets(10, 5, 0);
        gd.addCheckbox("Keep Original Images", keep_option);
        if (im4D) {
            gd.setInsets(5, 5, 0);
            gd.addCheckbox("4D_image", im4D_option);
            gd.setInsets(0, 20, 0);
            gd.addChoice("4th Dim", dims, dims[dim4_option]);
            gd.setInsets(0, 20, 0);
            assign = assign_option;
            gd.addCheckbox("Assign Default Colors", (dim4_option==CHANNEL?assign:false));
        }
        if (! macro) { // Monitor user selections
            choices = gd.getChoices();
            Vector checkboxes = gd.getCheckboxes();
            for (int i = 0; i < ((nImages+1)<maxEntries?(nImages+1):maxEntries); i++) {
                ((Choice)choices.elementAt(i)).addItemListener(this);
            }
            allWindows = (Checkbox)checkboxes.firstElement();
            allWindows.addItemListener(this);
            if (im4D) { // Add listeners to respond to user selection
                dim4choice = (Choice)choices.elementAt(choices.size()-1);
                dim4choice.setEnabled(im4D_option);
                dim4choice.addItemListener(this);
                assignCb = (Checkbox)checkboxes.elementAt(3);
                assignCb.setEnabled(im4D_option && dim4_option==CHANNEL);
                assignCb.addItemListener(this);
                im4dCb = (Checkbox)checkboxes.elementAt(2);
                im4dCb.addItemListener(this);
            }
            if (all_option) itemStateChanged(new ItemEvent(allWindows, ItemEvent.ITEM_STATE_CHANGED, null, ItemEvent.SELECTED));
        }
        gd.showDialog();
        
        if (gd.wasCanceled())
            return false;
        all_windows = gd.getNextBoolean();
        all_option = all_windows;
        newtitle = gd.getNextString();
        keep = gd.getNextBoolean();
        keep_option = keep; // set static option
        
        // Create ImagePlus array from user selection
        ImagePlus[] tmpImpArr = new ImagePlus[nImages+1];
        String[] tmpStrArr = new String[nImages+1];
        int index, count = 0;
        for (int i = 0; i < (nImages+1); i++) { // compile a list of images to concatenate from user selection
            if (all_windows) { // Useful to not have to specify images in batch mode
                index = i;
            } else {
                if (i == ((nImages+1)<maxEntries?(nImages+1):maxEntries) ) break;
                index = gd.getNextChoiceIndex();
            }
            if (index >= nImages) break; // reached the 'none' string or handled all images (in case of all_windows)
            if (! titles[index].equals("")) {
                tmpStrArr[count] = titles[index];
                tmpImpArr[count] = WindowManager.getImage(wList[index]);
                count++;
            }
        }
        if (count < 2) {
            IJ.error(pluginName, "Please select at least 2 images");
            return false;
        }
        
        // New ImagePlus array, same size as number of selected images
        imageTitles = new String[count];
        images = new ImagePlus[count];
        System.arraycopy(tmpStrArr, 0, imageTitles, 0, count);
        System.arraycopy(tmpImpArr, 0, images, 0, count);
        if (im4D) { // get Image 4D options
            im4D = gd.getNextBoolean();
            im4D_option = im4D;
            dim4 = dim4choice.getSelectedIndex();
            dim4_option = dim4;
            assign = gd.getNextBoolean();
            if (dim4 == CHANNEL) assign_option = assign; //Assign option not relevant for 'TIME'
        }
        return true;
    }
    
    // test if this imageplus appears again in the list
    boolean isDuplicated(ImagePlus imp, int index) {
        int length = images.length;
        if (index >= length - 1) return false;
        for (int i = index + 1; i < length; i++) {
            if (imp == images[i]) return true;
        }
        return false;
    }
    
    public void	itemStateChanged(ItemEvent ie) {
        Choice c;
        int count = 0;
        if (ie.getSource() == allWindows) { 
            if (allWindows.getState()) {   // User selected 'all windows' button
                for (int i = 0; i <= nImages; i++) {
                    c = ((Choice)choices.elementAt(i));
                    c.select(i);
                    c.setEnabled(false);
                }
            } else {   // User unselected 'all windows' button
                for (int i = 0; i <= nImages; i++) {
                    c = ((Choice)choices.elementAt(i));
                    c.select(i);
                    c.setEnabled(true);
                }
            }
        } else if (ie.getSource() == im4dCb || ie.getSource() == dim4choice) {
            if (im4dCb.getState()) {  // User selected Image 4D option
                dim4choice.setEnabled(true);
                assignCb.setEnabled(dim4choice.getSelectedIndex() == 1);
                if (ie.getSource() == dim4choice) {   
                    if (ie.getID() == ie.ITEM_STATE_CHANGED && dim4choice.getSelectedIndex() == TIME ) {
                        // 4th Dim set as 'Time'
                        assignCb.setState(false);
                        assignCb.setEnabled(false);
                    } else if (ie.getID() == ie.ITEM_STATE_CHANGED && dim4choice.getSelectedIndex() == CHANNEL ) {
                        // 4th Dim set as 'Channel'
                        assignCb.setState(assign);
                        assignCb.setEnabled(true);
                    }
                }
            } else {    // User unselected Image 4D option
                dim4choice.setEnabled(false);
                assignCb.setEnabled(false);
            }
        } else if (ie.getSource() == assignCb ) {
            assign = assignCb.getState(); // keep record of assign checkbox state
        } else { // User selected an image
            boolean foundNone = false;
            // All image choices after an occurance of 'none' are reset to 'none'
            for (int i = 0; i <= nImages; i++) {
                c = ((Choice)choices.elementAt(i));
                if (! foundNone) {  // All selections to this point are images
                    c.setEnabled(true);
                    if (c.getSelectedItem().equals(none)) foundNone = true;
                } else { // 'None' was selected for this choice or an earlier one
                    c.select(none);
                    c.setEnabled(false);
                }
            }
        }
    }
    
    public void setim4D(boolean bool) {
        im4D = bool;
    }
//     Concatenate_(String arg) { // not IJ compatible
//     super();
//        pluginName=arg;
//        im4D = false;
//    }
    
}
