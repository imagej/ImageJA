// This macro inverts an 8-bit image.
// On a 1024x1024 image, it is 188 times
// slower than the Edit>Invert command.

  w=getWidth; h=getHeight;
  start = getTime;
  for (y=0; y< h; y++) {
      for (x=0; x< w; x++)
           putPixel(x, y, 255-getPixel(x, y)); 
      if (y%20==0) showProgress(y, h);
  }
  print(round((w*h)/((getTime-start)/1000)) + " pixels/sec");
