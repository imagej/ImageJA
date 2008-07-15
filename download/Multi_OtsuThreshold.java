//
// Multi_OtsuThreshold_.java
//
// Algorithm: PS.Liao, TS.Chen, and PC. Chung,
//            Journal of Information Science and Engineering, vol 17, 713-727 (2001)
// 
// Coding   : Yasunari Tosa (ytosa@att.net)
// Date     : Feb. 19th, 2005
//
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Multi_OtsuThreshold implements PlugInFilter 
{
  ImagePlus imp;
  static final int NGRAY=256;

  public int setup(String arg, ImagePlus imp) 
  {
    this.imp = imp;
    return DOES_8G + NO_CHANGES;
  }

  public void run(ImageProcessor ip) 
  {
    int MLEVEL = 4; // 3 level

    GenericDialog gd = new GenericDialog("Select numLevels");
    String [] items= { "2", "3", "4", "5", };
    gd.addChoice("numLevels", items, "4");
    gd.showDialog();
    if (gd.wasCanceled())
      return;

    MLEVEL = gd.getNextChoiceIndex() + 2;
    IJ.log("numLevels set to " + MLEVEL);

    int [] threshold = new int[MLEVEL]; // threshold
    int width = ip.getWidth();
    int height = ip.getHeight();
    ////////////////////////////////////////////
    // Build Histogram
    ////////////////////////////////////////////
    float [] h = new float[NGRAY];
    byte[] pixels = (byte[]) ip.getPixels();
    buildHistogram(h, pixels, width, height);

    /////////////////////////////////////////////
    // Build lookup tables from h
    ////////////////////////////////////////////
    float [][] P = new float[NGRAY][NGRAY];
    float [][] S = new float[NGRAY][NGRAY];
    float [][] H = new float[NGRAY][NGRAY];
    buildLookupTables(P, S, H, h);

    ////////////////////////////////////////////////////////
    // now M level loop   MLEVEL dependent term
    ////////////////////////////////////////////////////////
    float maxSig = findMaxSigma(MLEVEL, H, threshold);
    String msg = "thresholds: ";;
    for (int i=0; i < MLEVEL; ++i)
      msg += i + "=" + threshold[i] + ", ";
    msg += " maxSig = " + maxSig;
    IJ.log(msg);

    ///////////////////////////////////////////////////////////////
    // show regions works for any MLEVEL
    ///////////////////////////////////////////////////////////////
    showRegions(MLEVEL, threshold, pixels, width, height);
  }

  public void buildHistogram(float [] h, byte [] pixels, int width, int height)
  {
    // note Java byte is signed. in order to make it 0 to 255 you have to
    // do int pix = 0xff & pixels[i];
    for (int i=0; i < width*height; ++i)
      h[(int) (pixels[i]&0xff)]++;
   // note the probability of grey i is h[i]/(width*height)
    float [] bin = new float[NGRAY];
    float hmax = 0.f;
    for (int i=0; i < NGRAY; ++i)
    {
      bin[i] = (float) i;
      h[i] /= ((float) (width*height));
      if (hmax < h[i])
	hmax = h[i];
    }
    PlotWindow histogram = new PlotWindow("Histogram", "grey", "hist", bin, h);
    histogram.setLimits(0.f, (float) NGRAY, 0.f, hmax);
    histogram.draw();
  }

  public void buildLookupTables(float [][] P, float [][] S, float [][] H, float [] h)
  {
    // initialize
    for (int j=0; j < NGRAY; j++)
      for (int i=0; i < NGRAY; ++i)
      {
	P[i][j] = 0.f;
	S[i][j] = 0.f;
	H[i][j] = 0.f;
      }
    // diagonal 
    for (int i=1; i < NGRAY; ++i)
    {
      P[i][i] = h[i];
      S[i][i] = ((float) i)*h[i];
    }
    // calculate first row (row 0 is all zero)
    for (int i=1; i < NGRAY-1; ++i)
    {
      P[1][i+1] = P[1][i] + h[i+1];
      S[1][i+1] = S[1][i] + ((float) (i+1))*h[i+1];
    }
    // using row 1 to calculate others
    for (int i=2; i < NGRAY; i++)
      for (int j=i+1; j < NGRAY; j++)
      {
	P[i][j] = P[1][j] - P[1][i-1];
	S[i][j] = S[1][j] - S[1][i-1];
      }
    // now calculate H[i][j]
    for (int i=1; i < NGRAY; ++i)
      for (int j=i+1; j < NGRAY; j++)
      {
	if (P[i][j] != 0)
	  H[i][j] = (S[i][j]*S[i][j])/P[i][j];
	else
	  H[i][j] = 0.f;
      }

  }

  public float findMaxSigma(int mlevel, float [][] H, int [] t)
  {
    t[0] = 0;
    float maxSig= 0.f;
    switch(mlevel)
    {
    case 2:
      for (int i= 1; i < NGRAY-mlevel; i++) // t1
      {
	float Sq = H[1][i] + H[i+1][255];
	if (maxSig < Sq)
	{
	  t[1] = i;
	  maxSig = Sq;
	}
      } 
      break;
    case 3:
      for (int i= 1; i < NGRAY-mlevel; i++) // t1
	for (int j = i+1; j < NGRAY-mlevel +1; j++) // t2
	{
	  float Sq = H[1][i] + H[i+1][j] + H[j+1][255];
	  if (maxSig < Sq)
	  {
	    t[1] = i;
	    t[2] = j;
	    maxSig = Sq;
	  }
	} 
      break;
    case 4:
      for (int i= 1; i < NGRAY-mlevel; i++) // t1
	for (int j = i+1; j < NGRAY-mlevel +1; j++) // t2
	  for (int k = j+1; k < NGRAY-mlevel + 2; k++) // t3
	  {
	    float Sq = H[1][i] + H[i+1][j] + H[j+1][k] + H[k+1][255];
	    if (maxSig < Sq)
	    {
	      t[1] = i;
	      t[2] = j;
	      t[3] = k;
	      maxSig = Sq;
	    }
	  } 
      break;
    case 5:
      for (int i= 1; i < NGRAY-mlevel; i++) // t1
	for (int j = i+1; j < NGRAY-mlevel +1; j++) // t2
	  for (int k = j+1; k < NGRAY-mlevel + 2; k++) // t3
	    for (int m = k+1; m < NGRAY-mlevel + 3; m++) // t4
	  {
	    float Sq = H[1][i] + H[i+1][j] + H[j+1][k] + H[k+1][m] + H[m+1][255];
	    if (maxSig < Sq)
	    {
	      t[1] = i;
	      t[2] = j;
	      t[3] = k;
	      t[4] = m;
	      maxSig = Sq;
	    }
	  } 
      break;
    }
    return maxSig; 
  }

  public void showRegions(int mlevel, int [] t, byte [] pixels, int width, int height)
  {
    ImagePlus [] region = new ImagePlus[mlevel];
    ImageProcessor [] rip = new ImageProcessor[mlevel];
    for (int i=0; i < mlevel; ++i)
    {
      region[i] = NewImage.createByteImage("Region "+i, width, height,1, NewImage.FILL_BLACK);
      rip[i] = region[i].getProcessor();
    }
    for (int i = 0; i < width*height; ++i)
    {
      int val = 0xff & pixels[i];
      for (int k = 0; k < mlevel; k++)
      {
	if (k < mlevel-1)
	{
	  if (val < t[k+1] && val > t[k]) // k-1 region
	    rip[k].putPixel(i%width, i/width, val);
	}
	else // k= mlevel-1 last region
	{
	  if (val > t[k])
	    rip[k].putPixel(i%width, i/width, val);
	}
      }
    }
    for (int i=0; i < mlevel; i++)
      region[i].show();
  }
}
