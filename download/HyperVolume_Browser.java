/*
 * HyperVolume_Browser.java
 *
 * Created on 19 mars 2003, 14:37 Copyright (C) 2003 Patrick Pirrotte
 * ImageJ plugin
 * Version  : 1.0
 * Author   : Patrick Pirrotte
 *            written for the IBMP-CNRS Strasbourg(France)
 * Email    : patrick.pirrotte@gmx.net, jerome.mutterer@ibmp-ulp.u-strasbg.fr
 * Description :    HyperVolume_Browser allows to browse a 4D-hypervolume with two scrollbars,
 *                  one for the third and one for the fourth dimension. At program startup
 *                  you have to supply labels for those supplementary dimensions, an depth
 *                  for the third. Those parameters may also be sent through plugin arguments.
 *
 *                  A checkbox lets you choose whether to work with a new copy of the stack,
 *                  or to add new controls to the existing stack (the new default
 *                  behavior).  If you use the existing stack, this plugin will let you
 *                  browse virtual stacks that are too large to copy or open in RAM.
 *
 *                  The original argument processing appears to be broken as of
 *                  ImageJ 1.35a, and in any case was not compatible with the
 *                  macro-recording feature.  The new argument processor accepts
 *                  arguments as recorded by the macro recorder:
 *
 *                  "depth=<slices-per-vol> 3rd=<3d label> 4th=<4d label> [make]"
 *
 *                  <slices-per-vol> is the integer number of slices per 3D volume,
 *                     and must evenly divide the original stack's depth
 *                  <3d label> is the string to label the third dimension
 *                  <4d label> is the string to label the fourth dimension
 *                  [make], if present, causes the plugin to create a new copy
 *                     of the original stack
 *
 *                  (Feature added by Jeff Brandenburg, Duke Center for In-Vivo Microscopy,
 *                  jeffb@orion.duhs.duke.edu)
 *
 *                  Please send bug reports and wishes to the above email address.
 *
 * Release History :
 *
 *      11.8.2005 : v1.1 Added view-without-copying option.
 *                  Comments/bugs to jeffb@orion.duhs.duke.edu
 *
 *      25.3.2003 : v1.0 First official release
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

import ij.plugin.PlugIn;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import java.io.*;
import java.math.*;

import java.util.*;
import ij.*;
import ij.gui.*;
import ij.measure.*;

import ij.process.*;
import ij.ImagePlus.*;
import javax.swing.*;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;

/* HyperVolume_Browser class begin*/
public class HyperVolume_Browser implements PlugIn {

    static final int WIDTH = 400;
    static final int HEIGHT = 200;

    public int stackSize;
    public ImageStack stack2;
    public ImagePlus imp2;
    private int depth=0;
    private int depth2=0;
    private  String lab_3D;
    private  String lab_4D;
    private boolean makeCopy;
    ImagePlus imp1;
    ImageStack stack1;

    /**
     * Returns the String value of an option if present, null if absent.
     * Does not maintain internal state; repeated calls with the same
     * arguments will return the same results.
     *
     * Only matches options of the form "name=value".  Values are delimited
     * by '=' on the left, ' ' (space) or end-of-string on the right.  Values
     * cannot contain embedded spaces; sorry.
     *
     * Example:  getOptionValue("arg1", "arg1=foo blah arg2=bar") == "foo"
     *           getOptionValue("arg2", "arg1=foo blah arg2=bar") == "bar"
     *           getOptionValue("arg3", "arg1=foo blah arg2=bar") == null
     *           getOptionValue("blah", "arg1=foo blah arg2=bar") == null
     *           getOptionValue("blah", "blah = buh") == null
     *           getOptionValue("blah", "blah= buh") == ""
     * @param optionName String option name to retrieve
     * @param macroOptions String argument as passed to run()
     * @return String value of option
     */
    private String getOptionValue(String optionName, String macroOptions) {
    if (macroOptions == null) return null;
        optionName += "=";
        int optStart = macroOptions.indexOf(optionName);
        if (optStart < 0) return null;
        int start=optStart + optionName.length();
        int end=macroOptions.indexOf( " ", start);
        if (end < 0) end = macroOptions.length();
        return macroOptions.substring(start, end);
    }

    /* entry point of plugin, check for args, if not present show dialog*/
    public void run(String arg) {

 		if (IJ.versionLessThan("1.34s"))
			return;
            imp1 = WindowManager.getCurrentImage();
            if ((imp1==null) || (imp1.getStackSize()==0))  {
                IJ.error("No stack selected!");
                return;
            }
            stack1 = imp1.getStack();
            stackSize = stack1.getSize();
            if (!arg.equals("")) {
                // The old argument-processing logic seems to fail as of
                // ImageJ 1.35a, and probably earlier.
                /*
                int idx = arg.indexOf(" ");
                lab_3D = arg.substring(arg.indexOf("3D=")+3,idx);
                String str1=arg.substring(arg.indexOf("3DV=")+4,arg.indexOf(" ",idx+1));
                lab_4D = arg.substring(arg.indexOf("4D=")+3,arg.length());
                depth = Integer.valueOf(str1).intValue();
                */
               String depthstr;
               // Process arguments as recorded by macro recorder.
               lab_3D = getOptionValue("3rd", arg);
               lab_4D = getOptionValue("4th", arg);
               depthstr = getOptionValue("depth", arg);
               makeCopy = (arg.indexOf("make") >= 0);
               if (lab_3D == null) lab_3D = "z-depth";
               if (lab_4D == null) lab_4D = "time";
               depth = Integer.valueOf(depthstr).intValue();
            } else {
                    String[] items_D = {"z-depth", "time", "lambda", "Other"};
                    int dimensions = 2;
                    int channels = imp1.getNChannels();
                    if (channels>1) dimensions++;
                    int slices = imp1.getNSlices();
                    if (slices >1) dimensions++;
                    int frames = imp1.getNFrames();
                    if (frames >1) dimensions++;
                    String label3 = items_D[0];
                    String label4 = items_D[1];
                    if (dimensions==4) {
                        if (channels>1) {
                            depth = channels;
                            label3 = items_D[2];
                            if (slices>1) label4 = items_D[0];
                        } else
                            depth = slices;
                    }
                    GenericDialog gd = new GenericDialog("Variable definition", IJ.getInstance());
                    gd.addMessage("Stack consists of "+imp1.getStackSize()+" slices");
                    gd.addMessage("");
                    gd.addNumericField("Depth of 3rd dimension :", depth, 0);
                    gd.addMessage("Optional :");
                    gd.addChoice("3rd dimension label :", items_D, label3);
                    gd.addChoice("4th dimension label :", items_D, label4);
                    gd.addCheckbox("Make new copy for browsing", false);
                    gd.showDialog();
                    if (gd.wasCanceled())
                        return;
                    depth = (int) gd.getNextNumber();
                    lab_3D = (String) gd.getNextChoice();
                    lab_4D = (String) gd.getNextChoice();
                    makeCopy = gd.getNextBoolean();
            }
            if (depth==0 ||depth>=stackSize) {
                IJ.error("Depth must be an integer superior to 0\n and inferior to "+Integer.toString(stackSize) + ", not " + depth);
                return;
            }
            if ((stackSize % depth) != 0){
                IJ.error("Depth must be an integer and a divisor of "+Integer.toString(stackSize) + ", not " + depth);
                return;
            }

            if (lab_3D.equals(lab_4D)) IJ.error("3rd and 4th dimension labels are indentical. Note that this is not critical\n"+
                "but a correct input of those values simplifies the use of this browser!");
            Label label_3D = new Label(lab_3D);
            Label label_4D = new Label(lab_4D);
            if (imp1 instanceof ImagePlus && imp1.getStackSize() > 1)
                initFrame(makeCopy);
    }
    /* Frame initialisation method*/
    void initFrame(boolean makeCopy){
        if (makeCopy) {
            int w = imp1.getWidth();
            int h = imp1.getHeight();
            ImageStack stack2 = new ImageStack(w, h);
            for (int i = 1; i <= stackSize; i++) {
                IJ.showStatus(i + "/" + stackSize);
                stack2.addSlice(null, stack1.getProcessor(i));
                IJ.showProgress((double) i / stackSize);
            }
            ImageProcessor ip;
            imp2 = new ImagePlus(imp1.getTitle(), stack2);
            CustomCanvas cc = new CustomCanvas(imp2);
            new CustomWindow(imp2, cc);
        } else {
            imp1.hide(); // Let's not have two windows open on one stack.
            imp1.setSlice(1); // Otherwise things get confusing.
            CustomCanvas cc = new CustomCanvas(imp1);
            new CustomWindow(imp1, cc);
        }
    }
    /* CustomCanvas class begin*/
    class CustomCanvas extends ImageCanvas {

        CustomCanvas(ImagePlus imp) {
            super(imp);
        }

    } /* CustomCanvas class end*/

    /* CustomWindow class begin*/
    private class CustomWindow extends ImageWindow implements AdjustmentListener{

        private Scrollbar sliceSel1;
        private Scrollbar sliceSel2;
        private int z=1;
        private int t=1;
        private ImagePlus i;

        /* CustomCanvas constructors, initialisation*/
        CustomWindow(ImagePlus imp){
            super(imp,new CustomCanvas(imp));
        }

        CustomWindow(ImagePlus imp, ImageCanvas ic) {
            super(imp, ic);
            i = imp;
           addPanel();
        }

        /* adds the Scrollbar to the custom window*/
        void addPanel() {
            depth2 = (stackSize / depth);
            sliceSel1 = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1,depth+1);
            sliceSel2 = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, stackSize/depth+1);

            sliceSel1.addAdjustmentListener(this);
            sliceSel2.addAdjustmentListener(this);

            int blockIncrement = (depth)/10;
            if (blockIncrement<1) blockIncrement = 1;
            sliceSel1.setUnitIncrement(1);
            sliceSel1.setBlockIncrement(blockIncrement*10);

            blockIncrement = (stackSize/depth)/10;
            if (blockIncrement<1) blockIncrement = 1;
            sliceSel2.setUnitIncrement(1);
            sliceSel2.setBlockIncrement(blockIncrement);

            add(sliceSel1);
            add(sliceSel2);

            pack();
            show();
            int previousSlice = imp.getCurrentSlice();
            i.setSlice(1);
            //WindowManager.addWindow(this);
            if (previousSlice>1 && previousSlice<=stackSize)
                imp.setSlice(previousSlice);
        }

        /* Scrollbar Listener*/
        public void adjustmentValueChanged(java.awt.event.AdjustmentEvent adjustmentEvent) {
            if (adjustmentEvent.getSource()==sliceSel1){
                z = sliceSel1.getValue();
             }
            if (adjustmentEvent.getSource()==sliceSel2){
                t = sliceSel2.getValue();
            }
            showSlice(z+depth*(t-1));
       }

        /* selects and shows slice defined by index*/
        public void showSlice(int index) {
             if (index>=1 && index<=i.getStackSize())
                i.setSlice(index);
            i.updateAndDraw();
        }

        /* drawinfo overrides method from ImageWindow and adds 3rd and 4th
         * dimension slice position, original code from Wayne Rasband, modified
         * by me*/

        public void drawInfo(Graphics g) {
        int TEXT_GAP = 0;

        String s="";
        Insets insets = super.getInsets();
        int nSlices = imp.getStackSize();
        if (nSlices>1) {
            ImageStack stack = imp.getStack();
            int currentSlice = imp.getCurrentSlice();
            s += currentSlice+"/"+nSlices;
            s += " "+lab_3D+ ": "+sliceSel1.getValue()+"/"+depth+" "+lab_4D+ ": "+sliceSel2.getValue()+"/"+depth2;
            boolean isLabel = false;
            String label = stack.getSliceLabel(currentSlice);
            if (label!=null && label.length()>0)
                s += " (" + label + ")";
            if ((this instanceof CustomWindow) && running) {
                g.drawString(s, 5, insets.top+TEXT_GAP);
                return;
            }
            s += "; ";
        }

        int type = imp.getType();
        Calibration cal = imp.getCalibration();
        if (cal.pixelWidth!=1.0 || cal.pixelHeight!=1.0)
            s += IJ.d2s(imp.getWidth()*cal.pixelWidth,2) + "x" + IJ.d2s(imp.getHeight()*cal.pixelHeight,2)
            + " " + cal.getUnits() + " (" + imp.getWidth() + "x" + imp.getHeight() + "); ";
        else
            s += imp.getWidth() + "x" + imp.getHeight() + " pixels; ";
        int size = (imp.getWidth()*imp.getHeight()*imp.getStackSize())/1024;
        switch (type) {
            case ImagePlus.GRAY8:
                s += "8-bit grayscale";
                break;
            case ImagePlus.GRAY16:
                s += "16-bit grayscale";
                size *= 2;
                break;
            case ImagePlus.GRAY32:
                s += "32-bit grayscale";
                size *= 4;
                break;
            case ImagePlus.COLOR_256:
                s += "8-bit color";
                break;
            case ImagePlus.COLOR_RGB:
                s += "RGB";
                size *= 4;
                break;
        }
        s += "; " + size + "K";
        g.drawString(s, 5, insets.top+TEXT_GAP);
    }

    }
    /* CustomWindow class end*/

}
/* HyperVolume_Browser class ends*/
