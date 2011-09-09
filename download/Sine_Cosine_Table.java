import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

/**
This plugin adds a sine/cosine table to the ImageJ results table
and displays it in the Results window. It is equivalent to this macro:
<pre>
        run("Clear Results");
        row = 0;
        for (n=0; n<=2*PI; n += 0.1) {
            setResult("n", row, n);
            setResult("Sine(n)", row, sin(n));
            setResult("Cos(n)", row, cos(n));
            row++;
        }
        setOption("ShowRowNumbers", false);
        updateResults()
</pre>
Plugins can also display tables in a TextWindow:
<pre>
    String title = "Sine/Cosine Table";
    String headings = "n\tSine(n)\tCos(n)";
    TextWindow tw = new TextWindow(title, headings, "", 400, 500);
    for (double n=0; n<=2*Math.PI; n += 0.1)
       tw.append(IJ.d2s(n,2)+"\t"+IJ.d2s(Math.sin(n),4)+"\t"+IJ.d2s(Math.cos(n),4));
</pre>
*/
public class Sine_Cosine_Table implements PlugIn {

	public void run(String arg) {
		if (IJ.versionLessThan("1.45o"))
			return;
		ResultsTable rt = new ResultsTable();
		for (double n=0; n<=2*Math.PI; n += 0.1) {
			rt.incrementCounter();
			rt.addValue("n", n);
			rt.addValue("Sine(n)", Math.sin(n));
			rt.addValue("Cos(n)", Math.cos(n));
		}
		rt.showRowNumbers(false);
		rt.show("Results");
		//rt.show("Sine/Cosine Table");
	}

}
