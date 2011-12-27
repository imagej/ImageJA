import java.awt.Component;
import java.awt.Frame;
import java.awt.TextArea;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import ij.IJ;
import ij.ImageJ;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.text.TextPanel;

/**
 * @version  	1.1.0 26 Dec 2011
 * 				- support of TextArea component
 * 				- standard main
 * 				- bug fix
 * 				1.0.0 16 Dec 2011
 * 				- results are written into the system ResultTable
 *  
 *            
 * @author Dimiter Prodanov
 * 		   IMEC
 *
 *
 * @contents This plugin imports the values of a text window in the ResultsTable. 
 * Useful for example for histograms or line profiles. 
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


public class Results_Importer implements PlugIn {

	public static boolean debug=IJ.debugMode;
	
	private  boolean found=false;
	private ArrayList< String> cm =new ArrayList<String>(); 
	private	ConcurrentHashMap<String,TextPanel> cm2 =new ConcurrentHashMap<String,TextPanel>(); 
	private	ConcurrentHashMap<String,TextArea> cm3 =new ConcurrentHashMap<String,TextArea>();
		
	public void run(String arg) {
	 	
		Frame[] frms=WindowManager.getNonImageWindows();
	
		
		for (Frame w:frms) {
	
			String name=w.getTitle(); 
			
 			for (Component c:w.getComponents()) {
			
 				if (debug)	
 					System.out.println(c.toString());
				 
				
				if (c instanceof TextPanel) {
					cm.add(name);
					cm2.put(name, (TextPanel)c);
				
					found=true;
					if (debug)
						System.out.println(name);
				}
				
				if (c instanceof TextArea) {
					
					cm.add( name);
		
					cm3.put(name, (TextArea)c);
		
					found=true;
					if (debug)
						System.out.println(name);
				}
			}
		}
		
		if (!found)
			return;
		
		GenericDialog gd=new GenericDialog("Import");
		 
		
		final String[] items=cm.toArray(new String[0]);
		
		gd.addChoice("windows", items, items[0]);
		
		gd.showDialog();
		
		if (gd.wasCanceled()) 
			return;
 
		final String selname=gd.getNextChoice();
		
		final TextPanel tp=cm2.get(selname);
		if (tp!=null) {
			String txt=tp.getText();
			//System.out.println(txt);
		
			parseText(txt);

			return;
		}
		final TextArea ta=cm3.get(selname);
		if (ta!=null) {
			String txt=ta.getText();
			//System.out.println(txt);
		
			parseText(txt);
	
			return;
		}
		
	}
	
	/**
	 * @param text
	 */
	private void parseText(String txt) {
		StringTokenizer st=new StringTokenizer(txt,"\n");
		String title0=st.nextToken();
		ResultsTable rt=Analyzer.getResultsTable();
		rt.reset();
		
		String[] colhead=title0.split("\t");
		IJ.setColumnHeadings(title0);
		while (st.hasMoreElements()) {
			rt.incrementCounter();
			double[] arr=parseArray(st.nextToken());
			for(int i=0; i<arr.length; i++)
				rt.addValue(colhead[i], arr[i]);
			
			
		}
		rt.show("Results");
	}

	/**
	 * @param arg
	 */
	public static double parseDouble(String arg) {
    	double ret =Double.NaN;
    	try {
			ret=Double.parseDouble(arg);
		} catch (NumberFormatException e) {
			System.err.println("error parsing " +arg);
		}
    	return ret;
    }
	
	 
	/**
	 * @param text
	 */
	private double[] parseArray(String text) {
		Vector<Double> v=new Vector<Double>();
		String[] tokens=text.split("[,;]\\s|\\s");
		for (int i=0; i<tokens.length; i++) {
			if  (debug)
				System.out.println(tokens[i]);

			double db=parseDouble(tokens[i]);
			//IJ.log("db "+db);
			if (!Double.isNaN(db)) {
				v.addElement(db);
				//IJ.log("db2 "+db);
			}

		}
		int sz=v.size();
		double[] ret=new double[sz];
		int i=0;
		for (Enumeration<Double> e=v.elements(); e.hasMoreElements(); ) {
			Double db=((Double)e.nextElement());
			ret[i]=db.doubleValue();
			i++;
		}

		return ret;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
    		System.setProperty("plugins.dir", args[0]);
    		new ImageJ();
    	
    	}
    	catch (Exception ex) {
    		IJ.log("plugins.dir misspecified");
    	}


	}

}
