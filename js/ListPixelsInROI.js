// List Pixels In ROI
//
// Displays the coordinates and values of
// the pixels within a non-rectangular ROI.
//
  var img = IJ.getImage();
  var roi = img.getRoi();
  var mask = roi!=null?roi.getMask():null;
  if (mask==null)
      IJ.error("Non-rectangular ROI required");
  var ip = img.getProcessor();
  var r = roi.getBounds();
  var z = img.getCurrentSlice()-1;
  for (var y=0; y<r.height; y++) {
     for (var x=0; x<r.width; x++) {
        if (mask.getPixel(x,y)!=0)
            IJ.log(x+" \t"+y+" \t"+z+"  \t"+ip.getPixel(r.x+x,r.y+y));
     }
  }

