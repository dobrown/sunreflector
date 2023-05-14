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

import java.util.ArrayList;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;
import org.opensourcephysics.numerics.Quaternion;

/**
 * This is a reflecting surface that can be turned, tilted and dipped.
 * It determines reflections for SunTab.
 */
public class SunReflector {
	
	static final Quaternion X_EAST = new Quaternion(0, 1, 0, 0);
	static final Quaternion Y_NORTH = new Quaternion(0, 0, 1, 0);
	static final Quaternion Z_UP = new Quaternion(0, 0, 0, 1);
			
	SunTab tab;
	
	Quaternion normal = new Quaternion(0, 0, 0, 1);
	Quaternion tiltAxis, dipAxis;
	double tiltAxisAzimuth;
	double tilt, dip;

	/**
	 * Constructor
	 * 
	 * @param tab the SunTab
	 */
	SunReflector(SunTab tab) {
		this.tab = tab;
	}
		
	/**
	 * Returns a rotated Quaternion vector
	 * 
	 * @param q the Quaternion vector to rotate
	 * @param axis the Quaternion axis of rotation
	 * @param theta the rotation angle in radians
	 * @return the rotated Quaternion
	 */
	static Quaternion rotate(Quaternion q, Quaternion axis, double theta) {
		double sin = Math.sin(theta/2);
		double cos = Math.cos(theta/2);
		double[] coords = axis.getCoordinates();
		Quaternion rot = new Quaternion(cos, coords[1]*sin, coords[2]*sin, coords[3]*sin);
		Quaternion result = new Quaternion(rot);
		result.conjugate();
		result.multiply(q);
		result.multiply(rot);
		return result;
	}
	
	/**
	 * Returns a reflected Quaternion vector
	 * 
	 * @param normal the normal to the reflecting surface
	 * @param in the incoming Quaternion vector
	 * @return the reflected Quaternion vector
	 */
	static Quaternion reflect(Quaternion normal, Quaternion in) {
		Quaternion out = new Quaternion(normal);
		out.multiply(in);
		out.multiply(normal);
		return out;
	}
	
	/**
	 * Returns the azimuth and altitude of a Quaternion vector.
	 * 
	 * @param q the Quaternion vector
	 * @return double[] {azim, altitude}
	 */
	public static double[] getAzimAltitude(Quaternion q) {
		double[] comps = q.getCoordinates();
		double alt = Math.PI/2 - Z_UP.angle(q);
		double az = Math.PI/2 - Math.atan2(comps[2], comps[1]);	
		if (az < 0)
			az += 2*Math.PI;
		return new double[] {az, alt};
	}
	
	/** 
	 * Returns a Quaternion vector representing a vector
	 * pointing to the (azimuth, altitude) specified.
	 * 
	 * @param azalt double[] {azimuth, altitude}
	 * @return the Quaternion
	 */
	static Quaternion getQuaternion(double[] azalt) {
		Quaternion q = new Quaternion(0, 0, 1, 0); // north, 0 degrees
		Quaternion altiAxis = new Quaternion(0, 1, 0, 0); // 90 degrees
		
		Quaternion down = new Quaternion(Z_UP);
		down.conjugate();
		// first rotate q about down by azimuth
		q = rotate(q, down, azalt[0]);
		altiAxis = rotate(altiAxis, down, azalt[0]);
		// then rotate q around rotated altiAxis by altitude
		altiAxis = rotate(q, altiAxis, azalt[1]);
		return altiAxis;
	}
	
	/** 
	 * Sets the azimuth of the tilt axis.
	 * 
	 * @param theta the azimuth of the tilt axis in radians
	 */
	void setTiltAxis(double theta) { // in radians
		tiltAxisAzimuth = theta;
		tiltAxis = new Quaternion(0, Math.sin(theta), Math.cos(theta), 0);
	}
		
	/** 
	 * Sets the tilt angle.
	 * 
	 * @param theta the tilt angle in radians
	 */
	void setTilt(double theta) { // in radians
		tilt = theta;
	}
		
	/** 
	 * Sets the dip angle.
	 * 
	 * @param theta the dip in radians
	 */
	void setDip(double theta) { // in radians
		dip = theta;
	}
	
	/** 
	 * Applies the current tilt and dip rotations to a Quaternion.
	 * 
	 * @param q the Quaternion
	 * @return the rotated Quaternion
	 */
	Quaternion applyRotations(Quaternion q) {
		// rotate about tiltAxis
		q = rotate(q, tiltAxis, tilt);
		// rotate about dipAxis
		return rotate(q, dipAxis, dip);
	}
		
	/**
	 * Determines reflections at all times and days for the
	 * current rotation axis and angles.
	 * Also determines insolation and reflection visibility.
	 * 
	 * DatasetManager[0] has reflection data (azimuth, altitude)
	 * DatasetManager[1] has (insolation, visibility)
	 */
	DatasetManager[] loadReflectionData(DatasetManager sunData) {
		// set up rotation axes
		tiltAxis = new Quaternion(0, Math.sin(tiltAxisAzimuth), Math.cos(tiltAxisAzimuth), 0);
		dipAxis = rotate(tiltAxis, Z_UP, -Math.PI/2);
		dipAxis = rotate(dipAxis, tiltAxis, tilt);
		
		normal = applyRotations(new Quaternion(Z_UP));
		
		if (sunData == null)
			return null;

		ArrayList<Dataset> sunDatasets = sunData.getDatasets();		
		DatasetManager reflectionData = new DatasetManager();
		DatasetManager insolData = new DatasetManager();
		
		int n = sunDatasets.size(); // number of days of data
		for (int i = 0; i < n; i++) {
			double[] insolation = new double[tab.when.hours.length];
			double[] vis = new double[tab.when.hours.length];
			Dataset azalt = sunDatasets.get(i);
			double[] az = azalt.getXPoints();
			double[] alt = azalt.getYPoints();
		  
			double[] reflAz = new double[az.length];
			double[] reflAlt = new double[alt.length];
			for (int k = 0; k < az.length; k++) {
				// determine reflection angles and add to list
				double[] arr = new double[] {az[k], alt[k]};
				Quaternion sunbeam = getQuaternion(arr); // going out
					
				double ang = normal.angle(sunbeam);
				// reflection not visible if sunbeam hits back of panel
				// or if sunbeam is below horizon
				if (ang > Math.PI/2 || alt[k] < 0) {
					vis[k] = Double.NaN;
					insolation[k] = 0;					
				}
				else {
					insolation[k] = Math.cos(ang);
				}
				sunbeam.conjugate(); // now coming in, not going out
				Quaternion reflected = reflect(normal, sunbeam);
				double[] angle = getAzimAltitude(reflected);
				reflAz[k] = angle[0];
				reflAlt[k] = angle[1];
			}
			Dataset dataset = reflectionData.getDataset(i);
			dataset.set(reflAz, reflAlt);			
			dataset = insolData.getDataset(i);
			dataset.set(insolation, vis);
		}
		return new DatasetManager[] {reflectionData, insolData};
	}
	
	/**
	 * A class to save and load data for this class.
	 */
	static class Loader implements XML.ObjectLoader {

		/**
		 * Saves an object's data to an XMLControl.
		 *
		 * @param control the control to save to
		 * @param obj     the object to save
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			SunReflector reflector = (SunReflector)obj;
			control.setValue("tilt_axis", reflector.tiltAxisAzimuth); //$NON-NLS-1$
			control.setValue("tilt", reflector.tilt); //$NON-NLS-1$
			control.setValue("dip", reflector.dip); //$NON-NLS-1$
		}

		/**
		 * Creates a new object.
		 *
		 * @param control the XMLControl with the object data
		 * @return the newly created object
		 */
		@Override
		public Object createObject(XMLControl control) {
			return null;
		}

		/**
		 * Loads an object with data from an XMLControl.
		 *
		 * @param control the control
		 * @param obj     the object
		 * @return the loaded object
		 */
		@Override
		public Object loadObject(XMLControl control, Object obj) {
			SunReflector reflector = (SunReflector)obj;
			reflector.setTiltAxis(control.getDouble("tilt_axis")); //$NON-NLS-1$
			reflector.tilt = control.getDouble("tilt"); //$NON-NLS-1$
			reflector.dip = control.getDouble("dip"); //$NON-NLS-1$
			return obj;
		}
	}

	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

}
