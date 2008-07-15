/**Created on November 15, 2005
 * Modified on February 15, 2006, 3:29 PM
 *
 *The algorithm generates a Diffusion Limited Aggregate in steps,
 *where each interval is saved as an image in a stack.  The stack
 *can be analyzed in FracLac to view the change in the fractal dimension
 *over construction of a DLA.  This plugin was modified from
 *DLA_StackGenerator.java (a plugin for ImageJ written by A. Karperien
 *and improved by Wayne Rasband) as requested by T. Hamida of the
 *University of Alberta, Canada.  This code is in the public domain.
  * @author Audrey Karperien, Charles Sturt University
 */
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.*;
import java.util.Random;


/**
 *Draws diffusion limited aggregate models in ImageJ.
 * @author Audrey Karperien, Charles Sturt University
 */
public class DLA_Generator implements PlugIn {
        static final String[] shapes = {"Square",  "Round", "Triangular"};
        static int NumberOfIterations = 2000;
        static boolean showProgress = true;
        static boolean binary = false;
        static String shape = shapes[0];
        static int imageSize = 320;
        static int UserParticleSize = 1;        
        int movedistance=1;
        int padding=20;
        int sx = 250, sy=250;
        int arraysize;
        int render = 7;
        static int lastslice=5;
        double lim = 0;
        static boolean DoSquareDLA=false;
        dlaPt checkPoint, point;
        static boolean isCountAllDlapoints = false;
        static boolean OutlineDrawingElement = false;
        int CounterForNewSlice=1;
        String time;
    
 /**
  * Runs the plugin.
  * @param str string 
  */
 public void run (String str) {  } 
 
    /** Creates a new instance of NewClass */
    public DLA_Generator () {    
        ij.gui.GenericDialog gd = new GenericDialog("DLA Generator");
        gd.addNumericField("Number of Particles:", NumberOfIterations, 0);
        gd.addNumericField("Particle Size:", UserParticleSize, 0);
        gd.addNumericField("Image Size:", imageSize, 0);
        gd.addChoice("Particle Shape:", shapes, shape);
        gd.addCheckbox("Square DLA", true);
        gd.addCheckbox("Fill Particles", true);
        gd.addCheckbox("Watch Model Grow", showProgress);
        gd.addCheckbox("Binary image?", false);
        gd.addNumericField("Stacks?", lastslice, 0);
        gd.showDialog();
        if (gd.wasCanceled()) return;
        NumberOfIterations = (int)gd.getNextNumber();
        UserParticleSize = (int)gd.getNextNumber();
        imageSize = (int)gd.getNextNumber();
        arraysize = imageSize - 2*padding;
        int neededSize = (int)
        (180+0.047* NumberOfIterations-4.57e-7
                * NumberOfIterations*NumberOfIterations);
        if (arraysize<neededSize) arraysize = neededSize;
        imageSize = arraysize + 2*padding;
        shape = gd.getNextChoice();
        if (shape.equals("Square")) render=1;
        else if (shape.equals("Round")) render=2;
        else render=7;
        DoSquareDLA = gd.getNextBoolean();
        OutlineDrawingElement = !gd.getNextBoolean();
        showProgress = gd.getNextBoolean();
        binary = gd.getNextBoolean();
        lastslice=(int)gd.getNextNumber ();
        if (lastslice<=1)lastslice=1;
        CounterForNewSlice = (int)(NumberOfIterations/lastslice);
        sx=arraysize/2;
        sy=arraysize/2;
        lim=50;
        String title="Diffusion Limited Aggregate";
        BufferedImage B = 
                new BufferedImage (imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D)B.getGraphics ();
        ImagePlus img = null;
        //if the option to show progress is chosen, then an image is made ahead of time
        //so it can be drawn on in the makeDLAPoints function
        //otherwise, the image is made from null and drawn on after all the points are made
        if (showProgress||lastslice>1) {
            int[] pixels = ((DataBufferInt) B.getRaster().getDataBuffer()).getData();
            img = new ImagePlus (title, new ColorProcessor(imageSize, imageSize, pixels));
           if(showProgress)
            img.show();
           else img.updateImage ();
        }
        makeDLAPoints (img, g2, sx,  sy, render, movedistance, padding, arraysize);
        if (!showProgress)
            new ImagePlus (title, B).show(time);
    }
        
    
    /**
     * Main method in case user wants to double click. Starts ImageJ.
     * @param args String array
     */
    public static void main (String args[]) {
        new ImageJ ();
        new DLA_Generator();
        
    }
   
    /******************************************************************/
    /**
     * Calculates points of a growing diffusion limited aggregate.  Normally
     * run until a point for each point is calculated, 
     * but can be
     * set to count every point tried by setting countAllDlapoints
     * to true. 
     * 
     * @param Render integer specifying type of drawing for each particle
     * @param padding integer for extra space around structure
     * @param Arraysize for the array of points made
     * @param G Graphics2D for drawing on
     * @param sx double for origin's x coordinate
     * @param sy double for origin's y coordinate
     * @param movedistance int that specifies the
     * length of each particle
     */
    public void makeDLAPoints(
            ImagePlus img,
            java.awt.Graphics2D G, 
            double sx,
            double sy,
            int Render,
            int movedistance, 
            int padding,
            int Arraysize//should be made larger than the screen being written on
        ) {        
       
        Color temp = Color.blue;
        //Color newcol = Color.blue;
        //start a seed at the origin
        boolean stuck = false, donemoving = false;
        //Arraysize is the current image width
        //make an integer for the size of the array that
        //has to hold a pixel indicator for each pixel on
        //the screen plus a buffer zone
        int arraysize = Arraysize+padding+padding;
        int moves = 0;
        
        //make an array to store x, y coordinates
        
        //make an array to record each point in the screen
        //make each value in the array 0,
        //to mark that no point has been drawn
        //there yet
        //this array is bigger than the screen
        int [][] pts =   new int[arraysize][arraysize];
        for (int i = 0; i < pts.length; i++) {
            for (int j = 0; j< pts.length; j++) pts[i][j]=0;
        }
       
        int cx = (int)sx, cy = (int)sy; //the fixed centre x and y that never change
        if (((cx+padding)<0)||((cx+padding)>=arraysize))cx=(int)(arraysize/2);
        if (((cy+padding)<0) || ((cy+padding)>=arraysize))
            cy=(int)(arraysize/2);     
        //note the first point, the centre        
        pts[cx+padding][cy+padding]=1; 
        checkPoint=new dlaPt(cx, cy);
        point = new dlaPt(cx, cy);
        int ptsmade = 1;      
        //draw the origin point
        drawOrigin(Render, cx, cy, UserParticleSize, G);        
        //now release a particle
        boolean startagain = true;
        boolean changed = false;
        MovingParticle P=new MovingParticle(padding, arraysize, cx, cy);
        
        IJ.resetEscape();
        IJ.showStatus("Press Esc to abort");
        long start = System.currentTimeMillis();
        int count = 0;
        int inc = 60;//NumberOfIterations/100;
        if (inc<1) inc = 1;
        int thisslice = 1;

        do {
           
            if (startagain) {
                // reset the startagain thing                
                // make a screen point on the circle bounding all
                // the points so far then made larger 
               if (DoSquareDLA)
                   point = PointOnBiggerSquare(cx, cy, checkPoint);
                // let this point be the origin of a new particle
               else 
                  point = PointOnBiggerCircle(cx, cy, checkPoint);    
               P.makeNewParticle(point);  
               startagain=false;
            }
            
            // each time through the loop, start a new particle
            // if the old one was done, and always check to
            // see if the particle considered sticks to anything
            if (P.SticksTo(pts)) {
                 moves = 0;  
                 if (binary)G.setColor(Color.white);
                 else if (//if at one of 4 times in the drawing
                (ptsmade ==(int)(NumberOfIterations/5)*4)
                || (ptsmade ==(int)(NumberOfIterations/5)*3)
                || (ptsmade ==(int)(NumberOfIterations/5)*2)
                || (ptsmade ==(int)(NumberOfIterations/5)))  {
                    temp = randomColours();
                    G.setColor(temp);
                    changed=true;
                }
                
                int ParticleSize=(int)UserParticleSize;
                if (ParticleSize<=0) ParticleSize=1;
                //IJ.log((count++)+ "  "+ParticleSize+"  "+Render);
                switch (Render) {
                    case 1:
                        if (OutlineDrawingElement)
                            G.drawRect(P.x, P.y, ParticleSize, ParticleSize);   
                        else
                            G.fillRect(P.x, P.y, ParticleSize, ParticleSize);
                        break;
                    case 7:
                        if (ParticleSize<2) ParticleSize=2;
                        int [] xpoints = {P.x, P.x+ParticleSize, P.x+(ParticleSize/2)};
                        int []ypoints = {P.y, P.y, P.y+(int)(Math.sqrt(3.0)*(ParticleSize/2.0))};
                        if (OutlineDrawingElement)
                            G.drawPolygon(xpoints, ypoints, 3);
                        else
                            G.fillPolygon(xpoints, ypoints, 3);
                        break;
                    default:
                        if (OutlineDrawingElement)
                            G.drawOval(P.x, P.y,ParticleSize, ParticleSize); 
                        else
                            G.fillOval(P.x, P.y, ParticleSize, ParticleSize);
                    } // switch

                    //record a point in the array as drawn
                    pts[P.x+padding][P.y+padding]=1;
                    
                    //count a point and record the x and y of the point
                    if (isCountAllDlapoints==false)ptsmade++;
                    checkPoint=new dlaPt (P.x, P.y);  
                    
                    //notify program to make a new point
                    startagain = true;
                    if (((ptsmade%inc)==0)||ptsmade%CounterForNewSlice==0) {
                        IJ.showProgress(ptsmade, NumberOfIterations);
                        if (img!=null) img.updateAndDraw();        
                        if((ptsmade%CounterForNewSlice==0)&&lastslice>1)
                        {makeStacks (img, thisslice); thisslice++;}
                        if (IJ.escapePressed()) ptsmade=NumberOfIterations;
                    }
            } // end what to do if it sticks 
            else {
                // else if it does not stick, move it 
                donemoving = P.moveParticle(movedistance); 
                moves++;
            }
            //if the move goes out of bounds, start over with
            //a new point, but if not just check the new position
            //at the top of the loop again
            if (donemoving) startagain=true; //||//(P.outBounds(bounds)))
            if (((P.x+padding)>pts[0].length)||((P.y+padding)>pts[1].length)) startagain=true;
            if (((P.x+padding)<0)||((P.y+padding)<0)) startagain=true;
            //if this option is turned on, count all points even ones that do not stick
            if (isCountAllDlapoints) ptsmade++;
            //count a point even if it is rejected if this option is chosen
        } while (ptsmade<=NumberOfIterations);
        if (img!=null) img.updateAndDraw();
        time = (System.currentTimeMillis()-start)/1000+" seconds";
        IJ.showStatus(time);
        IJ.showProgress(1.0);
    }
    
    ImageStack TheStack;
    
    void makeStacks(ImagePlus img, int thisslice)
    {
        String title = "DLA image";
          // img.setSlice(thisslice);
           img.updateImage ();
          // img.updateAndRepaintWindow();
           BufferedImage bi
                    = new BufferedImage(img.getWidth(),
                    img.getHeight(), BufferedImage.TYPE_INT_ARGB);
           Graphics2D g = (Graphics2D)bi.getGraphics();
           g.drawImage(img.getImage (), 0, 0, null);            
            ImagePlus imgg = new ImagePlus("new", bi);
            
            if(thisslice==1) 
            {
                TheStack = new ImageStack(img.getWidth(), img.getHeight());
                TheStack.addSlice(title, imgg.getProcessor());                
            }
            else TheStack.addSlice(title, imgg.getProcessor());
            if(thisslice==lastslice)
            {
                StackWindow iww = new StackWindow(new ImagePlus(img.getTitle()
                        + thisslice, TheStack));
                iww.setVisible(true);
            }
    }
    
    void drawOrigin(
    int Render,   
    int cx, int cy, int ParticleSize, 
    java.awt.Graphics2D G) {
        
        origin: switch(Render) {
            case 1:
            {
                if(OutlineDrawingElement)G.drawRect(cx, cy, ParticleSize, ParticleSize);
                else    G.fillRect(cx, cy, ParticleSize,ParticleSize);
                    break;
            }
            
            case 7:
            {if ((ParticleSize)<2)ParticleSize=2;
                int [] xpoints =
                        {
                            cx,
                         cx+ParticleSize,
                         cx+(ParticleSize/2)};
                         int []ypoints =
                         {cy, cy,
                          cy+(int)(Math.sqrt(3.0)
                          *(ParticleSize/2.0))};
                          
                          
                          
                        if (OutlineDrawingElement)G.drawPolygon(xpoints, ypoints, 3);
                        else G.fillPolygon(xpoints, ypoints, 3);
                
                          break;
            }
            default: 
            {
                if (OutlineDrawingElement)
                {
                            G.drawOval(cx, cy, ParticleSize,ParticleSize);

                }
                else
                {
                        G.fillOval(cx, cy, ParticleSize,ParticleSize);

                }
                break;
        
                
            }
        }
        }
    Random rnd = new Random(System.currentTimeMillis());
    boolean didfirst=false;
    float myradius;
    /**
     * Gets the last point added, to compare to the saved greatest
     *     radius from the centre.  If its distance is greater than the saved
     *     radius then a new radius is stored.  The radius is for the circle
     *     defining where points are dropped from. 
     *     Starts incoming particles on a circle centred on the origin
     * with radius = the distance of the farthest stuck pixel from
     * the origin.  Up to the edge of the field the DLA is 
     * drawn in (plus a padding of 200 pixels), with each farther point, 
     * the circle's radius is
     * increased by the amount specified
     * 
     * @param cex x coordinate for origin
     * @param cey y coordinate for origin
     * @param checkpt point to check
     * @return dlaPt new point
     */ 
    public dlaPt PointOnBiggerCircle(double cex, double cey, dlaPt checkpt)
    {//takes an origin and a bunch of points
        //finds the point that is furthest from the origin
        //defines that length + a bit more as a circle's radius
        
       
       float newradius=0f;
        
        //test all points so far to find the 
        //farthest away distance
        //
       
       if (didfirst==false){didfirst=true; myradius=4.1f;}
       else
       {
           {
               newradius = (float)Math.max(
        Math.abs(
        (
        ((float)java.awt.geom.Point2D.distance((float)cex, (float)cey, 
        (float)checkpt.x, (float)checkpt.y)))
        +lim
        ), 
        myradius);
       }
       }    
        //distance from origin to greatest distance + border
       if (newradius>myradius) myradius=newradius;// + lim;
        //let the circle be at 0,0, then find a random x and its y
        //using the radius i.e. y = sqrt(rsq-xsqr)
        //then move it to one of the four quadrants and its centre
        //make a new x somewhere right or left from the origin
        double coord = (double)(rnd.nextInt((int)myradius+1));//FIXME
        //don't lose the decimal part of x
       coord=(coord+rnd.nextDouble());
       double circlestuff=0;
       if(coord>=myradius)
       {
           coord=myradius;
           
       }
       
        //make the y coordinate for the arc in this quadrant
       else circlestuff = Math.sqrt((myradius*myradius)-(coord*coord));//FIXME
        //now translate x and y
        double F=1.0, G=1.0;
        double D= Math.abs(rnd.nextDouble());
        if(D<=0.25);
        else if (D<=0.50){F=-1;G=-1;}
        else if (D<=0.75){F=-1; G=1;}
        else if (D<=1.0){F=1.0; G=-1.0;}
        double xx=cex+(F*coord);
        double yy = cey-(G*circlestuff);
        if(rnd.nextBoolean()==false)
        {
            xx=cex+(F*circlestuff);
            yy=cey-(G*coord);
        }
        return new dlaPt(xx, yy);        
        
    }////////////////end////////////////////////////////
     /***********************************************************/
    /////////////////////////////////////////////////////////////
    /** Returns a random Color instance using Random().nextInt(256)
     * for red, green, and blue integer values of a
     * new Color(red, green, blue).
     * @see java.util.Random
     * @see java.awt.Color
     * @return a colour
     */
    public static java.awt.Color randomColours()
    {
        java.util.Random rnd = new java.util.Random();
        return new java.awt.Color(
        rnd.nextInt(256),
        rnd.nextInt(256),
        rnd.nextInt(256));
    }//////END/////////
    //////////////////////////////////////////////////////////////
     /**
     * Starts incoming particles on a square centred on the origin
     * with radius = the distance of the farthest stuck pixel from
     * the origin.  Up to the size of the field the DLA is 
     * being drawn on (plus a padding), this box
     * is increased by the amount specified
     * in {@link #lim}
     * each time a further away point attaches.
     * 
     * @param cex centre x coordinate
     * @param cey centre y coordinate
     * @param checkpt point to check
     * @return dlaPt new point
     */ 
    public dlaPt PointOnBiggerSquare(double cex, double cey, dlaPt checkpt)
    {//takes an origin and a bunch of points
        //finds the point that is furthest from the origin
        //defines that length + a bit more as a circle's radius
        
        double F = 1.0;
       float newradius=0.0f;
        Random RND = new Random(System.currentTimeMillis());
        if (RND.nextDouble()<=.5)F=-1.0;
        //test all points so far to find the 
        //farthest away distance
       if (didfirst==false){didfirst=true; myradius=4.0f;}
       else
       {
           if (((myradius+cex)>arraysize)
           ||((myradius+cey)>arraysize)
          ||((cex-myradius<(-1*padding))
          ||(cey-myradius)<(-1*padding)));
        ////System.out.println("myradius in " + myradius);   
           else //find the greater of the x or y distance from the last
               //point to the centre
               //if this is greater than the stored value, increase the limit
           {
               newradius =
                       (float)Math.max(Math.abs(cex-checkpt.x), Math.abs(cey-checkpt.y));
           }  
       }   
        
        //distance from origin to greatest distance + border
       if (newradius>myradius) myradius=(float)(newradius + lim);
       int K=-1;
       if(rnd.nextBoolean())K=1;
       double xx = cex+(myradius*K);
       double yy = cey+(double)(rnd.nextInt((int)myradius+1));
       if (RND.nextBoolean()==false)
       {//half the time use the x 
           yy=cey+(myradius*K);
           K=-1;
           if(rnd.nextBoolean())K=1;
           xx=cex+((double)(rnd.nextInt((int)myradius+1))*K);
       }
       else
       {//the rest of the time use the y
           xx=cex+(myradius*K);
           K=-1; if (rnd.nextBoolean())K=1;
           yy=cey+((double)(rnd.nextInt((int)myradius+1))*K);
       }
        return new dlaPt(xx, yy);
    }
    
}
/**Methods manipulate and move DLAGenerator particles.
  */
 class MovingParticle
{
    
    int x = 0;
    int y = 0;
    int cex = 0;
    int cey = 0; 
    int bigarraysize;
    int padding;
    /** Constructor sets the boundary of the aggregate. Calls
     * {@link #makeNewParticle makeNewParticle}.
     * @param Padding
     * @param Bigarraysize
     * @param ceXx origin's x coordinate
     * @param ceYy origin's y coordinate
     */
    public MovingParticle(int Padding, int Bigarraysize, int ceXx, int ceYy) 
    {//constructor to make a new Particle object
        cex = ceXx;
        cey = ceYy;
        bigarraysize = Bigarraysize;
        padding=Padding;
        //makeNewParticle(bigarraysize, cex, cey, padding);
    }
        

    /**
     * @param point
     */    
    public void makeNewParticle(dlaPt point) {
        x=(int)point.x;
        y=(int)point.y;
        //if (x>(size-2)) x = (size-2);
        //if (y>(size-2)) y = (size-2);
        //if (x<1) x = 1;if (y<1)y=1;
    }
    
    /**Checks if a point is in the 3x3 array surrounding
     * an already "stuck" pixel. Array of passed
     * points must be of size x or y+1; x and y cannot be
     * less than 0, either.
     * @param pnts int array of x and y coordinates for
     * structure up to this point including all possible
     * pixel locations and whether or not they are filled.
     * @return boolean true if it is near a drawn point*/
    public boolean SticksTo(int[][] pnts) {
        int X=x+padding;
        int Y = y+padding;
        boolean itSticks = false;
        
        if (X>=pnts.length-2||Y>=pnts.length-2||X<1||Y<1)
            ; 
        else {
            //code to see if it sticks or not
            if (pnts[X+1][Y] ==1) itSticks = true;
            if (pnts[X+1][Y+1] ==1) itSticks = true;
            if (pnts[X+1][Y-1] ==1) itSticks = true;
            if (pnts[X][Y+1] ==1) itSticks = true;
            if (pnts[X][Y-1] ==1) itSticks = true;
            if (pnts[X-1][Y] ==1) itSticks = true;
            if (pnts[X-1][Y-1] ==1) itSticks = true;
            if (pnts[X-1][Y+1] ==1) itSticks = true;
            if (pnts[X][Y]==1) itSticks=false;
        }
        return itSticks;
    }
    
     java.util.Random jrnd = new java.util.Random(System.currentTimeMillis());
     
    /** Moves particle distance ad randomly.  Returns
     * true if the particle is at the edge and can no longer be stuck to
     * equally in all directions.
     * @param ad int specifies distance of each move
     * @return boolean for whether or not the point is out*/
    public boolean moveParticle(int ad) {
        boolean done = false;
       
       //code to move particle randomly one step from its current position
       // double Mdom = Math.abs(Math.random());
        double Mdom = jrnd.nextDouble();
        ad=Math.abs(ad);
        if (ad<1) ad=1;
        //randomly choose one of the 8 neighbouring pixels from the centre
        //point then\
        ifloop:
       if (true) {
           if (Mdom<=.125) {x=(x-ad); y=(y-ad); break ifloop;}
           else  if (Mdom<=.250){x=(x-ad);break ifloop;}
           else  if (Mdom<=.375){y=(y+ad);break ifloop;}
           else  if (Mdom<=.500){y=(y-ad);break ifloop;}
           else  if (Mdom<=.625){x=(x+ad); y=(y+ad);break ifloop;}
           else  if (Mdom<=.750){x=(x-ad); y=(y+ad);break ifloop;}
           else  if (Mdom<=.875){x=(x+ad); y=(y-ad);break ifloop;}
           else  if (Mdom<=1.00){x=(x+ad);break ifloop;}
       }
       return done;
    }
    
    /** Moves particle distance ad randomly.  Returns
     * true if the particle is at the edge and can no longer be stuck to
     * equally in all directions.
     * @param ad int specifies distance of each move
     * @return boolean for whether or not the point is out*/
    boolean outBounds(int bounds) {
        boolean isOut = false;        
        
        if (x>=(bigarraysize-padding)-2) isOut=true;
        if (y>=(bigarraysize-padding)-2) isOut=true;
        if (x<=(-1*padding)) isOut=true;//x = 0;
        if (y<=(-1*padding)) isOut=true;//y = 0;
        return isOut;
    }
    
}
/**Class specifies a point (i.e., two coordinates).
 *  @author  Audrey Karperien (c) 2003 Audrey Karperien
  * @since J2SDK 1.4.1_02
 */
 class dlaPt {
    /** x coordinate of point */
    public float x=0;
    /** Y coordinate of point*/ 
    public float y=0;
    
    /** Constructor takes floats for x and y respectively.
     * @param x float coordinate of point
     * @param y float coordinate of point
     */    
    public dlaPt(float x, float y) {
        this.x = x;
        this.y = y;    
    }
    
    /** Constructor converts <CODE>integers</CODE> 
     * to <CODE>floats</CODE> for x and y.
     * @param x integer for x coordinate
     * @param y integer for y coordinate
     */    
    public dlaPt(int x, int y) {
        this.x = (float)x;
        this.y = (float)y;        
    }
    
    /** Constructor converts doubles to floats for x and y.
     * @param x double for x coordinate
     * @param y double for y coordinate
     */    
    public dlaPt(double x, double y) {
        this.x = (float)x;
        this.y = (float)y;        
    }
        
    /**
     * Calculates slope between this dlaPt and passed dlaPt.
     * Error if this dlaPt is not initialized.
     * 
     * @param p2
     * @return double for slope
     */    
    public double slope(dlaPt p2) {
        return (p2.y-y)/(p2.x-x);
    }
    
   
}
