import java.awt.AWTEvent;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/*
* @version 		2.0	19 March 2013
* 					vectorization
* 					migration to PluginFilter 
* 					support of 8 connected neighborhood 
* 				1.0	24 Nov 2012
*   				support of 4 connected neighborhood 
* 
* @author Dimiter Prodanov
* 		  IMEC
*
*
* @contents
* This pluign computes labels blobs. The implementation is based on
*  Neil Brown and  Judy Robertson 
*  http://homepages.inf.ed.ac.uk/rbf/HIPR2/labeldemo.htm
* 
* 
* @license This library is free software; you can redistribute it and/or
*      modify it under the terms of the GNU Lesser General Public
*      License as published by the Free Software Foundation; either
*      version 2.1 of the License, or (at your option) any later version.
*
*      This library is distributed in the hope that it will be useful,
*      but WITHOUT ANY WARRANTY; without even the implied warranty of
*      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
*       Lesser General Public License for more details.
*
*      You should have received a copy of the GNU Lesser General Public
*      License along with this library; if not, write to the Free Software
*      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
public class Blob_Labeler  implements PlugInFilter {


	final int flags=DOES_8G+ DOES_8C+NO_CHANGES+NO_UNDO + DOES_STACKS +PARALLELIZE_STACKS;
	private String version="2.0";
   
	private static int bgcol=0;
	public static boolean debug=IJ.debugMode;
	private IndexColorModel lut=null;
	
	private static boolean segment = true;
	
	public ImagePlus getResult() {
		return outimg;
	}
	
	private static RoiManager roiman=RoiManager.getInstance();

	private ImagePlus outimg=null;
	
	private static boolean edge=true;
	
	private static boolean neighb8=false;
	
	 /*
	  * @param args - args[0] should point to the folder where the plugins are installed 
	  */
	public static void main(String[] args) {
			
			try {
	    		
	    		File f=new File(args[0]);
	    		
	    		if (f.exists() && f.isDirectory() ) {
	    			System.setProperty("plugins.dir", args[0]);
	    			new ImageJ();
	    		} else {
	    			throw new IllegalArgumentException();
	    		}
	    	}
	    	catch (Exception ex) {
	    		IJ.log("plugins.dir misspecified\n");
	    		ex.printStackTrace();
	    	}
		
	}

	@Override
	public int setup(String arg, ImagePlus imp) {		
		if (imp==null) return DONE;
		if (arg.equals("about")){
            showAbout();
            return DONE;
        }
		if(IJ.versionLessThan("1.47")|| !showDialog(imp)) {
            return DONE;
        }
        else {
        	lut = makeLut(bgcol);       	
            return IJ.setupDialog(imp, flags);
        }
	}


	
	private void showAbout() {
		IJ.showMessage("Blob Labeler "+version,
		        "The labels blobs in 2D images"
		        );
		
	}

	@Override
	public void run(ImageProcessor ip) {
		 
		final int width=ip.getWidth();
		final int height=ip.getHeight();
		final int size=width*height;

		ShortProcessor map=new ShortProcessor (width, height);

		
		int numberOfLabels=0;
		String title="blobs 8c";
		long time=-System.nanoTime();
		
		if (neighb8) 
			numberOfLabels=doLabels8(ip, map, bgcol,edge);
		else {
			numberOfLabels=doLabels4(ip, map, bgcol,edge);
			title="blobs 4c";
		}
		time+=System.nanoTime();;
		
		System.out.println("labeling run time " + (time/1000) + " us");
		
		if (numberOfLabels==0) {
			return;
		}
		
		map.setColorModel(lut);

		outimg=new ImagePlus(title+" "+numberOfLabels,map);
		
		
		/*
		 *  segmenting the polygons
		 */
		
		outimg.show();
		
		if(segment ) {
			
			Wand wand= new  Wand(map);
			final Overlay overlay= new Overlay();
			
			if (roiman==null) {
				System.out.println("new instance of RoiManager started");
				roiman=new RoiManager();
			}
		
			if (!roiman.isVisible())
				roiman.setVisible(true);
			
			time=-System.nanoTime();
			HashMap<Integer,int[]> vcoords=new  HashMap<Integer,int[]>(100);

			for (int i=0; i< size; i++) {
				if (map.get(i)>0) {
						final int x=i%width; 
						final int y=i/width; 
						vcoords.put(i,new int[]{x,y});		
				}
			}

			for (int k=1; k<=numberOfLabels; k++) {
				Polygon p= getContour3(map,k, vcoords, wand);
				if (p!=null) {
					PolygonRoi roi=new PolygonRoi(p, Roi.POLYGON);
					try {
						roiman.addRoi(roi);
						overlay.add(roi);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			time+=System.nanoTime();;
			
			System.out.println("vectorization run time " + (time/1000) + " us");
			outimg.setOverlay(overlay);
			
		}

		
	 
	}
	
	private Polygon getContour3(ShortProcessor map,int label, 
			HashMap<Integer,int[]> coords, Wand wand) {
		final int width=map.getWidth();
		final int height=map.getHeight();
		int sz=width*height;
		LinkedHashMap<Integer,ArrayList<int[]>> cmap= new LinkedHashMap<Integer,ArrayList<int[]>> (100);
	
		//System.out.print("<< " +label+" \n");
		
		coords.entrySet();
		for (int i=0; i<sz; i++ ) {
			int key=map.get(i); 
			if (key==label) {
				distribute(i,   width,  cmap);
					
			} // end if
			
		} // end for
		//System.out.print("\n "+cnt+" >> \n");
		Set<Entry<Integer, ArrayList<int[]>>> entries=cmap.entrySet();
		//Iterator<Entry<Integer, ArrayList<int[]>>> iter=entries.iterator();
		int key=0;
		boolean first=false;
		for (Entry<Integer, ArrayList<int[]>> e:entries) {
			//ArrayList<int[]> value=e.getValue();
			//System.out.print(e.getKey() + ">>");
			if (!first) {
				key=e.getKey();
				break;
			}
			first=true;
			
		/*	for (int[] k: value) {
				System.out.print("("+k[0]+" "+ k[1]+"),");
			}
			System.out.print(" <<\n");
			*/
		}
		
		 		
		try {
			
			Polygon poly=new Polygon();
			ArrayList<int[]> aux=cmap.get(key);
			if (aux!=null) {
				int[] c=aux.get(0);
				int startX=c[0];
				int startY=c[1];
				//System.out.println(key +" -> ("+startX+" "+ startY+")");
				wand.autoOutline(startX, startY, label, label+1);		
				poly.xpoints=wand.xpoints;
				poly.ypoints=wand.ypoints;
				poly.npoints=wand.npoints;
				return poly;
			}  
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	final int maxd=2; // maximal metrical radius of a "circle"
	private void distribute(int idx, int width,  LinkedHashMap<Integer,ArrayList<int[]>> cmap) {
		int[] c=new int[2];		
		c[0]=idx % width;
		c[1]=idx / width;
		
		for (Entry<Integer, ArrayList<int[]>> e:cmap.entrySet()) {
			ArrayList<int[]> clist=e.getValue();
			final int lastind=clist.size()-1;
			final int[] ctail=clist.get(lastind);
			final int[] chead=clist.get(0);
			int dist= dist (ctail,c); 
			if (dist<maxd ) {
				clist.add(c);
				return;
			}
			dist= dist (chead,c); 
			if (dist<maxd ) {
				clist.add(0,c);
				return;
			}
			
		}
		ArrayList<int[]> alist=new ArrayList<int[]>(100);
		alist.add(c);
		cmap.put(idx, alist);
	}
	
	private int dist(int[] u, int[] v) {
		final int d= Math.max(Math.abs(u[0]-v[0]) , Math.abs(u[1]-v[1]));
		return d;
	}

	/**
	 * @return
	 */
	private IndexColorModel makeLut(int ubgcol) {
	    boolean acceptColor = false;
	    byte [] reds=new byte[256];
	    byte [] greens=new byte[256];
	    byte [] blues=new byte[256];
	    
	    int cnt=0;	    
	    ubgcol= ubgcol & 0x000000FF;
	    int bgcol= (ubgcol& 0x000000ff) | (ubgcol  >> 8) | (ubgcol  >> 16);
	    
	    //System.out.println( "background " +Integer .toHexString(bgcol) +" ( "+ bgcol+ " )");
	    
	    while (cnt <256) {
	    	final int col = (int)(Math. random() * 16777216);
	    	acceptColor = !((col& 0x000000ff) < 64) &&
	    			(((col & 0x0000ff00) >> 8) < 64) &&
	    			(((col & 0x00ff0000) >> 16) < 200) ;
	    	if(acceptColor && col!= bgcol)	{	    			
	    		blues[cnt]= (byte) (col& 0x0000FF);
	    		greens[cnt]= (byte) ((col & 0x00FF00) >>8);
	    		reds[cnt]= (byte) ((col & 0xFF0000)>>16);	    
	    		cnt ++;
	    	}    
	    	blues[0]= (byte) ubgcol;
	    	greens[0]= (byte) ubgcol;
	    	reds[0]= (byte) ubgcol;
	    }
	    IndexColorModel alut=new IndexColorModel(8, 256,reds, greens, blues);
	    return alut;
	}

	/** Code based on
	 * http://homepages.inf.ed.ac.uk/rbf/HIPR2/labeldemo.htm
	 */
	private int doLabels4(ImageProcessor bp, ShortProcessor map, int bgcol, boolean edgecorr) {
		
		if (bp instanceof ColorProcessor)
			return -1;
			
		final int width=bp.getWidth();
		final int height=bp.getHeight();
		final int size=width*height;
		
		final int mwidth=map.getWidth();
		final int mheight=map.getHeight();
		
		if (width!=mwidth || height!= mheight)
			throw new IllegalArgumentException ("dimensions mismatch ");
			
		if (edgecorr) {
			for (int a=0;a<mwidth; a++) {
				bp.set(a, 0, bgcol);
			}

			for (int a=0;a<mheight; a++) {
				bp.set(0, a, bgcol);
			}
		}
		
		int [] labels  = new int[size/2];
		
		for (int i=0; i<labels.length; i++) {
			labels[i]=i ; // ramp
		}
		

		int[] nbs= new int[2];
		int[] nbslab= new int[2];
		
		int numberOfLabels =1;
		int labelColour=1; // background
		
		int result=0;
		
		for(int y=0; y<height; y++) {	 
			//labelColour=0;
			for(int x=0; x<width; x++){		
				final int val=bp.get(x, y);				
			      if( val == bgcol ){
			    	  result = 0;  //nothing here
			      } else {

			    	  //The 4 connected visited neighbours
			    	  neighborhood4(bp, nbs, x, y, width);
			    	  neighborhood4(map, nbslab, x, y, width);

			    	  //label the point
			    	 // if( (nbs[0] == nbs[1]) && (nbs[1] == nbs[2]) && (nbs[2] == nbs[3])&& (nbs[0] == bgcol )) { 
			    	  if( (nbs[0] == nbs[1])   && (nbs[0] == bgcol )) { 
					    	
			    	  // all neighbours are 0 so gives this point a new label
			    		  result = labelColour;
			    		  labelColour++;
			    	  } else { //one or more neighbours have already got labels

			    		  int count = 0;
			    		  int found = -1;
			    		  for( int j=0; j<nbs.length; j++){
			    			  if( nbs[ j ] != bgcol ){
			    				  count +=1;
			    				  found = j;
			    			  }
			    		  }
			    		  if( count == 1 ) {
			    			  // only one neighbour has a label, so assign the same label to this.
			    			  result = nbslab[ found ];
			    		  } else {
			    			  // more than 1 neighbour has a label
			    			  result = nbslab[ found ];
			    			  // Equivalence the connected points
			    			  for( int j=0; j<nbslab.length; j++){
			    				  if( ( nbslab[ j ] != 0 ) && (nbslab[ j ] != result ) ){
			    					  associate(labels, nbslab[ j ], result );
			    				  } // end if
			    			  } // end for
			    		  } // end else
			    		  
			    	  } // end else
			    	  map.set(x, y, result);
			      } // end if			    
			} // end for
		} // end for
		//reduce labels ie 76=23=22=3 -> 76=3
		//done in reverse order to preserve sorting
		System.out.println(" labels " + labelColour);
		for( int i= labels.length -1; i > 0; i-- ){
			labels[ i ] = reduce(labels, i );
		}

		/*now labels will look something like 1=1 2=2 3=2 4=2 5=5.. 76=5 77=5
			      this needs to be condensed down again, so that there is no wasted
			      space eg in the above, the labels 3 and 4 are not used instead it jumps
			      to 5.
		 */
		if (labelColour>0) {
		int condensed[] = new int[ labelColour ]; // can't be more than nextlabel labels
		
		int count = 0;
		for (int i=0; i< condensed.length; i++){
			if( i == labels[ i ] ) 
				condensed[ i ] = count++;
		}
		
		/*for( int i= condensed.length -1; i > 0; i-- ){
			System.out.println(" l " + i+ " "+condensed[ i ]);
		}*/
		numberOfLabels = count -1;
		 
		// now run back through our preliminary results, replacing the raw label
		// with the reduced and condensed one, and do the scaling and offsets too
	    for (int i=0; i< size; i++){
	    	int val=map.get(i);
	    	val = condensed[ labels[ val ] ];	        
	    	map.set(i, val);
	    }
			return numberOfLabels;
		} else {
			return -1;
		}
	}

	/** Code based on
	 * http://homepages.inf.ed.ac.uk/rbf/HIPR2/labeldemo.htm
	 */
	private int doLabels8(ImageProcessor bp, ShortProcessor map, int bgcol, boolean edgecorr) {
		
		if (bp instanceof ColorProcessor)
			return -1;
			
		final int width=bp.getWidth();
		final int height=bp.getHeight();
		final int size=width*height;
		
		final int mwidth=map.getWidth();
		final int mheight=map.getHeight();
		
		if (width!=mwidth || height!= mheight)
			throw new IllegalArgumentException ("dimensions mismatch ");
			
		if (edgecorr) {
			for (int a=0;a<mwidth; a++) {
				bp.set(a, 0, bgcol);
			}

			for (int a=0;a<mheight; a++) {
				bp.set(0, a, bgcol);
			}
		}
		
		int [] labels  = new int[size/2];
		
		for (int i=0; i<labels.length; i++) {
			labels[i]=i ; // ramp
		}
		

		int[] nbs= new int[4];
		int[] nbslab= new int[4];
		
		int numberOfLabels =1;
		int labelColour=1; // background
		
		int result=labelColour;
		
		for(int y=0; y<height; y++) {	 
			//labelColour=0;
			for(int x=0; x<width; x++){		
				final int val=bp.get(x, y);				
			      if( val == bgcol ){
			    	  result = 0;  //nothing here
			      } else {

			    	  //The 8-connected visited neighbours
			    	  neighborhood8(bp, nbs, x, y, width);
			    	  neighborhood8(map, nbslab, x, y, width);
			    	 
			    	  //label the point
			    	  if( (nbs[0] == nbs[1]) && (nbs[1] == nbs[2])  && (nbs[2] == nbs[3]) && (nbs[0] == bgcol )) { 
			    		  // all neighbours are 0 so gives this point a new label
			    		  result = labelColour;
			    		  labelColour++;
			    	  } else { //one or more neighbours have already got labels

			    		  int count = 0;
			    		  int found = -1;
			    		  for( int j=0; j<nbs.length; j++){
			    			  if( nbs[ j ] != bgcol ){
			    				  count +=1;
			    				  found = j;
			    			  }
			    		  }
			    		  if( count == 1 ) {
			    			  // only one neighbour has a label, so assign the same label to this.
			    			  result = nbslab[ found ];
			    		  } else {
			    			  // more than 1 neighbour has a label
			    			  result = nbslab[ found ];
			    			  // Equivalence of the connected points
			    			  for( int j=0; j<nbslab.length; j++){
			    				  if( ( nbslab[ j ] != 0 ) && ( nbslab[ j ] != 0 )&& (nbslab[ j ] != result ) ){
			    					  associate(labels, nbslab[ j ], result );
			    				  } // end if
			    			  } // end for
			    		  } // end else
			    		  
			    	  } // end else
			    	  map.set(x, y, result);
			      } // end if			    
			} // end for
		} // end for
		//reduce labels ie 76=23=22=3 -> 76=3
		//done in reverse order to preserve sorting
		for( int i= labels.length -1; i > 0; i-- ){
			labels[ i ] = reduce(labels, i );
		}

		/*now labels will look something like 1=1 2=2 3=2 4=2 5=5.. 76=5 77=5
			      this needs to be condensed down again, so that there is no wasted
			      space eg in the above, the labels 3 and 4 are not used instead it jumps
			      to 5.
		 */
		if (labelColour>0) {
		int condensed[] = new int[ labelColour ]; // can't be more than nextlabel labels
		
		int count = 0;
		for (int i=0; i< labelColour; i++){
			if( i == labels[ i ] ) 
				condensed[ i ] = count++;
		}
		numberOfLabels = count-1;
		 
		// now run back through our preliminary results, replacing the raw label
		// with the reduced and condensed one, and do the scaling and offsets too
	    for (int i=0; i< size; i++){
	    	int val=map.get(i);
	    	val = condensed[ labels[ val ] ];	    
	    	//val =  labels[ val ] ;	
	    	map.set(i, val);
	 
	    }
			return numberOfLabels;
		} else {
			return -1;
		}
	}
	
	/**
	 * @param bp
	 * @param nbs
	 * @param x
	 * @param y
	 */
	private void neighborhood4(ImageProcessor bp, int[] nbs, int x, int y, int width) {
		if ( x <= 0 ) x=1;
		if ( x >= width ) x=width-1;
		if ( y <= 0 ) y=1;
		nbs[0]=bp.get(x-1,y); // west
		nbs[1]=bp.get(x,y-1); // south
	}

	/**
	 * @param bp
	 * @param nbs
	 * @param x
	 * @param y
	 */
	private void neighborhood8(ImageProcessor bp, int[] nbs, int x, int y, int width) {
		if ( x <= 0 ) x=1;
		if ( x >= width ) x=width-1;
		if ( y <= 0 ) y=1;
		nbs[0]=bp.get(x-1,y); // W
		nbs[1]=bp.get(x,y-1); // N
		nbs[2]=bp.get(x-1,y-1); // W
		nbs[3]=bp.get(x+1,y-1); // NW

	}
	
	/**
	 * @param bp
	 * @param nbs
	 * @param width
	 * @param i
	 */
	public void neighborhood4i(ImageProcessor bp, int[] nbs, int width, int i) {
		int x= i % width;
		int y =i / width;
		if ( x >= width ) x=width-1;
		if ( x <= 0 ) x=1;
		if ( y <= 0 ) y=1;
		nbs[0]=bp.get(x-1,y); // W
		nbs[1]=bp.get(x,y-1); // N
	}
	
	/**
	 * @param bp
	 * @param nbs
	 * @param width
	 * @param i
	 */
	public void neighborhood8i(ImageProcessor bp, int[] nbs, int width, int i) {
		int x= i % width;
		int y =i / width;
		if ( x <= 0 ) x=1;
		if ( y <= 0 ) y=1;
		if ( x >= width ) x=width-1;
		nbs[0]=bp.get(x-1,y); // W
		nbs[1]=bp.get(x,y-1); // N
		nbs[2]=bp.get(x-1,y-1); // NW
		nbs[3]=bp.get(x+1,y-1); // NE
	}

	 /**
	   * Associate(equivalence) a with b.
	   *  a should be less than b to give some ordering (sorting)
	   * if b is already associated with some other value, then propagate
	   * down the list.
	    */
	  private void associate(int[] labels, int a, int b ) {	    
	    if( a > b ) {
	      associate(labels, b, a );
	      return;
	    }
	    if( ( a == b ) || ( labels[ b ] == a ) ) return;
	    if( labels[ b ] == b ) {
	      labels[ b ] = a;
	    } else {
	      associate(labels, labels[ b ], a );
	      if (labels[ b ] > a) {             //***rbf new
	        labels[ b ] = a;
	      }
	    }
	  }
	  
	  /**
	   * Reduces the number of labels.
	   */
	  private int reduce(int[] labels, int a ){
	    
	    if (labels[a] == a ){
	      return a;
	    } else {
	      return reduce(labels, labels[a] );
	    }
	  }
	 
	
	 
	public boolean showDialog(ImagePlus imp) {
	
		GenericDialog gd=new GenericDialog("Labeler "+version);
		gd.addNumericField("color", bgcol, 0);
		gd.addCheckbox("8-connected", neighb8);
		gd.addCheckbox("edge", edge);
		gd.addCheckbox("vectorize", segment);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		
		bgcol= (int)(gd.getNextNumber());
		neighb8 = gd.getNextBoolean();	
		edge = gd.getNextBoolean();	
		segment = gd.getNextBoolean();	
		
		return true;
	}

	 
	
}
