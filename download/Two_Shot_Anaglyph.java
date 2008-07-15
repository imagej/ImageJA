import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Two_Shot_Anaglyph implements PlugInFilter {
    ImagePlus imp;
    private String[] titles;
    int[] wList;
    int leftImageIndex;
    int rightImageIndex;
    ImagePlus Left;
    ImagePlus Right;
    
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB;
    }
    
    public void run(ImageProcessor ip) {
        wList = WindowManager.getIDList();
        if (wList==null || wList.length<2) {
            IJ.showMessage("Anaglyph", "There must be at least two windows open");
            return;
        }
        
        titles = new String[wList.length];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null)
                titles[i] = imp.getTitle();
            else
                titles[i] = "";
        }
        
        if (!showDialog())
            return;
        
        Anaglyph(Left, Right);
    }
    
    public boolean showDialog() {
        GenericDialog gd = new GenericDialog("Anaglyph");
        gd.addMessage("Choose two images to make a stereo image.\n If the result doesn't work swap images.");
        gd.addChoice("Left image:", titles, titles[0]);
        gd.addChoice("Right image:", titles, titles[1]);
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        int leftImageIndex = gd.getNextChoiceIndex();
        int rightImageIndex = gd.getNextChoiceIndex();
        Left = WindowManager.getImage(wList[leftImageIndex]);
        Right = WindowManager.getImage(wList[rightImageIndex]);
        return true;
    }
    public void Anaglyph(ImagePlus Left, ImagePlus Right){
        int LWidth = Left.getWidth();
        int RWidth = Right.getWidth();
        int LHeight = Left.getHeight();
        int RHeight = Right.getHeight();
        
        if(LWidth!=RWidth && LHeight!=RHeight){
            IJ.showMessage("Anaglyph", "Images must have equal dimensions");
            return;
        }
        
        ImageProcessor anaglyphIP = new ColorProcessor(LWidth,LHeight);
        ImageProcessor LeftIP = Left.getProcessor();
        ImageProcessor RightIP = Right.getProcessor();
        int red, green, blue;
        int[] StereoPixels = (int[])anaglyphIP.getPixels();
        int[] LeftPixels = (int[])LeftIP.getPixels();
        int[] RightPixels = (int[])RightIP.getPixels();
        
        for(int i=0;i<LWidth*LHeight;i++){
            red = (LeftPixels[i] & 0xff0000)>>16;       //Left eye image consists of red channel
            green = (RightPixels[i] & 0xff00ff00)>>8;   // Right eye image consists of green and blue channel
            blue = (RightPixels[i] & 0x0000ff);
            
            StereoPixels[i] = ((red & 0xff)<<16)+((green & 0xff)<<8)
            + (blue & 0xff);
        }
        anaglyphIP.setPixels(StereoPixels);
        ImagePlus StereoImage = new ImagePlus("Stereo Image", anaglyphIP);
        StereoImage.show();
    }
    
}
