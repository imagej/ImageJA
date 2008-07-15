/* 	This plugin implements the DUAL-ENERGY (DE) algorithm. It assumes that two
	images, namely a LOW ENERGY (LE) image and a HIGH ENERGY (HE) image, are 
        loaded before starting the DE analysis. For details on the dual-energy technique
        see  "A. Taibi et al, Phys. Med. Biol. 48, 1945-1956, 2003" and references therein.
 
	Acknowledgment: The DE_ plugin is based on the NORMALIZE_ plugin, written by Jeffrey Kuhn.
                        It also makes use of the Panel_Window plugin, written by Wayne Rasband. The
                        help of Francesco Sisini (francescosisini@yahoo.it) is also kindly acknowledged. 
        
                                                        Angelo Taibi
                                                        University of Ferrara
                                                        Italy
                                                        taibi@fe.infn.it

                                                                Ferrara, June 2003
*/


import java.awt.*;
import java.lang.*;
import java.awt.event.*;
import java.awt.image.*;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.*;
import ij.process.FloatProcessor;


public class DE_ implements PlugInFilter {

/** If "none" is selected, then that image is not used in the calculation */
	private static final String strNONE = "-- none --";

	/** Name of LE image */
	private static String strLEName = null;		

	/** Name of HE image */
	private static String strHEName = null;

	/** Mean grey level from the low-energy radiograph of an object of known thickness and composition */
	private static double di0l = 100.0;

	/** Mean grey level from the high-energy radiograph of an object of known thickness and composition */
	private static double di0h = 100.0;

	/** Linear attenuation coefficient at low energy of the basis material A */
	private static double dmual = 0.741;

	/** Linear attenuation coefficient at high energy of the basis material A */
	private static double dmuah = 0.388;

	/** Linear attenuation coefficient at low energy of the basis material B */
	private static double dmubl = 0.423;

	/** Linear attenuation coefficient at high energy of the basis material B */
	private static double dmubh = 0.262;


	/** Thickness of the known material to calculate incident x-ray intensity (in cm) */
	private static double dt = 5.0;

	/** Projection angle in degrees */
	private static double dangle = 45.0;

	/** Coefficient R for the calculation of the projection matrix */
	private static double dR = 1.0;
        
        

	/** ImageJ ID of LE image */
	private int iLEID;							

	/** ImageJ ID of HE image */
	private int iHEID;		
	
	/** The image window to work on */					
	private ImagePlus impImage;

	/** Report what kind of images this plugin can handle to ImageJ */
	public int setup(String arg, ImagePlus imp) 
	{
		impImage = imp;
		return DOES_ALL+NO_CHANGES;
	
	}

	/** Request the user to specify the LE and HE images */
	public boolean getArguments() 
		{
		int[] aiWinList = WindowManager.getIDList();
		if (aiWinList==null) {
			IJ.error("No windows are open.");
			return false;
		}

		String[] astrWinTitles = new String[aiWinList.length + 1];
		astrWinTitles[0] = strNONE;
		for (int i=0; i<aiWinList.length; i++) {
			ImagePlus imp = WindowManager.getImage(aiWinList[i]);
			if (imp != null) {
				astrWinTitles[i+1] = imp.getTitle();
			} else {
				astrWinTitles[i+1] = "";
			}
		}
		if (strLEName == null)
			strLEName = strNONE;
		if (strHEName == null)
			strHEName = strNONE;

		GenericDialog gd = new GenericDialog("Dual-energy analysis", 	IJ.getInstance());

		gd.addMessage("Image selection: " + impImage.getTitle());
		gd.addChoice("LOW-ENERGY Image", astrWinTitles, strLEName);
		gd.addChoice("HIGH-ENERGY Image", astrWinTitles, strHEName);
		gd.addNumericField("I low", di0l, 2);
		gd.addNumericField("I high", di0h, 2);
		gd.addNumericField("µ A low", dmual, 3);
		gd.addNumericField("MU A high", dmuah, 3);
		gd.addNumericField("MU B low", dmubl, 3);
		gd.addNumericField("MU B high", dmubh, 3);
		gd.addNumericField("thickness (cm)", dt, 2);
                gd.addNumericField("projection angle (deg)", dangle, 2);

                int sliderRange = 256;
                Scrollbar contrastSlider, brightnessSlider;
                
        brightnessSlider = new Scrollbar(Scrollbar.HORIZONTAL, sliderRange/2, 1, 0, sliderRange);

                

		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}

		int iIndex;

		di0l = gd.getNextNumber();
		di0h = gd.getNextNumber();
		dmual = gd.getNextNumber();
		dmuah = gd.getNextNumber();
		dmubl = gd.getNextNumber();
		dmubh = gd.getNextNumber();
		dt = gd.getNextNumber();
                dangle = gd.getNextNumber();
                
		iIndex = gd.getNextChoiceIndex();
		if (iIndex == 0) {
			iLEID = 0;
			strLEName = null;
		} else {
			iLEID = aiWinList[iIndex-1];
			strLEName = astrWinTitles[iIndex];
		}

		iIndex = gd.getNextChoiceIndex();
		if (iIndex == 0) {
			iHEID = 0;
			strHEName = null;
		} else {
			iHEID = aiWinList[iIndex-1];
			strHEName = astrWinTitles[iIndex];
		}

		return true;
	}

	
	public void run(ImageProcessor ip) 
	{
        /**
	 * Ask the user for the LE and HE images
	 */
            
		if (!getArguments()) {
			return;
		}

		int iW = ip.getWidth();
		int iH = ip.getHeight();
		int iLen = iW * iH;

		ImagePlus impLE = null;
		ImagePlus impHE = null;

		if (strLEName != null) {
			impLE = WindowManager.getImage(iLEID);
			if (impLE.getType() != impImage.getType()) {
				IJ.error("The low-energy image type does not match this image type.");
				return;
			}
			if ((impLE.getProcessor().getWidth() != iW) || (impLE.getProcessor().getHeight() != iH)) {
				IJ.error("The low-energy image size does not match this image size.");
				return;
			}
		}
		if (strHEName != null) {
			impHE = WindowManager.getImage(iHEID);
			if (impHE.getType() != impImage.getType()) {
				IJ.error("The high-energy image type does not match this image type.");
				return;
			}
			if ((impHE.getProcessor().getWidth() != iW) || (impHE.getProcessor().getHeight() != iH)) {
				IJ.error("The high-energy image size does not match this image size.");
				return;
			}
		}

		if ((impLE == null) && (impHE == null)) {
			IJ.error("Both the LE image and the HE image must be specified. Dual-Energy analysis cannot proceed.");
			return;
		}

             
                /* Calculate the intensity of the incident beam on the flat ROI (the known object) */
		double di0lin=di0l*Math.exp(dmual*dt);
		double di0hin=di0h*Math.exp(dmuah*dt);
		dR = dmuah*dmubl - dmubh*dmual;
		double drad = dangle*Math.PI/180;
		
		
                /* Create the hybrid image */
                
		ImagePlus Hybrid = NewImage.createFloatImage ("Hybrid", iW, iH, 1, NewImage.FILL_BLACK);
		ImageProcessor h_ip = Hybrid.getProcessor();
		
                
                /* Start the calculation of the hybrid image at the projection angle */
                
		float[] Image = (float[]) h_ip.getPixels();
                float[] asImage = new float [iLen];
		short[] asLE = (short[])impLE.getProcessor().getPixels();
		short[] asHE = (short[])impHE.getProcessor().getPixels();
                float[] ml = new float [iLen];
                float[] mh = new float [iLen];
			
			for (int i=0; i<iLen; i++) 
			{
			ml[i] = (float)(Math.log(di0lin/asLE[i]));
			mh[i] = (float)(Math.log(di0hin/asHE[i]));
			}

                               
		int iIndex;
                       
                        
			for (int i=0; i<iLen; i++) 
			{
			Image[i] = (float)((dmubl*Math.cos(drad)-dmual*Math.sin(drad))/dR*mh[i] +
                	(dmuah*Math.sin(drad)-dmubh*Math.cos(drad))/dR*ml[i]);
			}
                        

                // Reset the min and max displayed values
		double min = h_ip.getMin();
		double max = h_ip.getMax();
                h_ip.setMinAndMax(min, max);
                
               /* Create a new window to display the hybrid image at a certain projection angle */
                        
		ImagePlus imp2 = new ImagePlus("Hybrid Image", h_ip);
		CustomCanvas cc = new CustomCanvas(imp2);
		new CustomWindow(imp2, cc,iLen, di0lin, di0hin,impLE,ml,mh);

    }


    class CustomCanvas extends ImageCanvas {

	CustomCanvas(ImagePlus imp) {
		super(imp);
		
	}
	

    }


    class CustomWindow extends ImageWindow implements ActionListener {

        private Button button1;
        private Button buttonChangeAngle;
        private TextField textProjectionAngle;
        private TextField res;
   	private ImagePlus myimp;
   	private int iLen;
   	private double  di0lin, di0hin;
   	private ImagePlus impLE;
   	double drad; 
   	float[] ml;
        float[] mh;
	CustomWindow(ImagePlus imp, ImageCanvas ic,int il,double a,double b,ImagePlus ip,float[] mlr,float[] mhr) {
		super(imp, ic);
		myimp=imp;
		addPanel();
		iLen=il;
		di0lin=a;
		di0hin=b;
		impLE=ip;
		ml=mlr;
		mh=mhr;
	}

        /* Define a new panel to enter a different projection angle so as to recalculate the hybrid image */
        
	void addPanel() {
             
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
                panel.add(new Label("Change projection Angle:"));
		textProjectionAngle = new TextField(Double.toString(dangle));
		panel.add(textProjectionAngle);
		// res=new TextField("0");
		// panel.add( res);
		buttonChangeAngle = new Button("Recalculate");
		buttonChangeAngle.addActionListener(this);
		panel.add(buttonChangeAngle);
                
                
		add(panel);
		pack();
	}
        
        /* Recalculate the hybrid image at a different projection angle */
        
  	private void calcola(){
  		//String d="";
  		//try{
  		
  		String da=textProjectionAngle.getText();
  		
  		ImageProcessor h_ip = myimp.getProcessor(); 
  		float[] Image = (float[]) h_ip.getPixels();
  		      
		int iIndex;
                       
                     
                    	drad= (new Double(da).parseDouble(da))*Math.PI/180;  
			for (int i=0; i<iLen; i++) 
			{
			Image[i] = (float)((dmubl*Math.cos(drad)-dmual*Math.sin(drad))/dR*mh[i] +
                	(dmuah*Math.sin(drad)-dmubh*Math.cos(drad))/dR*ml[i]);
			}
                        
		//d="4";
                
                /* Reset the min and max displayed values */
                
		double min = h_ip.getMin();
		double max = h_ip.getMax();
                h_ip.setMinAndMax(min, max);
                //res.setText(new Double(min).toString());
                
                //--Exported  
                imp.updateAndDraw();
	//}catch(Exception s){res.setText(d+" "+s.toString());}
                
  		
  		
  	}
  
  
        public void actionPerformed(ActionEvent e) {
		Object b = e.getSource();
		calcola();
		if (b==button1) {
			res.setText("b"); 
			
                        
			//imp.updateAndDraw();
		} else {
			
			
			//imp.updateAndDraw();
		}

	}


    }

}
