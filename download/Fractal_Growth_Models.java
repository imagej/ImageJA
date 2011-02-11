 /* This plugin draws iterated growth models in ImageJ.
 *
 * Points for each model are calculated using the same
 * basic expressions: <BR>
 * <B>x<sub>n+1</sub> = mx<sub>n</sub><sup>xpow</sup>
 * + ny<sub>n</sub><sup>ypow</sup> + o</B> <BR>and<BR>
 * <B>y<sub>n+1</sub> = fx<sub>n</sub><sup>xpow</sup> +
 * gy<sub>n</sub><sup>ypow</sup> + h</B>.
 * <p>
 * Each equation's coefficients (m,n,o;f,g,h; xpow, ypow) are
 * specified in a set that also contains a probability of using the
 * set.  The plugin shows default models specified by sets of
 * coefficients and probabilities of using each set,
 * but these can also be modified by
 * the user.  Currently, colours are generated randomly, without user input.
 * The user can change the size of the particles
 * in the models, however, using an option in the first popup.</p>
 * <p>After the user
 * makes basic selections of model type and size, a
 * popup appears for the number of sets of coefficients to use.
 * The user can make sets of variables for both equations
 * and specifies the likelihood of using each set
 * using popups that appear automatically.
 * The number and content of sets can be modified by
 * changing the value for Number of Sets and/or the coefficients.
 * After the number of sets is selected, another popup
 * comes up with each coefficient's
 * name listed beside the textbox for its
 * value.  At the bottom of each popup, there is a space for
 * the probability that the coefficients above it
 * will be used in generating the next point.  The total of
 * all probabilities should not be greater than 1; all sets for values
 *that make the cumulative total greater than 1 are ignored.
 * </p>
 * The "Henon" and "Henon Map" models are specified by one set of coefficients.
 * The Henon Map has a (capacity) fractal dimension that is
 * assumed to be around 1.261 (correlation dimension around 1.25).
 * The "Random" model is similar to the Henon models, but uses
 * two sets of coefficients and probabilities.
 * The "D. Greene Fern" model is from an algorithm my
 * David Greene (Charles Sturt University), and the custom model is
 * essentially the same with a few changes in coefficients and probabilities.
 *
 * Updated February 2011 - increased image size, added incremental stacks, and
 * fixed bug so filled and outlined rectangles work
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


public class Fractal_Growth_Models implements PlugIn {
    static int NumberOfIterations = 90000;
    static int NumberOfSetsToUse=1;
    static int PatternType =1;
    int inc = 100;
    ImagePlus Bigimg = null;
    String [] PatternTypeArray = {"Henon Multifractal",
                    "D. Greene Fern", "Random Spiral",
                        "Henon Multifractal Map", "Custom"};

            boolean Fern=false;
            boolean Henon=false,Custom=false, Spiral=false, Map=false;
            static boolean showProgress=true,UseOval=false, FillIt=false,
                    UseSingleColour=true;
            static int imageSize = 900;
            static int UserParticleSize = 1;
            static int lastslice = 35, CounterForNewSlice=1;
            double [] m={-0.2f,0,0,0};
            double[] n={1f,0,0,0};
            double[] o={1f,0,0,0};
            double[] f={0.999f,0,0,0};
            double[] g={0f,0,0,0};
            double []h={0f,0,0,0};
            double[] xxpower={2f, 1, 1, 1};
            double []xypower={1f, 1, 1, 1};
            double []yypower={1f, 1, 1, 1};
            double[] yxpower={1f, 1, 1, 1};
            double probabilities[] = {1, 0, 0,0};
            double centrex=imageSize/2;//300;
            double centrey=imageSize/2;//300;
            int grow=90;
            int over=0, up=0;
            static java.awt.Color nc=Color.ORANGE;        
            String time;
    
 /**
  * Runs the plugin.
  * @param str string 
  */
 public void run (String str) {  } 
 
   /** Creates a new instance of Fractal_Growth_Models and
             * gets user inputs for drawing a model, then selects the
             * corresponding variables, and draws the model
             * unless cancelled. Calls itself again if not cancelled.*/
    public Fractal_Growth_Models () {
        boolean okd = getInputs();
        if (!okd)return;
        //uses the dialogue inputs just obtained to make a title string
        //for the type of fractal that is being generated
        String title=PatternTypeArray[PatternType];
        //makes the image that will be used
        BufferedImage B =
                new BufferedImage
                (imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D)B.getGraphics ();
        ImagePlus img = null;
        //sets the colour that will be used for foreground using a colour 
        //chooser
        if(UseSingleColour)g2.setColor(setFGColour());
        //if the option to show progress or make a stack is chosen,
        //then an image is made ahead of time
        //so it can be drawn on 
        //otherwise, the image is made from null and
        //drawn on after all the points are made
        if (showProgress||lastslice>1)
        {
            int[] pixels =
                    ((DataBufferInt) B.getRaster().getDataBuffer()).getData();
            img = new ImagePlus
                    (title, new ColorProcessor(imageSize, imageSize, pixels));
           if(showProgress)  img.show();
           else img.updateImage ();
        }
        //calls a function to calculate the points and draw them
        makeFGPoints(img, g2);
        //shows the image if option is selected
        if (!showProgress) new ImagePlus (title, B).show(time);
        //keep on making fractals if you want to; calls itself again
        //if you didn't cancel the last one...fixme
        if (okd)
        {
            new Fractal_Growth_Models();
        }
    }
    /**Gets user inputs for the fractal and how it will be displayed*/
    boolean getInputs()
    {
        GenericDialog gd = new GenericDialog ("Iterator");
        gd.addNumericField ("Number of Particles:", NumberOfIterations, 0);
        gd.addNumericField ("Size of Particles:", UserParticleSize, 0);
        gd.addCheckbox ("Fill?", FillIt);
        gd.addCheckbox ("Oval (uncheck for rectangle)?", UseOval);
        gd.addCheckbox ("Single colour (uncheck for multi)?", UseSingleColour);
        gd.addChoice ("Select type", PatternTypeArray,
                PatternTypeArray[PatternType]);
        gd.addCheckbox ("Show Progress?", showProgress);
        gd.addNumericField ("Slices?", lastslice, 0);
        gd.showDialog ();
        //if the input dialog was cancelled, returns false, otherwise goes on
        if (gd.wasCanceled ()) return false;
        //get all the values the user selected
        NumberOfIterations = (int)gd.getNextNumber ();
        UserParticleSize=(int)gd.getNextNumber ();
        if(UserParticleSize<1){UserParticleSize=1;}
        FillIt=gd.getNextBoolean ();
        UseOval=gd.getNextBoolean ();
        if(UseOval&&(UserParticleSize<2)){UseOval=false;}
        UseSingleColour=gd.getNextBoolean ();
        PatternType=gd.getNextChoiceIndex ();
        showProgress = gd.getNextBoolean();
        lastslice=(int)gd.getNextNumber ();
            if (lastslice<1)lastslice=1;
            CounterForNewSlice = (int)(NumberOfIterations/lastslice);
        //get user inputs for how many arrays to use when calculating
        //the fractal model; brings up a new dialog with defaults that
        //reflect the fractal model chosen
        NumberOfSetsToUse =
            (int)IJ.getNumber ("How many probability sets?",
            (PatternType==4||PatternType==1)?4:
            PatternType==2?2:1);
        if (NumberOfSetsToUse==IJ.CANCELED)return false;
        //if it wasn't cancelled, set variables for the pattern chosen
        if (PatternType==4)
        {Custom=true; MakeCustomArrays (NumberOfSetsToUse);}
        if (PatternType==1)
        {Fern=true; MakeFern (NumberOfSetsToUse); }
        if (PatternType==2)
        {Spiral=true;MakeSpiral (NumberOfSetsToUse);}
        if (PatternType==3)
        {Map=true; MakeHenonMap (NumberOfSetsToUse);}
        if (PatternType==0)
        {Henon=true; MakeHenon (NumberOfSetsToUse);}
        //get values for the each array to be
        //used to calculate the fractal model selected
        //These are preset to generate the standard models but the user
        //can change them to explore different effects on fractal growth
        for (int i = 0; i <NumberOfSetsToUse; i++)
        {
            GenericDialog gt=new GenericDialog ("Probabilities");
            gt.addNumericField ("M", m[i], 4);
            gt.addNumericField ("N", n[i], 4);
            gt.addNumericField ("O", o[i], 4);
            gt.addNumericField ("F", f[i], 4);
            gt.addNumericField ("G", g[i], 4);
            gt.addNumericField ("H", h[i], 4);
            gt.addNumericField ("Xx power", xxpower[i], 4);
            gt.addNumericField ("Xy power", xypower[i], 4);
            gt.addNumericField ("Yx power", yxpower[i], 4);
            gt.addNumericField ("Yy power", yypower[i], 4);
            gt.addNumericField
            ("Probability of Using this Set",
            probabilities[i], 5);

            gt.showDialog ();
            if (gt.wasCanceled ()) return false;

            m[i]=(float)gt.getNextNumber ();
            n[i]=(float)gt.getNextNumber ();
            o[i]=(float)gt.getNextNumber ();
            f[i]=(float)gt.getNextNumber ();
            g[i]=(float)gt.getNextNumber ();
            h[i]=(float)gt.getNextNumber ();

            xxpower[i]=(float)gt.getNextNumber ();
            xypower[i]=(float)gt.getNextNumber ();
            yxpower[i]=(float)gt.getNextNumber ();
            yypower[i]=(float)gt.getNextNumber ();

            probabilities[i]=(float)gt.getNextNumber ();
            if (gt.wasCanceled ()) return false;
        }
        //if it hasn't been cancelled at this point, return true
        return true;
    }
    /**
     * Main method in case user wants to double click. Starts ImageJ.
     * @param args String array
     */
    public static void main (String args[]) {
        new ImageJ ();
        new Fractal_Growth_Models();
        
    }
    /**Method for setting the foreground colour for the fractal model
     * being generated. If the option has been set to use a single colour
     * rather than multiple colours a color chooser appears.
     * Each option reveals different features of the same pattern.
     */
    public Color setFGColour()
    {
        if (UseSingleColour)
        {
            ij.gui.ColorChooser cc =
                        new ij.gui.ColorChooser("Select a Colour", nc, false);
            nc=cc.getColor();
            return (nc);
        }
        else return(randomColours());

    }
    /******************************************************************/
/** Calculates {@link #NumberOfIterations a specified number} of
* points using two equations for x, and y.
* The points are made using <BR>
*      <B>x<sub>n+1</sub> = mx<sub>n</sub><sup>xpow</sup>
* + ny<sub>n</sub><sup>ypow</sup> + o</B> <BR>and
* <B>y<sub>n+1</sub> = fx<sub>n</sub><sup>xpow</sup> +
* gy<sub>n</sub><sup>ypow</sup> + h</B>.
* @param G2 Graphics2D on which to draw
*/
    public void makeFGPoints(
            ImagePlus imgToDrawPointsOn,
            java.awt.Graphics2D G2
        ) {        
            
        IJ.resetEscape();
        IJ.showStatus("Press Esc to abort");
        long start = System.currentTimeMillis();  
        int thisslice = 1;//this is for tracking which slice is being made
        int ptsmade=1;//for counting points made so far out of total desired
        /*array for storing coordinates for points to be drawn*/
        double xy[][] = new double[2] [NumberOfIterations];
        //initialize the array of points' first two boxes
        xy[0][0]=0;
        xy[1][0]=0;
        //start a loop that iterates at least once until ptsmade reaches 
        //the maximum desired or the value is reset within by an escape
        do {
            //call a function to set the colour if the option for single
            //colour was selected
            if(!UseSingleColour)G2.setColor(randomColours());
            //get a value specifying which entry in the array of probabilities
            //will be used for the next point, and ensure it will be in bounds
            int j=CumulativeProb ();
            while (j>=NumberOfIterations){j=CumulativeProb();}
            if (NumberOfSetsToUse==1)j=0;
            //fill the array of x,y coordinates for points to draw using
            //the formular outlined in the intro text
            xy[0][ptsmade]= m[j]*Math.pow(xy[0][ptsmade-1],xxpower[j])
                    +n[j]*Math.pow (xy[1][ptsmade-1],xypower[j])+o[j];
            xy[1][ptsmade]=f[j]*Math.pow (xy[0][ptsmade-1], yxpower[j])
                +g[j]*Math.pow (xy[1][ptsmade-1], yypower[j])+h[j];
            //make a double value for the x and y
            double x=xy[0][ptsmade], y = xy[1][ptsmade];
            //need a correction if making a Henon multifractal
            if (ptsmade-1==1&& Henon)
            {
                over=(int)(centrex -(int)grow*x)+60;
                up=(int)(centrey-(int)grow*y)+60;
            }
            //finally, calculate x,y ints for coordinates, corrected for image
            //size and position
            int X= (int)( over+grow*x);
            int Y=(int)(up+grow*y);
            //Draw the point using the user variables selected in the getinputs
            //dialog in the constructor
            if((UseOval&&FillIt))
            {G2.fillOval (X, Y, UserParticleSize, UserParticleSize);}
            else if((UseOval&&!FillIt))
            {G2.drawOval (X, Y, UserParticleSize, UserParticleSize);}
            else if((!UseOval&&!FillIt))
            {G2.drawRect (X, Y, UserParticleSize, UserParticleSize);}
            else if((!UseOval&&FillIt))
            {G2.fillRect (X, Y, UserParticleSize, UserParticleSize);}
            //if it is time to make a layer for the stack or to show how
            //the model has grown to this point, update the image and
            //call the makeStacks function.
            //It is time if we are at an even interval of points to make the
            //specified number of stack layers, or we are past the last even
            //interval and have the final points to display
            if (ptsmade%CounterForNewSlice==0||(ptsmade==NumberOfIterations-1))
            {
                IJ.showProgress(ptsmade, NumberOfIterations);
                if (imgToDrawPointsOn!=null) imgToDrawPointsOn.updateAndDraw();
                if(lastslice>1&&thisslice<=lastslice)
                {
                    makeStacks(imgToDrawPointsOn, thisslice); thisslice++;
                }
                if (IJ.escapePressed())
                {
                    ptsmade=NumberOfIterations;
                    makeStacks(imgToDrawPointsOn, lastslice);
                }
            }
        //increase the point making counter for the next iteration
        ptsmade++;
        } while (ptsmade<NumberOfIterations);
        //end the do loop if the point making counter is at the maximum
        if (imgToDrawPointsOn!=null) imgToDrawPointsOn.updateAndDraw();
        time = (System.currentTimeMillis()-start)/1000+" seconds";
        IJ.showStatus(time);
        IJ.showProgress(1.0);
    }
    
    ImageStack TheStack;
    /**This makes a stack from the layers made in makeFGpts*/
    void makeStacks(ImagePlus stackdrawing, int thisslice)
    {
        String title = PatternTypeArray[PatternType];
          // stackdrawing.setSlice(thisslice);
           stackdrawing.updateImage ();
          // stackdrawing.updateAndRepaintWindow();
           BufferedImage bi
                    = new BufferedImage(stackdrawing.getWidth(),
                    stackdrawing.getHeight(), BufferedImage.TYPE_INT_ARGB);
           Graphics2D gg = (Graphics2D)bi.getGraphics();
           gg.drawImage(stackdrawing.getImage (), 0, 0, null);
            ImagePlus imgg = new ImagePlus("new", bi);

            if(thisslice==1)
            {
                TheStack = new ImageStack(stackdrawing.getWidth(),
                        stackdrawing.getHeight());
                TheStack.addSlice(title, imgg.getProcessor());
            }
            else TheStack.addSlice(title, imgg.getProcessor());
            if(thisslice==lastslice)
            {
                StackWindow iww = new StackWindow(new ImagePlus
                        (stackdrawing.getTitle()+ thisslice, TheStack));
                iww.setVisible(true);
            }
    }
    /**This is for setting up the arrays that will be used for each type
     * of fractal model.
     * @param N
     */
    void MakeCustomArrays (int N)
            {
                over=300;
                up=50;
                centrey=centrey-200;
                InitializeArrays (N);
                MatrixVariables M = new MatrixVariables ();
                probabilities= new double[N];
                for (int i = 0; i < N; i++)
                {
                    if(i<M.FERNRS.length)
                    {
                        m[i]=M.FERNMS[i];
                        n[i]=M.FERNNS[i];
                        o[i]=M.FERNOS[i];
                        g[i]=M.FERNGS[i];
                        h[i]=M.FERNHS[i];
                        f[i]=M.FERNFS[i];
                        xxpower[i]=1;
                        xypower[i]=1;
                        yypower[i]=1;
                        yxpower[i]=1;
                        probabilities[i]=M.FERNRS[i];

                    }
                    else
                    {
                        m[i]=.1;
                        n[i]=.81;
                        o[i]=.9;
                        f[i]=.2;
                        g[i]=.1;
                        h[i]=.01;
                        xxpower[i]=1;
                        xypower[i]=1;
                        yypower[i]=1;
                        yxpower[i]=1;
                        probabilities[i]=(1f/(double)N)*(i+1);}
                }

            }
    /**For making the Henon Eyes Multifractal*/
            void MakeHenon (int N)
            {
                InitializeArrays (N);
                MatrixVariables M = new MatrixVariables ();
                probabilities= new double[N];
                for (int i = 0; i < N; i++)
                {
                    if(i<M.HENONRS.length)
                    {
                        m[i]=M.HENONMS[i];
                        n[i]=M.HENONNS[i];
                        o[i]=M.HENONOS[i];
                        g[i]=M.HENONGS[i];
                        h[i]=M.HENONHS[i];
                        f[i]=M.HENONFS[i];
                        xxpower[i]=M.HENONXforXS[i];
                        xypower[i]=M.HENONrestofexponentS[i];
                        yypower[i]=M.HENONrestofexponentS[i];
                        yxpower[i]=M.HENONrestofexponentS[i];
                        probabilities[i]=M.HENONRS[i];

                    }
                    else
                    {
                        m[i]=.1;
                        n[i]=.81;
                        o[i]=.9;
                        f[i]=.2;
                        g[i]=.1;
                        h[i]=.01;
                        xxpower[i]=1;
                        xypower[i]=1;
                        yypower[i]=1;
                        yxpower[i]=1;
                        probabilities[i]=(1f/(double)N);}
                }

            }
            /**Make variables for the D. Greene fern model*/
            void MakeFern (int N)
            {
                MatrixVariables M = new MatrixVariables ();
                centrey=centrey-200;
                over=400;//300;
                grow=80;//50;
                up=20;//50;
                InitializeArrays (N);
                for (int i = 0; i < N; i++)
                {
                    if(i<M.GreeneFERNRS.length)
                    {
                        m[i]=M.GreeneFERNMS[i];
                        n[i]=M.GreeneFERNNS[i];
                        o[i]=M.GreeneFERNOS[i];
                        g[i]=M.GreeneFERNGS[i];
                        h[i]=M.GreeneFERNHS[i];
                        f[i]=M.GreeneFERNFS[i];
                        xxpower[i]=1;
                        xypower[i]=1;
                        yypower[i]=1;
                        yxpower[i]=1;
                        probabilities[i]=M.GreeneFERNRS[i];

                    }
                    else
                    {
                        m[i]=.1;
                        n[i]=.81;
                        o[i]=.9;
                        f[i]=.2;
                        g[i]=.1;
                        h[i]=.01;
                        xxpower[i]=1;
                        xypower[i]=1;
                        yypower[i]=1;
                        yxpower[i]=1;
                        probabilities[i]=(1f/(double)N);}
                }
            }
/**Make variables for the random spiral model; multifractal*/
            void MakeSpiral (int N)
            {
                MatrixVariables M = new MatrixVariables ();
                InitializeArrays (N);
                grow=180;
                over=380;
                up=540;
                for (int i = 0; i < N; i++)
                {
                    if(i<M.RandomSpiralHenonrs.length)
                    {
                        m[i]=M.RandomSpiralHenonMs[i];
                        n[i]=M.RandomSpiralHenonNs[i];
                        o[i]=M.RandomSpiralHenonOs[i];
                        f[i]=M.RandomSpiralHenonFs[i];
                        g[i]=M.RandomSpiralHenonGs[i];
                        h[i]=M.RandomSpiralHenonHs[i];
                        probabilities[i]=M.RandomSpiralHenonrs[i];
                        xxpower[i]=M.RandomSpiralHenonXforX[i];
                        xypower[i]=1;
                        yypower[i]=1;
                        yxpower[i]=1;

                    }
                    else
                    {
                        m[i]=.1;
                        n[i]=.81;
                        o[i]=.9;
                        f[i]=.2;
                        g[i]=.1;
                        h[i]=.01;
                        xxpower[i]=1;
                        xypower[i]=1;
                        yypower[i]=1;
                        yxpower[i]=1;
                        probabilities[i]=(1f/(double)N);}
                }
            }
/**Make variables for the Henon Map multifractal model.*/
            void MakeHenonMap (int N)
            {

                grow=300;//200;
                over = 460;
                up=450;//500
                MatrixVariables M = new MatrixVariables ();
                InitializeArrays (N);
                for (int i = 0; i < N; i++)
                {
                    if(i<M.HENONRS.length)
                    {
                        m[i]=M.henonmapMS[i];
                        n[i]=M.henonmapNS[i];
                        o[i]=M.HENONOS[i];
                        f[i]=M.henonmapFS[i];
                        g[i]=M.HENONGS[i];
                        h[i]=M.HENONHS[i];
                        probabilities[i]=M.HENONRS[i];
                        xxpower[i]=M.HENONXforXS[i];
                        xypower[i]=1;
                        yypower[i]=1;
                        yxpower[i]=1;

                    }
                    else
                    {
                        m[i]=.1;
                        n[i]=.81;
                        o[i]=.9;
                        f[i]=.2;
                        g[i]=.1;
                        h[i]=.01;
                        xxpower[i]=1;
                        xypower[i]=1;
                        yypower[i]=1;
                        yxpower[i]=1;
                        probabilities[i]=(1f/(double)N)*(i+1);}
                }



            }
            /**Make and initialize new arrays to 0.
             * @param N int for the number of elements (this is the number
             * of sets of probabilities the user has chosen or is the default)*/
            void InitializeArrays (int N)
            {
                
                m=new double[N];
                n=new double[N];
                o=new double[N];
                f=new double[N];
                g=new double[N];
                h=new double[N];
                probabilities=new double[N];
                xxpower= new double [N];
                xypower= new double [N];
                yypower= new double [N];
                yxpower= new double [N];
                for (int i = 0; i< N; i++)
                {      m[i]=0;
                       n[i]=0;
                       o[i]=0;
                       f[i]=0;
                       g[i]=0;
                       h[i]=0;
                       xxpower[i]=0;
                       xypower[i]=0;
                       yypower[i]=0;
                       yxpower[i]=0;
                }
            }
    
    Random rnd = new Random(System.currentTimeMillis());
    boolean didfirst=false;
    float myradius;
    
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
     

/**Returns the index for which set of variables to use
             * based on a randomly selected value and the cumulative
             *probabilities stored in the {@link #probabilities} array.
             *
             * The int corresponds to the index
             *for a number in the array that a randomly generated probability
             * is not greater than, in order of the indices from 0 to the array's length.
             *
             *@return int index for the position in the  {@link #probabilities} array
             *that was randomly chosen
             **/
            int CumulativeProb ()
            {
                int j = 0;
                double p = Math.random ();
                double cumulativeProbability=0;
                for ( j = 0; j < probabilities.length; j++)
                {
                    cumulativeProbability=cumulativeProbability+probabilities[j];
                    if (cumulativeProbability>=p)return j;
//if the p value is less than the cumulative probability,
                }

                return j-1;

            }
}

/**Matrices of set variables specifying the direction of a move for ferns
 * and for the default structures made by the enclosing class.
 * D prefaced arrays are default values. Fern are for
 * fern structure. Equations for x and y for each move
 * in a structure are:
 * <br> <B>x = mx<sup>xpow</sup>
 * + ny<sup>ypow</sup> + o</B>
 * and <br><B>y = fx<sup>xpow</sup> + gy<sup>ypow</sup> + h</B>.*/
class MatrixVariables
{


    /**DMS, DNS, DOS, DFS, DGS, and DHS, DRS are all
     * permanent arrays of default coefficients for making nonferns.*/
    public double[] DMS =
    {1.000, 1.000,1.000,1.000,1.000,1.000,1.000,1.000},
            DNS =
    {0.000,0.000,0.000,0.000,0.000,0.000,0.000,0.000},
            DOS =
    {1.000,-1.000,0.000,1.000,0.000,1.000,-1.000,-1.000},
            DFS =
    {0.000,0.000,0.000,0.000,0.000,0.000,0.000,0.000},
            DGS =
    {1.000, 1.000,1.000,1.000,1.000,1.000,1.000,1.000},
            DHS =
    {0.000,-1.000,1.000,-1.000,-1.000,1.000,0.000,1.000},
            DRS =
    {0.125, 0.250,0.375,0.500,0.625, 0.750, 0.875, 1.000};
    /**Default permanent coefficients for theta array.*/
    public double[] T1 =
    {20.000, 45.000,60.000, 90.000, 100.000, 120.000, 140.000, 160.000},
            TRS=
    {0.125, 0.250,0.375,0.500,0.625, 0.750, 0.875, 1.000},
            c1=
    {1.000, 1.000,1.000,1.000,1.000,1.000,1.000,1.000},
            d1=
    {1.000, 1.000,1.000,1.000,1.000,1.000,1.000,1.000},
            T2=
    {180.000, 200.000,220.000,
             240.000, 260.000, 280.000, 300.000,340.000},
            c2=
             {1.000, 1.000,1.000,1.000,1.000,1.000,1.000,1.000},
                     d2 =
             {1.000, 1.000,1.000,1.000,1.000,1.000,1.000,1.000};


             /**Default permanent coefficients for making ferns.*/
//             public double[] FERNRS =
//             {0.100, 0.8400, 0.9200, 1.000},
             public double[] FERNRS =
             {0.100, 0.7400, 0.0800, 0.080},   FERNMS=
             {0.000,-0.7500,0.200,-0.150,0.000,00.000,0.000,0.000},
                     FERNNS=
             {0.010,0.0400,-.02600,0.2800,0.000,0.000,0.000,0.000},
                     FERNOS=
             {0.000,0.000,0.000,0.000,0.000,0.000,0.000,0.000},
                     FERNFS=
             {0.000,-0.0400,0.2300,0.2600,0.000,0.000,0.000,0.000},
                     FERNGS=
             {0.1600,0.8500,0.2200,0.2400,0.00,0.00,00.00,0.0},
                     FERNHS =
             {0.000,1.600,1.600,0.440,0.000,0.000,0.000,0.000};
//    Parameter* a      b      c     d      e     f      p
//     ------------------------------------------------------
//     Fern      0.0    0.0    0.0   0.16   0.0   0.0   0.10
//               0.2   -0.26   0.23  0.22   0.0   1.6   0.08
//              -0.15   0.28   0.26  0.24   0.0   0.44  0.08
//               0.75   0.04  -0.04  0.85   0.0   1.6   0.74
//
//??
             /**Default permanent coefficients for making ferns specified by David Greene..*/
//             public double[] GreeneFERNRS =
//             {0.100, 0.1800, 0.26, 1.000},
             public double[] GreeneFERNRS =
             {0.100, 0.0800, 0.08, 0.740},
                     GreeneFERNMS=
             {0.000, 0.2, -0.15,0.75},
                     GreeneFERNNS=
             {0.000,-.2600,0.2800,.04},
                     GreeneFERNOS=
             {0.000,0.000,0.000,0.000},
                     GreeneFERNFS=
             {0.000,0.2300,0.2600,-.04},
                     GreeneFERNGS=
             {0.16, 0.22, 0.24, 0.85},
                     GreeneFERNHS =
             {0.000,1.600,.44, 1.600};

             /**Default permanent coefficients for making Henon Maps.*/
//             public double[] HENONRS =
//             {1.00},
             public double[] HENONRS =
             {1.00},
                     HENONMS=
             {-0.2000,0.00,0.00,0.0,0.000,00.000,0.000,0.000},
                     henonmapMS=
             {-1.4000,0.00,0.00,0.0,0.000,00.000,0.000,0.000},
                     HENONNS=
             {1.000,0.000,0.000,0.00,0.000,0.000,0.000,0.000},
                     henonmapNS=
             {0.3,0.000,0.000,0.00,0.000,0.000,0.000,0.000},
                     HENONOS=
             {1.000,0.000,0.000,0.000,0.000,0.000,0.000,0.000},
                     HENONFS=
             {0.999,0.00,0.00,0.00,0.000,0.000,0.000,0.000},
                     spiralHENONFS=
             {-0.9991,0.00,0.00,0.00,0.000,0.000,0.000,0.000},
                     henonmapFS=
             {1.000,0.00,0.00,0.00,0.000,0.000,0.000,0.000},
                     HENONGS=
             {0.000,0.00,0.00,0.00,0.00,0.00,00.00,0.0},
                     HENONHS =
             {0.000,0.00,0.00,0.0,0.000,0.000,0.000,0.000},
                     HENONXforXS =
             {2.000,2.00,2.00,2.0,2.000,2.000,2.000,2.000},
                     HENONrestofexponentS =
             {1.000,1.00,1.00,1.0,1.000,1.000,1.000,1.000},
//                     RandomSpiralHenonrs=
//             {0.994, 1.0},
                     RandomSpiralHenonrs=
             {0.994, 0.006},
                     RandomSpiralHenonMs=
             {-0.2,	-0.210031,	0.0,	0.0,	0.0,	0.0,	0.0,	0.0},
                     RandomSpiralHenonNs=
             {1.0,	1.0,	0.0,	0.0,	0.0,	0.0,	0.0,	0.0},
                     RandomSpiralHenonOs=
             {1.0,	1.0,	0.0,	0.0,	0.0,	0.0,	0.0,	0.0},
                     RandomSpiralHenonFs=
             {-0.9991,	0.99,	0.0,	0.0,	0.0,	0.0,	0.0,	0.0},
                     RandomSpiralHenonGs=
             {0.02, 0.0040,	0.0,	0.0,	0.0,	0.0,	0.0,	0.0},
                     RandomSpiralHenonHs=
             {0.0,	 0.0,	0.0,	0.0,	0.0,	0.0,	0.0,	0.0},
                     RandomSpiralHenonXforX=
             {2.0,	2.0,	2.0,	2.0,	2.0,	2.0,	2.0,	2.0};


             /**Default permanent P array for 1 on each column.*/
             public double[] r1 =
             {1.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000},
                     r2=
             {0.000, 1.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000},
                     r3 =
             {0.000, 0.000, 1.000, 0.000, 0.000, 0.000, 0.000, 0.000},
                     r4=
             {0.000, 0.000, 0.000, 1.000, 0.000, 0.000, 0.000, 0.000},
                     r5 =
             {0.000, 0.000, 0.000, 0.000, 01.000, 0.000, 0.000, 0.000},
                     r6=
             {0.000, 0.000, 0.000, 0.000, 0.000, 1.000, 0.000, 0.000},
                     r7 =
             {0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 1.000, 0.000},
                     r8 =
             {0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 1.000};


}//////END/////////
