import java.awt.*;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

/* Author: Ann Sablina (sablinaaa@mail.ru)
*Description: This plugin automates counting of stress granules (SGs) 
*(http://en.wikipedia.org/wiki/Stress_granule) in images of eucaryotic cells that are flattened on substrate
*when SGs are revealed by a fluorescent marker. Uses ParticleAnalyser functionality. Recognition of SGs is 
*possible even if values of their pixels are less than background brightness in the centre of the cell (as 
*in large CV-1 cells).
*Parameters for HeLa cells (default):
*Number of smoothes: 70
*Number of smoothes after subtraction: 4
*Threshold: 3000
*Parameters for CV-1 cells:
*Number of smoothes: 6
*Number of smoothes after subtraction: 3
*Threshold: 4500
*Not for stacks */

public class SG_counter implements PlugInFilter {
	public int setup(String arg, ImagePlus imp) {
		return DOES_16+DOES_8G+ROI_REQUIRED+SUPPORTS_MASKING+NO_CHANGES;
	}
	/* Source image*/
	ImagePlus imp0;
	ImageProcessor ip0;
	Rectangle roi;
	ImageProcessor mask;
	/* A copy that undergoes subtraction*/
	ImageProcessor ip1;
	ImagePlus imp1;
	/* A smoothed image */
	ImageProcessor ip2;
	ImagePlus imp2;
	/* Number of smoothes */
	short smooth_nr=70;
	short smooth_after_subtraction=4;
	/* Sum of pixel values inside ROI after subtraction (required for normalization)*/
	double sum;
	/* Normalization value for sum of pixel values (arbitrary)*/
	double sum_n=400;
	/* Number of pixels inside ROI (for normalization of total brightness)*/
	int pix_n=0;
	/*Threshold for binarization required for particle analysis*/
	double threshold=3000;
	/*ParticleAnalyzer options*/
	int pa_options = Prefs.getInt("ap.options",ParticleAnalyzer.CLEAR_WORKSHEET)
	 | ParticleAnalyzer.RECORD_STARTS | ParticleAnalyzer.SHOW_SUMMARY
	 | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES |ParticleAnalyzer.SHOW_RESULTS | ParticleAnalyzer.SHOW_OUTLINES;
	int pa_measurments = Analyzer.getMeasurements();
	double pa_minSize=1, pa_maxSize=10000;
	double pa_minCirc=0.2, pa_maxCirc=1.0;
	
	public void run(ImageProcessor ip) {
		ip0=ip;
		imp0=WindowManager.getCurrentImage();
		roi = ip0.getRoi();
		mask=ip0.getMask();
		GenericDialog d= new GenericDialog("Stress granule counter");
		d.addCheckbox("Subjective estimate: the cell has SG", false);
		d.addCheckbox("The whole cell fits into the image", true);
		d.addNumericField("Number of smoothes:", smooth_nr, 0);
		d.addNumericField("Number of smoothes after subtraction:", smooth_after_subtraction, 0);
		d.addNumericField("Threshold:", threshold, 0);
		d.addNumericField("Minimum particle size, pixels:", pa_minSize, 0);
		d.addNumericField("Maximum particle size, pixels:", pa_maxSize, 0);
		d.addNumericField("Minimum particle circularity:", pa_minCirc, 2);
		d.addCheckbox("Show intermediate steps", false);
		d.showDialog();
		boolean has_SG=d.getNextBoolean();
		boolean cell_fits=d.getNextBoolean();
		boolean show_int=d.getNextBoolean();		
		short has_sg=(short)(has_SG?1:0);
		short cell_fits1=(short)(cell_fits?1:0);
		if (d.wasCanceled())
		 return ;
		 else {
			 smooth_nr=(short)d.getNextNumber();
			 smooth_after_subtraction=(short)d.getNextNumber();
			 threshold=d.getNextNumber();
			 pa_minSize=d.getNextNumber();
			 pa_maxSize=d.getNextNumber();
			 pa_minCirc=d.getNextNumber();
			 		 
		 }	
		/* Step #1: Smoothing*/
		duplicate(imp0); 
		imp1=WindowManager.getCurrentImage();
		ip1=imp1.getProcessor();
		duplicate(imp1);
		imp2=WindowManager.getCurrentImage();
		ip2=imp2.getProcessor();
		int i=0;
		while (i<smooth_nr){
		ip2.smooth();
		i++;}
		imp2.updateAndDraw();
		/* Step#2: Subtraction*/
		ip1.copyBits(ip2, 0, 0, Blitter.SUBTRACT);
		i=0;
		while (i<smooth_after_subtraction){
			ip1.smooth();
			i++;}
		imp1.updateAndDraw(); 
		/* Step#3: Brightness normalization*/
		/* Collecting the total brightness ("sum") and pixel number ("pix_n") */
		sum=0;
		int height=ip1.getHeight();
		int width=ip1.getWidth();
		for (int v = 0; v < height; v++) {// process all pixels inside the ROI
			for (int u = 0; u < width; u++) {
				if (mask.getPixel(u, v) > 0) {
					sum+=ip1.getPixelValue(u, v);
					pix_n++;
		}}}
		int ratio=(int)((sum_n*pix_n)/sum);
		/* Normalization */
		for (int v = 0; v < height; v++) {
			for (int u = 0; u < width; u++) {
				if (mask.getPixelValue(u, v) > 0) {	
				ip1.putPixelValue(u, v, ratio*ip1.getPixelValue(u,v));
				}else{
					ip1.putPixelValue(u, v, 0); // pixels outside the ROI are not displayed in the resulting image
				}}}
		imp1.updateAndDraw(); 		
		/* Step#4: Threshold setting*/
		ip1.setThreshold(threshold, 100000, ImageProcessor.RED_LUT);
		/* Step#5: Particle counting*/
		imp1.getWindow().setAlwaysOnTop(true);
		ParticleAnalyzer pa=new ParticleAnalyzer(pa_options, pa_measurments, null, pa_minSize, pa_maxSize, pa_minCirc, pa_maxCirc);		
			pa.analyze(imp1, ip1);
			if (!show_int){
				imp1.getWindow().close();
				imp2.getWindow().close();
				}
			String name="SG counter output";
			TextWindow tw = (TextWindow)WindowManager.getFrame(name);
			String results=pix_n+"\t"+has_sg+"\t"+cell_fits1+"\t"+smooth_nr+"\t"
				+smooth_after_subtraction+"\t"+threshold+"\t"+pa_minSize+"\t"
				+pa_maxSize+"\t"+pa_minCirc+"\t"+imp1.getTitle();
			if (tw==null){
				tw=new TextWindow (name, "Cell area (pixels)\tSubjective SG presence\tCell fits the image\tNr of smoothes\tNr of smoothes after subtr\tThreshold\tmin size\tmax size\tmin circ\tImage name", results, 500, 500);
			}else{
				tw.append(results);
			}
			WindowManager.getFrame("Summary").setAlwaysOnTop(true);
			TextWindow r = (TextWindow)WindowManager.getFrame("Results");
			if(r!=null) {				
				TextWindow s=new TextWindow (imp1.getTitle()+" cell with area="+pix_n+" SG count", r.getTextPanel().getColumnHeadings(), r.getTextPanel().getText(), 
					500, 500);
				s.getTextPanel().append("cell area="+pix_n);
				s.setVisible(true);
			}
			ImageWindow f = (ImageWindow)WindowManager.getFrame("Drawing of "+imp1.getTitle());
			if (f!=null){
				duplicate(f.getImagePlus());
				WindowManager.getCurrentImage().setTitle(imp1.getTitle()+" cell with area="+pix_n+"SG drawing");
				f.close();
			}
			
		}
	
	/* Code from Duplicater, IJ.run() doesn't run in PlugInFilter*/
	public void duplicate(ImagePlus imp) {
		String title = imp.getTitle();
		String newTitle = WindowManager.getUniqueName(title);
		ImagePlus imp2;
		Roi roi = imp.getRoi();
			ImageProcessor ip2 = imp.getProcessor().crop();
			imp2 = imp.createImagePlus();
			imp2.setProcessor(newTitle, ip2);
			String info = (String)imp.getProperty("Info");
			if (info!=null)
				imp2.setProperty("Info", info);
		imp2.show();
		if (roi!=null && roi.isArea() && roi.getType()!=Roi.RECTANGLE)
			imp2.restoreRoi();
	}

}
