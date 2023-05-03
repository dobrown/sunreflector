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

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.media.core.TPoint;

/**
 * This is a Point2D with methods to directly get and set screen positions
 * on a dedicated DrawingPanel. 
 *
 * @author Douglas Brown
 */
public class SunPoint extends TPoint {
	
	DrawingPanel myPanel;
	AffineTransform transform;
	
	public SunPoint(DrawingPanel panel) {
		this(panel, 0, 0);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param panel the dedicated DrawingPanel 
	 * @param x the x-position in DrawingPanel units
	 * @param y the y-position in DrawingPanel units
	 */
	public SunPoint(DrawingPanel panel, double x, double y) {
		super(x, y);
		myPanel = panel;
		screenPt = new Point();
		worldPt = new Point2D.Double();
		transform = new AffineTransform();
		
	}
	
	/**
	 * Sets the screen position on the dedicated DrawingPanel.
	 * 
	 * @param x the x-position in screen pixels
	 * @param y the y-position in screen pixels
	 */
	public void setScreenPosition(int x, int y) {
		screenPt.setLocation(x, y);
		transform.setTransform(myPanel.getPixelTransform());
		try {
			transform.inverseTransform(screenPt, worldPt);
		} catch (NoninvertibleTransformException ex) {
			ex.printStackTrace();
		}
		setLocation(worldPt.x, worldPt.y);
	}

	/**
	 * Gets the screen position on the dedicated DrawingPanel.
	 * 
	 * @return a Point with pixel x, y 
	 */
	public Point getScreenPosition() {
		transform.setTransform(myPanel.getPixelTransform());
		transform.transform(this, screenPt);
		return screenPt;
	}
	
}
