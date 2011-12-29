/**
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This software is licensed under the Apache License:
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
 
import ij.plugin.tool.PlugInTool;
import ij.ImagePlus;
import ij.IJ;
import ij.process.ImageProcessor;
import ij.gui.ImageCanvas;
import ij.measure.Calibration;
import java.awt.event.MouseEvent;

/**
 * This tool will change the window/level by dragging the mouse.  The window is changed
 * by dragging the mouse over the image on the x axis.  The level is changed
 * by dragging the mouse on the y axis.
 */
public final class Window_Level_Tool extends PlugInTool {
	private int ImageID;
	private double currentMin = 0;
	private double currentMax = 0;
	private int lastX = -1;
	private int lastY = -1;

	public void mousePressed(ImagePlus imp, MouseEvent e) {
		lastX = e.getX();
		lastY = e.getY();
		currentMin = imp.getProcessor().getMin();
		currentMax = imp.getProcessor().getMax();
	}

	public void mouseDragged(ImagePlus imp, MouseEvent e ) {
		double minMaxDifference = currentMax - currentMin;
		int x = e.getX();
		int y = e.getY();
		int xDiff = x - lastX;
		int yDiff = y - lastY;
		int totalWidth  = (int) (imp.getWidth() * imp.getCanvas().getMagnification() );
		int totalHeight = (int) (imp.getHeight() * imp.getCanvas().getMagnification() );
		double xRatio = ((double)xDiff)/((double)totalWidth);
		double yRatio = ((double)yDiff)/((double)totalHeight);
		
		//scale to our image range
		double xScaledValue = minMaxDifference*xRatio;
		double yScaledValue = minMaxDifference*yRatio;

		//invert x
		xScaledValue = xScaledValue * -1;

		adjustWindowLevel(imp, xScaledValue, yScaledValue );
	}

	void adjustWindowLevel(ImagePlus imp, double xDifference, double yDifference ) {
		ImageProcessor processor = imp.getProcessor();

		//current settings
		double currentWindow = currentMax - currentMin;
		double currentLevel = currentMin + (.5*currentWindow);

		//change
		double newWindow = currentWindow + xDifference;
		double newLevel = currentLevel + yDifference;

		if( newWindow < 0 )
			newWindow = 0;
		if( newLevel < 0 )
			newLevel = 0;

		Calibration cal = imp.getCalibration();
		IJ.showStatus( "Window: " + IJ.d2s(newWindow) + ", Level: " + IJ.d2s(cal.getCValue(newLevel)) );

		//convert to min/max
		double newMin = newLevel - (.5*newWindow);
		double newMax = newLevel + (.5*newWindow);

		processor.setMinAndMax( newMin, newMax );
		imp.updateAndDraw();
	}

	public String getToolIcon() {
		return "T0b12W Tbb12L";
	}

}
