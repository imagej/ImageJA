import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.geom.*;
import ij.plugin.*;

public class Asymmetry_ implements PlugIn {
  ImagePlus imp; // original ImagePlus
  ImageProcessor ip;
  ImagePlus filteredImp; // ImagePlus for maxima detection
  ImageProcessor filteredIp;
  ImagePlus maxImp; // ImagePlus for maximum of intensity
  ImageProcessor maxIp;
  ImagePlus minImp1; // ImagePlus for min1
  ImageProcessor minIp1;
  ImagePlus minImp2; // ImagePlus for min2
  ImageProcessor minIp2;
  ImagePlus asymImp; // ImagePlus for asymmetry of min1 and min2
  ImageProcessor asymIp;

  String titleString; // properties of the analysed image
  int impWidth;
  int impHeight;
  int stackSize;
  boolean isFilteredStack; // whether the image for maxima detection is a stack
 
  boolean[][] mask; // the geometric mask for maxima detection
  int maskWidth;
  int maskHeight;
  boolean showMask; // whether the mask should be displayed

  double spacing; // distance between min and max
  double angle; // angle (where atom "1" can be found)
  int x; // x distance of atom "1"
  int y; // x distance of an atom "1"
  int xa;  // x distance of atom a
  int ya; // y distance of atom a
  int xb; // x distance of atom b
  int yb; // y distance of atom b
  boolean allMin; // whether 3 or only 1 atom should be used
  boolean showMinMax; // true if images for max and min should be displayed
  boolean showAsymmetry; // true if image with asymmetry should be displayed
  boolean fillAll; // option: fill all of the target image

  public void run(String arg) {
    titleString = IJ.getImage().getTitle(); // title of default picture

    /* DIALOG FOR PLUGIN OPTIONS */
    GenericDialog gd = new GenericDialog("Asymmetry Plugin");

    gd.addNumericField("Atomic spacing (px): ", 12, 0);
    gd.addNumericField("Orientation (-90...0 deg): ", -73 , 0);
    gd.addCheckbox("Use mean value of 3 neighbored atoms", true);
   
    int[] idArray = WindowManager.getIDList(); // list of all opened images (IDs)
    String[] titleArray = new String[idArray.length]; // titles of opened images
    for (int i = 0; i < idArray.length; i++) {
      titleArray[i] = WindowManager.getImage(idArray[i]).getTitle();
    }
    gd.addChoice("Image for maxima positions", titleArray, titleString);
    gd.addChoice("Image for asymmetry values", titleArray, titleString);

    String[] titleArray2 = new String[idArray.length + 2];
    titleArray2[0] = "draw circle";
    titleArray2[1] = "draw hexagon";
    for (int i = 2; i < idArray.length+2; i++) {
      titleArray2[i] = WindowManager.getImage(idArray[i-2]).getTitle();
    }
    gd.addChoice("Geometric mask", titleArray2, titleArray2[1]);
    gd.addCheckbox("Show mask", false);

    gd.addCheckbox("Show images with min and max", false);
    gd.addCheckbox("Show image with asymmetry", true);
    gd.addCheckbox("Fill all of the target image", true);
    gd.showDialog();
    if (gd.wasCanceled()) {
      IJ.error("Plugin canceled!");
      return;
    }

    spacing = (double) gd.getNextNumber();
    angle = ((double) gd.getNextNumber())*(3.14159264)/180.0;

    allMin = gd.getNextBoolean();
    String imp1Title = gd.getNextChoice();
    String imp2Title = gd.getNextChoice();
    String maskImpTitle = gd.getNextChoice();
    showMask = gd.getNextBoolean();
    showMinMax = gd.getNextBoolean();
    showAsymmetry = gd.getNextBoolean();
    if (!showMinMax && !showAsymmetry && !showMask) { // Plugin hasn't to do anything
      IJ.error("No output selected. Plugin canceled");
      return;
    }		
    fillAll = gd.getNextBoolean();

    filteredImp = WindowManager.getImage(imp1Title); // the image for maxima detection
    filteredIp = filteredImp.getProcessor(); // its ImageProcessor
    imp = WindowManager.getImage(imp2Title); // the image we get the values from
    ip = imp.getProcessor();
    // checks if images have same size:
    if (!(ip.getWidth() == filteredIp.getWidth() && ip.getHeight() == filteredIp.getHeight())) {
      IJ.error("Error", "Image sizes do not match. Plugin canceled.");
      return;
    }

    if (maskImpTitle == "draw circle") { // create geometric mask (circle)
      int searchRadius = (int) IJ.getNumber("Circle radius", spacing);
      int maskSize = (2*searchRadius)+1; // should be odd number -> +1
      mask = new boolean[maskSize][maskSize];
      for (int i = 0; i < maskSize; i++) {
        for (int j = 0; j < maskSize; j++) {
	  int x = i - searchRadius;
	  int y = j - searchRadius; // sign doesn't matter
	  if ((x*x)+(y*y) <= searchRadius*searchRadius) mask[i][j] = true;
	  else mask[i][j] = false;
        }
      }
      maskWidth = maskSize;
      maskHeight = maskSize;
    }

    if (maskImpTitle == "draw hexagon") { // create geometric mask (hexagon)
      int searchRadius = (int) IJ.getNumber("Hexagon radius", spacing);
      int maskSize = (2*searchRadius)+1; // should be odd number -> +1
      mask = new boolean[maskSize][maskSize];
      for (int i = 0; i < maskSize; i++) {
        for (int j = 0; j < maskSize; j++) {
	  int x = i - searchRadius;
	  int y = -(j - searchRadius); // sign of y must be right for method cartesianToPolar and inHexagon
          Point cartesian = new Point(x, y);
	  Point2D.Double polar = cartesianToPolar(cartesian);
	  if (inHexagon(polar, searchRadius, angle)) mask[i][j] = true;
	  else mask[i][j] = false;	  
        }
      }
      maskWidth = maskSize;
      maskHeight = maskSize;
    }	

    if (maskImpTitle != "draw circle" && maskImpTitle != "draw hexagon") { // create geometric mask from image
      ImagePlus maskImp = WindowManager.getImage(maskImpTitle);
      ImageProcessor maskIp = maskImp.getProcessor();
      mask = new boolean[maskImp.getWidth()][maskImp.getHeight()];
      for (int i = 0; i < maskIp.getWidth(); i++) {
        for (int j = 0; j < maskIp.getHeight(); j++) {
          double pixelValue = maskIp.getPixelValue(i, j);
          if (pixelValue > 0.9 && pixelValue < 1.1) { // range 0.9 ... 1.1 avoids problems with imprecise numbers
            mask[i][j] = true;
          } else { mask[i][j] = false; }
	}
      }
      maskWidth = maskIp.getWidth();
      maskHeight = maskIp.getHeight();
    }

    if (showMask) {
      ImagePlus showMaskImp = NewImage.createFloatImage("Mask", maskWidth, maskHeight, 1, NewImage.FILL_BLACK);
      ImageProcessor showMaskIp = showMaskImp.getProcessor();
      for (int i = 0; i < maskWidth; i++) {
        for (int j = 0; j < maskHeight; j++) {
          if (mask[i][j]) showMaskIp.putPixelValue(i, j, 1);
          else showMaskIp.putPixelValue(i, j, 0);
        }
      }
      showMaskImp.show();
    }
		
    /* GET PROPERTIES OF SELECTED IMAGES / STACKS */
    impWidth = imp.getWidth();
    impHeight = imp.getHeight();
    stackSize = imp.getStackSize();
    if (stackSize > 1 && filteredImp.getStackSize() == stackSize) { // if filtered image is a stack of same size
      isFilteredStack = true;
    }
    else {
      isFilteredStack = false;
    }
    titleString = imp.getTitle(); // titleString is now title of selected image with real values
		
		
    /* INITIALIZE IMAGES FOR MIN/MAX AND ASYMMETRY (BOTH OPTIONAL) */
    if (showMinMax) {
      maxImp = NewImage.createFloatImage("Maximum intensity", impWidth, impHeight, stackSize, NewImage.FILL_BLACK);
      maxIp = maxImp.getProcessor();
      minImp1 = NewImage.createFloatImage("Minimum intensity 1", impWidth, impHeight, stackSize, NewImage.FILL_BLACK);
      minIp1 = minImp1.getProcessor();
      minImp2 = NewImage.createFloatImage("Minimum intensity 2", impWidth, impHeight, stackSize, NewImage.FILL_BLACK);
      minIp2 = minImp2.getProcessor();
      maxImp.show();
      minImp1.show();
      minImp2.show();
    }
    if (showAsymmetry) {
      asymImp = NewImage.createFloatImage("Asymmetry", impWidth, impHeight, stackSize, NewImage.FILL_BLACK);
      asymIp = asymImp.getProcessor();
      asymImp.show();
    }

    /* COMPUTE PIXEL VALUES FROM SPACING AND ANGLE */
    x= (int) (spacing * Math.cos(angle));
    y= (int) (spacing * Math.sin(angle));
    xa= (int) (spacing * Math.cos(angle+2.094395)); // + 120 deg
    ya= (int) (spacing * Math.sin(angle+2.094395));
    xb= (int) (spacing * Math.cos(angle+4.18879)); // + 240 deg
    yb= (int) (spacing * Math.sin(angle+4.18879));

    /* FOR EVERY SLICE DO ... */
    for (int s = 1; s <= stackSize; s++) {
      // select slice s in all necessary stacks
      if (isFilteredStack) {
        filteredImp.setSlice(s);
        filteredIp = filteredImp.getProcessor();
      }
      if (showMinMax) {
        maxImp.setSlice(s);
        maxIp = maxImp.getProcessor();
      }
      if (showAsymmetry) {
        asymImp.setSlice(s);
        asymIp = asymImp.getProcessor();
      }
        imp.setSlice(s);
        ip = imp.getProcessor();
      /* FOR EVERY PIXEL DO ... */
      for (int i = maskWidth/2; i < impWidth - (maskWidth/2); i++) {
        for (int j = maskHeight/2; j < impHeight - (maskHeight/2); j++) {
          Point maxPoint = maxPosition(i, j); // find maximum point for position i, j
          Point minPoint1 = new Point(maxPoint.x - x, maxPoint.y + y);
          Point minPoint2 = new Point(maxPoint.x + x, maxPoint.y - y); // y coordinates: the other way round
          Point minPoint1a = new Point(maxPoint.x - xa, maxPoint.y + ya); 
          Point minPoint2a = new Point(maxPoint.x + xa, maxPoint.y - ya);
          Point minPoint1b = new Point(maxPoint.x - xb, maxPoint.y + yb);
          Point minPoint2b = new Point(maxPoint.x + xb, maxPoint.y - yb);
			
	  // get pixel values
          double max = ip.getPixelValue(maxPoint.x, maxPoint.y);
          double min1 = ip.getPixelValue(minPoint1.x, minPoint1.y);
          double min2 = ip.getPixelValue(minPoint2.x, minPoint2.y);
          if (allMin) { // 'use mean value of 3 neighbored atoms' option 
            min1 += ip.getPixelValue(minPoint1a.x, minPoint1a.y); 
            min2 += ip.getPixelValue(minPoint2a.x, minPoint2a.y);
            min1 += ip.getPixelValue(minPoint1b.x, minPoint1b.y);
            min2 += ip.getPixelValue(minPoint2b.x, minPoint2b.y);
            min1/=3.0; // mean value
            min2/=3.0;
          }
          
	  // Option 1: Fill all of the target image
	  if (fillAll) {
	    if (showMinMax) {
	      maxIp.putPixelValue(i, j, max);
	      minIp1.putPixelValue(i, j, min1);
	      minIp2.putPixelValue(i, j, min2);
	    }
	    if (showAsymmetry) {
	      double asym=(min2 - min1)/(max - 0.5*min1 - 0.5*min2);
	      // clip asym at -1, +1:
	      if (asym > 1) asym=1;
	      if (asym < -1) asym=-1;
	      asymIp.putPixelValue(i, j, asym);
	    }
	  }
			
	  // Option 2: Put data only at maxima positions
	  if (!fillAll) {
	    if (showMinMax) {
	      maxIp.putPixelValue(maxPoint.x, maxPoint.y , max);
	      minIp1.putPixelValue(maxPoint.x, maxPoint.y , min1);
	      minIp2.putPixelValue(maxPoint.x, maxPoint.y , min2);
	    }
	    if (showAsymmetry) {
	      double asym=(min2 - min1)/(max - 0.5*min1 - 0.5*min2);
	      // clip asym at -1, +1:
	      if (asym > 1) asym=1;
	      if (asym < -1) asym=-1;
	      asymIp.putPixelValue(maxPoint.x, maxPoint.y , asym);
	    }
	  }
			
	} // end for j
      } // end for i
    } // end for s
    if (showAsymmetry) {
      asymImp.updateAndDraw(); // redraw asym image
    }
  } // end method


  /* FIND NEXT MAXIMUM METHOD */
  public Point maxPosition(int i, int j) {
    double maxIntensity = -1e99; // should be small enough to be overwritten within first iterations
    Point maxPoint = new Point(i, j);
    for (int k = 0; k < maskWidth; k++) {
      for (int l = 0; l < maskHeight; l++) {
        int absoluteX = i - (maskWidth/2) + k; // 'centers' mask
        int absoluteY = j - (maskHeight/2) + l;
        if (mask[k][l]) { // if this pixel is inside geometric mask
	  if (filteredIp.getPixelValue(absoluteX, absoluteY) > maxIntensity) {
            maxIntensity = filteredIp.getPixelValue(absoluteX, absoluteY);
            maxPoint.x = absoluteX;
	    maxPoint.y = absoluteY;
          }
        }
      }
    }
    return maxPoint;
  } // end method

  /* METHODS FOR HEXAGONS */
  public boolean inHexagon(Point2D.Double polar, double radius, double angle) { 
    // shift angle to between -90 and 0 deg
      while (angle > 0) 
	  {
	      angle -=(1.0/3.0*Math.PI);
	  }
      while (angle < -0.5*Math.PI) 
	  {
	      angle +=(1.0/3.0*Math.PI);
	  }

    double phi = polar.getY() - angle;
    phi = phi % (1.0/3.0*Math.PI); // gets angle to next edge of hexagon
    double rMax = Math.sqrt(3.0)*radius/2.0/Math.cos((Math.PI/6.0)-phi);
    return (polar.getX() <= rMax);
  }
	
  public Point2D.Double cartesianToPolar(Point cartesian) {
    double x = cartesian.getX();
    double y = cartesian.getY();
    double r = Math.sqrt((x*x) + (y*y));
    double phi = Math.atan2(y, x); // atan2 parameters must be the other way round
    if (phi < 0) phi += 2*Math.PI; // changes range from [-Pi, Pi] to [0 ... 2Pi]
      return new Point2D.Double(r, phi);
    }

} // END CLASS
