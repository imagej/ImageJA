/*
 * RGB_Profiler.java
 *
 * Created on 20/02/2004 Copyright (C) 2003 IBMP
 * ImageJ plugin
 * Version  : 1.0
 * Authors  : C. Laummonerie & J. Mutterer
 *            written for the IBMP-CNRS Strasbourg(France)
 * Email    : jerome.mutterer at ibmp-ulp.u-strasbg.fr
 * Description :  This Plugin draw the Red, Green and Blue profile plot 
 * of an RGB image on the same Plot, for each type of line selection. 
 * This profile is actualized when a new selection is made. 
 * Large parts of this code were taken from plugins by Wayne Rasband.
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
 
import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import ij.util.Tools;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import ij.gui.NewImage.*;
import ij.io.*;
import java.io.*;
import java.awt.image.*;
import ij.process.ImageConverter.*;
import ij.plugin.*;

    public class RGB_Profiler implements PlugInFilter, MouseListener, MouseMotionListener, KeyListener {
        ImagePlus img,imred,imgreen,imblue;
        ImageCanvas canvas;
        ImagePlus plotImage;

    public int setup(String arg, ImagePlus img) {
         if (IJ.versionLessThan("1.31i"))
            return DONE;
        this.img = img;
        IJ.register(RGB_Profiler.class);
        if (!isSelection()) {
             IJ.showMessage("RGB_Profiler", "Image with line selection required.");
             return DONE;
        } else
            return DOES_ALL+NO_CHANGES;
    }

    public void run(ImageProcessor ip) {
        if (img.getType()!=img.COLOR_RGB) {
            IJ.showMessage("", "image must be color RGB");
            return;
        }
	Integer id = new Integer(img.getID());
        ImageWindow win = img.getWindow();
        canvas = win.getCanvas();
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);
	creatergb();
        updateProfile();
	positionPlotWindow();
    }

    void positionPlotWindow() {
        IJ.wait(500);
        if (plotImage==null || img==null) return;
        ImageWindow pwin = plotImage.getWindow();
        ImageWindow iwin = img.getWindow();
        if (pwin==null || iwin==null) return;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension plotSize = pwin.getSize();
        Dimension imageSize = iwin.getSize();
        if (plotSize.width==0 || imageSize.width==0) return;
        Point imageLoc = iwin.getLocation();
        int x = imageLoc.x+imageSize.width+10;
        if (x+plotSize.width>screen.width)
            x = screen.width-plotSize.width;
        pwin.setLocation(x, imageLoc.y);
        iwin.toFront();
   }
    public void mouseReleased(MouseEvent e) {
	    updateProfile();
    }
    public void keyReleased(KeyEvent e) {
        updateProfile();
    }

    void creatergb(){
	int lon,lar,x,y;
	ImageProcessor ip1,ipred,ipgreen,ipblue;
	ImagePlus iDic;
	ip1= img.getProcessor();
	lon = img.getWidth();
	lar = img.getHeight();
	imred = NewImage.createFloatImage("red",lon,lar,1,NewImage.FILL_BLACK);
	imblue = NewImage.createFloatImage("blue",lon,lar,1,NewImage.FILL_BLACK);
	imgreen = NewImage.createFloatImage("green",lon,lar,1,NewImage.FILL_BLACK);
	ipred = imred.getProcessor();
	ipblue = imblue.getProcessor();
	ipgreen = imgreen.getProcessor();
	int[] pixels = (int[])ip1.getPixels();
	for (y=0; y<lar; y++) {
                for (x=0; x<lon; x++) {
                    int pos = y*lon +x;
		    int c = pixels[pos];
		    int red =(c&0xff0000)>>16;
		    int green =(c&0x00ff00)>>8;
		    int blue =(c&0x0000ff);
		    ipred.putPixelValue(x,y,red);
		    ipblue.putPixelValue(x,y,blue);
		    ipgreen.putPixelValue(x,y,green);
		}
	}
    }
    
    void updateProfile() {
         if (!isSelection())
             return;
         checkPlotWindow();
	 
	 Roi sel;
	 sel = img.getRoi();
	 imred.setRoi(sel);
	 double[] yred,ygreen,yblue;
         ProfilePlot profile = new ProfilePlot(imred);
         yred = profile.getProfile();
         if (yred==null || yred.length==0)
             return;
	 imblue.setRoi(sel);
         profile = new ProfilePlot(imblue);
         yblue = profile.getProfile();
         if (yblue==null || yblue.length==0)
             return;
	 imgreen.setRoi(sel);
         profile = new ProfilePlot(imgreen);
         ygreen = profile.getProfile();
         if (ygreen==null || ygreen.length==0)
             return;
         int n = yred.length;
         double[] x = new double[n];
         for (int i=0; i<n; i++)
             x[i] = i;
         Plot plot = new Plot("Profile", "Distance","Value", x, yred);	 

	 plot.setLimits(0,n-1,0,256);
	 plot.setColor(java.awt.Color.blue);
	 plot.addPoints(x,yblue,2);
	 plot.setColor(java.awt.Color.green);
	 plot.addPoints(x,ygreen,2);
	 plot.setColor(java.awt.Color.red);
	 
         ImageProcessor ip = plot.getProcessor();
         if (plotImage==null) {
             plotImage = new ImagePlus("Profiles of "+img.getTitle(), ip);
             plotImage.show();
        }
        plotImage.setProcessor(null, ip);
    }

    // returns true if there is a line selection
    boolean isSelection() {
        if (img==null)
            return false;
        Roi roi = img.getRoi();
        if (roi==null)
            return false;
        int roiType = roi.getType();
        if (roiType==Roi.LINE || roiType==Roi.POLYLINE || roiType==Roi.FREELINE )
            return true;
       else
            return false;
    }

    // stop listening for mouse events if the plot window has been closed
    void checkPlotWindow() {
       if (plotImage==null) 
            return;
       ImageWindow win = plotImage.getWindow();
       if (win==null || win.isVisible()) 
           return;
       win = img.getWindow();
       if (win==null)
           return;
       canvas = win.getCanvas();
       canvas.removeMouseListener(this);
       canvas.removeMouseMotionListener(this);
       canvas.removeKeyListener(this);
   }


    public void mousePressed(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {}
    public void keyPressed(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}   
    public void mouseEntered(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}

}



