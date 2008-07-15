import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.colorchooser.*;

import ij.plugin.frame.*;

//based on code from http://java.sun.com/docs/books/tutorial/uiswing/components/colorchooser.html#eg

public class Color_Chooser extends PlugInFrame implements ChangeListener, ItemListener, FocusListener, WindowListener {

    JColorChooser tcc = new JColorChooser(Toolbar.getForegroundColor());
    Checkbox cbForeground, cbBackground;
    static Frame instance;

    public Color_Chooser() {
        super("Color Chooser");
    }

    public void run(String arg) {

        if (instance!=null) {
            instance.toFront();
            return;
        }
        instance = this;

        setLayout(new BorderLayout());

        Panel panelRadio = new Panel(new FlowLayout());
        Panel panelColor = new Panel(new FlowLayout());

        CheckboxGroup cbg = new CheckboxGroup();
        cbForeground = new Checkbox("Foreground", cbg, true);
        cbBackground = new Checkbox("Background", cbg, false);
        panelRadio.add(cbForeground);
        panelRadio.add(cbBackground);

        cbForeground.addItemListener(this);
        cbBackground.addItemListener(this);

        addFocusListener(this);
        addWindowListener(this);

        tcc.getSelectionModel().addChangeListener(this);
        //tcc.setPreviewPanel(new JPanel());
        AbstractColorChooserPanel[] panels = tcc.getChooserPanels();
        //tcc.removeChooserPanel(panels[0]);
        //tcc.removeChooserPanel(panels[2]);
        panelColor.add(tcc);

        add(panelRadio, BorderLayout.NORTH);
        add(panelColor, BorderLayout.SOUTH);
        pack();
        WindowManager.addWindow(this);
        GUI.center(this);
        show();
    }

    public void itemStateChanged(ItemEvent e) {
        updateColor();
    }

    public void stateChanged(ChangeEvent e) {
        Color newColor = tcc.getColor();
        if(cbForeground.getState()) {
            IJ.setForegroundColor( newColor.getRed(), newColor.getGreen(), newColor.getBlue() );
        } else {
            IJ.setBackgroundColor( newColor.getRed(), newColor.getGreen(), newColor.getBlue() );
        }
    }

    public void windowActivated(WindowEvent e) {
        super.windowActivated(e);
        updateColor();
    }

    public void windowClosed(WindowEvent e) {
        super.windowClosed(e);
        instance = null;
    }

    void updateColor() {
        if(cbForeground.getState()) {
            tcc.setColor( Toolbar.getForegroundColor() );
        } else {
            tcc.setColor( Toolbar.getBackgroundColor() );
        }
    }

}
