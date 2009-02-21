// This script demonstrates how to implement
// a filter that supports Undo and how to find
// out if the Process>Background>Options 
// "Black Background" flag is true of false.

  var imp = IJ.getImage();
  var ip = imp.getProcessor();
  ip.snapshot();
  Undo.setup(Undo.FILTER, imp);
  ip.invert();
  if (Prefs.blackBackground)
    ip.erode();
  else
    ip.dilate();
  ip.invert();
  imp.updateAndDraw();
