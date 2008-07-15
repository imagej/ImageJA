import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;

/*
This file contains a Java language implementation of the 
Fast Hartley Transform algorithm which is covered under
United States Patent Number 4,646,256.

This code may therefore be freely used and distributed only
under the following conditions:

	1)  This header is included in every copy of the code; and
	2)  The code is used for noncommercial research purposes only.

 Firms using this code for commercial purposes may be infringing a United
 States patent and should contact the

	Office of Technology Licensing
	Stanford University
	857 Serra Street, 2nd Floor
	Stanford, CA   94305-6225
	(415) 723 0651

This implementation is based on Pascal
code contibuted by Arlo Reeves.
*/

/** Fast Hartley Transform. */
public class FFT_ implements  PlugInFilter {

	private ImagePlus imp;
	private float[] C;
	private float[] S;
	//private float[] fht;

	public int setup(String arg, ImagePlus imp) {
 		this.imp = imp;
		return DOES_8G+ DOES_16+ DOES_32+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		boolean inverse;
		if (!powerOf2Size(ip)) {
			IJ.error("A square, power of two size image or selection\n(128x128, 256x256, etc.) is required.");
			return;
		}
		ImageProcessor fht  = (ImageProcessor)imp.getProperty("FHT");
		if (fht!=null) {
			ip = fht;
			inverse = true;
		} else
			inverse = false;
		ImageProcessor ip2 = ip.crop();
		if (!(ip2 instanceof FloatProcessor)) {
			ImagePlus imp2 = new ImagePlus("", ip2);
			new ImageConverter(imp2).convertToGray32();
			ip2 = imp2.getProcessor();
		}
		fft(ip2, inverse);
	}
	
	public boolean powerOf2Size(ImageProcessor ip) {
		Rectangle r = ip.getRoi();
		return powerOf2(r.width) && r.width==r.height;
	}

	boolean powerOf2(int n) {		
		int i=2;
		while(i<n) i *= 2;
		return i==n;
	}

	public void fft(ImageProcessor ip, boolean inverse) {
		//IJ.write("fft: "+inverse);
		//new ImagePlus("Input", ip.crop()).show();
		int maxN = ip.getWidth();
		makeSinCosTables(maxN);
		float[] fht = (float[])ip.getPixels();
	 	rc2DFHT(fht, inverse, maxN);
		if (inverse) {
			ip.resetMinAndMax();
			new ImagePlus(imp.getTitle(), ip).show();
		} else {
			ImageProcessor ps = calculatePowerSpectrum(fht, maxN);
			ImagePlus imp = new ImagePlus("FFT", ps);
			imp.setProperty("FHT", ip);
			imp.show();
			if (IJ.altKeyDown()) {
				ImageProcessor amp = calculateAmplitude(fht, maxN);
				new ImagePlus("Amplitude", amp).show();
			}
		}
	}

	void makeSinCosTables(int maxN) {
		int n = maxN/4;
		C = new float[n];
		S = new float[n];
		double theta = 0.0;
		double dTheta = 2.0 * Math.PI/maxN;
		for (int i=0; i<n; i++) {
			C[i] = (float)Math.cos(theta);
			S[i] = (float)Math.sin(theta);
			theta += dTheta;
		}
	}
	
	/** Row-column Fast Hartley Transform */
	void rc2DFHT(float[] x, boolean inverse, int maxN) {
		//IJ.write("FFT: rc2DFHT (row-column Fast Hartley Transform)");
		for (int row=0; row<maxN; row++)
			dfht3(x, row*maxN, inverse, maxN);
		transposeR(x, maxN);
		for (int row=0; row<maxN; row++)		
			dfht3(x, row*maxN, inverse, maxN);
		transposeR(x, maxN);

		int mRow, mCol;
		float A,B,C,D,E;
		for (int row=0; row<maxN/2; row++) { // Now calculate actual Hartley transform
			for (int col=0; col<maxN/2; col++) {
				mRow = (maxN - row) % maxN;
				mCol = (maxN - col)  % maxN;
				A = x[row * maxN + col];	//  see Bracewell, 'Fast 2D Hartley Transf.' IEEE Procs. 9/86
				B = x[mRow * maxN + col];
				C = x[row * maxN + mCol];
				D = x[mRow * maxN + mCol];
				E = ((A + D) - (B + C)) / 2;
				x[row * maxN + col] = A - E;
				x[mRow * maxN + col] = B + E;
				x[row * maxN + mCol] = C + E;
				x[mRow * maxN + mCol] = D - E;
			}
		}
	}
	
	/* An optimized real FHT */
	void dfht3 (float[] x, int base, boolean inverse, int maxN) {
		int i, stage, gpNum, gpIndex, gpSize, numGps, Nlog2;
		int bfNum, numBfs;
		int Ad0, Ad1, Ad2, Ad3, Ad4, CSAd;
		float rt1, rt2, rt3, rt4;

		Nlog2 = log2(maxN);
		BitRevRArr(x, base, Nlog2, maxN);	//bitReverse the input array
		gpSize = 2;     //first & second stages - do radix 4 butterflies once thru
		numGps = maxN / 4;
		for (gpNum=0; gpNum<numGps; gpNum++)  {
			Ad1 = gpNum * 4;
			Ad2 = Ad1 + 1;
			Ad3 = Ad1 + gpSize;
			Ad4 = Ad2 + gpSize;
			rt1 = x[base+Ad1] + x[base+Ad2];   // a + b
			rt2 = x[base+Ad1] - x[base+Ad2];   // a - b
			rt3 = x[base+Ad3] + x[base+Ad4];   // c + d
			rt4 = x[base+Ad3] - x[base+Ad4];   // c - d
			x[base+Ad1] = rt1 + rt3;      // a + b + (c + d)
			x[base+Ad2] = rt2 + rt4;      // a - b + (c - d)
			x[base+Ad3] = rt1 - rt3;      // a + b - (c + d)
			x[base+Ad4] = rt2 - rt4;      // a - b - (c - d)
		 }

		if (Nlog2 > 2) {
			 // third + stages computed here
			gpSize = 4;
			numBfs = 2;
			numGps = numGps / 2;
			//IJ.write("FFT: dfht3 "+Nlog2+" "+numGps+" "+numBfs);
			for (stage=2; stage<Nlog2; stage++) {
				for (gpNum=0; gpNum<numGps; gpNum++) {
					Ad0 = gpNum * gpSize * 2;
					Ad1 = Ad0;     // 1st butterfly is different from others - no mults needed
					Ad2 = Ad1 + gpSize;
					Ad3 = Ad1 + gpSize / 2;
					Ad4 = Ad3 + gpSize;
					rt1 = x[base+Ad1];
					x[base+Ad1] = x[base+Ad1] + x[base+Ad2];
					x[base+Ad2] = rt1 - x[base+Ad2];
					rt1 = x[base+Ad3];
					x[base+Ad3] = x[base+Ad3] + x[base+Ad4];
					x[base+Ad4] = rt1 - x[base+Ad4];
					for (bfNum=1; bfNum<numBfs; bfNum++) {
					// subsequent BF's dealt with together
						Ad1 = bfNum + Ad0;
						Ad2 = Ad1 + gpSize;
						Ad3 = gpSize - bfNum + Ad0;
						Ad4 = Ad3 + gpSize;

						CSAd = bfNum * numGps;
						rt1 = x[base+Ad2] * C[CSAd] + x[base+Ad4] * S[CSAd];
						rt2 = x[base+Ad4] * C[CSAd] - x[base+Ad2] * S[CSAd];

						x[base+Ad2] = x[base+Ad1] - rt1;
						x[base+Ad1] = x[base+Ad1] + rt1;
						x[base+Ad4] = x[base+Ad3] + rt2;
						x[base+Ad3] = x[base+Ad3] - rt2;

					} /* end bfNum loop */
				} /* end gpNum loop */
				gpSize *= 2;
				numBfs *= 2;
				numGps = numGps / 2;
			} /* end for all stages */
		} /* end if Nlog2 > 2 */

		if (inverse)  {
			for (i=0; i<maxN; i++)
			x[base+i] = x[base+i] / maxN;
		}
	}

	void transposeR (float[] x, int maxN) {
		int   r, c;
		float  rTemp;

		for (r=0; r<maxN; r++)  {
			for (c=r; c<maxN; c++) {
				if (r != c)  {
					rTemp = x[r*maxN + c];
					x[r*maxN + c] = x[c*maxN + r];
					x[c*maxN + r] = rTemp;
				}
			}
		}
	}
	
	int log2 (int x) {
		int count = 15;
		while (!btst(x, count))
			count--;
		return count;
	}

	
	private boolean btst (int  x, int bit) {
		//int mask = 1;
		return ((x & (1<<bit)) != 0);
	}

	void BitRevRArr (float[] x, int base, int bitlen, int maxN) {
		int    l;
		float[] tempArr = new float[maxN];
		for (int i=0; i<maxN; i++)  {
			l = BitRevX (i, bitlen);  //i=1, l=32767, bitlen=15
			tempArr[i] = x[base+l];
		}
		for (int i=0; i<maxN; i++)
			x[base+i] = tempArr[i];
	}

	//private int BitRevX (int  x, int bitlen) {
	//	int  temp = 0;
	//	for (int i=0; i<=bitlen; i++)
	//		if (btst (x, i))
	//			temp = bset(temp, bitlen-i-1);
	//	return temp & 0x0000ffff;
	//}

	private int BitRevX (int  x, int bitlen) {
		int  temp = 0;
		for (int i=0; i<=bitlen; i++)
			if ((x & (1<<i)) !=0)
				temp  |= (1<<(bitlen-i-1));
		return temp & 0x0000ffff;
	}

	private int bset (int x, int bit) {
		x |= (1<<bit);
		return x;
	}

	ImageProcessor calculatePowerSpectrum (float[] fht, int maxN) {
		int base;
		float  r, scale;
		float min = Float.MAX_VALUE;
  		float max = Float.MIN_VALUE;
   		float[] fps = new float[maxN*maxN];
 		byte[] ps = new byte[maxN*maxN];

  		for (int row=0; row<maxN; row++) {
			FHTps(row, maxN, fht, fps);
			base = row * maxN;
			for (int col=0; col<maxN; col++) {
				r = fps[base+col];
				if (r<min) min = r;
				if (r>max) max = r;
			}
		}

		if (min<1.0)
			min = 0f;
		else
			min = (float)Math.log(min);
		max = (float)Math.log(max);
		scale = (float)(253.0/(max-min));

		for (int row=0; row<maxN; row++) {
			base = row*maxN;
			for (int col=0; col<maxN; col++) {
				r = fps[base+col];
				if (r<1f)
					r = 0f;
				else
					r = (float)Math.log(r);
				ps[base+col] = (byte)(((r-min)*scale+0.5)+1);
			}
		}
		ImageProcessor ip = new ByteProcessor(maxN, maxN, ps, null);
		swapQuadrants(ip);
		return ip;
	}

	/** Power Spectrum of one row from 2D Hartley Transform. */
 	void FHTps(int row, int maxN, float[] fht, float[] ps) {
 		int base = row*maxN;
		int l;
		for (int c=0; c<maxN; c++) {
			l = ((maxN-row)%maxN) * maxN + (maxN-c)%maxN;
			ps[base+c] = (sqr(fht[base+c]) + sqr(fht[l]))/2f;
 		}
	}

	ImageProcessor calculateAmplitude(float[] fht, int maxN) {
   		float[] amp = new float[maxN*maxN];
   		for (int row=0; row<maxN; row++) {
			amplitude(row, maxN, fht, amp);
		}
		ImageProcessor ip = new FloatProcessor(maxN, maxN, amp, null);
		swapQuadrants(ip);
		return ip;
	}

	/** Amplitude of one row from 2D Hartley Transform. */
 	void amplitude(int row, int maxN, float[] fht, float[] amplitude) {
 		int base = row*maxN;
		int l;
		for (int c=0; c<maxN; c++) {
			l = ((maxN-row)%maxN) * maxN + (maxN-c)%maxN;
			amplitude[base+c] = (float)Math.sqrt(sqr(fht[base+c]) + sqr(fht[l]));
 		}
	}

		float sqr(float x) {
		return x*x;
	}

	/**	Swap quadrants 1 and 3 and quadrants 2 and 4 so the power 
		spectrum origin is at the center of the image.
		2 1
		3 4
	*/
 	public void swapQuadrants (ImageProcessor ip) {
 		ImageProcessor t1, t2;
		int size = ip.getWidth()/2;
		ip.setRoi(size,0,size,size);
		t1 = ip.crop();
  		ip.setRoi(0,size,size,size);
		t2 = ip.crop();
		ip.insert(t1,0,size);
		ip.insert(t2,size,0);
		ip.setRoi(0,0,size,size);
		t1 = ip.crop();
  		ip.setRoi(size,size,size,size);
		t2 = ip.crop();
		ip.insert(t1,size,size);
		ip.insert(t2,0,0);
	}

}

/*


procedure ApplyFilter(rData: rImagePtr);
var
	row, col, width, height, base, i: LongInt;
	line: LineType;
	passMode: boolean;
	t:FateTable;
begin
	SwapQuadrants;
	with info^ do begin
		width := pixelsPerLine;
		height := nLines;
	end;
	for row:= 0 to height - 1 do begin
		GetLine(0, row, width, line);
		base := row * width;
		for col := 0 to width - 1 do
			rData^[base + col] := line[col]/255.0 * rData^[base + col];
	end;
end;


procedure doMasking(rData: rImagePtr);
var
	row, col, width, height, base, i: LongInt;
	line: LineType;
	passMode: boolean;
	t:FateTable;
begin
	GetRectHistogram;
	if (histogram[0] = 0) and (histogram[255] = 0) then
		exit(doMasking);
	UpdateMeter(0, 'Masking');
	passMode := histogram[255] <> 0;
	if passMode then
		ChangeValues(0,254,0)
	else
		ChangeValues(1,255,255);
	for i := 1 to 3 do
		Filter(UnweightedAvg, 0, t);
	UpdatePicWindow;
	ApplyFilter(rData);
end;


	procedure doFFT(fftKind: fftType);
	var
		startTicks, maxN: LongInt;
		trect: rect;
		RealData: rImagePtr;
		doInverse: boolean;
	begin
			doInverse := fftKind <> ForewardFFT;
			if not PowerOf2Size then
				exit(doFFT);
			startTicks := tickCount;
			if info^.DataH = nil then begin
				if doInverse then begin
					PutError('A real image is required to do an inverse transform.');
 					AbortMacro;
					exit(doFFT);
				end;
				if not MakeRealImage then begin
 					AbortMacro;
					exit(doFFT);
				end
			end else begin
				KillRoi;
				SetFFTWindowName(doInverse);
			end;
			hlock(info^.DataH);
			RealData := rImagePtr(info^.DataH^);
			ShowWatch;
			maxN := info^.PixelsPerLine;
			if not MakeSinCosTables(maxN) then
				exit(doFFT);
			AbortFFT := false;
			ShowMessage(CmdPeriodToStop);
			if doInverse then begin
				if fftKind = InverseFFTWithMask then 
					doMasking(RealData)
				else if fftKind = InverseFFTWithFilter then
					ApplyFilter(RealData);
				rc2DFHT(RealData, true, maxN);
				if not AbortFFT then
					DisplayRealImage(RealData);
			end else begin
				rc2DFHT(RealData, false, maxN);
				if not AbortFFT then
					DisplayPowerSpectrum(RealData);
			end;
			if AbortFFT then
				UpdateMeter(-1, 'Hide');
			hunlock(info^.dataH);
			UpdatePicWindow;
			SetRect(trect, 0, 0, maxN, maxN);
			ShowTime(startTicks, trect, '');
			UpdateWindowsMenuItem;
			DisposePtr(ptr(C));
			DisposePtr(ptr(S));
	end;
	

procedure RedisplayPowerSpectrum;
	var
		rData: rImagePtr;
	begin
			if info = noInfo then
				exit(RedisplayPowerSpectrum);
			KillRoi;
			if not PowerOf2Size then
				exit(RedisplayPowerSpectrum);
			if not isFFT then begin
					PutError('Real frequency domain image required.');
					exit(RedisplayPowerSpectrum);
				end;
			hlock(info^.DataH);
			rData := rImagePtr(info^.DataH^);
			DisplayPowerSpectrum(rData);
			hunlock(info^.dataH);
			UpdatePicWindow;
	end;


	function arctan2 (x, y: extended): extended;
{ returns angle in the correct quadrant }
	begin
		if x = 0 then
			x := 1E-30; { Could be improved }
		if x > 0 then
			if y >= 0 then
				arctan2 := arctan(y / x)
			else
				arctan2 := arctan(y / x) + 2 * pi
		else
			arctan2 := arctan(y / x) + pi;
	end;
	

	procedure ShowFFTValues (hloc, vloc, ivalue: LongInt);
		var
			tPort: GrafPtr;
			hstart, vstart: integer;
			r, theta, center: extended;
	begin
		with info^ do
			begin
				hstart := InfoHStart;
				vstart := InfoVStart;
				GetPort(tPort);
				SetPort(InfoWindow);
				TextSize(9);
				TextFont(Monaco);
				TextMode(SrcCopy);
				if hloc < 0 then
					hloc := -hloc;
				center := pixelsPerLine div 2;
				r := sqrt(sqr(hloc - center) + sqr(vloc - center));
				theta := arctan2(hloc - center, center - vloc);
				theta := theta * 180 / pi;
				MoveTo(xValueLoc, vstart);
				if SpatiallyCalibrated then begin
						DrawReal(pixelsPerLine / r / xScale, 6, 2);
						DrawString(xUnit);
						DrawString('/c ');
						DrawString('(');
						DrawReal(hloc - center, 4, 0);
						DrawString(')');
					end else begin
						DrawReal(pixelsPerLine / r, 6, 2);
						DrawString('p/c  ');
						DrawString('(');
						DrawReal(hloc - center, 4, 0);
						DrawString(')');
					end;
				DrawString('    ');
				vloc := PicRect.bottom - vloc - 1;
				if vloc < 0 then
					vloc := -vloc;
				MoveTo(yValueLoc, vstart + 10);
				DrawReal(theta, 6, 2);
				TextMode(srcOr);
				DrawString('ç    ');
				TextMode(srcCopy);
				DrawString('(');
				DrawReal(vloc - center + 1, 4, 0);
				DrawString(')');
				DrawString('    ');
				MoveTo(zValueLoc, vstart + 20);
				if fit <> uncalibrated then
					begin
						DrawReal(cvalue[ivalue], 6, 2);
						DrawString(' (');
						DrawLong(ivalue);
						DrawString(')');
					end
				else
					DrawLong(ivalue);
				DrawString('    ');
				SetPort(tPort);
			end;
	end;


end. {fft Unit}
*/


