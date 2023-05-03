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

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * This represents a face of a PV panel defined by 4 corner indices.
 *
 * @author Douglas Brown
 */
public class Face extends ArrayList<Integer> {
	
	Path2D myPath = new Path2D.Double();
	
	/**
	 * Constructor
	 * 
	 * @param i0 a corner index
	 * @param i1 a corner index
	 * @param i2 a corner index
	 * @param i3 a corner index
	 */
	Face(int i0, int i1, int i2, int i3) {
		add(i0);
		add(i1);
		add(i2);
		add(i3);
	}
	
	/**
	 * Determines if this Face occludes another specified Face.
	 * 
	 * @param f the Face that may be occluded
	 * @param viewCoords the PVView coordinates of all corners
	 * @return true if occluded
	 */
	public boolean occludes(Face f, double[][] viewCoords) {
		for (int i = 0; i < f.size(); i++) {
			int n = f.get(i);
			if (this.contains(n))
				continue;
			double[] xyz = new double[] 
					{viewCoords[n][0],
					viewCoords[n][1],
					viewCoords[n][2]};
			if (isPointFarther(xyz, viewCoords)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the normal vector for this Face.
	 * 
	 * @param viewCoords the PVView coordinates of all PV panel corners
	 * @return the Vector3D normal
	 */
	Vector3D getNormal(double[][] viewCoords) {
		// get two edges of triangle and take cross product
		int n = get(0);
		Vector3D a = new Vector3D( 
				viewCoords[n][0],
				viewCoords[n][1],
				viewCoords[n][2]);
		n = get(1);
		Vector3D b = new Vector3D(
				viewCoords[n][0],
				viewCoords[n][1],
				viewCoords[n][2]);
		n = get(2);
		Vector3D c = new Vector3D( 
				viewCoords[n][0],
				viewCoords[n][1],
				viewCoords[n][2]);
		// turn c into normalized edge b-c
		c = c.minus(b);
		c.normalize();
		// turn b into normalized edge a-b
		b = b.minus(a);
		b.normalize();
		// return cross product of b and c
		return b.cross(c);
	}

	/**
	 * Determines if the PVView view of a point is inside 
	 * the view of this Face.
	 * 
	 * @param xyz the point
	 * @param viewCoords the PVView coordinates of all PV panel corners
	 * @return true if inside
	 */
	boolean isViewInside(double[] xyz, double[][] viewCoords) {
		myPath.reset();
		myPath.moveTo(viewCoords[get(0)][0], viewCoords[get(0)][1]);
		myPath.lineTo(viewCoords[get(1)][0], viewCoords[get(1)][1]);					
		myPath.lineTo(viewCoords[get(2)][0], viewCoords[get(2)][1]);					
		myPath.lineTo(viewCoords[get(3)][0], viewCoords[get(3)][1]);								
		myPath.closePath();
		Point2D p = new Point2D.Double(xyz[0], xyz[1]);
		return myPath.contains(p);
	}
	
	/**
	 * Gets the z coordinate of the intersection point between a line
	 * from a specified point and the (infinite) plane of this Face.
	 * 
	 * @param xyz the point
	 * @param viewCoords the PVView coordinates of all PV panel corners
	 * @return the z-coordinate of the intersection
	 */
	double getZIntersect(double[] xyz, double[][] viewCoords) {
		Vector3D o = new Vector3D(xyz[0], xyz[1], xyz[2]); // origin point for ray
		Vector3D d = new Vector3D(0, 0, 1); // direction of the ray
		Vector3D n = getNormal(viewCoords); // normal to the face
		Vector3D s = new Vector3D( // any point on the face
				viewCoords[get(0)][0], 
				viewCoords[get(0)][1], 
				viewCoords[get(0)][2]);
		s = s.minus(o);
		double denom = d.dot(n);
		double t = denom == 0? 0: s.dot(n) / denom;
		d = d.times(t);
		o = o.plus(d);
		return o.z;
	}

	/**
	 * Determines if this Face occludes a corner point.
	 * 
	 * @param xyz the point to test
	 * @param viewCoords the PVView coordinates of all corners
	 * @return true if occluded
	 */
	boolean occludesPoint(double[] xyz, double[][] viewCoords) {
		return isViewInside(xyz, viewCoords) && isPointFarther(xyz, viewCoords);
	}

	/**
	 * Determines if the z-intersect of a point with this Face
	 * is farther than the point itself.
	 * 
	 * @param xyz the point to test
	 * @param viewCoords the PVView coordinates of all corners
	 * @return true if farther
	 */
	boolean isPointFarther(double[] xyz, double[][] viewCoords) {
		double z = getZIntersect(xyz, viewCoords);
		// more positive z reduces distance
		return xyz[2] < z;
	}

}

