import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.*;

/* DCT plugin
   v 1.0, Sep. 18, 2001
   Werner Bailer <werner@wbailer.com> */
public class DCT_ implements PlugInFilter {

    protected double outscale;
    protected double outadd;

    public int setup(String arg, ImagePlus imp) {
        return DOES_8G+DOES_16+DOES_32+DOES_STACKS;
    }

    public void run(ImageProcessor ip) {
      int n=8;
      int u=0;
      int v=0;
      boolean forward = true;
      boolean resize =false;
      boolean pwr2 = false;

      // user interface ////////////////////////////////////////////////////////

      while (!pwr2) {
        GenericDialog dlg = new GenericDialog("DCT/IDCT parameters");
        dlg.addNumericField("block size N (power of 2)",8,0);
        dlg.addCheckbox("inverse DCT",false);
        String[] resOrTiles = new String[2];
        resOrTiles[0]="split image into tiles of size N x N";
        resOrTiles[1]="resize image to N x N";
        dlg.addChoice("resize or tile?",resOrTiles,resOrTiles[0]);
        dlg.addNumericField("multiply output with:",1.0,5);
        dlg.addNumericField("add to output:",0.0,5);
        dlg.showDialog();

        if (dlg.wasCanceled()) return;
        n = (int) dlg.getNextNumber();
        outscale = dlg.getNextNumber();
        pwr2 = (((n & (n-1))==0) && (n>0));
        forward = !(dlg.getNextBoolean());
        if (dlg.getNextChoiceIndex()==1) resize=true;
        if (!pwr2) IJ.showMessage(n+" is not a power of 2!");
      }

      //////////////////////////////////////////////////////////////////////////

      if (resize) ip=ip.resize(n,n);

      ImagePlus newimage = null;
      DCT dct = new DCT(n);
      int w = ip.getWidth();
      int h = ip.getHeight();

      // prepare output image
      newimage = NewImage.createFloatImage("",w-w%n,h-h%n,1,NewImage.FILL_BLACK);
      ImageProcessor np = newimage.getProcessor();

      // calculate (I)DCT for each tile
      for (int i=0;i<w/n;i++) {
        u=i*n;
        for (int j=0;j<h/n;j++) {
          v=j*n;
          double[][] pixels = null;
          if (ip instanceof ByteProcessor) pixels = getBytePixels((ByteProcessor) ip,u,v,n);
          if (ip instanceof ShortProcessor) pixels = getShortPixels((ShortProcessor) ip,u,v,n);
          if (ip instanceof FloatProcessor) pixels = getFloatPixels((FloatProcessor) ip,u,v,n);

          if (forward) pixels = dct.forwardDCT(pixels);
          else pixels = dct.inverseDCT(pixels);

          setFloatPixels((FloatProcessor) np,pixels,u,v);
        }
      }

      // show result
      np.resetMinAndMax();
      newimage.setProcessor("",np);
      if (forward) newimage.setTitle("DCT");
      else newimage.setTitle("IDCT");
      newimage.updateAndDraw();
      newimage.show();
    }

    /* extract pixels from images of different type */
    protected double[][] getBytePixels(ByteProcessor ip, int x, int y, int n) {
      byte[] pix = (byte[]) ip.getPixels();
      double[][] pix2d = new double[n][n];
      for (int j=0;j<n;j++) {
        int offs = (y+j)*ip.getWidth();
        for (int i=0;i<n;i++) {
          pix2d[i][j] = (double)(pix[offs+x+i]&0xff);
          pix2d[i][j]=(pix2d[i][j]-128)/128.0;
        }
      }
      return pix2d;
    }

    protected double[][] getShortPixels(ShortProcessor ip, int x, int y, int n) {
      short[] pix = (short[]) ip.getPixels();
      double[][] pix2d = new double[n][n];
      for (int j=0;j<n;j++) {
        int offs = (y+j)*ip.getWidth();
        for (int i=0;i<n;i++) {
          pix2d[i][j] = (double)(pix[offs+x+i]&0xffff);
          pix2d[i][j]=(pix2d[i][j]-65536)/65536;
        }
      }
      return pix2d;
    }

    protected double[][] getFloatPixels(FloatProcessor ip, int x, int y, int n) {
      float[] pix = (float[]) ip.getPixels();
      double[][] pix2d = new double[n][n];
      for (int j=0;j<n;j++) {
        int offs = (y+j)*ip.getWidth();
        for (int i=0;i<n;i++) {
          pix2d[i][j] = (double)pix[offs+x+i];
        }
      }
      return pix2d;
    }

    /* set block of float pixels */
    protected void setFloatPixels(FloatProcessor ip, double[][] dp, int x, int y) {
      FloatProcessor fp = new FloatProcessor(dp.length,dp[0].length);
      float[] pix = new float[dp.length*dp[0].length];
      for (int j=0;j<dp.length;j++) {
        int offs = j*dp.length;
        for (int i=0;i<dp.length;i++) {
          pix[offs+i] = (float) (dp[i][j]*outscale+outadd);
        }
      }
      fp.setPixels(pix);
      ip.insert(fp,x,y);
    }

}

 class DCT {

    // block size
    protected int N;
    // coefficients
    protected double[][] c;
    // zig zag matrix
    protected int[][] zigzag;

    public DCT(int n) {
        this.N=n;
        initCoefficients();
        zigzag = makeZigZagMatrix();
    }

    /* initialize coefficient matrix */
    protected void initCoefficients() {
        c = new double[N][N];

        for (int i=1;i<N;i++) {
        	for (int j=1;j<N;j++) {
        		c[i][j]=1;
        	}
        }

        for (int i=0;i<N;i++) {
                c[i][0]=1/Math.sqrt(2.0);
        	c[0][i]=1/Math.sqrt(2.0);
        }
        c[0][0]=0.5;
    }

    protected double[][] forwardDCT(double[][] input) {
        double[][] output = new double[N][N];

        for (int u=0;u<N;u++) {
          for (int v=0;v<N;v++) {
            double sum = 0.0;
            for (int x=0;x<N;x++) {
              for (int y=0;y<N;y++) {
                sum+=input[x][y]*Math.cos(((2*x+1)/(2.0*N))*u*Math.PI)*Math.cos(((2*y+1)/(2.0*N))*v*Math.PI);
              }
            }
            sum*=c[u][v]/4.0;
            output[u][v]=sum;
          }
        }
        return output;
    }

    protected double[][] inverseDCT(double[][] input) {
       double[][] output = new double[N][N];

       for (int x=0;x<N;x++) {
        for (int y=0;y<N;y++) {
          double sum = 0.0;
          for (int u=0;u<N;u++) {
            for (int v=0;v<N;v++) {
            sum+=c[u][v]*input[u][v]*Math.cos(((2*x+1)/(2.0*N))*u*Math.PI)*Math.cos(((2*y+1)/(2.0*N))*v*Math.PI);
            }
          }
          sum/=4.0;
          output[x][y]=sum;
        }
       }
       return output;
    }

    /* write dct coefficient matrix into 1D array in zig zag order */
    public double[] zigZag(double[][] m) {
    	double[] zz = new double[N*N];
    	for (int i=0;i<N;i++) {
    		for (int j=0;j<N;j++) zz[zigzag[i][j]]=m[i][j];
    	}
    	return zz;
    }

    /* write zig zag ordered coefficients into matrix */
    public double[][] unZigZag(double[] zz) {
      double[][] m = new double[N][N];
      for (int i=0;i<N;i++) {
        for (int j=0;j<N;j++) {
          m[i][j]=zz[zigzag[i][j]];
        }
      }
      return m;
    }

    /* generate zig zag matrix */
    private int[][] makeZigZagMatrix() {
        int[][] zz = new int[N][N];
        int zval=0;
        int zval2=N*(N-1)/2;
        int i,j;
        for (int k=0;k<N;k++) {
          if (k%2==0) {
            i=0;
            j=k;
            while (j>-1) {
              zz[i][j]=zval;
              zval++;
              i++;
              j--;
            }
            i=N-1;
            j=k;
            while (j<N) {
              zz[i][j]=zval2;
              zval2++;
              i--;
              j++;
            }
          }
          else {
            i=k;
            j=0;
            while (i>-1) {
              zz[i][j]=zval;
              zval++;
              j++;
              i--;
            }
            i=k;
            j=N-1;
            while (i<N) {
              zz[i][j]=zval2;
              zval2++;
              i++;
              j--;
            }
          }
        }
        return zz;
    }

}
