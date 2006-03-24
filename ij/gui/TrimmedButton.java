package ij.gui;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

/** This is an extended Button class used to reduce the width of the HUGE buttons on Mac OS X. */
public class TrimmedButton extends JButton {
    private int trim = 0;
    
    public TrimmedButton(String title, int trim) {
        super(title);
        this.trim = trim;
    }

    public Dimension getMinimumSize() {
        return new Dimension(super.getMinimumSize().width-trim, super.getMinimumSize().height);
    }

    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

}
