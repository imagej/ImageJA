package ij.desktop.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * @version 1.0 9/3/99
 */  
public class MThumbSliderExample extends JFrame {
  public MThumbSliderExample() {
    super("MThumbSlider Example");  
    
    JSlider slider = new JSlider();
    slider.putClientProperty( "JSlider.isFilled", Boolean.TRUE );      
    
    int n = 2;
    MThumbSlider mSlider = new MThumbSlider(n);
    mSlider.setMinimum(0);
    mSlider.setMaximum(255);
    mSlider.setValueAt(0, 0);             
    mSlider.setValueAt(255, 1); 
    mSlider.setFillColorAt(Color.green,  0); 
    mSlider.setFillColorAt(Color.yellow, 1); 
    mSlider.setTrackFillColor(Color.red);
    mSlider.putClientProperty( "JSlider.isFilled", Boolean.TRUE );      
    
    getContentPane().setLayout(new FlowLayout());
    getContentPane().add(slider);
    getContentPane().add(mSlider);
  }

  public static void main (String args[]) {
    MThumbSliderExample f = new MThumbSliderExample();
    f.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	System.exit(0);
      }
    });
    f.setSize (300, 80);
    f.show();
  }
}
