package ij.desktop.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;



/**
 * @version 1.0 09/08/99
 */
public class MThumbSliderAdditionalUI {
  
  MThumbSlider  mSlider;
  BasicSliderUI ui;
  Rectangle[]   thumbRects;
  int           thumbNum;
  private transient boolean isDragging;
  Icon thumbRenderer;
  
  Rectangle trackRect;
  
  ChangeHandler changeHandler;
  TrackListener trackListener;
  

  
  public MThumbSliderAdditionalUI(BasicSliderUI ui)   {
    this.ui      = ui;
  }

  
  public void installUI(JComponent c)   {
    mSlider    = (MThumbSlider)c;
    thumbNum   = mSlider.getThumbNum();
    thumbRects = new Rectangle[thumbNum];
    for (int i=0; i<thumbNum; i++) {
      thumbRects[i] = new Rectangle();
    }
    isDragging = false;
    trackListener = new MThumbSliderAdditionalUI.TrackListener(mSlider);
    changeHandler = new ChangeHandler();
  }
  
  public void uninstallUI(JComponent c) {
    thumbRects = null;
    trackListener = null;
    changeHandler = null;
  }
  
      
  protected void calculateThumbsSize() {
    Dimension size = ((MThumbSliderAdditional)ui).getThumbSize();
    for (int i=0; i<thumbNum; i++) {
      thumbRects[i].setSize( size.width, size.height );
    }
  }
  
    
  protected void calculateThumbsLocation() {
    for (int i=0; i<thumbNum; i++) {
      if ( mSlider.getSnapToTicks() ) {
        int tickSpacing = mSlider.getMinorTickSpacing();	    
        if (tickSpacing == 0) {
          tickSpacing = mSlider.getMajorTickSpacing();
        }
        if (tickSpacing != 0) {      
          int sliderValue  = mSlider.getValueAt(i);           
          int snappedValue = sliderValue; 
          //int min = mSlider.getMinimumAt(i);                           
          int min = mSlider.getMinimum();                          
          if ( (sliderValue - min) % tickSpacing != 0 ) {
            float temp = (float)(sliderValue - min) / (float)tickSpacing;
            int whichTick = Math.round( temp );
            snappedValue = min + (whichTick * tickSpacing);            
            mSlider.setValueAt( snappedValue , i);           
          }
        }
      }	
      trackRect = getTrackRect(); 	
      if ( mSlider.getOrientation() == JSlider.HORIZONTAL ) {
        int value = mSlider.getValueAt(i);
        int valuePosition = ((MThumbSliderAdditional)ui).xPositionForValue(value);
        thumbRects[i].x = valuePosition - (thumbRects[i].width / 2);
        thumbRects[i].y = trackRect.y;
        
      } else {
        int valuePosition = ((MThumbSliderAdditional)ui).yPositionForValue(mSlider.getValueAt(i));     // need
        thumbRects[i].x = trackRect.x;
        thumbRects[i].y = valuePosition - (thumbRects[i].height / 2);
      }
    }
  }
  
    
  public int getThumbNum() {
    return thumbNum;
  }
  
  public Rectangle[] getThumbRects() {
    return thumbRects;
  }
  
  
 

  private static Rectangle unionRect = new Rectangle();
  
  public void setThumbLocationAt(int x, int y, int index)  { 
    Rectangle rect = thumbRects[index];  
    unionRect.setBounds( rect );
    
    rect.setLocation( x, y );
    SwingUtilities.computeUnion( rect.x, rect.y, rect.width, rect.height, unionRect ); 
    mSlider.repaint( unionRect.x, unionRect.y, unionRect.width, unionRect.height );
  }
  
  
  public Rectangle getTrackRect() {
    return ((MThumbSliderAdditional)ui).getTrackRect();
  }
  
  
  
  
  public class ChangeHandler implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      if ( !isDragging ) {
        calculateThumbsLocation();
	mSlider.repaint();
      }
    }
  }
  
  
  
  public class TrackListener extends MouseInputAdapter {
    protected transient int offset;
    protected transient int currentMouseX, currentMouseY;
    protected Rectangle adjustingThumbRect = null;
    protected int adjustingThumbIndex;
    protected MThumbSlider   slider;
    protected Rectangle trackRect;
    
    TrackListener(MThumbSlider slider) {
      this.slider = slider;
    }
  
    public void mousePressed(MouseEvent e) {
      if ( !slider.isEnabled() ) {
        return; 
      }
      currentMouseX = e.getX();
      currentMouseY = e.getY();
      slider.requestFocus();

      for (int i=0; i<thumbNum; i++) {
        Rectangle rect = thumbRects[i];
        if ( rect.contains(currentMouseX, currentMouseY) ) {
          
          switch ( slider.getOrientation() ) {
            case JSlider.VERTICAL:
                 offset = currentMouseY - rect.y;
                 break;
            case JSlider.HORIZONTAL:
                 offset = currentMouseX - rect.x;
                 break;
          }
          isDragging = true;
          slider.setValueIsAdjusting(true);
          adjustingThumbRect = rect;
          adjustingThumbIndex = i;
          return;
        }
      }
    }
    
    public void mouseDragged( MouseEvent e ) {                    
      if ( !slider.isEnabled() 
                 || !isDragging 
                 || !slider.getValueIsAdjusting()
                 || adjustingThumbRect == null ) {
        return;
      }
      int thumbMiddle = 0;
      currentMouseX = e.getX();
      currentMouseY = e.getY();

      Rectangle rect = thumbRects[adjustingThumbIndex];
      trackRect = getTrackRect();      
      switch ( slider.getOrientation() ) {
        case JSlider.VERTICAL:      
          int halfThumbHeight = rect.height / 2;
          int thumbTop    = e.getY() - offset;
          int trackTop    = trackRect.y;
          int trackBottom = trackRect.y + (trackRect.height - 1);

          thumbTop = Math.max( thumbTop, trackTop    - halfThumbHeight );
          thumbTop = Math.min( thumbTop, trackBottom - halfThumbHeight );

          setThumbLocationAt(rect.x, thumbTop, adjustingThumbIndex);

          thumbMiddle = thumbTop + halfThumbHeight;
          mSlider.setValueAt( ui.valueForYPosition( thumbMiddle ) , adjustingThumbIndex);
          break;
          
        case JSlider.HORIZONTAL:
          int halfThumbWidth = rect.width / 2;
          int thumbLeft  = e.getX() - offset;
          int trackLeft  = trackRect.x;
          int trackRight = trackRect.x + (trackRect.width - 1);

          thumbLeft = Math.max( thumbLeft, trackLeft  - halfThumbWidth );
          thumbLeft = Math.min( thumbLeft, trackRight - halfThumbWidth );

          setThumbLocationAt( thumbLeft, rect.y, adjustingThumbIndex);

          thumbMiddle = thumbLeft + halfThumbWidth;
          mSlider.setValueAt( ui.valueForXPosition( thumbMiddle ), adjustingThumbIndex );          
          break;
      }
    }
    
    public void mouseReleased(MouseEvent e) {
      if ( !slider.isEnabled() ) {
        return;
      }
      offset = 0;
      isDragging = false;
      mSlider.setValueIsAdjusting(false);
      mSlider.repaint();
    }

    public boolean shouldScroll(int direction) {
      return false;
    }
    
  }
  
}

