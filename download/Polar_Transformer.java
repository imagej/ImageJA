import ij.*;
import ij.process.*;
import ij.plugin.*;
import java.lang.*;
import java.awt.*;
import java.awt.image.*;
import java.text.DecimalFormat;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.gui.*;


/*

    This plugin will convert images to and from polar coordinates. It assumes that the polar image
    uses the y value to represent the angle (theta) and the x value for r (distance from the center). The
    "180" option assumes that the x values lines in polar space go from -radius to +radius and that there
    are 180 degrees worth of data (the way a sinogram is stored in computed tomography), while the
    "360" option assumes that the x values go from 0 to +radius and that there are 360 degrees worth of data
    (a more intuitive option for other applications). The default setting is that the origin (0,0) of Cartesian space
    is the center of the image, though there is an option to manually change it. You can also define the origin
    by drawing a selection (point, line or area) onto the initial image *before* launching the plugin.
    The plug-in produces a 16-bit grayscale image for 8 or 16-bit grayscale inputs, and an RGB color image
    for RGB inputs. Choosing "polar" will assume that input image is in Cartesian space and will remap it to
    polar coordinates, while choosing "Cartesian" will assume that the input image is in polar coordinates and
    will remap it to Cartesian space.
    By default, when creating a polar map, the plug-in will use 1 line for each degree (either 180 or 360, depending
    upon the above option) but this can be manually changed.

*/


// -- Written by Edwin F. Donnelly
// -- edwin.donnelly@vanderbilt.edu

// -- Updated 15.02.2008 by Frédéric Mothe (mothe@nancy.inra.fr):
//    Main changes :
//       - selection may be used for giving the default centre
//       - calibration function, contrast and pixel size are copied to the transformed image
//       - clockWise option added

public class Polar_Transformer implements PlugIn
{
 int widthInitial, heightInitial, widthTransform, heightTransform;
 double centerX, centerY;
 ImageProcessor ipTransform, ipInitial;
 ImagePlus iTransform, iInitial;

 // Persistent options with default values:
 static boolean toPolar = true;
 static boolean polar180 = false; // true;
 static boolean defaultLines = true;
 static boolean defaultCenter = true;
 static boolean clockWise = false; // true

 boolean isColor = false;
 String title;
 static String[] op1 = {"Polar", "Cartesian"};
 static String[] op2 = {"180", "360"};
 int angleLines = 180;
 int [] rgbArray = new int[3];
 int [] xLyL = new int[3];
 int [] xLyH = new int[3];
 int [] xHyL = new int[3];
 int [] xHyH = new int[3];

 public void run(String arg)
 {
  iInitial = WindowManager.getCurrentImage();
  if(iInitial ==null)
	{IJ.noImage();return ;}
  ipInitial = iInitial.getProcessor();

  if (showDialog(ipInitial))
  {

   widthInitial = ipInitial.getWidth();
   heightInitial = ipInitial.getHeight();
   if (ipInitial instanceof  ColorProcessor) isColor = true;

   if (toPolar)
   {
     title = "Polar Transform of "+iInitial.getTitle();
     if(polar180) angleLines = 180;
     else angleLines = 360;
     if(!defaultLines) getLines();
     if (polar180) polar180();
     else polar360();
   }
   else
   {
     title = "Cartesian Transform of "+iInitial.getTitle();
     if (polar180) cart180();
     else cart360();
   }

   // Copy settings from the original to the transformed image:
   ipTransform.setMinAndMax(ipInitial.getMin (), ipInitial.getMax ());
   ipTransform.setCalibrationTable (ipInitial.getCalibrationTable ());
   iTransform = new ImagePlus(title, ipTransform);
   iTransform.setCalibration (iInitial.getCalibration());
   iTransform.show();
  }

 }


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////// -- polar180()
 public void polar180()
 {

  // Establish the default center of Cartesian space
  getPolarCenter ();

  // Set up the Polar Grid:
  // Use y values for angles
  // -- 180 degrees (0 to 179...)
  heightTransform = angleLines;

  // Line width will be:
  //  --  equal to 2*radius+1 -- Need to find the greatest radius
  //  --  (4 possibilities: from center to each corner)
  //  --  Top-Left Corner (0,0):
  double radius = Math.sqrt((centerX-0)*(centerX-0) + (centerY-0)*(centerY-0));
  //  --  Top-Right Corner (widthInitial, 0):
  double radiusTemp = Math.sqrt((centerX-widthInitial)*(centerX-widthInitial) + (centerY-0)*(centerY-0));
  if (radiusTemp>radius) radius = radiusTemp;
  //  --  Bottom-Left Corner (0, heightInitial):
  radiusTemp = Math.sqrt((centerX-0)*(centerX-0) + (centerY-heightInitial)*(centerY-heightInitial));
  if (radiusTemp>radius) radius = radiusTemp;
  //  -- Bottom-Right Corner (widthInitial , heightInitial):
  radiusTemp = Math.sqrt((centerX-widthInitial)*(centerX-widthInitial) + (centerY-heightInitial)*(centerY-heightInitial));
  if (radiusTemp>radius) radius = radiusTemp;
  int radiusInt = (int)radius;
  widthTransform = radiusInt*2+1;


  // -- Create the new image
  if (isColor) ipTransform = new ColorProcessor(widthTransform, heightTransform);
  else ipTransform = new ShortProcessor(widthTransform, heightTransform);

  // Fill the Polar Grid
  IJ.showStatus("Calculating...");
  for (int yy = 0; yy < heightTransform; yy++)
  {
   for (int xx = 0; xx < widthTransform; xx++)
   {

    // -- For each polar pixel, need to convert it to Cartesian coordinates
    double r = xx - radiusInt;
    double angle = (yy/(double)angleLines)*Math.PI;

    // -- Need convert (x,y) into pixel coordinates
    double x = getCartesianX (r, angle) + centerX;
    double y = getCartesianY (r, angle) + centerY;

    if (isColor)
    {
     interpolateColorPixel(x, y);
     ipTransform.putPixel(xx,yy,rgbArray);
    }
    else
    {
     double newValue = ipInitial.getInterpolatedPixel(x,y);
     ipTransform.putPixelValue(xx,yy,newValue);
    }

    // -- End out the loops
   }
   IJ.showProgress(yy, heightTransform);
  }
  IJ.showProgress(1.0);
 }


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////// -- polar360()
 public void polar360()
 {

  // Establish the default center of Cartesian space
  getPolarCenter ();

  // Set up the Polar Grid:
  // Use y values for angles
  // -- Need 360 degrees (0 to 359...)
  heightTransform = angleLines;

  // Line width will be:
  //  --  equal to radius -- Need to find the greatest radius
  //  --  (4 possibilities: from center to each corner)
  //  --  Top-Left Corner (0,0):
  double radius = Math.sqrt((centerX-0)*(centerX-0) + (centerY-0)*(centerY-0));
  //  --  Top-Right Corner (widthInitial, 0):
  double radiusTemp = Math.sqrt((centerX-widthInitial)*(centerX-widthInitial) + (centerY-0)*(centerY-0));
  if (radiusTemp>radius) radius = radiusTemp;
  //  --  Bottom-Left Corner (0, heightInitial):
  radiusTemp = Math.sqrt((centerX-0)*(centerX-0) + (centerY-heightInitial)*(centerY-heightInitial));
  if (radiusTemp>radius) radius = radiusTemp;
  //  -- Bottom-Right Corner (widthInitial , heightInitial):
  radiusTemp = Math.sqrt((centerX-widthInitial)*(centerX-widthInitial) + (centerY-heightInitial)*(centerY-heightInitial));
  if (radiusTemp>radius) radius = radiusTemp;
  int radiusInt = (int)radius;
  widthTransform = radiusInt;

  // -- Create the new image
  if (isColor) ipTransform = new ColorProcessor(widthTransform, heightTransform);
  else ipTransform = new ShortProcessor(widthTransform, heightTransform);

  // Fill the Polar Grid
  IJ.showStatus("Calculating...");
  for (int yy = 0; yy < heightTransform; yy++)
  {
   for (int xx = 0; xx < widthTransform; xx++)
   {

    // -- For each polar pixel, need to convert it to Cartesian coordinates
    double r = xx;
    double angle = (yy/(double)angleLines)*Math.PI*2;

    // -- Need convert (x,y) into pixel coordinates
    double x = getCartesianX (r, angle) + centerX;
    double y = getCartesianY (r, angle) + centerY;

    if (isColor)
    {
     interpolateColorPixel(x, y);
     ipTransform.putPixel(xx,yy,rgbArray);
    }
    else
    {
     double newValue = ipInitial.getInterpolatedPixel(x,y);
     ipTransform.putPixelValue(xx,yy,newValue);
    }

    // -- End out the loops
   }
   IJ.showProgress(yy, heightTransform);
  }
  IJ.showProgress(1.0);
 }


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////// -- cart180()
 public void cart180()
 {

  // Set up the Cartesian Grid:
  // -- Will have radius x radius pixels
  heightTransform = widthInitial;
  widthTransform = widthInitial;

  // Establish the default center of Cartesian space
  getCartesianCenter ();

  // -- Create the new image
  if (isColor) ipTransform = new ColorProcessor(widthTransform, heightTransform);
  else ipTransform = new ShortProcessor(widthTransform, heightTransform);

  // Fill the Cartesian Grid
  IJ.showStatus("Calculating...");
  for (int yy = 0; yy < heightTransform; yy++)
  {
   for (int xx = 0; xx < widthTransform; xx++)
   {

    // -- For each Cartesian pixel, need to convert it to Polar coordinates
    double x = xx - centerX;
    double y = yy - centerY;
    double r = getRadius (x, y);
    double angle = getAngle(x,y);

    // -- Need convert (x,y) into pixel coordinates
    if (angle >= 180) {
     angle = angle - 180;
     x = -r;
    } else {
     x = r;
    }

    x = x + widthInitial/2;
    y = angle*(heightInitial/180.0);

    if (isColor)
    {
     interpolateColorPixel(x, y);
     ipTransform.putPixel(xx,yy,rgbArray);
    }
    else
    {
     double newValue = ipInitial.getInterpolatedPixel(x,y);
     ipTransform.putPixelValue(xx,yy,newValue);
    }

    // -- End out the loops
   }
   IJ.showProgress(yy, heightTransform);
  }
  IJ.showProgress(1.0);
 }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////// -- cart360()
 public void cart360()
 {

  // Set up the Cartesian Grid:
  // -- Will have radius x radius pixels
  heightTransform = widthInitial*2+1;
  widthTransform = widthInitial*2+1;

  // Establish the default center of Cartesian space
  getCartesianCenter ();

  // -- Create the new image
  if (isColor) ipTransform = new ColorProcessor(widthTransform, heightTransform);
  else ipTransform = new ShortProcessor(widthTransform, heightTransform);

  // Fill the Cartesian Grid
  IJ.showStatus("Calculating...");
  for (int yy = 0; yy < heightTransform; yy++)
  {
   for (int xx = 0; xx < widthTransform; xx++)
   {

    // -- For each Cartesian pixel, need to convert it to Polar coordinates
    double x = xx - centerX;
    double y = yy - centerY;
    double r = getRadius (x, y);
    double angle = getAngle(x,y);

    // -- Need convert (x,y) into pixel coordinates
    x = r;
    y = angle*(heightInitial/360.0);

    if (isColor)
    {
     interpolateColorPixel(x, y);
     ipTransform.putPixel(xx,yy,rgbArray);
    }
    else
    {
     double newValue = ipInitial.getInterpolatedPixel(x,y);
     ipTransform.putPixelValue(xx,yy,newValue);
    }

    // -- End out the loops
   }
   IJ.showProgress(yy, heightTransform);
  }
  IJ.showProgress(1.0);
 }


 boolean showDialog(ImageProcessor ip)
 {
  GenericDialog gd = new GenericDialog("Polar Transformer");
  gd.addChoice("Method:", op1, op1[toPolar ? 0 : 1]);
  gd.addChoice("Degrees used for Polar Space:", op2, op2[polar180 ? 0 : 1]);
  gd.addCheckbox("Default Center for Cartesian Space", defaultCenter);
  gd.addCheckbox("For Polar Transforms, Use 1 Line Per Angle", defaultLines);
  gd.addCheckbox("Clock-wise rotation", clockWise);
  gd.showDialog();
  if (gd.wasCanceled()) return false;
  toPolar = (gd.getNextChoiceIndex() == 0);
  polar180 = (gd.getNextChoiceIndex() == 0);
  defaultCenter = gd.getNextBoolean();
  defaultLines = gd.getNextBoolean();
  clockWise = gd.getNextBoolean();
  return true;
 }

 public void getCartesianCenter ()
 {
  centerX = widthTransform/2;
  centerY = heightTransform/2;
  if (! defaultCenter) {
   getCenter();
  }
 }

 public void getPolarCenter ()
 {
  // If a roi was defined, use it as default center :
  Rectangle b = new Rectangle(0, 0, widthInitial, heightInitial);
  Roi roi = iInitial.getRoi();
  if (roi!=null) {
   b = roi.getBounds();
  }
  centerX = b.x + b.width/2;
  centerY = b.y + b.height/2;
  if (! defaultCenter) {
   getCenter();
  }
 }

 void getCenter()
 {
  GenericDialog gd = new GenericDialog("Center of Cartesian Grid");
  gd.addNumericField("Center x Coordinate:", centerX, 2);
  gd.addNumericField("Center y Coordinate:", centerY, 2);
  gd.showDialog();
  centerX =  gd.getNextNumber();
  centerY =  gd.getNextNumber();
 }

 void getLines()
 {
  GenericDialog gd = new GenericDialog("Polar Transform Options");
  gd.addNumericField("Number of Lines in Angle Dimension:", angleLines, 0);
  gd.showDialog();
  angleLines =  (int)gd.getNextNumber();
 }

 double getCartesianX (double r, double angle)
 {
  return r*Math.cos(angle);
 }

 double getCartesianY (double r, double angle)
 {
  double y = r*Math.sin(angle);
  return clockWise ? -y : y;
 }

 double getRadius(double x, double y)
 {
  return Math.sqrt(x*x+ y*y);
 }

 double getAngle(double x, double y)
 {
  // Returns an angle in the range [0, 360[
  double angle = Math.toDegrees (Math.atan2 (y, x));
  if (angle < 0) {
   angle += 360;
  }
  return clockWise ? 360 - angle : angle;
 }

  void interpolateColorPixel(double x, double y)
 {

  int xL, yL;

  xL = (int)Math.floor(x);
  yL = (int)Math.floor(y);
  xLyL = ipInitial.getPixel(xL, yL, xLyL);
  xLyH = ipInitial.getPixel(xL, yL+1, xLyH);
  xHyL = ipInitial.getPixel(xL+1, yL, xHyL);
  xHyH = ipInitial.getPixel(xL+1, yL+1, xHyH);
  for (int rr = 0; rr<3; rr++)
  {
    double newValue = (xL+1-x)*(yL+1-y)*xLyL[rr];
    newValue += (x-xL)*(yL+1-y)*xHyL[rr];
    newValue += (xL+1-x)*(y-yL)*xLyH[rr];
    newValue += (x-xL)*(y-yL)*xHyH[rr];
    rgbArray[rr] = (int)newValue;
  }
 }

}
