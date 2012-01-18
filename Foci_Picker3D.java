/*
16.01.2012, "System.gc()" was removed in the code, in turn this speeds up the code very much. (pointed by Iulian Dragos).
16.05.2010, the released version of the FociPicker at Imagej website.
      swithed on the multithread function, one can choose the number of threads
29.01.2010, show AreaXY AreaXY and AreaYZ in the result table, to estimate the resolution of Z-stack.
22.01.2010, ZTolerance is given as pixels,  but result is filtered by the calibrated z scale.
21.01.2010  fix a bug that deletes one more local max when the this.analyze method return false. 

04.01.2009  the multithread function is swithed off. the number of threads is set to 1 manually;
            The background level is now 60% of the mean of middle 1/3 surrounding pixels.
21.12.2009  FocuPicker_V3.0:multi-threads, based on the number of available CPUs.

14.12.2009  FociPicker_V2.0: based on a new algorithm different from FociPicker_V1.0;
  use the function grwoFromOpenBorderIndex() of Class PixelCollection3D to analyze the object, grow objects from local maxima       

  
  Compare: for the analysis of the image"" Focipicker3D_V1.0 takes 15.7 seconds
              Focipicker3D_V2.0 takes 5 seconds

08.12.2009  programming

 * 23.04.2009, Guanghua Du, 
 * 3D object counter based on find local maximum and grow the maxima to object method. 
 * find the maximum center pixel if the maxium in the center of the voxel,
 * or the edge maxium if it's at the edge of the image.
 * based on theses maximum pixels, thresholden its surroundings until the drop reaches a Tolerance value (relative to maximum or absolute value).
 * this surrounding grows from a small voxels, increases until the surface of the voxel has no pixels fall under the hat of the Tolerance.
 * If one pixel..
 *
 * Name of variables: Numberof... start from 1----to----n
 *                    indexof.....start from 0----to----(n-1)
 *                    nFoci starts from 2 (nominous), !!!! Pixel value in the output image = No of the Foci +1, for example, 1st Foci has a value of 2.;
 */
import ij.*;
import ij.ImagePlus.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import java.lang.*;
import ij.measure.*;
import ij.util.*;
import java.sql.*;
import java.awt.*;
import java.util.*;
import java.util.ArrayList;
import java.awt.event.*;

public class Foci_Picker3D implements PlugIn, Runnable {

  /*********************** end of claiming variables **********************************/
  // constant
  static final String pluginName = "Foci_Picker3D";
  private static int[] Neighbor = { 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0,
      1, 1, 1, 0, 1, 1, 1 };
  private static int[] Surrounding = { 1, -1, -1, 1, -1, 0, 1, -1, 1, 1, 0, -1,
      1, 0, 0, 1, 0, 1, 1, 1, -1, 1, 1, 0, 1, 1, 1, 0, -1, -1, 0, -1, 0, 0, -1,
      1, 0, 0, -1, 0, 0, 1, 0, 1, -1, 0, 1, 0, 0, 1, 1, -1, -1, -1, -1, -1, 0,
      -1, -1, 1, -1, 0, -1, -1, 0, 0, -1, 0, 1, -1, 1, -1, -1, 1, 0, -1, 1, 1, };
  private static final int N_NEIGHBOR = 7;
  private static final int N_SURROUNDING = 26;
  private static final short RIM_VALUE = 1; // RIM_Value must >=1;
  private static final boolean SUCCESS = true;
  private static final boolean NOSUCCESS = false;

  // startup dialog GUI, for input of parameters
  GenericDialog gd;

  // input parameters
  float MinISetting, MinIValue;
  float ToleranceSetting, ToleranceValue; // ToleranceValue== ToleranceSetting
  float UniformBackground; // background level for all foci objects
  float VoxelX, VoxelY, VoxelZ; // image scale == calibration factors, pixels
                                // per micrometer
  int UnitX, UnitY, UnitZ; // defining a box used for seaching for local maximum
                           // in findMaximum3D()
  int MinVolume; // the minimum number of pixels of a defined focus object
  int AutoBKGRadius; // the radius (actually the length of edge of a rectangle,
                     // used to obtain the background. i.e. the darkest pixel
                     // value
  float ContrastFactor; // the factor used to adjust the MinIValue; the original
                        // MinIValue = (Max-BKG)*(1-MinISetting/100)+BKG; the
                        // adjusted MinIValue =
                        // (Max-BKG)*(1-(MinISetting+(ContrastFactor/Banlance))/100)+BKG
  float ContrastBalance; // together used with ContrastFactor;
  float ZTolerance; // the distance in Z of a spontaneous focus to the track
                    // plane;
  float FociShapeR;
  // String ToleranceType;
  String MinIType;
  String BackgroundType; // to get the background level using the uniform or
                         // automatic calculation
  String SpontExclusionChoice;
  String FociShapeChoice;
  String[] AbsoRelaOption = { "AbsoluteBrightness", "RelativetoMaximum" };
  String[] BackgroundOption = { "uniform", "automatic", "balanced" };
  String[] YesNoOption = { "Yes", "No" }; // if yes, then a focus will be
                                          // removed from the result table, when
                                          // its delta_Z to the track plane is
                                          // bigger than ZTolerance

  // int DarkestPixelMax;
  // Variables related to the input images (the active image window in ImageJ)
  ImagePlus img; // the input image to be analyzed
  int Width; // width, height, slices of the input image
  int Height;
  int NbSlices;
  String imgTitle;

  int[][][] imgPixel; // 3D array holds pixel info of animage
  short[][][] MaskPixel; // 3D array holds pixels of the recognized objects, the
                         // pixel value of each object is markd as their ID+1.
  // int [][][] MaximumPixel; ////3D array hold pixel info of the local maximum
  // pixels

  // inner class Objects used to analyze the image
  PixelCollection3D CollectionLocalMax; // holds the local maximum found by
                                        // findMaximum()
  PixelCollection3D CollectionMax; // holds the non-duplicated maximum pixels
                                   // from CollectionLocalMax..
  PixelCollection3D CollectionBKG; // holds the surrounding pixels of a maximum
                                   // for calculation of background level
  PixelCollection3D CollectionObjBKG; // holds the background level of the
                                      // objects
  PixelCollection3D[] PixelsInObject; // holds the pixels inside a focus object

  // PixelCollection3D PixelsOfBorder; //holds the inner border pixels of a
  // focus object, pixels belong to the object, marked by pixelValue = objectID
  // +2
  // PixelCollection3D PixelsOfEdge; //holds the outer edge pixels of a focus
  // object, pixels do not belong to the object, just marked by pixel value=1

  // Variables of analysing method//
  int nMax, nLocalMax;
  short nFoci;
  int PixVal;
  int nThreads, nRunningThreads;
  int RTStartCounter, RTEndCounter;
  int nFociExcluded;
  boolean[] SearchObject;
  // int DiffMaxFoci; //holds the difference between the number of recognized
  // foci and the Maxima

  // time that an analysis takes
  long startTime;

  // result table holds the result
  ResultsTable FociResultsTable;

  /*********************** end of claiming variables **********************************/

  public void run(String arg) {
    if (!setupGUI(arg))
      return;
    Width = img.getWidth();
    Height = img.getHeight();
    NbSlices = img.getStackSize();
    imgTitle = img.getTitle();
    initialize();
    analyze();
  }

  public boolean setupGUI(String arg) {
    img = WindowManager.getCurrentImage();
    if (img == null) {
      IJ.noImage();
      return false;
    } else if (img.getStackSize() == 1) {
      // IJ.error("Stack required");
      // return false;
    } else if (img.getType() != ImagePlus.GRAY8
        && img.getType() != ImagePlus.GRAY16) {
      // In order to support 32bit images, pict[] must be changed to float[],
      // and getPixel(x, y); requires a Float.intBitsToFloat() conversion
      IJ.error("8 or 16 bit greyscale image required");
      return false;
    }

    createGUI();

    if (!getGUIInput()) {
      return false;
    }
    if (gd.wasCanceled()) {
      img.updateAndDraw();
      return false;
    }

    IJ.register(Foci_Picker3D.class); // static fields preserved when plugin is
                                      // restarted

    return true;
  }

  void createGUI() { // create the GUI for user input and parameter settings

    gd = new GenericDialog("3D Foci Picker");

    gd.addMessage("------------------------Foci_Picker3D Version 1.0 released by Guanghua Du 15.04.2010---------------------------------------------------------");
    gd.addMessage("Foci_Picker3D Object Counter seaches for local maxima and grows the maxima to object (stops at a local threshold (MinI in ResultTable).");
    gd.addMessage("set uniform background or calculate local background using the radius parameter).");
    gd.addChoice("Background Level", BackgroundOption, "automatic");
    gd.addNumericField("Uniform Background Value", 1500, 0);
    gd.addNumericField("Automatic Background Radius", 6, 0);
    // gd.addMessage("-------------------------------------------------------------------------------------------------------------------------------------");
    gd.addMessage("Defining the local threshold,absolute--> same for all the maxima, =MinISetting+BKG;");
    gd.addMessage("relative--> local threshod =((Maximum-BKG)*MinISetting+BKG).");
    gd.addChoice("MinIType", AbsoRelaOption, "RelativetoMaximum");
    gd.addNumericField("MinISetting", 0.5, 2);
    gd.addMessage("--------------------------------------------------------------------------------------------------------------------------------------");
    gd.addNumericField("ToleranceSetting", 45, 0);
    gd.addNumericField("Minimum pixels number in the focus", 20, 0);
    gd.addMessage("Scale of image: pixels per um");
    gd.addNumericField("VoxelX (pixels): X", 1, 3);
    gd.addNumericField("VoxelY (pixels): Y", 1, 3);
    gd.addNumericField("VoxelZ (pixels): Z", 1, 3);
    gd.addMessage("----------------------------------advanced mode--------------------------------------------------------------------");

    // gd.addMessage("--------------------------------------------------------------------------------------------------------------------------------------");
    // gd.addNumericField("Stop thinking when the difference bt Max and Obj.No. >",20,0);
    // gd.addMessage("--------------------------------------------------------------------------------------------------------------------------------------");
    gd.addNumericField("Contrast Balance (0~1)", 0, 2);
    gd.addChoice("UseZTolerance", YesNoOption, "No");
    gd.addNumericField("ZTolerance", 5, 0);
    gd.addChoice("UseShapeValidation", YesNoOption, "No");
    gd.addNumericField("FociShapeR", 6, 0);
    gd.addNumericField("computingThread", 1, 0);
    gd.showDialog();

    Width = img.getWidth();
    Height = img.getHeight();
    NbSlices = img.getStackSize();

  }

  boolean getGUIInput() {
    BackgroundType = gd.getNextChoice();
    UniformBackground = (int) gd.getNextNumber();
    AutoBKGRadius = (int) gd.getNextNumber();
    MinIType = gd.getNextChoice();
    MinISetting = (float) gd.getNextNumber();

    // ToleranceType= gd. getNextChoice();
    ToleranceSetting = (float) gd.getNextNumber();
    ToleranceValue = ToleranceSetting;
    MinVolume = (int) gd.getNextNumber();
    // the default settings for findMaximum()
    UnitX = 3;
    UnitY = 3;
    UnitZ = 3;

    // VoxelX=(float) 9.717;
    // VoxelY=(float) 9.717;
    // VoxelZ=(float) 4;
    VoxelX = (float) gd.getNextNumber();
    VoxelY = (float) gd.getNextNumber();
    VoxelZ = (float) gd.getNextNumber();

    ContrastBalance = (float) gd.getNextNumber();

    SpontExclusionChoice = gd.getNextChoice();
    ZTolerance = (float) gd.getNextNumber();

    FociShapeChoice = gd.getNextChoice();
    FociShapeR = (float) gd.getNextNumber();
    nThreads = (int) gd.getNextNumber();

    // VoxXsq=VoxelX*VoxelX;
    // VoxYsq=VoxelY*VoxelY;
    // VoxZsq=VoxelZ*VoxelZ;

    // return the index of a border pixel, which has a neighbor pixel with
    // unsigned value in Mask (==0), and its pixel value is smaller than the
    // border pixel.
    IJ.log("Backgroud Type=" + BackgroundType);
    if (BackgroundType.contains("uniform")) {
      IJ.log("UniformBackground = " + UniformBackground);
    } else if (BackgroundType.contains("automatic")) {
      IJ.log("Auto-Background radius= " + AutoBKGRadius);
    } else {
      IJ.log("UniformBackground = " + UniformBackground);
      IJ.log("Auto-Background radius= " + AutoBKGRadius);
    }

    IJ.log("MinISetting = " + MinISetting + "(" + MinIType + ")");
    IJ.log("ToleranceSetting = " + ToleranceSetting);
    IJ.log("Minimum Volume (Pixel number) = " + MinVolume);
    IJ.log("Calibration of image: pixels per um");
    IJ.log(" VoxelX= " + VoxelX + " VoxelY= " + VoxelY + " VoxelZ = " + VoxelZ);
    IJ.log("ContrastBanlance = " + ContrastBalance);
    if (SpontExclusionChoice.contains("Yes")) {
      IJ.log("Exclude spontaneous foci? --Yes, with Z-Difference= "
          + ZTolerance);
    } else {
      IJ.log("Exclude spontaneous foci? --No");
    }
    if (FociShapeChoice.contains("Yes")) {
      IJ.log("Foci Shape Validation: Yes, with FociShapeR=" + FociShapeR);
    } else {
      IJ.log("Foci Shape Validation: No");
    }
    IJ.log("computing threads: n=" + nThreads);
    if (UnitX > Width || UnitY > Height || UnitZ > NbSlices) {
      // IJ.error("Error: Image size smaller than analysis unit!");
      // return false;
    }
    return true;

  }

  void analyze() {

    // get the image info, create arrays
    // load the stack into the imgPixel array
    // Find3D Maximum
    // grow 3D local Maximum into foci
    // count foci volume etc..

    // create arrays
    imgPixel = new int[Width][Height][NbSlices];
    MaskPixel = new short[Width][Height][NbSlices];

    // ImageCal=img.getCalibration();
    // if(ImageCal==null) ImageCal= new Calibration(img);

    // load image
    ImageStack stackA = img.getStack();
    ImageProcessor ip;
    for (int z = 0; z < NbSlices; z++) {
      ip = stackA.getProcessor(z + 1);
      for (int y = 0; y < Height; y++) {
        for (int x = 0; x < Width; x++) {
          imgPixel[x][y][z] = ip.getPixel(x, y);
        }
      }
    }

    find3DMaximum(imgPixel, Width, Height, NbSlices, UnitX, UnitY, UnitZ,
        UniformBackground, ToleranceValue);
    // CreateShow3DStack(MaximumPixel,Width, Height, NbSlices,
    // "Maximum Pixels");
    grow3DMaximum(imgPixel, Width, Height, NbSlices, MinIValue, ToleranceValue);
    CreateShow3DStack(MaskPixel, Width, Height, NbSlices, "FociMask_"
        + ToleranceValue);

    // clearMemory
    imgPixel = null;
    // MaximumPixel=null;

    MaskPixel = null;

  }

  void find3DMaximum(int[][][] PixelArray3D, int pictW, int pictH,
      int pictSlices, int cellX, int cellY, int cellZ, float minThre,
      float peakTol) {
    IJ.showStatus("searching for local maxima");

    int pixelvalue, backgroundlevel;
    boolean isMaximum;
    CollectionMax = new PixelCollection3D();
    CollectionLocalMax = new PixelCollection3D();
    CollectionBKG = new PixelCollection3D();
    nLocalMax = 0;
    nMax = 0;

    // ******************************* //

    // ******************************* //
    // is local Maximum?
    for (int z = 0; z < (int) (pictSlices); z++) {
      for (int y = 0; y < (int) (pictH); y++) {
        for (int x = 0; x < (int) (pictW); x++) {
          pixelvalue = PixelArray3D[x][y][z];
          isMaximum = true;
          if (pixelvalue >= ToleranceValue) {
            ExitCompareLoop: for (int icellx = (int) (-cellX / 2); icellx <= cellX / 2; icellx++) {
              for (int icelly = (int) (-cellY / 2); icelly <= cellY / 2; icelly++) {
                for (int icellz = (int) (-cellZ / 2); icellz <= cellZ / 2; icellz++) {
                  if (pixelvalue < getImagePixelValue((x + icellx),
                      (y + icelly), (z + icellz))) {
                    // MaximumPixel[centerX][centerY][centerZ]= (int)0;
                    isMaximum = false;
                    break ExitCompareLoop;
                  }
                }
              }
            }
          } else {
            isMaximum = false;

          }
          // Find and mark one maximum
          if (isMaximum) {
            backgroundlevel = getBackground(x, y, z, AutoBKGRadius, 0);
            if ((pixelvalue >= ToleranceValue + backgroundlevel)) {
              // MaximumPixel[centerX][centerY][centerZ]= (int) pixelvalue;
              CollectionLocalMax.addPixel(x, y, z, (int) pixelvalue);
              if (!CollectionLocalMax.isANeighborMax(x, y, z, (int) pixelvalue)) {
                CollectionMax.addPixelDescend(x, y, z, (int) pixelvalue,
                    (int) backgroundlevel);
              }
            }
          }
        }
      }
      IJ.showProgress(z + 1, pictSlices);
    }
    nMax = CollectionMax.getPixelNumber();
    nLocalMax = CollectionLocalMax.getPixelNumber();

    IJ.log("*****************    " + imgTitle + "   **************************");
    IJ.log("N. of Local Maximum: " + nLocalMax + ", N. of Seperate Maximum:  "
        + nMax);
    // if (nMax>0)DarkestPixelMax=CollectionMax.getValue(nMax-1);
    // else DarkestPixelMax=0;
    // CollectionMax.print();

    CollectionLocalMax = null;
    // IJ.log("**************************************************************");

  }

  void grow3DMaximum(int[][][] PixelArray3D, int pictW, int pictH,
      int pictSlices, float minThre, float peakTol) {
    IJ.showStatus("growing the maxima to the objects");

    int indexBorder, indexLineBorder;

    for (int i = 0; i < CollectionMax.getPixelNumber();) {
      // if ((i-nFoci)>DiffMaxFoci) break; // stop computing, when the
      // difference between current Maximum No and Foci number analyzed is
      // bigger than 10.
      nRunningThreads = 0;
      for (int j = 0; j < nThreads & (i + j) <= CollectionMax.getPixelNumber(); j++) {
        PixelsInObject[j] = new PixelCollection3D();
        SearchObject[j] = NOSUCCESS;
        PixelsInObject[j].setMaxIndex(i + j, j);
        (PixelsInObject[j]).start(); // start a thread to grow the object from
                                     // this maximum
        nRunningThreads++;

      }

      // wait until all the threads finishes
      for (int j = 0; j < nRunningThreads; j++) {
        try {
          PixelsInObject[j].join();
        } catch (InterruptedException e) {
        }

        IJ.showProgress(i + j, CollectionMax.getPixelNumber());

      }

      for (int j = 0; j < nThreads; j++) {
        saveResults(i + j, j);
        if (SearchObject[j] == SUCCESS) {
          i = i + 1;
        }

      }

    } // end of for{}, finishes the analysis of all maximum points
    if (SpontExclusionChoice.contains("Yes")) {
      nFociExcluded = 0;
      RTEndCounter = FociResultsTable.getCounter();
      int nRemoved = removeSpontFoci(RTStartCounter, RTEndCounter, ZTolerance);
      IJ.log("N. Foci found: " + (nFoci - 2) + "; N. Track foci:"
          + (nFoci - 2 - nRemoved));
    } else {
      IJ.log("N. of Foci found: " + (nFoci - 2));
    }
    FociResultsTable.show("Results");
    IJ.log("********** " + "measurement took "
        + IJ.d2s((System.currentTimeMillis() - startTime) / 1000.0, 2)
        + " seconds " + " ***********");
    CollectionMax = null;
    PixelsInObject = null;
    CollectionBKG = null;

  }

  class PixelCollection3D extends Thread { // inner class, contains information
                                           // of a collection of 3D pixels.
    private int Maximum, Minimum;
    private short nMarker;
    private ArrayList<Integer> pX, pY, pZ, pValue, pBKGLevel;
    private boolean isDescend = false;
    private int MaxIndex, ThreadIndex;

    PixelCollection3D(int indexofthread) {
      pX = new ArrayList<Integer>();
      pY = new ArrayList<Integer>();
      pZ = new ArrayList<Integer>();
      pBKGLevel = new ArrayList<Integer>();
      pValue = new ArrayList<Integer>();
      this.Maximum = Integer.MIN_VALUE;
      this.Minimum = Integer.MAX_VALUE;
      this.ThreadIndex = indexofthread;
    }

    PixelCollection3D() {
      pX = new ArrayList<Integer>();
      pY = new ArrayList<Integer>();
      pZ = new ArrayList<Integer>();
      pValue = new ArrayList<Integer>();
      pBKGLevel = new ArrayList<Integer>();
      this.Maximum = Integer.MIN_VALUE;
      this.Minimum = Integer.MAX_VALUE;

    }

    int addPixel(int x1, int y1, int z1, int pixelvalue) {
      this.pX.add(new Integer(x1));
      this.pY.add(new Integer(y1));
      this.pZ.add(new Integer(z1));
      this.pValue.add(new Integer(pixelvalue));
      return pX.size();
    }

    int addPixel(int index, int x1, int y1, int z1, int pixelvalue) { // add an
                                                                      // element
                                                                      // at
                                                                      // position
                                                                      // (index)
      this.pX.add(index, new Integer(x1));
      this.pY.add(index, new Integer(y1));
      this.pZ.add(index, new Integer(z1));
      this.pValue.add(index, new Integer(pixelvalue));
      return index;
    }

    int addPixelDescend(int x1, int y1, int z1, int pixelvalue) { // add an
                                                                  // element in
                                                                  // the order
                                                                  // of
                                                                  // descending
                                                                  // (index of
                                                                  // the Maximum
                                                                  // =0)
    // IJ.log("pixel( x= "+x1+" y= "+y1+
    // " z= "+z1+" )  has been added in a descending order.");
      if (getPixelNumber() == 0
          || pixelvalue <= this.getValue(getPixelNumber() - 1)) {
        return addPixel(x1, y1, z1, pixelvalue);
      } else {
        for (int i = getPixelNumber() - 1; i >= 0; i--) {
          if (pixelvalue <= this.getValue(i)) {
            return addPixel(i + 1, x1, y1, z1, pixelvalue);

          }
        }
        return addPixel(0, x1, y1, z1, pixelvalue);
      }

    }

    int addPixel(int index, int x1, int y1, int z1, int pixelvalue,
        int backgroundlevel) { // add an element at position (index)
      this.pX.add(index, new Integer(x1));
      this.pY.add(index, new Integer(y1));
      this.pZ.add(index, new Integer(z1));
      this.pValue.add(index, new Integer(pixelvalue));
      this.pBKGLevel.add(index, new Integer(backgroundlevel));
      return index;
    }

    int addPixelDescend(int x1, int y1, int z1, int pixelvalue,
        int backgroundlevel) { // add an element in the order of descending
                               // (index of the Maximum =0)
    // IJ.log("pixel( x= "+x1+" y= "+y1+
    // " z= "+z1+" )  has been added in a descending order.");
      if (getPixelNumber() == 0
          || pixelvalue <= this.getValue(getPixelNumber() - 1)) {
        return addPixel(getPixelNumber(), x1, y1, z1, pixelvalue,
            backgroundlevel);
      } else {
        for (int i = getPixelNumber() - 1; i >= 0; i--) {
          if (pixelvalue <= this.getValue(i)) {
            return addPixel(i + 1, x1, y1, z1, pixelvalue, backgroundlevel);

          }
        }
        return addPixel(0, x1, y1, z1, pixelvalue, backgroundlevel);
      }

    }

    boolean analyze(int i, int j) { // i is the index of a local max, j is the
                                    // index of the thread, i,j strats from 0;
      int x0, y0, z0, pixValue0;
      int n;
      int backgroundlevel;
      // IJ.log("analyze(): index of localMax i= "+i+", j="+j);
      float factor;

      x0 = CollectionMax.getX(i);
      y0 = CollectionMax.getY(i);
      z0 = CollectionMax.getZ(i);
      backgroundlevel = CollectionMax.getBackgroundLevel(i);

      pixValue0 = CollectionMax.getValue(i);
      ContrastFactor = (float) ContrastBalance * (float) 0.5 * (i - nMax / 2)
          / (float) nMax;
      factor = MinISetting * (1 - ContrastFactor);
      if (MinIType.indexOf("RelativetoMaximum") >= 0)
        MinIValue = (pixValue0 - backgroundlevel) * factor + backgroundlevel;
      else {
        MinIValue = MinISetting + backgroundlevel;
      }

      // IJ.log("MaskPixel ["+x0+"]["+y0+"]["+z0+"]="+(MaskPixel
      // [x0][y0][z0])+" Maximum= "+pixValue0);

      if ((MaskPixel[x0][y0][z0] > 1)) {
        // IJ.log("MaskPixel ["+x0+"]["+y0+"]["+z0+"]="+(MaskPixel
        // [x0][y0][z0])+" Maximum= "+pixValue0);
        CollectionMax.removePixel(i); // make the revisit of a new Maximum
                                      // coming up from a back position, which
                                      // is now at the current position,
                                      // possible
        // IJ.log("PixelCollection3D.analyze():Error--> (MaskPixel [x0][y0][z0]="+MaskPixel
        // [x0][y0][z0]+"), i="+i);
        return false;
      } else { // radiate lines from Maximum point to the 6 border plane of the
               // surrounding Cube, find the shortest way, and search for pixels
               // under the maximum peak.
        IJ.showStatus("Analyzing the Maximum NO." + (i + 1));

        this.nMarker = (short) (nFoci + this.ThreadIndex);
        this.clear();
        this.addPixelDescend(x0, y0, z0, pixValue0);
        MaskPixel[x0][y0][z0] = (short) (this.nMarker);
        growFromOpenBorderIndex(0);

        // this.cleanSingePixels();
        return true;
      }

    }

    void cleanSingePixels() {// remove single pixels in the object
      int x, y, z, x1, y1, z1;

      for (int j = 0; j < getPixelNumber(); j++) {
        x = getX(j);
        y = getY(j);
        z = getZ(j);
        // search for seperate single pixels that have no connection to other
        // object pixels(i.e,mask=nFoci)
        nextPixel: for (int i = 0; i < N_SURROUNDING; i++) {// N_SURROUNDING=3x3x3-1=26
          x1 = x + Surrounding[i * 3];
          y1 = y + Surrounding[i * 3 + 1];
          z1 = z + Surrounding[i * 3 + 2];
          if (withInBoundary(x1, y1, z1)) {
            if (MaskPixel[x1][y1][z1] == this.nMarker) {
              break nextPixel;
            } else if (i == N_SURROUNDING - 1) {
              MaskPixel[x][y][z] = RIM_VALUE;
              this.removePixel(x, y, z);
              // IJ.log("nFoci= "+this.nMarker+", remove seperate single pixel:");
            }
          }

        }
      }
      return;

    }

    void clear() {
      this.pX.clear();
      this.pY.clear();
      this.pZ.clear();
      this.pValue.clear();
      this.Maximum = Integer.MIN_VALUE;
    }

    float getAreaXY() {
      float a;
      int n = getPixelNumber();

      if (n > 0)
        a = 0;
      else
        return 0;
      boolean isExisted = false;
      int x1, y1;
      for (int i = 0; i < n; i++) {
        x1 = getX(i);
        y1 = getY(i);
        isExisted = false;
        for (int j = 0; j < i; j++) {
          if (x1 == getX(j) & y1 == getY(j))
            isExisted = true;
        }
        if (!isExisted)
          a++;
      }
      return a / (VoxelX * VoxelY);
    }

    float getAreaYZ() {
      float a;
      int n = getPixelNumber();
      if (n > 0)
        a = 0;
      else
        return 0;
      boolean isExisted = false;
      int y1, z1;
      for (int i = 0; i < n; i++) {
        z1 = getZ(i);
        y1 = getY(i);
        isExisted = false;
        for (int j = 0; j < i; j++) {
          if (z1 == getZ(j) & y1 == getY(j))
            isExisted = true;
        }
        if (!isExisted)
          a++;
      }
      return a / (VoxelY * VoxelZ);
    }

    float getAreaXZ() {
      float a;
      int n = getPixelNumber();
      if (n > 0)
        a = 0;
      else
        return 0;
      boolean isExisted = false;
      int x1, z1;
      for (int i = 0; i < n; i++) {
        z1 = getZ(i);
        x1 = getX(i);
        isExisted = false;
        for (int j = 0; j < i; j++) {
          if (z1 == getZ(j) & x1 == getX(j))
            isExisted = true;
        }
        if (!isExisted)
          a++;
      }
      return a / (VoxelX * VoxelZ);
    }

    int getBackgroundLevel(int index) {

      return (pBKGLevel.get(index)).intValue();

    }

    float getCenterX() {
      float a = 0;
      for (int i = 0; i < getPixelNumber(); i++) {
        a = a + (pX.get(i).intValue() - a) / (i + 1);
      }
      return a / VoxelX;
    }

    float getCenterY() {
      long a = 0;
      for (int i = 0; i < getPixelNumber(); i++) {
        a = a + (pY.get(i).intValue() - a) / (i + 1);
      }
      return a / VoxelY;
    }

    float getCenterZ() {
      long a = 0;
      for (int i = 0; i < getPixelNumber(); i++) {
        a = a + (pZ.get(i).intValue() - a) / (i + 1);
      }
      return (a / VoxelZ);
    }

    float getCoreX() { // center of mass, weighed by the pixel value.
      float a = 0;
      float b = getMeanValue();
      for (int i = 0; i < getPixelNumber(); i++) {
        a = a + (pX.get(i).intValue() * pValue.get(i).intValue() / b - a)
            / (i + 1);
      }
      return a / VoxelX;
    }

    float getCoreY() { // center of mass, weighed by the pixel value.
      float a = 0;
      float b = getMeanValue();
      for (int i = 0; i < getPixelNumber(); i++) {
        a = a + (pY.get(i).intValue() * pValue.get(i).intValue() / b - a)
            / (i + 1);
      }
      return a / VoxelY;
    }

    float getCoreZ() { // center of mass, weighed by the pixel value.
      float a = 0;
      float b = getMeanValue();
      for (int i = 0; i < getPixelNumber(); i++) {
        a = a + (pZ.get(i).intValue() * pValue.get(i).intValue() / b - a)
            / (i + 1);
      }
      return a / VoxelZ;
    }

    int getLowerOneThirdAverage() {
      float a;
      a = 0;
      int n = getPixelNumber();
      int m = n - (int) (n * 2 / 3);

      for (int i = (int) (n * 2 / 3); i < n; i++) {
        a = a + getValue(i) / m;
      }
      return (int) a;

    }

    int getMiddleOneThirdAverage() {
      float a;
      a = 0;
      int n = getPixelNumber();
      int m = (int) (n / 3);

      for (int i = m; i < 2 * m; i++) {
        a = a + getValue(i) / m;
      }
      return (int) a;

    }

    float getMeanValue() {
      float a = 0;
      for (int i = 0; i < getPixelNumber(); i++) {
        a = a + (pValue.get(i).intValue() - a) / (i + 1);
      }
      return a;
    }

    int getPixelIndex(int x1, int y1, int z1) {
      for (int i = 0; i < getPixelNumber(); i++) {
        if (getX(i) == x1 & getY(i) == y1 & getZ(i) == z1) {
          return i;
        }
      }

      return -1;

    }

    int getPixMaximum() {
      for (int i = 0; i < getPixelNumber(); i++) {
        Maximum = Maximum > getValue(i) ? Maximum : getValue(i);
      }
      return Maximum;
    }

    int getPixMinimum() {
      for (int i = 0; i < getPixelNumber(); i++) {
        Minimum = Minimum < getValue(i) ? Minimum : getValue(i);
      }
      return Minimum;
    }

    int getPixelNumber() {
      return pX.size();
    }

    int getPixRange() { // range = Maximum-Minimum

      return getPixMaximum() - getPixMinimum();
    }

    int[] getValueList() {
      Integer a;
      int[] b = new int[getPixelNumber()];
      for (int i = 0; i < getPixelNumber(); i++) {
        a = pValue.get(i);
        b[i] = a.intValue();
      }
      return b;
    }

    int getX(int i) {
      return (pX.get(i)).intValue();
    }

    int getY(int i) {
      return (pY.get(i)).intValue();
    }

    int getZ(int i) {
      return (pZ.get(i)).intValue();
    }

    int getValue(int i) {
      return (pValue.get(i)).intValue();
    }

    float getVolume() {
      return getPixelNumber() / (VoxelX * VoxelY * VoxelZ);
    }

    boolean isANeighborMax(int x1, int y1, int z1, int pixelvalue) { // check
                                                                     // whether
                                                                     // an input
                                                                     // pixel is
                                                                     // a
                                                                     // neighbor
                                                                     // of the
                                                                     // pixels
                                                                     // in the
                                                                     // collection
                                                                     // or not
      int x2, y2, z2;
      float sqaureDistance;

      for (int i = 0; i < getPixelNumber() - 1; i++) {
        x2 = getX(i);
        y2 = getY(i);
        z2 = getZ(i);
        if (squareD(x1, y1, z1, x2, y2, z2) <= 3)
          return true;

      }
      return false;

    }

    /*
     * grwoFromOpenBorderIndex() the most important function used to analyze the
     * object, grow a local maximum to an object.
     */

    boolean growFromOpenBorderIndex(int indexstartfrom) {

      int x = 0;
      int y = 0;
      int z = 0;
      int x1 = 0;
      int y1 = 0;
      int z1 = 0;
      int openneighbourindex = 0;
      int openborderindex = 0;
      int neighbourpixelvalue = 0;
      int borderpixelvalue = 0;
      boolean isOpen = false;

      // IJ.log("nFoci= "+nFoci+", index start from  "+indexstartfrom);
      // IJ.log("number of pixel in the object:    "+getPixelNumber());

      openneighbourindex = 0;
      // First, search for a pixel on the open border
      // if yes, then mark the (x,y,z) as a rim pixel(RIM)VALUE,and remove it
      // from the pixel collection
      // if no, continue with step2.
      findOpenBorderIndex: for (int j = indexstartfrom; j < getPixelNumber(); j++) {
        x = getX(j);
        y = getY(j);
        z = getZ(j);
        for (int i = 0; i < N_SURROUNDING; i++) { // calculate the sum distance
                                                  // from the neighbor pixels to
                                                  // point1 and point2
          x1 = x + Surrounding[i * 3];
          y1 = y + Surrounding[i * 3 + 1];
          z1 = z + Surrounding[i * 3 + 2];
          if (withInBoundary(x1, y1, z1)) {
            if (MaskPixel[x1][y1][z1] == 0) {
              // find a pixel on the open border
              isOpen = true;
              openneighbourindex = i;
              openborderindex = j;
              borderpixelvalue = getValue(j);
              break findOpenBorderIndex;
            }
          }

        }
        //
      }
      if (!isOpen)
        return SUCCESS; // if the subrutine finishes correctly, it should return
                        // here.

      // isOpen==true, continue Step2;
      // step2:
      // continue with checking the current pixel whether there is a unmarked
      // neighbour pixel
      // with pixel value > the current pixel(x,y,z),which is brightest pixel on
      // the open border
      // if yes, then mark the (x,y,z) as a rim pixel(RIM)VALUE,and remove it
      // from the pixel collection
      // if no, continue with step2.

      checkOpenBorderIndex: for (int i = openneighbourindex; i < N_SURROUNDING; i++) { // calculate
                                                                                       // the
                                                                                       // sum
                                                                                       // distance
                                                                                       // from
                                                                                       // the
                                                                                       // neighbor
                                                                                       // pixels
                                                                                       // to
                                                                                       // point1
                                                                                       // and
                                                                                       // point2
        x1 = x + Surrounding[i * 3];
        y1 = y + Surrounding[i * 3 + 1];
        z1 = z + Surrounding[i * 3 + 2];
        if (withInBoundary(x1, y1, z1)) {
          neighbourpixelvalue = imgPixel[x1][y1][z1];
          if (MaskPixel[x1][y1][z1] == 0
              & neighbourpixelvalue > borderpixelvalue) {
            MaskPixel[x][y][z] = RIM_VALUE;
            // IJ.log("trying to remove pixel( x= "+x+" y= "+y+ " z= "+z+" )");
            this.removePixel(x, y, z);
            // IJ.log("after remove a pixel, indexstart from ="+indexstartfrom+" Max index in collection="+(getPixelNumber()-1));
            // IJ.log("growFromOpenBorderIndex("+indexstartfrom+")");
            if (growFromOpenBorderIndex(indexstartfrom) == SUCCESS) {
              return SUCCESS;
            } else {
              return NOSUCCESS;
            }
          }
        }
      }

      // step3: all the unmarked neighbour pixel has a pixel value < current
      // pixel (x,y,z)
      // grow from this (x,y,z), mark the unmarked neighbour pixels to nFoci or
      // RIM_VALUE
      Grow: for (int i = openneighbourindex; i < N_SURROUNDING; i++) {
        x1 = x + Surrounding[i * 3];
        y1 = y + Surrounding[i * 3 + 1];
        z1 = z + Surrounding[i * 3 + 2];

        if (withInBoundary(x1, y1, z1)) {
          neighbourpixelvalue = imgPixel[x1][y1][z1];
          if (MaskPixel[x1][y1][z1] == 0) {
            if (neighbourpixelvalue <= borderpixelvalue) {
              if (neighbourpixelvalue > MinIValue) {
                MaskPixel[x1][y1][z1] = this.nMarker;
                addPixelDescend(x1, y1, z1, neighbourpixelvalue);
              } else {
                MaskPixel[x1][y1][z1] = RIM_VALUE;
              }

            } else {
              IJ.log("growFromOpenBorderIndex(): Error-->surrounding unmarked pixel value > open border pixel value");

            }

          }
        }

      }
      if (growFromOpenBorderIndex(indexstartfrom++) == SUCCESS) {
        return SUCCESS;
      } else {
        return NOSUCCESS;
      }// the code fails to finish correctly

    }

    void print() {
      for (int i = 0; i < getPixelNumber(); i++) {
        IJ.log(i + "  x=  " + getX(i) + "   y= " + getY(i) + "   z= " + getZ(i)
            + " pixValue =" + getValue(i));
      }

    }

    void removePixel(int i) { // remove pixel with index i in the MaxCollection.
      if (i < getPixelNumber() & i >= 0) {
        this.pX.remove(i);
        this.pY.remove(i);
        this.pZ.remove(i);
        this.pValue.remove(i);
        this.pBKGLevel.remove(i);
        // IJ.log("NO."+(i+1)+" Pixel has been removed: ");
        // IJ.log("Number of total pixels left in the collection now : "+getPixelNumber());
      } else {
        IJ.log("pixel index_" + i + "_ is out of range of the collection ");
      }

    }

    void removePixel(short x1, short y1, short z1) {
      for (int i = 0; i < getPixelNumber(); i++) {
        if (getX(i) == x1 & getY(i) == y1 & getZ(i) == z1) {
          this.pX.remove(i);
          this.pY.remove(i);
          this.pZ.remove(i);
          this.pValue.remove(i);

          // IJ.log("pixel( x= "+x1+" y= "+y1+
          // " z= "+z1+" )  has been removed: ");
          // IJ.log("Number of total pixels left in the collection now : "+getPixelNumber());
          return;
        }
      }
      IJ.log("removePixel(short x,y,z): Error-->can not remove pixel(" + x1
          + ", " + y1 + ", " + z1 + "), it does not exist!!");

    }

    void removePixel(int x1, int y1, int z1) {
      for (int i = 0; i < getPixelNumber(); i++) {
        if (getX(i) == x1 & getY(i) == y1 & getZ(i) == z1) {
          this.pX.remove(i);
          this.pY.remove(i);
          this.pZ.remove(i);
          this.pValue.remove(i);

          // IJ.log("pixel( x= "+x1+" y= "+y1+
          // " z= "+z1+" )  has been removed: ");
          // IJ.log("Number of total pixels left in the collection now : "+getPixelNumber());
          return;
        }
      }
      IJ.log("removePixel(int x,y,z): Error-->can not remove pixel(" + x1
          + ", " + y1 + ", " + z1 + "), it does not exist!!");

    }

    void removeFromMask() {
      for (int i = 0; i < getPixelNumber(); i++) {
        MaskPixel[getX(i)][getY(i)][getZ(i)] = 0;
      }
      // IJ.log(getPixelNumber()+"  Pixels in the collection has been reomoved from the MaskPixel");

    }

    public void run() {
      SearchObject[ThreadIndex] = this.analyze(MaxIndex, ThreadIndex);
    }

    int setPixel(int index, int x1, int y1, int z1, int pixelvalue) { // replace
                                                                      // the
                                                                      // element
                                                                      // at
                                                                      // position
                                                                      // (index)
                                                                      // with a
                                                                      // new
      this.pX.set(index, new Integer(x1));
      this.pY.set(index, new Integer(y1));
      this.pZ.set(index, new Integer(z1));
      this.pValue.set(index, new Integer(pixelvalue));
      return index;
    }

    void setMaxIndex(int maxindex, int threadindex) {
      this.MaxIndex = maxindex;
      this.ThreadIndex = threadindex;
    }

  } // end of inner class PixelCollection3D

  // ///////////////////////////////////////////////////////////////////
  // method and functions ///
  // ///////////////////////////////////////////////////////////////////

  int getBackground(int x, int y, int z, int radius, int depth) {
    if (BackgroundType.contains("uniform")) {
      return (int) UniformBackground;
    } else {
      int x1, y1, z1, backgroundvalue;

      y1 = y - radius;
      z1 = z;
      for (int i = 0; i <= radius; i++) {
        x1 = x + i;
        if (withInBoundary(x1, y1, z1)) {
          CollectionBKG.addPixelDescend(x1, y1, z1, imgPixel[x1][y1][z1]);
        }
      }

      y1 = y - radius;
      z1 = z;
      for (int i = 0; i <= radius; i++) {
        x1 = x + i;
        if (withInBoundary(x1, y1, z1)) {
          CollectionBKG.addPixelDescend(x1, y1, z1, imgPixel[x1][y1][z1]);
        }

      }
      x1 = x + radius;
      z1 = z;
      for (int i = 0; i <= radius; i++) {
        y1 = y + i;
        if (withInBoundary(x1, y1, z1)) {
          CollectionBKG.addPixelDescend(x1, y1, z1, imgPixel[x1][y1][z1]);
        }
      }

      x1 = x - radius;
      z1 = z;
      for (int i = 0; i <= radius; i++) {
        y1 = y + i;
        if (withInBoundary(x1, y1, z1)) {
          CollectionBKG.addPixelDescend(x1, y1, z1, imgPixel[x1][y1][z1]);
        }
      }
      backgroundvalue = (CollectionBKG.getMiddleOneThirdAverage()) * 3 / 5; // getLowerOneThirdAverage();
      CollectionBKG.clear();
      if (BackgroundType.contains("automatic")) {
        return backgroundvalue;
      } else if (BackgroundType.contains("balanced")) {
        return (int) ((backgroundvalue + UniformBackground) / 2);

      } else {
        IJ.log("getBackground():Error-->unknown BackgroundType ="
            + BackgroundType);
        return Integer.MAX_VALUE;
      }

    }
  }

  public int getImagePixelValue(int x, int y, int z) {
    if (withInBoundary(x, y, z)) {
      return (int) imgPixel[x][y][z];
    } else {
      return 0;
    }
  }

  void initialize() {
    startTime = System.currentTimeMillis();
    // nThreads=Runtime.getRuntime().availableProcessors();
    // nThreads=1;
    // IJ.log("available processors: "+nThreads);
    PixelsInObject = new PixelCollection3D[nThreads];
    SearchObject = new boolean[nThreads];
    nFoci = 2;

    // initialize results table
    FociResultsTable = ResultsTable.getResultsTable();
    // String [] ResultsLabel={"FocusNO", "CoreX", "CoreY", "CoreZ","Volume",
    // "AreaXY","AreaXZ","AreaYZ","Intensity","Background","MinI","MaxI","Range"};
    // //"CenterX","CenterY", "CenterZ",
    // for (int i=0;i<ResultsLabel.length;i++)
    // FociResultsTable.setHeading(i,ResultsLabel[i]);
    // RTStartCounter=FociResultsTable.getCounter();

  }

  public boolean withInBoundary(int m, int n, int o) {
    return (m >= 0 && m < Width && n >= 0 && n < Height && o >= 0 && o < NbSlices);
  }

  float squareD(int x1, int y1, int z1, int x2, int y2, int z2) {
    float square = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2)
        * (z1 - z2);
    return square;
  }

  void saveResults(int i, int j) { // i is the index of the max, j is the index
                                   // of the thread
    if (!approveResult(i, j)) {
      PixelsInObject[j].removeFromMask();
    } else {
      savetoResultsTable(i, j);
      nFoci++;
    }

    PixelsInObject[j].clear();
  }

  void savetoResultsTable(int i, int j) { // i is the index of the max, j is the
                                          // index of the thread
    FociResultsTable.incrementCounter();
    FociResultsTable.addLabel("ImageTitle", imgTitle); // imgTitle

    FociResultsTable.addValue("FocusNO", nFoci - 1);
    FociResultsTable.addValue("CoreX", PixelsInObject[j].getCoreX());
    FociResultsTable.addValue("CoreY", PixelsInObject[j].getCoreY());
    FociResultsTable.addValue("CoreZ", PixelsInObject[j].getCoreZ());
    FociResultsTable.addValue("Volume", PixelsInObject[j].getVolume());
    FociResultsTable.addValue("AreaXY", PixelsInObject[j].getAreaXY());
    FociResultsTable.addValue("AreaXZ", PixelsInObject[j].getAreaXZ());
    FociResultsTable.addValue("AreaYZ", PixelsInObject[j].getAreaYZ());
    FociResultsTable.addValue("Intensity", PixelsInObject[j].getMeanValue());
    // FociResultsTable.addValue(4,PixelsInObject[j].getCenterX());
    // FociResultsTable.addValue(5,PixelsInObject[j].getCenterY());
    // FociResultsTable.addValue(6,PixelsInObject[j].getCenterZ());
    FociResultsTable
        .addValue("Background", CollectionMax.getBackgroundLevel(i));
    FociResultsTable.addValue("MinI", MinIValue);
    FociResultsTable.addValue("MaxI", PixelsInObject[j].getValue(0));
    FociResultsTable.addValue("Range", PixelsInObject[j].getPixRange());

    // IJ.log("NO. of Maximum: "+(i+1));
    // IJ.log("number of pixels in the maximum:  "+PixelsInObject[j].getPixelNumber());
    // IJ.log("  centerX:=     "+PixelsInObject[j].getCenterX()+"  centerX:=     "+PixelsInObject[j].getCenterY()+"  centerX:=     "+PixelsInObject[j].getCenterZ());
    // // PixelsInObject[j].print();
  }

  boolean approveResult(int i, int j) {
    float volume;
    if ((SearchObject[j]) != SUCCESS)
      return false;
    if (PixelsInObject[j].getPixelNumber() < MinVolume)
      return false;
    if (PixelsInObject[j].getPixRange() < ToleranceValue)
      return false;
    if (FociShapeChoice.contains("Yes")) {
      volume = (float) PixelsInObject[j].getVolume();
      if (volume / (PixelsInObject[j].getAreaXY()) > FociShapeR / VoxelZ)
        return false;
      if (volume / (PixelsInObject[j].getAreaXZ()) > FociShapeR / VoxelY)
        return false;
      if (volume / (PixelsInObject[j].getAreaYZ()) > FociShapeR / VoxelX)
        return false;

    }

    return true;

  }

  int removeSpontFoci(int starti, int endi, float deltaz) {
    float maxDeltaZ = deltaz / VoxelZ;
    FociResultsTable.updateResults();

    float mediancorez;
    float corez;
    int i, j;
    mediancorez = medianCoreZ(starti, endi);
    // IJ.log("removeSpontFoci(): starti= "+starti+", endi= "+endi+", median CoreZ="+mediancorez);

    for (i = starti; i < endi; i++) {
      corez = (float) FociResultsTable.getValue("CoreZ", i);
      if (Math.abs(corez - mediancorez) > maxDeltaZ) {
        // IJ.log("CoreZ= "+corez+", FociResultsTable.deleteRow("+(i+1)+")");
        // j=i;
        // IJ.log("row = "+(j+1)+"; "+FociResultsTable.getRowAsString(j));
        FociResultsTable.deleteRow(i);
        nFociExcluded++;
        // IJ.log("j="+j+", nFociExcluded= "+nFociExcluded);
        FociResultsTable.updateResults();
        // IJ.log("rest N. of Foci= "+FociResultsTable.getCounter());
        endi = FociResultsTable.getCounter();
        removeSpontFoci(starti, endi, deltaz);
        break;
      }
    }

    return nFociExcluded;

  }

  float medianCoreZ(int starti, int endi) {
    FociResultsTable.updateResults();
    float[] mZ = new float[endi - starti + 1];
    int i;
    int j = 0;
    // IJ.log("medianCoreZ(): starti= "+(starti+1)+", endi= "+endi);
    for (i = starti; i < endi; i++) {
      mZ[j] = (float) FociResultsTable.getValue("CoreZ", i);// getValueAsDouble(3,i);
                                                            // //getValue("CoreZ",i);
      // IJ.log("mZ["+(1+j)+"]= "+mZ[j]);
      j++;
    }
    java.util.Arrays.sort(mZ);
    return (float) (mZ[(int) (j / 2)]);
  }

  void CreateShow3DStack(int[][][] PixelArray3D, int pictW, int pictH,
      int pictSlices, String imgtitle) {
    IJ.newImage(imgtitle, "16-bit black", pictW, pictH, pictSlices);
    ImagePlus new3Dstack = WindowManager.getCurrentImage();
    ImageStack stack = new3Dstack.getStack();
    ImageProcessor ip;
    for (int z = 0; z < NbSlices; z++) {
      ip = stack.getProcessor(z + 1);
      for (int y = 0; y < pictH; y++) {
        for (int x = 0; x < pictW; x++) {
          ip.setValue(PixelArray3D[x][y][z]);
          ip.drawPixel(x, y);
        }
      }

    }
    new3Dstack.show();
    IJ.run("FociPicker_256Colors");
    new3Dstack.updateAndDraw();
  }

  void CreateShow3DStack(short[][][] PixelArray3D, int pictW, int pictH,
      int pictSlices, String imgtitle) {
    IJ.newImage(imgtitle, "8-bit black", pictW, pictH, pictSlices);
    ImagePlus new3Dstack = WindowManager.getCurrentImage();
    ImageStack stack = new3Dstack.getStack();
    ImageProcessor ip;
    for (int z = 0; z < NbSlices; z++) {
      ip = stack.getProcessor((int) (z + 1));
      for (int y = 0; y < pictH; y++) {
        for (int x = 0; x < pictW; x++) {
          ip.setValue(PixelArray3D[x][y][z]);
          ip.drawPixel(x, y);
        }

      }

    }

    new3Dstack.show();
    IJ.run("FociPicker_256Colors");
    new3Dstack.updateAndDraw();

    // IJ.log("256_Colors lookup table");
  }

  public void run() {
    Width = img.getWidth();
    Height = img.getHeight();
    NbSlices = img.getStackSize();
    imgTitle = img.getTitle();
    analyze();

  }
}
