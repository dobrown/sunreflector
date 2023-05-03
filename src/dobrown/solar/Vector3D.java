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

import org.opensourcephysics.numerics.Util;

/**
 * A class to do vector3D operations. 
 */
public class Vector3D {
  double x, y, z;

	/**
	 * Constructor
	 * 
	 * @param x 
	 * @param y 
	 * @param z 
	 */
  Vector3D(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
  }

	/**
	 * Adds a vector to this one and returns the result
	 * 
	 * @param v the vector to add
	 * @return the result vector
	 */
  Vector3D plus(Vector3D v) {
      return new Vector3D(x + v.x, y + v.y, z + v.z);
  }

	/**
	 * Subtracts a vector to this one and returns the result
	 * 
	 * @param v the vector to subtract
	 * @return the result vector
	 */
  Vector3D minus(Vector3D v) {
      return new Vector3D(x - v.x, y - v.y, z - v.z);
  }

	/**
	 * Multiples this vector by a number and returns the result
	 * 
	 * @param s the multiplier
	 * @return the result vector
	 */
  Vector3D times(double s) {
      return new Vector3D(s * x, s * y, s * z);
  }

	/**
	 * Finds the cross-product of this vector with another and returns the result
	 * 
	 * @param v the vector to cross
	 * @return the result vector
	 */
  Vector3D cross(Vector3D v) {
    double x_ = z*v.y-y*v.z;
    double y_ = x*v.z-z*v.x;
    double z_ = y*v.x-x*v.y;
    return new Vector3D(x_, y_, z_);
  }

	/**
	 * Finds the dot-product of this vector with another and returns the result
	 * 
	 * @param v the vector to dot
	 * @return the dot product
	 */
  double dot(Vector3D v) {
      return x * v.x + y * v.y + z * v.z;
  }

	/**
	 * Normalizes this vector.
	 */
  public void normalize() {
    double norm = x*x + y*y + z*z;
    if (norm < Util.defaultNumericalPrecision || norm == 1) 
      return; // vector is zero
    norm = 1 / Math.sqrt(norm);
    x *= norm;
    y *= norm;
    z *= norm;
  }

  @Override
  public String toString() {
      return String.format("(%f, %f, %f)", x, y, z);
  }
}
