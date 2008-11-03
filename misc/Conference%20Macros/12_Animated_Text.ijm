//  AnimatedText
// This macro demonstrates how to use the setFont, 
// setColor, snapshot, reset and drawString functions.
// See Also: BouncingBar, Pong and RotatingPolygon macros

  if (nImages==0) run("Clown (14K)");
  setFont("Serif", 72, "antialiased");
  setColor(0,255,0);
  snapshot();
  x=-50; y=-20;
  while (x<getWidth) {
      reset();
      drawString("ImageJ", x++, y++);
  }
