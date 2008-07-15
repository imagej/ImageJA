import java.awt.*;
import java.text.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.text.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.Properties;
import java.io.*;
import ij.Prefs;
import javax.swing.border.*;

/* Color comparison of two 8-bit identically dimensioned gray scale images.
    Each pixel has an intensity in image A and in image B. Plot each point
    on a plain with image A intensity as abcissa and B intensity as ordinate.
    ColorComparison allows the user to compare Image1 with Image2 intensities
    using one of the methods below -
   1) Polar Comparison: In polar coordinates, a pixel's location is given by magnitude and
    angle. The color image shows pixel magnitude (or image A or image B intensity) as
    intensity. Polar angle is shown as hue. With positive valued images, pixel angle is
    between 0 and 90 degrees.
    Pixel angles are mapped to any continuous swatch of a color wheel where
           R=0, G=120, B=240 degrees.
   2) Percentage: p = 100 * (B intensity) / (A intensity + B intensity)
    may be used to specify hue. With methods 1) and 2) the mapping is isomorphic -
    all information in the two gray scale images is retained.
    in the color image. ColorComparison also generates a histogram of pixel pair angles(0 to
    90 degrees)
    or percentages(0 to 100%) sorted into a user specified number of bins.
   3) Regression: A linear regression and standard error of regression are calculated
    predicting Image2 from Image1 pixel intensities. The hue of each pixel in the color
    image expresses the residual, the difference between the predicted and actual
    Image2 intensity divided by the standard error of regression.
    The range of differences mapped by the color image is set by the number of histogram
    bins specified. For instance, sepecifying 10 bins sets a range of -5 to +5 standard
    errors.
    The hue of pixels outside this range are clamped to the max or min standard error.
    After the color image is generated, the user is allowed to draw ROIs on Image1.
    The mean residual within the ROI and its probability are displayed in a table.
    If the histogram uses weighted intensities the mean is calculated using weighted
    intensities.
    if HighlightPixels is checked, the program highlights all color image pixels with
    values outside regression confidence limits at the specified probability level alpha.
    Percentages of pixels above and below the confidences limits are displayed in the
    Results window. Pixels below the specified Theshold Intensity are excluded, and
    percentages are weighted by Image1 intensities if the weight histogram box is checked.
*/

public class ColorComparison_ implements PlugInFilter {
  private static int
    POLAR = 0,
    PERCENTAGE = 1,
    REGRESSION = 2;
  private static String PREFS  = "ColorComparisonPrefs.txt";
  private static String DESCRIPT  = "ColorComparisonDescription.txt";
  private static double EPSILON = 0.00001;
  private static String[] types = {"Image 1", "Image 2", "Root mean square"};
  private static String[] modes = {"Polar Angle", "Percentage", "Regression"};
  private int mode = REGRESSION,
              nbins = 15,
              iswitch = 0;
  private static int index1 = 0;
  private static int index2 = 1;
  protected String title = "Polar Map";
  private double loDegree = 120;
  private double hiDegree = -120;
  private double lo, hi; // hue limits in radians
  private double off120, off240;
  protected ImageStack stack = null;
  protected double[] hist = null;
  protected ImagePlus imp1, imp2, colImage;
  protected boolean weightHistogram;
  protected boolean showPalette;
  protected boolean titleInImage;
  protected boolean regressionGraph;
  protected ImageProcessor ip1, ip2, ipPolar;
  protected double thresholdPercent = 10;
  protected int threshold;

  double scaleIntensity = 1.0/ Math.sqrt(2.0);
  double PI2 = Math.PI / 2.0;
  double scaleHue;
  double sumIntent = 0;
  double binsize;
  int nimage1 = 0,
      nimage2 = 0;
  byte[] pixels1, pixels2;
  int runCount = 0;
  seSums s;
  JPanel jPanel1 = new JPanel();

  public int setup(String args, ImagePlus imp)
  {
     if(args.equals("about")) {
        showAbout();
        return DONE;
      }
    return DOES_8G;
  }

  public void run(ImageProcessor ip) {
// Set up parameters
  if(getParameters())
    {
// Initialize
    hist = new double[nbins];
    lo = 2.0*Math.PI/360.0 * loDegree;
    hi = 2.0*Math.PI/360.0 * hiDegree;
    off120 = 2.0 / 3.0 * Math.PI;
    off240 = 2.0 * off120;
    if(iswitch < 2) scaleIntensity = 1.0;
    scaleHue = (hi - lo) / PI2;
    binsize = PI2 / nbins;
// Do polar map
    if ((imp1.getType()!=imp1.GRAY8) || (imp2.getType()!=imp1.GRAY8)) {
        IJ.showMessage("Color Comparison", "Both images must be 8-bit grayscale.");
        return;
        }

    int w = imp1.getWidth();
    int h = imp1.getHeight();
    if((imp2.getWidth() != w) || (imp2.getHeight() != h)) {
        IJ.showMessage("Color Comparison", "Images must have same dimensions.");
        return;
        }
    ip1 = imp1.getProcessor();
    ip2 = imp2.getProcessor();
    nimage1 = imp1.getStackSize();
    nimage2 = imp2.getStackSize();
    if(nimage1 != nimage2)  {
        IJ.showMessage("Color Comparison", "There must be the same number of images in each stack.");
        return;
        };

    threshold = (int)(thresholdPercent/100 * ip1.getMax());
    if((mode != REGRESSION) && (nimage1 > 1)) doStackComparison(w, h, nimage1);
    else  doSingleImageComparison(w, h);
// Color Bar
    if(showPalette) makeColorBar();
// Show histogram
    for(int i=0; i<nbins; i++) hist[i] /= sumIntent;
    showHistogram(hist);
//    IJ.register(ColorComparison_.class);
// get SE for user defined regions
    if(mode == REGRESSION)
      {
      SeRegion jfRegion = new SeRegion(imp1, imp2, colImage, s,
      loDegree, hiDegree, nbins,
      weightHistogram, thresholdPercent);
      jfRegion.show();
      if(regressionGraph) doGraph(w, h, jfRegion);
      }
   }
  }

  void doGraph(int w, int h, SeRegion se)
  {
     double t = se.tInverse(se.alpha, s.n-2.0);
     IJ.write("t=" + t);
     int width = 256; int height = 256;
     pixels1 = (byte[]) imp1.getProcessor().getPixels();
     pixels2 = (byte[]) imp2.getProcessor().getPixels();
     int[] colorPixels = (int[]) colImage.getProcessor().getPixels();
     int[] graphPixels = new int[width*height];
     ColorProcessor ipGraph = new ColorProcessor(width, height);
     for(int i=0; i<graphPixels.length; i++) graphPixels[i] = 0;
     for(int i=0; i<pixels1.length; i++)
      {
      int v1 = (int)pixels1[i] & 255;
      int v2 = (int)pixels2[i] & 255;
      if(v1 >= threshold)
        {
        int j = width*(height-1-v2) + v1;
        graphPixels[j] = colorPixels[i];
        }
      }
     ipGraph.setPixels(graphPixels);
     ImagePlus graph = new ImagePlus("Regression", ipGraph);
     graph.setColor(new Color(127, 127, 127));
     ipGraph.drawLine(0, (int)(255-s.regress(0)), 255, (int)(255 - s.regress(255)));
// plot confidence bands
     graph.setColor(Color.white);
     for(int x=0; x<width; x += 4)
      {
      double bw = t * s.se * se.xc[x];
      ipGraph.drawPixel(x, (int)(255 - s.regress(x) + bw));
      ipGraph.drawPixel(x, (int)(255 - s.regress(x) - bw));
      }
     graph.updateAndDraw();
     graph.show();
  }


  void doSingleImageComparison(int w, int h)
  {
     pixels1 = (byte[]) imp1.getProcessor().getPixels();
     pixels2 = (byte[]) imp2.getProcessor().getPixels();
     int[] colorPixels = doColorComparison(pixels1, pixels2);

    ipPolar = new ColorProcessor(w, h);
    ipPolar.setPixels(colorPixels);
    ipPolar.setColor(Color.white);
    ipPolar.moveTo(10, 20);
    Font font = new Font("SansSerif", Font.BOLD, 16);
    ipPolar.setFont(font);
    if(titleInImage &&(title != null) && (title.length() > 0))
      {
      ipPolar.drawString(title);
      title = "Comparison";
      }
    if((title == null) && (title.length()== 0)) title = "";
    colImage = new ImagePlus(title, ipPolar);
    colImage.show();
    colImage.updateAndDraw();

 }

  void doStackComparison(int w, int h, int nimage)
  {
    ImageStack stackRGB = new ImageStack(w, h);
    ImageProcessor ipPolar = new ColorProcessor(w, h);
//    ImagePlus imp =  new ImagePlus(title, ipPolar);
    ImagePlus imp =  new ImagePlus(title,ipPolar);
    imp.createEmptyStack();
    for(int i=1; i<=nimage; i++)
      {
      imp1.setSlice(i);
      imp2.setSlice(i);
      pixels1 = (byte[]) imp1.getProcessor().getPixels();
      pixels2 = (byte[]) imp2.getProcessor().getPixels();
      int[] colorPixels = doColorComparison(pixels1, pixels2);
//      log("image " + i + " npix=" + colorPixels.length);
      stackRGB.addSlice("", (Object)colorPixels);
//      IJ.showProgress((double)i / (double)nimage);
      }
    imp.setStack(null, stackRGB);
    imp.show();
    imp.updateAndDraw();
  }

  boolean getParameters()
  {
  int[] wList = WindowManager.getIDList();
  if (wList==null) {
    IJ.error("No windows are open.");
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

    // Read preferences file
    Preferences prefs = new Preferences(PREFS);
    getPreferences(prefs);

    GenericDialog param = new GenericDialog("ColorComparison Parameters", IJ.getInstance());
    param.addChoice("Image #1: ", titles, titles[index1]);
    param.addChoice("Image #2: ", titles, titles[index2]);
    param.addStringField("Title", title, 40);
    param.addChoice("Analysis mode", modes, modes[mode]);
    param.addNumericField("Low angle(degrees)", loDegree, 0);
    param.addNumericField("High angle(degrees)", hiDegree, 0);
    param.addChoice("Set Intensity:", types, types[iswitch]);
    param.addNumericField("Histogram bins", nbins, 0);
    param.addNumericField("Lower theshold %", thresholdPercent, 0);
    param.addCheckbox("Intensity weight histogram?", weightHistogram);
    param.addCheckbox("Show Palette?", showPalette);
    param.addCheckbox("Show title in image?", titleInImage);
    param.addCheckbox("Regression graph?", regressionGraph);
    param.showDialog();
    if(!param.wasCanceled())
      {
      index1 = param.getNextChoiceIndex();
      index2 = param.getNextChoiceIndex();
      imp1 = WindowManager.getImage(wList[index1]);
      imp2 = WindowManager.getImage(wList[index2]);
      title = param.getNextString();
      mode = param.getNextChoiceIndex();
      loDegree = param.getNextNumber();
      hiDegree = param.getNextNumber();
      iswitch = param.getNextChoiceIndex();
      nbins = (int)param.getNextNumber();
      thresholdPercent = param.getNextNumber();
      if(nbins < 1) nbins = 1;
      weightHistogram = param.getNextBoolean();
      showPalette = param.getNextBoolean();
      titleInImage = param.getNextBoolean();
      regressionGraph = param.getNextBoolean();

  // save preferences to file
      savePreferences(prefs);
      return true;
      }
    return false;
  }

  void getPreferences(Preferences prefs)
  {
    title = prefs.getString("TITLE", title);
    mode = prefs.getInt("MODE", mode);
    loDegree = prefs.getInt("LODEGREE", (int)loDegree);
    hiDegree = prefs.getInt("HIDEGREE", (int)hiDegree);
    iswitch = prefs.getInt("INTENSITY", iswitch);
    nbins = prefs.getInt("BINS", nbins);
    thresholdPercent = prefs.getDouble("THRESHOLD", thresholdPercent);
    weightHistogram = prefs.getBoolean("WEIGHT", weightHistogram);
    showPalette = prefs.getBoolean("SHOWPALETTE", showPalette);
    titleInImage = prefs.getBoolean("SHOWTITLE", titleInImage);
    regressionGraph = prefs.getBoolean("GRAPH", regressionGraph);
  }

  void savePreferences(Preferences prefs)
  {
    prefs.putString("TITLE", title);
    prefs.putInt("MODE", mode);
    prefs.putInt("LODEGREE", (int)loDegree);
    prefs.putInt("HIDEGREE", (int)hiDegree);
    prefs.putInt("INTENSITY", iswitch);
    prefs.putInt("BINS", nbins);
    prefs.putDouble("THRESHOLD", thresholdPercent);
    prefs.putBoolean("WEIGHT", weightHistogram);
    prefs.putBoolean("SHOWPALETTE", showPalette);
    prefs.putBoolean("SHOWTITLE", titleInImage);
    prefs.putBoolean("GRAPH", regressionGraph);
    prefs.storePreferences();
  }

 class Preferences
 {
  String fileName;
  Properties props;

  Preferences(String fileName)
  {
     this.fileName = fileName;
     props = new Properties();
     if(props != null) loadPrefs();
  }

  void loadPrefs()
  {
    String prefFile = Prefs.getHomeDir() + "\\Plugins\\" + fileName;
    try
      {BufferedInputStream in = new BufferedInputStream(
        new FileInputStream(prefFile));
      props.load(in);
      in.close();
      }
    catch(Exception e)
      {
//      IJ.write("ColorComparison: Error loading preferences file");
      }
  }

  void storePreferences()
  {
    String prefFile = Prefs.getHomeDir() + "\\Plugins\\" + fileName;
    try
      {BufferedOutputStream out = new BufferedOutputStream(
        new FileOutputStream(prefFile));
      props.store(out, "ColorComparison_ Plugin Preferences");
      out.close();
      }
    catch(Exception e)
      {
      IJ.write("ColorComparison: Error loading preferences file");
      }

  }

    String getString(String key, String defaultValue)
    {
      return props.getProperty(key, defaultValue);
    }

    void putString(String key, String value)
    {
      props.setProperty(key, value);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (props==null) return defaultValue;
        String s = props.getProperty(key);
        if (s==null)
            return defaultValue;
        else
            return s.equals("true");
    }

    void putBoolean(String key, boolean b)
    {
      String value = "false";
      if(b) value = "true";
      props.setProperty(key, value);
    }

    int getInt(String key, int defaultValue) {
        if (props==null) //workaround for Netscape JIT bug
            return defaultValue;
        String s = props.getProperty(key);
        if (s!=null) {
            try {
                return Integer.decode(s).intValue();
            } catch (NumberFormatException e) {IJ.write(""+e);}
        }
        return defaultValue;
    }

    void putInt(String key, int value)
    {
      props.setProperty(key, Integer.toString(value));
    }

    double getDouble(String key, double defaultValue) {
        if (props==null)
            return defaultValue;
        String s = props.getProperty(key);
        Double d = null;
        if (s!=null) {
            try {d = new Double(s);}
            catch (NumberFormatException e){d = null;}
            if (d!=null)
                return(d.doubleValue());
        }
        return defaultValue;
    }

    void putDouble(String key, double value)
    {
      props.setProperty(key, Double.toString(value));
    }


 } // Preferences_

  double arcAngle(double x, double y)
  {
     double ITTY = 0.000001;
     double angle = 0;

     if(x == 0) x = ITTY;
     angle = Math.atan(y / x);
     if(x < 0) angle += Math.PI;
     if(angle < 0) angle += 2*Math.PI;
     return angle;
  }

  int makeRGBPixel(int r, int g, int b)
  {
    return 0xff000000 | ((r&0xff)<<16) | ((g&0xff)<<8) | b&0xff;
  }

  void showHistogram(double[] hist)
  {
  double start = 0, inc = 0;
  DecimalFormat df = new DecimalFormat("00.0");
  String h = "";
  String s = "";
  if(mode==POLAR)
    {
    inc = 90.0 / nbins;
    start = inc / 2;
    h += "Angle";
    }
  else if(mode == PERCENTAGE)
    {
    inc = 100.0 / nbins;
    start = inc / 2;
    h += "Percentage";
    }
  else  // Regression
    {
    inc = 1;
    start = -nbins / 2.0  + inc / 2;;
    h += "Standard Error";
    }
  for(int i=0; i<nbins; i++)
    {
    s += df.format(start) + "\t" +
       df.format(hist[i]*100) + "\n";
    start += inc;
    }
  TextWindow tw = new TextWindow("", h + "\tPercent", s, 200, 300);
  }

  void makeColorBar()
  {
// Color Bar
    int cw = 80;
    int ch = 255;
    ImageProcessor ipBar = new ColorProcessor(cw, ch);
    int[] bar = new int[cw*ch];
    double deltaHue = (hi - lo) / ch;
    double hue = hi;
    int x = 0;
    for(int y=0; y<ch; y++)
      {
      int color = makeColorPixel(hue, 255);
      for(int k=0; k<cw; k++)
        {
        bar[x] = color;
        x++;
        }
      hue -= deltaHue;
      }
    ipBar.setPixels(bar);
    ImagePlus colorBar =  new ImagePlus("Palette", ipBar);
    colorBar.show();
  }

  int[] doColorComparison(byte[] pixels1, byte[] pixels2)
  {
    int[] colorPixels = new int[pixels1.length];

 // check for regression image
    if(mode == REGRESSION)
      {
      try
        {
        s = new seSums(ip1, ip2, pixels1, pixels2, threshold);
        s.show();
        }
     catch(Exception e)
        {
        IJ.write("Regression failed.");
        return null;
        }
// color pixels in # se from prediction
        return colorErrors(s, pixels1, pixels2);
      }

    for(int x=0; x<pixels1.length; x++)
        {
          int v1 = (int)pixels1[x] & 255;
          int v2 = (int)pixels2[x] & 255;
          if((v1>0) || (v2>0))
            {
            double intent = 0;
            if(iswitch==0) intent = v1;
            else if(iswitch==1) intent = v2;
            else intent = scaleIntensity*Math.sqrt((double)(v1*v1) + (double)(v2*v2));
            double polar = 0;
            if(mode == 0) polar = arcAngle((double)v1, (double)v2);
            else polar = PI2 * v2/(v1 + v2);  //  percentage
        // increment histogram
            int bin = (int)(polar / binsize);
            if(bin < 0) bin = 0;
            if(bin >= nbins) bin = nbins - 1;
            if(weightHistogram)
              {
              hist[bin] += intent;
              sumIntent += intent;
              }
            else
              {
              hist[bin]++;
              sumIntent++;
              }
            double hue = lo + scaleHue*polar;
            colorPixels[x] = makeColorPixel(hue, intent);
            }
          else colorPixels[x] = 0;
        }
    return colorPixels;
  }

  int makeColorPixel(double hue, double intent)
  {
    int r = (int) (0.5 * intent * (1.0 + Math.cos(hue)));
    int g = (int) (0.5 * intent * (1.0 + Math.cos(hue - off120)));
    int b = (int) (0.5 * intent * (1.0 + Math.cos(hue - off240)));
    return makeRGBPixel(r, g, b);
  }

 int[] colorErrors(seSums s, byte[] pixels1, byte[] pixels2)
 {
 int count = 0;
 double errorLimit = nbins / 2;
    double mid = (hi + lo) / 2;
    double f = (hi - lo) / (2 * errorLimit);
    int[] colorPixels = new int[pixels1.length];
    for(int i=0; i<pixels1.length; i++)
      {
      int xv = (int)pixels1[i] & 255;
      int yv = (int)pixels2[i] & 255;
//      double y1 = yv - s.a*xv - s.b;
      double y1 = yv - s.regress(xv);
      double ye = 0;
      if(s.se > 0) ye = y1 / s.se;
      if(ye < -errorLimit) ye = - errorLimit;
      if(ye > errorLimit) ye = errorLimit;
      double hue = mid + f*ye;
      double intent = 0;
      if(iswitch==0) intent = xv;
      else if(iswitch==1) intent = yv;
      else intent = scaleIntensity*Math.sqrt((double)(xv*xv) + (double)(yv*yv));
      colorPixels[i] = makeColorPixel(hue, intent);
// increment histogram
//      int bin = (int)(ye + errorLimit - EPSILON);
      int bin = (int)Math.round(ye + errorLimit);
      if(bin < 0) bin = 0;
      if(bin >= nbins) bin = nbins - 1;
      if(weightHistogram)
        {
        hist[bin] += intent;
        sumIntent += intent;
        }
      else
        {
        hist[bin]++;
        sumIntent++;
        }
     }
    return colorPixels;
 }

  void log(String s)
  {
    System.out.println(s);
  }

   void showAbout() {
    String descriptionFile = Prefs.getHomeDir() +
      "\\Plugins\\" + DESCRIPT;
    String s = "";
    try
      {
      BufferedReader in = new BufferedReader(
        new InputStreamReader(new FileInputStream(descriptionFile)));
      String c = null;
    while ((c = in.readLine()) != null) {
       s += c + "\n";
       }
      in.close();
      IJ.showMessage("About ColorComparison", s);
      }
    catch(Exception e)
      {
      IJ.write("ColorComparison: Error loading description file: " + descriptionFile);
      IJ.write(e.toString());
      }
  }

  public ColorComparison_() {
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  private void jbInit() throws Exception {
  }

}

 class SeRegion extends JFrame {
  JPanel jPanel1 = new JPanel();
  JTextField jtLowerLimit = new JTextField();
  JTextField jtUpperLimit = new JTextField();
  JLabel jLabel1 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JButton jbMeasure = new JButton();
  JButton jbQuit = new JButton();
  JLabel jLabel3 = new JLabel();
  JTextField jtNse = new JTextField();
  JLabel jLabel4 = new JLabel();
  JTextField jtTitle = new JTextField();
  seSums s;
  boolean weight;
  double thresholdPercent;
  ImagePlus imp1, imp2, colImage;
  ImageProcessor ip1, ip2, ipColor;
  SignificantPixels sigPix;
  int[] colPixels;
  byte[] mask;
  double alpha = 0.05;
  static final double off120 = 2.0 / 3.0 * Math.PI;
  double hLo, hHi;
  int nse;
  boolean firstTime = true;
  TextWindow tw = null;
  JPanel jPanel2 = new JPanel();
  TitledBorder titledBorder1;
  JCheckBox jCBhilite = new JCheckBox();
  JTextField jtAlpha = new JTextField();
  JLabel jLabel5 = new JLabel();
  JLabel jLabel6 = new JLabel();
  JTextField jtThresh = new JTextField();
  double[] xc = new double[256];
  double cxx;
  JButton jbReset = new JButton();

  public SeRegion() {
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  public SeRegion(ImagePlus imp1, ImagePlus imp2, ImagePlus colImage, seSums s,
         double hLo, double hHi, int nse, boolean weight, double thresholdPercent)
  {
    this.imp1 = imp1;
    this.imp2 = imp2;
    this.colImage = colImage;
    this.s = s;
    cxx = s.sxx / s.n; // sum(x - xbar)^2
    this.hLo = hLo;
    this.hHi = hHi;
    this.nse = nse;
    this.weight = weight;
    this.thresholdPercent = thresholdPercent;
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    colImage.getProcessor().snapshot();
  }

  private void jbInit() throws Exception {
    DecimalFormat df = new DecimalFormat("000.0");
    titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(134, 134, 134)),"Pixel Deviations");
    this.setSize(new Dimension(300, 317));
    jPanel1.setBorder(BorderFactory.createEtchedBorder());
    jPanel1.setBounds(new Rectangle(10, 12, 271, 145));
    jPanel1.setLayout(null);
    jtLowerLimit.setEditable(false);
    jtLowerLimit.setText(df.format(hLo));
    jtLowerLimit.setBounds(new Rectangle(18, 26, 63, 21));
    this.getContentPane().setLayout(null);
    jtUpperLimit.setEditable(false);
    jtUpperLimit.setText(df.format(hHi));
    jtUpperLimit.setBounds(new Rectangle(17, 72, 63, 21));
    jLabel1.setText("Lower Limit(Hue)");
    jLabel1.setBounds(new Rectangle(20, 6, 118, 19));
    jLabel2.setText("Upper Limit(Hue)");
    jLabel2.setBounds(new Rectangle(19, 52, 104, 20));
    jbMeasure.setText("Measure");
    jbMeasure.setBounds(new Rectangle(132, 59, 101, 24));
    jbMeasure.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jbMeasure_actionPerformed(e);
      }
    });
    jbQuit.setBounds(new Rectangle(132, 111, 101, 27));
    jbQuit.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jbQuit_actionPerformed(e);
      }
    });
    jbQuit.setText("Quit");
    jLabel3.setBounds(new Rectangle(19, 100, 65, 20));
    jLabel3.setText("# SE");
    jtNse.setBounds(new Rectangle(18, 118, 63, 21));
    jtNse.setEditable(false);
    jtNse.setText(Integer.toString(nse));
    jLabel4.setText("Title");
    jLabel4.setBounds(new Rectangle(147, 9, 74, 16));
    jtTitle.setText("Region 1");
    jtTitle.setBounds(new Rectangle(125, 28, 139, 21));
    jPanel2.setBorder(titledBorder1);
    jPanel2.setBounds(new Rectangle(9, 175, 272, 112));
    jPanel2.setLayout(null);
    jCBhilite.setText("HighLight Pixels");
    jCBhilite.setBounds(new Rectangle(8, 20, 114, 20));
    jtAlpha.setText("0.05");
    jtAlpha.setBounds(new Rectangle(30, 80, 63, 21));
    jLabel5.setFont(new java.awt.Font("Serif", 1, 12));
    jLabel5.setText("alpha");
    jLabel5.setBounds(new Rectangle(36, 56, 74, 22));
    jLabel6.setFont(new java.awt.Font("Dialog", 1, 12));
    jLabel6.setText("threshold percent");
    jLabel6.setBounds(new Rectangle(135, 57, 107, 19));
    jtThresh.setText(Double.toString(thresholdPercent));
    jtThresh.setBounds(new Rectangle(152, 78, 53, 23));
    jbReset.setText("Reset");
    jbReset.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jbReset_actionPerformed(e);
      }
    });
    jbReset.setBounds(new Rectangle(132, 84, 101, 27));
    this.getContentPane().add(jPanel1, null);
    jPanel1.add(jtNse, null);
    jPanel1.add(jLabel1, null);
    jPanel1.add(jtLowerLimit, null);
    jPanel1.add(jLabel2, null);
    jPanel1.add(jtUpperLimit, null);
    jPanel1.add(jLabel3, null);
    jPanel1.add(jLabel4, null);
    jPanel1.add(jtTitle, null);
    jPanel1.add(jbQuit, null);
    jPanel1.add(jbMeasure, null);
    jPanel1.add(jbReset, null);
    this.getContentPane().add(jPanel2, null);
    jPanel2.add(jLabel6, null);
    jPanel2.add(jtThresh, null);
    jPanel2.add(jCBhilite, null);
    jPanel2.add(jtAlpha, null);
    jPanel2.add(jLabel5, null);
// compute t corrections for possible image1 values
    for(int i=0; i<256; i++) xc[i] = xcorrect((double)i);
// get pixels from color image
    colPixels = (int[])colImage.getProcessor().getPixels();
  }

  void jbQuit_actionPerformed(ActionEvent e) {
    this.dispose();
  }

  void jbMeasure_actionPerformed(ActionEvent e)
  {
    DecimalFormat dform = new DecimalFormat("#0.000");
    DecimalFormat df1 = new DecimalFormat("#0.0");
    DecimalFormat lf = new DecimalFormat("#########0");
    ip1 = imp1.getProcessor();
    ip2 = imp2.getProcessor();
    ipColor = colImage.getProcessor();
    Rectangle roi = ipColor.getRoi();
    if(roi == null) return;
    mask = ipColor.getMaskArray();
    int rx = (int)roi.getX(); int ry = (int)roi.getY();
    int w = (int)roi.getWidth(); int h = (int)roi.getHeight();
    double sumd = 0;
    double sumv1 = 0;
    long count = 0;
    int i = 0;
    double tReject = Double.MIN_VALUE;
    double tAccept = Double.MAX_VALUE;
    sigPix = new SignificantPixels();

    int thr  = (int)(thresholdPercent/100 * ip1.getMax());
    alpha = Double.parseDouble(this.jtAlpha.getText());
    double df = s.n - 2;
    for(int y=ry; y<ry+h; y++)
      for(int x=rx; x<rx+w; x++)
        {
        if((mask==null) || (mask[i++]!=0))
          {
          int v1 = ip1.getPixel(x, y);
          int v2 = ip2.getPixel(x, y);
          int rgb = ipColor.getPixel(x, y);
          IHS c = new IHS(rgb);
//          double d = v2 - s.a*v1 - s.b;
          double d = v2 - s.regress(v1);
// check t for pixel
          if((this.jCBhilite.isSelected()) && (v1 >= thr))
            {
            if(weight) sigPix.aboveThreshold += c.intent;
            else sigPix.aboveThreshold += 1.0;
            double t = (double)d / (s.se * xc[v1]);
            if(Math.abs(t) >= tAccept) hilitePixel(x, y, c.intent, t);
            else if(Math.abs(t) >= tReject)
              {
              double p = betai(0.5*df, 0.5, df /(df + t*t));
              if(p <= alpha)
                {
                hilitePixel(x, y, c.intent, t);
                tAccept = Math.abs(t);
                }
              else tReject = Math.abs(t);
              }
            }
//              System.out.println(c.toString());
          if(weight)
            {
            sumv1 += v1*c.intent;
            sumd += d * c.intent;
            count += c.intent;
            }
          else
            {
            sumv1 += v1;
            sumd += d;
            count++;
            }
//              System.out.println("hue=" + c.hue + " intent=" + c.intent);
          }
        } // for(int x
    double se = 0;
    double v1bar = 0;
    if(count > 0)
      {
      se = sumd / (count * s.se);
      v1bar = sumv1 / count; // mean x of ROI
      }
// compute t of deviation
//   double c = Math.sqrt(1.0 + 1.0/s.n + ((v1bar - s.xbar)*(v1bar - s.xbar)) / cxx);
    double c = xcorrect(v1bar);
    double t = se / c;
    double p = betai(0.5*df, 0.5, df /(df + t*t));

    if(nse <= 0) return;
    if(firstTime)
      {
      firstTime = false;
      String headings = "Region\tN\tDeviation\tp(t)";
      if(weight) headings = "Region\tWeight\tDeviation\tp(t)";
      String data = jtTitle.getText() + "\t" + lf.format(count) + "\t" +
      dform.format(se) + "\t" + dform.format(p);
      tw = new TextWindow("Regional Deviation", headings, data, 300, 200);
      }
    else
      {
      tw.append(jtTitle.getText() + "\t" + lf.format(count) + "\t" +
      dform.format(se) + "\t" + dform.format(p));
      }
 //     colImage.getProcessor().setPixels((Object)colPixels);
      colImage.updateAndDraw();
//IJ.write("reject=" + dform.format(tReject) + "  accept=" + dform.format(tAccept));
   if(this.jCBhilite.isSelected())
      {
      double plus = 100 * sigPix.tPlus / sigPix.aboveThreshold;
      double minus = 100 * sigPix.tMinus / sigPix.aboveThreshold;
      IJ.write(jtTitle.getText() + ":  n=" + df1.format(sigPix.aboveThreshold)
      +  "  t+ = " + df1.format(plus)
      + "%     t- = " + df1.format(minus) + "%");
      }
   }


   void hilitePixel(int x, int y, double intent, double t)
   {
      int color = 255;
      if(t > 0)
        {
        color = 127;
        if(weight) sigPix.tPlus += intent;
        else sigPix.tPlus += 1.0;
        }
      else
        {
        if(weight) sigPix.tMinus += intent;
        else sigPix.tMinus += 1.0;
        }

        colPixels[y*colImage.getWidth() + x] = makeRGBPixel(color, color, color);

 //     IJ.setForegroundColor(color, color, color);
 //     this.ipColor.drawPixel(x, y);
   }


// The regresson line certainly passes thru <xbar,ybar>, but its location becomes
//  less certain as x differs from xbar. So we need this tiny correction.
   double xcorrect(double x)
   {
      return Math.sqrt(1.0 + 1.0/s.n + ((x - s.xbar)*(x - s.xbar)) / cxx);
   }

   public double tInverse(double p, double df)
   {
   final double eps = 0.0001;
   double pt = 1.0;
   double t = 2.0;
   double lo = 0;
   double hi = 60.0;
   int i = 0;
   while(Math.abs(p - pt) > eps)
     {
     t = (hi + lo) / 2.0;
     pt = betai(0.5*df, 0.5, df /(df + t*t));
//     IJ.write("tInv t=" + t + " p=" + pt + " lo=" + lo + " hi=" + hi);
     if(pt < p) hi = t;
     else lo = t;
     i++;
     if(i>50) break;
     }
   return t;
   }

 class IHS
 {
  int rgb, r, g, b;
  double intent, hue, sat = 1.0;

  IHS(int rgb)
  {
    this.rgb = rgb;
    r = (rgb>>16) & 255;
    g = (rgb>>8) & 255;
    b = rgb & 255;
    intent = (r + g + b)/1.5;
    double cos = 2.0*r/intent - 1;
    if(cos > 1.0) cos = 1.0;
    else if(cos < -1.0) cos = -1.0;
    hue = Math.acos(cos);
// predict green
    double g1 = 0.5 * intent * (1.0 + Math.cos(hue - off120));
    double g2 = 0.5 * intent * (1.0 + Math.cos(-hue - off120));
    if(Math.abs(g - g2) < Math.abs(g - g1)) hue = -hue;
 }

   public String toString()
   {
    return "RGB: <" + r + "," + g + "," + b +
           ">  IHS: <" + intent + "," + hue + "," + sat + ">";
   }
} // end IHS


// t distribution methods
  double gamln(double xx)
  {
    double x, y, tmp, ser;
    double cof[] =
      {
      76.18009172947146,
      -86.50532032941677,
      24.01409824083091,
      -1.231739572450155,
      0.1208650973866179e-2,
      -0.5395239384953e-5
      };

    y = x = xx;
    tmp = x + 5.5;
    tmp -= (x + 0.5) * Math.log(tmp);
    ser = 1.000000000190015;
    for(int j=0; j<6; j++) ser += cof[j] / ++y;
    return -tmp + Math.log(2.5066282746310005*ser/x);
  }

  double betai(double a, double b, double x)
 // returns the incomplete beta fuction (Numerical Recipes)
 {
  double bt;

    if(x<0 || x>1) nerror("bad x in betai!");
    if(x==0 || x==1) bt = 0;
    else
      bt = Math.exp(gamln(a+b) - gamln(a) - gamln(b) + a*Math.log(x) + b*Math.log(1 - x));
    if(x<(a+1)/(a+b+2)) // use continued fraction directly
      return bt*betacf(a,b,x)/a;
    else  // use continued fraction after symetry transformation
      return 1 - bt*betacf(b,a,1-x)/b;
 }

 double betacf(double a, double b, double x)
 {
  // continued fraction for incomplete beta function
  final int MAXIT = 100;
  final double EPS = 3.0e-7;
  int m, m2;
  double aa,c,d,del,h,qab,qam,qap;

    qab = a + b;
    qap = a + 1.0;
    qam = a - 1.0;
    c = 1.0;
    d = 1.0 - qab*x/qap;
    if(Math.abs(d) < Double.MIN_VALUE) d = Double.MIN_VALUE;
    d = 1.0 / d;
    h = d;
    for(m=1; m<=MAXIT; m++)
      {
      m2 = 2*m;
      aa = m*(b-m)*x / ((qam + m2) * (a + m2));
      d = 1.0 + aa*d;
      if(Math.abs(d) < Double.MIN_VALUE) d = Double.MIN_VALUE;
      c = 1.0 + aa/c;
      if(Math.abs(c) < Double.MIN_VALUE) c = Double.MIN_VALUE;
      d = 1.0 / d;
      h *= d*c;
      aa = -(a+m)*(qab+m)*x / ((a+m2) * (qap+m2));
      d = 1.0 + aa*d;
      if(Math.abs(d) < Double.MIN_VALUE) d = Double.MIN_VALUE;
      c = 1.0 + aa/c;
      if(Math.abs(c) < Double.MIN_VALUE) c = Double.MIN_VALUE;
      d = 1.0 / d;
      del = d*c;
      h *= del;
      if(Math.abs(del-1.0) < EPS) break;
      }
      if(m > MAXIT) nerror("a or b too big, or MAXIT too smal in betacf");
      return h;
    }

  int makeRGBPixel(int r, int g, int b)
  {
    return 0xff000000 | ((r&0xff)<<16) | ((g&0xff)<<8) | b&0xff;
  }

  void nerror(String s)
  {
    System.out.println(s);
    System.exit(-1);
  }

  void jbReset_actionPerformed(ActionEvent e) {
// todo
    colImage.getProcessor().reset();
    colImage.updateAndDraw();
  }

  class SignificantPixels
  {
    double aboveThreshold, tPlus, tMinus;

    SignificantPixels()
    {
      super();
    }
  } // end SignificantPixels
} // end class SeREgion

  class seSums
  {
    double
      n = 0,
      sx=0, sy = 0,
      sxx=0, syy=0, sxy=0,
      xbar, ybar,
      a=0, b=0, se=0, r=0;
    double
      df, t, p;

    int rx, ry, w, h;
    byte[] mask;

    seSums(ImageProcessor ip1, ImageProcessor ip2,
         byte[] pixels1, byte[] pixels2, int threshold) throws Exception
    {
    IJ.write("threshold=" + threshold);
    if(ip1 != null)
      {
      Rectangle roi = ip1.getRoi();
      if(roi==null) IJ.write("roi is null");
      else
        {
        mask = ip1.getMaskArray();
        rx = (int)roi.getX(); ry = (int)roi.getY();
        w = (int)roi.getWidth(); h = (int)roi.getHeight();
        } // if(roi
      } // if(ip1
    else throw new Exception("No imageProcessor");

    int i = 0;
    for(int y=ry; y<ry+h; y++)
      for(int x=rx; x<rx+w; x++)
        {
        if((mask==null) || (mask[i++]!=0))
          {
          int vx = ip1.getPixel(x, y);
          if(vx >= threshold)
            {
            int vy = ip2.getPixel(x, y);
            n++;
            sx += vx; sy += vy;
            sxx += vx*vx; syy += vy*vy;
            sxy += vx*vy;
            }
          }
        } // for(int x
//  Get regression coefficients
//     a = (n*sxy - sx*sy) / (n*sxx - sx*sx);
//      b = (sy - a*sx) / n;
    xbar = sx / n;
    ybar = sy / n;
    sxx = n*sxx -sx*sx; syy = n*syy - sy*sy; sxy = n*sxy -sx*sy;
    a = sxy / sxx;
    b = (sy - a*sx) / n;

// Get standard error of estimate
    i = 0;
    for(int y=ry; y<ry+h; y++)
      for(int x=rx; x<rx+w; x++)
        {
        if((mask==null) || (mask[i++]!=0))
          {
          int vx = ip1.getPixel(x, y);
          if(vx >= threshold)
            {
            int vy = ip2.getPixel(x, y);
            double ye = vy - a*vx - b;
            se += (ye * ye);
            }
          }
        } // for(int x
    se = Math.sqrt(se / (n-2));
// Pearson r
//    r = (n*sxy - sx*sy) / Math.sqrt((n*sxx - sx*sx) * (n*syy - sy*sy));
      r = sxy / Math.sqrt(sxx * syy);
    }

    double regress(double x)
    {
    return b + a*x;
    }

    void show()
    {
    DecimalFormat df = new DecimalFormat("##0.00");
    IJ.write("Regression:  y = " + df.format(a) + "*x + " + df.format(b) +
              "\nn = " + n +
              "\nStandard Error = " + df.format(se) + "    Pearson r = "
		  + df.format(r));
    }
  } // end seSums




