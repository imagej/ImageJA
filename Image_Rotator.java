import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.text.TextWindow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

 
/*
 *  @version  1.0 10 May 2012
 *  
 *            
 * @author Dimiter Prodanov
 * 		   IMEC
 *
 *
 * @contents This plugin rotates the active image around the center of mass of a drawn ROI.
 * The image is rotated so that the X direction corresponds to the principal orientation axis of the ROI.  
 *
* @license This library is free software; you can redistribute it and/or
*      modify it under the terms of the GNU Lesser General Public
*      License as published by the Free Software Foundation; either
*      version 2.1 of the License, or (at your option) any later version.
*
*      This library is distributed in the hope that it will be useful,
*      but WITHOUT ANY WARRANTY; without even the implied warranty of
*      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
*       Lesser General Public License for more details.
*
*      You should have received a copy of the GNU Lesser General Public
*      License along with this library; if not, write to the Free Software
*      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

public class Image_Rotator implements PlugInFilter {
	private ImagePlus imp;
    private Roi roi;
    private static final String LINW="lineWidth";

	
    private boolean isProcessibleRoi=false;
	private double volume=0;
	private double xbar=0;
	private double ybar=0;
	private double [] centroid=new double[2];
	private int order=3;
	boolean raw_calculated=false, centr_calculated=false, scale_calculated=false;
 
	private double[][] centralmoments;

	double [][] cov=new double[2][2];
	private double lambda1=0;
	private double lambda2=0;
	private double theta=0;

	private static int lineWidth=(int)Prefs.getDouble(LINW,1);
	

	
	/* (non-Javadoc)
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	//private int width=-1, height=-1;
	private double eccentricity=-1;
	
	
	//@Override
	public void run(ImageProcessor ip) {
 
		int offsetx=0, offsety=0;
	 
		if (isProcessibleRoi) {
	
			Rectangle r= roi.getBounds();
	
			Log("rect: " +r.x + " "+ r.y+ " "+ r.width+ " "+ r.height);
			Log("offset: "+ offsetx + " ; "+ offsety);
			
			SimpleShapeStatistics ms=new SimpleShapeStatistics( roi.getPolygon());
			 
			String unit="pixel";
			String unit2=unit;
			 ms.calculateMoments();
		 
			
			centralmoments=ms.getCentralMoments();
			centroid=ms.getCentroid();
			volume=ms.getVolume();
			
			StringBuffer sb=new StringBuffer (200);
			
			double xxbar=centroid[0]+offsetx; // offset correction
			double yybar=centroid[1]+offsety; // offset correction
			sb.append("r.x\t"+  offsetx+ "\t"+"pixels"+"\n" );
			//ret.put("r.x", offsetx);
			sb.append("r.y\t"+  offsety+ "\t"+"pixels"+"\n" );
			//ret.put("r.y", offsety);
		 	
			sb.append("xbar\t"+  xxbar+ "\t"+unit2+"\n" );
			sb.append("ybar\t"+  yybar+ "\t"+unit2+"\n" );
			
			ret.put("xbar", xxbar);
			ret.put("ybar", yybar);
			xbar=centroid[0];  
			ybar=centroid[1];  
			
			for (int i=0; i<centralmoments.length-1; i++) {
				for (int j=0; j<centralmoments[0].length-1; j++) {
					int k=i+j;
					if (k<order){
					sb.append("mu["+i+"]["+j +"]\t"+centralmoments[i][j]+ "\t"+unit+"^" +k+ "\n" );
					//sb.append("I["+i+"]["+j +"]\t"+centralmoments[i][j]/volume+ "\t"+unit+"^" +k+ "\n" );
					}
				}
			}
			for (int i=0; i<centralmoments.length-1; i++) {
				final int a=i++;
				for (int j=0; j<centralmoments[0].length-1; j++) {
					int k=i+j;
					final int b=i++;
					if ((k<order) && (k>1)){
						sb.append("C["+a+"]["+b +"]\t"+IJ.d2s(centralmoments[i][j]/volume,4)+ "\t"+unit+"^" +k+ "\n" );
					}
				}
			}
			
			double momentinertia=centralmoments[2][0]+centralmoments[2][2];
				 
			orientationStatistics(ms  );
		 
		 
			sb.append("lambda1\t"+ IJ.d2s(lambda1,4) +"\t"+unit2+"^2\n");
			ret.put("lambda1", lambda1);
			sb.append("lambda2\t"+ IJ.d2s(lambda2,4)+"\t"+unit2+"^2\n");
			ret.put("lambda2", lambda2);
			
			theta=Math.PI/2-theta;
			
			sb.append("theta\t"+IJ.d2s(theta,4) +"\trad\n");
			ret.put("theta", theta);
			
			final double theta_deg=theta*180.0/Math.PI;
			
			sb.append("theta.deg\t"+IJ.d2s(theta_deg,4)+"\tdeg\n");
			sb.append("eccentricity\t"+ IJ.d2s(eccentricity,4)+"\n");
			sb.append("formfactor\t"+ IJ.d2s(formfactor,4)+"\n");
			ret.put("k", formfactor);
			new TextWindow("Parameters-"+imp.getTitle(), "parameter\tvalue\tunit", sb.toString(), 500, 300);
		 
			 rotateAll(imp, roi, theta_deg);
			 rotateRoi(theta,   roi); // in rads
		
			
		} else { // no ROI or not closed
		 
			 
			
		} // end else
		
		
	}
	
	int interpolationMethod=ImageProcessor.BICUBIC;
	
	public void rotateAll(ImagePlus imp, Roi roi, double theta) {
		
		Roi all=new Roi(0, 0, imp.getWidth(), imp.getHeight());	
		imp.setRoi(all);
		
		int n=imp.getStackSize();
		if (n==1) {
			
			ImageProcessor ip = imp.getProcessor();
			ip.setInterpolationMethod(interpolationMethod);
	 
			 rotate(theta, ip, roi);
		} else {
			ImageStack is=imp.getStack();
			int width=is.getWidth();
			int height=is.getHeight();
			ImageStack isnew=new ImageStack(width, height);
			for (int i=0; i<n; i++) {
				
				ImageProcessor ip =is.getProcessor(i+1);
				ip.setInterpolationMethod(interpolationMethod);
 
				rotate(theta, ip, roi);
	 
				isnew.addSlice(""+1, ip);
			}
			imp.setStack(isnew);
			is=null;
		}
		imp.updateAndDraw();		
		//imp.setRoi(roi, true);
		
		 
		 
	}

	public void rotate(double angle, ImageProcessor ip, Roi roi) {	
		Rectangle r=roi.getBounds();
		if (ip instanceof FloatProcessor)
			rotateF(angle, (FloatProcessor)ip, r);
		if (ip instanceof ByteProcessor)
			rotateB(angle, (ByteProcessor)ip, r);
		if (ip instanceof ShortProcessor)
			rotateS(angle, (ShortProcessor)ip, r);
		 
		
	}
	 
	/** Rotates the image or ROI 'angle' degrees clockwise.
	 */
	public void rotateF(double angle, FloatProcessor fp, Rectangle r) {
		System.out.println("rotating float...");
		float[] pixels = (float[])fp.getPixelsCopy();	 

		double centerX = r.x + (r.width)/2.0;
		double centerY = r.y + (r.height)/2.0;


		double angleRadians = -angle/(180.0/Math.PI);
		double ca = Math.cos(angleRadians);
		double sa = Math.sin(angleRadians);
		double tmp1 = centerY*sa-centerX*ca;
		double tmp2 = -centerX*sa-centerY*ca;
		double tmp3, tmp4, xs, ys;
		int index=0;
		int width=fp.getWidth();
		int height=fp.getHeight();

		for (int y=0; y<height; y++) {

			tmp3 = tmp1 - y*sa + centerX;
			tmp4 = tmp2 + y*ca + centerY;
			for (int x=0; x<width; x++) {
				index = y*width + x;
				xs = x*ca + tmp3;
				ys = x*sa + tmp4;
				pixels[index++] = (float)fp.getInterpolatedPixel(xs, ys);
			}			 
		}

		fp.setPixels(pixels);
	}

	/** Rotates the image or ROI 'angle' degrees clockwise.
	 */
	public void rotateB(double angle, ByteProcessor bp, Rectangle r) {
		System.out.println("rotating byte...");
		byte[] pixels = (byte[])bp.getPixelsCopy();


		double centerX = r.x + (r.width)/2.0;
		double centerY = r.y + (r.height)/2.0;


		double angleRadians = -angle/(180.0/Math.PI);
		double ca = Math.cos(angleRadians);
		double sa = Math.sin(angleRadians);
		double tmp1 = centerY*sa-centerX*ca;
		double tmp2 = -centerX*sa-centerY*ca;
		double tmp3, tmp4, xs, ys;
		int index=0;
		int width=bp.getWidth();
		int height=bp.getHeight();

		for (int y=0; y<height; y++) {

			tmp3 = tmp1 - y*sa + centerX;
			tmp4 = tmp2 + y*ca + centerY;
			for (int x=0; x<width; x++) {
				index = y*width + x;
				xs = x*ca + tmp3;
				ys = x*sa + tmp4;
				pixels[index++] = (byte)bp.getInterpolatedPixel(xs, ys);
			}			 
		}

		bp.setPixels(pixels);

	}

	/** Rotates the image or ROI 'angle' degrees clockwise.
	@see ImageProcessor#setInterpolate
	 */
	public void rotateS(double angle, ShortProcessor sp, Rectangle r) {
		System.out.println("rotating float...");
		short[] pixels = (short[])sp.getPixelsCopy();


		double centerX = r.x + (r.width)/2.0;
		double centerY = r.y + (r.height)/2.0;


		double angleRadians = -angle/(180.0/Math.PI);
		double ca = Math.cos(angleRadians);
		double sa = Math.sin(angleRadians);
		double tmp1 = centerY*sa-centerX*ca;
		double tmp2 = -centerX*sa-centerY*ca;
		double tmp3, tmp4, xs, ys;
		int index=0;
		int width=sp.getWidth();
		int height=sp.getHeight();

		for (int y=0; y<height; y++) {

			tmp3 = tmp1 - y*sa + centerX;
			tmp4 = tmp2 + y*ca + centerY;
			for (int x=0; x<width; x++) {
				index = y*width + x;
				xs = x*ca + tmp3;
				ys = x*sa + tmp4;
				pixels[index++] = (short)sp.getInterpolatedPixel(xs, ys);
			}			 
		}

		sp.setPixels(pixels);

	}

	
	 

	/**
	 * 
	 */
	private void updateResults(Hashtable<String, Double> ret) {
		ResultsTable rt=Analyzer.getResultsTable();
		
		rt.reset();

		int valind=rt.getFreeColumn("value");
		//int cnt=rt.getCounter();
		Set<Entry<String, Double>> es=ret.entrySet();
		
		for (Iterator<Entry<String, Double>> iter=es.iterator(); iter.hasNext();) {
	 		//cnt++;
			Entry<String, Double> entry=iter.next();
			rt.incrementCounter();			 	 
			rt.addValue(valind, entry.getValue());			
			rt.setLabel(entry.getKey(), rt.getCounter()-1);
			
			
		}
		if (debug) {
			rt.show("NR");
		}
	}

	Hashtable<String, Double> ret=new Hashtable<String, Double>();

	double formfactor=1.0;
	
  	public void orientationStatistics(SimpleShapeStatistics ms ){
		

  		//Volume: M00
		centralmoments=ms.getCentralMoments();
		volume=ms.polyArea();
		System.out.println(volume +" " + ms.polyArea());

		double mu20=centralmoments[2][0]/volume; // Ixx/vol
		double mu02=centralmoments[2][3]/volume; // Iyy/vol	
		double mu11=centralmoments[2][1]/volume; // Ixy/vol
		System.out.println("moments xx "+centralmoments[2][0]+ " yy " + centralmoments[2][3] +"  xy " + centralmoments[2][1]);
		//System.out.println("moments "+mu20+ " " + mu02 +" " + mu11);
			cov[0][0]=mu20;
			cov[1][1]=mu02;
			
			cov[1][0]=mu11;
			cov[0][1]=mu11;
		 		
			double trace=cov[0][0]+cov[1][1];
			//System.out.println("trace: "+trace);
			double det=cov[0][0]*cov[1][1]-cov[1][0]*cov[0][1];
			//System.out.println("det: "+det);
			
			theta=0.5*Math.atan2(2*mu11,(mu20-mu02));
			theta+=Math.PI/2;
					
			//if (theta<0) theta=Math.PI/2.0+theta;
			
			//double  eccentricity = (Math.pow((mu20-mu02),2.0)+(4.0*mu11*mu11))/volume;
			Log("C[1][1]: "+  cov[0][0]);
			Log("C[1][2]: "+  cov[0][1]);
			Log("C[2][2]: "+  cov[1][1]);
			
			//IJ.log("orienation: theta (rad): "+theta + " theta (deg): "+theta*180/Math.PI);

			
			//double D=Math.sqrt(mu11+Math.pow(mu20-mu02,2)/4);
			double D=Math.sqrt(trace*trace - 4*det);
			//Log("sqrtD " + D);
			//System.out.println("sqrtD "+D);
			
			//System.out.println("diff "+(mu20-mu02));
	
			if (mu20>mu02) {
				lambda1=0.5*(trace-D);
				lambda2=0.5*(trace+D);
				 
			}
			else {
				lambda2=0.5*(trace-D);
				lambda1=0.5*(trace+D);
				System.out.println("toggle 1");
				theta+=Math.PI/2;
			}
			
			System.out.println("lambda1: "+ lambda1+ " lambda2: "+ lambda2);
			eccentricity =Math.sqrt(1-Math.min(lambda1,lambda2)/ Math.max(lambda1,lambda2));
			 
			double k=Math.sqrt(4*Math.abs(volume)/Math.PI /Math.sqrt(Math.abs(det))); //already divided by volume
			k=Math.sqrt(k);
			formfactor=k;		 
			//IJ.write("k\t"+ k);
	 
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.setProperty("plugins.dir", args[0]);
			new ImageJ();
		}
		catch (Exception ex) {
			Log("plugins.dir misspecified");
		}
	
  
	 
	}

	/* (non-Javadoc)
	 * @see ij.plugin.filter.PlugInFilter# (java.lang.String, ij.ImagePlus)
	 */
	//@Override
	public int setup(String arg, ImagePlus imp) {
		this.imp=imp;
       
        if (arg.equals("about")){
            showAbout();
            return DONE;
        }
        try {
        	isProcessibleRoi=processibleRoi(imp);
        } catch (NullPointerException ex) { 
        	return DONE;
        }
        
        if(IJ.versionLessThan("1.44")) {
            return DONE;
        }
        else {
            return DOES_ALL + ROI_REQUIRED;
        }
	} //
 

	
	public void rotateRoi (double theta, Roi roi) {
		Polygon p =roi.getPolygon();
		Rectangle r=p.getBounds();
		
		double centerx= (r.x+r.width/2.0);
		double centery= (r.y+r.height/2.0);
		
		/*
		AffineTransform at=new AffineTransform();
		at.translate(centerx, centery);
		at.rotate(theta);
		at.translate(-r.x -r.width/2.0 , -r.y -r.height/2.0);
			
		ImageCanvas canvas = imp.getCanvas();
		*/
		int[] xpoints=p.xpoints;
		int[] ypoints=p.ypoints;
		
		/*
		
		if (canvas==null) return;
		else {
			GeneralPath path = new GeneralPath();
			Shape shape= at.createTransformedShape(p);
	 		path.append(shape, false);
	 		canvas.setDisplayList(path,Color.red, new BasicStroke(lineWidth));
			
		}
		*/
	
		for (int i=0; i<p.npoints; i++) {
			final double dx=xpoints[i]-centerx;
			final double dy=ypoints[i]-centery;
			final double rho=Math.sqrt(dx*dx+dy*dy);
			double a=Math.atan2(dy, dx);
			xpoints[i]= (int)(centerx+rho*Math.cos(-a-theta));
			ypoints[i]= (int)(centery-rho*Math.sin(-a-theta));
		}
		
		roi=new PolygonRoi ( xpoints, ypoints, p.npoints, Roi.FREEROI);
		imp.setRoi(roi, true);
	}
	
	 
	
	/* general support for debug variables 
     */
     private static boolean debug=IJ.debugMode;

     
     public static void Log(String astr) {
     	if (debug) IJ.log(astr);
     }
     
     
     public boolean processibleRoi(ImagePlus imp) {
    	   	roi = imp.getRoi();
    	       boolean ret=(roi!=null && !(roi.getType()==Roi.LINE || 
    	       						 roi.getType()==Roi.POLYLINE ||
    	       						 roi.getType()==Roi.ANGLE ||
    	       						 roi.getType()==Roi.FREELINE 
    	       						 	       						 )
    	       		   );
    	       //Log("roi ret "+ ret);
    	       return ret;

    }
     
     public void showAbout() {
         IJ.showMessage("About Image Rotator...",
         ""
         );
     }
     
     /*------------------------------------------------------------------*/
     boolean showDialog(ImagePlus imp)   {
         
         if (imp==null) return true;
         GenericDialog gd=new GenericDialog("Parameters");
         
         // Dialog box for user input
         gd.addMessage("This plugin calculates image orienation\n");
        // gd.addCheckbox("Non destructive", ovtype);
        
        // gd.addCheckbox("Continuity corrections?", corrections);
     	 //gd.addChoice("Draw:", drawings, drawings[drawType]);
         gd.addNumericField("Line width:", lineWidth, 0, 2, "pixels");
         gd.setResizable(false);

         gd.showDialog();
         
         // input handling
     
   
         Line.setWidth((int)gd.getNextNumber());
	     lineWidth = Line.getWidth();
      
	    	  
         if (gd.wasCanceled())
             return false;
         
        
         return true;
     } /* showDialog */
     
     
     /*------------------------------------------------------------------*/
     /* Saves the current settings of the plugin for further use
      * 
      *
     * @param prefs
     */
    public static void savePreferences(Properties prefs) {

        prefs.put(LINW, Integer.toString(lineWidth));
    }
     
 ///////////////////////////////////////////////////////////////
  
 
  private class SimpleShapeStatistics {
	  double volume=0;
		//double area=-1;
		double xbar=0;
		double ybar=0;
		double [] centroid=new double[2];
		
		double[][] rawmoments;
		double[][] centralmoments;
		
		int npoints=0;
		double [] x;
		double[] y;
		
		boolean moment0calculated=false;
		boolean moment1calculated=false;
		
		public SimpleShapeStatistics (  OvalRoi roi){
			this ( roi.getPolygon());
		}
		
		public double getVolume() {
 			return volume;
		}

		public SimpleShapeStatistics ( Polygon poli){
		 
			int n=poli.npoints;
			System.out.println("n="+n);
			  x=new double[n+1];
			  y=new double[n+1];
			
			npoints=n+1;
			for (int i=0; i<n; i++){ 
				x[i]=(double)poli.xpoints[i];
				y[i]=(double)poli.ypoints[i];
			}
			
			x[n]=poli.xpoints[0];
			y[n]=poli.ypoints[0];
			
			rawmoments=new double[3][];
			centralmoments=new double[3][];
			
		}
	  
		public double[][] getCentralMoments() {
			return centralmoments;
		}

		public double[][] getRawMoments() {
			return rawmoments;
		}
		public double[] getCentroid() {
			return centroid;
		}
		
		public void calculateMoments(){
			 polyArea();
			 calcM1();
			 calcM2();
				
		}
		
		public double polyArea () {
 			double a=0;
			for (int i=1; i<npoints; i++) {
				//System.out.println(i+" x["+i +"]=" +x[i]+ 
				//		" y["+i+ " ]=" + y[i]);
				a+=x[i-1]*y[i]- x[i]*y[i-1];
			}
			 

			rawmoments[0]=new double[1];
			centralmoments[0]=new double[1];
			rawmoments[0][0]= a/2;
			centralmoments[0][0]= a/2;
			volume=a/2;
			moment0calculated=true;
			return a/2;
		}
		
		public void calcM1() {
			double sum1=0, sum2=0;
			for (int i=1; i<npoints; i++) {
				final double d=(x[i-1]*y[i]- x[i]*y[i-1]);
				sum1+=(x[i-1]+x[i])*d;
				sum2+=(y[i-1]+y[i])*d;
			}
			
			rawmoments[1]=new double[2];
			rawmoments[1][0]=sum1/6; //6
			rawmoments[1][1]=sum2/6; //6
			
			double a=volume;
			if (! moment0calculated)
					a= polyArea (); 
			xbar=Math.abs(rawmoments[1][0]/a);
			ybar=Math.abs(rawmoments[1][1]/a);
			centroid[0]=xbar;
			centroid[1]=ybar;
			moment1calculated=true;
			
		}
		
		
		public void calcM2() {
			double sum1=0, sum2=0, sum3=0;
			for (int i=1; i<npoints; i++) {
				final double d=(x[i-1]*y[i]- x[i]*y[i-1]);
				sum1+=(x[i-1]*x[i-1]+ x[i]*x[i-1] + x[i]*x[i] )*d;
				sum2+=(y[i-1]*y[i-1]+ y[i]*y[i-1] + y[i]*y[i] )*d;
				sum3+=(2*x[i-1]*y[i-1] + x[i-1]*y[i]+ x[i]*y[i-1] +2*x[i]*y[i])*d;
			}
			
			rawmoments[2]=new double[4];
			rawmoments[2][0]=sum1/12;
			rawmoments[2][1]=sum3/24;
			rawmoments[2][2]=rawmoments[2][1];
			rawmoments[2][3]=sum2/12;
		
			double a=volume;
			if (! moment0calculated)
					a= polyArea (); 
			
			centralmoments[2]=new double[4];
			centralmoments[2][0]= rawmoments[2][0] - xbar*rawmoments[1][0];
			centralmoments[2][1]= rawmoments[2][1] - xbar*ybar*a;
			centralmoments[2][2]= centralmoments[2][1];
			centralmoments[2][3]= rawmoments[2][3] - ybar*rawmoments[1][1];
			
		}
	  
  }

}
