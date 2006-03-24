package ij.desktop.gui;

import java.awt.*;


/**
 * @version 1.0 09/08/99
 */
 //
 // MThumbSliderAdditionalUI <--> BasicMThumbSliderUI
 //                          <--> MetalMThumbSliderUI
 //                          <--> MotifMThumbSliderUI
 //
public interface MThumbSliderAdditional {

  public Rectangle getTrackRect();
  
  public Dimension getThumbSize();
  
  public int xPositionForValue(int value);
  
  public int yPositionForValue(int value);
  
}

