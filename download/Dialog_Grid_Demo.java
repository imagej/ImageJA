import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/** This example creates a GenericDialog containing a 5x4 grid of numeric fields. */
public class Dialog_Grid_Demo implements PlugIn {

    int gridWidth = 5;  
    int gridHeight = 4;
    int gridSize = gridWidth*gridHeight;
    TextField[] tf = new TextField[gridSize];
    double[] value = new double[gridSize];

    public void run(String arg) {
        if (IJ.versionLessThan("1.31l"))
            return;
        if (showDialog())
            displayValues();
    }

    boolean showDialog() {
        GenericDialog gd = new GenericDialog("Grid Example");
        gd.addPanel(makePanel(gd));
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        getValues();
        return true;
    }

    Panel makePanel(GenericDialog gd) {
        Panel panel = new Panel();
            panel.setLayout(new GridLayout(gridHeight,gridWidth));
        for (int i=0; i<gridSize; i++) {
            tf[i] = new TextField(""+value[i]);
            panel.add(tf[i]);
        }
        return panel;
    }

    void getValues() {
        for (int i=0; i<gridSize; i++) {
            String s = tf[i].getText();
            value[i] = getValue(s);
        }           
    }

    void displayValues() {
        for (int i=0; i<gridSize; i++) 
            IJ.log(i+"  "+value[i]);            
    }

    double getValue(String theText) {
        Double d;
        try {d = new Double(theText);}
        catch (NumberFormatException e){
            d = null;
        }
        return d==null?Double.NaN:d.doubleValue();
    }
}

