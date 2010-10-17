import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.util.Tools;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

public class Dotted_Line implements PlugIn {
    static double width = 5;
    static String dashpattern = "12, 12";
    float[] dashvalues;

    public void run(String arg) {
       ImagePlus imp = WindowManager.getCurrentImage();
       if (imp==null) {
          imp = IJ.createImage("Untitled", "8-bit Ramp", 512, 512, 1);
          imp.show();
          imp.setRoi(new OvalRoi(63, 64, 364, 363));
       }
       if (imp.getRoi()==null) {
          IJ.error("Selection required");
          return;
       }
       if (showDialog()) {
          Undo.setup(Undo.TYPE_CONVERSION, imp);
          drawLine(imp, width, dashvalues);
       }
    }

    public boolean showDialog() {
       GenericDialog gd = new GenericDialog("Dotted Line");
       gd.addNumericField("Line Width", width, 2);
       gd.addStringField("Dash Pattern", dashpattern);
       gd.showDialog();
       if (gd.wasCanceled())
          return false;
       width = gd.getNextNumber();
       dashpattern = gd.getNextString();
       String[] svalues = Tools.split(dashpattern, ",- \t");
       dashvalues = new float[svalues.length];
       for (int i=0; i<dashvalues.length; i++)
           dashvalues[i] = (float)Tools.parseDouble(svalues[i], 12);
       return true;
    }

    //Draws various dashed patterns with different roi type selections 
    public void drawLine(ImagePlus imp, double width, float[] dashvalues) {
       ImageProcessor ip = imp.getProcessor();
       ip = ip.convertToRGB();
       BufferedImage bi = ip.getBufferedImage();
       Graphics2D g2 = (Graphics2D)bi.getGraphics();
       Roi roi = imp.getRoi();
        if (roi==null) return;
       Color c = Toolbar.getForegroundColor();
       Stroke stroke = new BasicStroke((float)width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashvalues, 0);
       g2.setStroke(stroke);
       g2.setColor(c);
       int type = roi.getType();
       Rectangle r = roi.getBounds();
       switch (type) {
          case Roi.RECTANGLE:
             Rectangle2D rect = new Rectangle2D.Double(r.x, r.y, r.width, r.height);
             g2.draw(rect);
             break;
          case Roi.OVAL:
             Ellipse2D ellipse = new Ellipse2D.Double(r.x, r.y, r.width, r.height);
             g2.draw(ellipse);
             break;
          case Roi.POLYGON:
              PolygonRoi pr = (PolygonRoi) roi;
             int x1Points[] = pr.getXCoordinates();
             int y1Points[] = pr.getYCoordinates();
             int Length = pr.getNCoordinates();
             GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, Length);
             polygon.moveTo((x1Points[0] + r.x), (y1Points[0] + r.y));
             for (int index = 1; index < Length; index++)
                polygon.lineTo((x1Points[index] + r.x), (y1Points[index] + r.y));
             polygon.closePath();
             g2.draw(polygon);
             break;
          case Roi.FREEROI:
             PolygonRoi freehandpr = (PolygonRoi) roi;
             int fhx1Points[] = freehandpr.getXCoordinates();
             int fhy1Points[] = freehandpr.getYCoordinates();
             int freehandLength = freehandpr.getNCoordinates();
             GeneralPath freehandpolygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, freehandLength);
             freehandpolygon.moveTo((fhx1Points[0] + r.x), (fhy1Points[0] + r.y));
             for (int index = 1; index < freehandLength; index++)
                freehandpolygon.lineTo((fhx1Points[index] + r.x), (fhy1Points[index] + r.y));
             freehandpolygon.closePath();
             g2.draw(freehandpolygon);
             break;
          case Roi.LINE:
             Line L = (Line) roi;
             Line2D line = new Line2D.Double((double)L.x1, (double)L.y1, (double)L.x2, (double)L.y2);
             g2.draw(line);
             break;
          case Roi.POLYLINE:
              PolygonRoi seglinpr = (PolygonRoi) roi;
             int slx1Points[] = seglinpr.getXCoordinates();
             int sly1Points[] = seglinpr.getYCoordinates();
             int seglinLength = seglinpr.getNCoordinates();
             GeneralPath seglinpolygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, seglinLength);
             seglinpolygon.moveTo((slx1Points[0] + r.x), (sly1Points[0] + r.y));
             for (int index = 1; index < seglinLength; index++)
                seglinpolygon.lineTo((slx1Points[index] + r.x), (sly1Points[index] + r.y));
             g2.draw(seglinpolygon);
             break;
          case Roi.FREELINE:
              PolygonRoi fhlinpr = (PolygonRoi) roi;
             int fhlx1Points[] = fhlinpr.getXCoordinates();
             int fhly1Points[] = fhlinpr.getYCoordinates();
             int fhlinLength = fhlinpr.getNCoordinates();
             GeneralPath freehandlinpolygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, fhlinLength);
             freehandlinpolygon.moveTo((fhlx1Points[0] + r.x), (fhly1Points[0] + r.y));
             for (int index = 1; index < fhlinLength; index++)
                freehandlinpolygon.lineTo((fhlx1Points[index] + r.x), (fhly1Points[index] + r.y));
             g2.draw(freehandlinpolygon);
             break;
          case Roi.ANGLE:
             //Angle Selections
             PolygonRoi apr = (PolygonRoi) roi;
             int ax1Points[] = apr.getXCoordinates();
             int ay1Points[] = apr.getYCoordinates();
             int aLength = apr.getNCoordinates();
             GeneralPath apolygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, aLength);
             apolygon.moveTo((ax1Points[0] + r.x), (ay1Points[0] + r.y));
             for (int index = 1; index < aLength; index++)
                apolygon.lineTo((ax1Points[index] + r.x), (ay1Points[index] + r.y));
             g2.draw(apolygon);
             break;
          }
       imp.setImage(bi);
    }

}
