import ij.plugin.filter.PlugInFilter;
import java.awt.Color;
import java.util.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.process.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.Analyzer;
import ij.measure.*;

//computer assisted sperm analysis plugin, based on mTrack2r

public class CASA_ implements PlugInFilter,Measurements  {

	ImagePlus	imp;
	int		nParticles;
	float[][]	ssx;
	float[][]	ssy;
	String directory,filename;
	
	//minimum sperm size
	float	minSize = 0;
	//maximum sperm size
	float	maxSize = 40;
	//minimum length of sperm track in frames
	float 	minTrackLength = 97;
	//show or do not show all paths of tracked sperm
	boolean	bShowPaths = true;
	//a left over from the original program, I used their length calculation area to do my initial sperm velocity parameter analysis for each track (this should not be a choice it is just a hold over from the original)
	boolean	bShowPathLengths = true;
	//maximum velocity that is allowable between frames (this is the search radius for the next sperm location in a track... it will only look w/in this distance)
	float 	maxVelocity = 8;
	//minimum first point to end point velocity to be called motile
    	float	minVSL = 3;
	//minimum curvilinear velocity to be termed motile - this is the point to point (all all them) velocity
	float	minVCL = 25;
	//min velocity on the average path to be termed motile - this is the path that has been averaged with a roaming avg
	float	minVAP = 20;
	//this is the velocity below which will be recorded as low
	float	lowVAPspeed = 5;
	//this is the percent of movements between frames that is allowed to be zero for a motile sperm (greater percent == non-motile)
	float	maxPzVAP = 1;
	//same as above but using low VAP speed instead of zero
	float	maxPlVAP = 25;
	
	//low vcl and VAP values used in finding floaters
	float lowVCLspeed = 35;
	float lowVAPspeed2 = 25;
	//high wob / lin values for floater finding
	float highWOB = 80;
	float highLIN = 80;
	//high wob and lin 2 values for floater finding
	float highWOB2 = 50;
	float highLIN2 = 60;

	//this specifies the video frame rate / second
	int frameRate = 97;
	
	//this is the ratio of microns per pixels - size standard used in final velocity calculations
	float	microPerPixel = 1075;
	//crap from old program
	float 	maxColumns=75;
	//print the xy co-ordinates for all tracks?
	int printXY = 0;
	//print the motion character for all motile sperm?
	int printSperm = 0;
	//print median values for characteristics?
	int printMedian = 0;

	public class particle {
		float	x;
		float	y;
		int	z;
		int	trackNr;
		boolean inTrack=false;
		boolean flag=false;

		public void copy(particle source) {
			this.x=source.x;
			this.y=source.y;
			this.z=source.z;
			this.inTrack=source.inTrack;
			this.flag=source.flag;
		}

		public float distance (particle p) {
			return (float) Math.sqrt(sqr(this.x-p.x) + sqr(this.y-p.y));
		}
	}

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if (IJ.versionLessThan("1.17y"))
			return DONE;
		else
			return DOES_8G+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		//the stuff below is the box that pops up to ask for pertinant values - why doesn't it remember the values entered????
		
		GenericDialog gd = new GenericDialog("Sperm Tracker");
		gd.addNumericField("a, Minimum sperm size (pixels):", minSize,0);
		gd.addNumericField("b, Maximum sperm size (pixels):", maxSize,40);
		gd.addNumericField("c, Minimum track length (frames):", minTrackLength,100);
		gd.addNumericField("d, Maximum sperm velocity between frames (pixels):", maxVelocity,8);
		gd.addNumericField("e, Minimum VSL for motile (um/s):", minVSL,3);
		gd.addNumericField("f, Minimum VAP for motile (um/s):", minVAP,20);
		gd.addNumericField("g, Minimum VCL for motile (um/s):", minVCL,25);
		gd.addNumericField("h, Low VAP speed (um/s):", lowVAPspeed,5);
		gd.addNumericField("i, Maximum percentage of path with zero VAP:", maxPzVAP,1);
		gd.addNumericField("j, Maximum percentage of path with low VAP:", maxPlVAP,25);
		gd.addNumericField("k, Low VAP speed 2 (um/s):", lowVAPspeed2,25);
		gd.addNumericField("l, Low VCL speed (um/s):", lowVCLspeed,35);
		gd.addNumericField("m, High WOB (percent VAP/VCL):", highWOB,80);
		gd.addNumericField("n, High LIN (percent VSL/VAP):", highLIN,80);
		gd.addNumericField("o, High WOB two (percent VAP/VCL):", highWOB2,50);
		gd.addNumericField("p, High LIN two (percent VSL/VAP):", highLIN2,60);
		gd.addNumericField("q, Frame Rate (frames per second):", frameRate,97);
		gd.addNumericField("r, Microns per 1000 pixels:", microPerPixel, 1075);
		gd.addNumericField("s, Print xy co-ordinates for all tracked sperm?", printXY, 0);
		gd.addNumericField("t, Print motion characteristics for all motile sperm?", printSperm,0);
		gd.addNumericField("u, Print median values for motion characteristics?", printMedian,0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		
		minSize = (float)gd.getNextNumber();
		maxSize = (float)gd.getNextNumber();
		minTrackLength = (float)gd.getNextNumber();
		maxVelocity = (float)gd.getNextNumber();
		minVSL = (float)gd.getNextNumber();
		minVAP = (float)gd.getNextNumber();
		minVCL = (float)gd.getNextNumber();
		lowVAPspeed = (float)gd.getNextNumber();
		maxPzVAP = (float)gd.getNextNumber();
		maxPlVAP = (float)gd.getNextNumber();
		lowVAPspeed2 = (float)gd.getNextNumber();
		lowVCLspeed = (float)gd.getNextNumber();
		highWOB = (float)gd.getNextNumber();
		highLIN = (float)gd.getNextNumber();
		highWOB2 = (float)gd.getNextNumber();
		highLIN2 = (float)gd.getNextNumber();
		frameRate = (int)gd.getNextNumber();
		microPerPixel = (float)gd.getNextNumber();
		printXY = (int)gd.getNextNumber();
		printSperm = (int)gd.getNextNumber();

		//below I am trying to convert integer values required for float to decimal for percent calculations
		maxPzVAP = maxPzVAP / 100;
		maxPlVAP = maxPlVAP/100;

		microPerPixel = microPerPixel / 1000;
		lowVAPspeed = lowVAPspeed / frameRate;
		highWOB = highWOB / 100;
		highLIN = highLIN / 100;
		highWOB2 = highWOB2 / 100;
		highLIN2 = highLIN2 / 100;
		track(imp, minSize, maxSize, maxVelocity);
	}


	public double myAngle(double dX, double dY){
	    double aRad = 0;
	    double aFinal = 0;
	    if(dY > 0)
	    {
		//gives us 0->90, the angle is fine
		    if(dX > 0)
		    {
		        aRad = Math.atan((dY) / (dX));
		        aFinal = ((aRad * 180) / Math.PI);
		    }
 		    else if(dX < 0)
		    //gives us 0->-90, we need to add 180 (90->180)
		    {
			    aRad = Math.atan((dY) / (dX));
			    aFinal =180 + ((aRad * 180) / Math.PI);
		     }
		     else if(dX == 0)
		     {
			    aFinal = 90;
		     }
	    }
	    else if(dY < 0)
	    {
		//gives us 0->-90, add 360 and we've got it (270->360)
		    if(dX > 0)
		    {
			    aRad = Math.atan((dY) / (dX));
			    aFinal = 360 + ((aRad * 180) / Math.PI);
		     }
		     else if(dX < 0)
		     //gives us 0->90, add 180 and we've got it (180->270)
		     {
			    aRad = Math.atan((dY) / (dX));
			    aFinal =180 + ((aRad * 180) / Math.PI);
		     }
		     else if(dX == 0)
		     //should be able to handle -90 becomes 270 in the new system
		     {
			    aFinal = 270;
		     }
	    }
	    else if(dY==0)
	    {
		    if(dX > 0)
		    {
			    aFinal = 0;
		     }
		     else if(dX < 0)
		     {
			    aFinal = 180;
		     }
	    }
        return aFinal;
    }
    

    //Angle change and Direction Change; this may represent a substitute for beat cross calculations, however, I have not determined its usefullness and it does appear to be highly dependent on frame rate
    public double[] myAngleDelta(double oldA, double storA, double directionVCL)
    {
	//aChange is element 0
	//directionVCL is element 1
	//0 is clockwise, 1 is counter-clockwise
	double arrayAngle[] = new double[2]; 
        //use the angle from above to calculate the change from the previous angle (all angles are relative to the axis...)
	    double holdA1 = oldA + 180;
	    double holdA2 = oldA - 180;

        if(oldA > 180)
	    {
		    if(holdA2 < storA && oldA > storA)
		    {
			    arrayAngle[0] = oldA - storA;
			    arrayAngle[1] = 0;
		    }
		    else if(oldA < storA)
		    {
			    arrayAngle[0] = storA - oldA;
			    arrayAngle[1] = 1;
		    }
		    else if(oldA > storA)
		    {
			    arrayAngle[0] = storA - oldA + 360;
			    arrayAngle[1] = 1;
		    }
		    else if(oldA == storA)
		    {
			   arrayAngle[0] = 0;
			   arrayAngle[1] = directionVCL;
		    }
		    else if(holdA2 == storA)
		    {
			    arrayAngle[0] = 0;
			    arrayAngle[1] = directionVCL;
		    }
	    }
	    else if(oldA <= 180)
	    {
		    if(holdA1 > storA  && oldA < storA)
		    {
			    arrayAngle[0] = storA - oldA;
			    arrayAngle[1] = 1;
		    }
		    else if(oldA > storA)
		    {
			    arrayAngle[0] = oldA - storA;
			    arrayAngle[1] = 0;
		    }
		    else if(oldA < storA)
		    {
			    arrayAngle[0] = oldA - storA + 360;
			    arrayAngle[1] = 0;
		    }
		    else if(oldA == storA)
		    {
			    arrayAngle[0] = 0;
			    arrayAngle[1] = directionVCL;
		    }
		    else if(holdA1 == storA)
		    {
			    arrayAngle[0] = 0;
			    arrayAngle[1] = directionVCL;
		    }
	    }
	    return arrayAngle;
    }



	public void track(ImagePlus imp, float minSize, float maxSize, float maxVelocity) {
		int nFrames = imp.getStackSize();
		if (nFrames<2) {
			IJ.showMessage("Tracker", "Stack required");
			return;
		}

		ImageStack stack = imp.getStack();
		int options = 0; // set all PA options false
		int measurements = CENTROID;
		// Initialize results table
		ResultsTable rt = new ResultsTable();
		rt.reset();

		// create storage for particle positions
		List[] theParticles = new ArrayList[nFrames];
		int trackCount=0;

		// record particle positions for each frame in an ArrayList
		for (int iFrame=1; iFrame<=nFrames; iFrame++) {
			theParticles[iFrame-1]=new ArrayList();
			rt.reset();
			ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize);
			pa.analyze(imp, stack.getProcessor(iFrame));
			float[] sxRes = rt.getColumn(ResultsTable.X_CENTROID);
			float[] syRes = rt.getColumn(ResultsTable.Y_CENTROID);
			if (sxRes==null)
				return;

			for (int iPart=0; iPart<sxRes.length; iPart++) {
				particle aParticle = new particle();
				aParticle.x=sxRes[iPart];
				aParticle.y=syRes[iPart];
				aParticle.z=iFrame-1;
				theParticles[iFrame-1].add(aParticle);
			}
			IJ.showProgress((double)iFrame/nFrames);
		}

		// now assemble tracks out of the particle lists
		// Also record to which track a particle belongs in ArrayLists
		List theTracks = new ArrayList();
		for (int i=0; i<=(nFrames-1); i++) {
			IJ.showProgress((double)i/nFrames);
			for (ListIterator j=theParticles[i].listIterator();j.hasNext();) {
				particle aParticle=(particle) j.next();
				if (!aParticle.inTrack) {
					// This must be the beginning of a new track
					List aTrack = new ArrayList();
					trackCount++;
					aParticle.inTrack=true;
					aParticle.trackNr=trackCount;
					aTrack.add(aParticle);
					// search in next frames for more particles to be added to track
					boolean searchOn=true;
					particle oldParticle=new particle();
					particle tmpParticle=new particle();
					oldParticle.copy(aParticle);
					for (int iF=i+1; iF<=(nFrames-1);iF++) {
						boolean foundOne=false;
						particle newParticle=new particle();
						for (ListIterator jF=theParticles[iF].listIterator();jF.hasNext() && searchOn;) {
							particle testParticle =(particle) jF.next();
							float distance = testParticle.distance(oldParticle);
							// record a particle when it is within the search radius, and when it had not yet been claimed by another track
							if ( (distance < maxVelocity) && !testParticle.inTrack) {
								// if we had not found a particle before, it is easy
								if (!foundOne) {
									tmpParticle=testParticle;
									testParticle.inTrack=true;
									testParticle.trackNr=trackCount;
									newParticle.copy(testParticle);
									foundOne=true;
								}
								else {
									// if we had one before, we'll take this one if it is closer.  In any case, flag these particles
									testParticle.flag=true;
									if (distance < newParticle.distance(oldParticle)) {
										testParticle.inTrack=true;
										testParticle.trackNr=trackCount;
										newParticle.copy(testParticle);
										tmpParticle.inTrack=false;
										tmpParticle.trackNr=0;
										tmpParticle=testParticle;
									}
									else {
										newParticle.flag=true;
									}
								}
							}
							else if (distance < maxVelocity) {
							// this particle is already in another track but could have been part of this one
							// We have a number of choices here:
							// 1. Sort out to which track this particle really belongs (but how?)
							// 2. Stop this track
							// 3. Stop this track, and also delete the remainder of the other one
							// 4. Stop this track and flag this particle:
								testParticle.flag=true;
							}
						}
						if (foundOne)
							aTrack.add(newParticle);
						else
							searchOn=false;
						oldParticle.copy(newParticle);
					}
					theTracks.add(aTrack);
				}
			}
		}


		// this is my movement assessment bit that runs for each sperm
		//this is the number of points to be included in the VAP roaming average
		int vAPpoints = frameRate / 6;
		
		if(vAPpoints % 2 > 0){
			vAPpoints ++;
		}
		
		//arrays to hold on to the total movement for a given male
		double[] vCLS = new double[trackCount];
		double[] vAPS = new double[trackCount];
		double[] vSLS = new double[trackCount];
		double[] sumVAPdistsOrigin = new double[trackCount];
		
		//angle and beat calculations
		double dY = 0;
		double dX = 0;
		double oldA = 0;
		double storA = 0;
		double aChange = 0;
		double directionVCL = 0;
		double oldDirectionVCL = 0;
		double[] dirChangesVCL = new double [trackCount];
		double storTotalRotation = 0;
		double[] framesBeat = new double [trackCount];
		//used for myangleDelta
		double[] arrayAngle = {0, 0};
		
		//variables used in finding maximum movement from origin
		double firstVAPx = 0;
		double firstVAPy = 0; 
		double holdVAPdistance = 0;
		double storMaxVAPVSL = 0;
		
		//this keeps track of frames on the VCL path
		int[] frames = new int[trackCount];

		//this tracks for each sperm how many times the VCL path crosses VAP, and how many times a direction change in the VCL path occurs
		double[] beatCross = new double[trackCount];
		double vAPxG = 0;
		double vAPyG = 0;
		double vAPxGlast = 0;
		double vAPyGlast = 0;
		//array to hold the VAP points before roaming average
		double[] xCo = new double[vAPpoints];
		double[] yCo = new double[vAPpoints];
		
		//these two keep track of the number of times movement between frames is zero or low velocity for each sperm
		double[] numVAPzero = new double[trackCount];
		double[] numLowVAP = new double[trackCount];
		//this carries the zero and low vap instances
		double holdZeroVAPs = 0;
		double holdLowVAPs = 0;
		
		
		//points used in VAP distance calculations
		double vAPx = 0;
		double vAPy = 0;
		double vAPxOld = 0;
		double vAPyOld = 0;
		//used in vap calculations, holds total of points for average
		double holdX = 0;
		double holdY = 0;
		
		//for average distance from origin over whole path
		double holdTotalVAPdistance = 0;
		double distFirstVAPcurrent = 0;

		//holds values for percent motility calc
		int motileSperm = 0;
		double totalSperm = 0;

		//for calculations in VCL distance
		double holdDistance = 0;
		
		double[] framesVAP = new double[trackCount];
		double holdVAPframe = 0;

		//strings to print out all of the data gathered, point by point, also variables for holding points for printing
		String xyPts = " ";
		String xyVAPpts = " ";
		String firstVAPpts = " ";
		double holdMaxX = 0;
		double holdMaxY = 0;
		
		//initialize variables
		int frame = 0;
		double x1, y1, x2, y2;
		int trackNr=0;
		int displayTrackNr=0;
		
		//loop through all sperm tracks
		for (ListIterator iT=theTracks.listIterator(); iT.hasNext();) {
			trackNr++;
			List bTrack=(ArrayList) iT.next();
			if (bTrack.size() >= minTrackLength) {
				//keeps track of the current track
				displayTrackNr++;
				ListIterator jT=bTrack.listIterator();
				particle oldParticle=(particle) jT.next();
				particle firstParticle=new particle();
				firstParticle.copy(oldParticle);
				frames[displayTrackNr-1]=bTrack.size();
				frame = 0;
				holdVAPframe = 0;
				vAPx = 0;
				vAPy = 0;

				holdTotalVAPdistance = 0;
				
				for (;jT.hasNext();){
					particle newParticle=(particle) jT.next();

					//VCL calculations
					holdDistance= Math.sqrt(sqr(oldParticle.x-newParticle.x)+sqr(oldParticle.y-newParticle.y));
					vCLS[displayTrackNr-1] += holdDistance;
					xyPts = " " + newParticle.x + " " + newParticle.y;
					
					//hold the x and y co-ordinates for VAP - will only hold the number of co-ordinates in VAPpoints and will cycle them as this goes
					for (int j=0; j < (vAPpoints - 1); j++){
						holdX = xCo[j + 1];
						holdY = yCo[j + 1];
						xCo[j] = holdX;
						yCo[j] = holdY;
					}

					xCo[vAPpoints - 1] = newParticle.x;
					yCo[vAPpoints - 1] = newParticle.y;
					dX = newParticle.x - oldParticle.x;
					dY = newParticle.y - oldParticle.y;

					//calculate the angle of sperm between the two points at hand, but only if we have moved
					if(holdDistance > 0){
						//call angle function
						storA = myAngle(dX, dY);


						//use the angle from above to calculate the change from the previous angle (all angles are relative to the axis...)
						if(frame>1){
							//myAngleDelta returns an array
							//arrayAngle[0] = aDelta
							//arrayAngle[1] = rotDir
							arrayAngle = myAngleDelta(oldA, storA, directionVCL);
							aChange = arrayAngle[0];
							directionVCL = arrayAngle[1];
							
							//note a direction change by comparing the present clockwise or counter clockwise orientation of angular change to the last
							if(frame>2){
								if(oldDirectionVCL != directionVCL){
									dirChangesVCL[displayTrackNr-1]++;
								}
							}else{
								storTotalRotation += storA;
							}
							if(storTotalRotation >=360){
								dirChangesVCL[displayTrackNr-1]++;
								storTotalRotation = storTotalRotation - 360;
							}
							oldDirectionVCL = directionVCL;
							framesBeat[displayTrackNr-1] += 1;
						}
						oldA = storA;
					}
					
					vAPx = 0;
					vAPy = 0;

					//sum the stored vap co-ordinates
					for(int k=0; k<(vAPpoints); k++) {
						vAPx += xCo[k];
						vAPy += yCo[k];
					}

					//generate an average to make a vap point
					vAPx = vAPx / vAPpoints;
					vAPy = vAPy / vAPpoints;

					//if current frame is beyond the number of points used in VAP than calculate distance on VAP path
					if(frame > (vAPpoints + 1)){
						
						xyVAPpts =" " + vAPx + " " + vAPy;

						//calculates VSL
						if(holdVAPframe ==0){
							firstVAPy = vAPy;
							firstVAPx = vAPx;
							firstVAPpts = " " + firstVAPx + " " + firstVAPy;
						}
						else{
							firstVAPpts = " " + " ";
						}
						
						//calculate dif of vap point from vcl point for beat cross calculation
						if(vAPx > xCo[vAPpoints / 2]){
							vAPxG = 1;
						}else if(vAPx < xCo[vAPpoints / 2]){
							vAPxG = 0;
						}
						if(vAPy > yCo[vAPpoints / 2]){
							vAPyG = 1;
						}else if(vAPy < yCo[vAPpoints / 2]){
							vAPyG = 0;
						}
													
						if(holdVAPframe > 0){
							//beat cross calculation
							if((vAPxG != vAPxGlast) || (vAPyG != vAPyGlast)){
								beatCross[displayTrackNr-1]++;
							}
							vAPxGlast = vAPxG;
							vAPyGlast = vAPyG;
							
							distFirstVAPcurrent = Math.sqrt(sqr(firstVAPx - vAPx) + sqr(firstVAPy - vAPy));
							sumVAPdistsOrigin[displayTrackNr -1] += distFirstVAPcurrent;
							if(holdVAPframe ==1){
								storMaxVAPVSL = distFirstVAPcurrent;
							}else{
								if(distFirstVAPcurrent > storMaxVAPVSL){
									storMaxVAPVSL = distFirstVAPcurrent;
									holdMaxX = vAPx;
									holdMaxY = vAPy;
								}
							}
						}
						
						//keep tracks of frames used in VAP
						holdVAPframe ++;
						
						//calculate the distance between VAP points and add them to the array
						holdTotalVAPdistance = Math.sqrt(sqr(vAPxOld-vAPx)+sqr(vAPyOld-vAPy));
						vAPS[displayTrackNr-1] += holdTotalVAPdistance;

						if((holdTotalVAPdistance *microPerPixel*frameRate) < lowVAPspeed){
							numLowVAP[displayTrackNr-1]++;
						}
						if(holdTotalVAPdistance == 0){
							numVAPzero[displayTrackNr-1]++;
						}

					}
					
					vAPxOld = vAPx;
					vAPyOld = vAPy;
					frame++;

					//save the last two VCL points 
					oldParticle=newParticle;
					if(printXY !=0){
						IJ.write(xyPts + xyVAPpts + firstVAPpts);
					}
				}
				//put the calculated frames into an array, stored for each sperm and store VSL
				frames[displayTrackNr-1] = frame;
				framesVAP[displayTrackNr-1] = holdVAPframe;

				vSLS[displayTrackNr-1]=storMaxVAPVSL;

				String pointsAnalyzed = " " + holdMaxX + " " + holdMaxY;
				if(printXY !=0){
					IJ.write(xyPts + xyVAPpts + firstVAPpts + pointsAnalyzed);
				}
			}
		}
		//hold sum of all changes from frame to frame for a given sperm before generating a per second value for that sperm
		double holdVAP =0;
		double holdVCL = 0;
		double holdVSL = 0;
		double holdLIN = 0;
		double holdWOB = 0;
		double holdDistOrigin = 0;
		double holdBeatCross = 0;
		double holdDirChanges = 0;
		//carry over frame rate so that it can be used in calculation
		double frameRateCalc = frameRate;
		

		//sum the per frame movements
		for(int z=0; z<displayTrackNr; z++){
			
			holdVAP = vAPS[z];
			holdVCL = vCLS[z];
			holdVSL = vSLS[z];

			holdZeroVAPs = numVAPzero[z];
			holdLowVAPs = numLowVAP[z];
			holdBeatCross = beatCross[z];
			holdDirChanges = dirChangesVCL[z];
			holdDistOrigin = sumVAPdistsOrigin[z];
			
			//average the per frame to a per second value
			vCLS[z] = holdVCL * (frameRateCalc / (frames[z])) * microPerPixel;
			vAPS[z] = holdVAP * (frameRateCalc / (framesVAP[z])) * microPerPixel;
			vSLS[z] = holdVSL * (frameRateCalc / (framesVAP[z])) * microPerPixel;
			beatCross[z] = holdBeatCross *(frameRateCalc / (framesBeat[z] - 1));
			dirChangesVCL[z] = holdDirChanges * (frameRateCalc / (framesBeat[z] -1));
			//calculate LIN and WOB
			holdLIN = holdVAP / holdVCL;
			holdWOB = holdVSL / holdVAP;
			numVAPzero[z] = holdZeroVAPs / framesVAP[z];
			numLowVAP[z] = holdLowVAPs / framesVAP[z];
			sumVAPdistsOrigin[z] = holdDistOrigin *(frameRateCalc / (framesVAP[z])) *microPerPixel;
			
			
		}
		
		//keep track of velocities for the whole sperm population
		//these are used for description of motility character for all motile sperm
		double totalVAP = 0;
		double totalVCL = 0;
		double totalVSL = 0;
		double totalEffic = 0;
		double totalWOBbeats = 0;
		double totalProg = 0;
		double totalLINeffic = 0;
		double totalBeatCross = 0;
		
		//these hold the average (total value in variable above divided by the number of motile sperm)
		double avgVAP = 0;
		double avgVCL = 0;
		double avgVSL = 0;
		double avgEffic = 0;
		double avgWOBbeats = 0;
		double avgProg = 0;
		double avgLINeffic = 0;
		//these are the values for VAP/ VCL and VSL/ VAP to describe path curvature of all motile sperm
		double avgLin = 0;
		double avgWob = 0;
		double avgBeats = 0;
		
		//arrays to hold velocity characteristics for all motile sperm and then to calculate median values
		double[] motileVAP = new double [trackCount];
		double[] motileVSL = new double [trackCount];
		double[] motileVCL = new double [trackCount];
		double[] motileWOB = new double [trackCount];
		double[] motileLIN = new double [trackCount];
		double[] motileEffic = new double [trackCount];
		double[] motileLINeffic = new double [trackCount];
		double[] motileWOBbeats = new double [trackCount];
		double[] motileBeats = new double [trackCount];
		double[] motileProg = new double [trackCount];
		double[] motileTracks = new double [trackCount];
		
		//these hold the values to be added to the motility array and the total for motile sperm
		double addLIN = 0;
		double addWOB = 0;
		double addBeatCross =  0;
		double addVAP =0;
		double addVSL =0;
		double addVCL =0;
		

		//this is where we determine if a sperm is motile
		for(int m=0;m<displayTrackNr; m++){
			
			totalSperm++;
			motileTracks[m] = 0;
			//two tiers for motility determination, first check a set of characteristics then if we meet those criteria, check three other sets... have to meet the first tier and one of the second tiers 
			if((vSLS[m] > minVSL) && (vCLS[m] > minVCL) && (((vAPS[m] / vCLS[m]) * 100) > 2) && (vAPS[m] > minVAP)){
				if(((((vAPS[m] / vCLS[m]) * 100) < highWOB) && (((vSLS[m] / vAPS[m]) * 100) < highLIN) && (numLowVAP[m] < maxPlVAP) && (numVAPzero[m] < maxPzVAP)) 
				 || ((vCLS[m] > lowVCLspeed) && (vAPS[m] > lowVAPspeed2))
				 || ((((vAPS[m] / vCLS[m]) * 100) < highWOB2) || (((vSLS[m] / vAPS[m]) * 100) < highLIN2))){
				 
					addVAP =(vAPS[m]);
					addVSL =(vSLS[m]);
					addVCL =(vCLS[m]);
					addBeatCross =beatCross[m];
					
					addWOB = addVAP / addVCL;
					addLIN = addVSL / addVAP;
					if(printMedian != 0){
						motileVAP[motileSperm] = addVAP;
						motileVSL[motileSperm] = addVSL;
						motileVCL[motileSperm] = addVCL;
						motileWOB[motileSperm] = addWOB;
						motileLIN[motileSperm] = addLIN;
						motileEffic[motileSperm] = sumVAPdistsOrigin[m]/ addVAP;
						motileBeats[motileSperm] = addBeatCross;
						motileProg[motileSperm] = sumVAPdistsOrigin[m];
						motileLINeffic[motileSperm] = (addLIN / (sumVAPdistsOrigin[m] / addVAP))*100;
					}
					if(printSperm != 0){
						//output characteristics for all motile sperm if needed
						String sperm = " " + (float)addVCL + " " + (float)addVAP + " " + (float)addVSL + " " + (float)addLIN + " " + (float)addWOB + " "  + (float)(sumVAPdistsOrigin[m]/ addVAP) + " " + (float)addBeatCross;
						IJ.write(sperm);
					}
					
					//if the sperm is motile we add in its values into the holding variable to determine the average motility characteristics for the sample
					totalVAP+=(vAPS[m]);
					totalVSL+=(vSLS[m]);
					totalVCL+=(vCLS[m]);
					totalBeatCross +=beatCross[m];
					totalEffic += sumVAPdistsOrigin[m] / addVAP;
					totalWOBbeats += addWOB / addBeatCross;
					totalProg += sumVAPdistsOrigin[m];
					totalLINeffic += (addLIN / (sumVAPdistsOrigin[m] / addVAP)) * 100;
					
					motileSperm=motileSperm + 1;
					motileTracks[m] = 1;
					
				 }
			}		
		}
		

		int middleMotArray = 0;
		double medVAP = 0;
		double medVCL = 0;
		double medVSL = 0;
		double medLIN = 0;
		double medWOB = 0;
		
		double medEffic = 0;
		double medWOBbeats = 0;
		double medBeats = 0;
		double medProg = 0;
		double medLINeffic = 0; 		
		
		Arrays.sort(motileVAP);	
		Arrays.sort(motileVSL);
		Arrays.sort(motileVCL);
		Arrays.sort(motileBeats);
		Arrays.sort(motileLIN);
		Arrays.sort(motileWOB);
		
		Arrays.sort(motileEffic);
		Arrays.sort(motileWOBbeats);
		Arrays.sort(motileBeats);
		Arrays.sort(motileProg);
		Arrays.sort(motileLINeffic);
		
		middleMotArray = motileSperm /2;
		
		if (((motileSperm % 2) == 0) && (motileSperm > 2)) {
			medVAP = (motileVAP[trackCount -middleMotArray] + motileVAP[trackCount - middleMotArray -1]) /2;
			medVCL = (motileVCL[trackCount - middleMotArray] + motileVCL[trackCount - middleMotArray-1]) /2;
			medVSL = (motileVSL[trackCount - middleMotArray] + motileVSL[trackCount - middleMotArray-1])/2;
			medLIN = (motileLIN[trackCount - middleMotArray] + motileLIN[trackCount - middleMotArray - 1]) / 2;
			medWOB = (motileWOB[trackCount - middleMotArray] + motileWOB[trackCount - middleMotArray - 1]) / 2;
			medEffic = (motileEffic[trackCount - middleMotArray] + motileEffic[trackCount - middleMotArray - 1]) / 2;
			medBeats = (motileBeats[trackCount - middleMotArray] + motileBeats[trackCount - middleMotArray-1]) / 2;
			medProg = (motileProg[trackCount - middleMotArray] + motileProg[trackCount - middleMotArray - 1]) / 2;
			medLINeffic = (motileLINeffic[trackCount - middleMotArray] + motileLINeffic[trackCount - middleMotArray - 1]) / 2;
		}else if (motileSperm > 2){
			medVAP = motileVAP[trackCount - middleMotArray];
			medVCL = motileVCL[trackCount - middleMotArray];
			medVSL = motileVSL[trackCount - middleMotArray];
			medLIN = motileLIN[trackCount - middleMotArray];
			medWOB = motileWOB[trackCount - middleMotArray];
			medEffic = motileEffic[trackCount - middleMotArray];
			medBeats = motileBeats[trackCount - middleMotArray];
			medProg = motileProg[trackCount - middleMotArray];
			medLINeffic = motileLINeffic[trackCount - middleMotArray];
		}
		
		
		//average the velocity characteristics for motile sperm using totals from above
		avgBeats = totalBeatCross / motileSperm;
		avgVAP = totalVAP / motileSperm;
		avgVCL = totalVCL / motileSperm;
		avgVSL = totalVSL / motileSperm;
		avgEffic = totalEffic / motileSperm;
		avgProg = totalProg / motileSperm;
		avgLINeffic = totalLINeffic / motileSperm;

		avgLin = avgVSL/avgVAP;
		avgWob = avgVAP/avgVCL;
		//percent motility
		double motility = 0;
		
		
		motility = motileSperm/totalSperm;
		
		String sperm = " " + (float)motility + " " + (float)avgVCL + " " + (float)avgVAP + " " + (float)avgVSL + " " + (float)avgLin + " " + (float)avgWob + " " + (float)avgProg + " " + (float)avgBeats;
		if(printMedian !=0){
			sperm += " " + (float)medVCL + " " + (float)medVAP + " " + (float)medVSL + " " + (float)medLIN + " " + (float)medWOB + " " + (float)medProg + " " + (float)medBeats + " ";
		}
		sperm += " " + (float)totalSperm;
		IJ.write(sperm);
		
		// 'map' of tracks 
		if (imp.getCalibration().scaled()) {
			IJ.showMessage("MultiTracker", "Cannot display paths if image is spatially calibrated");
			return;
		}
		int upRes = 1;
		ImageProcessor ip = new ByteProcessor(imp.getWidth()*upRes, imp.getHeight()*upRes);
		ip.setColor(Color.white);
		ip.fill();
		int trackCount2=0;
		int trackCount3=0;
		int color;
		for (ListIterator iT=theTracks.listIterator();iT.hasNext();) {
			trackCount2++;
			List zTrack=(ArrayList) iT.next();
			if (zTrack.size() >= minTrackLength) {

				ListIterator jT=zTrack.listIterator();
				particle oldParticle=(particle) jT.next();
				
				color = 100;
				if(motileTracks[trackCount3] > 0){
					color = 0;
				}
				trackCount3++;
				
				for (;jT.hasNext();) {
					particle newParticle=(particle) jT.next();
					ip.setValue(color);
					ip.moveTo((int)oldParticle.x*upRes, (int)oldParticle.y*upRes);
					ip.lineTo((int)newParticle.x*upRes, (int)newParticle.y*upRes);
					oldParticle=newParticle;
				}
			}
		}
		new ImagePlus("Paths", ip).show();
			
	}

	// Utility functions
	double sqr(double n) {return n*n;}

	int doOffset (int center, int maxSize, int displacement) {
		if ((center - displacement) < 2*displacement) {
			return (center + 4*displacement);
		}
		else {
			return (center - displacement);
		}
	}

 	double s2d(String s) {
		Double d;
		try {d = new Double(s);}
		catch (NumberFormatException e) {d = null;}
		if (d!=null)
			return(d.doubleValue());
		else
			return(0.0);
	}

}
