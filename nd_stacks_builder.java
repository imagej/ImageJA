/*nd stack builder v1.0, 15/06/05
    Fabrice P Cordelières, fabrice.cordelieres at curie.u-psud.fr
*/

import ij.plugin.*;
import java.awt.*;
import java.io.*;
import java.lang.Exception.*;
import java.lang.String.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.util.*;

public class nd_stacks_builder implements PlugIn {
    String File;
    String FileExt;
    String FileName;
    String FileOpen;
    String FileSave;
    ImagePlus img;
    ImageStack result;
    ImagePlus stack;
    String StackName;
    String dir1;
    String dir2;
    String SaveAs;
    
    int WaveSelected;
    int PositionSelected;
    boolean TimepointChoice;
    boolean WaveChoice;
    boolean PositionChoice;
    boolean PositionInFileName;
    int StartTimepoint;
    int EndTimepoint;
    
    String line;
    String field;
    String info;
    String Message;
    
    //Variables for readND------------
    String Progression;
    BufferedReader in;
    int NreadLine;
    boolean DoTimelapse;
    int NTimePoints;
    String Description;
    boolean DoStage;
    int NStagePositions;
    String[] Stage;
    boolean DoWave;
    int NWavelengths;
    String[] WaveName;
    boolean[] WaveDoZ;
    int[][] WavePointsCollected;
    boolean WPC;
    String ImageName;
    boolean DoZSeries;
    int NZSteps;
    double ZStepSize;
    boolean WaveInFileName;
    boolean convert;
    
    //Variables for DoProj------------
    int StartSlice;
    int EndSlice;
    String[] ProjMeth = {"Average", "Maximum", "Minimum", "Sum", "SD", "Median"};
    int ProjMethInt;
    ImagePlus Proj;
    //--------------------------------
    
    int countTtl;
    int count;
    int i;
    int j;
    int k;
    int l;
    
    
    public void run(String arg) {
        //Open dialog-----------------------------------------------------------------
        OpenDialog od = new OpenDialog("Select the nd file in source folder...", "");
        if (od.getFileName()==null) return;
        File=od.getFileName();
        FileExt=File.substring(File.lastIndexOf(".")+1,File.length());
        FileName=File.substring(0,File.indexOf("."));
        dir1 = od.getDirectory();
        if(!FileExt.toLowerCase().equals("nd")){
            GenericDialog gd=new GenericDialog("!!! Warning !!!");
            gd.addMessage("This is not a ND file:\nProceed anyway ?");
            gd.showDialog();
            if (gd.wasCanceled()) return;
            NotANd();
            return;
        }
        //----------------------------------------------------------------------------
        
        readND();
        
        //Choice of elements to build-------------------------------------------------
        GenericDialog nd = new GenericDialog("nd stacks builder");
        if (DoTimelapse){
            nd.addMessage("Timelapse detected, "+NTimePoints+" timepoints.");
            nd.addNumericField("First timepoint: ",1,0);
            nd.addNumericField("Last timepoint: ",NTimePoints,0);
            nd.addMessage("");
        }
        
        
        if (DoWave){
            nd.addChoice(NWavelengths+" wavelengths: ",WaveName,WaveName[0]);
        } else {
            nd.addMessage("1 wavelength: "+ImageName);
        }
        if (DoStage){
            nd.addChoice(NStagePositions+" positions: ",Stage,Stage[0]);
        } else {
            nd.addMessage("1 position");
        }
        
        if (DoTimelapse) nd.addCheckbox("All timepoints", true);
        if (DoWave) nd.addCheckbox("All wavelengths",true);
        if (DoStage) nd.addCheckbox("All positions",true);
        
        if (DoZSeries){
            nd.addMessage("");
            nd.addMessage("Z series detected, "+NZSteps+" slices.");
            nd.addChoice("Do projection, Method: ",ProjMeth,"Maximum");
            nd.addNumericField("Top slice: ", 1, 0);
            nd.addNumericField("Bottom slice: ", NZSteps, 0);
        }
        
        if (NWavelengths==1) WaveInFileName=false;
        nd.addMessage("");
        nd.addCheckbox("WaveInFilename",WaveInFileName);
        nd.addCheckbox("PostionInFilename",false);
        nd.addMessage("");
        nd.addCheckbox("Convert to 8-bits", false);
        
        nd.showDialog();
        if (nd.wasCanceled()) return;
        
        if (DoTimelapse){
            StartTimepoint = (int)nd.getNextNumber();
            EndTimepoint = (int)nd.getNextNumber();
            if (EndTimepoint>NTimePoints) EndTimepoint=NTimePoints;
            if (StartTimepoint<1) StartTimepoint=1;
            if (EndTimepoint<StartTimepoint){
                int tmp=EndSlice;
                EndTimepoint=StartTimepoint;
                StartTimepoint=tmp;
            }
        }
        
        if (DoWave) WaveSelected=nd.getNextChoiceIndex();
        if (DoStage)PositionSelected=nd.getNextChoiceIndex();
        if (DoTimelapse) TimepointChoice=nd.getNextBoolean();
        if (DoWave)WaveChoice=nd.getNextBoolean();
        if (DoStage)PositionChoice=nd.getNextBoolean();
        
        if (DoZSeries){
            ProjMethInt= nd.getNextChoiceIndex();
            StartSlice = (int)nd.getNextNumber();
            EndSlice = (int)nd.getNextNumber();
            
            if (EndSlice<StartSlice){
                int tmp=EndSlice;
                EndSlice=StartSlice;
                StartSlice=tmp;
            }
        }
        
        WaveInFileName=nd.getNextBoolean();
        PositionInFileName=nd.getNextBoolean();
        convert=nd.getNextBoolean();
        
       //Save dialog-----------------------------------------------------------------
        SaveDialog sd = new SaveDialog("Open destination folder...", "---Destination Folder---", "");
        if (!sd.getFileName().equals("---Destination Folder---")){
            SaveAs=sd.getFileName()+"_";
            }else{
                SaveAs="";
            }
        dir2 = sd.getDirectory();
        //----------------------------------------------------------------------------
        
        buildStack();
        
    }
    
    public void readND() {
        
        //Setup the opening of file
        Progression="begin";
        NreadLine=0;
        
        //Check if the file is a ND file
        readLine();
        if (!field.equals("NDInfoFile")){
            IJ.showMessage("This is not a ND file");
            return;
        }
        
        Progression="progress";
        readLine();
        Description=info;
        
        //Get Timelapse info
        readLine();
        DoTimelapse=returnBool();
        if (DoTimelapse){
            readLine();
            NTimePoints=(int) Tools.parseDouble(info);
            readLine();
        } else {
            NTimePoints=1;
            readLine();
        }
        
        //Get Multipositioning info
        DoStage=returnBool();
        if (DoStage){
            readLine();
            NStagePositions=(int) Tools.parseDouble(info);
            Stage=new String[NStagePositions];
            for (i=0;i<NStagePositions;i++){
                readLine();
                Stage[i]=info.substring(1,info.length()-1);;
            }
        } else {
            NStagePositions=1;
        }
        
        
        //Get Multiwavelength info
        readLine();
        DoWave=returnBool();
        if (DoWave){
            readLine();
            NWavelengths=(int) Tools.parseDouble(info);
            WaveName=new String[NWavelengths];
            WaveDoZ=new boolean[NWavelengths];
            for (i=0;i<NWavelengths;i++){
                readLine();
                WaveName[i]=info.substring(1,info.length()-1);
                readLine();
                WaveDoZ[i]=returnBool();
            }
            readLine();
        } else {
            readLine();
            ImageName=info.substring(1,info.length()-1);;
            NWavelengths=1;
            WaveDoZ=new boolean[NWavelengths];
            WaveDoZ[0]=false;
        }
        
        
        //Deals with collection frequencies
        WPC=false;
        WavePointsCollected= new int[NWavelengths][NTimePoints];
        while (field.equals("WavePointsCollected")){
            //***************************************************************
            i=(int) Tools.parseDouble(info.substring(0,info.indexOf(",")))-1;
            info=info.substring((info.indexOf(","))+2,info.length());
            j=0;
            k=0;
            while (info.indexOf(",")!=-1){
                k=(int) Tools.parseDouble(info.substring(0,info.indexOf(",")));
                WavePointsCollected[i][j]=k;
                info=info.substring((info.indexOf(","))+2,info.length());
                
                j++;
            }
            WavePointsCollected[i][j]=(int) Tools.parseDouble(info);
            //***************************************************************
            
            WPC=true;
            readLine();
        }
        if (!WPC){
            if (DoWave){
                j=NreadLine-1;
            } else {
                j=NreadLine;
            }
            Progression="end";
            readLine();
            Progression="begin";
            for (i=0;i<j;i++){
                if (i==1) Progression="progress";
                readLine();
            }
            readLine();
        }
        
        
        //Get ZSeries info
        DoZSeries=returnBool();
        if (DoZSeries){
            readLine();
            NZSteps=(int) Tools.parseDouble(info);
            readLine();
            ZStepSize=Tools.parseDouble(info);
            if (!DoWave) WaveDoZ[0]=true;
        }
        
        
        //Get WaveInFileName info
        readLine();
        WaveInFileName=returnBool();
        
        
        //Close the ND file
        Progression="end";
        readLine();
    }
    
    public void readLine() {
        
        try{
            if (Progression.equals("begin")) in = new BufferedReader(new FileReader(dir1+File));
            if (Progression.equals("end")){
                in.close();
                NreadLine=0;
                return;
            }
            if (((line=in.readLine())!= null) && (line.indexOf(",") !=-1)) {
                field=line.substring(1,(line.indexOf(","))-1);
                info=line.substring((line.indexOf(","))+2);
                NreadLine++;
            }
        } catch ( IOException e ) {
            IJ.error("Error...");
        }
    }
    
    public boolean returnBool() {
        if (info.equals("TRUE")){
            return (true);
        } else {
            return (false);
        }
    }
    
    public void buildStack(){
        
        int iLow=0;
        int iHigh=NWavelengths;
        int jLow=0;
        int jHigh=NStagePositions;
        int kLow=0;
        int kHigh=NTimePoints;
        
        
        if (!WaveChoice){
            iLow=WaveSelected;
            iHigh=iLow+1;
        }
        
         if (!PositionChoice){
            jLow=PositionSelected;
            jHigh=jLow+1;
        }
        
        if (!TimepointChoice){
            kLow=StartTimepoint;
            kHigh=EndTimepoint+1;
        }
        
        l=0;
        countTtl=(iHigh-iLow)*(jHigh-jLow)*(kHigh-kLow);
        count=countTtl;
        
        for (i=iLow; i<iHigh;i++){
            for (j=jLow;j<jHigh;j++){
                for (k=kLow;k<kHigh;k++){
                    count--;
                    IJ.showProgress(countTtl-count,countTtl);
                    IJ.showStatus("Processing image "+(countTtl-count)+"/"+countTtl);
                    FileOpen=FileName;
                    FileSave=SaveAs+FileName;
                    
                    if (DoWave){
                        if (WaveInFileName){
                            FileOpen=FileOpen+"_w"+(i+1)+WaveName[i];
                            FileSave=FileSave+"_"+WaveName[i];
                        } else {
                            FileOpen=FileOpen+"_w"+(i+1);
                            FileSave=FileSave+"_"+WaveName[i];
                        }
                    }
                    
                    
                    if (DoStage){
                        if (PositionInFileName){
                            FileOpen=FileOpen+"_s"+(j+1)+Stage[j];
                            FileSave=FileSave+"_"+Stage[j];
                        } else {
                            FileOpen=FileOpen+"_s"+(j+1);
                            FileSave=FileSave+"_"+Stage[j];
                        }
                    }
                    
                    StackName=FileSave;
                    if (DoTimelapse) FileOpen=FileOpen+"_t"+(k+1);
                    if (DoZSeries & WaveDoZ[i]){
                        FileOpen=FileOpen+".stk";
                    } else {
                        FileOpen=FileOpen+".tif";
                    }
                    File f = new File(dir1+"/"+FileOpen);
                    if (!f.isDirectory()) {
                        img = new Opener().openImage(dir1, FileOpen);
                        if (img!=null) {
                            if (WaveDoZ[i] && img.getStackSize()!=1) img = DoProj(img);
                            if (l==0)result=new ImageStack(img.getWidth(), img.getHeight());
                            if (convert) img.setProcessor("Img",img.getProcessor().convertToByte(true));
                            if (result.getWidth()==img.getWidth() && result.getHeight()==img.getHeight()) result.addSlice(FileOpen,img.getProcessor());
                            img.flush();
                            l++;
                        }
                    }
                }
                if (result!=null){
                    stack=new ImagePlus(StackName, result);
                    save(stack,dir2);
                    stack.flush();
                    l=0;
                }
            }
            
        }
        IJ.showProgress(2,1);
        IJ.showStatus("Done");
    }
    
    public void NotANd(){
        //Save dialog-----------------------------------------------------------------
        SaveDialog sd = new SaveDialog("Open destination folder...", "---Destination Folder---", "");
        if (!sd.getFileName().equals("---Destination Folder---")){
            SaveAs=sd.getFileName();
            }else{
                SaveAs="Stack";
            }
        dir2 = sd.getDirectory();
        
        String[] temp = new File(dir1).list();
	if (temp==null) return;
                
        String[] list=new String[temp.length];
                
                
        // Filtering of the files & directories list using the extension of the 1st clicked file
        // This will allow the "projection setting" window to pop up even if the 1st member in the temp list
        //is a directory.
        for (i=0; i<temp.length;i++) {
            File f = new File(temp[i]);
            if (!f.isDirectory() && (temp[i].substring(temp[i].lastIndexOf(".")+1,temp[i].length())).equals(FileExt)) {
                list[j]=temp[i];
                j++;
            }
        }
                
        for (i=0; i<list.length; i++) {
            IJ.showStatus("Processing image "+i+"/"+list.length);
            IJ.showProgress(i,list.length);
            File f = new File(dir1+list[i]);
            if (!f.isDirectory()) {
                ImagePlus img = new Opener().openImage(dir1, list[i]);
                if (img!=null) {
                    if (i==0){
                        result=new ImageStack(img.getWidth(), img.getHeight());
                        if (img.getStackSize()!=1){
                            GenericDialog gd = new GenericDialog("3D projection parameters");
                            gd.addMessage("Keep Planes");
                            gd.addNumericField("Start Slice: ", 1, 0);
                            gd.addNumericField("End Slice: ", img.getStackSize(), 0);
                            gd.addMessage("Projection");
                            gd.addChoice("Projection Method: ",ProjMeth,"Maximum");
                            gd.showDialog();
                            if (gd.wasCanceled()) return;

                            StartSlice = (int)gd.getNextNumber();
                            EndSlice = (int)gd.getNextNumber();

                            if (EndSlice<StartSlice){
                                    int tmp=EndSlice;
                                    EndSlice=StartSlice;
                                    StartSlice=tmp;
                            }

                            if (EndSlice>img.getStackSize()) {
                                    EndSlice=img.getStackSize();
                            }

                            ProjMethInt= gd.getNextChoiceIndex();
                        }
                    }
                    if (img.getWidth()==result.getWidth() && img.getHeight()==result.getHeight()){
                        String title=img.getTitle();
                        if (img.getStackSize()!=1) img = DoProj(img);
                        result.addSlice(list[i],img.getProcessor());
                    }
                    
                    img.flush();
                    
                }
            }
        }
        if (result!=null){
            stack=new ImagePlus(SaveAs, result);
            save(stack,dir2);
            stack.flush();
        }
        IJ.showProgress(2,1);
        IJ.showStatus("Done");
    }
      
    
    public ImagePlus DoProj(ImagePlus img) {
        ZProjector zproj=new ZProjector(img);
        zproj.setStartSlice(StartSlice);
        zproj.setStopSlice(EndSlice);
        zproj.setMethod(ProjMethInt);
        zproj.doProjection();
        Proj=zproj.getProjection();
        Proj.setFileInfo(img.getFileInfo());
        return Proj;
    }
    
    public void save(ImagePlus img, String dir) {
        String name = img.getTitle();
        String path = dir + name;
        if (img.getStackSize()>1) {
            new FileSaver(img).saveAsTiffStack(path+".stk");
        } else {
            new FileSaver(img).saveAsTiff(path+".tif");
        }
    }
}
