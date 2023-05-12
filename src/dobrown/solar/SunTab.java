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
import java.awt.Stroke;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoIO.SingleExtFileFilter;
import org.opensourcephysics.tools.DataTool;
import org.opensourcephysics.tools.FunctionEditor;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This is a Sun Reflector tab.
 *
 * @author Douglas Brown
 */
public class SunTab {

	public static final double ONE_DEGREE = Math.toRadians(1);
	// constants to define visible rotation axis
	public static final int AXIS_UP = 0, AXIS_TILT = 1, AXIS_DIP = 2;
	public static final int AXIS_NONE = -1;
	public static final int DEFAULT_DAY_OF_YEAR = 196; // midsummer, July 15
	public static final double DEFAULT_HOUR = 12.0; // midday
	public static final double ZOOM_FACTOR = 1.05;
	static final String RESOURCE_PATH = "dobrown/solar/resources/";
  static final String START_DAY = "startday_";
  static final String LATITUDE = "_lat_";
  static final String LONGITUDE = "_long_";
  static final String ZONE = "_timezone_";
	static FileFilter zipFileFilter, imageFileFilter;
  static String defaultSunData = "south_tahoe.txt";
	static float[] skyFractions = new float[] {0.0f, 0.3f, 0.8f, 0.95f, 0.975f, 1f};
	static Color[] skyColors = {
		new Color(200, 200, 255, 60), // sky blue
		new Color(200, 200, 255, 60), // sky blue
		new Color(250, 250, 150, 60), // sky yellow
		new Color(255, 150, 80, 60), // sunset
		new Color(255, 50, 50, 60), // inside horizon
		new Color(255, 0, 0, 60) // red
	};
	static ArrayList<Color> rayColors = new ArrayList<Color>();	
	static Icon zoomIcon, openIcon, closeIcon;
  static String zipFilePath; // used when loading sunzip files
  
	NOAAReader reader;
	String textData;
	DatasetManager sunAzaltData, reflectionAzaltData;
	DatasetManager insolationData;				

	SunReflector reflector;
	SunMoment when;
	SunBlocker sunBlock;
	SunPlottingPanel plot;
	SunPVDrawingPanel pvDrawingPanel;
	SunFrame frame;
	SunControlPanel controls;
	SunTabPanel tabPanel;
	File myFile;
	
	int startDay, dayCount = 1, timeZone;
	double startHour, endHour, latitude, longitude;
	
	Color[][] currentRayColors = new Color[2][2];
	boolean[] currentRayVis = new boolean[2];
	
	int visibleRotationAxis = AXIS_NONE;
	boolean isLoading;
	
	static {
		// set up ray colors in order of altitude in degrees
		// reds near horizon
		for (int i = 0; i < 11; i++)
			rayColors.add(new Color(255, 15*i, 0));
		// greens higher up
		for (int i = 0; i < 10; i++)
			rayColors.add(new Color(255-10*i, 150+10*i, 0));
		for (int i = 0; i < 70; i++) {
			rayColors.add(new Color(155-i, 250, 0));
		}
		
		zoomIcon = ResourceLoader.getImageIcon(RESOURCE_PATH + "zoom.gif");
		openIcon = ResourceLoader.getImageIcon(RESOURCE_PATH + "open.gif");
		closeIcon = ResourceLoader.getImageIcon(RESOURCE_PATH + "close.gif");
		
		zipFileFilter = new SingleExtFileFilter("zip", "ZIP files");
		imageFileFilter = new SingleExtFileFilter(null, "Image files") { //$NON-NLS-1$
			@Override
			public boolean accept(File f, boolean checkDir) {
				String ext = VideoIO.getExtension(f); 
				return (checkDir && f.isDirectory()
						|| "jpg".equalsIgnoreCase(ext) //$NON-NLS-1$
						|| "jpeg".equalsIgnoreCase(ext) //$NON-NLS-1$
						|| "png".equalsIgnoreCase(ext) //$NON-NLS-1$
						|| "gif".equalsIgnoreCase(ext));  //$NON-NLS-1$
			}
		};
	}
	
	/**
	 * Constructor
	 */
	public SunTab() {
		currentRayColors[0][0] = Color.YELLOW;
		currentRayColors[0][1] = Color.RED.darker();

		// create default SunMoment in case no data is loaded
		when = new SunMoment();		
		sunBlock = new SunBlocker(this);
		reflector = new SunReflector(this);
		pvDrawingPanel = new SunPVDrawingPanel(this);
		plot = new SunPlottingPanel(this);
		controls = new SunControlPanel(this);
	}
	
	/**
	 * Gets azalt ray data at a specified time index
	 * 
	 * @param timeIndex the time index
	 * @return {az, alt} for the sun and reflected rays
	 */
	double[][] getRayData(int timeIndex) {
		if (sunAzaltData != null) {			
			double[][] data = new double[2][2];
			Dataset set = sunAzaltData.getDataset(when.getDayNumber());
			data[0][0] = set.getXPointsRaw()[timeIndex];
			data[0][1] = set.getYPointsRaw()[timeIndex];
			set = reflectionAzaltData.getDataset(when.getDayNumber());
			data[1][0] = set.getXPointsRaw()[timeIndex];
			data[1][1] = set.getYPointsRaw()[timeIndex];
			return data;
		}
		return null;
	}
	
	/**
	 * Gets appropriate ray colors for rays with specified azalt data
	 * 
	 * @param rayData {az, alt} for the sun and reflected rays
	 * @return {outerColor, innerColor} for sun and reflected rays
	 */
	Color[][] getRayColors(double[][] rayData) {
		int degrees = round(Math.toDegrees(rayData[1][1]));
		if (degrees >= 0)
			currentRayColors[1][0] = rayColors.get(degrees);
		else {
			Color c = rayColors.get(0);
			int r = Math.max(0, c.getRed() + degrees*5);
			currentRayColors[1][0] = new Color(r, 0, 0);
		}
		currentRayColors[1][1] = currentRayColors[1][0].darker();
		return currentRayColors;
	}
	
	/**
	 * Gets visibility of rays at a specified time index
	 * 
	 * @param timeIndex the time index
	 * @return {sun visibility, reflection visibility}
	 */
	boolean[] getRayVisibility(int timeIndex) {
		if (sunAzaltData != null) {
			// first find sun visibility
			Dataset azalt = sunAzaltData.getDataset(when.getDayNumber());
			double az = 0;
			try {
				az = azalt.getXPointsRaw()[timeIndex];
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			double alt = azalt.getYPointsRaw()[timeIndex];
			currentRayVis[0] = alt >=0;
			if (currentRayVis[0] && sunBlock != null && sunBlock.isEnabled()) {
				double transmission = sunBlock.getAltitude(az) > alt? 0: 1;
				currentRayVis[0] = transmission > 0;
			}
			// then find reflection visibility
			double[] vis = insolationData.getDataset(when.getDayNumber()).getYPointsRaw();
			currentRayVis[1] = currentRayVis[0] && !Double.isNaN(vis[timeIndex]);
		}
		return currentRayVis;
	}
	
	/**
	 * Gets total sun hours for the current day
	 * 
	 * @return {hours striking fixed panel, hours striking a sun-facing surface}
	 */
	double[] getTotalSunHours() {
		if (sunAzaltData == null)
			return new double[] {0, 0};
		double[] insol = insolationData.getDataset(when.getDayNumber()).getXPointsRaw();
		Dataset azalt = sunAzaltData.getDataset(when.getDayNumber());
		double[] az = azalt.getXPointsRaw();
		double[] alt = azalt.getYPointsRaw();
		double hrs = 0;
		double maxHrs = 0;
		for (int i = 0; i < insol.length; i++) {
			// check sun blocker
			double transmission = sunBlock.getAltitude(az[i]) > alt[i]? 0: 1;
			double sol = insol[i];
			sol *= transmission;
			hrs += 0.1*sol; // each insolation represents 0.1 hours
			double maxSol = 1;
			maxSol *= transmission;
			maxHrs += 0.1*maxSol;
		}
		return new double[] {hrs, maxHrs};
	}
		
	/**
	 * Loads new reflection data from current sun data
	 */
	public void updateReflections() {
		DatasetManager[] data = reflector.loadReflectionData(sunAzaltData);
		if (data != null) {
			reflectionAzaltData = data[0];
			insolationData = data[1];
		}
	}
	
	/**
	 * Refreshes all views
	 */
	protected void refreshViews() {
		pvDrawingPanel.refresh();
		plot.refreshPlotData();
		plot.plot();
		controls.refreshDisplay();
		if (tabPanel != null)
			tabPanel.refreshDisplay();
	}
	
	/**
	 * Gets the camera position (azimuth)
	 * 
	 * @return the camera azimuth
	 */
	double getCameraAz() {
		return pvDrawingPanel.horizontal.cameraAz;
	}
	
	/**
	 * Sets the camera position (azimuth)
	 * 
	 * @param azimuth the camera azimuth
	 */
	void setCameraAz(double azimuth) {
		pvDrawingPanel.horizontal.setCameraAz(azimuth);
	}
	
	/**
	 * Rounds double to int in one step, for convenience.
	 */
	int round(double d) {
		return Math.round(Math.round(d));
	}
	
	/**
	 * Gets a Stroke with a specified width.
	 * 
	 * @param w the width
	 */
	Stroke getStroke(float w) {
		return new BasicStroke(w, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER, 8, null, 0);
	}

	/**
	 * Gets a formatted String description of azimuth and altitude
	 * 
	 * @param az the azimuth
	 * @param alt the altitude
	 */
	public String getAzaltString(double az, double alt) {
		int alti = round(Math.toDegrees(alt));
		if (alti == 90)
			return "azimuth undefined, altitude " + alti + FunctionEditor.DEGREES;
		String azim = round(Math.toDegrees(az)) + FunctionEditor.DEGREES;
		String altitude = alti + FunctionEditor.DEGREES;
		return "azimuth " + azim + ", altitude "+altitude;
	}
	
	/**
	 * Gets the relative insolation at a specified time index
	 * 
	 * @param timeIndex the time index
	 * @return the relative insolation (0-1)
	 */
	public double getInsolation(int timeIndex) {
		if (insolationData == null)
			return 0;
		double[] insol = insolationData.getDataset(when.getDayNumber()).getXPointsRaw();
		return insol[timeIndex];
	}
	
	/**
	 * Parses text data with decimal hour in first column
	 * and a (azimuth, altitude) columns for each day.
	 * This assumes the text angle data is in degrees
	 * 
	 * This has the side effect of refreshing the hours array
	 * 
	 * The returned DatasetManager contains solar (azimuth, altitude) 
	 * for each day (dataset) and time index.
	 */
	void loadSunDataFromText(String textData) {
		if (textData == null)
			return;
		// let DataTool parse text data into DatasetManager
		DatasetManager data = DataTool.parseData(textData, null)[0];
		if (data == null) {
			JOptionPane.showMessageDialog(frame, 
					"Unable to parse data from text.", //$NON-NLS-1$
					"Error", //$NON-NLS-1$
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		this.textData = textData;
		ArrayList<Dataset> inputDatasets = data.getDatasets();
		Dataset time = inputDatasets.get(0);
		
		// data returned by DataTool consists of:
		// name: "startDay_xxx_lat_xx.xxxxxx_long_xx.xxxxxx_timezone_x" 
		// dataset 0: (row, decimal hour)
		// dataset 1/odd: (row, azimuth in degrees)
		// dataset 2/even: (row, altitude in degrees)
		
		// determine startDay, latitude, longitude, and time zone from name
		String name = data.getName();
		int n = name.indexOf(START_DAY);
		name = name.substring(START_DAY.length());
		n = name.indexOf(LATITUDE);
		startDay = Integer.parseInt(name.substring(0, n));
		name = name.substring(n + LATITUDE.length());
		n = name.indexOf(LONGITUDE);
		latitude = Double.parseDouble(name.substring(0, n));
		name = name.substring(n + LONGITUDE.length());
		n = name.indexOf(ZONE);
		longitude = Double.parseDouble(name.substring(0, n));
		name = name.substring(n + ZONE.length());
		timeZone = round(Double.parseDouble(name));
		// create SunMoment after startDay  and times are known
		when = new SunMoment(startDay, time.getYPoints());
		
		dayCount = (inputDatasets.size()-1) / 2;
		startHour = when.hours[0];
		endHour = when.hours[when.hours.length-1];

		DatasetManager newData = new DatasetManager();
		n = (inputDatasets.size()-1)/2;
		for (int i = 0; i < n; i++) {
			Dataset a = inputDatasets.get(1 + 2*i);
			Dataset e = inputDatasets.get(2 + 2*i);
			double[] az = a.getYPoints();
			double[] el = e.getYPoints();
			for (int j = 0; j < az.length; j++) {
				az[j] = Math.toRadians(az[j]);
				el[j] = Math.toRadians(el[j]);
			}
			Dataset azalt = newData.getDataset(i);
			azalt.set(az, el);
		}
		sunAzaltData = newData;
	}
	
	/**
	 * Loads data from a text file with az and alt columns for multiple days
	 */
	void loadSunDataFromPath(String textDataFilePath) {
		String textData = ResourceLoader.getString(textDataFilePath);
		loadSunDataFromText(textData);
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
			SunTab app = (SunTab)obj;			
			control.setValue("reflector", app.reflector); //$NON-NLS-1$
			control.setValue("day_time", app.when); //$NON-NLS-1$
			control.setValue("plot", app.plot); //$NON-NLS-1$
			control.setValue("camera_az", app.getCameraAz());
			control.setValue("sun_block", app.sunBlock); //$NON-NLS-1$
			control.setValue("sun_data", app.textData); //$NON-NLS-1$
		}

		/**
		 * Creates a new object. Not used.
		 *
		 * @param control the XMLControl with the object data
		 * @return null
		 */
		@Override
		public Object createObject(XMLControl control) {
			return new SunTab();
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
			SunTab tab = (SunTab)obj;
			tab.isLoading = true;
			tab.loadSunDataFromText(control.getString("sun_data"));
			control.getChildControl("reflector").loadObject(tab.reflector);
			tab.updateReflections();
			control.getChildControl("day_time").loadObject(tab.when);
			control.getChildControl("sun_block").loadObject(tab.sunBlock);
			control.getChildControl("plot").loadObject(tab.plot);
			tab.setCameraAz(control.getDouble("camera_az"));
			tab.refreshViews();
			tab.isLoading = false;
			return obj;
		}
	}

	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}


}
