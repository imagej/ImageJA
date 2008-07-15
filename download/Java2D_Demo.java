import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.font.*;

public class Java2D_Demo implements PlugIn {

        static final String TEXT = "ImageJ";
        static final int FONT_SIZE = 100;
        static final  int WIDTH =400;
        static final int HEIGHT = 250;

        public void run(String arg) {
                ImagePlus imp = WindowManager.getCurrentImage();
                if (imp!=null)
                    drawOnImage(imp);
                else
                    createNewImage();
        }

        public void drawOnImage(ImagePlus imp) {
                int width = imp.getWidth();
                int  height = imp.getHeight();
                BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = (Graphics2D)bi.getGraphics();
                g.drawImage(imp.getImage(), 0, 0, null);
                int fontSize = (FONT_SIZE*width)/360;
                drawDemo(TEXT, width, height, g, fontSize,  false);
                new ImagePlus("Java 2D Demo", bi).show();
        }

        public void createNewImage() {
                BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = (Graphics2D)bi.getGraphics();
                drawDemo(TEXT, WIDTH, HEIGHT, g, FONT_SIZE, true);
                ImagePlus imp = new ImagePlus("Java 2D Demo", bi);
                imp.show();
                //animate(imp);
       }

        public void animate( ImagePlus imp) {
                for (int size=10; size<200; size+=2) {
                    BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = (Graphics2D)bi.getGraphics();
                    drawDemo(TEXT, WIDTH, HEIGHT, g, size, true);
                	  imp.setImage(bi);
                    g.dispose();
                }
        }

        /** This method is from the 3D text shape sample program at
              http://java.sun.com/products/java-media/2D/samples/suite/  */
        void drawDemo(String text, int w, int h, Graphics2D g2, int fontSize, boolean drawStars) {
    
            int numStars = 300;
            Color colors[] = { Color.red, Color.green, Color.white };
            Shape shape, tshape,ribbon;
            AffineTransform at = AffineTransform.getTranslateInstance(-fontSize/20.0, -fontSize/20.0);
            Rectangle2D rect = new Rectangle2D.Double();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            /*
             * sets the color of each star, randomly sets their location
             * and renders them
             */
           if (drawStars) {
               for (int i = 0; i < numStars; i++) {
                    g2.setColor(colors[i%3]);
                    g2.setComposite(AlphaComposite.getInstance(
                             AlphaComposite.SRC_OVER, (float) Math.random()));
                    rect.setRect(w*Math.random(), h*Math.random(),2,2);
                    g2.fill(rect);
                }
            }
    
            FontRenderContext frc = g2.getFontRenderContext();
            Font font = new Font("serif.bolditalic", Font.PLAIN, fontSize);

            /*
             * gets the outline shape of the glyph vector created
             * from the current font and String
             */
            shape = font.createGlyphVector(frc, text).getOutline();

            // creates a shape transformed with the current transform
            tshape = at.createTransformedShape(shape);

            // gets a PathIterator that iterates the boundary of shape
            PathIterator pi = shape.getPathIterator(null);
            
            float seg[] = new float[6];
            float tseg[] = new float[6];
            
            GeneralPath working = new GeneralPath(GeneralPath.WIND_NON_ZERO);
            float x=0, y=0; // Current point on the path
            float tx=0, ty=0; // Transformed path point
            float cx=0, cy=0; // Last moveTo point, for SEG_CLOSE
            float tcx=0, tcy=0; // Transformed last moveTo point
            
            /*
             * iterates through the Shape and builds the ribbon
             * by adding GeneralPath objects
             */
            while(!pi.isDone()) {
                int segType = pi.currentSegment(seg);
                switch(segType) {
                    case PathIterator.SEG_MOVETO:
                            at.transform(seg, 0, tseg, 0, 1);
                            x = seg[0];
                            y = seg[1];
                            tx = tseg[0];
                            ty = tseg[1];
                            cx = x;
                            cy = y;
                            tcx = tx;
                            tcy = ty;
                            break;
                    case PathIterator.SEG_LINETO:
                            at.transform(seg, 0, tseg, 0, 1);
                            if (Line2D.relativeCCW(x, y, tx, ty,
                                                   seg[0], seg[1]) < 0) {
                                working.moveTo(x, y);
                                working.lineTo(seg[0], seg[1]);
                                working.lineTo(tseg[0], tseg[1]);
                                working.lineTo(tx, ty);
                                working.lineTo(x, y);
                            } else {
                                working.moveTo(x, y);
                                working.lineTo(tx, ty);
                                working.lineTo(tseg[0], tseg[1]);
                                working.lineTo(seg[0], seg[1]);
                                working.lineTo(x, y);
                            }
                            
                            x = seg[0];
                            y = seg[1];
                            tx = tseg[0];
                            ty = tseg[1];
                            break;
                            
                    case PathIterator.SEG_QUADTO:
                            at.transform(seg, 0, tseg, 0, 2);
                            if (Line2D.relativeCCW(x, y, tx, ty,
                                                   seg[2], seg[3]) < 0) {
                                working.moveTo(x, y);
                                working.quadTo(seg[0], seg[1],
                                               seg[2], seg[3]);
                                working.lineTo(tseg[2], tseg[3]);
                                working.quadTo(tseg[0], tseg[1],
                                               tx, ty);
                                working.lineTo(x, y);
                            } else {
                                working.moveTo(x, y);
                                working.lineTo(tx, ty);
                                working.quadTo(tseg[0], tseg[1],
                                               tseg[2], tseg[3]);
                                working.lineTo(seg[2], seg[3]);
                                working.quadTo(seg[0], seg[1],
                                               x, y);
                            }
                    
                            x = seg[2];
                            y = seg[3];
                            tx = tseg[2];
                            ty = tseg[3];
                            break;
            
                    case PathIterator.SEG_CUBICTO:
                            at.transform(seg, 0, tseg, 0, 3);
                            if (Line2D.relativeCCW(x, y, tx, ty,
                                                   seg[4], seg[5]) < 0) {
                                working.moveTo(x, y);
                                working.curveTo(seg[0], seg[1],
                                                seg[2], seg[3],
                                                seg[4], seg[5]);
                                working.lineTo(tseg[4], tseg[5]);
                                working.curveTo(tseg[2], tseg[3],
                                                tseg[0], tseg[1],
                                                tx, ty);
                                working.lineTo(x, y);
                            } else {
                                working.moveTo(x, y);
                                working.lineTo(tx, ty);
                                working.curveTo(tseg[0], tseg[1],
                                                tseg[2], tseg[3],
                                                tseg[4], tseg[5]);
                                working.lineTo(seg[4], seg[5]);
                                working.curveTo(seg[2], seg[3],
                                                seg[0], seg[1],
                                                x, y);
                            }
                    
                            x = seg[4];
                            y = seg[5];
                            tx = tseg[4];
                            ty = tseg[5];
                            break;
            
                    case PathIterator.SEG_CLOSE:
                            if (Line2D.relativeCCW(x, y, tx, ty,
                                                   cx, cy) < 0) {
                                working.moveTo(x, y);
                                working.lineTo(cx, cy);
                                working.lineTo(tcx, tcy);
                                working.lineTo(tx, ty);
                                working.lineTo(x, y);
                            } else {
                                working.moveTo(x, y);
                                working.lineTo(tx, ty);
                                working.lineTo(tcx, tcy);
                                working.lineTo(cx, cy);
                                working.lineTo(x, y);
                            }
                            x = cx; 
                            y = cy;
                            tx = tcx;
                            ty = tcy;
                }
                pi.next();
            } // while
            ribbon = working;
    
            g2.setComposite(AlphaComposite.SrcOver);
            Rectangle r = shape.getBounds();
            g2.translate(w*.5-r.width*.5,h*.4+r.height*.5);

            // fills the transformed shape with blue
            g2.setColor(Color.blue);
            g2.fill(tshape);
	
            /* 
             * fills the shape producing the 3D effect with a 
             * partially opaque white color
             */
            g2.setColor(new Color(255, 255, 255,200));
            g2.fill(ribbon);
	 
            // fills the untransformed shape with white
            g2.setColor(Color.white);
            g2.fill(shape);

            // strokes the outline of shape with blue    
            g2.setColor(Color.blue);
            g2.draw(shape);
        }
}
