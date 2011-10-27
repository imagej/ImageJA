/*Version 1.1: typo removed, sigmafilter removed ,  comments in English */

import ij.*;
import ij.plugin.PlugIn;
import ij.gui.*;
import ij.process.*;
import java.awt.*;


public class Anaglyph_ implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		if (imp.getBitDepth()!=8) {
			IJ.error("8-bit image required");
			return;
		}
		ImageProcessor ip = imp.getProcessor();
		int maxshift = 18; //maximum displacement
		int minshift = 6;
		boolean median, smooth, sharpen; //filter used
		boolean parinfo; // optional parameter info
		int output = 0;
		
		//GUI for user input
		GenericDialog input = new GenericDialog("Anaglyph Parameter");
		input.addNumericField("Maximum displacement", 18, 0);
		input.addNumericField("Minimum displacement", 6, 0);
		input.addMessage("Filter (for synthetic second image):");
		input.addCheckbox("Sharpen Filter", false);
		input.addCheckbox("Median Filter", false);
		input.addCheckbox("Smooth Filter", true);		
		input.addCheckbox("Parameter Info", false);
		
		String[] choice = {"Anaglyph", "3D-LCD", "Red only"};
		input.addChoice("Image output:", choice, "Anagyph" );
		
		input.showDialog();
		if (input.wasCanceled()) return;
		
		maxshift = (int)input.getNextNumber();
		minshift = (int)input.getNextNumber();
		sharpen = input.getNextBoolean();
		median = input.getNextBoolean();
		smooth = input.getNextBoolean();		
		parinfo = input.getNextBoolean();
		output = input.getNextChoiceIndex();				
		//generating synthetic image
		ImagePlus redpic = makeRed(ip, maxshift, minshift);		
		ImageProcessor red_ip = redpic.getProcessor();
		
		if (sharpen) red_ip.sharpen();
		if (median) red_ip.medianFilter();
		if (smooth) red_ip.smooth();
		
		if (output == 0) showAnaglyph(red_ip ,ip, maxshift, minshift, 
			median, smooth, sharpen, parinfo);
		if (output == 2) {
			redpic.show();
		        redpic.updateAndDraw();
		};
		if (output == 1) showLCD(red_ip, ip, maxshift, median, smooth, 
			sharpen, parinfo);
		
	}
	
	
	 ImagePlus makeRed(ImageProcessor ip, int maxshift, int minshift) {
		
		int w = ip.getWidth();
		int h = ip.getHeight();
		int offset; // n*offset = first pixel in n-th line
		
		ImagePlus redpic = NewImage.createImage("Red image", w, h, 1, 8,
		NewImage.FILL_BLACK);
		ImageProcessor red_ip = redpic.getProcessor();
		
		byte[] pixelsource = (byte[]) ip.getPixels(); //array for source image
		byte[] pixelred = (byte[])red_ip.getPixels(); //array for synthetic image
				
		for (int y=0; y<h; y++) {
			offset = y*w;
			for (int x=w-1; x>=0; x--) {
				int pos = offset + x;
				int c = 0xff & pixelsource[pos]; //current pixel value
				int thispointshift = Math.round(maxshift * c / 255);
				int colorshift = 0;
				if (thispointshift >= 1) 
					{colorshift = Math.round(c/thispointshift);}
				else {colorshift = 0;};
				
				for (int shift = thispointshift; shift >= 0; shift--) { 
					if (x + minshift + shift <= w-1)
						pixelred[pos + minshift + shift] = (byte)c;
					if (c >= colorshift) {c = (c - colorshift);}
					else {c = 0;};
				}
								
			}
			
		}
		
		return redpic;
		
	}
	
		
	void showAnaglyph(ImageProcessor red_ip, ImageProcessor ip, int maxshift,
	int minshift, boolean median, boolean smooth, boolean sharpen, boolean parinfo) {
		
		int w = ip.getWidth();
		int h = ip.getHeight();
		int offset;
		
		ImagePlus anaglyph = NewImage.createRGBImage("Anaglyph", w, h, 1,
		NewImage.FILL_BLACK);
		ImageProcessor ip_ana = anaglyph.getProcessor();
		
		byte[] pixelOrg = (byte[]) ip.getPixels(); // original image
		byte[] pixelRed = (byte[]) red_ip.getPixels(); // synthetic image
		int[] pixelAna = (int[]) ip_ana.getPixels(); // anaglyph
		
		for (int y=0; y<h; y++) {
			offset = y*w;
			for (int x=0; x<w; x++) {
				int pos = offset + x;
			        int r = 0xff & pixelRed[pos];
				int g = 0xff & pixelOrg[pos];
				int b = 0xff & pixelOrg[pos];
				
				pixelAna[pos] = ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff);
				
			}
			
		}
		
		//maximum displacement and used filters
		//are drawn into the upper left image corner
		if (parinfo) {
			
			String maxVer = "Maximum displacement: ";
			Integer maxi = new Integer(maxshift); 
			String shiftmax = maxi.toString(); 
			
			String minVer = "Minimum displacement: ";
			Integer mini = new Integer(minshift); 
			String shiftmin = mini.toString(); 
			
			ip_ana.drawString(maxVer, 50, 50);
			ip_ana.drawString(shiftmax, 50 + ip_ana.getStringWidth(maxVer), 50);
			ip_ana.drawString(minVer, 50, 60);
			ip_ana.drawString(shiftmin, 50 + ip_ana.getStringWidth(minVer), 60);
			if (median) ip_ana.drawString("+Median", 50, 70);
			if (smooth) ip_ana.drawString("+Smooth", 50, 80);
			if (sharpen) ip_ana.drawString("+Sharpen", 50, 90);
		}
		
		anaglyph.show();
		anaglyph.updateAndDraw();
	}
	
	
	
	//output for the Sharp LL-1513d monitor
	
	void showLCD(ImageProcessor red_ip, ImageProcessor ip, int maxshift,
	boolean median, boolean smooth, boolean sharpen, boolean parinfo) {
		
		int w = ip.getWidth();
		int h = ip.getHeight();
		int offset;
		
		ImagePlus interlanced = NewImage.createRGBImage("3D_LCD", w, h, 1,
		NewImage.FILL_BLACK);
		ImageProcessor ip_lcd = interlanced.getProcessor();
		
		byte[] pixelOrg = (byte[])ip.getPixels(); // original image
		byte[] pixelShift = (byte[])red_ip.getPixels(); // synthetic image
		int[] pixelLCD = (int[]) ip_lcd.getPixels(); // 3D-LCD
		
		for (int y=0; y<h; y++) {
			offset = y*w;
			for (int x=0; x<w; x++) {
				int pos = offset + x;
			        if (x % 2 == 0) {
					//int orgpix = 0xff & pixelOrg[pos];
					//int redpix = 0xff & pixelRed[pos];
					//int red   = (int)(orgpix & 0xff0000)>>16;
					//int green = (int)(redpix & 0x00ff00)>>8;
					//int blue  = (int)(orgpix & 0x0000ff);
					pixelLCD[pos] = ((pixelOrg[pos] & 0xff)<<16)+((pixelShift[pos] & 0xff)<<8) + (pixelOrg[pos] & 0xff);
				}
				else {
					//int orgpix = 0xff & pixelOrg[pos];
					//int redpix = 0xff & pixelRed[pos];
					//int red   = (int)(redpix & 0xff0000)>>16;
					//int green = (int)(orgpix & 0x00ff00)>>8;
					//int blue  = (int)(redpix & 0x0000ff);
					pixelLCD[pos] = ((pixelShift[pos] & 0xff)<<16)+((pixelOrg[pos] & 0xff)<<8) + (pixelShift[pos] & 0xff);
				};
				
			}
			
		}
		
		//maximum displacement and used filters
		//are drawn into the upper left image corner
		if (parinfo) {
			
			String maxVer = "Maximum displacement: ";
			Integer maxi = new Integer(maxshift); 
			String shift = maxi.toString(); 
			
			ip_lcd.drawString(maxVer, 50, 50);
			ip_lcd.drawString(shift, 50 + ip_lcd.getStringWidth(maxVer), 50);
			if (median) ip_lcd.drawString("+Median", 50, 65);
			if (smooth) ip_lcd.drawString("+Smooth", 50, 80);
			if (sharpen) ip_lcd.drawString("+Smooth", 50, 95);
		}
		
		interlanced.show();
		interlanced.updateAndDraw();
	}
	
	
	
}



