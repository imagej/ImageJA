package ij.gui;

import java.awt.*;
import java.awt.image.*;
import ij.*;

/** Elliptical region of interest. */
public class EllipseRoi extends PolygonRoi {
	private static final int vertices = 36;
	private static double defaultRatio = 0.6;
	private double x1, y1, x2, y2;
	private double aspectRatio = defaultRatio;
	private int[] handle = {0, vertices/4, vertices/2, vertices/2+vertices/4};

	public EllipseRoi(int sx, int sy, ImagePlus imp) {
		super(sx, sy, imp);
		type = FREEROI;
		x1 = ic.offScreenX(sx);
		y1 = ic.offScreenY(sy);
	}

	public void draw(Graphics g) {
		super.draw(g);
		int size2 = HANDLE_SIZE/2;
		for (int i=0; i<handle.length; i++)
			drawHandle(g, xp2[handle[i]]-size2, yp2[handle[i]]-size2);
	}

	protected void grow(int sx, int sy) {
		x2 = ic.offScreenX(sx);
		y2 = ic.offScreenY(sy);
		makeEllipse();
		imp.draw();
	}
		
	void makeEllipse() {
		double centerX = (x1 + x2)/2.0;
		double centerY = (y1 + y2)/2.0;
		double dx = x2 - x1;
		double dy = y2 - y1;
		double major = Math.sqrt(dx*dx + dy*dy);
		double minor = major*aspectRatio;
		double phiB = Math.atan2(dy, dx);         
		double alpha = phiB*180.0/Math.PI;
		nPoints = 0;
		for (int i=0; i<vertices; i++) {
			double degrees = i*360.0/vertices;
			double beta1 = degrees/180.0*Math.PI;
			dx = Math.cos(beta1)*major/2.0;
			dy = Math.sin(beta1)*minor/2.0;
			double beta2 = Math.atan2(dy, dx);
			double rad = Math.sqrt(dx*dx + dy*dy);
			double beta3 = beta2+ alpha/180.0*Math.PI;
			double dx2 = Math.cos(beta3)*rad;
			double dy2 = Math.sin(beta3)*rad;
			xp[nPoints] = (int)Math.round(centerX + dx2);
			yp[nPoints] = (int)Math.round(centerY + dy2);
			nPoints++;
		}
		finishPolygon();
	}

	void finishPolygon() {
		Polygon poly = new Polygon(xp, yp, nPoints);
		Rectangle r = poly.getBounds();
		x = r.x;
		y = r.y;
		width = r.width;
		height = r.height;
        for (int i=0; i<nPoints; i++) {
            xp[i] = xp[i]-x;
            yp[i] = yp[i]-y;
        }
	}
	
	protected void handleMouseUp(int screenX, int screenY) {
		if (state==CONSTRUCTING) {
            addOffset();
			finishPolygon();
        }
		state = NORMAL;
	}
	
	protected void moveHandle(int sx, int sy) {
		double ox = ic.offScreenX(sx); 
		double oy = ic.offScreenY(sy);
		switch(activeHandle) {
			case 0: 
				x2 = ox;
				y2 = oy;
				break;
			case 1: 
				double dx = (xp[handle[3]]+x) - ox;
				double dy = (yp[handle[3]]+y) - oy;
				updateRatio(Math.sqrt(dx*dx+dy*dy));
				break;
			case 2: 
				x1 = ic.offScreenX(sx);
				y1 = ic.offScreenY(sy);
				break;
			case 3: 
				dx = (xp[handle[1]]+x) - ox;
				dy = (yp[handle[1]]+y) - oy;
				updateRatio(Math.sqrt(dx*dx+dy*dy));
				break;
		}
		makeEllipse();
		imp.draw();
	}
	
	void updateRatio(double minor) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		double major = Math.sqrt(dx*dx+dy*dy);
		aspectRatio = minor/major;
		if (aspectRatio>1.0) aspectRatio = 1.0;
		defaultRatio = aspectRatio;
	}
	
	public int isHandle(int sx, int sy) {
		int size = HANDLE_SIZE+5;
		int halfSize = size/2;
		int index = -1;
		for (int i=0; i<handle.length; i++) {
			int sx2 = xp2[handle[i]]-halfSize, sy2=yp2[handle[i]]-halfSize;
			if (sx>=sx2 && sx<=sx2+size && sy>=sy2 && sy<=sy2+size) {
				index = i;
				break;
			}
		}
		return index;
	}

}
