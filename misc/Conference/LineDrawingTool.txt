 // Line drawing tool

  var lineWidth=2;
  macro "Line Tool -C00bL1de0L1ee1" {
        getCursorLoc(x, y, z, flags);
        xstart = x; ystart = y;
        x2=x; y2=y;        
        while (true) {
            getCursorLoc(x, y, z, flags);
            if (flags&16==0) {
                setLineWidth(lineWidth);
                drawLine(xstart, ystart, x, y);
                run("Select None");
                exit;
            }
            if (x!=x2 || y!=y2)
                makeLine(xstart, ystart, x, y);
            x2=x; y2=y;
            wait(10);
        };
  }
  macro "Line Tool Options" {
      lineWidth = getNumber("Line Width:", lineWidth);
  }
