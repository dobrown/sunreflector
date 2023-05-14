/*
 * The dobrown.solar package defines a Sun Reflector simulation
 * built on the Open Source Physics framework by Wolfgang Christian.
 *
 * Copyright (c) 2023 Douglas Brown, Wolfgang Christian
 *
 * Sun Reflector is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sun Reflector is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sun Reflector; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at <http://www.gnu.org/copyleft/gpl.html>
 */
package dobrown.solar;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.tools.FunctionEditor;

/**
 * A PVView from a camera on the horizon.
 */
public class HorizontalPVView extends PVView {
	
	Rectangle viewRect = new Rectangle();
	double centerAz; // angle at center of view
	boolean useNegativeAngles; // used in getViewPoint()
	
	/**
	 * Constructor
	 * 
	 * @param tab the SunTab
	 * @param panel the panel this will be drawn on
	 * @param x the x-position on the panel
	 * @param y the y-position on the panel
	 */
	public HorizontalPVView(SunTab tab, DrawingPanel panel, double x, double y) {
		super(tab, panel, x, y);
		cameraAlt = 0; // horizontal
		setCameraAz(Math.PI); // facing north
	}
	
	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		super.draw(panel, g);
		g2 = (Graphics2D)g;
		drawLabels();
	}

	@Override
	protected void drawBackground() {
		Rectangle rect = drawingPanel.getVisibleRect();
		int h = rect.height;
		rect.height = myScreenLoc.y;
		viewRect.setRect(rect);
		
		// fill above horizon with linear gradient paint
		Point2D below = new Point2D.Float(0, rect.y+rect.height);
		Point2D above = new Point2D.Float(0, rect.y);
    Paint paint = new java.awt.LinearGradientPaint(above, 
    		below, 
    		SunTab.skyFractions, 
    		SunTab.skyColors);
    g2.setPaint(paint);
    g2.fill(rect);
    
    // fill below horizon with sunBlock fillColor
    rect.y = myScreenLoc.y;
    rect.height = h - myScreenLoc.y;
    g2.setColor(tab.sunBlock.fillColor);
    g2.fill(rect);	
    
    // draw sunBlock
    if (tab.sunBlock.isEnabled()) {
    	drawSunBlock();
    }
    
    // draw horizontal line across horizon
    line.setLine(rect.x, myScreenLoc.y, rect.x+rect.width, myScreenLoc.y);
    g2.setColor(Color.GRAY);
    g2.draw(line);		
	}
	
	@Override
	protected void drawRays() {
		double[][] rayData = tab.getRayData(tab.when.getTimeIndex()); // sun and reflection
		if (rayData == null)
			return;
		boolean glare = Math.abs(rayData[1][1]) < 2 * SunTab.ONE_DEGREE
				&& Math.abs(cameraAz - rayData[1][0]) < 2 * SunTab.ONE_DEGREE;
		super.drawRays(glare);
		boolean[] vis = tab.getRayVisibility(tab.when.getTimeIndex());
		if (glare && vis[1]) {
			double[] azAlt = rayData[1];
			azAlt[0] = azAlt[0] < Math.PI? azAlt[0] + Math.PI: azAlt[0] - Math.PI;
			double delta = azAlt[0] - centerAz;
			azAlt[0] = centerAz - delta;
			delta = Math.sqrt(delta*delta + azAlt[1]*azAlt[1]);
			Point p = getViewPoint(azAlt);
			g2.setColor(new Color(255, 0, 0, 100));
			int r = 200 - tab.round(Math.abs(delta) * 100 / SunTab.ONE_DEGREE);
			r = Math.max(5, r);
			g2.fillOval(p.x - r, p.y - r, 2*r, 2*r);
		}
		// draw sun disk if visible and in view
		if (vis[0]) {
			Point p = getViewPoint(rayData[0]);
			g2.setColor(Color.YELLOW);
			g2.fillOval(p.x - 8, p.y - 8, 16, 16);
		}
	}
	
	/**
	 * Draws the sun blocker.
	 */
	protected void drawSunBlock() {
		double centerDegrees = Math.toDegrees(centerAz);
		useNegativeAngles = centerDegrees < 90 || centerDegrees > 270;
		int steps = tab.round(drawingPanel.getXMax() - drawingPanel.getXMin());
		myPath.reset();					
		myPath.moveTo(viewRect.x, myScreenLoc.y);
  	for (int j = 0; j <= steps; j++) {
  		int degrees = tab.round(centerDegrees + drawingPanel.getXMin() + j);
  		degrees = degrees > 359? degrees - 360: degrees < 0? degrees + 360: degrees;
  		double az = Math.toRadians(degrees);
			double alt = tab.sunBlock.getAltitude(az);
			Point p = getViewPoint(new double[] {az, alt});
			myPath.lineTo(p.x, p.y);					
		}
		myPath.lineTo(viewRect.x + viewRect.width, myScreenLoc.y);
		myPath.closePath();
		g2.setColor(tab.sunBlock.fillColor);
		g2.fill(myPath);
		g2.setColor(tab.sunBlock.edgeColor);
		g2.draw(myPath);					
	}
	
	/**
	 * Gets the screen position of an azAlt point in the current view
	 * 
	 * @param azAlt {azimuth, altitude} of the point
	 * @return the screen position
	 */
	protected Point getViewPoint(double[] azAlt) {
		double az = azAlt[0];
		double center = centerAz;
		if (useNegativeAngles) {
			center = center > Math.PI? center - 2 * Math.PI: center;
			az = az > Math.PI? az - 2 * Math.PI: az;
		}
		double deltaDegrees = Math.toDegrees(az - center);
		utilityPt.setLocation(deltaDegrees, Math.toDegrees(azAlt[1]));
		return new Point(utilityPt.getScreenPosition()); 			
	}
		
	/**
	 * Sets the camera azimuth.
	 * 
	 * @param az the azimuth
	 */
	protected void setCameraAz(double az) {
		cameraAz = az % (Math.PI*2);
		cameraAz = cameraAz < 0? cameraAz + 2*Math.PI: cameraAz;
		centerAz = cameraAz - Math.PI;
		if (centerAz < 0)
			centerAz += 2*Math.PI;
		pvViewCoords = null;
		drawingPanel.repaint();
	}

	/**
	 * Draw the labels in the view.
	 */
	protected void drawLabels() {
		int degrees = tab.round(Math.toDegrees(cameraAz)+180);
		if (degrees > 180)
			degrees -= 360;
		String label = degrees==0? "Facing North":
			degrees==90? "Facing East":
			degrees==180? "Facing South":
			degrees==-90? "Facing West":
			"Facing "+degrees + FunctionEditor.DEGREES;		
		// only the first label is drawn
		g2.setColor(Color.BLACK);
		g2.setFont(g2.getFont().deriveFont(12f).deriveFont(Font.BOLD));
    FontMetrics metrics = g2.getFontMetrics();
		double top = viewRect.getY();		
    int w = metrics.stringWidth(label);
    int h = metrics.getHeight() -  metrics.getAscent()/2;
		char[] c = label.toCharArray();
		g2.drawChars(c, 0, c.length, 
				tab.round(viewRect.getCenterX() - w/2), tab.round(top + h + 8));			
	}
	
	@Override
	protected double getRayLength(double[] azAlt, double[] xyz) {
		if (azAlt[1] == 0 || xyz[1] == 0)
			return 100000;
		double y = Math.toDegrees(azAlt[1]);
		return Math.abs(y / xyz[1]);
	}
	
}
