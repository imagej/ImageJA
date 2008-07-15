import java.awt.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.text.*;
import ij.plugin.PlugIn;

//This plugin correlates two 8-bit images or stacks. The resultant correlation plot is a 
//stack with the same number of slices as the stack with the fewer number.
//Many correlation plots need to be recontrasted. Pressing Apple/Shift/C will
//bring up the Brightness/Contrast menu.

public class Image_Correlator implements PlugIn {

    private static int index1;
    private static int index2;
    private static boolean displayCounts, collate;
    private ImageStack img1, img2;
    private int smallest;
    private int z1, z2, count;
                    
    public void run(String arg) {
        if (showDialog())
            correlate(img1, img2);
    }
    
    public boolean showDialog() {
        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.noImage();
            return false;
        }
        String[] titles = new String[wList.length];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null)
                titles[i] = imp.getTitle();
            else
                titles[i] = "";
        }
        if (index1>=titles.length)index1 = 0;
        if (index2>=titles.length)index2 = 0;
        GenericDialog gd = new GenericDialog("Image Correlator");
        gd.addChoice("Image1: ", titles, titles[index1]);
        gd.addChoice("Image2: ", titles, titles[index2]);
        gd.addCheckbox("Display Counts: ", displayCounts);
        gd.addCheckbox("Collate Z-Data (Stacks Only):", collate);
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        index1 = gd.getNextChoiceIndex();
        index2 = gd.getNextChoiceIndex();
        displayCounts = gd.getNextBoolean();
        collate = gd.getNextBoolean();
        String title1 = titles[index1];
        String title2 = titles[index2];
        //Test to find out if both images are 8-bit grayscale.
        ImagePlus sliceimg1 = WindowManager.getImage(wList[index1]);
        ImagePlus sliceimg2 = WindowManager.getImage(wList[index2]);
        if (sliceimg1.getType()!=sliceimg1.GRAY8 || sliceimg2.getType()!=sliceimg1.GRAY8) {
            IJ.showMessage("Image Correlator", "Both stacks must be 8-bit grayscale.");
            return false;
        }
        img1 = sliceimg1.getStack();
        img2 = sliceimg2.getStack();
        return true; 
   }
    
    public void correlate(ImageStack img1, ImageStack img2) {
        int size1 = img1.getSize();
        int size2 = img2.getSize();
        boolean unsigned = true;
        
        //Finds out which stack has fewer slices and sets it to be the 
        //number of slices in the correlation plot
        if (size1<=size2)
        smallest = size1;
        if (size2<size1)
        smallest = size2;
        int width = img1.getWidth();
        int height = img1.getHeight();
        
        ImageStack stackplot = new ImageStack(256, 256);
        ImageProcessor ip1, ip2, plot=null;
       
        for(int i=1; i<=smallest; i++) {
            ip1 = img1.getProcessor(i);
            ip2 = img2.getProcessor(i);
            if (i==1 || !collate)
                plot = new FloatProcessor(256, 256);
            for (int y=0; y<height; y++) {//Loop for Y-Values
                for (int x=0; x<width; x++) {//Loop for X-Values
                    z1 = (int)ip1.getPixelValue(x,y); // z-value of pixel (x,y)in stack #1 on slice s
                    z2 = 255-(int)ip2.getPixelValue(x,y); // z-value of pixel (x,y)in stack #2 on slice s
                            
                count = (int)plot.getPixelValue(z1, z2);
                count++;
                plot.putPixelValue(z1, z2, count);
                }
            }
            plot.invertLut();
            plot.resetMinAndMax();            
            if (i==1 || !collate)
                stackplot.addSlice("Correlation Plot", plot);
            IJ.showProgress((double)i/smallest);
            IJ.showStatus("Correlating slice: "+ i +"/" + smallest);
        }
        IJ.showProgress(1.0);  
        new ImagePlus("Correlation Plot", stackplot).show();
        IJ.run("Enhance Contrast", "saturated=0.5");
        if (displayCounts)
            displayCounts(stackplot);
    }
  
    boolean isStack(ImageStack i1, ImageStack i2){
        if ((i1.getSize()>1) && (i2.getSize()>1))
            return true;
        else
            return false;
    }
    
    void displayCounts(ImageStack plot) {
       StringBuffer sb = new StringBuffer();
       int count;
       int slices = plot.getSize();
       if (slices==1) {
            ImageProcessor ip = plot.getProcessor(1);
               for (int x=0; x<256; x++)
                  for (int y=255; y>=0; y--) {
                      count = (int)ip.getPixelValue(x,y);
                       if (count>0)
                           sb.append(x+"\t"+(255-y)+"\t"+count+"\n");
                   }
           new TextWindow("Non-zero Counts" , "X\tY\tCount", sb.toString(), 300, 400);
        } else {
           for (int slice=1; slice<=slices; slice++) {
               ImageProcessor ip = plot.getProcessor(slice);
               for (int x=0; x<256; x++)
                 for (int y=255; y>=0; y--) {
                      count = (int)ip.getPixelValue(x,y);
                       if (count>0)
                           sb.append(x+"\t"+(255-y)+"\t"+(slice-1)+"\t"+count+"\n");
                   }
           }
           new TextWindow("Non-zero Counts" , "X\tY\tZ\tCount", sb.toString(), 300, 400);
        }
    }
}

