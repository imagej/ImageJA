import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import java.awt.*;
import ij.plugin.*;
import ij.util.Tools;
import java.io.*;
import java.text.DecimalFormat;
import ij.measure.Calibration;

/**
 * This Plugin contains the methods which are needed to import 
 * pictures taken with a PerkinElmer spinning disk microscope. 
 * The Plugin is able to handle from UltraviewRS 
 * as well as data taken with am UltraviewERS
 *  
 * @author Arne Seitz
 * 
 * 09.02.2007 line 500 added;
 * 06.03.2007 Bugfix additional underscore in name;
 * 12.03.2007 Added Tif conversion option
 * 21.03.2007 Added extension ".nam" to skiplist (for PE-RS)
 * 26.03.2007 Added extension ".roi" to skiplist (for PE-ERS)
 *
 */
public class PerkinElmer_Reader implements PlugIn {
	
		public String []list;
		public String []name;
		public String []peinfo = new String [15];
		public String savename;
		private String dir;
		private	String xdim;
		private	String ydim;
		private	String tdim;
		private	String cdim;
		private	String zdim;
		public int nChannels;	
		public int nSlices;
		public int nFrames;
		public Calibration cal=new Calibration();
		public int cfg,tim,htm;
 		
		public int skip;
		public FileInfo fi = new FileInfo();
		private DecimalFormat df = new DecimalFormat("0000");
		
			
	public void run(String arg) {
		OpenDialog od = new OpenDialog("Select a file in source folder...", "");
		if (od.getFileName()==null) return;
		dir = od.getDirectory();
		if (readinfo()) {
			init();
			ImagePlus imp=open();
			imp.setCalibration(cal);
			//peo.image5D(imp);
			//updateInfoFrame(peo.peinfo);
			imp.show();
		}
	}		
			
		String[] sort(String[]list1, String []list2){
			if (list1.length!=list2.length){
				IJ.log("Both Arrays must be of the same length");
				return list1;
			}
			for (int i=0;i<list1.length-1;i++){
		        	
	        	for (int j=i+1; j<list.length;j++){
			        		
	        		if (list1[i].compareTo(list1[j])>0){
	        			String change=list1[i];
	
	        			list1[i]=list1[j];
	        			list1[j]= change;
	        			change=list2[i];
	        			list2[i]=list2[j];
	        			list2[j]=change;
	        		}
	        	}
	        	//IJ.log("sort"+i+"  "+list1[i]+"  "+list2[i]);
	        	
	        }
			
			
			return list2;
			
		}
		void init(){
			
			nChannels=Integer.parseInt(peinfo[5]);	
			nSlices=Integer.parseInt(peinfo[6]);
			nFrames=Integer.parseInt(peinfo[7]);
 		
			createlist();
			list=sort(name,list);
			
		}
		void createlist(){
			
			// make a list with all the files to open; do not include files which are not to open;
			// files with the extension in "skiplist" mustn't be added to the list
			
			String[] skiplist={".csv", ".tim", ".ano", ".cfg" , ".htm", ".HTM", ".rec", ".zpo", ".brf", ".nam",".roi"};
			String[] list = new File(dir).list();
			boolean makename;
				
			if (list==null) return;
			boolean [] open = new boolean [list.length];
		 	
		 	
		 	for (int i=0; i<list.length;i++){
	        	open[i]=false;
	        	makename=true;
	        	name[i]="";
	        	// loop to eliminate frames which are created with the UltraViewERS. the directory is containing 
			 	// one picture per channel which must be skiped.   	
			 	
	        	for (int j=1; j<=Integer.parseInt(peinfo[5]);j++){
					String test="."+String.valueOf(j);
					
					if (list[i].endsWith(test)){
						skip++;
						i++;
						//IJ.log("i="+i+"  skip="+skip+"   "+list[i]);
						name[i]="";
						
						continue;
						
					}
				}
				// eliminate all files with an extension in the skiplist 
	        	for (int j=0; j<skiplist.length; j++){
	        		
	        		if (list[i].endsWith(skiplist[j])){
						skip++;
						//IJ.log("i="+i+"  skip="+skip+"   "+list[i]);
						makename=false;
						j=skiplist.length;
						
					}
				}
	        	// If the extension of teh file is not in the skiplist a name has to be created. The information about the channel and 
	        	// the time is coded in a hexadcimal number. This number has to be converted into an integer and the time 
	        	// and channel information have to be extracted.
	        	
				if (makename){
					int dot=0,under=0,ltest=0;
					String stest=list[i];
					do {
						dot+=list[i].indexOf(".")+1;
						ltest=list[i].substring(dot).indexOf(".");
						stest=list[i].substring(dot);
					} while (ltest>0);
					
					stest=list[i];
				// The do loop is required to get the position of the last underscore in the filename (in case an under-
				// score is used in the filename.
					
					do {
						under+=stest.indexOf("_")+1;
						ltest=list[i].substring(under).indexOf("_");
						stest=list[i].substring(under);
					} while (ltest>0);
					
					if (under>-1 && under+4==dot)
						name[i]="z"+list[i].substring(dot-4,dot-1)+"_";
					else
						name[i]="z000_";
					
					String hilf=list[i].substring(dot,list[i].length());
					name[i]=name[i]+String.valueOf(df.format(toInt(hilf)));
				}
				if (name[i]!=""){
		        	String cut =name[i].substring(name[i].length()-4,name[i].length());
		        	String rest=name[i].substring(0,name[i].length()-5);
		        	int count=Integer.parseInt(cut);
		        	
		        	int time=(count-nChannels-1)/nChannels;
		        	int channel=((count-nChannels-1)%nChannels)+1;
		        	
		        	if (time>=nFrames){
		        		name[i]="";
		        		skip++;
		        		if (tim>0 && cfg==0) nSlices--;
		        		continue;
		        	}
		        	
		        	
		        	
		        	name[i]=rest+"_c"+String.valueOf(channel)+"_t"+String.valueOf(df.format(time));
		        	//IJ.log("name"+i+"   "+name[i]+"   "+list[i]);
		        		
		        	
		        	}
				
			}
		}
			
		boolean readinfo(){
			
			//Find ".cfg" file to obtain necessary variables for the import.
			int length=0;
			list = new File(dir).list();
			name = new File(dir).list();
			for (int i=0; i<list.length; i++) {
				length++;
				
				if (list[i].indexOf(".cfg")>0){
					cfg=i;
					length--;
					savename=list[i].substring(0,list[i].indexOf(".cfg"));
					
				}
				if (list[i].indexOf(".tim")>0){
					tim=i;
					length--;
					savename=list[i].substring(0,list[i].indexOf(".tim"));
					
				}
				if (list[i].indexOf(".HTM")>0){
					htm=i;
					savename=list[i].substring(0,list[i].indexOf(".HTM"));
					length--;
				}
				if (list[i].indexOf(".zpo")>0)length--;
				if (list[i].indexOf(".csv")>0)length--;
				//IJ.log("i="+i+"  length="+length+"  "+list[i]);
			}
			if (cfg==0 && tim==0){ IJ.error("The selected folder does not contain a file with the extension cfg " +
					"\n nor a file with the extension htm");
				return false;
			}
			//extract import variables from the .cfg file
			if (cfg>0){
				if (!readcfg(cfg)) return false;
			}	
			
			if (tim>0 && cfg==0){ 
				if (!readtim(tim, length)) return false;
				peinfo[6]=String.valueOf(length/((Integer.parseInt(peinfo[7]))*Integer.parseInt(peinfo[5])));
			}
			
			
			peinfo[11]=String.valueOf(IJ.d2s(Tools.parseDouble(peinfo[2],1)*Tools.parseDouble(peinfo[8],1)));
	        peinfo[12]=String.valueOf(IJ.d2s(Tools.parseDouble(peinfo[3],1)*Tools.parseDouble(peinfo[9],1)));
	        
	        if (cfg>0) peinfo[13]=String.valueOf(IJ.d2s(Tools.parseDouble(peinfo[6],1)*Tools.parseDouble(peinfo[10],1)));
	        
	        
	        cal.pixelHeight =Tools.parseDouble(peinfo[9],1); 
	        cal.pixelWidth=Tools.parseDouble(peinfo[8],1);
	        cal.setUnit("µm");
	        
			fi.fileType = FileInfo.GRAY16_UNSIGNED;
			fi.fileFormat = fi.RAW;
			fi.directory = dir;
			fi.width = Integer.parseInt(peinfo[2]);
			fi.height = Integer.parseInt(peinfo[3]);
			fi.offset = 6;
			fi.nImages = 1;
			fi.gapBetweenImages = 1;
			fi.intelByteOrder = true;
			fi.whiteIsZero = false;	
			return true;
		}
		boolean readcfg(int num){
			String file=dir+list[num];
			try {	
	           	BufferedReader r = new BufferedReader(new FileReader(file));
	           	while(true){
	           		String path = r.readLine();
	           		if (path==null)break;
	           		int n=path.indexOf("<Name>");
	           		int nn=path.indexOf("</Name>");
	           		int x=path.indexOf("<X>");
	           		int xx=path.indexOf("</X>");
	           		int y=path.indexOf("<Y>");
	           		int yy=path.indexOf("</Y>");
	           		int t=path.indexOf("<T>");
	           		int tt=path.indexOf("</T>");
	           		int c=path.indexOf("<C>");
	           		int cc=path.indexOf("</C>");
	           		int z=path.indexOf("<Z>");
	           		int zz=path.indexOf("</Z>");
	           		int b=path.indexOf("<Binning>");
	           		int bb=path.indexOf("</Binning>");
	           		int xp=path.indexOf("<XPixelSize>");
	           		int xxp=path.indexOf("</XPixelSize>");
	           		int yp=path.indexOf("<YPixelSize>");
	           		int yyp=path.indexOf("</YPixelSize>");
	           		int s=path.indexOf("<Space>");
	           		int ss=path.indexOf("</Space>");
	           		
	           		if (n>0){
	           			peinfo[0]=path.substring(n+6,nn);
	           			IJ.log(peinfo[0]);
	           		}
	           		if (x>0){
	           			xdim = path.substring(x+3,xx);
	           			peinfo[2]=xdim;
	           			IJ.log("Dimension  x: "+xdim+" Pixel");
	           		}
	           		if (y>0){
	           			ydim = path.substring(y+3,yy);
	           			peinfo[3]=ydim;	           			
	           			IJ.log("Dimension  y: "+ydim+" Pixel");
	           		}
	           		if (t>0){
	           			tdim = path.substring(t+3,tt);
	           			int time=Integer.parseInt(tdim);
	           			time--;
	           			tdim=String.valueOf(time);
	           			peinfo[7]=tdim;
	           			IJ.log("Time t : "+tdim+" Frames");
	           		}
	           		if (c>0){
	           			cdim = path.substring(c+3,cc);
	           			peinfo[5]=cdim;
	           			IJ.log("Channel: "+cdim);
	           		}
	           		if (z>0){
	           			zdim = path.substring(z+3,zz);
	           			int trans =Integer.parseInt(zdim);
	           			if (trans==0) {
	           				trans=1;
	           				zdim=String.valueOf(trans);
	           			}
	           			peinfo[6]=zdim;
	           			IJ.log("Slices: "+zdim);
	           		}
	           		if (b>0){
	           			
	           			peinfo[4]=path.substring(b+9,bb);
	           		}
	           		if (xp>0){
	           			peinfo[8]=path.substring(xp+12,xxp);
	           		}
	           		if (yp>0){
	           			peinfo[9]=path.substring(yp+12,yyp);
	           		}
	           		if (s>0){
	           			peinfo[10]=path.substring(s+7,ss);
	           		}
	           		
	           	}
			}   	
	        catch(IOException e) {
	        	IJ.error(""+e);
	            return false;
	        }
	        return true;
		}
		
		boolean readtim(int num, int length){
			int time=0;
			int channels=0;
			String file=dir+list[num];
			peinfo[0]=list[num].substring(0,list[num].indexOf(".tim"));
			IJ.log("Filename: "+peinfo[0]);
			try {
				int line=0;
	           	BufferedReader r = new BufferedReader(new FileReader(file));
	           	while(true){
	           		String path = r.readLine();
	           		if (path==null)break;
	           		line++;
	           		int a=path.indexOf("A");
	                int b=path.indexOf("B");
	                int c=path.indexOf("C");
	                int d=path.indexOf("D");
	                int e=path.indexOf("E");
	                int f=path.indexOf("F");
	                
	                if(a>0 && channels<1)channels++;
	                if(b>0 && channels<2)channels++;
	                if(c>0 && channels<3)channels++;
	                if(d>0 && channels<4)channels++;
	                if(e>0 && channels<5)channels++;
	                if(f>0 && channels<6)channels++;
	                
	           		if (a>0){
	           			time++;
	           		}
	           		if (line==1){
	           			int start=path.indexOf("um");
	           			peinfo[8]=path.substring(start+3,start+11);
	           			IJ.log(peinfo[8]);
	           			
	           			peinfo[9]=path.substring(start+12,start+20);
	           			IJ.log(peinfo[9]);
	           			
	           		}
	            	if (line==2){
	            		peinfo[2]=path.replaceAll(" ", "");
	            		IJ.log("xdim="+peinfo[2]);
	            		
	            	}
	            	if (line==3){
	            		peinfo[3]=path.replaceAll(" ", "");
	            		IJ.log("xdim="+peinfo[3]);
	            		
	            	}
	      
	           	}
			}   	
	        catch(IOException e) {
	        	IJ.error(""+e);
	            return false;
	        }
	        
	        peinfo[5]=String.valueOf(channels);
	        peinfo[6]=String.valueOf(length/(time*channels));
			peinfo[7]=String.valueOf(time);
			IJ.log("Time t : "+time+" Frames");
			IJ.log("Channels: "+channels);
			IJ.log("Z-Slices: "+peinfo[6]);
	        return true;
		
		}
		
		
		ImagePlus open() {
			
				
			ImageStack stack=null;
			ImagePlus imp=null;
			double min = Double.MAX_VALUE;
			double max = -Double.MAX_VALUE;
			for (int i=0; i<list.length; i++) {
				if (list[i].startsWith("."))
					continue;
				if (name[i].equals(""))
					continue;
					
				fi.fileName = list[i];
				imp = new FileOpener(fi).open(false);
				if (imp==null)
						IJ.log(list[i] + ": unable to open");
				else {
					
						if (stack==null)
							stack = imp.createEmptyStack();	
						try {
							ImageProcessor ip = imp.getProcessor();
							if (ip.getMin()<min) min = ip.getMin();
							if (ip.getMax()>max) max = ip.getMax();
							stack.addSlice(name[i], ip);
						}
						catch(OutOfMemoryError e) {
							IJ.outOfMemory("OpenAll");
							stack.trim();
							break;
						}
						IJ.showStatus((stack.getSize()+1) + ": " + list[i]);
					}
				}
				if (stack!=null) {
					imp = new ImagePlus(peinfo[0], stack);
					if (imp.getBitDepth()==16 || imp.getBitDepth()==32)
						imp.getProcessor().setMinAndMax(min, max);
		                Calibration cal = imp.getCalibration();
		                if (fi.fileType==FileInfo.GRAY16_SIGNED) {
		                    double[] coeff = new double[2];
		                    coeff[0] = -32768.0;
		                    coeff[1] = 1.0;
		                    cal.setFunction(Calibration.STRAIGHT_LINE, coeff, "gray value");
		                }
					
				}
			return imp;	
			}
		ImagePlus ZStack(String []list, String[]name, FileInfo fi) {
			
			
			ImageStack stack=null;
			ImagePlus imp=null;
			double min = Double.MAX_VALUE;
			double max = -Double.MAX_VALUE;
			for (int i=0; i<list.length; i++) {
				if (list[i].startsWith("."))
					continue;
				if (name[i].equals(""))
					continue;
				
				fi.fileName = list[i];
				imp = new FileOpener(fi).open(false);
				if (imp==null)
					IJ.log(list[i] + ": unable to open");
				else {
					
					if (stack==null)
						stack = imp.createEmptyStack();	
					try {
						ImageProcessor ip = imp.getProcessor();
						if (ip.getMin()<min) min = ip.getMin();
						if (ip.getMax()>max) max = ip.getMax();
						stack.addSlice(name[i], ip);
					}
					catch(OutOfMemoryError e) {
						IJ.outOfMemory("OpenAll");
						stack.trim();
						break;
					}
					IJ.showStatus((stack.getSize()+1) + ": " + list[i]);
				}
			}
			if (stack!=null) {
				imp = new ImagePlus(peinfo[0], stack);
				if (imp.getBitDepth()==16 || imp.getBitDepth()==32)
					imp.getProcessor().setMinAndMax(min, max);
	                Calibration cal = imp.getCalibration();
	                if (fi.fileType==FileInfo.GRAY16_SIGNED) {
	                    double[] coeff = new double[2];
	                    coeff[0] = -32768.0;
	                    coeff[1] = 1.0;
	                    cal.setFunction(Calibration.STRAIGHT_LINE, coeff, "gray value");
	                }
				
			}
			return imp;
		}
		ImagePlus sublist(int method){
				 		
	 		String []sublist = new String [nSlices];
	 		String []subexten= new String [nSlices];
	 		
			ImageStack resultStack=null;
			int position=0;
	 		for (int i=0; i<nChannels;i++){
	 			for (int j=0;j<nFrames;j++){
	 				for (int k=0; k<nSlices;k++){
	 					position=skip+j+i*nFrames+k*nChannels*nFrames;
	 					sublist[k]=list[position];
	 					subexten[k]=name[position];
	 					
	 				}
	 				
	 				
	 				ImagePlus imp = ZStack(sublist,subexten,fi);
	 				ZProjector zpj = new ZProjector(imp);
	 				zpj.setMethod(method);
	 				zpj.doProjection();
	 				imp=zpj.getProjection();
	 								
	 	
	 				if (resultStack==null){
	 					resultStack = imp.createEmptyStack();
	 				}
	 				if (resultStack!=null){
	 					resultStack.addSlice("c"+df.format(i)+"_t"+df.format(j), imp.getProcessor());
	 				}
	 			}
	 			
	 		}	
	 		ImagePlus imp=new ImagePlus(peinfo[0]+" Z-projection",resultStack);
	 		nSlices=1;
			return imp;

		}
		ImagePlus subvolume(int subchannel, int subslice, int startframe, int endframe){
	 		
	 		String []sublist = new String [endframe-startframe+1];
	 		String []subexten= new String [endframe-startframe+1];
	 		
			
			int position=0;
	 		for (int j=0;j<(endframe-startframe+1);j++){
	 			//position=skip+j+i*nFrames+k*nChannels*nFrames;
	 			position=(startframe-1)+skip+j+(subchannel-1)*nFrames+(subslice-1)*nChannels*nFrames;
	 			sublist[j]=list[position];
	 			subexten[j]=name[position];
	 		}
	 		ImagePlus imp = ZStack(sublist,subexten,fi);
	 		 		
	 		return imp;

	 		
		}


		Integer toInt( String hex )
		 {
		   double ergebnis = 0; //wsr
		   String hexarray[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
		   String hexsmall[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
		   int [] index=new int [hex.length()];
		   for (int i=0; i<hex.length();i++){
			   for (int j=0;j<16;j++){
				   if (hex.substring(i,i+1).equals(hexarray[j]))
					   index[i]=j;
				   else if (hex.substring(i,i+1).equals(hexsmall[j]))
					   index[i]=j;
			   }
			ergebnis+=Math.pow(16,hex.length()-i-1)*index[i];
			
		   }
		   return new Integer((int)ergebnis); //wsr
		 }
		public void save(ImagePlus imp, String dir, String name,String format) {
			
			
			String path = dir+name ;
			
			if (format.equals("Tiff"))
				new FileSaver(imp).saveAsTiffStack(path+".tif");
			
			else if (format.equals("Zip"))
				new FileSaver(imp).saveAsZip(path+".zip");
			else if (format.equals("Raw"))
				new FileSaver(imp).saveAsRaw(path+".raw");
			else if (format.equals("Jpeg"))
				new FileSaver(imp).saveAsJpeg(path+".jpg");
		}
		
		public void saveSingleFile(String dir,String format){
			String path=null;
			ImagePlus imp=null;
			
			for (int i=0; i<list.length; i++) {
				if (list[i].startsWith("."))
					continue;
				if (name[i].equals(""))
					continue;
					
				fi.fileName = list[i];
				imp = new FileOpener(fi).open(false);
				if (imp==null)
					IJ.log(list[i] + ": unable to open");
				else {
					path=dir+name[i];
					//IJ.log(i+"  "+path);
					
					if (format.equals("Tiff"))
						new FileSaver(imp).saveAsTiff(path+".tif");
					else if (format.equals("Zip"))
						new FileSaver(imp).saveAsZip(path+".zip");
					else if (format.equals("Jpeg"))
						new FileSaver(imp).saveAsJpeg(path+".jpg");
					else if (format.equals("Png"))
						new FileSaver(imp).saveAsPng(path+".png");
				
				}
				IJ.showStatus(i + ": " + list[i]);
			}
		}
		
}
