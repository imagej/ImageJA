import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import ij.measure.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.Math.*;
import java.awt.image.*;
import ij.plugin.filter.Analyzer;

// FRETcalc_ plugin version 4.0
// for analysis of FRET by acceptor photobleaching using ImageJ program
// is suitable for analysis of non-continuos compartments
// written by David Stepensky (david.stepensky@yale.edu)
// September 18, 2010

public class FRETcalc_ extends Dialog
implements ActionListener, WindowListener, Runnable {
	private Thread threadProcess = null;

	ImagePlus imp_orig, imp;
	ImageProcessor ip;

	Roi roi; Polygon p; Rectangle r;

	int  DPre = 2;
	int  DPost = 1;
	int  APre = 4;
	int  APost = 3;

	int  Donor_min = 0;	//Thresholds
	int  Acceptor_min = 0;
	int  Donor_max = 255;
	int  Acceptor_max = 255;

	boolean Choice_Bleach_th = false;
	int Bleach_min=50;
	int Bleach_max=100;

	boolean Choice_FRET_th = false;
	int FRET_min = -100;
	int FRET_max = 100;

	boolean Choice_FRET = true;	//Plots output
	boolean Choice_Bleach = false;
	boolean Choice_DonAcc = false;
	boolean Choice_FRET_Bleach=false;
	boolean Choice_FRET_Don = false;
	boolean Choice_FRET_Acc = false;
	boolean Choice_FRET_DonAcc = false;
	boolean Choice_DonHis = false;	//Histograms output
	boolean Choice_AccHis = false;
	boolean Choice_FRETHis = true;
	boolean Choice_Results = true;		//Results table output
	boolean Choice_RawResults = false;


	public FRETcalc_ () {
		
		super(new Frame(), "FRETcalc v4.0");
		if (IJ.versionLessThan("1.21a"))
			return;

		imp_orig = WindowManager.getCurrentImage();
		if (imp_orig == null) {
			IJ.showMessage("Image required.");
			return;
		}

		ip = imp_orig.getProcessor();
		doDialog();
	}

	public void run() {
	}

	// Build the dialog box.
	private GridBagLayout 	layout;
	private GridBagConstraints 	constraint;

	private Button 		bnSmooth;
	private Button 		bnA_B;
	private Button 		bnBg;
	private Button 		bnLUT;
	private Button 		bnSettings;
	private Button 		bnOutput;
	private Button 		bnCalc;
	private Button 		bnHelp;
	private Button 		bnSave;
	private Button 		bnClose;


	private void doDialog() {
		// Layout
		layout = new GridBagLayout();
		constraint = new GridBagConstraints();

		bnSmooth = new Button("Smooth");
		bnA_B = new Button("Px by Px subtract");
		bnBg = new Button("Bg subtraction");
		bnLUT = new Button("LUT->Rainbow2");
		bnSettings = new Button("Settings");
		bnOutput = new Button("Options");
		bnCalc = new Button("Calculate");
		bnHelp = new Button("Help");
		bnSave = new Button("Save Image as");
		bnClose = new Button("Close");

		// Panel parameters
		Panel pnMain = new Panel();
		pnMain.setLayout(layout);
		addComponent(pnMain, 0, 0, 2, 1, 2, new Label("Image manipulations:"));
		addComponent(pnMain, 1, 0, 1, 1, 5, bnBg);	//Bg
		addComponent(pnMain, 1, 1, 1, 1, 5, bnSmooth);	//Smooth
		addComponent(pnMain, 2, 0, 1, 1, 5, bnLUT);   //LUT change to Rainbow2
		addComponent(pnMain, 2, 1, 1, 1, 5, bnA_B);	//A-B
		addComponent(pnMain, 3, 0, 2, 1, 2, new Label("--------------------------------------------------"));
		addComponent(pnMain, 4, 0, 2, 1, 2, new Label("FRET data analysis settings:"));
		addComponent(pnMain, 5, 1, 1, 1, 5, bnSettings);//Settings
		addComponent(pnMain, 6, 0, 2, 1, 2, new Label("Output options:"));
		addComponent(pnMain, 7, 1, 1, 1, 5, bnOutput);	//Output
		addComponent(pnMain, 8, 0, 2, 1, 2, new Label("--------------------------------------------------"));
		addComponent(pnMain, 12, 0, 2, 1, 2, new Label("FRET calculation:"));
		addComponent(pnMain, 13, 1, 1, 1, 5, bnCalc);	//FRET calc
		addComponent(pnMain, 14, 0, 2, 1, 2, new Label("--------------------------------------------------"));
		addComponent(pnMain, 15, 0, 1, 1, 5, bnHelp);   //Help
		addComponent(pnMain, 16, 0, 1, 1, 5, bnSave);  //Save
		addComponent(pnMain, 16, 1, 1, 1, 5, bnClose);  //Close

		// Add Listeners
		bnBg.addActionListener(this);
		bnSmooth.addActionListener(this);
		bnLUT.addActionListener(this);
		bnA_B.addActionListener(this);
		bnSettings.addActionListener(this);
		bnOutput.addActionListener(this);
		bnCalc.addActionListener(this);
		bnHelp.addActionListener(this);
		bnSave.addActionListener(this);
		bnClose.addActionListener(this);

		// Build panel
		add(pnMain);
		pack();
		setResizable(false);
		GUI.center(this);
		setVisible(true);
		IJ.wait(250); // work around for Sun/WinNT bug
	}

	final private void addComponent(
	final Panel pn,
	final int row, final int col,
	final int width, final int height,
	final int space,
	final Component comp) {
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(space, space, space, space);
		constraint.weightx = IJ.isMacintosh()?90:100;
		constraint.fill = constraint.HORIZONTAL;
		layout.setConstraints(comp, constraint);
		pn.add(comp);
	}

	// Implement the listeners
 
	public synchronized  void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnClose) {
			dispose();
		}
		else if (e.getSource() == bnLUT){ //Change LUT

		IJ.run("Rainbow2 ");

		}
		else if (e.getSource() == bnSave){ //Save

			imp = WindowManager.getCurrentImage();
			IJ.run("Save");
	 		imp.updateAndDraw();

		}

		else if (e.getSource() == bnSettings){ //Change the settings

		 GenericDialog gd = new GenericDialog("FRET calculation settings");
		 gd.addMessage("Pre & post-bleach images\n(slice numbers):");
		 gd.addNumericField("Donor Pre-",DPre,0 );
		 gd.addNumericField("Donor Post-",DPost,0 );
		 gd.addNumericField("Acceptor Pre-",APre,0 );
		 gd.addNumericField("Acceptor Post-",APost,0 );
		 gd.addMessage("Settings for data analysis:");
		 gd.addNumericField("Donor threshold (min):", Donor_min, 0);
		 gd.addNumericField("Donor threshold (max):", Donor_max, 0);
		 gd.addNumericField("Acceptor threshold (min):", Acceptor_min, 0);
		 gd.addNumericField("Acceptor threshold (max):", Acceptor_max, 0);
		 gd.addCheckbox("Use %bleached thresholds?",Choice_Bleach_th);
		 gd.addNumericField("%bleached threshold (min):", Bleach_min, 0);
		 gd.addNumericField("%bleached threshold (max):", Bleach_max, 0);
		 gd.addCheckbox("Use %FRET thresholds?",Choice_FRET_th);		
		 gd.addNumericField("%FRET threshold (min):", FRET_min, 0);
		 gd.addNumericField("%FRET threshold (max):", FRET_max, 0);

   		 gd.showDialog();
		 if (gd.wasCanceled()) return;
		 DPre = (int) gd.getNextNumber();
		 DPost = (int) gd.getNextNumber();
		 APre = (int) gd.getNextNumber();
		 APost = (int) gd.getNextNumber();
		 Donor_min = (int) gd.getNextNumber();
		 Donor_max = (int) gd.getNextNumber();
		 Acceptor_min = (int) gd.getNextNumber();
		 Acceptor_max = (int) gd.getNextNumber();
		 Choice_Bleach_th =  gd.getNextBoolean();
		 Bleach_min = (int) gd.getNextNumber();
		 Bleach_max = (int) gd.getNextNumber();
		 Choice_FRET_th =  gd.getNextBoolean();
		 FRET_min = (int) gd.getNextNumber();
		 FRET_max = (int) gd.getNextNumber();
		}

		else if (e.getSource() == bnOutput){ //Set the output options

		 GenericDialog gd = new GenericDialog("Output options:");
		 gd.addMessage("Plots:");
		 gd.addCheckbox("%FRET plot",Choice_FRET);
		 gd.addCheckbox("%Bleached plot",Choice_Bleach);
		 gd.addCheckbox("Donor vs. Acceptor plot",Choice_DonAcc);
		 gd.addCheckbox("%FRET vs. %Bleached plot",Choice_FRET_Bleach);
		 gd.addCheckbox("%FRET vs. Donor plot",Choice_FRET_Don);
		 gd.addCheckbox("%FRET vs. Acceptor plot",Choice_FRET_Acc);
		 gd.addCheckbox("%FRET vs. Donor/Acceptor ratio plot",Choice_FRET_DonAcc);
		 gd.addMessage("Histograms:");
		 gd.addCheckbox("Donor histogram",Choice_DonHis);
		 gd.addCheckbox("Acceptor histogram",Choice_AccHis);
		 gd.addCheckbox("%FRET histogram",Choice_FRETHis);
		 gd.addMessage("Tables:");
		 gd.addCheckbox("Summary Results table",Choice_Results);
		 gd.addCheckbox("Raw Results table",Choice_RawResults);

		 gd.showDialog();
		 if (gd.wasCanceled()) return;
		 Choice_FRET = gd.getNextBoolean();
		 Choice_Bleach = gd.getNextBoolean();
		 Choice_DonAcc =  gd.getNextBoolean();
		 Choice_FRET_Bleach = gd.getNextBoolean();
		 Choice_FRET_Don = gd.getNextBoolean();
		 Choice_FRET_Acc = gd.getNextBoolean();
		 Choice_FRET_DonAcc = gd.getNextBoolean();
		 Choice_DonHis =  gd.getNextBoolean();
		 Choice_AccHis =  gd.getNextBoolean();
		 Choice_FRETHis =  gd.getNextBoolean();
		 Choice_Results =  gd.getNextBoolean();
		 Choice_RawResults =  gd.getNextBoolean();

		}

		else if (e.getSource() == bnSmooth){ // Smooth the image -> new image

		 ImagePlus imp_orig = WindowManager.getCurrentImage();
		 ImageStack stack = imp_orig.getStack();
		 String title = imp_orig.getTitle();
		 IJ.run("Smooth");
		 imp_orig.hide();
		 new ImagePlus(title+"_smoothed",stack).show();
		 ImagePlus imp_sm = WindowManager.getCurrentImage();
		 imp_sm.updateAndDraw();

		}
		else if (e.getSource() == bnA_B){ // Pixel to pixel difference: Slice A - Slice B

		ImagePlus imp = WindowManager.getCurrentImage();
		ImageStack stack = imp.getStack();
		String label = "Donor Post-Pre";

		 GenericDialog gd = new GenericDialog("Pixel by pixel A-B");
		 gd.addStringField("New slice name:", label );
		 gd.addNumericField("sliceA",1,0 );
		 gd.addNumericField("sliceB",2,0 );
  		 gd.showDialog();
		 if (gd.wasCanceled()) return;
		 label = gd.getNextString();
		 int A = (int) gd.getNextNumber();
		 int B = (int) gd.getNextNumber();

		 ImageProcessor ipA, ipB, ipC;
		 ipA = stack.getProcessor(A);
		 ipB = stack.getProcessor(B);

 		 int width  = ipA.getWidth(); int height = ipA.getHeight(); int v;

                 stack.addSlice(label, ip.createProcessor(width, height),stack.getSize());
		 ipC = stack.getProcessor(stack.getSize());

                for (int x=0; x<width; x++) {
                  for (int y=0; y<height; y++) {
                     v = (int) ipA.getPixelValue(x,y)- (int) ipB.getPixelValue(x,y);
			  if (v > 255) v=255;
                          if (v < 0) v=0;
		     ipC.putPixelValue(x, y, v);
	
		   }
                }

		 imp.updateAndDraw();
		 imp.setSlice(stack.getSize());
		}

		else if (e.getSource() == bnBg){ // Background subtraction -> new image


		ImagePlus imp_orig = WindowManager.getCurrentImage();
		ImageStack stack = imp_orig.getStack();
		String title = imp_orig.getTitle();

		roi = imp_orig.getRoi();
		if(imp_orig.getRoi()==null) 
			{IJ.showMessage("Error", "No ROI");
			return;}
		p = roi.getPolygon();
		r = roi.getBounds();

		 GenericDialog gd = new GenericDialog("Bg subtraction");
		 gd.addNumericField("From slice",1,0 );
		 gd.addNumericField("To slice",4,0 );
  		 gd.showDialog();
		 if (gd.wasCanceled()) return;
		 int A = (int) gd.getNextNumber();
		 int B = (int) gd.getNextNumber();

		 imp_orig.hide();
		 new ImagePlus(title+"-bg",stack).show();

		 ImagePlus imp_bg = WindowManager.getCurrentImage();
		 ImageStack stack_bg = imp_bg.getStack();
		 ImageProcessor ip_bg;

		for (int Z=A; Z<=B; Z++) { // loop for all the selected slices

		 ip_bg = stack.getProcessor(Z);
 		 int width  = ip_bg.getWidth();
 	         int height = ip_bg.getHeight();
		 int v; int Sum=0; int PxCount=0; int MeanBg=0;

                for (int i=r.x; i<r.x+r.width; i++) {	// measure the bg ROI mean
                  for (int j=r.y; j<r.y+r.height; j++) {
	       if (p.contains (i,j)==true) {
                     	Sum = Sum+(int) ip_bg.getPixelValue(i,j);
		 PxCount++;
	       }
	     }
	   }
		MeanBg= (int) Math.round((double) Sum/(double)PxCount);

                for (int i=0; i<width; i++) {  // subtract the background
                  for (int j=0; j<height; j++) {
                       v = (int) ip_bg.getPixelValue(i,j) - MeanBg;
		if (v > 255) v=255;
                        	if (v < 0) v=0;
		ip_bg.putPixelValue(i, j, v);
	     }
	   }
	}
		 imp_bg.updateAndDraw();


		}
		else if (e.getSource() == bnCalc){ // FRET calculation & output

		ImagePlus imp = WindowManager.getCurrentImage();
		ImageStack stack = imp.getStack();
 		String title = imp.getTitle();

		roi = imp.getRoi();
		if(imp.getRoi()==null) 
			{IJ.showMessage("Error", "No ROI");
			return;}

		p = roi.getPolygon();
		r = roi.getBounds();

		ImageProcessor ipDPre, ipDPost, ipAPre, ipAPost; 
 		ipDPre = stack.getProcessor(DPre);
		ipDPost = stack.getProcessor(DPost);
		ipAPre = stack.getProcessor(APre);
		ipAPost = stack.getProcessor(APost);
		int width  = ipDPre.getWidth(); int height = ipDPre.getHeight(); 

		int count=0;double bleach_v; double FRET_v;
		double DonPre, AccPre, DonPost, AccPost;

		double DonPre_allpix, AccPre_allpix, DonPost_allpix, AccPost_allpix;
		DonPre_allpix=0; AccPre_allpix=0; DonPost_allpix=0; AccPost_allpix=0;

                for (int i=r.x; i<r.x+r.width; i++) {	 // Check number of pixels (count) above the thresholds
		for (int j=r.y; j<r.y+r.height; j++) { // without knowing this count the histograms could not be plotted correctly
		      if (p.contains (i,j)==true) {

			 DonPre = (int) ipDPre.getPixelValue(i,j);
			 AccPre = (int) ipAPre.getPixelValue(i,j);
			 DonPost=(int) ipDPost.getPixelValue(i,j);
			 AccPost=(int) ipAPost.getPixelValue(i,j);
			
			 bleach_v=(double) 100*(1-AccPost/AccPre);
			 FRET_v=100*(DonPost-DonPre)/DonPost;

	  if((DonPost>=Donor_min && DonPost<=Donor_max && AccPre>=Acceptor_min && AccPre<=Acceptor_max) &&
	  (Choice_Bleach_th == false || (Choice_Bleach_th == true && bleach_v >= Bleach_min && bleach_v <= Bleach_max)) &&
	  (Choice_FRET_th == false || (Choice_FRET_th == true && FRET_v >= FRET_min && FRET_v <= FRET_max))) {count++;}
		       }  
		 }
	  }

		int [][] Data_px = new int[count][2];
		double [] DPre_px = new double[count];
		double [] DPost_px = new double[count];
		double [] APre_px = new double[count];
		double [] APost_px = new double[count];
		double [] DA_ratio_px = new double[count];
		double [] Bleach_px = new double[count];
		double [] FRET_px = new double[count];
		int [] FRET_to_plot = new int[count];
		count=0;

                for (int i=r.x; i<r.x+r.width; i++) {	// fill the arrays with data
		  for (int j=r.y; j<r.y+r.height; j++) {
		        if (p.contains (i,j)==true) {

			 DonPre = (int) ipDPre.getPixelValue(i,j); // data for individual pixels
			 AccPre = (int) ipAPre.getPixelValue(i,j);
			 DonPost=(int) ipDPost.getPixelValue(i,j);
			 AccPost=(int) ipAPost.getPixelValue(i,j);

			 DonPre_allpix += DonPre;	// for FRETall calculation 
			 AccPre_allpix += AccPre;	// based on all pixels
			 DonPost_allpix +=DonPost;	// in the selected ROI
			 AccPost_allpix +=AccPost;	//
			
			 bleach_v=(double) 100*(1-AccPost/AccPre);
			 FRET_v=100*(DonPost-DonPre)/DonPost;

	  if((DonPost>=Donor_min && DonPost<=Donor_max && AccPre>=Acceptor_min && AccPre<=Acceptor_max) &&
	  (Choice_Bleach_th == false || (Choice_Bleach_th == true && bleach_v >= Bleach_min && bleach_v <= Bleach_max)) &&
	  (Choice_FRET_th == false || (Choice_FRET_th == true && FRET_v >= FRET_min && FRET_v <= FRET_max))) {

			//add the data for individual pixels to the arrays
                   	Data_px[count][0] =i;			// Xstart			
		  	Data_px[count][1] =j;			// Ystart
		   	DPre_px[count]  =DonPre;		// Dpre
		   	DPost_px[count] = DonPost;		// DPost
		   	APre_px[count] = AccPre;		// APre
		   	APost_px[count] =AccPost;		// APost
		   	DA_ratio_px[count] =DonPost/AccPre;	// D/A ratio
		   	Bleach_px[count]=bleach_v;		// %bleaching
		   	FRET_px[count]	=FRET_v;		// %FRET

			FRET_to_plot[count]=(int) Math.round((double) 2.55*FRET_v); // FRET presentation on 8-bit image
			 if (FRET_to_plot[count]>255) FRET_to_plot[count]=255;
			 if (FRET_to_plot[count]<0) FRET_to_plot[count]=0;
			count++;
       	    }
		      }
		   }
	  }


		//Statistical analysis via histograms
		ImageProcessor ip_FRET_His = new FloatProcessor(1,count,FRET_px);
		ImageProcessor ip_Bleach_His = new FloatProcessor(1,count,Bleach_px);
		ImageProcessor ip_DPre_His = new FloatProcessor(1,count,DPre_px);
		ImageProcessor ip_APre_His = new FloatProcessor(1,count,APre_px);
		ImageProcessor ip_DPost_His = new FloatProcessor(1,count,DPost_px);
		ImageProcessor ip_APost_His = new FloatProcessor(1,count,APost_px);
		ImageProcessor ip_DA_ratio_His = new FloatProcessor(1,count,DA_ratio_px);

		ImagePlus imp_FRET_His = new ImagePlus("Temp", ip_FRET_His);
		ImagePlus imp_Bleach_His = new ImagePlus("Temp", ip_Bleach_His);
		ImagePlus imp_DPre_His = new ImagePlus("Temp", ip_DPre_His);
		ImagePlus imp_APre_His = new ImagePlus("Temp", ip_APre_His);
		ImagePlus imp_DPost_His = new ImagePlus("Temp", ip_DPost_His);
		ImagePlus imp_APost_His = new ImagePlus("Temp", ip_APost_His);
		ImagePlus imp_DA_ratio_His = new ImagePlus("Temp", ip_DA_ratio_His);

	//Histograms output
	if(Choice_DonHis==true) new HistogramWindow("Donor", imp_DPost_His, 50, 0, ip_DPost_His.getMax());
	if(Choice_AccHis==true) new HistogramWindow("Acceptor", imp_APre_His, 50, 0, ip_APre_His.getMax());
	if(Choice_FRETHis==true) new HistogramWindow("%FRET_th", imp_FRET_His, 50, ip_FRET_His.getMin(), ip_FRET_His.getMax());

	// Plots output
	  double [] stama=new double[1];
	  double [] stamb=new double[1];

		ImageProcessor ip_FRET, ip_DonAcc;

		if (Choice_FRET == true) {
			
			stack.addSlice("%FRET_th", ip.createProcessor(width, height),stack.getSize());
			ip_FRET = stack.getProcessor(stack.getSize());
			for (int i=0; i<count; i++) {
				ip_FRET.putPixelValue(Data_px[i][0],Data_px[i][1], FRET_to_plot[i]);
			}
		}

		if (Choice_Bleach == true) {
			ImageProcessor ip_Bleach;
			stack.addSlice("%Bleach_th", ip.createProcessor(width, height),stack.getSize());
			ip_Bleach = stack.getProcessor(stack.getSize());
			for (int i=0; i<count; i++) {
				ip_Bleach.putPixelValue(Data_px[i][0],Data_px[i][1], (int) Bleach_px[i]);
			}
		}


		if (Choice_DonAcc == true) {
PlotWindow DonAcc_PW = new PlotWindow("Donor vs. Acceptor_"+title, "Acceptor", "Donor", stama, stamb);
DonAcc_PW.setLimits(0, 255, 0, 255);
DonAcc_PW.addPoints(APre_px,DPost_px,5);
DonAcc_PW.draw();
		}

		if (Choice_FRET_Bleach == true) {
PlotWindow FRET_Bleach_PW = new PlotWindow("%FRET_th vs. %bleached_th_"+title, "%bleached", "%FRET_th", stama, stamb);
FRET_Bleach_PW.setLimits(ip_Bleach_His.getMin(), ip_Bleach_His.getMax(), ip_FRET_His.getMin(), ip_FRET_His.getMax());
FRET_Bleach_PW.addPoints(Bleach_px,FRET_px,5);
FRET_Bleach_PW.draw();
		}

		if (Choice_FRET_Don == true) {
PlotWindow FRET_Don_PW = new PlotWindow("%FRET_th vs. Donor_"+title, "Donor", "%FRET_th", stama, stamb);
FRET_Don_PW.setLimits(0, 255, ip_FRET_His.getMin(), ip_FRET_His.getMax());
FRET_Don_PW.addPoints(DPost_px,FRET_px,5);
FRET_Don_PW.draw();
		}

		if (Choice_FRET_Acc == true) {
PlotWindow FRET_Acc_PW = new PlotWindow("%FRET_th vs. Acceptor_"+title, "Acceptor", "%FRET_th", stama, stamb);
FRET_Acc_PW.setLimits(0, 255, ip_FRET_His.getMin(), ip_FRET_His.getMax());
FRET_Acc_PW.addPoints(APre_px,FRET_px,5);
FRET_Acc_PW.draw();
		}

		if (Choice_FRET_DonAcc == true) {
PlotWindow FRET_DonAcc_PW = new PlotWindow("%FRET_th vs. D/A ratio_"+title, "D/A ratio", "%FRET_th", stama, stamb);
FRET_DonAcc_PW.setLimits(ip_DA_ratio_His.getMin(), ip_DA_ratio_His.getMax(), ip_FRET_His.getMin(), ip_FRET_His.getMax());
FRET_DonAcc_PW.addPoints(DA_ratio_px,FRET_px,5);
FRET_DonAcc_PW.draw();
		}

	if(Choice_Results == true) { // Results table

		Analyzer a = new Analyzer();
		ResultsTable rt =Analyzer.getResultsTable();
		rt.setPrecision(2);

		ImageStatistics stat_DPre = ImageStatistics.getStatistics(ip_DPre_His,Measurements.MEDIAN,null);
		ImageStatistics stat_APre = ImageStatistics.getStatistics(ip_APre_His,Measurements.MEDIAN,null);
		ImageStatistics stat_DPost = ImageStatistics.getStatistics(ip_DPost_His,Measurements.MEDIAN,null);
		ImageStatistics stat_APost = ImageStatistics.getStatistics(ip_APost_His,Measurements.MEDIAN,null);

		double DonPre_sum, AccPre_sum, DonPost_sum, AccPost_sum;
		DonPre_sum=0; AccPre_sum=0; DonPost_sum=0; AccPost_sum=0;

                for (int i=0; i<count; i++) {	// FRETth calculation from average of all pixels above the thresholds
		   	DonPre_sum += DPre_px[i];		//Dpre
		   	DonPost_sum+= DPost_px[i];		//DPost
		   	AccPre_sum += APre_px[i];		//APre
		   	AccPost_sum+= APost_px[i];		//APost
		}


		//Add the results to the results table
		rt.incrementCounter();
		rt.addLabel("File",imp.getTitle());
		rt.addValue("Donor th (min)",(double) Donor_min);
		rt.addValue("Donor th (max)",(double) Donor_max);
		rt.addValue("Acceptor th (min)",(double) Acceptor_min);
		rt.addValue("Acceptor th (max)",(double) Acceptor_max);
		if (Choice_Bleach_th==true) {
			rt.addValue("Bleach_th min",Bleach_min);
			rt.addValue("Bleach_th max",Bleach_max);
			}
		if (Choice_FRET_th==true) {
			rt.addValue("FRET_th min",FRET_min);
			rt.addValue("FRET_th max",FRET_max);
			}
		rt.addValue("DPre",(double) stat_DPre.median);
		rt.addValue("DPost",(double) stat_DPost.median);
		rt.addValue("APre",(double) stat_APre.median);
		rt.addValue("APost",(double) stat_APost.median);

		// calculation of %FRET based on the sum of pixels above the thresholds
		rt.addValue("%bleach_th",(double) 100*(AccPre_sum-AccPost_sum)/AccPre_sum); 
		rt.addValue("%FRET_th",(double) 100*(DonPost_sum-DonPre_sum)/DonPost_sum);

		// calculation of %FRET based on all the pixels in the selected ROI, not taking thresholds in account
		rt.addValue("%bleach_all",(double) 100*(AccPre_allpix-AccPost_allpix)/AccPre_allpix);
		rt.addValue("%FRET_all",(double) 100*(DonPost_allpix-DonPre_allpix)/DonPost_allpix);

		a.displayResults();
		a.updateHeadings();
	}


	if(Choice_RawResults == true) { // Raw Results table

		Analyzer b = new Analyzer();
		ResultsTable rt_raw =Analyzer.getResultsTable();
		rt_raw.setPrecision(2);
		for (int i=0; i<count; i++) {
			rt_raw.incrementCounter();
			rt_raw.addLabel("File",imp.getTitle());
		rt_raw.addValue("Donor th (min)",(double) Donor_min);
		rt_raw.addValue("Donor th (max)",(double) Donor_max);
		rt_raw.addValue("Acceptor th (min)",(double) Acceptor_min);
		rt_raw.addValue("Acceptor th (max)",(double) Acceptor_max);
		if (Choice_Bleach_th==true) {
			rt_raw.addValue("Bleach_th min",Bleach_min);
			rt_raw.addValue("Bleach_th max",Bleach_max);
			}
		if (Choice_FRET_th==true) {
			rt_raw.addValue("FRET_th min",FRET_min);
			rt_raw.addValue("FRET_th max",FRET_max);
			}
			rt_raw.addValue("Xstart",Data_px[i][0]);
			rt_raw.addValue("Ystart",Data_px[i][1]);
			rt_raw.addValue("DPre",DPre_px[i]);
			rt_raw.addValue("DPost",DPost_px[i]);
			rt_raw.addValue("APre",APre_px[i]);
			rt_raw.addValue("APost",APost_px[i]);
			rt_raw.addValue("%bleach",Bleach_px[i]);
			rt_raw.addValue("%FRET",FRET_px[i]);

		}

		b.displayResults();
		b.updateHeadings();

	 }
		 imp.updateAndDraw();
		 imp.setSlice(stack.getSize());
		
		}
		else if (e.getSource() == bnHelp) {
			IJ.showMessage("Help",
			"FRETcalc plugin version 4.0\n"+
			"for analysis of FRET images obtained by acceptor photobleaching using ImageJ program\n"+
			"written by David Stepensky (david.stepensky@yale.edu) September 18, 2010\n"+
			" \n"+
 			"File (8-bit) is required with 4 slices corresponding to Donor & Acceptor pre & post-bleaching images\n"+
 			"Supports all types of regions of interest (polygonal, rectangular, elliptical, and freehand ROIs)\n"+
			" \n"+
			"Bg subtraction - subtracts the average intensity of the pre-defined ROI from each pixel on the selected slice/s\n"+
			"Smooth - applies 3x3 filter to smooth all the slices of the image, is equivalent to Process->Smooth command\n"+
			"LUT->Rainbow2 - changes the LUT of each slice to Rainbow2 (requires rainbow2.lut in ImageJ LUT directory)\n"+
			"Px by Px subtract - subtracts pixel-wise one slice from another and plots the result as a new slice\n"+
			" \n"+
			"Settings - allows input of the slice order and the thresholds for data analysis\n"+
			"Options - allows choice of the output options\n"+
			"Calculate - performs the calculation & outputs the results for the pre-selected rectangular ROI\n"+
			" \n"+
			"Save Image as - allows saving of the active image file only, Histograms, Results Table & ROIs should  be saved separately\n"+
			"Close - quits the FRETcalc plugin\n");
		}
		notify();
	}


	public void windowActivated(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		dispose();
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e){
	}

	public void windowIconified(WindowEvent e){
	}

	public void windowOpened(WindowEvent e){
	}
}

