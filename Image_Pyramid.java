import java.awt.AWTEvent;
import java.awt.Rectangle;
import java.io.File;
import java.util.Properties;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.Convolver;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/*
* @version 1.0	24 Nov 2012
*   
* 
* @author Dimiter Prodanov
* 		  IMEC
*
*
* @contents
* This pluign computes image pyramids
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
public class Image_Pyramid  implements PlugIn {

	private String version="1.0";
	static int order=3;
	public static boolean debug=IJ.debugMode;
	private FloatProcessor[] pyramid;
	
	@Override
	public void run(String arg) {		
		if (IJ.versionLessThan("1.47"))
			return;
		ImagePlus imp = IJ.getImage();
		if (imp.getBitDepth()==24) {
			IJ.error("This plugin does not work with RGB images.");
			return;
		}
		ImageProcessor ip = imp.getProcessor();
		order=maxorder(ip);
		if (!showDialog())
			return;
		run(ip);
	}

	public ImagePlus getResult() {
		return outimg;
	}
	
	public FloatProcessor getLevel(int i) {
		return pyramid[i];
	}

	private ImagePlus outimg=null;
	public static boolean showimg=true;

	public int maxorder(ImageProcessor ip) {
		final int width=ip.getWidth();
		final int height=ip.getHeight();		
		int wh=Math.min(width, height)>>1;
		int c=1;
		int cnt=0;
		while (c<wh ) { 		
			c = c<<1;
			cnt++;
		}
		return cnt;
	}
	
	public void run(ImageProcessor ip) {
		FloatProcessor integral=imageIntegral(ip);
		int c=1;
		final int width=ip.getWidth();
		final int height=ip.getHeight();
		
		int cwidth=width;
		int cheight=height;
		
		int wh=Math.min(width, height)>>1;
		Rectangle rect=new Rectangle (0,0,1,1);
		int cnt=1;
		
		int[][] offsets=new int[2][order+1];		
	
		while (c<wh && cnt <= order) {			
			c = c<<1;
			rect.width=c;
			rect.height=c;
			cwidth=cwidth>>1;
			cheight=cheight>>1;
		
			offsets[0][cnt]=cwidth;
			offsets[1][cnt]=cheight;
			
			if (debug)
				IJ.log(c+" width  "+cwidth + " height "+cheight);
			
			FloatProcessor fp=new FloatProcessor(cwidth,cheight);
			int rx=0;
			for (int row=0; row<=width-c; row+=c ) {
				int ry=0;
				for (int col=0; col<=height-c; col+=c ) {
					try {
						rect.x=row;
						rect.y=col;
						final float value=avg(integral,  rect);
						fp.setf(rx, ry, value);
					} catch (Exception e) {
						IJ.log(rect +" ic: "+ ry+ " ri: "+rx+" level: "+c);
						e.printStackTrace();
					}
					ry++;
					
				} // end for
				rx++;
			} // end for
			
			pyramid[cnt-1]=fp;
			
			//new ImagePlus("level "+cnt, fp).show();
			cnt++;
		}
		
		cnt--;
		FloatProcessor flat=new FloatProcessor(width,height);
		int xloc=0;
		int yloc=0;
		for (int i=0; i<order; i++) {		
			xloc+=offsets[0][i];
			yloc+=offsets[1][i];
			flat.insert(pyramid[i],xloc,yloc );
	
		}
		outimg=new ImagePlus("pyramid "+cnt, flat);
		if (showimg)
			outimg.show();
	 
	}
	
	public float avg(FloatProcessor integral, Rectangle rect) {
		float sum=  integral.getf(rect.x		   , rect.y) 
				  + integral.getf(rect.x+rect.width, rect.y+rect.height) 
				  - integral.getf(rect.x+rect.width, rect.y )
				  - integral.getf(rect.x		   , rect.y+rect.height);
		return sum/((float)(rect.width*rect.height));
		
	}
	

	/**
	 * @param ip
	 * @return
	 */
	private FloatProcessor imageIntegral(ImageProcessor ip) {
		final int width=ip.getWidth();
		final int height=ip.getHeight();

		FloatProcessor sum=new FloatProcessor(width+1, height+1);
		
		for (int c=1; c<height+1; c++) {
			for (int r=1; r<width+1; r++) {
				float value=  ip.getf(r-1, c-1)
							+ sum.getf(r-1, c) 
							+ sum.getf(r,c-1)
							- sum.getf(r-1, c-1);
				sum.setf(r, c, value);
			}
		}
		 
		return sum;
	}

	/*private FloatProcessor imageIntegral(ImageProcessor ip, float scale ) {
		final int width=ip.getWidth();
		final int height=ip.getHeight();
		
		FloatProcessor sum=new FloatProcessor(width, height);
		int cnt=1;
		for (int p=0;p <width*height; p++) {
			float value=ip.get(p)*scale;
			if (value>0)
				sum.setf(p,cnt++ );
		}
		for (int c=0; c<height; c++) {
			for (int r=0; r<width; r++) {
				float value=sum.getf(r, c) ;
				if (value>0){
					float[] quad=quadrantL(sum,r,c,1,1.0f);
					float m=max(quad);				
					m=Math.min(m, value)+1;				
					sum.setf(r, c, m);
				}
			}
		}
		 
		return sum;
	}*/
	
	/*	half neighborhood
	 *  x x
	 *  x
	 */
/*	private float[] quadrantL(ImageProcessor ip, int x, int y, int r, float scale) {
		int v=0;
		int h=(r+1)*(r+1)-1;
		float[] ret =new float[h];
		int width=ip.getWidth();
		int height=ip.getHeight();
		for (int k=-r;k<=0; k++) {
			for (int p=-r;p<=0; p++) {
				if ((x+k>=0 && x+k <width && y+p >= 0 && y+p <height))
				if (p*k!=0)
					ret[v++]= ip.getf(x+k,y+p)* scale;
			}
		}
		return ret;
	}
	
	private float max(float[] arr) {
		float ret=arr[0];
		for (int i=1; i<arr.length; i++) {
			ret=Math.max(arr[i], ret);
		}
		return ret;
	}*/
	
	public boolean showDialog() {
		GenericDialog gd=new GenericDialog("Pyramid "+version);
		gd.addNumericField("order", order, 0);
		gd.addCheckbox("show image", showimg);
		gd.addCheckbox("debug", debug);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		order= (int)(gd.getNextNumber());
		showimg = gd.getNextBoolean();	
 		debug = gd.getNextBoolean();	
 		pyramid=new FloatProcessor[order];
		return true;
	}
	
	
}
