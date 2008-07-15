
import ij.IJ;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.OvalRoi;
import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.awt.*;

/**
 * User: Tom Larkworthy
 * Date: 08-Jun-2006
 * Time: 17:32:42
 */
public class ROI_Brush implements PlugIn {

    public void run(String arg) {
        if (IJ.versionLessThan("1.37c"))
            return;

        MacroInstaller installer = new MacroInstaller();
        String macros =
                "var brushWidth = 10;\n" +
                "var leftClick=16, alt=9;\n" +
                "\n" +
                "macro 'Roi Brush Tool - Cf800O11ff' {\n" +
                " while (true) {\n" +
                "  getCursorLoc(x, y, z, flags);\n" +
                "  if (flags&leftClick==0) exit();\n" +
                "  if (flags&alt==0){\n" +
                "   call('ROI_Brush.label', x,y,z,flags,brushWidth);\n" +
                "  }else{\n" +
                "   call('ROI_Brush.unlabel', x,y,z,flags,brushWidth);\n" +
                "  }\n" +
                "  wait(10);\n" +
                " }\n" +
                "}\n" +
                "\n" +
                "macro 'Roi Brush Tool Options...' {\n" +
                " brushWidth = getNumber('Roi Brush Width (pixels):', brushWidth);\n" +
                "}";
        installer.install(macros);
    }

    //methods in a macro accessable format
    public static void label(String x, String y, String z, String flags, String width) {
        label(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z),
                Integer.parseInt(flags),
                Integer.parseInt(width));
    }

    public static void unlabel(String x, String y, String z, String flags, String width) {
        unlabel(Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z),
                Integer.parseInt(flags),
                Integer.parseInt(width));
    }

    public static void label(int x, int y, int z, int flags, int width) {
        Roi roi = IJ.getImage().getRoi();
        if (roi != null) {
            if (!(roi instanceof ShapeRoi)) {
                roi = new ShapeRoi(roi);
            }

            ShapeRoi roiShape = (ShapeRoi) roi;

            roiShape.or(getBrushRoi(x, y, width));
        } else {
            roi = getBrushRoi(x, y, width);
        }

        IJ.getImage().setRoi(roi);
    }

    public static void unlabel(int x, int y, int z, int flags, int width) {
        Roi roi = IJ.getImage().getRoi();
        if (roi != null) {
            if (!(roi instanceof ShapeRoi)) {
                roi = new ShapeRoi(roi);
            }

            ShapeRoi roiShape = (ShapeRoi) roi;

            roiShape.not(getBrushRoi(x, y, width));

            IJ.getImage().setRoi(roi);
        }
    }


    private static ShapeRoi getBrushRoi(int x, int y, int width) {
        return new ShapeRoi(new OvalRoi(x - width / 2, y - width / 2, width, width));
    }


}
