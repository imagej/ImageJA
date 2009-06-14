/*
File: Smart_Projector.java
Written: mars 2009
Author: Fanny Serman (fanny (dot) serman (at) gmail (dot) com
written with Notepad++ http://notepad-plus.sourceforge.net/fr/site.htm

Permission to use, copy, modify, and distribute this software for any purpose 
without fee is hereby granted, provided that this entire notice is included in 
all copies of any software which is or includes a copy or modification of this 
software and in all copies of the supporting documentation for such software.
Any for profit use of this software is expressly forbidden without first
obtaining the explicit consent of the author. 
THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED WARRANTY. 
 
*/


import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.io.*;
import ij.io.*;
import java.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.awt.event.*;
import ij.measure.*;
import java.awt.event.*;
import java.lang.*;


/*This plugin is developed to project "smartly" several series of stacks 
*in order to obtain quickly a good projection of your time-lapse movies. 
*
*For each slice of the stack, the plugin will calculate the function 
*"focus_index" whose maxima should correspond to the sharpest structures 
*in the stack. If your stack corresponds to a thin and simple specimen, 
*the focus_index will have only one maximum along the Z (which will 
*hopefully be the structure you really want to see and not the noise you 
*have acquired under and above your tissue / organ...) Projection can be 
*made using this maximum (= "autofocus" slice) with different options : 
*
*- around the autofocus: the given parameter is the number of slices 
*projected around the maximum (3 or 4 will correspond to the projection 
*of 3 slices : the one with the maximal focus_index, one before and one 
*after). You can activate tracking of the maximum which forces the 
*program to search the maximum in the next stack only in the slices close 
*to previous found maximum index. 
*
*- near a % of the autofocus: keeps for 
*the projection all the slices which have a focus_index at least equal to 
*a certain percentage of the maximum (can give some rough results if you 
*have several interesing structures in you stack) 
*- around the first peak 
*of autofocus: sometimes two structures are recognised (for example 
*membranes and nuclei) and this option forces the program to keep only 
*the first maximum and to project around it (the % parameter checks that 
*the first maximum is big enough to be considered). 
*
*When you launch 
*Smart_Projector, the plugin asks you to open one file among your 
*selected directory (as in the Import>ImageSequence command of ImageJ). 
*Then you give the number of different stacks you want to proceed, the 
*kind of projection (same options as in the Z-project plugin of ImageJ) 
*and the option of projection (see above). 
*
*You can avoid all calculation 
*by choosing to project all slices of each stack. Can be handy if you 
*have several kind of stacks (with different number of slices in each) 
*and can' t directly use Concatenate_ and Grouped_ZProjector to compile 
*your film. Can help too if your files are too big to be all concatenated 
*because the plugin will handle each one separately. Example : 
*
*See also : for more information about the calculation of focus_index, 
*see " Digital autofocus methods for automated microscopy", Shen F, 
*Hodgson L, Hahn K., Methods Enzymol. 2006;414:620-32. */ 



public class Smart_Projector implements PlugIn, ItemListener {

	/** Strings denoting the possible projection methods.  These should
	*  be the same as those supported by ZProjector. */
	// Note: This will need to be updated if the projection methods supported
	// by ZProjector changes. */
	static public final String[] methodStrings ={"Average Intensity", "Max Intensity", "Min Intensity","Sum slices","Standard deviation", "Median"};
	static private int defaultMethod = ZProjector.MAX_METHOD;
	static private int progress = 0 ;
	static private int finalprogress = 1 ;
	// Pairs of instance / static variables for user options

	Vector checkboxes ;
	Vector numericfields ;
	Checkbox allstacks ;
	Checkbox normalize ;
	Checkbox peakautofocus ;
	Checkbox tracklisten ;
	Checkbox criteriumautofocus ;
	Checkbox lastoption ;
	Checkbox last2option ;
	TextField  num1;
	TextField  num2;
	TextField  num3;
	TextField  num2A;
	TextField  num4;
	TextField  num5;
	TextField  num6;
	TextField  num7;	
	
	public Smart_Projector() {;} //Nothing to do.


	//---------------------------------------------------------------------------------------------------
	//prog principal

	/* This method will be called when the plugin is loaded.
	*/
	public void run(String arg) {
		
		//*****************************
		//kernel definition
		float[] kernel={-1,0,1};
		int lengkernel=3; 
		int heigkernel=1;
		//*******************************
		//other 2D kernel
		//float[] kernel={{-1,-2,1},{-2,12,-2},{-1,-2,-1}};
		//int lengkernel=3; 
		//int heigkernel=3;

		//같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같�
		// first dialog to select directory 
		// the user opens whatever images he wants in the good directory
		OpenDialog od = new OpenDialog("Open image ...", arg);
		if( od.getFileName()==null ) return; //The user pushed the cancel button.
		String initialfilmname = od.getFileName(); // get film name to initialize output
		//IJ.write("debug-initialfilmname-"+initialfilmname);
		String directory = od.getDirectory(); // get directory name
		String[] fileslist2 = (new File(directory)).list(); // get list of files in the directory
		if (fileslist2==null)
		return;
		fileslist2 = sortFileList (fileslist2); // sort files
		//for(int i=0;i<fileslist2.length;i++){IJ.write("debugfileslist-"+fileslist2[i]);}
		int filetypet = (new Opener()).getFileType(directory+initialfilmname);
		//같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같
		//Builds a second dialog to query users about the number of different films
		//and the number of projections they want
		GenericDialog gd = new GenericDialog("Smart projection :)");
		gd.addNumericField("Number of films with different Z-stack size", 1, 0);
		gd.addChoice("Projection Type:",methodStrings,methodStrings[defaultMethod]);
		gd.addCheckbox("Project all the stack", false);
		gd.addCheckbox("Normalize intensity (read documentation first)", false);
		gd.addMessage("________________________________________________________");
		gd.addCheckbox("Project around the autofocus", true);
		gd.addNumericField("Number of projections:",3,0);
		gd.addCheckbox("Activate tracking", false);
		gd.addNumericField("+/- n slices : n =",3,0);
		gd.addNumericField("Force beginning slice",0,0);
		gd.addMessage("________________________________________________________");
		gd.addCheckbox("Project near a % of the autofocus", false);
		gd.addNumericField("% of autofocus",75,0);
		gd.addMessage("      (100% = keep only the autofocused slice / 0 % = project all slices)");
		gd.addMessage("________________________________________________________");
		gd.addMessage("If you have two structures in the stack: ");
		gd.addCheckbox("Project around the first peak of autofocus", false);
		gd.addNumericField("Number of projections:",3,0);
		gd.addNumericField("Max difference between peaks (in %):",50,0);
		gd.addCheckbox("Project around the second peak of autofocus", false);
		gd.addNumericField("Number of projections:",3,0);
		gd.addNumericField("Max difference between peaks (in %):",50,0);
		
		//add listeners
		checkboxes = gd.getCheckboxes();
		allstacks = (Checkbox)checkboxes.elementAt(0);
		allstacks.addItemListener(this);
		normalize = (Checkbox)checkboxes.elementAt(1);
		normalize.addItemListener(this);
		peakautofocus = (Checkbox)checkboxes.elementAt(2);
		peakautofocus.addItemListener(this);
		tracklisten = (Checkbox)checkboxes.elementAt(3);
		tracklisten.addItemListener(this);
		criteriumautofocus = (Checkbox)checkboxes.elementAt(4);
		criteriumautofocus.addItemListener(this);
		lastoption = (Checkbox)checkboxes.elementAt(5);
		lastoption.addItemListener(this);
		last2option = (Checkbox)checkboxes.elementAt(6);
		last2option.addItemListener(this);
		
		numericfields = gd.getNumericFields();
		num1 = (TextField)numericfields.elementAt(1);
		num2 = (TextField)numericfields.elementAt(2);
		num3 = (TextField)numericfields.elementAt(4);
		num2A = (TextField)numericfields.elementAt(3);
		num4 = (TextField)numericfields.elementAt(5);
		num5 = (TextField)numericfields.elementAt(6);
		num6 = (TextField)numericfields.elementAt(7);
		num7 = (TextField)numericfields.elementAt(8);
		
		num2.setEnabled(false);
		num3.setEnabled(false);
		num2A.setEnabled(false);
		num4.setEnabled(false);
		num5.setEnabled(false);
		num6.setEnabled(false);
		num7.setEnabled(false);
		
		gd.showDialog();
		if( gd.wasCanceled() ) return; //The user pushed the cancel button
		boolean all_option = (boolean)gd.getNextBoolean();
		boolean normoption = (boolean)gd.getNextBoolean();
		boolean peak_option = (boolean)gd.getNextBoolean();
		boolean tracking = (boolean)gd.getNextBoolean();
		boolean criterium_option = (boolean)gd.getNextBoolean();
		boolean last_option = (boolean)gd.getNextBoolean();		
		int filmnumber = (int)gd.getNextNumber(); // get number of films
		int projnumber = (int)gd.getNextNumber(); // get proj number
		int trackingnumber = (int)gd.getNextNumber(); // get number of slices for tracking
		int beginnumber = (int)gd.getNextNumber(); // get beginning number for tracking
		double criterium = (double)gd.getNextNumber(); // get criterium of projection
		int firstprojnumber = (int)gd.getNextNumber(); // get proj number for first peak
		double paraecart = (double)gd.getNextNumber(); // get proj number for first peak
		int secondprojnumber = (int)gd.getNextNumber(); // get proj number for first peak
		double secondparaecart = (double)gd.getNextNumber(); // get proj number for first peak
		defaultMethod = gd.getNextChoiceIndex(); // get method of projection
		//error if user do not put criterium between 0 and 1
		if (criterium >100 || criterium < 0) {
			IJ.error("Criterium no included between 0 and 100");
			return;
		}
		//같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같�
		//같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같
		// third dialog to pu root names
		GenericDialog gd2 = new GenericDialog("Write your file names");
		if (filmnumber == 1){
			if (initialfilmname.length()>14){
				gd2.addStringField("File name contains ", initialfilmname.substring(0,9), 11);
			} else{
				gd2.addStringField("File name contains ", initialfilmname.substring(0,initialfilmname.length()-4), 11);
			}
		} else {
			for (int l=0; l<filmnumber;l++){
				gd2.addStringField("File name "+(l+1)+" contains ", "", 40);
			}	
		}
		gd2.showDialog();
		if( gd2.wasCanceled() ) return; //The user pushed the cancel button
		//같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같같�
		//initialize the output stack
		ImagePlus initialimage;
		ImageStack out_stack;
		if (filetypet == 0){//condition for non-tiff files (lsm for example)
			//same program but handle other files than tiff and stk, for example lsm
			//--------------------------------------------------------------------------------------------------
			//initialisation of out_stack : the final stack of projected images
			IJ.open(directory+initialfilmname);
			int[] listeID = WindowManager.getIDList();
			int maxid = 0;
			for (int tempo=0 ; tempo<listeID.length ; tempo++){
				if (listeID[tempo] < maxid) {
					maxid=listeID[tempo];
				}
			}
			IJ.selectWindow(maxid);
			//IJ.selectWindow(initialfilmname+" Channel : Ch1");
			//IJ.selectWindow(initialfilmname);
			initialimage = WindowManager.getCurrentImage();			
			out_stack = initialimage.createEmptyStack();
			initialimage.close();
			//ImageWindow imagewin = initialimage.getWindow();
			//imagewin.close();
			//WindowManager.closeAllWindows();
		}else{// condition for normal files, tiff for example
			initialimage = (new Opener()).openImage(directory,initialfilmname);
			out_stack = initialimage.createEmptyStack();
		}
		//get image type for future condition to simplify calculs for 16-bits images
		int type = initialimage.getType();
		ImageStack initial_stack = initialimage.getStack();
		//--------------------------------------------------------------------------------------------------
		//we launch a loop to do operations on each group of films
		String[] filmname = new String[filmnumber];	//filmname is a table of strings containing filter for each group of films
		if( beginnumber < 0 || beginnumber > initial_stack.getSize()){IJ.error("Tracking begin number is not included between 0 and number of slices ");return;}
		int paratracking = -1;
		if (beginnumber != 0 ) {paratracking = beginnumber;}
		for (int i = 0; i < filmnumber; i++){
			String[] fileslist = (new File(directory)).list();
			fileslist = sortFileList (fileslist);
			
			filmname[i] = gd2.getNextString();
			//builds lists that contains file names of each group 
			if (filmname[i]!=null && (filmname[i].equals("") || filmname[i].equals("*")))
			filmname[i] = null;
			if (filmname[i]==null) {
				IJ.error("You did not enter a file name");
				return;
			}	
			int filteredImages = 0;
			for (int m=0; m<fileslist.length; m++) {
				if (fileslist[m].indexOf(filmname[i])>=0 && fileslist[m].endsWith(".txt") == false){
					filteredImages++;
				}
				else {
					fileslist[m] = null;
				}
			}
			if (filteredImages==0) {
				IJ.error("None of the "+fileslist.length+" files contain\n the string '"+filmname[i]+"' in their name.");
				return;
			}
			String[] grouplist = new String[filteredImages];
			int j = 0;
			for (int k=0; k<fileslist.length; k++) {
				if (fileslist[k]!=null)
				grouplist[j++] = fileslist[k];
			}
			//the list containing all files from group i is grouplist
			//for each stack of group list, we will open the stack, 
			//calculate the max of stddev after filtering high frequencies
			//and then project around this max on the original stack
			//we will save the image in a new imageplus exitimage that will be show to the user at the end
			//----------------------------------------------------------------------------------------------------
			progress = 0;
			finalprogress = grouplist.length;
			for (int n=0; n<grouplist.length; n++) {
				//we open the stack as an imageplus and convert it in a imagestack to be able
				//to process each slice
				//perraps possible to do much simpler ?
				//we do it twice to have normal and convoluted imag
				progress++;
				IJ.showProgress(progress, finalprogress);
				IJ.showStatus("-------------------- slice "+progress+" / "+finalprogress+" (group "+(i+1)+")");
				
				ImagePlus improjectionplus;
				if (filetypet == 0){
					IJ.open(directory+grouplist[n]);
					int[] listeID = WindowManager.getIDList();
					int maxid = 0;
					for (int tempo=0 ; tempo<listeID.length ; tempo++){
						if (listeID[tempo] < maxid) {
							maxid=listeID[tempo];
						}
					}
					IJ.selectWindow(maxid);
					//IJ.selectWindow(grouplist[n]+" Channel : Ch1");
					improjectionplus = WindowManager.getCurrentImage();
					WindowManager.removeWindow(WindowManager.getFrontWindow());
				}else{
					improjectionplus = (new Opener()).openImage(directory, grouplist[n]);
				}
				IJ.showProgress(progress, finalprogress);
				IJ.showStatus("-------------------- slice "+progress+" / "+finalprogress+" (group "+(i+1)+")");
				
				//-----------------------------------------------------------------------------
				//projection of all slices if user choose to do that
				if (all_option == true){
					//the goal is to project all slices of imageplus
					ZProjector zproj = new ZProjector(improjectionplus);
					zproj.setStartSlice(1);
					zproj.setStopSlice(improjectionplus.getStackSize());
					zproj.setMethod(defaultMethod);
					zproj.doProjection();
					ImagePlus projection =  zproj.getProjection();
					ImageProcessor improc = projection.getProcessor();
					//IJ.write("debug-filmname"+filmname[i]);
					out_stack.addSlice("Film-"+filmname[i]+"-proj-"+(n+1), improc);
					IJ.showProgress(progress, finalprogress);
					IJ.showStatus("-------------------- slice "+progress+" / "+finalprogress+" (group "+(i+1)+")");
				}//end of if all_option == true
				else{		
					//---------------------------------------------------------------------------------------
					//calcul of focus index for each slice of the stack
					//we use 8-bit to calculate focus index but we use 16bit to project
					ImagePlus imageplus;
					ImagePlus convoluplus;
					if (filetypet == 0){
						IJ.open(directory+grouplist[n]);
						int[] listeID = WindowManager.getIDList();
						int maxid = 0;
						for (int tempo=0 ; tempo<listeID.length ; tempo++){
							if (listeID[tempo] < maxid) {
								maxid=listeID[tempo];
							}
						}
						IJ.selectWindow(maxid);
						//IJ.selectWindow(grouplist[n]+" Channel : Ch1");
						imageplus = WindowManager.getCurrentImage();
						WindowManager.removeWindow(WindowManager.getFrontWindow());
					}else{
						imageplus = (new Opener()).openImage(directory, grouplist[n]);
					}
					IJ.showProgress(progress, finalprogress);
					IJ.showStatus("-------------------- slice "+progress+" / "+finalprogress+" (group "+(i+1)+")");
					if(type==0){//condition for 8-bit files
					}else{//condition for 16 bits to economize memory
						noshowconvertToGray8(imageplus);}
					ImageStack image = imageplus.getStack();
					if (filetypet == 0){
						IJ.open(directory+grouplist[n]);
						int[] listeID = WindowManager.getIDList();
						int maxid = 0;
						for (int tempo=0 ; tempo<listeID.length ; tempo++){
							if (listeID[tempo] < maxid) {
								maxid=listeID[tempo];
							}
						}
						IJ.selectWindow(maxid);
						//IJ.selectWindow(grouplist[n]+" Channel : Ch1");
						convoluplus = WindowManager.getCurrentImage();
						WindowManager.removeWindow(WindowManager.getFrontWindow());
					}else{
						convoluplus = (new Opener()).openImage(directory, grouplist[n]);
					}
					IJ.showProgress(progress, finalprogress);
					IJ.showStatus("-------------------- slice "+progress+" / "+finalprogress+" (group "+(i+1)+")");
					if(type==0){//condition for 8-bit files
					}else{//condition for 16 bits to economize memory
						noshowconvertToGray8(convoluplus);}
					ImageStack convolu = convoluplus.getStack();
					double[] focusindex = new double[image.getSize()];
					
					int firstnumero;
					int lastnumero;
					
					//implements slices to be looked at, when tracking is activated
					
					if (tracking == true && paratracking >= 0){
						if( (paratracking-trackingnumber)<0){firstnumero = 0 ;}else{firstnumero =paratracking-trackingnumber;}
						if( (paratracking+trackingnumber)>image.getSize()-1){lastnumero = image.getSize();}else{lastnumero = paratracking+trackingnumber+1;}
					}else{
						firstnumero = 0 ;
						lastnumero = image.getSize();
					}
					if (normoption == true){
						for (int numero=firstnumero; numero<lastnumero ; numero++){					
							ImageProcessor convolusousimage_ip = convolu.getProcessor(numero+1);
							convolusousimage_ip.convolve(kernel,lengkernel, heigkernel );
							byte[] convolupixels = (byte[]) convolusousimage_ip.getPixels();
							double temp1=0;
							for (int ref=0; ref < convolupixels.length ; ref++){
								temp1 = temp1 + (convolupixels[ref] & 0xff)*(convolupixels[ref] & 0xff);
							}
							focusindex[numero] = temp1;
						}
					}else{// end of if(normoption == true) == if (normoption == false)
						for (int numero=firstnumero; numero<lastnumero ; numero++){
							// we extract image processor for the 2 images
							ImageProcessor sousimage_ip = image.getProcessor(numero+1);						
							ImageProcessor convolusousimage_ip = convolu.getProcessor(numero+1);
							//we convolve with kernel which can be changed and is implemented in the beginning
							convolusousimage_ip.convolve(kernel,lengkernel, heigkernel );
							//lines if we want to show images
							//ImagePlus convolusousimage = new ImagePlus("", convolusousimage_ip);
							//ImagePlus sousimage = new ImagePlus("", sousimage_ip);
							//convolusousimage.show();
							//sousimage.show();
							//we get pixels from original and filtered images
							byte[] convolupixels = (byte[]) convolusousimage_ip.getPixels();
							byte[] pixels = (byte[]) sousimage_ip.getPixels();
							//add to debug to osee image value
							//for (int index=0; index<pixels.length;index++){
							//	IJ.write(index+"pixels"+pixels[index]+"convolu"+convolupixels[index]);
							//	}
							double temp1=0;
							double temp2=0;
							
							for (int ref=0; ref < pixels.length ; ref++){
								temp1 = temp1 + (convolupixels[ref] & 0xff)*(convolupixels[ref] & 0xff);
								temp2 = temp2 + (pixels[ref] & 0xff );
								//add to calculate standard deviation and mean gray value of images
								//temp4 = temp4 + (convolupixels[ref] & 0xff);
							}
							focusindex[numero] = temp1*100000000/(temp2*temp2);
							
						}
					}// end of if (normoption == false)
					if (filetypet == 0){
						imageplus.close();
						convoluplus.close();
					}
					//..........................................................
					//calcul of the maximum of focusindex and of the corresponding number
					double maxfocus = focusindex[firstnumero];
					int indicemax = firstnumero;
					for (int numero=firstnumero+1 ; numero < lastnumero ; numero++){
						if (focusindex[numero] > maxfocus){
							maxfocus = focusindex[numero];	
							indicemax = numero;
						}
					}
					//implements new paratracking for next stack
					paratracking = indicemax;
					
					//........................................................
					if( peak_option ==true){
						//the goal is to project slices around the index indicemax
						ZProjector zproj = new ZProjector(improjectionplus);
						if((indicemax-(int)(projnumber/2))<0){
							zproj.setStartSlice(1);
						}
						else{
							zproj.setStartSlice((indicemax-(int)(projnumber/2)+1));
						}
						if((indicemax+(int)(projnumber/2))>(improjectionplus.getStackSize()-1)){
							zproj.setStopSlice(improjectionplus.getStackSize());
						}
						else{
							zproj.setStopSlice((indicemax+(int)(projnumber/2)+1));
						}
						zproj.setMethod(defaultMethod);
						//if (isRGB)
						//zproj.doRGBProjection();
						//else
						zproj.doProjection();
						ImagePlus projection =  zproj.getProjection();
						ImageProcessor improc = projection.getProcessor();
						out_stack.addSlice("Film-"+filmname[i]+"-proj-"+(n+1), improc);
						IJ.showProgress(progress, finalprogress);
						IJ.showStatus("-------------------- slice "+progress+" / "+finalprogress+" (group "+(i+1)+")");
						
					}//end of peak_option==true
					else{
						if ( criterium_option == true){
							//in this case we project only slices with focusindex>(focusindex*criterium)
							//so we put black images in others
							ImageStack stackimprojectionplus = improjectionplus.getStack();
							for(int numero=0 ; numero<focusindex.length ; numero++){
								if (focusindex[numero]<maxfocus*criterium/100){
									ImageProcessor improjectionplus_ip = stackimprojectionplus.getProcessor(numero+1);
									if(type==0){
										byte[] ipppixels = (byte[]) improjectionplus_ip.getPixels();
										for(int e = 0 ; e <ipppixels.length ; e++){
											ipppixels[e]= (byte)(0);
										}
									}
									else{
										short[] ipppixels = (short[]) improjectionplus_ip.getPixels();
										for(int e = 0 ; e <ipppixels.length ; e++){
											ipppixels[e]= (short)(0);
										}
									}
								}
							}
							ZProjector zproj = new ZProjector(improjectionplus);
							zproj.setStartSlice(1);
							zproj.setStopSlice(improjectionplus.getStackSize());
							zproj.setMethod(defaultMethod);
							zproj.doProjection();
							ImagePlus projection =  zproj.getProjection();
							ImageProcessor improc = projection.getProcessor();
							out_stack.addSlice("Film-"+filmname[i]+"-proj-"+(n+1), improc);
							IJ.showProgress(progress, finalprogress);
							IJ.showStatus("-------------------- slice "+progress+" / "+finalprogress+" (group "+(i+1)+")");
						}else{
							
							//case where we want to project the first of two maximal peaks
							//we search for a second maxima near enough to the first one (for example 50% of the first one
							//if this second maxima does nt exist, we keep the only maxima
							//if it exists, we take the first of the two maxima to project around
							double secondmaxfocus ;
							secondmaxfocus = 0 ;
							int secondindicemax ;
							secondindicemax = indicemax ;
							//IJ.write("indicemax "+indicemax+" maxfocus "+maxfocus);
							//for(int nummer = 0; nummer<focusindex.length; nummer++){
							//IJ.write(nummer+" focusindex "+focusindex[nummer]);}
							if (indicemax <= 1){
								//IJ.write("1er cas");
								for (int numero = indicemax+2 ; numero < focusindex.length ; numero++){
									if(focusindex[numero]>focusindex[numero-1] && focusindex[numero] > secondmaxfocus){
										secondmaxfocus = focusindex[numero];
										secondindicemax = numero;
									}
									//IJ.write(numero+" 2indicemax "+secondindicemax+" 2maxfocus "+secondmaxfocus);
								}
							}else{
								if (indicemax >= focusindex.length-2){
									//IJ.write("2e cas");
									for (int numero = indicemax-1 ; numero > 0 ; numero--){
										if(focusindex[numero-1]>focusindex[numero] && focusindex[numero-1] > secondmaxfocus){
											secondmaxfocus = focusindex[numero-1];
											secondindicemax = numero-1;
										}
										//IJ.write(numero+" 2indicemax "+secondindicemax+" 2maxfocus "+secondmaxfocus);
									}
								}else{
									//IJ.write("3e cas");
									for (int numero = indicemax+2 ; numero < focusindex.length ; numero++){
										if(focusindex[numero]>focusindex[numero-1] && focusindex[numero] > secondmaxfocus){
											secondmaxfocus = focusindex[numero];
											secondindicemax = numero;
										}
									}
									for (int numero = indicemax-1 ; numero > 0 ; numero--){
										if(focusindex[numero-1]>focusindex[numero] && focusindex[numero-1] > secondmaxfocus){
											secondmaxfocus = focusindex[numero-1];
											secondindicemax = numero-1;
										}
									}
								}
							}
							if (last_option == true){
								//condition to check if the second peak is big enough and if it is in the first position
								//if not we keep the maximal peak
								if ( secondmaxfocus < (1-paraecart/100) * maxfocus || secondindicemax > indicemax){
									secondindicemax = indicemax;
								}
								//the goal is to project slices around the index secondindicemax
								ZProjector zproj = new ZProjector(improjectionplus);
								if((secondindicemax-(int)(firstprojnumber/2))<0){
									zproj.setStartSlice(1);
								}
								else{
									zproj.setStartSlice((secondindicemax-(int)(firstprojnumber/2)+1));
								}
								if((secondindicemax+(int)(firstprojnumber/2))>(improjectionplus.getStackSize()-1)){
									zproj.setStopSlice(improjectionplus.getStackSize());
								}
								else{
									zproj.setStopSlice((secondindicemax+(int)(firstprojnumber/2)+1));
								}
								zproj.setMethod(defaultMethod);
								//if (isRGB)
								//zproj.doRGBProjection();
								//else
								zproj.doProjection();
								ImagePlus projection =  zproj.getProjection();
								ImageProcessor improc = projection.getProcessor();
								//IJ.write("debug-filmname"+filmname[i]);
								out_stack.addSlice("Film-"+filmname[i]+"-proj-"+(n+1), improc);
								IJ.showProgress(progress, finalprogress);
								IJ.showStatus("-------------------- slice "+progress+" / "+finalprogress+" (group "+(i+1)+")");
							}else{//end of if (last_option == true)
								//condition to check if the second peak is big enough and if it is in the first position
								//if not we keep the maximal peak
								if ( secondmaxfocus < (1-secondparaecart/100) * maxfocus || secondindicemax < indicemax){
									secondindicemax = indicemax;
								}
								//the goal is to project slices around the index secondindicemax
								ZProjector zproj = new ZProjector(improjectionplus);
								if((secondindicemax-(int)(secondprojnumber/2))<0){
									zproj.setStartSlice(1);
								}
								else{
									zproj.setStartSlice((secondindicemax-(int)(secondprojnumber/2)+1));
								}
								if((secondindicemax+(int)(secondprojnumber/2))>(improjectionplus.getStackSize()-1)){
									zproj.setStopSlice(improjectionplus.getStackSize());
								}
								else{
									zproj.setStopSlice((secondindicemax+(int)(secondprojnumber/2)+1));
								}
								zproj.setMethod(defaultMethod);
								//if (isRGB)
								//zproj.doRGBProjection();
								//else
								zproj.doProjection();
								ImagePlus projection =  zproj.getProjection();
								ImageProcessor improc = projection.getProcessor();
								//IJ.write("debug-filmname"+filmname[i]);
								out_stack.addSlice("Film-"+filmname[i]+"-proj-"+(n+1), improc);
								IJ.showProgress(progress, finalprogress);
								IJ.showStatus("-------------------- slice "+progress+" / "+finalprogress+" (group "+(i+1)+")");
							}//end of else (last_option == false)
						}//end of else (criterium_option == false)
					}//end of else (peak_option == false)
				}//end of else (all_option == false)
				//----------------------------------------------------------------------------------------------------
				if (filetypet == 0){improjectionplus.close();}
			}//end of loops for all stacks inside a group
		}//end of loop for each groups of files
		
		ImagePlus out_image = new ImagePlus("Projection", out_stack);	
		out_image.show();

	}

	
	
	//function sortfilelist who sorts all files in selected folder
	String[] sortFileList(String[] list) {
		int listLength = list.length;
		//IJ.write("debug "+listLength);
		int first = listLength>1?1:0;
		//IJ.write("debug "+first);
		//IJ.write("debug "+listLength);
		//IJ.write(" "+(list[first].length())+" "+(list[listLength-1].length())+" "+(list[listLength/2].length()));
		//for(int i=0;i<list.length;i++){IJ.write("list1-"+list[i]);}
		
		//I don't understand this condition and it bugs with some folders
		//-------------------------------------------------------------------
		//if ((list[first].length()==list[listLength-1].length())&&(list[first].length()==list[listLength/2].length()))
		//{ij.util.StringSorter.sort(list); return list;}
		//--------------------------------------------------------------
		int maxDigits = 15;
		String[] list2 = null;
		char ch;
		for (int i=0; i<listLength; i++) {
			int len = list[i].length();
			String num = "";
			for (int j=0; j<len; j++) {
				ch = list[i].charAt(j);
				if (ch>=48&&ch<=57) num += ch;
			}
			if (list2==null) list2 = new String[listLength];
			num = "000000000000000" + num; // prepend maxDigits leading zeroes
			num = num.substring(num.length()-maxDigits);
			list2[i] = num + list[i];
		}
		if (list2!=null) {
			ij.util.StringSorter.sort(list2);
			for (int i=0; i<listLength; i++)
			list2[i] = list2[i].substring(maxDigits);
			//for(int i=0;i<list2.length;i++){IJ.write("list2-"+list2[i]);}
			return list2;
			
		} else {
			ij.util.StringSorter.sort(list);
			//for(int i=0;i<list.length;i++){("list3-"+list[i]);}
			return list;
		}
	}
	// same function converttogray8 but without showing status and progress
	void noshowconvertToGray8(ImagePlus imp) {
		int type, nSlices, width, height;
		type = imp.getType();
		nSlices = imp.getStackSize();
		if (nSlices<2)
		throw new IllegalArgumentException("Stack required");
		width = imp.getWidth();
		height = imp.getHeight();
		ImageStack stack1 = imp.getStack();
		int currentSlice =  imp.getCurrentSlice();
		ImageProcessor ip = imp.getProcessor();
		boolean colorLut = ip.isColorLut();
		boolean pseudoColorLut = colorLut && ip.isPseudoColorLut();
		if (type==ImagePlus.GRAY8 && pseudoColorLut) {
			boolean invertedLut = ip.isInvertedLut();
			ip.setColorModel(LookUpTable.createGrayscaleColorModel(invertedLut));
			stack1.setColorModel(ip.getColorModel());
			
			imp.updateAndDraw();
			return;
		}
		
		ImageStack stack2 = new ImageStack(width, height);
		Image img;
		String label;
		double min = ip.getMin();
		double max = ip.getMax();
		int inc = nSlices/20;
		if (inc<1) inc = 1;
		for(int i=1; i<=nSlices; i++) {
			label = stack1.getSliceLabel(1);
			ip = stack1.getProcessor(1);
			stack1.deleteSlice(1);
			ip.setMinAndMax(min, max);
			boolean scale = ImageConverter.getDoScaling();
			stack2.addSlice(label, ip.convertToByte(scale));
			if ((i%inc)==0) {
				//IJ.showProgress((double)i/nSlices);
				//IJ.showStatus("Converting to 8-bits: "+i+"/"+nSlices);
			}
		}
		imp.setStack(null, stack2);
		imp.setSlice(currentSlice);
		imp.setCalibration(imp.getCalibration()); //update calibration
		//IJ.showProgress(1.0);
	}

	public void	itemStateChanged(ItemEvent ie) {
		
		if (ie.getSource() == tracklisten) {
			if (tracklisten.getState()) {
				num2.setEnabled(true);
				num2A.setEnabled(true);
				tracklisten.transferFocus();
			} else {
				num2.setEnabled(false);
				num2A.setEnabled(false);
			}
		}
		if (ie.getSource() == peakautofocus) { 
			if (peakautofocus.getState()) {
				normalize.setEnabled(true);
				peakautofocus.setEnabled(true);
				peakautofocus.setState(true);
				num1.setEnabled(true);
				tracklisten.setEnabled(true);
				tracklisten.setState(false);
				num2.setEnabled(false);
				num2A.setEnabled(false);
				criteriumautofocus.setEnabled(true);
				criteriumautofocus.setState(false);
				num3.setEnabled(false);
				num2A.setEnabled(false);
				lastoption.setEnabled(true);
				lastoption.setState(false);
				num4.setEnabled(false);
				num5.setEnabled(false);
				last2option.setEnabled(true);
				last2option.setState(false);
				num6.setEnabled(false);
				num7.setEnabled(false);
				peakautofocus.transferFocus();
			} else {
				normalize.setEnabled(true);
				peakautofocus.setEnabled(true);
				peakautofocus.setState(false);
				num1.setEnabled(false);
				tracklisten.setEnabled(false);
				tracklisten.setState(false);
				num2.setEnabled(false);
				criteriumautofocus.setEnabled(true);
				criteriumautofocus.setState(true);
				num3.setEnabled(true);
				num2A.setEnabled(true);
				lastoption.setEnabled(true);
				lastoption.setState(false);
				num4.setEnabled(false);
				num5.setEnabled(false);
				last2option.setEnabled(true);
				last2option.setState(false);
				num6.setEnabled(false);
				num7.setEnabled(false);
				criteriumautofocus.transferFocus() ;
			}
		}
		if (ie.getSource() == criteriumautofocus) { 
			if (criteriumautofocus.getState()) {
				normalize.setEnabled(true);
				peakautofocus.setEnabled(true);
				peakautofocus.setState(false);
				num1.setEnabled(false);
				tracklisten.setEnabled(false);
				tracklisten.setState(false);
				num2.setEnabled(false);
				criteriumautofocus.setEnabled(true);
				criteriumautofocus.setState(true);
				num3.setEnabled(true);
				num2A.setEnabled(true);
				lastoption.setEnabled(true);
				lastoption.setState(false);
				num4.setEnabled(false);
				num5.setEnabled(false);
				last2option.setEnabled(true);
				last2option.setState(false);
				num6.setEnabled(false);
				num7.setEnabled(false);
				criteriumautofocus.transferFocus() ;
			} else {
				peakautofocus.setEnabled(true);
				normalize.setEnabled(true);
				peakautofocus.setState(false);
				num1.setEnabled(false);
				tracklisten.setEnabled(false);
				tracklisten.setState(false);
				num2.setEnabled(false);
				criteriumautofocus.setEnabled(true);
				criteriumautofocus.setState(false);
				num3.setEnabled(false);
				num2A.setEnabled(false);
				lastoption.setEnabled(true);
				lastoption.setState(true);
				num4.setEnabled(true);
				num5.setEnabled(true);
				last2option.setEnabled(true);
				last2option.setState(false);
				num6.setEnabled(false);
				num7.setEnabled(false);
				lastoption.transferFocus() ;
			}
		}
		if (ie.getSource() == lastoption) { 
			if (lastoption.getState()) {
				peakautofocus.setEnabled(true);
				normalize.setEnabled(true);
				peakautofocus.setState(false);
				num1.setEnabled(false);
				tracklisten.setEnabled(false);
				tracklisten.setState(false);
				num2.setEnabled(false);
				criteriumautofocus.setEnabled(true);
				criteriumautofocus.setState(false);
				num3.setEnabled(false);
				num2A.setEnabled(false);
				lastoption.setEnabled(true);
				lastoption.setState(true);
				num4.setEnabled(true);
				num5.setEnabled(true);
				last2option.setEnabled(true);
				last2option.setState(false);
				num6.setEnabled(false);
				num7.setEnabled(false);
				lastoption.transferFocus() ;
			} else {
				peakautofocus.setEnabled(true);
				normalize.setEnabled(true);
				peakautofocus.setState(false);
				num1.setEnabled(false);
				tracklisten.setEnabled(false);
				tracklisten.setState(false);
				num2.setEnabled(false);
				criteriumautofocus.setEnabled(true);
				criteriumautofocus.setState(false);
				num3.setEnabled(false);
				num2A.setEnabled(false);
				lastoption.setEnabled(true);
				lastoption.setState(false);
				num4.setEnabled(false);
				num5.setEnabled(false);
				last2option.setEnabled(true);
				last2option.setState(true);
				num6.setEnabled(true);
				num7.setEnabled(true);
				last2option.transferFocus();
			}
		}
		if (ie.getSource() == last2option) { 
			if (last2option.getState()) {
				peakautofocus.setEnabled(true);
				normalize.setEnabled(true);
				peakautofocus.setState(false);
				num1.setEnabled(false);
				tracklisten.setEnabled(false);
				tracklisten.setState(false);
				num2.setEnabled(false);
				criteriumautofocus.setEnabled(true);
				criteriumautofocus.setState(false);
				num3.setEnabled(false);
				num2A.setEnabled(false);
				lastoption.setEnabled(true);
				lastoption.setState(false);
				num4.setEnabled(false);
				num5.setEnabled(false);
				last2option.setEnabled(true);
				last2option.setState(true);
				num6.setEnabled(true);
				num7.setEnabled(true);
				last2option.transferFocus();
			} else {
				peakautofocus.setEnabled(true);
				normalize.setEnabled(true);
				peakautofocus.setState(true);
				num1.setEnabled(true);
				tracklisten.setEnabled(true);
				tracklisten.setState(false);
				num2.setEnabled(false);
				criteriumautofocus.setEnabled(true);
				criteriumautofocus.setState(false);
				num3.setEnabled(false);
				num2A.setEnabled(false);
				lastoption.setEnabled(true);
				lastoption.setState(false);
				num4.setEnabled(false);
				num5.setEnabled(false);
				last2option.setEnabled(true);
				last2option.setState(false);
				num6.setEnabled(false);
				num7.setEnabled(false);
				peakautofocus.transferFocus();
			}
		}
		if (ie.getSource() == allstacks) { 
			if (allstacks.getState()) {   // User selected 'all stack' button
				peakautofocus.setEnabled(false);
				normalize.setEnabled(false);
				peakautofocus.setState(false);
				num1.setEnabled(false);
				tracklisten.setEnabled(false);
				tracklisten.setState(false);
				num2.setEnabled(false);
				criteriumautofocus.setEnabled(false);
				criteriumautofocus.setState(false);
				num3.setEnabled(false);
				num2A.setEnabled(false);
				lastoption.setEnabled(false);
				lastoption.setState(false);
				num4.setEnabled(false);
				num5.setEnabled(false);
				last2option.setEnabled(false);
				last2option.setState(false);
				num6.setEnabled(false);
				num7.setEnabled(false);
				peakautofocus.transferFocus();
				
			} else { // User unselected 'all stack' button
				peakautofocus.setEnabled(true);
				normalize.setEnabled(true);
				peakautofocus.setState(true);
				num1.setEnabled(true);
				tracklisten.setEnabled(true);
				tracklisten.setState(false);
				num2.setEnabled(false);
				criteriumautofocus.setEnabled(true);
				criteriumautofocus.setState(false);
				num3.setEnabled(false);
				num2A.setEnabled(false);
				lastoption.setEnabled(true);
				lastoption.setState(false);
				num4.setEnabled(false);
				num5.setEnabled(false);
				last2option.setEnabled(true);
				last2option.setState(false);
				num6.setEnabled(false);
				num7.setEnabled(false);
				peakautofocus.transferFocus();
			}
		} 
	}
}  // End 
