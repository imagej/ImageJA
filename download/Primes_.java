/**	Primes_.java
*	ImageJ plug-in that finds all primes <32000 and displays them in an image
*/

import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

public class Primes_ extends ImagePlus implements PlugIn {

	short[] pixels;

	public void run(String arg) {
		ImageProcessor primeIP = new ShortProcessor(500, 64, false);
		createPrimes(primeIP);
		setProcessor("Primes", primeIP);
		show();
	}

	void createPrimes(ImageProcessor ip) {
		// list of found primes
		int[] A1 = new int[32000];
		A1[0] = 2;

		// increments
		int[] A2 = new int[32000];
		A2[0] = 0;

		// is the number currently checked a prime
		boolean isPrime = true;

		// counter
		int counter = 2;

		// create new image and get pixel array
		pixels = (short[]) ip.getPixels();
		pixels[2] = 2;

		// next position in A1 and A2
		int pos = 1;

		// square root of 32000
		int sqrt32k = (int) (Math.sqrt(32000))+1;

		int max=0;

		while (counter<32000) {
			// increase counter
			counter++;
			// increase values in A2 and compare
			isPrime = true;
			// check all filled array fields or sqrt(32k) - whichever
			// is smaller
			if (pos<sqrt32k) max = pos;
			else max = sqrt32k;
			for (int i=0;i<max;i++) {
				A2[i]++;
				if (A1[i]==A2[i]) {
					isPrime = false;
					A2[i] = 0;
				}
			}
			// if prime add value to array
			if (isPrime) {
				A1[pos] = counter;
				A2[pos] = 0;
				// write prime value to image
				pixels[counter] = (short) counter;
				// increase position in array
				pos++;
			}
		}
	}
 
	/** Overrides ImagePlus.mouseMoved(). */
	public void mouseMoved(int x, int y) {
		if (pixels[y*500+x]!=0)
			IJ.showStatus((y*500+x)+" is a prime!");
		else
			IJ.showStatus(""+(y*500+x));

	}

}
