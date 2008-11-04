// JavaScript that calculates the mean pixel value pixel
// by pixel and by calling ImagePlus.getStatistics().
// Note: the first method only works with 8-bit images.

  start = System.currentTimeMillis();		
  ip = IJ.getImage().getProcessor();
  n = ip.getWidth()*ip.getHeight();
  pixels = ip.getPixels();
  sum = 0;
  for (i=0; i<n; i++)
     sum += pixels[i]&255;
  t1 = System.currentTimeMillis()-start;
  print(sum/n+" "+t1);

  start = System.currentTimeMillis();		
  img = IJ.getImage();
  t2 = System.currentTimeMillis()-start;
  print(img.getStatistics().mean+" "+t2+" ("+t1/t2+" times faster)");
