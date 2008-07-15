import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

/**
    Adds a panel containing "Invert" and "Flip"  buttons to the right side of the current
    image or stack. Creates a blank 400x400 byte image if no image is open.
*/
public class Side_Panel implements PlugIn {

    static final int WIDTH = 400;
    static final int HEIGHT = 400;

    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        ImageProcessor ip;
        if (imp==null) {
            ip = new ByteProcessor(WIDTH, HEIGHT);
            ip.setColor(Color.white);
            ip.fill();
            imp = new ImagePlus("Side Panel Demo", ip);
        }
        CustomCanvas cc = new CustomCanvas(imp);
        if (imp.getStackSize()>1)
            new CustomStackWindow(imp, cc);
        else
           new CustomWindow(imp, cc);
    }


    class CustomCanvas extends ImageCanvas {
    
        CustomCanvas(ImagePlus imp) {
            super(imp);
        }

        public void paint(Graphics g) {
            super.paint(g);
            int size = 40;
            int screenSize = (int)(size*getMagnification());
            int x = screenX(imageWidth/2 - size/2);
            int y = screenY(imageHeight/2 - size/2);
            g.setColor(Color.red);
            g.drawOval(x, y, screenSize, screenSize);
        }
    
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            IJ.write("mousePressed: ("+offScreenX(e.getX())+","+offScreenY(e.getY())+")");
        }

        /** Overrides handlePopupMenu() in ImageCanvas to suppress the right-click popup menu. */
       //protected void handlePopupMenu(MouseEvent e) {
       //}
    
    } // CustomCanvas inner class
    
    
    class CustomWindow extends ImageWindow implements ActionListener {
    
        private Button button1, button2;
       
        CustomWindow(ImagePlus imp, ImageCanvas ic) {
            super(imp, ic);
            setLayout(new FlowLayout());
            addPanel();
        }
    
        void addPanel() {
            Panel panel = new Panel();
            panel.setLayout(new GridLayout(2, 1));
            button1 = new Button(" Invert ");
            button1.addActionListener(this);
            panel.add(button1);
            button2 = new Button(" Flip ");
            button2.addActionListener(this);
            panel.add(button2);
            add(panel);
            pack();
        }
      
        public void actionPerformed(ActionEvent e) {
            Object b = e.getSource();
            if (b==button1) {
                imp.getProcessor().invert();
                imp.updateAndDraw();
            } else {
                imp.getProcessor().flipVertical();
                imp.updateAndDraw();
            }
    
        }
        
    } // CustomWindow inner class

   class CustomStackWindow extends StackWindow implements ActionListener {
    
        private Button button1, button2;
       
        CustomStackWindow(ImagePlus imp, ImageCanvas ic) {
            super(imp, ic);
            setLayout(new FlowLayout());
            remove(sliceSelector);
            addPanel();
        }
    
        void addPanel() {
            Panel panel = new Panel();
            panel.setLayout(new GridLayout(4, 1));
            button1 = new Button("Invert Pixels");
            button1.addActionListener(this);
            panel.add(button1);
            button2 = new Button("Flip Vertically");
            button2.addActionListener(this);
            panel.add(button2);
            panel.add(new Label(""));
            panel.add(sliceSelector);
            add(panel);
            pack();
        }
      
        public void actionPerformed(ActionEvent e) {
            Object b = e.getSource();
            if (b==button1) {
                imp.getProcessor().invert();
                imp.updateAndDraw();
            } else {
                imp.getProcessor().flipVertical();
                imp.updateAndDraw();
            }
    
        }
        
    } // CustomStackWindow inner class

} // Side_Panel class
