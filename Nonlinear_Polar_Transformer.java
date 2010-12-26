/*   
*	 This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import ij.*;
import ij.process.*;
import ij.plugin.*;
import java.util.*;						// properties object
import java.io.FileInputStream;			// read properties file
import java.io.FileOutputStream;		// write properties file
import java.io.IOException;				// read/write properties IO exceptions
import java.lang.*;
import java.awt.*;
import java.awt.image.*;
import java.text.DecimalFormat;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.gui.*;


/*
*	date				22-12-2010
*	author				Jan Bonne Aans
*	contact				j.b.aans@erasmusmc.nl
*	institute			Center for Optical Diagnostics and Therapy at the Erasmus Medical Center
*	version				1.0
*	Short description: 	Parametric angular and radial polynomial transformator
*	Written for: 		ImageJ, Fiji
*	Files:				Nonlinear_Polar_Transformer.java
*						Polynomial.java
*	Intended use: 		Correct X-ray image radial and angular distortions originated in image intensifiers of the type with electron accellerators
*	Long description: 	This plug-in allows for center-symmetrical radial and angular transformation of an image e.g. the radius r = SQRT(x^2 + y^2) 
*						as well as the the angle theta = arctan(y / x) of each pixel can be changed with respect to a center pixel.
*						Both radial and angular transformation are defined by N order polynomials (you could fit an elephant with a 5th order polynomial).
*						The center of transformation can be defined by the user as well.
*	Function scheme:	For each pixel:
*							Transform Cartesian coordinates to Polar coordinates
*							Calculate new polar coordinates using the defined center coordinates and transformation polynomials for r and theta
*							Transform new polar coordinates back to Cartesian coordinates
*							Place pixel at new Cartesian coordinates using interpolation
*						The plug-in produces a 16-bit grayscale image for 8 or 16-bit grayscale inputs, and an RGB color image
*						for RGB inputs.
*	Hints:				See Nonlinear_Polar_Transformer.properties for default settings
*						Works with Tudor DICOM plugin
*	Acknowledgments:	This plug-in borrows code from the Polar Transform plug-in written by Edwin F. Donnelly
*						This plug-in uses the (modified version of) Polynomial class by "O.C. and R.P"
*	Todo:				Improve interpolation (e.g. bicubic)
*						Improve DICOM handling (copy headers instead of used workaround)				
*
*/

/*
* Class constructor
*/
public class Nonlinear_Polar_Transformer implements PlugIn {

	// global definitions
	final static int POLYNORDERRADIUS = 3;	// default polynomial order for radius
	final static int POLYNORDERANGLE = 3;	// default polynomial order for angle
	final static String CONFIGFILE = "Nonlinear_Polar_Transformer.properties";
	
	/*
	* Class holds parameters for transformation
	*/
	public class TransformParams{
		// holds transformation variables
		public Polynomial radialPoly;	// radial transformation polynomial
		public Polynomial angularPoly;	// angular transformation polynomial
		public double dblCenterX;		// center X coordinate
		public double dblCenterY;		// center Y coordinate
		public boolean defaultCenter;	// use center coordinates
		public boolean boolSet = false;	// parameters set flag
	}
	
	
	

	/*
	* Class holds a cartesian coordinate
	*/
	public class Cartesian2D {
		// coordinates
		public double x,y;
		
		// constructor
		public Cartesian2D(){
		}
		
		// constructor
		public Cartesian2D(double x, double y){
			this.x = x;
			this.y = y;
		}
		
		// return coordinate in polar values
		public Polar2D toPolar(){
			return new Polar2D( Math.sqrt(x*x + y*y) , Math.atan2(y, x) );
		}
		
		// normalizes this coordinate with respect to center by using the x,yMax values
		public void Normalize(Cartesian2D pxCenter, double xMax, double yMax){
			x = (x - pxCenter.x) / xMax;
			y = (pxCenter.y - y) / yMax;
		}
		
		// turns this coordinate into a denormalized coordinate
		public void Denormalize(Cartesian2D pxCenter, double xMax, double yMax){
			x = (x * xMax) + pxCenter.x;
			y = pxCenter.y - (y * yMax);
		}
	}
	
	/*
	* Class holds a polar coordinate
	*/
	public class Polar2D {

		// coordinates
		public double r, theta;
		
		// constructor
		public Polar2D(){
		}
		
		// constructor	
		public Polar2D(double r, double theta){
			this.r = r;
			this.theta = theta;
		}
		
		// set radius of this coordinate
		public void setR(double r){
			this.r = r;
		}

		// set angle of this coordinate
		public void setTheta(double theta){
			this.theta = theta;
		}

		// return coordinate in cartesian values
		public Cartesian2D toCartesian(){
			return new Cartesian2D( r * Math.cos(theta) , r * Math.sin(theta) );
		}
	}	

	

	
	public void run(String arg)	{
		// create image references
		// need to use original image reference since ImageJ doesn't 
		// handle DICOM headers when recreating an imageplus
		ImageProcessor ip;
		ImagePlus imp = WindowManager.getCurrentImage();
		if(imp == null) {
			IJ.noImage();
			return;
		}
		ip = imp.getProcessor();

		// get user input
		TransformParams userParams = GetUserParams(POLYNORDERRADIUS,POLYNORDERANGLE);
		if (!userParams.boolSet) return;

		// create transformed image
		ip = paramTransform(ip,userParams);
		imp.setProcessor( ip );
		imp.setTitle( "Nonlinear Polar Transformation of "+imp.getTitle() );
		imp.setCalibration (imp.getCalibration());
		imp.show();
	
	}
	
	
	/* 
	*	perform polynomial transformation of radius and angle on image
	*
	*	@param	ipOriginal	Original image
	*	@param	userParams	Transformation parameters given by user
	*	@return				Transformed image
	*/
	public ImageProcessor paramTransform(ImageProcessor ipOriginal, TransformParams userParams)	{
		ImageProcessor ipTransformed;
		Cartesian2D pxCenter;
		
		// get image dimensions
		int intWidth = ipOriginal.getWidth();
		int intHeight = ipOriginal.getHeight();

		// determine the centre coordinate to be used for the transformation
		// coordinate is with relative to left top.
		if(userParams.defaultCenter) pxCenter = new Cartesian2D( intWidth/2 , intHeight/2 );
		else pxCenter =  new Cartesian2D( userParams.dblCenterX , userParams.dblCenterY );
		// determine the maximum values for coordinates relative to the centre coordinate
		double xMax = Math.abs( pxCenter.x - (0.5 * intWidth) ) + (0.5 * intWidth);
		double yMax = Math.abs( pxCenter.y - (0.5 * intHeight) ) + (0.5 * intHeight);		
		
		// create the new image
		if (ipOriginal instanceof ColorProcessor) ipTransformed = new ColorProcessor(intWidth, intHeight);
		else ipTransformed = new ShortProcessor(intWidth, intWidth);
		
		// process each pixel in the original image, line-by-line and top-down.
		IJ.showStatus("Calculating...");
		for (int y = 0; y < intHeight; y++) {
			for (int x = 0; x < intWidth; x++) {
				// create coordinate
				Cartesian2D pxCart = new Cartesian2D(x,y);
				pxCart.Normalize(pxCenter, xMax, yMax);			// normalized
				Polar2D pxPolar = pxCart.toPolar();				// polar form
				
				pxPolar.setR( pxPolar.r * userParams.radialPoly.eval(pxPolar.r) );			// calculate new r, changes with radius
				pxPolar.setTheta( pxPolar.theta + userParams.angularPoly.eval(pxPolar.r) );	// calculate new theta, changes with radius
				
				pxCart = pxPolar.toCartesian();					// cartesian form
				pxCart.Denormalize(pxCenter, xMax, yMax);		// denormalized
				
				//put pixel in the new image
				if (ipOriginal instanceof ColorProcessor) ipTransformed.putPixel(x,y,getInterpolatedColourPixel(ipOriginal, pxCart.x, pxCart.y));
				else ipTransformed.putPixelValue(x,y,ipOriginal.getInterpolatedPixel(pxCart.x,pxCart.y));

			}
			IJ.showProgress(y, intHeight);
		}
		IJ.showProgress(1.0);
		
		// apply options of original image to new image
		ipTransformed.setMinAndMax(ipOriginal.getMin(), ipOriginal.getMax() );
		ipTransformed.setCalibrationTable(ipOriginal.getCalibrationTable() );
		return ipTransformed;
	}
	
	
	/*
	*	show a dialog to acquire transformation parameters from user
	*
	*	@param	nOrderA	The order of the transformation polynomial for the radius
	*	@param	nOrderB	The order of the transformation polynomial for the angle
	*	@return			Transformation parameters given by user
	*/
	private TransformParams GetUserParams(int nOrderA, int nOrderB) {	
		// Read previously used parameters from file
		FileInputStream myInputStream = null;
        FileOutputStream myOutputStream = null;
		Properties myProperties = new Properties();				// create properties object
		TransformParams userParams = new TransformParams();		// create parameter object
		final int lowestInputCoeffA = 1;						// 0-th coefficient will be calculated
		final int lowestInputCoeffB = 0;
		
		/**********************************
		*		read properties from file
		*/
		try {
			myInputStream = new FileInputStream(CONFIGFILE);
			myProperties.load(myInputStream);
		} catch(IOException e) {}
		finally {
			// close file stream
			try{
				if (myInputStream != null) myInputStream.close();
			} catch(IOException e) {}
        }

		// get default values
		nOrderA = StringToInt(myProperties.getProperty("POLYNORDERRADIUS",""+nOrderA)); 	// with default value for when key not found
		nOrderB = StringToInt(myProperties.getProperty("POLYNORDERANGLE",""+nOrderB));
		int highestInputCoeffA = nOrderA;
		int highestInputCoeffB = nOrderB;
		userParams.radialPoly = new Polynomial(nOrderA);
		userParams.angularPoly = new Polynomial(nOrderB);
		userParams.dblCenterX = StringToDouble(myProperties.getProperty("CENTREX","512.0"));
		userParams.dblCenterY = StringToDouble(myProperties.getProperty("CENTREY","512.0"));
		userParams.defaultCenter = Boolean.valueOf(myProperties.getProperty("defaultCenter","true"));
		myProperties.getProperty("A[0]","0");
		StringToDouble(myProperties.getProperty("A[0]","0"));
		for (int i = highestInputCoeffA; i >= lowestInputCoeffA; i--) userParams.radialPoly.set(i, StringToDouble(myProperties.getProperty("A["+i+"]","0")) );
		for (int i = highestInputCoeffB; i >= lowestInputCoeffB; i--) userParams.angularPoly.set(i, StringToDouble(myProperties.getProperty("B["+i+"]","0")) );
		
		/**********************************
		*		display dialog
		*/
	
		// create generic dialog
		GenericDialog settingsDialog = new GenericDialog("Nonlinear Polar Transformation settings");
		// transformation center
		settingsDialog.addMessage("Transformation will be with respect to a center coordinate\n");
		settingsDialog.addNumericField("Transformation center x:", userParams.dblCenterX,1, 10, "px");
		settingsDialog.addNumericField("Transformation center y:", userParams.dblCenterY,1, 10, "px");
		settingsDialog.addCheckbox("Don't use these coordinates, use center of image instead", userParams.defaultCenter);
		// radial transformation polynomial
		settingsDialog.addMessage("Pixel radius will be transformed using a polynomial of order "+highestInputCoeffA+"\n"+
								  "in the form of R = r * ("+getPolynomialString("r","a",0,highestInputCoeffA)+")\n"+
								  "where a[0] will be calculated automatically to retain image size");
		for (int i = highestInputCoeffA; i >= lowestInputCoeffA; i--) settingsDialog.addStringField("Coefficient a["+(i)+"]:", ""+userParams.radialPoly.get(i),10);
		
		// angular transformation polynomial
		settingsDialog.addMessage("Pixel angle will be transformed using a polynomial of order "+highestInputCoeffB+"\n"+
								  "in the form of \u0398 = \u03D1 - ("+getPolynomialString("r","b",0,highestInputCoeffB)+")\n"+
								  "where b[0] defines rotation of the whole image in radians");
		for (int i = highestInputCoeffB; i >= lowestInputCoeffB; i--) settingsDialog.addStringField("Coefficient b["+(i)+"]:", ""+userParams.angularPoly.get(i),10);
		
		// show dialog
		settingsDialog.showDialog();
		if (settingsDialog.wasCanceled()) 	return userParams;
		
		// get user input values
		userParams.dblCenterX = (double) settingsDialog.getNextNumber();		// transform center x
		userParams.dblCenterY = (double) settingsDialog.getNextNumber();		// transform center y
		userParams.defaultCenter = settingsDialog.getNextBoolean();				// use default (image) center
		for (int i = highestInputCoeffA; i >= lowestInputCoeffA; i--) userParams.radialPoly.set( i, StringToDouble(settingsDialog.getNextString()) );
		for (int i = highestInputCoeffB; i >= lowestInputCoeffB; i--) userParams.angularPoly.set(i, StringToDouble(settingsDialog.getNextString()) );
		
		// set first and zeroeth radial coefficients
		userParams.radialPoly = setRadialCoefficient0(userParams.radialPoly);
		
		/**********************************
		*		write properties to file
		*/
		
		// set properties
		myProperties.setProperty("POLYNORDERRADIUS",""+nOrderA);
		myProperties.setProperty("POLYNORDERANGLE",""+nOrderB);
		myProperties.setProperty("CENTREX",""+userParams.dblCenterX);
		myProperties.setProperty("CENTREY",""+userParams.dblCenterY);
		myProperties.setProperty("defaultCenter",""+userParams.defaultCenter);
		for (int i = highestInputCoeffA; i >= lowestInputCoeffA; i--) myProperties.setProperty("A["+i+"]",""+userParams.radialPoly.get(i) );
		for (int i = highestInputCoeffB; i >= lowestInputCoeffB; i--) myProperties.setProperty("B["+i+"]",""+userParams.angularPoly.get(i) );

		// write properties
        try {
			myOutputStream = new FileOutputStream(CONFIGFILE);
			myProperties.store(myOutputStream, "Config file for Nonlinear Polar Transformation plugin for ImageJ / Fiji");
		} catch(IOException e) {}
		finally {
            // close file stream
			try{
				if (myOutputStream != null) myOutputStream.close();
			} catch(IOException e) {}
        }
		
		userParams.boolSet = true;
		return userParams;
	}


	/*
	*	Return n-th order polynomial function string
	*
	*	@param	strVariable		The variable name, e.g. 'x'
	*	@param	strCoefficient	The coefficient name, e.g. 'c'
	*	@param	n				Lowest order term to print, n => 1
	*	@param	N				Highest order term to print
	*	@return					The n-th order polynomial function string
	*/	
	private String getPolynomialString(String strVariable, String strCoefficient, int n, int N) {
		String strPolynomial = "";
		for(int i = N; (i >= n) & (i > 1); i--) {
			if(i < N) strPolynomial += " + ";
			strPolynomial += strCoefficient + "[" + i + "] * " + strVariable+"^" + i; 
		}
		if (n<=1) strPolynomial += " + " + strCoefficient + "[1] * " + strVariable;
		if (n==0) strPolynomial += " + " + strCoefficient + "[0]";
		return strPolynomial;
	}
	
	

	
	/* 
	* set zeroeth radial coefficient
	*/
	private Polynomial setRadialCoefficient0(Polynomial p){
		double SUM = 0;
		for(int i = p.deg(); i > 0; i--) SUM += p.get(i);
		p.set(0, (1 - SUM) );
		return p;
	}

	

	/*
	*	Calculates the interpolated colour value of the pixel
	*
	*	@param	ipImage	The Image to process
	*	@param	x		The x pixel coordinate
	*	@param	y		The y pixel coordinate
	*	@return			The RGB array containing the interpolated colour value
	*/	
	private int [] getInterpolatedColourPixel(ImageProcessor ipImage, double x, double y) {
		int [] rgbArray = new int[3];
		int [] xLyL = new int[3];
		int [] xLyH = new int[3];
		int [] xHyL = new int[3];
		int [] xHyH = new int[3];
		int xL, yL;
		
		xL = (int)Math.floor(x);
		yL = (int)Math.floor(y);
		xLyL = ipImage.getPixel(xL, yL, xLyL);
		xLyH = ipImage.getPixel(xL, yL+1, xLyH);
		xHyL = ipImage.getPixel(xL+1, yL, xHyL);
		xHyH = ipImage.getPixel(xL+1, yL+1, xHyH);
		for (int rr = 0; rr<3; rr++) {
			double newValue = (xL+1-x)*(yL+1-y)*xLyL[rr];
			newValue += (x-xL)*(yL+1-y)*xHyL[rr];
			newValue += (xL+1-x)*(y-yL)*xLyH[rr];
			newValue += (x-xL)*(y-yL)*xHyH[rr];
			rgbArray[rr] = (int)newValue;
		}
		
		return rgbArray;
	}


	/*
	*	returns double value of string
	*
	*	@param	s		The string to process
	*	@return			Double value
	*/
	private double StringToDouble(String s){
		double d = 0;
		try {
			d = Double.valueOf(s.trim()).doubleValue();
		} catch (NumberFormatException nfe) {
			IJ.showMessage("NumberFormatException: " + nfe.getMessage());
		}
		return d;
	}
	
	/*
	*	returns double value of string
	*
	*	@param	s		The string to process
	*	@return			Integer value
	*/
	private int StringToInt(String s){
		int i = 0;
		try {
			i = Integer.valueOf(s.trim()).intValue();
		} catch (NumberFormatException nfe) {
			IJ.showMessage("NumberFormatException: " + nfe.getMessage());
		}
		return i;
	}
	
	/**
	 * This class represents a Polynom with real coefficients. The storage
	 * of coefficients is full storage. All coefficients are stored.
	 * Coefficients are stored from left to right with increasing
	 * degree. This mean than mCoeff[0] is the constant of the polynom ie
	 * If p is a polynom p(0) = mCoeff[0]
	 *
	 * @author O.C. and R.P.
	 * @date 05/2003
	 * @since Opale-Mathtools 0.13
	 * @see <code>PolynomModel</code>
	 */
	public final class Polynomial implements Cloneable
	{
	
		private int mDeg;	//the degree
		private double[] mCoeff;	// the coeff
	
		/**
		 * Constructs a n-degree polynom initialized to zero
		 * for all coefficients except then n-degree one..
		 * @param n the degree
		 */
		public Polynomial(int n)
		{
			if (n<0)
			{
				throw new IllegalArgumentException("n must be a positive or null number !!");
			}
			mDeg = n;
			mCoeff = new double[mDeg+1];
			mCoeff[mDeg] = 1.0;
		}
	
		/**
		 * Constructs a n-degree polynom with the coefficients in the array tab.
		 * Note : tab[i] = mCoeff[i] and
		 * Degree's Polynomial is tab.length - 1
		 * @param tab the array of coefficients
		*/
		public Polynomial(double[] tab)
		{
			this(tab.length - 1);
			for(int i =0; i <=mDeg; i++)
			{
				mCoeff[i] = tab[i];
			}
			if (tab[mDeg] == 0)
			{
				throw new IllegalArgumentException("Not a valid n-degree polynomial. The n-degree coefficient must be different from zero");
			}
		}
	
		/**
		 * Returns the i-th coefficient of this Polynom.
		 * @param  i index of the coefficient to get
		 * @return double, the value of the coefficient
		 */
		public double get(int i)
		{
			return mCoeff[i];
		}
	
		/**
		 * Sets the i-th coefficient  with the given value x
		 * @param i the coefficient to set
		 * @param x the value
		 */
		public void set(int i, double x)
		{
			mCoeff[i] = x;
	//		if ( i == mDeg && Precision.isEqual(x,0))
	//		{
	//			mDeg -= 1;
	//		}
		}
	
		/**
		 * Return the degree.
		 * @return int, the degree
		 */
		public int deg()
		{
			return mDeg;
		}
	
		/**
		 * Evaluates the polynom at the given point x using Horner's method.
		 * @param  x the point x
		 * @return the evaluation of the polynom
		 */
		public double eval(double x)
		{
			double y=mCoeff[mDeg];
			for(int i=mDeg-1; i>=0; i--)
			{
				y=y*x+mCoeff[i];
			}
			/*int j; 	// second version
				double y=coeff[j= deg];
				while (j>0)	y=y*x+coeff[--j];*/
			return y;
		}
	
		/**
		 * Returns a string representation of the Polynomial
		 * @return String, a string representation of the object.
		 * @since Opale-Mathtools 0.13
		 */
		public String toString()
		{
			StringBuffer s = new StringBuffer("P(X) = ");
			s.append(Double.toString(get(mDeg)));
			s.append("X^");
			s.append(Integer.toString(mDeg));
	
			for(int i=mDeg-1; i >0; i--)
			{
				if (get(i) < 0 )
				{
					s.append(" - ");
					s.append(Double.toString(-get(i)));
				}
				else
				{
					s.append(" + ");
					s.append(Double.toString(get(i)));
				}
				s.append("X^"+i);
			}
			if (get(0) < 0)
			{
					s.append(" - ");
					s.append(Double.toString(-get(0)));
			}
			else
			{
					s.append( " + ");
					s.append(Double.toString(get(0)));
			}
			return s.toString();
		}
	
		/**
		 * Determines whether or not two Polynomial are equals.
		 * @param obj an object to be compared with this Polynomial
		 * @return <code>true </code> if the object to be compared is an
		 * instance of Polynomial and has the degree and the same values; false otherwise.
		 * @since Opale mathtools 0.20
		 */
		public boolean equals(Object obj)
		{
			if (obj instanceof Polynomial)
			{
				Polynomial p = (Polynomial) obj;
				if (p.mDeg == this.mDeg)
				{
					for (int i = 0 ; i <= mDeg; i++)
					{
						//if (Precision.isDifferent(this.mCoeff[i], p.mCoeff[i]))
						if ( this.mCoeff[i] != p.mCoeff[i]  )
						{
							return false;
						}
					}
					return true;
				}
				return false;
			}
			return false;
		}
	
		/**
			* Creates a new object of the same class and with the same contents as this object
			* @return a clone of this instance.
			* @see        java.lang.Cloneable
			* @since Opale mathtools 0.20
			*/
		public Object clone()
		{
			try
			{
				Polynomial p = (Polynomial) super.clone();
				p.mDeg = mDeg;
				p.mCoeff = new double[mDeg+1];
				for (int i = 0; i <= mDeg; i++)
				{
					p.mCoeff[i] = mCoeff[i];
				}
				return p;
			}
			catch (CloneNotSupportedException e) // ne devrait jamais arriver
			{
				throw new InternalError();
			}
		}
	
		/*public static void main(String[] arfg)
		{
		Polynomial p = new Polynomial(2);
		p.set(0,1);
		p.set(1,-2);
		p.set(2,-4);
		System.out.println(p.deg());
		System.out.println(p);
		System.out.println(p.eval(2));
		}*/
	}

}
