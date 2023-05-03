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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.numerics.Quaternion;

/**
 * A class to draw the PV panel, sun/reflected rays and a background
 * seen from a camera at a given azimuth and altitude.
 * 
 * @author Douglas Brown
 */
public class PVView extends SunPoint {
	
	SunApp app;
	DrawingPanel drawingPanel;
	
	double[][] pvVertices;
	Face front, back, top, bottom, left, right;
	Face[] pvFaces; // array of the 6 faces defined above
	Color[] FACE_COLORS = {Color.WHITE, Color.GRAY, 
			Color.BLUE, new Color(160, 80, 255), Color.GREEN, Color.RED};
	
	double[][] pvViewCoords;
	Quaternion dipAxis;
	
	Graphics2D g2;
	Point myScreenLoc;
	Path2D myPath = new Path2D.Double(); // used to draw pvFaces and sunBlock
	Line2D line = new Line2D.Double(); // used to draw rays and normal
	SunPoint utilityPt; 
	
	double[] normal = {0, 0, 0.5};  // normal to PV front face
	
	double[][] axisUp = {{0, 0, -1.5}, {0, 0, 1.5}}; // z-axis
	double[][] axisNorth = {{0, -1.5, 0}, {0, 1.5, 0}}; // y-axis
	double[][] axisEast = {{-1.5, 0, 0}, {1.5, 0, 0}}; // x-axis
	double[][][] axes = {axisUp, axisNorth, axisEast};
	
	double cameraAz = Math.PI; // view from south
	double cameraAlt = Math.PI/2; // overhead view
	double cameraDistance = 7;
	double inflation = 1;
	
	ArrayList<Integer> visibleFaces;
	boolean isFrontVisible;
	AffineTransform rayTransform, arrowTransform;
	GeneralPath arrowhead = new GeneralPath(); 

	/**
	 * Constructor
	 * 
	 * @param app the SunApp
	 * @param panel the panel this will be drawn on
	 * @param x the x-position on the panel
	 * @param y the y-position on the panel
	 */
	public PVView(SunApp app, DrawingPanel panel, double x, double y) {
		super(panel, x, y);
		this.app = app;
		drawingPanel = panel;
		rayTransform = new AffineTransform();
		arrowTransform = new AffineTransform();
		utilityPt = new SunPoint(panel);
		
		// set up PV vertices
		double pvThickness = 0.1;
		pvVertices = new double[][] {
			new double[] {-1, -1, 0}, // left bottom front
			new double[] {-1, 1, 0}, // left top front
			new double[] {1, 1, 0}, // right top front
			new double[] {1, -1, 0}, // right bottom front
			new double[] {-1, -1, -pvThickness}, // left bottom back 
			new double[] {-1, 1, -pvThickness}, // left top back 
			new double[] {1, 1, -pvThickness}, // right top back 
			new double[] {1, -1, -pvThickness} // right bottom back 
		};
		// set up PV panel faces
		front = new Face(3,2,1,0);
		back = new Face(4,5,6,7);
		top = new Face(1,2,6,5);
		bottom = new Face(3,0,4,7);
		left = new Face(0,1,5,4);
		right = new Face(2,3,7,6);		
		pvFaces = new Face[] {front, back, top, bottom, left, right};
		
		arrowhead.moveTo(-3, -5);
		arrowhead.lineTo(0, 5);
		arrowhead.lineTo(3, -5);
	}
			
	/**
	 * Returns the closest corner in the view
	 * @return the index of the closest corner
	 */
	private int getClosestCorner() {
		int closest = 0;
		double zmin = -2; // far away
		for (int i = 0; i < pvViewCoords.length; i++) {
			if (pvViewCoords[i][2] > zmin) {
				zmin = pvViewCoords[i][2];
				closest = i;
			}
		}
		return closest;
	}
	
	/**
	 * Draws and fills a single face of the PV panel
	 * 
	 * @param face the Face
	 * @param fill the fill color
	 */
	private void drawAndFill(Face face, Color fill) {
		myPath.reset();
		myPath.moveTo(pvViewCoords[face.get(0)][0], pvViewCoords[face.get(0)][1]);
		myPath.lineTo(pvViewCoords[face.get(1)][0], pvViewCoords[face.get(1)][1]);					
		myPath.lineTo(pvViewCoords[face.get(2)][0], pvViewCoords[face.get(2)][1]);					
		myPath.lineTo(pvViewCoords[face.get(3)][0], pvViewCoords[face.get(3)][1]);								
		myPath.closePath();
		Shape shape = transform.createTransformedShape(myPath);
		g2.setColor(fill);
		g2.fill(shape);
		g2.setColor(Color.BLACK);
		g2.draw(shape);			
	}
	
	/**
	 * Draws the normal vector to the PV panel
	 */
	protected void drawNormal() {
		// draw normal vector and its end circle
		double[] xyz = getPVViewCoords(normal);
		line.setLine(0,  0,  xyz[0], xyz[1]);
		Shape shape = transform.createTransformedShape(line);
		g2.setStroke(app.getStroke(2f));
		g2.setColor(Color.BLUE);
		g2.draw(shape);

		double r = 0.05;
		Ellipse2D circle = new Ellipse2D.Double(xyz[0]-r, xyz[1]-r, 2*r, 2*r);
		shape = transform.createTransformedShape(circle);
		g2.setColor(Color.BLUE);
		g2.fill(shape);
	}
	
	/**
	 * Draws the current rotation axis.
	 * 
	 * @param nearOnly true to draw the axis only where it's in front of the panel
	 */
	private void drawRotationAxes(boolean nearOnly) {
		g2.setStroke(new BasicStroke(2));
		for (int i = 0; i < 3; i++) {
			if (app.visibleRotationAxis == i) {
				double[][] ends = getAxisCoords(axes[i]);			
				Shape shape = null;
				// if axis straight up z, draw circle
				if (ends[0].length == 3) {
					Quaternion q = new Quaternion(0, ends[0][0], ends[0][1], ends[0][2]);
					if (Math.abs(q.dot(SunReflector.Z_UP)) > 0.99) {
						double r = .02;
						Ellipse2D circle = new Ellipse2D.Double(-r, -r, 2*r, 2*r);
						shape = transform.createTransformedShape(circle);
					}
				}
				if (nearOnly) {
					if (i == 2 || (i == 1 && app.reflector.dip == 0)) {
						if (!isFrontVisible) {
							continue;
						}
					}
					else if (isFrontVisible) {
						if (front.isPointFarther(ends[0], pvViewCoords))
							ends[0] = new double[] {0, 0};
						else
							ends[1] = new double[] {0, 0};														
					}
					else {
						if (back.isPointFarther(ends[0], pvViewCoords))
							ends[0] = new double[] {0, 0};
						else
							ends[1] = new double[] {0, 0};																					
					}
				}
				line.setLine(ends[0][0],  ends[0][1],  ends[1][0],  ends[1][1]);
				if (shape == null) {
					shape = transform.createTransformedShape(line);
				}
				g2.setStroke(app.getStroke(4));
				g2.setColor(Color.MAGENTA);
				g2.draw(shape);
			}				
		}
	}
	
	/**
	 * Gets the visible faces seen by the camera
	 * @return ArrayList of Face indices
	 */
	private ArrayList<Integer> getVisibleFaces() {
		int closest = getClosestCorner();
		visibleFaces = new ArrayList<Integer>();
		isFrontVisible = false;
		for (int i = 0; i < pvFaces.length; i++) {
			if (pvFaces[i].contains(closest)) {
				visibleFaces.add(i);
				if (pvFaces[i] == front)
					isFrontVisible = true;
			}
		}
		// determine drawing order--occluded ones first
		for (int i = 0; i < visibleFaces.size(); i++) {
			// swap faces if first occludes second
			int k = i==2? 0: i;
			int face1 = visibleFaces.get(k);
			int face2 = visibleFaces.get(k+1);
			boolean swap = pvFaces[face1].occludes(pvFaces[face2], pvViewCoords);
			if (swap) {
				visibleFaces.remove(k);
				visibleFaces.add(k+1, face1);
			}				
		}
		return visibleFaces;
	}
	
	/**
	 * Draws the PV panel.
	 */
	private void drawPVPanel() {
		g2.setStroke(new BasicStroke(0.7f));
		// draw faces in determined order
		for (int i = 0; i < visibleFaces.size(); i++) {
			int n = visibleFaces.get(i);
			drawAndFill(pvFaces[n], FACE_COLORS[n]);
		}			
	}
	
	/**
	 * Draws the sun and reflected rays with no glare.
	 */
	protected void drawRays() {
		drawRays(false);
	}
	
	/**
	 * Draws the sun and reflected rays with possible glare.
	 * 
	 * @param hasGlare true if direct glare is present at the camera
	 */
	protected void drawRays(boolean hasGlare) {
		double[][] rayData = app.getRayData(app.when.getTimeIndex()); // sun and reflection		
		if (rayData == null)
			return;
		Color[][] rayColors = app.getRayColors(rayData);
		boolean[] vis = app.getRayVisibility(app.when.getTimeIndex());

		// first the sun ray, if visible
		if (vis[0]) {
			double[] xyz = getRayCoords(rayData[0]);
			double len = getRayLength(rayData[0], xyz);
			line.setLine(0,  0,  len*xyz[0], len*xyz[1]);
								
			Shape shape = rayTransform.createTransformedShape(line);
			// draw wide outer line
			g2.setStroke(app.getStroke(5f));
			g2.setColor(rayColors[0][0]);
			g2.draw(shape);			

			// draw narrow inner line
			g2.setStroke(app.getStroke(0.7f));
			g2.setColor(rayColors[0][1]);
			g2.draw(shape);
			
			// draw arrow
			double d = getArrowDistance(xyz);
			d = Math.min(3*len/4, d);
			utilityPt.setLocation(d*xyz[0], -d*xyz[1]);
			Point p = utilityPt.getScreenPosition();
			double theta = utilityPt.angle(0, 0);
			arrowTransform.setToTranslation(p.x, p.y);
			arrowTransform.rotate(3*Math.PI/2-theta);
			Shape arrow = arrowTransform.createTransformedShape(arrowhead);
			g2.setColor(rayColors[0][1]);
			g2.setStroke(app.getStroke(1.4f));
			g2.draw(arrow);
			g2.fill(arrow);
		}

		// draw the reflection, if visible
		if (vis[1]) {
			double[] xyz = getRayCoords(rayData[1]);
			double[] azAlt = rayData[1];
			if (hasGlare) {
				azAlt[0] = azAlt[0] > Math.PI? azAlt[0] - Math.PI: azAlt[0] + Math.PI;
			}
			double len = getRayLength(azAlt, xyz);
			line.setLine(0,  0,  len*xyz[0], len*xyz[1]);
			Shape shape = rayTransform.createTransformedShape(line);
			g2.setStroke(app.getStroke(3f));
			g2.setColor(rayColors[1][0]);
			g2.draw(shape);
			
			// draw narrow blue
			g2.setStroke(app.getStroke(0.7f));
			g2.setColor(rayColors[1][1]);
			g2.draw(shape);
			
			// draw arrow
			double d = getArrowDistance(xyz);
			d = Math.min(3*len/4, d);
			utilityPt.setLocation(d*xyz[0], -d*xyz[1]);
			Point p = utilityPt.getScreenPosition();
			double theta = utilityPt.angle(0, 0);
			arrowTransform.setToTranslation(p.x, p.y);
			arrowTransform.rotate(Math.PI/2-theta);
			Shape arrow = arrowTransform.createTransformedShape(arrowhead);
			g2.setColor(rayColors[1][1]);
			g2.setStroke(app.getStroke(1.4f));
			g2.draw(arrow);
			g2.fill(arrow);
		}
	}
	
	/**
	 * Draws the background. Implemented by subclasses.
	 */
	protected void drawBackground() {
	}
	
	/**
	 * Sets the inflation. Inflation is size, but when very small the PV
	 * panel is not drawn at all so this is used to hide it as well.
	 * 
	 * @param inflate the inflation
	 */
	protected void setInflation(double inflate) {
		inflation = inflate;
	}
	
	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		if (inflation < 0.1)
			return;

		myScreenLoc = getScreenPosition(); // uses transform, so do this first	
		
		g2 = (Graphics2D)g;
		Stroke s = g2.getStroke();
		Color c = g2.getColor();
		Font f = g2.getFont();
		Shape clip = g2.getClip();

		if (pvViewCoords == null) {
			refreshPVViewCoords();
		}
					
		// set up graphics transforms
		// note graphics draw with +y-axis down
		transform.setToIdentity();
		transform.translate(myScreenLoc.x, myScreenLoc.y);
		double scale = myPanel.getYPixPerUnit();
		transform.scale(scale, scale);
		transform.scale(inflation, inflation);
		
		rayTransform.setToIdentity();
		rayTransform.translate(myScreenLoc.x, myScreenLoc.y);
		rayTransform.scale(scale, scale);
		
		boolean[] vis = app.getRayVisibility(app.when.getTimeIndex());
		
		getVisibleFaces();
		drawBackground();
		if (!vis[1] || !isFrontVisible)
			drawRays();
		if (!isFrontVisible)
			drawNormal();
		// draw full rotation axes behind PV panel
		drawRotationAxes(false);			
		drawPVPanel();	
		// redraw near end of rotation axes in front of PV panel
		if (vis[1] && isFrontVisible)
			drawRays();
		if (isFrontVisible)
			drawNormal();
		drawRotationAxes(true);			
		
		// restore graphics
		g2.setColor(c);
		g2.setStroke(s);
		g2.setFont(f);
		g2.setClip(clip);
		g2 = null;
	}
	
	/**
	 * Applies the current reflector rotations to a Quaternion.
	 * 
	 * @param q the Quaternion
	 * @return the rotated Quaternion
	 */
	protected Quaternion applyPVRotations(Quaternion q) {
		// rotate about Y_NORTH
		q = SunReflector.rotate(q, SunReflector.Y_NORTH, app.reflector.tilt);
		// rotate about dip axis
		q = SunReflector.rotate(q, dipAxis, app.reflector.dip);
		// rotate about Z_UP
		q = SunReflector.rotate(q, SunReflector.Z_UP, -app.reflector.tiltAxisAzimuth);
		return q;
	}
	
	/**
	 * Applies the current camera rotations to a Quaternion.
	 * 
	 * @param q the Quaternion
	 * @return the rotated Quaternion
	 */
	protected Quaternion applyCameraRotations(Quaternion q) {
		double rotAz = cameraAz - Math.PI;
		double rotAlt = cameraAlt - Math.PI/2 ;
		// rotate about Z_UP
		q = SunReflector.rotate(q, SunReflector.Z_UP, rotAz);
		// rotate about X_EAST
		q = SunReflector.rotate(q, SunReflector.X_EAST, rotAlt);
		return q;
	}
	
	/**
	 * Refreshes the camera view coordinates of the PV panel corners.
	 */
	private void refreshPVViewCoords() {
		// set up dip axis by rotating X_EAST about Y_NORTH by tilt angle
		dipAxis = SunReflector.rotate(SunReflector.X_EAST, 
				SunReflector.Y_NORTH, app.reflector.tilt);

		pvViewCoords = new double[pvVertices.length][];
		for (int i = 0; i < pvVertices.length; i++) {
			double[] d = pvVertices[i];
			pvViewCoords[i] = getPVViewCoords(d);
		}
	}
	
	/**
	 * Gets the camera view coordinates of a point.
	 * 
	 * @param xyz the point coordinates
	 * @return the camera view coordinates
	 */
	protected double[] getPVViewCoords(double[] xyz) {
    double d = Math.sqrt(xyz[0]*xyz[0] + xyz[1]*xyz[1] + xyz[2]*xyz[2]);
    
    Quaternion q = new Quaternion(0, xyz[0], xyz[1], xyz[2]);
		q = applyPVRotations(q);
		q = applyCameraRotations(q);
		double[] comps = getVectorCoords(q, d);

		// resize x and y as 1/(cameraDistance - z)
		double dist = cameraDistance - comps[2]; // +z points toward camera
		double factor = cameraDistance / dist;
		comps[0] *= factor;
		comps[1] *= -factor; // also flip y-axis
		return comps;
	}
	
	/**
	 * Gets unit vector coordinates for azimuth and altitude data.
	 * 
	 * @param azAlt the azimuth and altitude
	 * @return the vector coordinates
	 */
	protected double[] getRayCoords(double[] azAlt) {
		Quaternion ray = SunReflector.getQuaternion(azAlt); // unit length
		ray = applyCameraRotations(ray);
		double[] comps = getVectorCoords(ray, 1);
		comps[1] *= -1; // flip y-axis
		return comps;
	}
	
	/**
	 * Gets the ray length required for the ray to be drawn to the
	 * correct altitude in the view.
	 * 
	 * @param azAlt the azimuth and altitude
	 * @param xyz the unit vector coordinates
	 * @return the ray length
	 */
	protected double getRayLength(double[] azAlt, double[] xyz) {
		double r = 90 - Math.toDegrees(azAlt[1]);
		double r0 = Math.sqrt(xyz[0]*xyz[0] + xyz[1]*xyz[1]);
		return r / r0;
	}
	
	/**
	 * Gets the relative distance to the arrow drawn on a ray.
	 * 
	 * @param xyz the unit vector coordinates
	 * @return the relative distance from 0
	 */
	protected double getArrowDistance(double[] xyz) {
		double h = drawingPanel.getYMax() - drawingPanel.getYMin();
		double r = 0.6 * h;
		double r0 = Math.sqrt(xyz[0]*xyz[0] + xyz[1]*xyz[1]);
		return r / r0;
	}
	
	/**
	 * Gets the view coordinates of the ends of a rotation axis.
	 * 
	 * @param axis the end positions
	 * @return the end coordinates in the camera view
	 */
	private double[][] getAxisCoords(double[][] axis) {	
		double[][] result = new double[2][3];
		for (int i = 0; i < 2; i++) {
			double x = axis[i][0];
			double y = axis[i][1];
			double z = axis[i][2];
		  double d = Math.sqrt(x*x + y*y + z*z); // length
			Quaternion q = new Quaternion(0, x, y, z);
	    if (axis == axisEast) {
				q = applyPVRotations(q);
				q = applyCameraRotations(q);
	    }
	    else {
				// rotate about UP
				q = SunReflector.rotate(q, SunReflector.Z_UP, -app.reflector.tiltAxisAzimuth);
				q = applyCameraRotations(q);
	    }
			result[i] = getVectorCoords(q, d);			
			// apparent size goes like 1/(cameraDistance-z)
			double factor = cameraDistance / (cameraDistance - result[i][2]);
			result[i][0] *= factor;
			result[i][1] *= -factor;  // flip y-axis
		}
		return result;
	}
	
	/**
	 * Gets the scaled vector coordinates from a Quaternion.
	 * 
	 * @param q the Quaternion
	 * @param d the scale multiplier
	 * @return the scaled vector coordinates
	 */
	protected double[] getVectorCoords(Quaternion q, double d) {
		double[] comps = q.getCoordinates();
		return new double[] {d*comps[1], d*comps[2], d*comps[3]};
	}
	
}

