import ij.plugin.frame.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.PlugInFilter;
import ij.text.*;
import ij.measure.*;
import ij.plugin.filter.*;

/**
 * This version requires v1.30m or later: 2003/05/12.
 * This version of QDH plugin is now "macro-able": the DICOM header can easily be queried in a
 * macro, and the result of the query can then be used by the macro.  Because the results are
 * returned to the results table in differing formats depending on whether the returned result
 * is numeric or non-numeric, the Type field in the results table is used to differentiate
 * numeric data (type=1) from non-numeric data (type=2) from missing or invalid data (type=9).
 * Usage is particularly straightforward if the [group,element] query returns a numeric result;
 * this can be accessed by a macro using the getResult function.  If the [group, element] query
 * returns a non-numeric result, this result is appended to the name of the attribute after a
 * colon and placed in the Attribute column.  This can then be accessed from the results table
 * using the getInfo function followed by the split function to divide the contents of the results
 * table into lines, then the lines into fields, and then the appropriate field into two items
 * divided by a colon.
 *
 * @author Anthony Padua
 * @author Neuroradiology
 * @author Duke University Medical Center
 * @author padua001@mc.duke.edu
 *
 * @author Daniel Barboriak, MD
 * @author Neuroradiology
 * @author Duke University Medical Center
 * @author barbo013@mc.duke.edu
 *
 */

public class Query_Dicom_Header implements PlugInFilter {
    private ImagePlus       imp;
    private String          userInput;
    private String          header,value;
    private boolean         bAbort;
    private ResultsTable    rt;
    private String          attribute;
    private double          type,valNum;
    
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        IJ.register(Query_Dicom_Header.class);
        if (imp!=null){Roi roi = imp.getRoi();}
        return DOES_ALL;
    }
    
    public void run(ImageProcessor ip) {
        if(IJ.versionLessThan("1.30m"))                     // check for appropriate version
            return;
        
        bAbort = false;
        attribute = " ";
        type = 1;
        valNum = -99999;
        getInput();
        if (bAbort)
            return;
        int currSlice = imp.getCurrentSlice();
        ImageStack stack = imp.getStack();
        String header = stack.getSize()>1?stack.getSliceLabel(currSlice):(String)imp.getProperty("Info");
        if(header!=null){
            int idx1 = header.indexOf(userInput);
            int idx2 = header.indexOf(":",idx1);
            int idx3 = header.indexOf("\n",idx2);
            if(idx1>=0 && idx2>=0 && idx3>=0){
                try{
                    attribute = header.substring(idx1+9,idx2);
                    attribute = attribute.trim();
                    value = header.substring(idx2+1,idx3);
                    value = value.trim();
                    Double obj = new Double(value);
                    valNum = obj.doubleValue();
                    nextStep(attribute,1,valNum);
                }
                catch (Throwable e) { // Anything else
                    nextStep(attribute+":"+value,2,valNum);
                }
            }
            else{
                attribute = "MISSING";
                nextStep(attribute,9,valNum);
                return;
            }
        }
        else{
            IJ.error("Header is null.");
            return;
        }
    }
    
    void nextStep(String attribute, double type, double valNum){
        int measurements = Analyzer.getMeasurements();
        Analyzer.setMeasurements(measurements);
        Analyzer analyzer = new Analyzer();
        ImageStatistics stat = imp.getStatistics();
        Roi roi = imp.getRoi();
        analyzer.saveResults(stat,roi);
        rt = Analyzer.getResultsTable();
        rt.addLabel("Attribute",attribute);
        rt.addValue("Type",type);
        rt.addValue("ValNum",valNum);
        int counter = rt.getCounter();
        //if(counter==1)
        updateHeadings(rt);
        IJ.write(rt.getRowAsString(counter-1));
    }
    
    public void updateHeadings(ResultsTable rt) { // Wayne Rasband
        TextPanel tp = IJ.getTextPanel();
        if (tp==null)
            return;
        String worksheetHeadings = tp.getColumnHeadings();
        String tableHeadings = rt.getColumnHeadings();
        if (!worksheetHeadings.equals(tableHeadings))
            IJ.setColumnHeadings(tableHeadings);
    } // end of 'updateHeadings' method
    
    void getInput(){
        GenericDialog gd = new GenericDialog("Query DICOM Header", IJ.getInstance());
        gd.addStringField("9 characs [group,element] in format: xxxx,xxxx", "0020,0013", 9);
        gd.showDialog();
        if (gd.wasCanceled()){
            bAbort = true;
            return;
        }
        userInput = gd.getNextString();
        if(userInput.length()!=9){
            IJ.error("Input requirement:\n9 characs [group,element] in format: xxxx,xxxx");
            bAbort = true;
            return;
        }
    }
    
}
