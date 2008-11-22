// Rotates the selection by a specified angle.
// Implements the Edit>Selection>Rotate command.
// Enlarge Selection and Make Selection Band are also macros.
// Called from ij.plugin.Selection:
//     String value = IJ.runMacroFile("ij.jar:RotateSelection", angle);

// Demonstrates use of the getArgument, Dialog.*, getSelectionCoordinates,
// sqrt, atan2, sin, cos and makeSelection functions.

  //angle = parseFloat(getArgument());
  angle = 25;
  if (isNaN(angle)) exit("Angle is invalid: "+getArgument());
  Dialog.create("Rotate Selection");
  decimalPlaces = 0;
  if (floor(angle)!=angle) decimalPlaces = 2;
  Dialog.addNumber("     Angle:", angle, decimalPlaces, 3, "degrees");
  Dialog.addMessage("Enter negative angle to \nrotate counter-clockwise");
  Dialog.show();
  angle = Dialog.getNumber();
  theta = -angle*PI/180;
  getBoundingRect(xbase, ybase, width, height);
  xcenter=xbase+width/2; ycenter=ybase+height/2;
  getSelectionCoordinates(x, y);
  for (i=0; i<x.length; i++) {
      dx=x[i]-xcenter; dy=ycenter-y[i];
      r = sqrt(dx*dx+dy*dy);
      a = atan2(dy, dx);
      x[i] = xcenter + r*cos(a+theta);
      y[i] = ycenter - r*sin(a+theta);
  }
  makeSelection(selectionType, x, y);
  return toString(angle);

----------------------------------------------
More examples:

Run the Polygon.txt macro in ..ImageJ/macros:
    IJ.runMacroFile("Polygon");

Or call a macro inline using IJ.runMacro():

    String macro = "angle = getArgument();"+
        "getRawStatistics(area, mean);"+
        "setBackgroundColor(mean, mean, mean);"+
        "run('Arbitrarily...', 'interpolate fill angle='+angle);";
    double angle = 15;
    IJ.runMacro(macro, ""+angle);
