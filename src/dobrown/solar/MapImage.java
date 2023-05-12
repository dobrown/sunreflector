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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.MeasuredImage;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This is a MeasuredImage that draws itself to scale on a plotting panel
 * and has a fixed origin.
 * 
 * @author Douglas Brown
 */
public class MapImage extends MeasuredImage {
	
	private double[] originXY = {0, 0};
	private double scale = -1;
	private String imagePath;
	String tempImageName;  // used when saving zip

	/**
	 * Constructor
	 * 
	 * @param path a image path
	 */
	public MapImage(String path) {
		path = XML.forwardSlash(path);
		BufferedImage mapImage = ResourceLoader.getBufferedImage(path);
		if (mapImage != null) {
			setImage(mapImage); // also sets imagePath to null
			imagePath = path; // set actual path 
		}
	}
	
	/**
	 * Constructor
	 * 
	 * @param mapImage a BufferedImage
	 */
	public MapImage(BufferedImage mapImage) {
		if (mapImage != null) {
			setImage(mapImage);
		}
	}
	
	/**
	 * Constructor
	 * 
	 * @param image an Image
	 */
	public MapImage(Image image) {
		if (image != null && image instanceof BufferedImage) {
			setImage((BufferedImage)image);
		}
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		g2.drawImage(image, 0, 0, null);
		setImage(bi);
	}
	
	@Override
	public void setImage(BufferedImage image) {
		super.setImage(image);
		if (scale == -1) {
			scale = 1;
			originXY[0] = image.getWidth()/2;
			originXY[1] = image.getHeight()/2;
		}
		imagePath = null;
		refreshMinMax();
	}
	
	/**
	 * Sets the origin.
	 * 
	 * @param x the x-position on the panel
	 * @param y the y-position on the panel
	 */
	public void setOrigin(double x, double y) {
		originXY[0] = x;
		originXY[1] = y;
		refreshMinMax();
	}
	
	/**
	 * Gets the origin.
	 * 
	 * @return double[] fixed origin {x, y}
	 */
	public double[] getOrigin() {
		return new double[] {originXY[0], originXY[1]};
	}
	
	/**
	 * Sets the scale keeping the origin fixed.
	 * 
	 * @param scale the scale
	 */
	public void setScaleWithFixedOrigin(double scale) {
		this.scale = scale;
		refreshMinMax();
	}

	/**
	 * Sets the scale keeping the image center fixed.
	 * 
	 * @param newScale the scale
	 */
	public void setScaleWithFixedImageCenter(double newScale) {
		// determine prev center loc of image
		double w = scale * getImage().getWidth();
		double h = scale * getImage().getHeight();
		double x0 = getXMin() + w/2;
		double y0 = getYMin() + h/2;
		// set scale and determine new center loc
		setScaleWithFixedOrigin(newScale);
		w = scale * getImage().getWidth();
		h = scale * getImage().getHeight();
		double x = getXMin() + w/2;
		double y = getYMin() + h/2;
		// set origin to restore prev center loc
		originXY[0] += (x-x0)/scale;
		originXY[1] -= (y-y0)/scale;
		refreshMinMax();
	}

	/**
	 * Gets the scale.
	 * 
	 * @return the scale
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * Gets the image path.
	 * 
	 * @return the path
	 */
	public String getImagePath() {
		return imagePath;
	}
	
	/**
	 * Gets the visibility.
	 * 
	 * @return true if visible
	 */
	public boolean isVisible() {
		return visible;
	}
	
	/**
	 * Refreshes the min/max edges of this image in scaled units
	 * based on the current scale and origin.
	 */
	private void refreshMinMax() {
		if (getImage() == null)
			return;
		double x = scale * originXY[0];
		double y = scale * originXY[1];
		double w = scale * getImage().getWidth();
		double h = scale * getImage().getHeight();
		setMinMax(-x, w-x, y-h, y);		
	}
	
	boolean contains(SunPoint p) {
		if (p == null) return false;
		return p.getX() > getXMin()
				&& p.getX() < getXMax()
				&& p.getY() > getYMin()
				&& p.getY() < getYMax();
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
			MapImage map = (MapImage)obj;
			control.setValue("name", map.tempImageName);
			if (map.tempImageName == null) {
				control.setValue("path", map.imagePath);
			}
			control.setValue("origin", map.getOrigin());
			control.setValue("scale", map.getScale());			
		}

		/**
		 * Creates a new object.
		 *
		 * @param control the XMLControl with the object data
		 * @return the newly created object
		 */
		@Override
		public Object createObject(XMLControl control) {
			if (control.getPropertyNamesRaw().contains("name")) {
				String path = SunTab.zipFilePath + control.getString("name");
				MapImage map = new MapImage(path);
				if (map.image != null)
					return map;
			}
			else if (control.getPropertyNamesRaw().contains("path")) {
				String path = control.getString("path");
				MapImage map = new MapImage(path);
				if (map.image != null)
					return map;
			}
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
			MapImage map = (MapImage)obj;
			double[] origin = (double[])control.getObject("origin");
			map.setOrigin(origin[0], origin[1]);
			map.setScaleWithFixedOrigin(control.getDouble("scale"));
			return map;
		}
	}

	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

}
