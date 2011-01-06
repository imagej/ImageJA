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

// IntraCell plugin version 1.0 for ImageJ program
// for analysis of nanoparticle (NP) colocalization with organelles within the cells
// written by David Stepensky (davidst@bgu.ac.il)
// January 5, 2011

public class IntraCell_v1 extends Dialog
implements ActionListener, WindowListener, Runnable {
	private Thread threadProcess = null;

	ImagePlus imp_orig, imp;
	ImageStack stack;
	ImageProcessor ip;
	
	String title;
	String name0 = "Cell Surface";
	String name1 = "Nanoparticles";
	String name2 = "Organelle1";
	String name3 = "Organelle2";
	String none = "*None*";

	ImagePlus impCh0, impCh1, impCh2, impCh3;
	ImageStack stackCh0, stackCh1, stackCh2, stackCh3;
	ImageProcessor ipCh0, ipCh1, ipCh2, ipCh3;
	int slice = 1; int width = 512; int height = 512;

	int[] index = new int[4];
	int[] wList; 	String[] titles = new String[5];
	int LastImage = 999; // no images for analysis

	boolean outlineCh0 = true;
	boolean outlineCh1 = true;
	boolean outlineCh2 = true;
	boolean outlineCh3 = true;
	boolean labelCh0 = true;
	boolean labelCh1 = true;
	boolean labelCh2 = true;
	boolean labelCh3 = true;
	boolean countCh = true;
	boolean intensityCh = true;
	
	int thresh1 = 500; int thresh2 = 500; int thresh3 = 500;	// Thresholds

	Roi roi; Polygon p;
	Analyzer a; 	// Results table
	ResultsTable rt;

	public IntraCell_v1 () {
		
		super(new Frame(), "IntraCell_v1");
		if (IJ.versionLessThan("1.21a"))
			return;
		doDialog();
	}

void checkImage() {
	imp_orig = WindowManager.getCurrentImage();
	if (imp_orig == null) {
		IJ.showMessage("Image required.");
		return;
	}
	ip = imp_orig.getProcessor();
}

void drawOutlineLabel(ImagePlus imp, Roi roi, int slice, boolean A, boolean B) {
	if (roi==null) return;
	stack = imp.getStack();
	ImageProcessor ip = stack.getProcessor(slice);
	Polygon p;
	p = roi.getPolygon();
	ip.setColor((int) ip.maxValue());

	if (A==true) {
		ip.setRoi(roi);
		ip.drawPolygon(p);
	}

	if (B==true) {
		int n = Analyzer.getCounter() + 1;
		String count = "" + n;
		Rectangle r = roi.getBoundingRect();
		int x = r.x + r.width/2 - ip.getStringWidth(count)/2;
		int y = r.y + r.height/2 + 6;
		ip.setFont(new Font("SansSerif", Font.PLAIN, 9));
		ip.drawString(count, x, y);
	}
	imp.updateAndDraw();
}

	public void run() {
	}

	// Build the dialog box.
	private GridBagLayout 	layout;
	private GridBagConstraints 	constraint;
	private Button 		bnSelection;
	private Button 		bnSettings;
	private Button 		bnCalc;
	private Button 		bnSmooth;
	private Button 		bnLUT;
	private Button 		bnHelp;
	private Button 		bnClose;

	private void doDialog() {
		// Layout
		layout = new GridBagLayout();
		constraint = new GridBagConstraints();
		bnSelection = new Button("Images Selection");
		bnSettings = new Button("Settings & Output");
		bnCalc = new Button("Calculate");
		bnSmooth = new Button("Smooth");
		bnLUT = new Button("LUT->Rainbow2");
		bnHelp = new Button("Help");
		bnClose = new Button("Close");

		// Panel parameters
		Panel pnMain = new Panel();
		pnMain.setLayout(layout);
		addComponent(pnMain, 0, 0, 1, 1, 2, new Label("Analysis:"));
		addComponent(pnMain, 1, 0, 1, 1, 2, bnSelection);	//Images Selection
		addComponent(pnMain, 2, 0, 1, 1, 2, bnSettings);	//Settings & Output
		addComponent(pnMain, 3, 0, 1, 1, 2, bnCalc);		//Calculation
		addComponent(pnMain, 4, 0, 1, 1, 2, new Label("-------------------------------"));
		addComponent(pnMain, 5, 0, 1, 1, 2, new Label("Image manipulations:"));
		addComponent(pnMain, 6, 0, 1, 1, 2, bnSmooth);	//Smooth
		addComponent(pnMain, 7, 0, 1, 1, 2, bnLUT);  	//LUT change to Rainbow2
		addComponent(pnMain, 8, 0, 1, 1, 2, new Label("-------------------------------"));
		addComponent(pnMain, 9, 0, 1, 1, 2, new Label("Other:"));
		addComponent(pnMain, 10, 0, 1, 1, 1, bnHelp);   	//Help
		addComponent(pnMain, 11, 0, 1, 1, 1, bnClose);  	//Close

		// Add Listeners
		bnSelection.addActionListener(this);
		bnSettings.addActionListener(this);
		bnCalc.addActionListener(this);
		bnSmooth.addActionListener(this);
		bnLUT.addActionListener(this);
		bnHelp.addActionListener(this);
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

	else if (e.getSource() == bnSmooth){ // Smooth the image -> new image
		checkImage();
		IJ.run("Smooth");
	}

	else if (e.getSource() == bnLUT){ //Change LUT
		checkImage();
		IJ.run("Rainbow2 ");
	}

	else if (e.getSource() == bnSelection){ // Update/Selection of images
	
	checkImage();
	wList = WindowManager.getIDList();
	if (wList.length > 4) {
		IJ.showMessage("Too many open images.");
		return;
	}

	LastImage = wList.length-1;
	
	for (int i=0; i<5; i++) {
     	  titles[i] = none;
	}

	for (int i=0; i<wList.length; i++) {
	  imp = WindowManager.getImage(wList[i]);
	  titles[i] = imp!=null?imp.getTitle():"";
	}

	GenericDialog gd = new GenericDialog("Update/Selection of images for analysis");
	gd.addStringField("Image1", name0, 20);
	gd.addChoice("Cell Slcie Stack:", titles, titles[0]);
	gd.addMessage(" ");
	gd.addStringField("Image2", name1, 20);
	gd.addChoice("Nanoparticles Stack:", titles, titles[1]);
	gd.addMessage(" ");
	gd.addStringField("Image3", name2, 20);
	gd.addChoice("Organelle1 Stack:", titles, titles[2]);
	gd.addMessage(" ");
	gd.addStringField("Image4", name3, 20);
	gd.addChoice("Organelle2 Stack:", titles, titles[3]);
	gd.showDialog();
	if (gd.wasCanceled())
	     return;

	name0 = gd.getNextString();
	index[0] = gd.getNextChoiceIndex();
	name1 = gd.getNextString();
	index[1] = gd.getNextChoiceIndex();
	name2 = gd.getNextString();
	index[2] = gd.getNextChoiceIndex();
	name3 = gd.getNextString();
	index[3] = gd.getNextChoiceIndex();

	if (wList.length >=4 ) {
		impCh3 = WindowManager.getImage(wList[index[3]]);
		titles[3] = impCh3.getTitle();	
		stackCh3 = impCh3.getStack();
	}

	if (wList.length >=3) {
		impCh2 = WindowManager.getImage(wList[index[2]]);
		titles[2] = impCh2.getTitle();	
		stackCh2 = impCh2.getStack();
	}

	if (wList.length >=2) {
		impCh1 = WindowManager.getImage(wList[index[1]]);
		titles[1] = impCh1.getTitle();		
		stackCh1 = impCh1.getStack();
	}

	if (wList.length >= 1) {
		impCh0 = WindowManager.getImage(wList[index[0]]);
		titles[0] = impCh0.getTitle();		
		stackCh0 = impCh0.getStack();
	}

	if (wList.length == 0) {
		IJ.showMessage("One or more images required.");
		return;
	}

	}

	else if (e.getSource() == bnSettings){ //Change the thresholds
		GenericDialog gd = new GenericDialog("Analysis Settings & Output");
		gd.addMessage("Thresholds:");
		gd.addNumericField(name1 + " threshold ",thresh1,0 );
		gd.addNumericField(name2 + " threshold ",thresh2,0 );
		gd.addNumericField(name3 + " threshold ",thresh3,0 );
		gd.addMessage("--------------------");
		gd.addMessage("ROIs Outline & Label:");
		gd.addMessage(name0);
		gd.addCheckbox("Outline ROI?",outlineCh0);
		gd.addCheckbox("Label ROI?",labelCh0);
		gd.addMessage(name1);
		gd.addCheckbox("Outline ROI?",outlineCh1);
		gd.addCheckbox("Label ROI?",labelCh1);
		gd.addMessage(name2);
		gd.addCheckbox("Outline ROI?",outlineCh2);
		gd.addCheckbox("Label ROI?",labelCh2);
		gd.addMessage(name3);
		gd.addCheckbox("Outline ROI?",outlineCh3);
		gd.addCheckbox("Label ROI?",labelCh3);
		gd.addMessage("--------------------");
		gd.addMessage("Results table:");
		gd.addCheckbox("Calculate pixel counts?", countCh);
		gd.addCheckbox("Calculate pixel intenisites?", intensityCh);

   		gd.showDialog();
		if (gd.wasCanceled()) return;

		thresh1 = (int) gd.getNextNumber();
		thresh2 = (int) gd.getNextNumber();
		thresh3 = (int) gd.getNextNumber();

		outlineCh0 = gd.getNextBoolean();
		labelCh0 = gd.getNextBoolean();
		outlineCh1 = gd.getNextBoolean();
		labelCh1 = gd.getNextBoolean();
		outlineCh2 = gd.getNextBoolean();
		labelCh2 = gd.getNextBoolean();
		outlineCh3 = gd.getNextBoolean();
		labelCh3 = gd.getNextBoolean();

		countCh = gd.getNextBoolean();
		intensityCh = gd.getNextBoolean();
	}

	else if (e.getSource() == bnCalc){ // calculation & output

		if (LastImage == 999) {
			IJ.showMessage("One or more images required.");
			return;
		}

		slice = impCh0.getCurrentSlice();
 		ipCh0 = stackCh0.getProcessor(slice);

		if (LastImage > 0) ipCh1 = stackCh1.getProcessor(slice);
		if (LastImage > 1) ipCh2 = stackCh2.getProcessor(slice);
		if (LastImage > 2) ipCh3 = stackCh3.getProcessor(slice);

	 	width  = ipCh0.getWidth();
		height = ipCh0.getHeight();

		if(impCh0.getRoi()==null) 
		  {IJ.showMessage("Error", "No ROI in Cell Surface Stack");
		   return;
		}

		roi = impCh0.getRoi();	 
		p = roi.getPolygon();

		//Add the results to the results table
		a = new Analyzer();
		rt =Analyzer.getResultsTable();
		rt.setPrecision(2);

		//0-total, 1-NP, 2-organelle1, 3-organelle2
		//area (pixel number) & intensity (sum of intensities) calculations

		int px_area0 = 0; int px_area1 = 0; int px_area2 = 0; int px_area3 = 0;
		int px_area12 = 0; int px_area13 = 0; int px_area23 = 0; int px_area123 = 0;

		int px_intens1 = 0; int px_intens12 = 0; int px_intens13 = 0; int px_intens123 = 0;

		Rectangle r = roi.getBounds();
		Calibration cal = imp.getCalibration();
		double px_w = cal.pixelWidth;
		double px_h = cal.pixelHeight;
		double px_area=px_w*px_h;

		rt.incrementCounter();
		rt.addLabel("File",impCh0.getTitle());
		rt.addValue("Slice",(int) slice);
		if (LastImage >= 1) rt.addValue(name1 + "_th",(int) thresh1);
		if (LastImage >= 2) rt.addValue(name2 + "_th",(int) thresh2);
		if (LastImage >= 3) rt.addValue(name3 + "_th",(int) thresh3);

		rt.addValue("Xpos",(double) (r.x+r.width/2)*px_w);
		rt.addValue("Ypos",(double) (r.y+r.height/2)*px_h);


		if (countCh == true || intensityCh == true) {
			for (int i=0; i<width; i++) {
			 for (int j=0; j<height; j++) {
			    if (roi.contains (i,j)==true) {
				if (LastImage >= 0) {px_area0++;}

				if (LastImage >= 1) {
				   if ((int)ipCh1.getPixelValue(i,j)>thresh1) {px_area1++; px_intens1+=(int)ipCh1.getPixelValue(i,j);}
				}

				if (LastImage >= 2) {
				   if ((int)ipCh2.getPixelValue(i,j)>thresh2) {px_area2++;}
				   if (((int)ipCh1.getPixelValue(i,j)>thresh1) && ((int)ipCh2.getPixelValue(i,j)>thresh2)) {
					px_area12++;
					px_intens12+=(int)ipCh1.getPixelValue(i,j);
				   }
				}

				if (LastImage >= 3) {
				   if ((int)ipCh3.getPixelValue(i,j)>thresh3) {px_area3++;}
				   if (((int)ipCh1.getPixelValue(i,j)>thresh1) && ((int)ipCh3.getPixelValue(i,j)>thresh3)) {
					px_area13++;
					px_intens13+=(int)ipCh1.getPixelValue(i,j);
				   }
				   if (((int)ipCh2.getPixelValue(i,j)>thresh2) && ((int)ipCh3.getPixelValue(i,j)>thresh3)) {px_area23++;}
				   if (((int)ipCh1.getPixelValue(i,j)>thresh1) && ((int)ipCh2.getPixelValue(i,j)>thresh2)
						&& ((int)ipCh3.getPixelValue(i,j)>thresh3)) {
					px_area123++;
					px_intens123+=(int)ipCh1.getPixelValue(i,j);
				   }
				 }
			   }
			 }
			}

			if (countCh == true) {
				if (LastImage >= 2) rt.addValue(name0+ " area", (double) px_area0*px_area);
				if (LastImage >= 0) rt.addValue(name2+ " area", (double) px_area2*px_area);
				if (LastImage >= 0) rt.addValue(name3+ " area", (double) px_area3*px_area);
				if (LastImage >= 3) rt.addValue(name2 +" & "+ name3 + " area", (double) px_area23*px_area);

				if (LastImage >= 1) rt.addValue(name1+ " area", (double) px_area1*px_area);
				if (LastImage >= 2) rt.addValue(name1 +" & "+ name2 + " area", (double) px_area12*px_area);
				if (LastImage >= 3) rt.addValue(name1 +" & "+ name3 + " area", (double) px_area13*px_area);
				if (LastImage >= 3) rt.addValue(name1 +" & "+ name2 + " & "+ name3 + " area",
					(double) px_area123*px_area);
			}

			if (intensityCh == true) {
				if (LastImage >= 1) rt.addValue(name1+ " Intensity", (double) px_intens1);
				if (LastImage >= 2) rt.addValue(name1 +" ("+ name2 + "+) Intensity", (double) px_intens12);
				if (LastImage >= 2) rt.addValue(name1 +" ("+ name3 + "+) Intensity", (double) px_intens13);
				if (LastImage >= 3) rt.addValue(name1 +" ("+ name2 +" & "+ name3 + "+) Intensity", (double) px_intens123);
			}

			a.displayResults();
			a.updateHeadings();

		}

		//Outline & label the ROIs
		if (LastImage >= 0) drawOutlineLabel(impCh0, roi, slice, outlineCh0, labelCh0);
		if (LastImage >= 1) drawOutlineLabel(impCh1, roi, slice, outlineCh1, labelCh1);
		if (LastImage >= 2) drawOutlineLabel(impCh2, roi, slice, outlineCh2, labelCh2);
		if (LastImage >= 3) drawOutlineLabel(impCh3, roi, slice, outlineCh3, labelCh3);
	}

	else if (e.getSource() == bnHelp) {
		IJ.showMessage("Help",
		"IntraCell plugin version 1.0 for ImageJ program\n"+
		"for analysis of colocalization of nanoparticles (NP) with organelles within the cells\n"+
		"written by David Stepensky (davidst@bgu.ac.il)\n"+
		"January 5, 2011\n"+
		" \n"+
 		"Images of the same sample are required, e.g.: 1) Cell Surface, 2) Nanoparticles, 3) Organelle1, 4) Organelle2\n"+
		" \n"+
		"Update selection - update/selection of the images for analysis\n"+
		"Settings & Output - change of the thresholds for data analysis and output options\n"+
		"Calculate - calculation & output of the results for the pre-selected polygon ROI\n"+
		" \n"+
		"Smooth - applies 3x3 filter to smooth all the slices of the image, is equivalent to Process->Smooth command\n"+
		"LUT->Rainbow2 - changes the LUT of each slice to Rainbow2 (requires rainbow2.lut in ImageJ LUT directory)\n"+
		" \n"+
		"Close - quits the plugin\n");
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

