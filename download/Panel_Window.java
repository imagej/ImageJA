import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

/**
     Opens an image window and adds a panel below the image containing "Invert" and
     "Flip" buttons. If no images are open, creates a blank 400x200 byte image, 
     otherwise.
*/
public class Panel_Window implements PlugIn {

    static final int WIDTH = 400;
    static final int HEIGHT = 610;

    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp==null) {
            ImageProcessor ip = new ByteProcessor(WIDTH, HEIGHT);
            ip.setColor(Color.white);
            ip.fill();
            imp = new ImagePlus("Panel Window", ip);
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
    
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            IJ.write("mousePressed: ("+offScreenX(e.getX())+","+offScreenY(e.getY())+")");
        }
    
    } // CustomCanvas inner class
    
    
    class CustomWindow extends ImageWindow implements ActionListener {
    
        private Button button1, button2;
       
        CustomWindow(ImagePlus imp, ImageCanvas ic) {
            super(imp, ic);
            addPanel();
        }
    
        void addPanel() {
            Panel panel = new Panel();
            panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            button1 = new Button(" Invert ");
            button1.addActionListener(this);
            panel.add(button1);
            button2 = new Button(" Flip ");
            button2.addActionListener(this);
            panel.add(button2);
            add(panel);
            pack();
       Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            Point loc = getLocation();
            Dimension size = getSize();
            if (loc.y+size.height>screen.height)
                getCanvas().zoomOut(0, 0);
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
            addPanel();
        }
    
        void addPanel() {
            Panel panel = new Panel();
            panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            button1 = new Button(" Invert ");
            button1.addActionListener(this);
            panel.add(button1);
            button2 = new Button(" Flip ");
            button2.addActionListener(this);
            panel.add(button2);
            add(panel);
            pack();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            Point loc = getLocation();
            Dimension size = getSize();
            if (loc.y+size.height>screen.height)
                getCanvas().zoomOut(0, 0);
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

} // Panel_Window class
