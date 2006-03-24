package ij.desktop.gui;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class SplashWindow extends Window {

     private Image splashImage;

    /**

     * This attribute is set to true when method
     * paint(Graphics) has been called at least once since the
     * construction of this window.

     */
    private boolean paintCalled = false;

    /**
     * Constructs a splash window and centers it on the
     * screen.
     *
     * @param owner The frame owning the splash window.
     * @param splashImage The splashImage to be displayed.
     */

    public SplashWindow(Frame owner, Image splashImage) {
        super(owner);
        this.splashImage = splashImage;


        // Load the image
        MediaTracker mt = new MediaTracker(this);
        mt.addImage(splashImage,0);
        try {
            mt.waitForID(0);
        } catch(InterruptedException ie) {}

        // Center the window on the screen.
        int imgWidth = splashImage.getWidth(this);
        int imgHeight = splashImage.getHeight(this);  

        setSize(imgWidth, imgHeight);
        Dimension screenDim =

            Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
            (screenDim.width - imgWidth) / 2,
            (screenDim.height - imgHeight) / 2
        );
        MouseAdapter disposeOnClick = new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                // Note: To avoid that method splash hangs, we
                // must set paintCalled to true and call notifyAll.
                // This is necessary because the mouse click may
                // occur before the contents of the window
                // has been painted.
                synchronized (SplashWindow.this) {
                    SplashWindow.this.paintCalled = true;
                    SplashWindow.this.notifyAll();
                }
                dispose();
            }
        };
        addMouseListener(disposeOnClick);
    }

    /**
     * Updates the display area of the window.
     */

    public void update(Graphics g) {
        // Note: Since the paint method is going to draw an
        // image that covers the complete area of the component we
        // do not fill the component with its background color
        // here. This avoids flickering.

        g.setColor(getForeground());
        paint(g);
    }
    /**
     * Paints the image on the window.
     */

    public void paint(Graphics g) {
        g.drawImage(splashImage, 0, 0, this);

        // Notify method splash that the window
        // has been painted.
        if (! paintCalled) {
            paintCalled = true;
            synchronized (this) { notifyAll(); }
        }
    }
    /**
     * Constructs and displays a SplashWindow.<p>
     * This method is useful for startup splashs.
     * Dispose the returned frame to get rid of the splash window.<p>
     *
     * @param splashImage The image to be displayed.
     * @return Returns the frame that owns the SplashWindow.
     */

    public static Frame splash(Image splashImage) {
        Frame f = new Frame();
        SplashWindow w = new SplashWindow(f, splashImage);

        // Show the window.
        w.toFront();
        w.show();

 

        // Note: To make sure the user gets a chance to see the
        // splash window we wait until its paint method has been
        // called at least once by the AWT event dispatcher thread.
        if (! EventQueue.isDispatchThread()) {
            synchronized (w) {
                while (! w.paintCalled) {
                    try {

                        w.wait();

                    } catch (InterruptedException e) {}
                }
            }
        }
        return f;
    }
}


