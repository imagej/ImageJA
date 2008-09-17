/*JACoP: "Just Another Colocalization Plugin..." v1, 13/02/06
    Fabrice P Cordelières, fabrice.cordelieres at curie.u-psud.fr
    Susanne Bolte, Susanne.bolte@isv.cnrs-gif.fr
 
    Copyright (C) 2006 Susanne Bolte & Fabrice P. Cordelières
  
    License:
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 *
 *
*/

import ij.*;
import ij.ImagePlus.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class JACoP_ implements PlugIn, AdjustmentListener, TextListener {
        //Load preferences
        private static boolean PearsonBool=Prefs.get("JACoP_Pearson.boolean", true);
        private static boolean OverlapBool=Prefs.get("JACoP_Overlap.boolean", true);
        private static boolean MMBool=Prefs.get("JACoP_MM.boolean", true);
        private static boolean CostesThrBool=Prefs.get("JACoP_CostesThr.boolean", true);
        private static boolean CCFBool=Prefs.get("JACoP_CCF.boolean", true);
        private static int CCFx=(int)Prefs.get("JACoP_CCFx.double", 20);
        private static boolean CytoBool=Prefs.get("JACoP_Cyto.boolean", true);
        private static boolean ICABool=Prefs.get("JACoP_ICA.boolean", true);
        private static boolean CostesRandBool=Prefs.get("JACoP_CostesRand.boolean", true);
        private static boolean DistBool=Prefs.get("JACoP_Dist.boolean", false);
        private static boolean SpaPearBool=Prefs.get("JACoP_SpaPear.boolean", false);
        private static boolean lineBool=Prefs.get("JACoP_Line.boolean", true);
        
        private static int MicroscopeType=(int) Prefs.get("JACoP_MicroscopeType.double",0);
        private static int xycal=(int) Prefs.get("JACoP_xycal.double",67);
        private static int zcal=(int) Prefs.get("JACoP_zcal.double",200);
        private static int wa=(int) Prefs.get("JACoP_wa.double",519);
        private static int wb=(int) Prefs.get("JACoP_wb.double",565);
        private static double NA=Prefs.get("JACoP_NA.double",1.4);
        private static double IR=Prefs.get("JACoP_IR.double",1.518);
        
        private static int xyBlock=(int) Prefs.get("JACoP_xyBlock.double",3);
        private static int zBlock=(int) Prefs.get("JACoP_zBlock.double",3);
        private static int nbRand=(int) Prefs.get("JACoP_nbRand.double", 200);
        private static double binWidth=Prefs.get("JACoP_binWidth.double",0.0005);
        private static int fillMeth=(int) Prefs.get("JACoP_fillMeth.double", 0);
        private static boolean xyRand=Prefs.get("JACoP_xyRand.boolean", true);
        private static boolean zRand=Prefs.get("JACoP_zRand.boolean", true);
        private static boolean showRand=Prefs.get("JACoP_showRand.boolean", true);
        
        String[] fill={"Shrink", "Pad w/black px"};
        int widthCostes;
        int heightCostes;
        int nbsliceCostes;
        
         
        
        
        
        //Variables
        int [] imgIDList;
        String [] imgTitleList;
        String title;
        
        ImagePlus imgA;
        String titleA;
        int depthA;
        int widthA;
        int heightA;
        int nbsliceA;
        double[] A;
        int thrA;
        double Amean=0;
        double Amin;
        double Amax=0;
        double[] ACostes;
        
        
        ImagePlus imgB;
        String titleB;
        int depthB;
        int widthB;
        int heightB;
        int nbsliceB;
        double[] B;
        int thrB;
        double Bmean=0;
        double Bmin;
        double Bmax=0;
        double[] BCostes;
        double[] BRandCostes;
        
    
        Vector sliders;
        Vector value;
        
        //Values for stats
        boolean doThat;
        double sumA;
        double sumB;
        double sumAB;
        double sumsqrA;
        double Aarraymean;
        double Barraymean;
        
        
        //Parameters
        String [] MicroscopeTypeList={"WideField","Confocal"};
        String [] Chromophore={"Other","Alexa 350","DAPI","eCFP","eGFP","FITC","Alexa 488","YFP","CY3","Alexa 555","Alexa 546","EtBr","DsRed","Alexa 633","Alexa 647","CY5"};
        int[] Excitation={0,346,358,436,488,494,495,514,548,555,556,518,558,632,650,650};
        int[] Emission={0,442,461,474,507,518,519,527,562,565,573,605,583,647,668,670};
        
        double resxy;
        int pixxy;
        double resz;
        int pixz;
        
        
        //int CCFy;
        //int CCFz;
        
        int a;
        int b;
        int c;
        int d;
        int e;
        int f;
        int i;
        int j;
        int k;
        int l;
        int m;
        
    
    
    
    public void run(String arg) {
        
        //Check that at least two images are opened
        if (WindowManager.getImageCount()<2){
            IJ.showMessage("Error", "Man,\n"+"You're in deep troubles:\n"+"you need at least two images...");
            return;
        }
        
        //Get the list of currently opened windows
        imgIDList= new int[WindowManager.getImageCount()];
        imgIDList=WindowManager.getIDList();
        imgTitleList=new String [WindowManager.getImageCount()];
        
        for (i=0;i<imgIDList.length;i++) imgTitleList[i]=WindowManager.getImage(imgIDList[i]).getTitle();
        
        GenericDialog gd=new GenericDialog("Just Another Colocalization Plugin");
        gd.addChoice("Image A", imgTitleList,imgTitleList[0]);
        gd.addChoice("Image B", imgTitleList,imgTitleList[1]);
        gd.addMessage("");
        gd.addMessage("Correlation-based colocalization:");
        gd.addCheckbox("Pearson's coefficient",PearsonBool);
        gd.addCheckbox("Overlap coefficient, k1 & k2",OverlapBool);
        gd.addCheckbox("M1 & M2 coefficients",MMBool);
        gd.addCheckbox("Costes' automatic threshold", CostesThrBool);
        gd.addCheckbox("Van Steensel's CCF",CCFBool);
        gd.addNumericField("shift_x", CCFx,0);
        gd.addCheckbox("Cytofluorogram", CytoBool);
        gd.addCheckbox("Li's ICA", ICABool);
        gd.addCheckbox("Costes' randomization", CostesRandBool);
        gd.addMessage("");
        gd.addMessage("Object-based colocalization:");
        gd.addCheckbox("Distance based colocalization", DistBool);
        gd.addCheckbox("Spatial Pearson's coefficient",SpaPearBool);
        
        //**********************************************
        gd.addMessage("");
        gd.addCheckbox("Add the zero line", lineBool);
         
        //gd.addNumericField("shift_y", 20,0);
        //gd.addNumericField("shift_z", 20,0);
        gd.showDialog();
        if (gd.wasCanceled()) return;
        
        imgA=WindowManager.getImage(imgIDList[gd.getNextChoiceIndex()]);
        titleA=imgA.getTitle();
        depthA=imgA.getBitDepth();
        widthA=imgA.getWidth();
        heightA=imgA.getHeight();
        nbsliceA=imgA.getStackSize();
        A=new double[widthA*heightA*nbsliceA];
               
        imgB=WindowManager.getImage(imgIDList[gd.getNextChoiceIndex()]);
        titleB=imgB.getTitle();
        depthB=imgB.getBitDepth();
        widthB=imgB.getWidth();
        heightB=imgB.getHeight();
        nbsliceB=imgB.getStackSize();
        B=new double[widthB*heightB*nbsliceB];
        
        if(depthA==24 || depthB==24){
            IJ.showMessage("Error", "Sorry,\n"+"This plugin is not intended to work on color images...\n"+"Try splitting the image into its 3 RGB components\n"+"using Image\\Color\\RGB split function of ImageJ, then\n"+"relaunch JACoP on 2 of the resultant images.");
            return;
        }
        
        IJ.run("Profile Plot Options...", "width="+((int)Math.pow(2, depthA))+" height="+((int)Math.pow(2, depthA))+" minimum=0 maximum="+((int)Math.pow(2, depthA)));
        IJ.run("Profile Plot Options...", "width=256 height=256 minimum=0 maximum=256");
                
        IJ.log("**************************************************\nImage A: "+titleA+"\nImage B :"+titleB);
            
        PearsonBool=gd.getNextBoolean();
        OverlapBool=gd.getNextBoolean();
        MMBool=gd.getNextBoolean();
        CostesThrBool=gd.getNextBoolean();
        CCFBool=gd.getNextBoolean();
        CCFx=(int) gd.getNextNumber();
        CytoBool=gd.getNextBoolean();
        ICABool=gd.getNextBoolean();
        CostesRandBool=gd.getNextBoolean();
        DistBool=gd.getNextBoolean();
        SpaPearBool=gd.getNextBoolean();
        //**********************************************
        lineBool=gd.getNextBoolean();
        
        
        if (CostesRandBool || DistBool || SpaPearBool) {
            GenericDialog gd2=new GenericDialog("Microscope Parameters");
            gd2.addChoice("Microscope type",MicroscopeTypeList,MicroscopeTypeList[0]);
            gd2.addNumericField("xy calibration (nm)", xycal,0);
            gd2.addNumericField("z calibration (nm)", zcal,0);
            gd2.addNumericField("Wavelength A (nm)", wa,0);
            gd2.addNumericField("Wavelength B (nm)", wb,0);
            gd2.addNumericField("NA", NA,1);
            gd2.addNumericField("Refraction index", IR,3);
            gd2.showDialog();
            if (gd2.wasCanceled()) return;
            
            MicroscopeType=gd2.getNextChoiceIndex();
            xycal=(int) gd2.getNextNumber();
            zcal=(int) gd2.getNextNumber();
            wa=(int) gd2.getNextNumber();
            wb=(int) gd2.getNextNumber();
            NA=gd2.getNextNumber();
            IR=gd2.getNextNumber();
            
            if (MicroscopeType==0){
                resxy=0.61*wa/NA;
                resz=2*wa/Math.pow(NA,2);
            }else{
                resxy=0.4*wa/NA;
                resz=1.4*wa/Math.pow(NA,2);
            }
        
            pixxy=(int)resxy/xycal;
            pixz=(int)resz/zcal;
            
                    
            IJ.log("\nMicroscope type: "+MicroscopeTypeList[MicroscopeType]+"\nNA: "+NA+" & IR: "+IR+"\nCalibrations: xy="+xycal+" nm/pixel & z="+zcal+" nm/pixel\nWavelengths: A="+wa+" nm & B="+wb+" nm");
            IJ.log("\nResolution: dxy="+round(resxy,2)+" nm ("+pixxy+" pixels) & dz="+round(resz,2)+" nm ("+pixz+" pixels)");
        }
        
        if (CostesRandBool) {
            GenericDialog gd3=new GenericDialog("Costes randomization parameters");
            gd3.addMessage("Recommended block size in xy: "+pixxy+" px and in z: "+pixz+" px");
            gd3.addMessage("");
            gd3.addNumericField("xy block size (px)", xyBlock,0);
            gd3.addNumericField("z block size (px)", zBlock,0);
            gd3.addNumericField("Number of randomization rounds", nbRand,0);
            gd3.addNumericField("Bin width", binWidth,5);
            gd3.addMessage("");
            gd3.addChoice("Image fitting to blocks' sizes",  fill, fill[fillMeth]); 
            gd3.addMessage("");
            gd3.addCheckbox("Slices to be considered as independent",xyRand);
            gd3.addCheckbox("z randomization as well", zRand);
            gd3.addMessage("");
            gd3.addCheckbox("Show last random image",showRand);
            gd3.addMessage("");
            gd3.showDialog();
            if (gd3.wasCanceled()) return;
            
            xyBlock=(int) gd3.getNextNumber();
            zBlock=(int) gd3.getNextNumber();
            nbRand=(int) gd3.getNextNumber();
            binWidth=gd3.getNextNumber();
            fillMeth=gd3.getNextChoiceIndex();
            xyRand=gd3.getNextBoolean();
            zRand=gd3.getNextBoolean();
            showRand=gd3.getNextBoolean();
            
            if (fillMeth==0){
                widthCostes=((int)(widthA/xyBlock))*xyBlock;
                heightCostes=((int)(heightA/xyBlock))*xyBlock;
            }else{
                widthCostes=(((int)(widthA/xyBlock))+1)*xyBlock;
                heightCostes=(((int)(heightA/xyBlock))+1)*xyBlock;
            }
            
            if (zRand){
                if (fillMeth==0){
                    nbsliceCostes=((int)(nbsliceA/zBlock))*zBlock;
                }else{
                    nbsliceCostes=(((int)(nbsliceA/zBlock))+1)*zBlock;
                }
                if (nbsliceA==1) nbsliceCostes=1;
                ACostes=new double[widthCostes*heightCostes*nbsliceCostes];       
                BCostes=new double[widthCostes*heightCostes*nbsliceCostes];
                BRandCostes=new double[widthCostes*heightCostes*nbsliceCostes];
            }else{
                nbsliceCostes=nbsliceB;
                ACostes=new double[widthCostes*heightCostes*nbsliceA];       
                BCostes=new double[widthCostes*heightCostes*nbsliceB];
                BRandCostes=new double[widthCostes*heightCostes*nbsliceB];
            }
        }
        
        
        if (DistBool || MMBool) {
            thrA=imgA.getProcessor().getAutoThreshold();
            imgA.getProcessor().setThreshold(thrA,Math.pow(2,16),ImageProcessor.RED_LUT);
            imgA.setSlice((int) nbsliceA/2);
            imgA.updateAndDraw();
                
            thrB=imgB.getProcessor().getAutoThreshold();
            imgB.getProcessor().setThreshold(thrB,Math.pow(2,16),ImageProcessor.RED_LUT);
            imgB.setSlice((int) nbsliceA/2);
            imgB.updateAndDraw();
                 
            GenericDialog gd4=new GenericDialog("Threshold Parameters");
            gd4.addSlider("Threshold A: ",imgA.getProcessor().getMin(), imgA.getProcessor().getMax(),thrA);
            gd4.addSlider("Threshold B: ",imgB.getProcessor().getMin(), imgB.getProcessor().getMax(),thrB);
            gd4.addMessage("");
            gd4.addSlider("Slice: ",1, nbsliceA,(int) nbsliceA/2);
            sliders=gd4.getSliders();
            ((Scrollbar)sliders.elementAt(0)).addAdjustmentListener(this);
            ((Scrollbar)sliders.elementAt(1)).addAdjustmentListener(this);
            ((Scrollbar)sliders.elementAt(2)).addAdjustmentListener(this);
            value = gd4.getNumericFields();
            ((TextField)value.elementAt(0)).addTextListener(this);
            ((TextField)value.elementAt(1)).addTextListener(this);
            ((TextField)value.elementAt(2)).addTextListener(this);
            gd4.showDialog();
            
            if (gd4.wasCanceled()){
                imgA.getProcessor().resetThreshold();
                imgA.updateAndDraw();
                imgB.getProcessor().resetThreshold();
                imgB.updateAndDraw();
                return;
            }
            
            thrA=(int) gd4.getNextNumber();
            thrB=(int) gd4.getNextNumber();
            IJ.log("\nThreshold: A="+thrA+" & B="+thrB);
        }
                
        if (depthA!=depthB || widthA!=widthB || heightA!=heightB || nbsliceA!=nbsliceB){
            IJ.showMessage("Error", "Images should be of same caracteristics,\n"+"(Depth, height, width, nb of slices");
            return;
        }
        
        //Load images A & B into an aray and calculate min, max and mean on the full stack !!! (getStatistics only return value from current slice)
        for (k=1; k<=nbsliceA; k++){
            imgA.setSlice(k);
            imgB.setSlice(k);
            
            Amean+=widthA*heightA*imgA.getStatistics().mean;
            Amin=Math.min(imgA.getStatistics().min,Amin);
            Amax=Math.max(imgA.getStatistics().max,Amax);
            
            Bmean+=widthB*heightB*imgB.getStatistics().mean;
            Bmin=Math.min(imgB.getStatistics().min,Bmin);
            Bmax=Math.max(imgB.getStatistics().max,Bmax);
        
            for (j=0; j<heightA; j++){
                for (i=0; i<widthA; i++){
                    int location=offset(i,j,k);
                    A[location]=imgA.getProcessor().getPixel(i,j);
                    B[location]=imgB.getProcessor().getPixel(i,j);
                    if (CostesRandBool){
                        int locationCostes=offsetCostes(i,j,k);
                        ACostes[locationCostes]=A[location];
                        BCostes[locationCostes]=B[location];
                    }
                }
            }
        }
        Amean/=widthA*heightA*nbsliceA;
        Bmean/=widthB*heightB*nbsliceB;
        
        if (PearsonBool) Pearson();
        if (OverlapBool) Overlap();
        if (MMBool) MM();
        if (CostesThrBool) CostesThr();
        if (CCFBool) CCF();
        if (CytoBool) CytoFluo();
        if (ICABool) ICA();
        if (CostesRandBool) CostesRand();
        if (SpaPearBool) SpatialPearson();
        if (DistBool) Dist();
        
        imgA.getProcessor().resetThreshold();
        imgA.updateAndDraw();
        imgB.getProcessor().resetThreshold();
        imgB.updateAndDraw();
        
        //Save preferences
        Prefs.set("JACoP_Pearson.boolean", PearsonBool);
        Prefs.set("JACoP_Overlap.boolean", OverlapBool);
        Prefs.set("JACoP_MM.boolean", MMBool);
        Prefs.set("JACoP_CostesThr.boolean", CostesThrBool);
        Prefs.set("JACoP_CCF.boolean", CCFBool);
        Prefs.set("JACoP_CCFx.double", CCFx);
        Prefs.set("JACoP_Cyto.boolean", CytoBool);
        Prefs.set("JACoP_ICA.boolean", ICABool);
        Prefs.set("JACoP_CostesRand.boolean", CostesRandBool);
        Prefs.set("JACoP_Dist.boolean", DistBool);
        Prefs.set("JACoP_SpaPear.boolean", SpaPearBool);
        Prefs.set("JACoP_Line.boolean", lineBool);
        
        Prefs.set("JACoP_MicroscopeType.double",MicroscopeType);
        Prefs.set("JACoP_xycal.double",xycal);
        Prefs.set("JACoP_zcal.double",zcal);
        Prefs.set("JACoP_wa.double",wa);
        Prefs.set("JACoP_wb.double",wb);
        Prefs.set("JACoP_NA.double",NA);
        Prefs.set("JACoP_IR.double",IR);
        
        Prefs.set("JACoP_xyBlock.double",xyBlock);
        Prefs.set("JACoP_zBlock.double",zBlock);
        Prefs.set("JACoP_nbRand.double",nbRand);
        Prefs.set("JACoP_binWidth.double",binWidth);
        Prefs.set("JACoP_fillMeth.double",fillMeth);
        Prefs.set("JACoP_xyRand.boolean",xyRand);
        Prefs.set("JACoP_zRand.boolean",zRand);
        Prefs.set("JACoP_showRand.boolean",showRand);
        
        
        
    }
    
    public void Pearson() {
        doThat=true;
        IJ.log("\nPearson's Coefficient:\nr="+round(linreg(A,B,0,0)[2],3));
    }
        
    public void Overlap(){
        double num=0;
        double den1=0;
        double den2=0;
        
        for (i=0; i<A.length; i++){
            num+=A[i]*B[i];
            den1+=Math.pow(A[i], 2);
            den2+=Math.pow(B[i], 2);
        }
        
        double OverlapCoeff=num/(Math.sqrt(den1*den2));
        IJ.log("\nOverlap Coefficient:\nr="+round(OverlapCoeff,3));
        IJ.log("\nr²=k1xk2:\nk1="+round(num/den1,3)+"\nk2="+round(num/den2,3));
    }
    
    public void MM(){
        double sumAcoloc=0;
        double sumAcolocThr=0;
        double sumA=0;
        double sumBcoloc=0;
        double sumBcolocThr=0;
        double sumB=0;
        
        for (i=0; i<A.length; i++){
            if (B[i]>0) sumAcoloc+=A[i];
            if (B[i]>thrB) sumAcolocThr+=A[i];
            if (A[i]>0) sumBcoloc+=B[i];
            if (A[i]>thrA) sumBcolocThr+=B[i];
            sumA+=A[i];
            sumB+=B[i];
        }
                
        double M1=sumAcoloc/sumA;
        double M1Thr=sumAcolocThr/sumA;
        double M2=sumBcoloc/sumB;
        double M2Thr=sumBcolocThr/sumB;
        IJ.log("\nManders' Coefficients (original):\nM1="+round(M1,3)+" (fraction of A overlapping B)\nM2="+round(M2,3)+" (fraction of B overlapping A)");
        IJ.log("\nManders' Coefficients (using thresholds):\nM1="+round(M1Thr,3)+" (fraction of A overlapping B)\nM2="+round(M2Thr,3)+" (fraction of B overlapping A)");
    }
    
    public void CostesThr() {
        int CostesThrA=(int)Amax;
        int CostesThrB=(int)Bmax;
        double CostesSumAThr=0;
        double CostesSumA=0;
        double CostesSumBThr=0;
        double CostesSumB=0;
        double CostesPearson=1;
        double [] rx= new double[(int)(Amax-Amin+1)];
        double [] ry= new double[(int)(Amax-Amin+1)];
        double rmax=0;
        double rmin=1;
        doThat=true;
        int count=0;
        
        //First Step: define line equation
        doThat=true;
        double[] tmp=linreg(A,B,0,0);
        double a=tmp[0];
        double b=tmp[1];
        double CoeffCorr=tmp[2];
        doThat=false;
        
        int LoopMin= (int) Math.max(Amin, (Bmin-b)/a);
        int LoopMax= (int) Math.min(Amax, (Bmax-b)/a);
        
        
        //Minimize r of points below (thrA,a*thrA+b)
        for (i=LoopMax;i>=LoopMin;i--){
            IJ.showStatus("Costes' threshold calculation in progress : "+(int)(100*(LoopMax-i)/(LoopMax-LoopMin))+"% done");
            IJ.showProgress((int)LoopMax-i, (int)(LoopMax-LoopMin));
            
            if (IJ.escapePressed()) {
                IJ.showStatus("Task canceled by user");
                IJ.showProgress(2,1);
                return;
            }
            
            CostesPearson=linregCostes(A,B,i,a*i+b)[2];
            
            rx[count]=i;
            ry[count]=CostesPearson;
            rmax=Math.max(rmax,CostesPearson);
            rmin=Math.min(rmin,CostesPearson);
            count++;
            
            if (CostesPearson<=0){
                CostesThrA=i;
                CostesThrB=(int)(a*i+b);
                i=(int)Amin-1;
            }
        }
        
        
        for (i=0; i<A.length; i++){
            CostesSumA+=A[i];
            if (A[i]>CostesThrA) CostesSumAThr+=A[i];
            CostesSumB+=B[i];
            if (B[i]>CostesThrB) CostesSumBThr+=B[i];
        }
        
        Plot plot=new Plot("Costes' threshold "+titleA+" and "+titleB,"ThrA", "Pearson's coefficient below",rx,ry);
        plot.setLimits(LoopMax, CostesThrA, CostesPearson, rmax);
        plot.setColor(Color.black);
        plot.draw();
        if (lineBool){
           double[] xline={LoopMax, CostesThrA};
           double[] yline={0, 0};
           plot.setColor(Color.red);
           plot.addPoints(xline, yline, 2);
        }
        plot.show();
        
        ImagePlus CostesMask=NewImage.createRGBImage("Costes' mask",widthA,heightA,nbsliceA,0);
        CostesMask.getProcessor().setValue(Math.pow(2, depthA));
        for (k=1; k<=nbsliceA; k++){
            CostesMask.setSlice(k);
            for (j=0; j<heightA; j++){
                for (i=0; i<widthA; i++){
                    int position=offset(i,j,k);
                    int [] color=new int[3];
                    color[0]=(int) A[position];
                    color[1]=(int) B[position];
                    color[2]=0;
                    if (color[0]>CostesThrA && color[1]>CostesThrB){
                        //CostesMask.getProcessor().setValue(((A[position]-CostesThrA)/(LoopMax-CostesThrA))*Math.pow(2, depthA));
                        //CostesMask.getProcessor().drawPixel(i,j);
                        for (l=0; l<=2; l++) color[l]=255;
                    }
                    CostesMask.getProcessor().putPixel(i,j,color);
                }
            }
        }
        CostesMask.setSlice(1);
        CostesMask.show();
        //IJ.setMinAndMax(0,Math.pow(2, depthA));
        //IJ.run("Invert LUT");
        
        
        
        IJ.showStatus("");
        IJ.showProgress(2,1);
        
        IJ.log("\nCostes' automatic threshold set to "+CostesThrA+" for imgA & "+CostesThrB+" for imgB");
        IJ.log("Pearson's Coefficient:\nr="+round(linreg(A,B,CostesThrA,CostesThrB)[2],3)+" ("+round(CostesPearson,3)+" below thresholds)");
        IJ.log("M1="+round(CostesSumAThr/CostesSumA,3)+" & M2="+round(CostesSumBThr/CostesSumB,3));
        
    }
    
    public void CCF(){
        double num;
        double den1;
        double den2;
        double CCF0=0;
        double CCFmin=0;
        int lmin=-CCFx;
        double CCFmax=0;
        int lmax=-CCFx;
        
        double [] CCFarray=new double[2*CCFx+1];
        double [] x=new double[2*CCFx+1];
        
        int count=0;
        
        IJ.log("\nVan Steensel's Cross-correlation Coefficient between "+titleA+" and "+titleB+":");
        for (l=-CCFx; l<=CCFx; l++){
            IJ.showStatus("CCF calculation in progress: "+(count+1)+"/"+(2*CCFx+1));
            IJ.showProgress(count+1, 2*CCFx+1);
            
            if (IJ.escapePressed()) {
                IJ.showStatus("Task canceled by user");
                IJ.showProgress(2,1);
                return;
            }
            
            num=0;
            den1=0;
            den2=0;
            
            for (k=1; k<=nbsliceA; k++){
                for (j=0; j<heightA; j++){
                    for (i=0; i<widthA; i++){
                        if (i+l>0 && i+l<widthA){
                            int coord=offset(i,j,k);
                            int coordShift=offset(i+l,j,k);
                            
                            num+=(A[coord]-Amean)*(B[coordShift]-Bmean);
                            den1+=Math.pow((A[coord]-Amean), 2);
                            den2+=Math.pow((B[coordShift]-Bmean), 2);
                        }
                    }
                }
            }

            double CCF=num/(Math.sqrt(den1*den2));
            
            if (l==-CCFx){
                CCF0=CCF;
                CCFmin=CCF;
                CCFmax=CCF;
            }else{
                if (CCF<CCFmin){
                    CCFmin=CCF;
                    lmin=l;
                }
                if (CCF>CCFmax){
                    CCFmax=CCF;
                    lmax=l;
                }
            }
            x[count]=l;
            CCFarray[count]=CCF;
            count++;
        }
        IJ.log ("CCF min.: "+round(CCFmin,3)+" (obtained for dx="+lmin+") CCF max.: "+round(CCFmax,3)+" (obtained for dx="+lmax+")");
        Plot plot=new Plot("Van Steensel's CCF between "+titleA+" and "+titleB,"dx", "CCF",x,CCFarray);
        plot.setLimits(-CCFx, CCFx, CCFmin, CCFmax);
        plot.setColor(Color.black);
        plot.draw();
        
        if (lineBool){
           double[] xline={0,0};
           double[] yline={CCFmin,CCFmax};
           plot.setColor(Color.red);
           plot.addPoints(xline, yline, 2);
        }
        
        IJ.showStatus("");
        IJ.showProgress(2,1);
        
        plot.show();
        
    }
    
    public void CytoFluo(){
        //ImagePlus cyto=NewImage.createRGBImage("Cytofluo. "+titleB+"=f("+titleA+")", (int) Math.pow(2,depthA), (int) Math.pow(2,depthA), 1, 1);
        Plot plot = new Plot("Cytofluorogram between "+titleA+" and "+titleB, titleA, titleB, A, B);
        double limHigh=Math.max(Amax, Bmax);
        double limLow=Math.min(Amin, Bmin);
        plot.setLimits(limLow, limHigh, limLow, limHigh);
        plot.setColor(Color.white);
        
        doThat=true;
        double[] tmp=linreg(A,B,0,0);
        double a=tmp[0];
        double b=tmp[1];
        double CoeffCorr=tmp[2];
        plot.draw();
        plot.setColor(Color.black);
        plot.addPoints(A, B, 6);
        
        if (lineBool){
           double[] xline={limLow,limHigh};
           double[] yline={a*limLow+b,a*limHigh+b};
           plot.setColor(Color.red);
           plot.addPoints(xline, yline, 2);
        }
        
        //cyto.show();
        plot.show();
        IJ.log("\nCytofluorogram's parameters:\na: "+round(a,3)+"\nb: "+round(b,3)+"\nCorrelation coefficient: "+round(CoeffCorr,3));

    }
    
     public void ICA(){
        double[] array=new double[1];
        double[] Anorm=new double[A.length];
        double[] Bnorm=new double[A.length];
        double AnormMean=0;
        double BnormMean=0;
        double prodMin=0;
        double prodMax=0;
        double lim=0;
        double[] x= new double[A.length];
        double ICQ=0;
        
        //Intensities are normalized to range from 0 to 1
        for (i=0; i<A.length;i++){
            Anorm[i]=(A[i]-Amin)/Amax;
            Bnorm[i]=(B[i]-Bmin)/Bmax;
            AnormMean+=Anorm[i];
            BnormMean+=Bnorm[i];
        }
        AnormMean=AnormMean/A.length;
        BnormMean=BnormMean/A.length;
        
        
        
        for (i=0; i<A.length;i++){
            x[i]=(Anorm[i]-AnormMean)*(Bnorm[i]-BnormMean);
            if (x[i]>prodMax) prodMax=x[i];
            if (x[i]<prodMin) prodMin=x[i];
            if (x[i]>0) ICQ++;
       }
       
       if (Math.abs(prodMin)>Math.abs(prodMax)){
           lim=Math.abs(prodMin);
       }else{
           lim=Math.abs(prodMax);
       }
       
       ICQ=ICQ/A.length-0.5;
       
       Plot plotA = new Plot("ICA A ("+titleA+")", "(Ai-a)(Bi-b)", titleA, x, Anorm);
       plotA.setColor(Color.white);
       plotA.setLimits(-lim, lim, 0, 1);
       plotA.draw();
       plotA.setColor(Color.black);
       plotA.addPoints(x, Anorm, 6);
       
       if (lineBool){
           double[] xline={0,0};
           double[] yline={0,1};
           plotA.setColor(Color.red);
           plotA.addPoints(xline, yline, 2);
        }
       plotA.show();
       
       Plot plotB = new Plot("ICA B ("+titleB+")", "(Ai-a)(Bi-b)", titleB, x, Bnorm);
       plotB.setColor(Color.white);
       plotB.setLimits(-lim, lim, 0, 1);
       plotB.draw();
       plotB.setColor(Color.black);
       plotB.addPoints(x, Bnorm, 6);
       
       if (lineBool){
           double[] xline={0,0};
           double[] yline={0,1};
           plotB.setColor(Color.red);
           plotB.addPoints(xline, yline, 2);
        }
       plotB.show();
       
       IJ.log("\nLi's Intensity correlation coefficient:\nICQ: "+ICQ);
        
     }
     
     public void CostesRand(){
         double direction;
         int shift;
         int newposition;
         if (xyRand || nbsliceCostes==1){
             //If slices independent 2D there is no need to take into account the z thickness and ranndomization along z axis should not be done
             zBlock=1;
             zRand=false;
         }
         doThat=true;
         double r2test=linreg(ACostes, BCostes, 0, 0)[2];
         doThat=false;
         double[] arrayR= new double[nbRand];
         double mean=0;
         double SD=0;
         double Pval=0;
         double[] arrayDistribR= new double[(int)(2/binWidth+1)];
         
         
         for (f=0; f<nbRand; f++){
             
             //Randomization by shifting along x axis
             for (e=1; e<=nbsliceCostes-zBlock+1; e+=zBlock){
                 for (d=0; d<heightCostes-xyBlock+1; d+=xyBlock){
                     
                     //Randomization of the shift's direction
                     direction=1;
                     if(Math.random()<0.5) direction=-1;
                     //Randomization of the shift: should be a multiple of the xy block size
                     shift=((int) (direction*Math.random()*widthCostes/xyBlock))*xyBlock;
                     
                     for (a=0; a<widthCostes; a++){
                        for (b=d; b<d+xyBlock; b++){
                            for (c=e; c<e+zBlock; c++){
                                newposition=a+shift;
                                if (newposition>=widthCostes) newposition-=widthCostes;
                                if (newposition<0) newposition+=widthCostes;
                                BRandCostes[offsetCostes(newposition,b,c)]=BCostes[offsetCostes(a,b,c)];
                            }
                        }
                     }
                 }
             }
             for (i=0; i<BCostes.length; i++) BCostes[i]=BRandCostes[i];
             
             //Randomization by shifting along y axis
             for (e=1; e<=nbsliceCostes-zBlock+1; e+=zBlock){
                 for (d=0; d<widthCostes-xyBlock+1; d+=xyBlock){
                     
                     //Randomization of the shift's direction
                     direction=1;
                     if(Math.random()<0.5) direction=-1;
                     //Randomization of the shift: should be a multiple of the xy block size
                     shift=((int) (direction*Math.random()*heightCostes/xyBlock))*xyBlock;
                     
                     for (a=0; a<heightCostes; a++){
                        for (b=d; b<d+xyBlock; b++){
                            for (c=e; c<e+zBlock; c++){
                                newposition=a+shift;
                                if (newposition>=heightCostes) newposition-=heightCostes;
                                if (newposition<0) newposition+=heightCostes;
                                BRandCostes[offsetCostes(b,newposition,c)]=BCostes[offsetCostes(b,a,c)];
                            }
                        }
                     }
                 }
             }
             for (i=0; i<BCostes.length; i++) BCostes[i]=BRandCostes[i];
             
             if (zRand){
                 //Randomization by shifting along z axis
                 for (e=0; e<heightCostes-xyBlock+1; e+=xyBlock){
                     for (d=0; d<widthCostes-xyBlock+1; d+=xyBlock){

                         //Randomization of the shift's direction
                         direction=1;
                         if(Math.random()<0.5) direction=-1;
                         //Randomization of the shift: should be a multiple of the z block size
                         shift=((int) (direction*Math.random()*nbsliceCostes/zBlock))*zBlock;

                         for (a=1; a<=nbsliceCostes; a++){
                            for (b=d; b<d+xyBlock; b++){
                                for (c=e; c<e+xyBlock; c++){
                                    newposition=a+shift;
                                    if (newposition>nbsliceCostes) newposition-=nbsliceCostes;
                                    if (newposition<1) newposition+=nbsliceCostes;
                                    BRandCostes[offsetCostes(b,c,newposition)]=BCostes[offsetCostes(b,c,a)];
                                }
                            }
                         }
                     }
                 }
                 for (i=0; i<BCostes.length; i++) BCostes[i]=BRandCostes[i];
             }
         arrayR[f]=linreg(ACostes, BCostes, 0, 0)[2];
         if (arrayR[f]<r2test) Pval++;
         mean+=arrayR[f];
         arrayDistribR[(int)((arrayR[f]+1)/binWidth)]++;
         IJ.showStatus("Costes' randomization loop n°"+f+"/"+nbRand);
         }
         
         //Draw the last randomized image, if requiered
         if (showRand){
             ImagePlus Rand=NewImage.createShortImage("Randomized images of "+titleB,widthCostes,heightCostes,nbsliceCostes,0);
             for (k=1; k<=nbsliceCostes; k++){
                 Rand.setSlice(k);
                 for (j=0;j<heightCostes; j++){
                     for (i=0; i<widthCostes;i++){
                         Rand.getProcessor().setValue(BRandCostes[offsetCostes(i,j,k)]);
                         Rand.getProcessor().drawPixel(i,j);
                     }
                 }
             }
             Rand.setSlice(1);
             Rand.show();
             IJ.setMinAndMax(Bmin,Bmax);
             IJ.run("Invert LUT");
         }
         
         //Plots the r probability distribution
         double[] x= new double[arrayDistribR.length];
         double minx=-1;
         double maxx=1;
         double maxy=0;
         
         for (i=0; i<arrayDistribR.length;i++){
             x[i]=i*binWidth-1;
             if (minx==-1 && arrayDistribR[i]!=0) minx=x[i]-binWidth;
             if (maxy<arrayDistribR[i]) maxy=arrayDistribR[i];
         }
         minx=Math.min(minx,r2test-binWidth);
         
         i=arrayDistribR.length-1;
         while (arrayDistribR[i]==0) {
             maxx=x[i]+binWidth;
             i--;
         }
         
         maxx=Math.max(maxx,r2test+binWidth);
         
         Plot plot = new Plot("Costes' method ("+titleA+" & "+titleB+")", "r", "Probability density of r", x, arrayDistribR);
         plot.setColor(Color.black);
         plot.setLimits(minx, maxx, 0, maxy);
         plot.draw();
         double[] xline={r2test,r2test};
         double[] yline={0,maxy};
         plot.setColor(Color.red);
         plot.addPoints(xline, yline, 2);
         plot.show();
         
         
         //Retrieves the mean, SD and P-value of the r distribution
         for (i=1; i<nbRand; i++) SD+=Math.pow(arrayR[i]-mean,2);
         mean/=nbRand;
         SD=Math.sqrt(SD/(nbRand-1));
         Pval/=nbRand;
         
         
         IJ.log("\nCostes' randomization based colocalization:\nParameters: Nb of randomization rounds: "+nbRand+", Resolution (bin width): "+binWidth);
         IJ.log("r (original)="+round(r2test,3)+"\nr (randomized)="+round(mean,3)+"±"+round(SD,3)+"\nP-value="+round(Pval*100,2)+"%");
         
     }
    
    public void Dist() {
        ImagePlus DistA=NewImage.createShortImage("DistA "+titleA,widthA,heightA,nbsliceA,0);
        ImagePlus DistB=NewImage.createShortImage("DistB "+titleB,widthB,heightB,nbsliceB,0);
        ImagePlus InterA=NewImage.createShortImage("InterA "+titleA,widthA,heightA,nbsliceA,0);
        ImagePlus InterB=NewImage.createShortImage("InterB "+titleB,widthB,heightB,nbsliceB,0);
        boolean alreadydone;
        int countA=0;
        int thrpixA=0;
        int countB=0;
        int thrpixB=0;
        
        for (k=1; k<=nbsliceA; k++){
            for (j=0; j<heightA; j++){
                for (i=0; i<widthA; i++){
                    alreadydone=false;
                    if (A[offset(i,j,k)]>thrA){
                        for (c=k-pixz; c<=k+pixz; c++){
                            for (b=j-pixxy; b<=j+pixxy; b++){
                                for (a=i-pixxy; a<i+pixxy; a++){
                                    if (B[offset(a,b,c)]>thrB){
                                        if (!alreadydone){
                                            DistA.setSlice(k);
                                            DistA.getProcessor().setValue(A[offset(i,j,k)]);
                                            DistA.getProcessor().drawPixel(i,j);
                                            
                                            InterA.setSlice(k);
                                            InterA.getProcessor().setValue(A[offset(i,j,k)]);
                                            InterA.getProcessor().drawPixel(i,j);
                                            
                                            alreadydone=true;
                                        }
                                        
                                        if (i==a && j==b && k==c){
                                            InterB.setSlice(k);
                                            InterB.getProcessor().setValue(B[offset(i,j,k)]);
                                            InterB.getProcessor().drawPixel(i,j);
                                            
                                        }
                                        
                                        DistB.setSlice(c);
                                        DistB.getProcessor().setValue(B[offset(a,b,c)]);
                                        DistB.getProcessor().drawPixel(a,b);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        for (k=1; k<=nbsliceA; k++){
            for (j=0; j<heightA; j++){
                for (i=0; i<widthA; i++){
                    if (A[offset(i,j,k)]>thrA) thrpixA++;
                    if (B[offset(i,j,k)]>thrB) thrpixB++;
                    DistA.setSlice(k);
                    DistB.setSlice(k);
                    if (DistA.getProcessor().getPixel(i,j)!=0) countA++;
                    if (DistB.getProcessor().getPixel(i,j)!=0) countB++;
                }
            }
        }
                    
        DistA.setSlice(1);
        DistA.show();
        IJ.setMinAndMax(imgA.getProcessor().getMin(),imgA.getProcessor().getMax());
        IJ.run("Invert LUT");
        
        DistB.setSlice(1);
        DistB.show();
        IJ.setMinAndMax(imgB.getProcessor().getMin(),imgB.getProcessor().getMax());
        IJ.run("Invert LUT");
        
        InterA.setSlice(1);
        InterA.show();
        IJ.setMinAndMax(imgA.getProcessor().getMin(),imgA.getProcessor().getMax());
        IJ.run("Invert LUT");
        
        InterB.setSlice(1);
        InterB.show();
        IJ.setMinAndMax(imgB.getProcessor().getMin(),imgB.getProcessor().getMax());
        IJ.run("Invert LUT");
        
        IJ.log("\nDistance based colocalization:\n% of positive A thresholded pixels="+100*countA/thrpixA+"\n% of positive B thresholded pixels="+100*countB/thrpixB);
        
    }
    
    public void SpatialPearson() {
        ImagePlus SpaPear=NewImage.createFloatImage("Spatial Pearson of "+titleA+" & "+titleB,widthA,heightA,nbsliceA,0);
        ImageProcessor ip;
        double locMeanA=0;
        double locMeanB=0;
        double num=0;
        double den1=0;
        double den2=0;
        
        for (k=1; k<=nbsliceA; k++){
            SpaPear.setSlice(k);
            ip=SpaPear.getProcessor();
            for (j=0; j<heightA; j++){
                for (i=0; i<widthA; i++){
                    num=0;
                    den1=0;
                    den2=0;
                    locMeanA=0;
                    locMeanB=0;
                    d=0;
                    
                    //Calculation of local means
                    for (c=k-pixz; c<=k+pixz; c++){
                        for (b=j-pixxy; b<=j+pixxy; b++){
                            for (a=i-pixxy; a<i+pixxy; a++){
                                locMeanA+=A[offset(a,b,c)];
                                locMeanB+=B[offset(a,b,c)];
                                d++;
                            }
                        }
                    }
                    locMeanA/=d;
                    locMeanB/=d;
                    
                    //Calculation of the local Pearson's coefficient
                    for (c=k-pixz; c<=k+pixz; c++){
                        for (b=j-pixxy; b<=j+pixxy; b++){
                            for (a=i-pixxy; a<i+pixxy; a++){
                                num+=(A[offset(a,b,c)]-locMeanA)*(B[offset(a,b,c)]-locMeanB);
                                den1+=Math.pow((A[offset(a,b,c)]-locMeanA), 2);
                                den2+=Math.pow((B[offset(a,b,c)]-locMeanB), 2);
                            }
                        }
                    }
                ip.putPixelValue(i,j,num/(Math.sqrt(den1*den2)));     
                }
            }
        }
        SpaPear.show();
        SpaPear.updateAndDraw();
        IJ.run("Invert LUT");
        
        
        
    }
 
    
        
    public void adjustmentValueChanged(AdjustmentEvent e) {
        thrA=((Scrollbar)sliders.elementAt(0)).getValue();
        thrB=((Scrollbar)sliders.elementAt(1)).getValue();
        imgA.getProcessor().setThreshold(thrA,Math.pow(2,16),ImageProcessor.RED_LUT);
        imgB.getProcessor().setThreshold(thrB,Math.pow(2,16),ImageProcessor.RED_LUT);
        imgA.setSlice(((Scrollbar)sliders.elementAt(2)).getValue());
        imgB.setSlice(((Scrollbar)sliders.elementAt(2)).getValue());
        imgA.updateAndDraw();
        imgB.updateAndDraw();
    }
    
    public void textValueChanged(TextEvent e) {
        ((Scrollbar)sliders.elementAt(0)).setValue((int) Tools.parseDouble(((TextField)value.elementAt(0)).getText()));
        ((Scrollbar)sliders.elementAt(1)).setValue((int) Tools.parseDouble(((TextField)value.elementAt(1)).getText()));
        ((Scrollbar)sliders.elementAt(2)).setValue((int) Tools.parseDouble(((TextField)value.elementAt(2)).getText()));
        
        if ((int) Tools.parseDouble(((TextField)value.elementAt(2)).getText())>nbsliceA){
            ((Scrollbar)sliders.elementAt(2)).setValue(nbsliceA);
            ((TextField)value.elementAt(2)).setText(""+nbsliceA);
        }
        if ((int) Tools.parseDouble(((TextField)value.elementAt(2)).getText())<1){
                ((Scrollbar)sliders.elementAt(2)).setValue(1);
                ((TextField)value.elementAt(2)).setText("1");
        }
        thrA=((Scrollbar)sliders.elementAt(0)).getValue();
        imgA.getProcessor().setThreshold(thrA,Math.pow(2,16),ImageProcessor.RED_LUT);
        imgA.updateAndDraw();
        thrB=((Scrollbar)sliders.elementAt(1)).getValue();
        imgB.getProcessor().setThreshold(thrB,Math.pow(2,16),ImageProcessor.RED_LUT);
        imgB.updateAndDraw();
        imgA.setSlice(((Scrollbar)sliders.elementAt(2)).getValue());
        imgB.setSlice(((Scrollbar)sliders.elementAt(2)).getValue());
    }
    
     public double round(double y, int z){
         //Special tip to round numbers to 10^-2
         y*=Math.pow(10,z);
         y=(int) y;
         y/=Math.pow(10,z);
         return y;
     }
     
     public int offset(int m,int n,int o){
        if (m+n*widthA+(o-1)*widthA*heightA>=widthA*heightA*nbsliceA){
            return widthA*heightA*nbsliceA-1;
        }else{
            if (m+n*widthA+(o-1)*widthA*heightA<0){
                return 0;
            }else{
                return m+n*widthA+(o-1)*widthA*heightA;
            }
        }
    }
     
    public int offsetCostes(int m,int n,int o){
        if (m+n*widthCostes+(o-1)*widthCostes*heightCostes>=widthCostes*heightCostes*nbsliceCostes){
            return widthCostes*heightCostes*nbsliceCostes-1;
        }else{
            if (m+n*widthCostes+(o-1)*widthCostes*heightCostes<0){
                return 0;
            }else{
                return m+n*widthCostes+(o-1)*widthCostes*heightCostes;
            }
        }
    } 
     
     public double[] linreg(double[] Aarray, double[] Barray, double TA, double TB){
         double num=0;
         double den1=0;
         double den2=0;
         double[] coeff=new double[6];
         int count=0;
         
         if (doThat){
             sumA=0;
             sumB=0;
             sumAB=0;
             sumsqrA=0;
             Aarraymean=0;
             Barraymean=0;
             for (m=0; m<Aarray.length; m++){
                if (Aarray[m]>=TA && Barray[m]>=TB){
                    sumA+=Aarray[m];
                    sumB+=Barray[m];
                    sumAB+=Aarray[m]*Barray[m];
                    sumsqrA+=Math.pow(Aarray[m],2);
                    count++;
                }
             }

             Aarraymean=sumA/count;
             Barraymean=sumB/count;
         }
         
         for (m=0; m<Aarray.length; m++){
            if (Aarray[m]>=TA && Barray[m]>=TB){
                num+=(Aarray[m]-Aarraymean)*(Barray[m]-Barraymean);
                den1+=Math.pow((Aarray[m]-Aarraymean), 2);
                den2+=Math.pow((Barray[m]-Barraymean), 2);
            }
         }
        
        //0:a, 1:b, 2:corr coeff, 3: num, 4: den1, 5: den2
        coeff[0]=(count*sumAB-sumA*sumB)/(count*sumsqrA-Math.pow(sumA,2));
        coeff[1]=(sumsqrA*sumB-sumA*sumAB)/(count*sumsqrA-Math.pow(sumA,2));
        coeff[2]=num/(Math.sqrt(den1*den2));
        coeff[3]=num;
        coeff[4]=den1;
        coeff[5]=den2;
        return coeff;
     }
     
     public double[] linregCostes(double[] Aarray, double[] Barray, double TA, double TB){
         double num=0;
         double den1=0;
         double den2=0;
         double[] coeff=new double[3];
         int count=0;
         
         sumA=0;
         sumB=0;
         sumAB=0;
         sumsqrA=0;
         Aarraymean=0;
         Barraymean=0;
         
         for (m=0; m<Aarray.length; m++){
            if (Aarray[m]<TA && Barray[m]<TB){
                sumA+=Aarray[m];
                sumB+=Barray[m];
                sumAB+=Aarray[m]*Barray[m];
                sumsqrA+=Math.pow(Aarray[m],2);
                count++;
            }
        }

             Aarraymean=sumA/count;
             Barraymean=sumB/count;
                  
         
         for (m=0; m<Aarray.length; m++){
            if (Aarray[m]<TA && Barray[m]<TB){
                num+=(Aarray[m]-Aarraymean)*(Barray[m]-Barraymean);
                den1+=Math.pow((Aarray[m]-Aarraymean), 2);
                den2+=Math.pow((Barray[m]-Barraymean), 2);
            }
         }
        
        coeff[0]=(count*sumAB-sumA*sumB)/(count*sumsqrA-Math.pow(sumA,2));
        coeff[1]=(sumsqrA*sumB-sumA*sumAB)/(count*sumsqrA-Math.pow(sumA,2));
        coeff[2]=num/(Math.sqrt(den1*den2));
        return coeff;
     }
    
}

