import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import ij.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.Animator;
import ij.gui.*;
import ij.plugin.MacroInstaller;
import javax.swing.*;
import javax.swing.JOptionPane;
import javax.swing.JDialog;



/* This plugin allows the user to adjust an ROI by bumping it with a circle with a diameter that the user chooses.
 * To use, open an image, draw an roi on the image, click on the green circle in the toolbar then click on the image and drag the circle
 * into the part of the ROI to push it in a specific direction.  To adjust the size of the circle double click on the green
 * circle in the toolbar and enter the desired diameter.  
 * This plugin is a modification of the ROI_brush Tool plugin.  The ROI_brush Tool plugin was written by Johannes 
 * Schindelin and Tom Larkworthy.  The idea of writing this ROI_adjust plugin was Larry Dougherty's and the coding 
 * was done by Gamaliel Isaac both of who are from the department of Radiology at the University of Pennsylvania.
 **/



public class ROI_adjust implements PlugInFilter {

	ImagePlus imp;
 public static Roi labelRoi,roiWOcircles,adjustedROI;
 
public static ShapeRoi adjustedShapeROI;
static ShapeRoi roiShapeWOcircles; 
 public static int width,height,roiIncrement;
static String message;
static ShapeRoi roiPlusCircle;


public ROI_adjust()
{
    // IJ.log("ROI_adjust.java public ROI_adjust()");
    imp = WindowManager.getCurrentImage();
    setup("",imp);
   ImageProcessor ip = imp.getProcessor();
    run(ip);
}

        
public int setup(String arg, ImagePlus imp) 
{
    // IJ.log("ROI_adjust.java public int setup()");
		this.imp = imp;
                width = 20;
                height = 20;
                roiIncrement = 5;
                message = "";
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
    // IJ.log("ROI_adjust.java public run()");
            
            
                 if (IJ.versionLessThan("1.37c"))
            return;
                 
                 
		int numSlices = imp.getStackSize();
		ImageStack stack = imp.getStack();
               
	//	ROIadjust ms = new ROIadjust(imp);
                

        MacroInstaller installer = new MacroInstaller();
        
      
          
            String macros =                
                "var radius = 20;\n" +
                "var leftClick=16, alt=9;\n" +
                "macro 'Roi Adjust Tool - C0d60O11ff' {\n" +
                " while (true) {\n" +
                " getCursorLoc(x, y, z, flags);\n" +     
                " xmr = x-radius;\n" +
                " ymr = y-radius;\n" +     
                " call('ROI_adjust.makeOval2', x, y,radius,radius);\n" +   
                "wait(10);\n" +   
                " call('ROI_adjust.drawOval',x,y,radius,radius);\n" +                
                "  if (flags&leftClick==0){\n" +
                "call('ROI_adjust.forgetRoi');\n"+ 
                "call('ROI_adjust.eraseOval');\n"+    
                "call('ROI_adjust.shapeToROI');\n"+                             
                 " exit();\n" +
                 "}\n" +
                 "  if (flags&alt==0){\n" +
                "  }else{\n" +
                "   call('ROI_adjust.unlabel', x,y,z,flags);\n" +
                    "}\n" +
                "  wait(5);\n" +
                " }\n" +
                "}\n" +
                "macro 'Roi Adjust Tool Options' {"+
                "radius = getNumber(\"Radius: \", radius);"+
                "}\n";
              
        installer.install(macros);
	}
        
       

 
public static void appendRoiType(String p_message,Roi roi)
{

    // IJ.log("ROI_adjust.java public appendRoiType()");

switch(roi.getType())
          {
             
              case (Roi.POLYGON):
                 message += p_message +  ": Roi Type is polygon"; 
              break;
              case (Roi.POLYLINE):
                 message += p_message +  ": Roi Type is polyline"; 
              break;
              case (Roi.ANGLE):
                 message += p_message +  ": Roi Type is angle"; 
              break;
              case (Roi.OVAL):
                 message += p_message +  ": Roi Type is oval"; 
              break;    
              case (Roi.LINE):
                 message += p_message +  ": Roi Type is line"; 
              break;
              case (Roi.POINT):
                 message += p_message +  ": Roi Type is point"; 
              break;
              case (Roi.RECTANGLE):
                 message += p_message +  ": Roi Type is rectangle"; 
              break;
              case (Roi.COMPOSITE):
                 message += p_message +  ": Roi Type is composite"; 
              break;
              case (Roi.FREELINE):              
                 message += p_message +  ": Roi Type is FREELINE"; 
              break;
              case (Roi.FREEROI):
                 message += p_message +  ": Roi Type is FREEROI";                  
              break;

          }
    message += "\n";
}            
   
        
        
//methods in a macro accessable format
    public static void label(String x, String y, String z, String flags, String width) {
        label(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z),
                Integer.parseInt(flags),
                Integer.parseInt(width));
    }

    public static void unlabel(String x, String y, String z, String flags) {
        unlabel(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z),
                Integer.parseInt(flags));                
    }
    
    public static void makeOval2(String x,String y,String radius,String height){
       makeOval2(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(radius),
                Integer.parseInt(height)
                ); 
    }
    
    
    public static void drawOval(String x, String y, String z, String flags) {
        drawOval(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z),
                Integer.parseInt(flags));                
    }

    
     public static void forgetRoi()
     {         
         // IJ.log("ROI_adjust.java forgetRoi()");
            labelRoi = null;
     }
     
     public static void eraseOval()
     {
         // IJ.log("ROI_adjust.java eraseOval()");
         if (roiWOcircles != null)
         {
             System.out.println("Erase Oval");
             IJ.getImage().setRoi(roiWOcircles);  
         }
         
     }
     
     public static void shapeToROI()
     {
         // IJ.log("ROI_adjust.java shapeToRoi()");
         adjustedROI = adjustedShapeROI.shapeToRoi();
         
 //        appendRoiType("ROI_adjust.java shapeToROI adjustedROI",adjustedROI);
         IJ.getImage().setRoi(adjustedROI); 
 //        IJ.showMessage(message);
     }
     
     public static void drawOval(int x, int y, int width, int height)
     {
         Roi circleRoi = getBrushRoi(x, y, width,height);
             IJ.getImage().setRoi(circleRoi); 
     }
        
        
	public static void makeOval2(int x, int y,int width,int height) {

         Roi roi2,brushROI;
         int xRadius = width/2;
         int yRadius = height/2;
         int xpr = x + xRadius;
         int ypr = y + yRadius;
         
         xpr = x;
         ypr = y;        
                
        if (labelRoi == null)
        {
          labelRoi = IJ.getImage().getRoi();
          roiWOcircles = IJ.getImage().getRoi();
        }
     
        if (labelRoi != null) {
            if (!(labelRoi instanceof ShapeRoi)) {
                labelRoi = new ShapeRoi(labelRoi);
                roiWOcircles = new ShapeRoi(roiWOcircles);
            }                                
            
            ShapeRoi roiShape = (ShapeRoi) labelRoi;      
            
            roiShapeWOcircles = (ShapeRoi) roiWOcircles;   
            
           
            if (roiShape.contains(x,y))
            {
              roiShape.or(getBrushRoi(xpr, ypr, width,height)); // this ors the roi
            }
            else
            {
              roiShape.not(getBrushRoi(xpr, ypr, width,height)); // this ors the roi
            }
            
            
            if (roiShapeWOcircles.contains(x,y))
            {
              roiShapeWOcircles.or(getBrushRoi(xpr, ypr, width,height)); // this ors the roi
            }
            else
            {
              roiShapeWOcircles.not(getBrushRoi(xpr, ypr, width,height)); // this ors the roi
            }                                  
            
            roi2 = (Roi) labelRoi.clone();
            roiPlusCircle = (ShapeRoi)roi2;            
            roiPlusCircle.xor(getBrushRoi(xpr, ypr, width,height)); // This adds the circle to the roi            
  //          adjustedShapeROI = roiPlusCircle;
              adjustedShapeROI = roiShapeWOcircles;
        } else {
            roi2 = getBrushRoi(xpr, ypr, width,height);
        }

          IJ.getImage().setRoi(roi2);       
          adjustedROI = roi2;
	}        
        
    
    public static void label(int x, int y, int z, int flags, int width) {
        Roi roi = IJ.getImage().getRoi();
        if (roi != null) {
            if (!(roi instanceof ShapeRoi)) {
                roi = new ShapeRoi(roi);
            }

            ShapeRoi roiShape = (ShapeRoi) roi;

            roiShape.or(getBrushRoi(x, y, width,height));
        } else {
            roi = getBrushRoi(x, y, width,height);
        }

        IJ.getImage().setRoi(roi);
    }

    public static void mylabel(int x, int y, int z, int flags, int width) {
        
        Roi roi2,brushROI;
                
                
        if (labelRoi == null)
        {
          labelRoi = IJ.getImage().getRoi();
        }
     //   Roi roi2 = (Roi)roi.clone();
        if (labelRoi != null) {
            if (!(labelRoi instanceof ShapeRoi)) {
                labelRoi = new ShapeRoi(labelRoi);
            }
            
            /*
             *start with the fused roi from last time without the circle
             fuse the roi and the circle.  
             *show the fused roi and the circle
             *To do that I need to make a copy of the fused roi xor it with the circle
             *and then display it.  The question is if I clone the roiShape
             *wont' it still point to an identical roi?  If so it will alter the ROI and
             *this won't work.  I need to start though that way. 
             *
             *Problem is that to set the roi I need to make it the roi of the image.
             *The first time I want to get the roi from the image.  The second time
             *I want to 
             */
        
            ShapeRoi roiShape = (ShapeRoi) labelRoi;           
            roiShape.or(getBrushRoi(x, y, width,height)); // this ors the roi
            roi2 = (Roi) labelRoi.clone();
            ShapeRoi roiPlusCircle = (ShapeRoi)roi2;            
            roiPlusCircle.xor(getBrushRoi(x, y, width,height)); // This adds the circle to the roi
        } else {
            roi2 = getBrushRoi(x, y, width,height);
        }

        IJ.getImage().setRoi(roi2);
        
    }

    public static void unlabel(int x, int y, int z, int flags) {
        Roi roi = IJ.getImage().getRoi();
        if (roi != null) {
            if (!(roi instanceof ShapeRoi)) {
                roi = new ShapeRoi(roi);
            }

            ShapeRoi roiShape = (ShapeRoi) roi;

            roiShape.not(getBrushRoi(x, y, width,height));

            IJ.getImage().setRoi(roi);
        }
        labelRoi = null;
    }


    private static ShapeRoi getBrushRoi(int x, int y, int width,int height) {
        return new ShapeRoi(new OvalRoi(x - width / 2, y - width / 2, width, height));
    }

} // class roi_adjust


