//===========================================================================
// Cell_Outliner.java
//
// Draw cell boundaries across time, with a minimum of fuss.
//
// Based on Segmenting Assistant, which is
// Copyright (C) 2002 Michael A. Miller <mmiller3@iupui.edu>
//
// Changes made in the Sheetz Lab, Columbia University:
// Hacked a bit 2002 Ben Dubin-Thaler, bjd14@columbia.edu and Jana Gruenewald
// Hacked up beyond recognition 2002 mike castleman, mlc67@columbia.edu
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or (at
// your option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
// USA.
//
//===========================================================================

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.PlugInFrame;
import ij.measure.*;

/**
 * ImageJ plugin to outline cells.
 *
 * @author Michael Miller, Ben Dubin-Thaler, mike castleman
 */
public class Cell_Outliner extends PlugInFrame implements Measurements {
    ImagePlus imp;
    WandMM wand;
    Roi roi;
    ImageStack stack;

    double x_centroid;
    double y_centroid;
    double min_level;
    double max_level;
    double image_min;
    double image_max;
    double pixn_old_half, rw, rh;

    int min_width=15;
    int min_height=15;
    int xd=20;
    int x, y, z, pixn_old, pixn_new, roi_width_old, roi_height_old;
    int slice_offset = 0;

    String s;
    Rectangle r;

    private JLabel xLabel, yLabel, maxLevelLabel;
    private JSlider xSlider, ySlider, maxLevelSlider;
    private JLabel sliceLabel;
    private JTextField firstSlice, lastSlice;

    // by declaring this static final, we allow javac to perform the test
    // at compile time rather than runtime, and remove debug code when
    // debug is false. thus there is *no* performance hit in non-debug mode
    // with the disadvantage that we cannot switch between debug and
    // release without a recompile.
    private static final boolean DEBUG = false;

    /** Class constructor.
     * 
     * This creates a new <code>Cell_Outliner</code> and displays it --
     * unless a <code>Cell_Outliner</code> exists, in which case this
     * constructor simply brings the previous one to the front, and
     * goes away.
     */
    public Cell_Outliner() {
	super("Cell Outliner");

	if (DEBUG)
	    IJ.log("Cell_Outliner.<init>(): initializtion happening");

	initialize();
	makeWindow();
    }

    public void windowActivated(WindowEvent e) {
	if (DEBUG)
	    IJ.log("Cell_Outliner.windowActivated(" + e.toString() + ")");

	super.windowActivated(e);

	/* re-initialize if the current image has changed */
	if (WindowManager.getCurrentImage() != imp) {
	    slice_offset = 0;
	    initialize();
	    remakeWindow();
	}
	updateSliceLabel();
    }

    /** Legacy method. Not used. */
    public void run(ImageProcessor ip) {
        if ( DEBUG ) IJ.log("Cell_Outliner.run(ImageProcessor)...");

        initialize();

        if ( roi == null || roi.getType() > 3 ) {
            IJ.error("You must draw a closed ROI before using this plugin." );
            return;
        }
        if ( DEBUG ) IJ.log( "roi.getType() = " + roi.getType() );

        makeWindow();

        if ( outline() ) IJ.log( "found:" + imp.getRoi() );

    }

    public void initialize() {
        if ( DEBUG ) IJ.log("Cell_Outliner.initialize...");

        imp = WindowManager.getCurrentImage();
	stack = imp.getStack();
	if (stack == null) {
	    IJ.error("This plugin only works on image stacks, not single frames.");
	    return;
	}
        roi = imp.getRoi();
	if (roi == null) { // ensure there is always some ROI.
	    roi = new Roi(0, 0, imp.getWidth(), imp.getHeight(), imp);
	    imp.setRoi(roi);
	}

        int measurements = Analyzer.getMeasurements();
        // defined in Set Measurements dialog
        measurements |= CENTROID; // make sure centroid is included
        measurements |= MIN_MAX;  // make sure min_max is included
        Analyzer.setMeasurements(measurements);

        ImageStatistics stats = imp.getStatistics(measurements);

        x_centroid = stats.xCentroid;
        y_centroid = stats.yCentroid;

        min_level = stats.min;
        max_level = stats.max;

        imp.killRoi();
        stats = imp.getStatistics(measurements);
        image_min = stats.min;
        image_max = stats.max;
        IJ.log("min max = " + image_min + " " + image_max );

        if ( DEBUG ) {
	    IJ.log("Centroid = (" + x_centroid + ", " + y_centroid + ")" );
	    IJ.log("Pixel levels are in [" + min_level + ", " + max_level + "]" );
	}
    }

    boolean outline() {
        if ( DEBUG ) IJ.log("Cell_Outliner.outline...");

        if ( DEBUG ) IJ.log("trying to make a WandMM...");
        wand = new WandMM( imp.getProcessor() ) ;
        if ( DEBUG ) IJ.log("made a WandMM...");

        imp.killRoi();
        wand.npoints = 0;

        x = (int)x_centroid;
        y = (int)y_centroid;
        wand.autoOutline( x, y, (int)min_level, (int)max_level );

        s = ":[" + x + "," + y + "]: ("
            + wand.npoints + ":" + wand.xpoints[0] + "," + wand.ypoints[0] + ")";
        if ( wand.npoints < 3 ) {
            IJ.log( "wand.autoOutline failed" + s );
            return false;
        }

        roi = new PolygonRoi( wand.xpoints, wand.ypoints, wand.npoints, imp, Roi.TRACED_ROI);
        imp.setRoi( roi );
        r = roi.getBoundingRect();
        if ( r.width < min_width || r.height < min_height ) {
            IJ.log( "wand object too small:" + r.width + "x" + r.height + " " + s );
            return false;
        }
        if ( wand.xpoints[0] - x > xd ) {
            IJ.log( "wand too far?" + s );
            return false;
        }
        return true;
    }

    protected void makeWindow() {
        if ( DEBUG ) IJ.log("Cell_Outliner.makeWindow...");

        // f.setSize( 200, 200 );
        // f.setLocation(500, 500);

        // Create a label for the x position slider:
        xLabel = new JLabel( "Horizontal Centroid:: " + (int)x_centroid,
			     JLabel.LEFT );
        xLabel.setAlignmentX( Component.LEFT_ALIGNMENT );

        // Create the x position slider:
        xSlider = new JSlider( JSlider.HORIZONTAL,
			       0, imp.getWidth(), (int)x_centroid );
        xSlider.setMajorTickSpacing( imp.getWidth()/4 );
        xSlider.setMinorTickSpacing( imp.getWidth()/16 );
        xSlider.setPaintTicks( true );
        xSlider.setPaintLabels( true );
        xSlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );

        // Create a label for the y position slider:
        yLabel = new JLabel( "Vertical Centroid:: " + (int)y_centroid,
			     JLabel.LEFT );
        xLabel.setAlignmentX( Component.LEFT_ALIGNMENT );

        // Create the y position slider:
        ySlider = new JSlider( JSlider.HORIZONTAL,
			       0, imp.getHeight(), (int)y_centroid );
        ySlider.setMajorTickSpacing( imp.getHeight()/4 );
        ySlider.setMinorTickSpacing( imp.getHeight()/16 );
        ySlider.setPaintTicks( true );
        ySlider.setPaintLabels( true );
        ySlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );


        // Create a label for the max level slider:
        maxLevelLabel = new JLabel( "Max Level: " + (int)max_level,
				    JLabel.LEFT );
        maxLevelLabel.setAlignmentX( Component.LEFT_ALIGNMENT );

        // Create the max level slider:
        maxLevelSlider = new JSlider( JSlider.HORIZONTAL,
				      (int)image_min, (int)image_max,
				      (int)max_level );
        maxLevelSlider.setMajorTickSpacing( (int)(image_max-image_min)/4 );
        maxLevelSlider.setMinorTickSpacing( (int)(image_max-image_min)/16 );
        maxLevelSlider.setPaintTicks( true );
        maxLevelSlider.setPaintLabels( true );
        maxLevelSlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );

	JButton preprocessButton = new JButton("Preprocess Image");
	preprocessButton.setActionCommand("Preprocess Image");

        JButton outlineButton = new JButton( "Outline" );
        outlineButton.setActionCommand( "Outline" );

        JButton newROIButton = new JButton( "Set New ROI" );
        newROIButton.setActionCommand( "Set New ROI" );

        JButton doneButton = new JButton( "Done" );
        doneButton.setActionCommand( "Done" );

        // For stacks:
        int ns = stack.getSize();
        if ( DEBUG ) IJ.log( "getSize = " + ns );

        JButton nextButton = new JButton( "+1" );
        nextButton.setActionCommand( "+1" );

	JButton plusFive = new JButton( "+5" );
	plusFive.setActionCommand("+5");

	JButton plus25 = new JButton ("+25");
	plus25.setActionCommand("+25");

        JButton previousButton = new JButton( "-1" );
        previousButton.setActionCommand( "-1" );

	JButton minusFive = new JButton ("-5");
	minusFive.setActionCommand("-5");

	JButton minus25 = new JButton("-25");
	minus25.setActionCommand("-25");

	/* Set a too-long label here, so that when things are packed,
	   they will be sufficiently big. Then, we re-set the label text
	   to say what we actually want to say. */
	sliceLabel = new JLabel("Slice 4444/4444", JLabel.CENTER);

        JButton goButton = new JButton ( "Go" );
        goButton.setActionCommand( "Go" );

	JButton startButton = new JButton("Starting Slice:");
	startButton.setActionCommand("starting slice");

	JButton endButton = new JButton("Ending Slice");
	endButton.setActionCommand("ending slice");

	firstSlice = new JTextField("1", 4);
	lastSlice = new JTextField(Integer.toString(imp.getStackSize()), 4);

        // Setup event handlers for the sliders:
        xSlider.addChangeListener( new ChangeListener() {
		public void stateChanged( ChangeEvent e ) {
		    xLabel.setText( "Horizontal Centroid:: " + xSlider.getValue() );
		    x_centroid = (double)xSlider.getValue();
		    outline();
		}
	    } );
        ySlider.addChangeListener( new ChangeListener() {
		public void stateChanged( ChangeEvent e ) {
		    yLabel.setText( "Vertical Centroid:: " + ySlider.getValue() );
		    y_centroid = (double)ySlider.getValue();
		    outline();
		}
	    } );
        maxLevelSlider.addChangeListener( new ChangeListener() {
		public void stateChanged( ChangeEvent e ) {
		    maxLevelLabel.setText( "Max Level: " + maxLevelSlider.getValue() );
		    max_level = (double)maxLevelSlider.getValue();
		    outline();
		}
	    } );


        // Register for mouse events:
	preprocessButton.addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if (DEBUG)
			IJ.log("Button pressed: " + e.getActionCommand());

		    trimStack();
		    IJ.run("Miscellaneous...", "real=256 divide=3.4028235E38 use scale");
                                                IJ.run("Select None");
                                                IJ.run("Median...", "radius=1 stack");
		    initialize(); // for fun and profit.
		    remakeWindow();
		}
	    });

        outlineButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( DEBUG ) IJ.log( "Button pressed: " + e.getActionCommand() );
		    outline();
		}
	    } );
        newROIButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( DEBUG ) IJ.log( "Button pressed: " + e.getActionCommand() );
		    initialize();
		    // maxLevelSlider.setValue( (int)max_level );
		    // maxLevelLabel.setText( "Max Level: " + maxLevelSlider.getValue() );
		    xSlider.setValue( (int)x_centroid );
		    xLabel.setText( "Horizontal Centroid: " + xSlider.getValue() );
		    ySlider.setValue( (int)y_centroid );
		    yLabel.setText( "Vertical Centroid: " + ySlider.getValue() );
		}
	    } );

        goButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e )
		{
		    if ( DEBUG )
			IJ.log( "Button pressed: " + e.getActionCommand() );

		    trimStack();

		    outline();

		    IJ.run("Miscellaneous...", "real=256 divide=3.4028235E38 use scale hide");
		    /**IJ.run("Median...", "radius=1 stack");*/

		    for (int i = 1; i <= imp.getStackSize(); ++i) {
			imp.setSlice(i);
			IJ.log("Working on slice no" +imp.getCurrentSlice());
			/**pixn_old = wand.npoints;
			   pixn_old_half = pixn_old * 0.5;*/
			roi_width_old = r.width;
			roi_height_old = r.height;
			outline();
			/**pixn_new = wand.npoints;
			   IJ.log("Daten: [pix_old:" + pixn_old + "pix_new:" + pixn_new + "] , (" + wand.npoints + ":" + wand.xpoints[0] + "," + wand.ypoints[0] + ")");*/
			z = (int)y_centroid;
			rw = r.width;
			rh = r.height;
			while ( rw <= roi_width_old * 0.75 || rh <= roi_height_old *0.75) { /**(pixn_new <= pixn_old_half)*/
			    IJ.log("wand zu klein ");
			    imp.killRoi();
			    /**y_centroid = y_centroid + 1;
			       y = (int)y_centroid;*/
			    z ++;
			    IJ.log("Y-Centroid: " +z);
			    wand.autoOutline( x, z, (int)min_level, (int)max_level );
			    roi = new PolygonRoi( wand.xpoints, wand.ypoints, wand.npoints, imp, Roi.TRACED_ROI );
			    imp.setRoi( roi );
			    IJ.log("Daten: [" + x + "," + y + "]: ("
				     + wand.npoints + ":" + wand.xpoints[0] + "," + wand.ypoints[0] + ")");
			    r = roi.getBoundingRect();
			    rw = r.width;
			    rh = r.height;
			    /**pixn_new = wand.npoints;*/
			    
			}
			processSlice();
		    }
                                                    IJ.run("8-bit");
                                                    IJ.run("Miscellaneous...", "real=256 divide=3.4028235E38 use scale");
		    String [] messages = new String[] { 
			"Thank you for using Cell Outliner.",
			"Your settings were:",
			"(Initial) Centroid: (" + xSlider.getValue() + ", " + ySlider.getValue() + ")",
			"Maximum Level: " + maxLevelSlider.getValue(),
			"Frames Used: [" + (slice_offset+1) + ", " + (slice_offset+imp.getStackSize()) + "]"
		    };
		    JOptionPane.showConfirmDialog(Cell_Outliner.this, messages, "Cell Outliner Done!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
		}
	    } );

        doneButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( DEBUG ) IJ.log( "Button pressed: " + e.getActionCommand() );

		    // set "Hide Stack Dialogue" off again
		    IJ.run("Miscellaneous...", "real=256 divide=3.4028235E38 use scale");

		    Cell_Outliner.this.setVisible( false );
		}
	    } );

        nextButton.addActionListener( new SliceMover(1) );
        previousButton.addActionListener( new SliceMover(-1) );
	plusFive.addActionListener(new SliceMover(5));
	plus25.addActionListener(new SliceMover(25));
	minusFive.addActionListener(new SliceMover(-5));
	minus25.addActionListener(new SliceMover(-25));

	startButton.addActionListener(new SliceSetter(firstSlice));
	endButton.addActionListener(new SliceSetter(lastSlice));


        // Position everything in the content pane:
        JPanel contentPane = new JPanel();
        contentPane.setLayout( new BoxLayout( contentPane, BoxLayout.Y_AXIS ) );
        contentPane.add( xLabel );
        contentPane.add( xSlider );
        contentPane.add( yLabel );
        contentPane.add( ySlider );
        contentPane.add( maxLevelLabel );
        contentPane.add( maxLevelSlider );

        JPanel roiPanel = new JPanel();
	roiPanel.add( preprocessButton );
        roiPanel.add( outlineButton );
        roiPanel.add( newROIButton );
        contentPane.add(roiPanel);

        JPanel slicePanel = new JPanel();
	slicePanel.add(minus25);
	slicePanel.add(minusFive);
        slicePanel.add( previousButton );
	slicePanel.add(sliceLabel);
        slicePanel.add( nextButton );
	slicePanel.add(plusFive);
	slicePanel.add(plus25);
        contentPane.add(slicePanel);

	JPanel slicerPanel = new JPanel();
	slicerPanel.add(startButton);
	slicerPanel.add(firstSlice);
	slicerPanel.add(Box.createHorizontalStrut(10));
	slicerPanel.add(endButton);
	slicerPanel.add(lastSlice);
	slicerPanel.add(Box.createHorizontalStrut(30));
	slicerPanel.add(goButton);
	contentPane.add(slicerPanel);

        contentPane.add( doneButton );

	//     contentPane.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
	add(contentPane);

        pack();
        setVisible( true );
	updateSliceLabel(); // ich.
    }

    /**
     * Demonstration of absolutely horrible design practices.
     *
     * When we detect that a new image is active, we must remake all
     * the sliders, and fun stuff like that.
     * <p>
     * If this code were well-designed to start with, there would probably
     * be a nice way to write this function. But it isn't, so there isn't.
     * That, combined with the fact that I am too lazy to rewrite the whole
     * damn thing, leads to this function.
     * @author mlc
     */
    protected void remakeWindow() {
	if (DEBUG) {
	    IJ.log("Cell_Outliner.remakeWindow()");
	    IJ.log("x: 0 " + x_centroid + " " + imp.getWidth());
	    IJ.log("y: 0 " + y_centroid + " " + imp.getHeight());
	    IJ.log("level: " + image_min + " " + max_level + " " + image_max);
	}

	xLabel.setText("Horizontal Centroid:: " + (int)x_centroid);
	xSlider.setMinimum(0);
	xSlider.setMaximum(imp.getWidth());
	xSlider.setValue((int)x_centroid);
	xSlider.setMajorTickSpacing(imp.getWidth() / 4);
	xSlider.setMinorTickSpacing(imp.getWidth() / 16);
	xSlider.repaint(); // shouldn't be needed...

	yLabel.setText("Vertical Centroid:: " + (int)y_centroid);
	ySlider.setMinimum(0);
	ySlider.setMaximum(imp.getHeight());
	ySlider.setValue((int)y_centroid);
	ySlider.setMajorTickSpacing(imp.getHeight() / 4);
	ySlider.setMinorTickSpacing(imp.getHeight() / 16);
	ySlider.repaint(); // shouldn't be needed...

	maxLevelLabel.setText("Max Level: " + (int)max_level);
	maxLevelSlider.setMinimum((int)image_min);
	maxLevelSlider.setMaximum((int)image_max);
	maxLevelSlider.setValue((int)max_level);
	maxLevelSlider.setMajorTickSpacing((int)(image_max-image_min) / 4);
	maxLevelSlider.setMinorTickSpacing((int)(image_max-image_min) / 16);
	maxLevelSlider.repaint(); // shouldn't be needed...

	firstSlice.setText("1");
	lastSlice.setText(Integer.toString(imp.getStackSize()));

	updateSliceLabel();
    }

    /** Update the slice label as appropriate. */
    public void updateSliceLabel() {
	if (imp != null) {
	    StringBuffer buf = new StringBuffer("Slice ");
	    buf.append(imp.getCurrentSlice());
	    buf.append('/');
	    buf.append(imp.getStackSize());
	    if (DEBUG)
		IJ.log("updateSliceLabel: " + buf.toString());
	    sliceLabel.setText(buf.toString());
	} else {
	    sliceLabel.setText("No Image!");
	}
    }

    /** Processes a single slice of the image. */
    private void processSlice() {
	Roi oldRoi = imp.getRoi();
	ImageProcessor p = imp.getProcessor();

	imp.killRoi();
	p.setColor(Color.black);
	p.fill();

	imp.setRoi(oldRoi);
	p.setColor(Color.white);
	oldRoi.drawPixels();

	// IJ.run( "Clear" );
	// IJ.run( "Clear Outside" );
	// IJ.run( "Draw" );
    }

    /** Trims the stack, removing unwanted slices.
     * @throws IllegalArgumentException when it feels like it
     */
    private void trimStack() throws IllegalArgumentException{
	int first, last;
	try {
	    first = Integer.parseInt(firstSlice.getText());
	    last = Integer.parseInt(lastSlice.getText());
	} catch (NumberFormatException ex) {
	    IJ.error("Invalid first or last slice: please use an integer!");
	    throw new IllegalArgumentException();
	}

	if (first < 1 || last > imp.getStackSize()) {
	    IJ.error("Invalid slice range: please stick within the bounds of the current image stack.");
	    throw new IllegalArgumentException();
	}

	if (first >= last) {
	    IJ.error("Invalid slice range: first slice must be strictly less than last slice.");
	    throw new IllegalArgumentException();
	}

	// okay, now trim the stack as required.
	// the API for this is pretty crappy, but we can make do.
	stack = imp.getStack(); // for sanity.
	while (stack.getSize() > last) {
	    stack.deleteLastSlice();
	}
	for (int i = 1; i < first; ++i) {
	    stack.deleteSlice(1);
	}
	imp.setStack(null, stack); // is this needed? can't hurt.
	imp.setSlice(1);
	slice_offset += first - 1;

	firstSlice.setText("1");
	lastSlice.setText(Integer.toString(imp.getStackSize()));
	updateSliceLabel();
    }

    /**
     * Abstract the previous / next slice functionality.
     */
    private class SliceMover implements ActionListener {
	private int n;
	public SliceMover(int n) {
	    this.n = n;
	}
	public void actionPerformed( ActionEvent e ) {
	    if ( DEBUG ) IJ.log( "Button pressed: " + e.getActionCommand() +
				   "; moving " + n + " slice(s)." );
	    int x = imp.getCurrentSlice() + n;
	    if (x > stack.getSize()) {
		x = stack.getSize();
	    } else if (x < 1) {
		x = 1;
	    }
	    imp.setSlice(x);
	    outline();
	    updateSliceLabel();
	}
    }

    /**
     * Set the value of a text box based on the current slice.
     */
    private class SliceSetter implements ActionListener {
	private JTextField target;
	public SliceSetter(JTextField target) {
	    this.target = target;
	}

	public void actionPerformed(ActionEvent e) {
	    if (DEBUG) IJ.log("SliceSetter: " + e.getActionCommand());

	    target.setText(Integer.toString(imp.getCurrentSlice()));
	}
    }
}

/**
 * Reimplementation of wand tool.
 *
 * This class implements something like ImageJ's wand (tracing) tool.
 * The difference is that this one is intended to work with all image
 * types, not just byte and 8 bit color images.
 */
class WandMM {
    private static final boolean DEBUG = false;

    static final int UP = 0,
	DOWN = 1,
	UP_OR_DOWN = 2,
	LEFT = 3,
	RIGHT = 4,
	LEFT_OR_RIGHT = 5,
	NA = 6;

    // The number of points in the generated outline:
    public int npoints;
    private int maxPoints = 1000;

    // The x-coordinates of the points in the outline:
    public int[] xpoints = new int[maxPoints];
    // The y-coordinates of the points in the outline:
    public int[] ypoints = new int[maxPoints];

    private ImageProcessor wandip;

    private int width, height;
    private float lowerThreshold, upperThreshold;

    // Construct a Wand object from an ImageProcessor:
    public WandMM( ImageProcessor ip ) {
	if ( DEBUG ) IJ.log("WandMM...");

	wandip = ip;

	width = ip.getWidth();
	height = ip.getHeight();
	if ( DEBUG ) IJ.log("WandMM middle pixel = " + ip.getPixelValue(128,128));
	if ( DEBUG ) IJ.log("done with constructor");
    }

    private boolean inside( int x, int y ) {
	//if ( DEBUG ) IJ.log("WandMM.inside...");
	float value;
	if ( DEBUG ) IJ.log( "WandMM.getPixel(x,y) = " + wandip.getPixelValue(x,y) );

	// sanity check.
	if (x >= width || x < 0 || y >= height || y < 0)
	    return false;

	//value =  getPixel( x, y );
	value = wandip.getPixelValue(x,y);
	return ( value >= lowerThreshold ) && ( value <= upperThreshold );
    }

    // Are we tracing a one pixel wide line?
    boolean isLine( int xs, int ys ) {
	if ( DEBUG ) IJ.log("WandMM.isLine...");

	int r = 5;
	int xmin = xs;
	int xmax = xs + 2 * r;
	if ( xmax >= width ) xmax = width - 1;
	int ymin = ys - r;
	if ( ymin < 0 ) ymin = 0;
	int ymax = ys + r;
	if ( ymax >= height ) ymax = height - 1;
	int area = 0;
	int insideCount = 0;
	for ( int x = xmin; ( x <= xmax ); x++ )
	    for ( int y = ymin; y <= ymax; y++ ) {
		area++;
		if ( inside( x, y ) )
		    insideCount++;
	    }
	if (IJ.debugMode)
	    IJ.log((((double)insideCount)/area>=0.75?"line ":"blob ")
		     + insideCount
		     + " "
		     + area
		     + " "
		     + IJ.d2s(((double)insideCount)/area));
	return ((double)insideCount)/area>=0.75;
    }

    // Traces an object defined by lower and upper threshold
    // values. The boundary points are stored in the public xpoints
    // and ypoints fields.
    public void autoOutline( int startX, int startY, int lower, int upper ) {
	if ( DEBUG ) IJ.log("WandMM.autoOutline...");

	int x = startX;
	int y = startY;
	int direction;
	lowerThreshold = lower;
	upperThreshold = upper;
	if ( inside(x,y) ) {
	    do { x++; } while ( inside(x,y) );
	    if ( ! inside( x-1, y-1 ) )
		direction = RIGHT;
	    else if ( inside( x, y-1 ) )
		direction = LEFT;
	    else
		direction = DOWN;
	} else {
	    do { x++; } while ( ! inside(x,y) && x < width );
	    direction = UP;
	    if ( x >= width ) return;
	}
	traceEdge( x, y, direction );
    }

    void traceEdge( int xstart, int ystart, int startingDirection ) {
	if ( DEBUG ) IJ.log("WandMM.traceEdge...");

	int[] table = {
	    // 1234, 1=upper left pixel,  2=upper right, 3=lower left, 4=lower right
	    NA,                 // 0000, should never happen
	    RIGHT,              // 000X,
	    DOWN,               // 00X0
	    RIGHT,              // 00XX
	    UP,                 // 0X00
	    UP,                 // 0X0X
	    UP_OR_DOWN,         // 0XX0 Go up or down depending on current direction
	    UP,                 // 0XXX
	    LEFT,               // X000
	    LEFT_OR_RIGHT,      // X00X  Go left or right depending on current direction
	    DOWN,               // X0X0
	    RIGHT,              // X0XX
	    LEFT,               // XX00
	    LEFT,               // XX0X
	    DOWN,               // XXX0
	    NA,                 // XXXX Should never happen
	};
	int index;
	int newDirection;
	int x = xstart;
	int y = ystart;
	int direction = startingDirection;

	boolean UL = inside( x-1, y-1 );    // upper left
	boolean UR = inside( x, y-1 );      // upper right
	boolean LL = inside( x-1, y );      // lower left
	boolean LR = inside( x, y );        // lower right
	//xpoints[0] = x;
	//ypoints[0] = y;
	int count = 0;
	//IJ.log("");
	//IJ.log(count + " " + x + " " + y + " " + direction + " " + insideValue);
	do {
	    index = 0;
	    if (LR) index |= 1;
	    if (LL) index |= 2;
	    if (UR) index |= 4;
	    if (UL) index |= 8;
	    newDirection = table[index];
	    if (newDirection==UP_OR_DOWN) {
		if (direction==RIGHT)
		    newDirection = UP;
		else
		    newDirection = DOWN;
	    }
	    if (newDirection==LEFT_OR_RIGHT) {
		if (direction==UP)
		    newDirection = LEFT;
		else
		    newDirection = RIGHT;
	    }
	    if (newDirection!=direction) {
		xpoints[count] = x;
		ypoints[count] = y;
		count++;
		if (count==xpoints.length) {
		    int[] xtemp = new int[maxPoints*2];
		    int[] ytemp = new int[maxPoints*2];
		    System.arraycopy(xpoints, 0, xtemp, 0, maxPoints);
		    System.arraycopy(ypoints, 0, ytemp, 0, maxPoints);
		    xpoints = xtemp;
		    ypoints = ytemp;
		    maxPoints *= 2;
		}
		//if (count<10) IJ.log(count + " " + x + " " + y + " " + newDirection + " " + index);
	    }
	    switch (newDirection) {
	    case UP:
		y = y-1;
		LL = UL;
		LR = UR;
		UL = inside(x-1, y-1);
		UR = inside(x, y-1);
		break;
	    case DOWN:
		y = y + 1;
		UL = LL;
		UR = LR;
		LL = inside(x-1, y);
		LR = inside(x, y);
		break;
	    case LEFT:
		x = x-1;
		UR = UL;
		LR = LL;
		UL = inside(x-1, y-1);
		LL = inside(x-1, y);
		break;
	    case RIGHT:
		x = x + 1;
		UL = UR;
		LL = LR;
		UR = inside(x, y-1);
		LR = inside(x, y);
		break;
	    }
	    direction = newDirection;
	} while ((x!=xstart || y!=ystart || direction!=startingDirection));
	npoints = count;
    }
}

