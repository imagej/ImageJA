import java.awt.*;
import java.awt.event.*;
import java.awt.SystemColor;
import java.io.*;
import java.lang.*;
import ij.plugin.frame.*;
import ij.*;
import ij.util.*;
import ij.process.*;
import ij.gui.*;

/**
 *	This plugin simplifies the task of measuring the Modulation Transfer
 *	Function (resolution) of an optical system, given an image of variously
 *	sized bar patterns. <p>
 *	This dialog-box-like plugin is based on the PlugInFrame class,
 *	and could serve as an example of how to automate repeated
 *	measurements.<p>
 *
 *	Reference:<p>
 *    Sitter, D.N., Goddard, J.S., and Ferrell, R.K., (1995), "Method for the
 *    measurement of the modulation transfer function of sampled imaging systems
 *    from bar-target patterns.", Applied Optics, v. 34 n. 4, pp. 746-751.
 *
 *	@author	Jeffrey Kuhn
 *	@author	The University of Texas at Austin
 *	@author	jkuhn@ccwf.cc.utexas.edu
 */
public class MeasureMTF_ extends PlugInFrame implements ActionListener {

	/* 
	 *	User interface fields
	 */
	Panel		panel;
	TextField	textMeasuredLength;
	Button		buttonRetrieveLength;
	TextField	textKnownLength;
	Label		labelScale;
	Label		labelWhiteLevel;
	Button		buttonMeasureWhite;
	Label		labelBlackLevel;
	Button		buttonMeasureBlack;
	TextField	textCyclesToMeasure;
	Choice		choiceBarSpacing;
	TextField	textDutyCycle;
	TextField	textLinesToAverage;
	Label		labelMeasurementSize;
	Checkbox	checkVertical;
	Checkbox	checkFirstTopLeft;
	Checkbox	checkPlotDFT;
	Button		buttonRecalculate;
	Button		buttonCreateRoi;
	Button		buttonMeasureMtf;
	Button		buttonClearMtf;
	Button		buttonFirstBar;
	Button		buttonNextBar;

	/*
	 *	calculation fields 
	 */

	/** number of DFT harmonics to plot */
	static final int nHARMONICS = 15;		

	/** distance between each grating in um (micrometers) */
	static final double dBARLENGTH = 5;		

	/** measured length in pixels */
	static double dMeasuredLength = 0;

	/** known length in um (micrometers) */
	double	dKnownLength = 20;				

	/** calculated scale in pix/um */
	double	dScale = 0;						

	/** measured white bar intensity */
	double	dWhiteLevel = -1;				

	/** measured black bar intensity */
	double	dBlackLevel = -1;				

	/** number of cycles to measure */
	int		iCyclesToMeasure = 5;			

	/** distance between cycles */
	double	dBarSpacing;					

	/** length of white portion/total spacing 
	 *	(i.e. if white=1/4 width and black=3/4 width, Duty Cycle=0.25)  */
	double	dDutyCycle = 0.5;				

	/** how many lines of pixel data to average for each bar profile */
	int		iLinesToAverage = 50;			

	/** total length of profile to measure */
	int		iMeasurementSize = 0;			

	/** measure vertically? */
	boolean	bVertical = false;				

	/** first bar is at the top or on the left */
	boolean bFirstTopLeft = true;			

	/** plot the DFT result? */
	boolean bPlotDFT = true;				

	/** Have the column headings in the main measurement window been created? */
	static boolean bMtfHeadingsCreated = false;
	static boolean bHasMtfData = false;

	/**
	 * Constructor. Creates the main user interface panel.
	 */
	public MeasureMTF_() {
		super("Measure MTF");
		setBackground(SystemColor.control);

		setLayout(new BorderLayout());
		panel = new Panel();
		panel.setLayout(new GridLayout(14, 3, 2, 2));
		panel.setBackground(SystemColor.control);

		//-------------------NEXT ROW-----------------------
		panel.add(new Label("Measured Length:"));

		textMeasuredLength = new TextField(Double.toString(dMeasuredLength));
		panel.add(textMeasuredLength);

		buttonRetrieveLength = new Button("Retrieve");
		buttonRetrieveLength.addActionListener(this);
		panel.add(buttonRetrieveLength);

		//-------------------NEXT ROW-----------------------
		panel.add(new Label("Known Length:"));

		textKnownLength = new TextField(Double.toString(dKnownLength));
		panel.add(textKnownLength);

		panel.add(new Label("um"));

		//-------------------NEXT ROW-----------------------
		panel.add(new Label("Scale:"));

		labelScale = new Label("unknown");
		panel.add(labelScale);

		panel.add(new Label("pix/um"));

		//-------------------NEXT ROW-----------------------
		panel.add(new Label("White Level:"));

		labelWhiteLevel = new Label("unknown");
		panel.add(labelWhiteLevel);

		buttonMeasureWhite = new Button("Measure White");
		buttonMeasureWhite.addActionListener(this);
		panel.add(buttonMeasureWhite);

		//-------------------NEXT ROW-----------------------
		panel.add(new Label("Black Level:"));

		labelBlackLevel = new Label("unknown");
		panel.add(labelBlackLevel);

		buttonMeasureBlack = new Button("Measure Black");
		buttonMeasureBlack.addActionListener(this);
		panel.add(buttonMeasureBlack);

		//-------------------NEXT ROW-----------------------
		panel.add(new Label("Cycles to Measure:"));

		textCyclesToMeasure = new TextField(Integer.toString(iCyclesToMeasure));
		panel.add(textCyclesToMeasure);

		panel.add(new Label());

		//-------------------NEXT ROW-----------------------
		panel.add(new Label("Bar Spacing:"));

		choiceBarSpacing = new Choice();
		choiceBarSpacing.add("4.000");
		choiceBarSpacing.add("2.000");
		choiceBarSpacing.add("1.000");
		choiceBarSpacing.add("0.500");
		choiceBarSpacing.add("0.250");
		choiceBarSpacing.add("0.200");
		panel.add(choiceBarSpacing);

		panel.add(new Label("um"));

		//-------------------NEXT ROW-----------------------
		panel.add(new Label("Duty Cycle:"));

		textDutyCycle = new TextField(Double.toString(dDutyCycle));
		panel.add(textDutyCycle);

		panel.add(new Label("white width/spacing"));
		
		//-------------------NEXT ROW-----------------------
		panel.add(new Label("Lines To Average:"));

		textLinesToAverage = new TextField(Integer.toString(iLinesToAverage));
		panel.add(textLinesToAverage);

		panel.add(new Label());
		
		//-------------------NEXT ROW-----------------------
		panel.add(new Label("Measurement Size:"));

		labelMeasurementSize = new Label("unknown");
		panel.add(labelMeasurementSize);

		panel.add(new Label("pix"));

		//-------------------NEXT ROW-----------------------
		panel.add(new Label());

		panel.add(new Label());

		panel.add(new Label());

		
		//-------------------NEXT ROW-----------------------
		checkVertical = new Checkbox("Vertical Bars", bVertical);
		panel.add(checkVertical);

		buttonCreateRoi = new Button("Create ROI");
		buttonCreateRoi.addActionListener(this);
		panel.add(buttonCreateRoi);

		buttonMeasureMtf = new Button("Measure MTF");
		buttonMeasureMtf.addActionListener(this);
		panel.add(buttonMeasureMtf);

		//-------------------NEXT ROW-----------------------
		checkFirstTopLeft = new Checkbox("First at top/left", bFirstTopLeft);
		panel.add(checkFirstTopLeft);

		buttonFirstBar = new Button("Goto First Bar");
		buttonFirstBar.addActionListener(this);
		panel.add(buttonFirstBar);

		buttonClearMtf = new Button("Clear MTF");
		buttonClearMtf.addActionListener(this);
		panel.add(buttonClearMtf);

		//-------------------NEXT ROW-----------------------
		checkPlotDFT = new Checkbox("Plot DFT", bPlotDFT);
		panel.add(checkPlotDFT);

		buttonNextBar = new Button("Goto Next Bar");
		buttonNextBar.addActionListener(this);
		panel.add(buttonNextBar);

		buttonRecalculate = new Button("Update Values");
		buttonRecalculate.addActionListener(this);
		panel.add(buttonRecalculate);

		add(panel, BorderLayout.CENTER);

		pack();
		show();
	}

	/**
	 * Handle button presses, etc.
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonRecalculate) {
			recalculate();
			return;
		}

		if (e.getSource() == buttonClearMtf) {
			if (bHasMtfData) {
				// ask the user if they really want to erase the data
				boolean okay = IJ.showMessageWithCancel(
						"Clear MTF",
						"There is MTF data in the measurement window.\n" +
						"are you sure that you want to clear it?.");
				if (okay) {
					clearMTF();
				}
			} else {
				clearMTF();
			}
			return;
		}

		// Everything from here, down requires an image to work with
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.error("There is no active image.");
			return;
		}

		if (e.getSource() == buttonRetrieveLength) {
			bVertical = checkVertical.getState();
			Rectangle r = imp.getProcessor().getRoi();
			if (bVertical) {
				dMeasuredLength = r.height;
			} else {
				dMeasuredLength = r.width;
			}
			textMeasuredLength.setText(Double.toString(dMeasuredLength));
			recalculate();
			return;
		}
		if (e.getSource() == buttonMeasureWhite) {
			ImageStatistics stat = imp.getStatistics();
			dWhiteLevel = stat.mean;
			int iInt = (int)dWhiteLevel;
			int iFrac = (int)((dWhiteLevel - iInt)*10);
			labelWhiteLevel.setText(Integer.toString(iInt)+"."+Integer.toString(iFrac));
			recalculate();
			return;
		}
		if (e.getSource() == buttonMeasureBlack) {
			ImageStatistics stat = imp.getStatistics();
			dBlackLevel = stat.mean;
			int iInt = (int)dBlackLevel;
			int iFrac = (int)((dBlackLevel - iInt)*10);
			labelBlackLevel.setText(Integer.toString(iInt)+"."+Integer.toString(iFrac));
			recalculate();
			return;
		}
		if (e.getSource() == buttonCreateRoi) {
			recalculate();
			createROI(imp);
			return;
		}

		if (e.getSource() == buttonFirstBar) {
			recalculate();
			int iCurrentBar = choiceBarSpacing.getSelectedIndex();
			double dDistDown = -iCurrentBar * dBARLENGTH;
			double dDistOver = 0;
			// Calculate how far we have to move back over
			for (int i=0; i<iCurrentBar; i++) {
				dDistOver -= 0.25 * parseDouble(choiceBarSpacing.getItem(i));
			}
			if (!bFirstTopLeft) {
				dDistDown = -dDistDown;
			}
			moveROI(imp, dDistDown, dDistOver);
			choiceBarSpacing.select(0);
			recalculate();
			createROI(imp);
			return;
		}

		if (e.getSource() == buttonNextBar) {
			recalculate();
			int iCurrentBar = choiceBarSpacing.getSelectedIndex();
			double dDistDown = dBARLENGTH;
			double dDistOver = 0.25 * dBarSpacing;
			if (!bFirstTopLeft) {
				dDistDown = -dDistDown;
			}
			moveROI(imp, dDistDown, dDistOver);
			choiceBarSpacing.select(iCurrentBar + 1);
			recalculate();
			createROI(imp);
			return;
		}

		if (e.getSource() == buttonMeasureMtf) {
			recalculate();
			// Check to make sure that the scale has been properly set
			if (dScale == 0.0) {
				IJ.error("The image scale has not been measured. Please\n"
					   + "measure a known width before proceeding with\n"
					   + "the MTF calculation.");
				return;
			}
			// Check to make sure that we have a properly sized ROI
			Rectangle r = imp.getProcessor().getRoi();
			int iW, iH;
			if (bVertical) {
				iW = iLinesToAverage;
				iH = iMeasurementSize;
			} else {
				iW = iMeasurementSize;
				iH = iLinesToAverage;
			}
			if ((iW != r.width) || (iH != r.height)) {
				IJ.error("The measurement area is not the right size.\n"
					   + "Be sure and press the \"Create ROI\" button\n"
					   + "and move it to the right location before\n"
					   + "proceeding with the MTF calculation.");
				return;
			}

			// Check to make sure that the black and white levels have been
			// measured
			if ((dWhiteLevel < 0) || (dBlackLevel < 0)) {
				IJ.error("The white and black levels have not been defined.\n"
					   + "Please measure them before proceeding with the\n"
					   + "MTF calculation.");
				return;
			}
			if (!bMtfHeadingsCreated)
				clearMTF();

			measureMTF(imp);
			return;
		}
	}

	/**
	 * Creates a rectangular Region Of Interest that is the right size
	 * to perform the profile measurement.
	 */
	void createROI(ImagePlus imp) {
		Rectangle r = imp.getProcessor().getRoi();
		int iX, iY, iW, iH;
		if (bVertical) {
			iW = iLinesToAverage;
			iH = iMeasurementSize;
		} else {
			iW = iMeasurementSize;
			iH = iLinesToAverage;
		}

		// Center the new ROI relative to the old ROI
		iX = r.x + (r.width - iW) / 2;
		if (iX < 0) 
			iX = 0;
		if ((iX + iW) > imp.getWidth()) 
			iX = imp.getWidth() - iW;

		iY = r.y + (r.height - iH) / 2;
		if (iY < 0) 
			iY = 0;
		if ((iY + iH) > imp.getHeight())
			iY = imp.getHeight() - iH;

		imp.setRoi(iX, iY, iW, iH);
	}

	/**
	 * Moves the current Region Of Interest down by a certain distance in um.
	 * If we are measuring vertical bars, the ROI is moved to the right instead.
	 */
	void moveROI(ImagePlus imp, double dDistDown, double dDistOver) {
		Rectangle r = imp.getProcessor().getRoi();
		if (bVertical) {
			r.x += dDistDown * dScale;
			r.y += dDistOver * dScale;
		} else {
			r.y += dDistDown * dScale;
			r.x += dDistOver * dScale;
		}

		// Make sure we don't move outside of the image
		if (r.x < 0) 
			r.x = 0;
		if ((r.x + r.width) > imp.getWidth()) 
			r.x = imp.getWidth() - r.width;

		if (r.y < 0) 
			r.y = 0;
		if ((r.y + r.height) > imp.getHeight())
			r.y = imp.getHeight() - r.height;

		imp.setRoi(r.x, r.y, r.width, r.height);
	}


	/**
	 * Recalculate all parameters from user inputs
	 */
	void recalculate() {
		// always read these values
		bVertical = checkVertical.getState();
		bFirstTopLeft = checkFirstTopLeft.getState();
		bPlotDFT = checkPlotDFT.getState();
		dDutyCycle = parseDouble(textDutyCycle.getText());

		// calculate scale
		dMeasuredLength = parseDouble(textMeasuredLength.getText());
		dKnownLength = parseDouble(textKnownLength.getText());
		if (dKnownLength != 0.0 && dMeasuredLength != 0.0) {
			dScale = dMeasuredLength / dKnownLength; 
			labelScale.setText(Double.toString(dScale));
		} else {
			dScale = 0;
			labelScale.setText("unknown");
		}
		
		// calculate Measurement ROI
		if (dScale > 0) {
			iLinesToAverage = Integer.parseInt(textLinesToAverage.getText());
			iCyclesToMeasure = Integer.parseInt(textCyclesToMeasure.getText());
			dBarSpacing = parseDouble(choiceBarSpacing.getSelectedItem());
			iMeasurementSize = (int)(iCyclesToMeasure * dBarSpacing * dScale + 0.5);
			textLinesToAverage.setText(Integer.toString(iLinesToAverage));
			textCyclesToMeasure.setText(Integer.toString(iCyclesToMeasure));
			labelMeasurementSize.setText(Integer.toString(iMeasurementSize));
		}
	}

	/**
	 * Clears all MTF values from the ImageJ measurement list.
	 */
	void clearMTF() {
		IJ.setColumnHeadings("d\t1/d\tMTF\tN\tk\tFT(0)\tFT(k)\tDuty\tAmp\tOff");
		bMtfHeadingsCreated = true;
		bHasMtfData = false;
	}

	/**
	 * Finds the profile, calculates the DFT of the data, then
	 * calculates the MTF for this bar spacing based on the fundamental
	 * frequency (zero'th harmonic).
	 */
	void measureMTF(ImagePlus imp) {
		ProfilePlot profile = new ProfilePlot(imp, bVertical);
		double[] adX = profile.getProfile();

		int iN = adX.length;
		int iNK = nHARMONICS * iCyclesToMeasure;
		if (iNK > (iN/2))
			iNK = iN/2;

		double[] adFTX = AbsDFT(adX, iNK);
		if (adFTX==null)
			return;

		if (bPlotDFT) {
			// Plot the DFT in a window

   			float[] xValues = new float[iNK];

			for (int i=0; i<iNK; i++) {
        		xValues[i] = (float)(dScale * i / iN);
        		//xValues[i] = (float)(i);
			}

			float[] yValues = new float[iNK];
			for (int i=0; i<iNK; i++) {
        		yValues[i] = (float)adFTX[i];
			}

			PlotWindow pw = new PlotWindow("DFT of " + iCyclesToMeasure + " bars", 
					"Spacial Frequency (cycles/um)", "DFT Value", 
					xValues, yValues);
			pw.draw();
		}

		// Now that we have the DFT of the data, we can calculate the 
		// Modulation Transfer Function.

		iN			= iMeasurementSize;
		int iK		= iCyclesToMeasure;
		double dD	= dBarSpacing;
		double dL	= dDutyCycle * dD;
		double dFT0 = adFTX[0];
		double dFTK = adFTX[iK];
		double dDI	= 1/dD;
		double dA	= dWhiteLevel - dBlackLevel;
		double dC	= dBlackLevel;

		// Modulation Transfer Function
		//
		//	           |      d*FT(K)    |
		//	|H(1/d)| = |-----------------|
		//	           | N*a*L*sinc(L/d) |
		//
		double dMTF = Math.abs((dD*dFTK) / (iN*dA*dL*sinc(dL/dD)));

		// Estimage the Duty Cycle
		//
		//	       1    /  FT(0)     \
		//	L/d = --- * | ------ - c |
		//	       a    \    N       /
		double dDuty = (dFT0/iN - dC) / dA;

		IJ.write(dD +"\t"+
				 dDI +"\t"+
				 dMTF +"\t"+
				 iN +"\t"+
				 iK +"\t"+
				 dFT0 +"\t"+
				 dFTK +"\t"+
				 dDuty +"\t"+
				 dA +"\t"+
				 dC);

		bHasMtfData = true;
		IJ.beep();
	}

	/**
	 * Calculates the sinc function of a number
	 */
	double sinc(double x) {
		return Math.sin(Math.PI * x) / (Math.PI * x);
	}

	/**
	 * Calculates the magnitude of the Discrete Fourier Transform
	 * of an array of values.
	 */
	double[] AbsDFT(double[] adX, int iNK) {
		int iN = adX.length;
		if (iNK > iN)
			return null;
		double[] adFTX = new double[iNK];

		double dA, dB, dT, dTheta;
		for (int k=0; k<iNK; k++) {
			dA = 0;
			dB = 0;
			dT = -2.0 * Math.PI * k / iN;
			for (int x=0; x<iN; x++) {
				dTheta = dT * x;
				// Use the Euler identity exp(j*x) = cos(x) + j*sin(x)
				dA += adX[x] * Math.cos(dTheta);
				dB += adX[x] * Math.sin(dTheta);
			}
			adFTX[k] = Math.sqrt(dA*dA + dB*dB);
		}

		return adFTX;
	}

	/**
	* Returns a new double initialized to the value represented by the 
	* specified <code>String</code>.
	*
	* @param      s   the string to be parsed.
	* @return     the double value represented by the string argument.
	* @exception  NumberFormatException  if the string does not contain a
	*             parsable double.
	*/
	double parseDouble(String s) throws NumberFormatException {
		Double d = new Double(s);
		return(d.doubleValue());
	}
}
