// This script draws 15 triangles using lines 
// that vary in width from 1 to 15 pixels.

   IJ.newImage("Test", "8-bit Black", 200, 200, 1);
   img = IJ.getImage();
   ip = img.getProcessor();
  for (i=1; i<=15; i++) {
      lw =i;
      color = 255;
      ip.setColor(0);
      ip.setRoi(0, 0, ip.getWidth(), ip.getHeight());
      ip.fill();
      ip.setLineWidth(lw);
      ip.setColor(color);
      ip.moveTo(50, 50);
      ip.lineTo(150, 50);
      ip.lineTo(150, 150);
      ip.lineTo(50, 50);
      img.updateAndDraw();
      IJ.wait(1000);
  }
 
