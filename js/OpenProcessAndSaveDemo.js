// OpenProcessAndSaveDemo.js
//
// This script demonstrates how to open,
// process and save an image.

  if (IJ.getVersion()<"1.41f")
      IJ.error("This script requires v1.41f or later");
  dir = IJ.getDirectory("home");
  img = IJ.openImage(dir+"test.jpg");
  if (img==null)
      IJ.error("File not found: "+dir+"test.jpg");
  IJ.run(img, "Unsharp Mask...", "radius=3 mask=0.6");
  IJ.run(img, "Flip Horizontally", "");
  IJ.run(img, "8-bit Color", "number=256");
  IJ.save(img, dir+"test.png");
  img = IJ.openImage(dir+"test.png");
  img.show();


/*
  // This is the Java version
  if (IJ.getVersion().compareTo("1.41f")<0) {
     IJ.error("This script requires v1.41f or later");
     return;
  }
  String dir = IJ.getDirectory("home");
  ImagePlus img = IJ.openImage(dir+"test.jpg");
  if (img==null) {
      IJ.error("File not found: "+dir+"test.jpg");
      return;
  }
  IJ.run(img, "Unsharp Mask...", "radius=3 mask=0.6");
  IJ.run(img, "Flip Horizontally", "");
  IJ.run(img, "8-bit Color", "number=256");
  IJ.save(img, dir+"test.png");
  img = IJ.openImage(dir+"test.png");
  img.show();
*/
 
