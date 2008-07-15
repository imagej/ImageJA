import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;

/** iMorph
/*   @author Hajime HIrase
/*   @version 0.1.1
/* modified from ImageCalculator plug-in
**/

public class iMorph_ implements PlugIn {

    private static int operator;
    private static String title1 = "";
    private static String title2 = "";
    private static boolean createWindow = true;

    private int nFrame=10;
    public void run(String arg) {
	int[] wList = WindowManager.getIDList();
	if (wList==null) {
	    IJ.error("No windows are open.");
	    return;
	}
	//IJ.register(iMorph_.class);
	String[] titles = new String[wList.length];
	String[] operators = {"Linear"};
	for (int i=0; i<wList.length; i++) {
	    ImagePlus imp = WindowManager.getImage(wList[i]);
	    if (imp!=null && imp.getStackSize()==1)
		titles[i] = imp.getTitle();
	    else
		titles[i] = "";
	}
	GenericDialog gd = new GenericDialog("iMorph", IJ.getInstance());
	String defaultItem;
	if (title1.equals(""))
	    defaultItem = titles[0];
	else
	    defaultItem = title1;
	gd.addChoice("Image1:", titles, defaultItem);
	gd.addChoice("Operation:", operators, operators[operator]);
	if (title2.equals(""))
	    defaultItem = titles[0];
	else
	    defaultItem = title2;
	gd.addChoice("Image2:", titles, defaultItem);
	gd.addNumericField("Number of frames", nFrame, 0);		
	gd.showDialog();
	if (gd.wasCanceled())
	    return;
	int index1 = gd.getNextChoiceIndex();
	title1 = titles[index1];
	operator = gd.getNextChoiceIndex();
	int index2 = gd.getNextChoiceIndex();
	title2 = titles[index2];
	nFrame = (int)gd.getNextNumber();
	//IJ.write(" "+operator);
	ImagePlus img1 = WindowManager.getImage(wList[index1]);
	ImagePlus img2 = WindowManager.getImage(wList[index2]);
	if (img1.getWidth()!=img2.getWidth() || img1.getHeight() != img2.getHeight()) {
	    IJ.showMessage("The two images must have the same dimensions.");
	    return;
	}
	int type1 = img1.getType();
	int type2 = img2.getType();
	if (type1==ImagePlus.GRAY16 || type1==ImagePlus.GRAY32 || type2==ImagePlus.GRAY16
	|| type2==ImagePlus.GRAY32) {
	    IJ.showMessage("Both images must be either 8-bit grayscale or RGB.");
	    return;
	}
	if (nFrame<2) {
	    IJ.showMessage("nFrame must be bigger than 2.");
	    return;
	}
	doOperation(img1, img2);
    }

    void doOperation(ImagePlus img1, ImagePlus img2) {
	ImageProcessor ip1 = img1.getProcessor();
	ImageProcessor ip2 = img2.getProcessor();
	
	ImagePlus iMorphedPlus = NewImage.createRGBImage("iMorphed",ip1.getWidth(), ip1.getHeight(),nFrame,NewImage.FILL_BLACK);
	iMorphedPlus.getStack().getProcessor(1).insert(ip1,0,0);
	iMorphedPlus.getStack().getProcessor(nFrame).insert(ip2,0,0);
	switch (operator) {
	case 0:
		iMLinear(iMorphedPlus);
		break;
	default:
	    iMLinear(iMorphedPlus);
	    break;
	}
	iMorphedPlus.show();
    }
    
    void iMLinear(ImagePlus imp) {
	int n = imp.getStackSize();
	int size = imp.getWidth() * imp.getHeight();
	int fromPixels[] = (int []) imp.getStack().getProcessor(1).getPixels();
	int toPixels[] = (int []) imp.getStack().getProcessor(n).getPixels();
	int tmpPixels[];
	int fRed,fGre,fBlu;
	int tRed,tGre,tBlu;
	int mRed,mGre,mBlu;
	    
	for (int i=2;i<n;i++) {
	    tmpPixels = (int []) imp.getStack().getProcessor(i).getPixels();
	    for (int j = 0;j<size;j++) {
		fRed = (fromPixels[j]&0xff0000)>>16;
		fGre = (fromPixels[j]&0xff00)>>8;
		fBlu = fromPixels[j]&0xff;
		tRed = (toPixels[j]&0xff0000)>>16;
		tGre = (toPixels[j]&0xff00)>>8;
		tBlu = toPixels[j]&0xff;
		mRed = (int)(fRed + ((float)tRed-fRed)/(n-1)*(i-1));
		mGre = (int)(fGre + ((float)tGre-fGre)/(n-1)*(i-1));
		mBlu = (int)(fBlu + ((float)tBlu-fBlu)/(n-1)*(i-1));
		tmpPixels[j] = mRed<<16|mGre<<8|mBlu;
	    }
	}
    }
}
