// This script duplicates what the Edit>Copy and
// File>New>Internal Clipboard commands do.

  img = IJ.getImage();
  ip = img.getProcessor();
  ip = ip.crop();
  roi = img.getRoi();
  if (roi!=null&&roi.isArea()&&roi.getType()!=Roi.RECTANGLE) {
     roi = roi.clone();
     roi.setLocation(0,0);
     ip.setColor(0);
     ip.snapshot();
     ip.fill();
     s1 = new ShapeRoi(roi);
     s2 = new ShapeRoi(new Roi(0,0, ip.getWidth(), ip.getHeight()));
     s3 = s1.xor(s2);
     ip.reset(s3.getMask());
  }
  new ImagePlus("img", ip).show();
