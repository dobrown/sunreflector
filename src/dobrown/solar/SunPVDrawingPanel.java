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

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;

import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.numerics.Quaternion;
import org.opensourcephysics.tools.FunctionEditor;

/**
 * A DrawingPanel to display and control a HorizontalPVView for a SunTab.
 *
 * @author Douglas Brown
 */
public class SunPVDrawingPanel extends DrawingPanel {
	
	SunTab tab;
	
	HorizontalPVView horizontal;
	Point prevMousePt;
	double prevAz;

	/**
	 * Constructor
	 * 
	 * @param tab the SunTab
	 */
	SunPVDrawingPanel(SunTab tab) {
		this.tab = tab;
		removeOptionController();
		setShowCoordinates(false);
		setBorder(BorderFactory.createEtchedBorder());
		this.setSquareAspect(true);
		setPreferredMinMax(-20, 20, -4, 16); // defines view pane in degrees
		setToolTipText("<html>Horizontal view of the solar panel,"
				+ " light rays and mountains.</html>");
		horizontal = new HorizontalPVView(tab, this, 0, 0);
		horizontal.setInflation(4);
		addDrawable(horizontal);
		
		addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				prevMousePt = e.getPoint();
				prevAz = horizontal.cameraAz;
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				setMouseCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				setMouseCursor(Cursor.getDefaultCursor());
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (prevMousePt != null) {
					Point p = e.getPoint();
					double delta = p.x - prevMousePt.x;
					double az = prevAz - Math.toRadians(delta/4);
					horizontal.setCameraAz(az);	
					tab.refreshViews();
				}
			}
		});
	}
	
	/**
	 * Refreshes the display and messages
	 */
	void refresh() {
		horizontal.pvViewCoords = null;
		Quaternion normal = tab.reflector.normal;
		double[] azAlt = SunReflector.getAzimAltitude(normal);
		String s = tab.getAzaltString(azAlt[0], azAlt[1]);
		setMessage("Normal: "+s, 0);

		double[][] rayData = tab.getRayData(tab.when.getTimeIndex());
		if (rayData != null) {
			Quaternion sunbeam = SunReflector.getQuaternion(rayData[0]); // going out		
			double ang = normal.angle(sunbeam);
			int degrees = tab.round(Math.toDegrees(ang));
			setMessage("Incident angle " + degrees + FunctionEditor.DEGREES, 1);
		}

		repaint();
	}	
	
}
