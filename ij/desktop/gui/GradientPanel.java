package ij.desktop.gui;

 
import javax.swing.*; 
import java.awt.*; 
import java.awt.geom.*;    


public class GradientPanel extends javax.swing.JPanel {

    private Rectangle2D.Double square =
    new Rectangle2D.Double(10, 10, 18, 180);
    private GradientPaint gradient =
    new GradientPaint(0, 0, Color.black, 0, 200, Color.white);
    
    public GradientPanel(){
        setPreferredSize(new Dimension(18, 180));
        
        //setContentPane();
        setVisible(true);    
    }
    
    public void paintComponent(Graphics g) {
        clear(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setPaint(gradient);
        g2d.fill(square);
    }
    
    // super.paintComponent clears offscreen pixmap,
    // since we're using double buffering by default.
    
    protected void clear(Graphics g) {
        super.paintComponent(g);
    }
    
    protected Rectangle2D.Double getSquare() {
        return(square);
    }
    

}


