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
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import javax.swing.BorderFactory;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;
import org.opensourcephysics.display.Drawable;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display.axes.PolarAxes;
import org.opensourcephysics.tools.FunctionEditor;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * A plotting panel for sun and reflection rays, tracks, sun block
 * and map image.
 *
 * @author Douglas Brown
 */
public class SunPlottingPanel extends PlottingPanel {
	
	static ArrayList<Color> rayColors = new ArrayList<Color>();
	static {
		// reflected rays are green above 15 degrees, sunset colors near horizon
		for (int i = 0; i < 30; i++)
			rayColors.add(new Color(100+3*i, 230, 0));
		for (int i = 0; i < 6; i++)
			rayColors.add(new Color(230, 160 - 25*i, 0));
	}

	// default map
	static double[] defaultMapOrigin = {500, 300};
	static double defaultMapScale = 0.4;
	static String defaultMapImage = "south_tahoe.gif";
	
	// instance fields
	SunApp app;
	
	Color reflectionTrackColor = new Color(80, 150, 20);
	
	MapImage map;
	int mapAlpha = 200;
	
	SunPoint mapControlPt; // used to move map
	SunPoint originPt; // always at the origin
	
	DatasetManager sunPlotData, reflectionPlotData;
	
	SunPlotBackground overlay;
	SunDataset sunDataset, reflectionDataset;
	boolean tracksVisible = false;
	PVView pvPanel;
	int pvInflation = 25;
	
	int defaultPlotMinMax = 105; // degrees
	int maxPlotWidth = 270; // limits zooming out
	
	NumberFormat percentFormat = NumberFormat.getPercentInstance();
	AffineTransform transform = new AffineTransform();
	Ellipse2D circle = new Ellipse2D.Double(); // used variously
	Polygon camera = new Polygon(); 
	
	/**
	 * Constructor
	 * 
	 * @param app the SunApp
	 */
	SunPlottingPanel(SunApp app) {
		super("", "", "");
		this.app = app;
		refreshPlotData();
		createGUI();
	}

	/**
	 * Creates the GUI.
	 */
	public void createGUI() {
		removeOptionController();
		setBorder(BorderFactory.createEtchedBorder());
		overlay = new SunPlotBackground();
		// load map
//		String jarDir = OSPRuntime.getLaunchJarDirectory();
//		BufferedImage image = ResourceLoader.getBufferedImage(
//				jarDir + "/" + defaultMapImage); 
		BufferedImage image = ResourceLoader.getBufferedImage(
				SunApp.RESOURCE_PATH + defaultMapImage); 
		if (image != null) {
			MapImage map = new MapImage(image);
			setMap(map);
			getMap().setOrigin(defaultMapOrigin[0], defaultMapOrigin[1]);
			getMap().setScaleWithFixedOrigin(defaultMapScale);			
		}
		// set up camera
		camera.addPoint(0, 5);
		camera.addPoint(-4, -5);
		camera.addPoint(4, -5);
		
		setToolTipText("<html>Overhead view of the solar panel, light rays, horizon and mountains."
				+ "<br>The solar panel is at the center and the horizon surrounds it.</html>");
				
		// set up axes
		setPolar("", 180);
		PolarAxes axes = (PolarAxes)getAxes();
		axes.autospaceRings(false);
		axes.setDeltaTheta(Math.PI/2);
		axes.setShowMajorYGrid(false);
		axes.setShowMinorYGrid(false);
		// set interior background 
		axes.setInteriorBackground(new Color(255,255,255, 255-mapAlpha));
		setAutoscaleX(false);
		setAutoscaleY(false);
		setPreferredMinMaxX(-defaultPlotMinMax, defaultPlotMinMax);
		setPreferredMinMaxY(-defaultPlotMinMax, defaultPlotMinMax);
				
		pvPanel = new PVView(app, this, 0, 0);
		pvPanel.setInflation(pvInflation);

		addMouseWheelListener(new MouseAdapter() {
			public void mouseWheelMoved(MouseWheelEvent e){
				int delta = -e.getWheelRotation();
				double factor = 1 + delta/20.0;
				if (map != null) {
					map.setScaleWithFixedOrigin(map.getScale() * factor);
					repaint();
				}
			}
		});
		addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				int but = e.getButton();
				if (but != MouseEvent.BUTTON1)
					return;
				Point p = e.getPoint();
				mapControlPt.setScreenPosition(p.x, p.y);
				pvPanel.setInflation(0.2);
				repaint();
			}	
			
			@Override
			public void mouseReleased(MouseEvent e) {
				pvPanel.setInflation(pvInflation);
				repaint();
			}	

		});
		
		originPt = new SunPoint(this);
		
		mapControlPt = new SunPoint(this) {			

			@Override
			public void setXY(double x, double y) {
				// x, y in scaled units
				double dx = x - this.x;
				double dy = y - this.y;
				super.setXY(x, y);
				if (mouseEvent == null)
					return;
				// move map
				if (map != null) {
					double[] xy = map.getOrigin();
					xy[0] -= dx / map.getScale();
					xy[1] += dy / map.getScale();
					map.setOrigin(xy[0], xy[1]);
				}
				plot();
			}	
			
			@Override
			public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
				if (mouseEvent != null && mouseEvent.getButton()== MouseEvent.BUTTON1) {
					setMouseCursor(null);	// setMouseCursor() chooses correct cursor
					return this;
				}
				return null;
			}
			
		};

		plot();		
	}
	
	/**
	 * Refreshes the plot data (x, y) for the current sun and reflection azalt data
	 */
	void refreshPlotData() {
		sunPlotData = getPlotData(app.sunAzaltData);
		reflectionPlotData = getPlotData(app.reflectionAzaltData);
	}
	
	/**
	 * Determines plot data (x, y) for the specified azalt data (az, alt).
	 * 
	 * @param azaltData DatasetManager
	 * @return plotData DatasetManager
	 */
	DatasetManager getPlotData(DatasetManager azaltData) {
		DatasetManager plotData = new DatasetManager();
		if (azaltData != null) {
			int n = azaltData.getDatasetsRaw().size();
			for (int i = 0; i < n; i++) {
				Dataset azalt = azaltData.getDataset(i);
				double[] az = azalt.getXPoints(); // in radians
				double[] alt = azalt.getYPoints();
				double[] x = new double[az.length];
				double[] y = new double[az.length];
				for (int j = 0; j < az.length; j++) {
					double r = 90 - Math.toDegrees(alt[j]);
					x[j] = r * Math.sin(az[j]);
					y[j] = r * Math.cos(az[j]);
				}
				Dataset plotted = plotData.getDataset(i);
				plotted.set(x, y);
				plotted.setLineColor(azalt.getLineColor());
			}	
		}
		return plotData;
	}
	
	/**
	 * Returns the azimuth and altitude for a given plot (x, y).
	 * 
	 * @param x
	 * @param y
	 * @return double[] {azimuth, altitude}
	 */
	public double[] getAzalt(double x, double y) {
		double az = Math.PI/2 - Math.atan2(y, x);	
		if (az < 0)
			az += 2*Math.PI;
		double d = Math.sqrt(x*x + y*y);
		double alt = (90 - d) * Math.PI / 180;
		return new double[] {az, alt};
	}
	
	/**
	 * Plots by adding current ray datasets and other drawables.
	 */
	public void plot() {
		clear();
		pvPanel.pvViewCoords = null;
		addDrawable(pvPanel);
		addDrawable(overlay);
		addDrawable(mapControlPt);
		addDrawable(app.sunBlock);
		ArrayList<Dataset> sets = sunPlotData.getDatasetsRaw();
		int t = app.when.getDayNumber();
		if (t < sets.size()) {
			Dataset xy = sunPlotData.getDataset(t);
			sunDataset = new SunDataset(xy);
			addDrawable(sunDataset);		
			xy = reflectionPlotData.getDataset(t);
			reflectionDataset = new SunDataset(xy);
			addDrawable(reflectionDataset);
		}
		repaint();
	}
	
	@Override
	public void setMouseCursor(Cursor cursor) {
		if (mouseEvent != null && 
				(mouseEvent.isControlDown() || mouseEvent.isShiftDown())) {
			super.setMouseCursor(map == null?
					Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR):
					Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));	
		}
		else
			super.setMouseCursor(Cursor.getDefaultCursor());
	}
			
	/**
	 * Gets the map.
	 * 
	 * @return the map
	 */
	MapImage getMap() {
		return map;
	}
	
	/**
	 * Sets the map.
	 * 
	 * @param map a MapImage, may be null
	 */
	void setMap(MapImage map) {
		this.map = map;
	}
	
	/**
	 * Sets tracks visibility.
	 * 
	 * @param vis
	 */
	void setTracksVisible(boolean vis) {
		tracksVisible = vis;
		plot();
	}
	
	/**
	 * Gets tracks visibility.
	 * 
	 * @return true if visible
	 */
	boolean isTracksVisible() {
		return tracksVisible;
	}
	
	/**
	 * Sets the map alpha (opacity).
	 * 
	 * @param alfa 0-255
	 */
	void setMapAlpha(int alfa) {
		mapAlpha = alfa % 256;
		getAxes().setInteriorBackground(new Color(255,255,255,255-mapAlpha));
		repaint();
	}
	
	@Override
	protected void paintFirst(Graphics g) {
		// fill the component with solid background color
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		
		// draw map
		if (map != null)
			map.draw(this, g);
		
		// draw the axes
		axes.draw(this, g);
	}
	
	@Override
	protected void paintLast(Graphics g) {
		// paint outside border so map not seen
		Rectangle rect = new Rectangle(0, 0, getWidth(), getHeight());
		Area area = new Area(rect);
		Rectangle plotRect = new Rectangle(getLeftGutter(), 
				getTopGutter(), 
				getWidth()-getRightGutter()-getLeftGutter()+1, 
				getHeight()-getBottomGutter()-getTopGutter()+1);
		area.subtract(new Area(plotRect));
		Graphics2D g2 = (Graphics2D)g;
		g.setColor(getBackground());
		g2.fill(area);
		
		// find origin and radius to the horizon
		Point o = originPt.getScreenPosition();			
		int centerX = o.x; // origin
		int centerY = o.y; // origin
		int r = xToPix(90) - centerX;
		
		// get sunCircle shape and fill rest with sunBlock fill color
		transform.setToTranslation(centerX, centerY);
		circle.setFrame(-r, -r, 2 * r, 2 * r);			
		Shape sunCircle = transform.createTransformedShape(circle);	
		area = new Area(plotRect);
		area.subtract(new Area(sunCircle));
		g.setColor(app.sunBlock.fillColor);
		g2.fill(area);

	}
	
	/**
	 * A Drawable class to draw the background
	 */
	protected class SunPlotBackground implements Drawable {
		
		@Override
		public void draw(DrawingPanel panel, Graphics g) {
			
			Graphics2D g2 = (Graphics2D) g;
	    Color gcolor = g.getColor();
			Font gfont = g2.getFont();
			
			Point o = originPt.getScreenPosition();			
			int centerX = o.x; // origin
			int centerY = o.y; // origin
			int r = Math.max(1, xToPix(90) - centerX);
			
			g2.setStroke(app.getStroke(1.5f));
			
			 // get plot limits
			int top = panel.getTopGutter();
			int h = panel.getBounds().height;
			int bottom = h - panel.getBottomGutter();
			int left = panel.getLeftGutter();
			int w = panel.getBounds().width;
			int right = w - panel.getRightGutter();
			
			// set up 90 degree circle to fill with paint
			transform.setToTranslation(centerX, centerY);
			circle.setFrame(-r, -r, 2 * r, 2 * r);			
			Shape horizonCircle = transform.createTransformedShape(circle);	
			
			// make radial paint
			Point2D center = new Point2D.Float(o.x, o.y);
	    Paint paint = new RadialGradientPaint(center, r, app.skyFractions, app.skyColors);
	    g2.setPaint(paint);
			g2.fill(horizonCircle);
	    						
			g.setColor(Color.LIGHT_GRAY);
			g2.setStroke(app.getStroke(1f));
			g2.draw(horizonCircle);
			
			// write compass points			
			g2.setColor(Color.BLACK);
			g2.setFont(g2.getFont().deriveFont(12f).deriveFont(Font.BOLD));
			char deg = FunctionEditor.DEGREES.charAt(0);
			char[] c = new char[] {'0', deg, ' ', 'N',};
			g2.drawChars(c, 0, c.length, centerX-11, top + 16);
			c = new char[] {'1', '8', '0', deg, ' ', 'S'};
			g2.drawChars(c, 0, c.length, centerX-18, bottom -6);
			c = new char[] {'9', '0', deg, ' ', 'E',};
			g2.drawChars(c, 0, c.length, right-32, centerY+4);
			c = new char[] {'-', '9', '0', deg, ' ', 'W'};
			g2.drawChars(c, 0, c.length, left+4, centerY+4);

			// draw camera
			r += 5 * getXPixPerUnit();
			double x = centerX + r * Math.sin(app.getCameraAz());
			double y = centerY - r * Math.cos(app.getCameraAz());
			transform.setToTranslation(x, y);
			transform.rotate(app.getCameraAz());
			Shape cam = transform.createTransformedShape(camera);	
			g.setColor(Color.CYAN.darker());
			g2.fill(cam);
			g2.draw(cam);
			
			// restore graphics
			g.setColor(gcolor);
			g2.setFont(gfont);
		}

	}
	
	/**
	 * A Dataset to draw the sun and reflection rays and tracks.
	 *
	 * @author Douglas Brown
	 */
	class SunDataset extends Dataset {
		
		SunDataset(Dataset xy) {
			super();
			setConnected(true);
			setLineColor(xy.getLineColor());
			append(xy.getXPoints(), xy.getYPoints());
			setMarkerShape(Dataset.NO_MARKER);
			setConnected(true);
		}
		
		@Override
		public void draw(DrawingPanel drawingPanel, Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			if (tracksVisible)
				drawTrack(drawingPanel, g2);
			drawMessages();
		}

		/**
		 * Draws a ray track across the sky, showing only sections that are
		 * not blocked by mountains.
		 * 
		 * @param drawingPanel the SunPlottingPanel
		 * @param g2 the Graphics2D
		 */
		protected void drawTrack(DrawingPanel drawingPanel, Graphics2D g2) {
			boolean isSun = this == sunDataset;
			
			// replace data with visible ray data only
			// may have multiple visible sections
			double[] x = getXPoints();
			double[] y = getYPoints();
			int startIndex = 0, len = 0;
			while (startIndex + len < x.length) {
				int n = startIndex + len;
				len = 0;
				startIndex = n-1;
				for (int i = n; i < x.length; i++) {
					if (!app.getRayVisibility(i)[isSun? 0: 1]) {
						if (startIndex > n)
							break;
						else 
							continue;
					}
					// visible ray
					len++;
					if (startIndex < n)
						startIndex = i;
				}
				if (len == 0)
					return;
				
				double[] xx = new double[len];
				double[] yy = new double[len];
				for (int i = 0; i < len; i++) {
					xx[i] = x[startIndex + i];
					yy[i] = y[startIndex + i];
				}
				set(xx, yy);
				
				if (isSun) {
					// draw wide yellow line
					g2.setStroke(app.getStroke(2));
					setLineColor(app.currentRayColors[0][0]);
					super.drawData(drawingPanel, g2);
					// draw narrow blue
					g2.setStroke(app.getStroke(0.5f));
					setLineColor(app.currentRayColors[0][1]);
					super.drawData(drawingPanel, g2);
				}
				else {
					g2.setStroke(app.getStroke(1));
					setLineColor(reflectionTrackColor);				
					super.drawData(drawingPanel, g2);							
				}
				// restore original data for next iteration
				set(x, y);
			}
			
		}
				
		/**
		 * Writes the current ray & insolation data in the message boxes
		 */
		protected void drawMessages() {
			boolean isSun = this == sunDataset;
			int t = app.when.getTimeIndex();
			
			// write ray data in lower message boxes
			double[][] rayData = app.getRayData(t);
			if (rayData == null)
				return;

			double az = rayData[isSun? 0: 1][0];
			double alt = rayData[isSun? 0: 1][1];
			String dataString = "";
			boolean[] vis = app.getRayVisibility(t);
			if (!vis[isSun? 0: 1]) {
				dataString = isSun? 
					alt < 0? "Sun is below the horizon":"Sun is blocked":
					"No reflection";
			}
			else if (isSun && !vis[1]) {
				dataString = "Sun hits back of the panel";
			}
			else {
				dataString = (isSun? "Sun:": "Reflection:") +" "+app.getAzaltString(az, alt);
			}
			setMessage(dataString, isSun? 0: 1);
		
			// write sun hours and insolation in upper message boxes
			if (isSun) {
				String date = app.when.getDateString();
				String time = app.when.getTimeString();
				double[] sunHrs = app.getTotalSunHours();
				double hrs = app.round(sunHrs[0]*10) / 10.0;
				setMessage(date+": sun hours "+hrs, 3);
				String insolation = time+": insolation "+percentFormat.format(app.getInsolation(t));
				setMessage(insolation, 2);
			}								
		}
		
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
			SunPlottingPanel plot = (SunPlottingPanel)obj;
			control.setValue("tracks_visible", plot.isTracksVisible());
			control.setValue("map", plot.map);
			control.setValue("map_alpha", plot.mapAlpha);
			control.setValue("tracks_visible", plot.tracksVisible);
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
			SunPlottingPanel plot = (SunPlottingPanel)obj;
			plot.setTracksVisible(control.getBoolean("tracks_visible"));
			plot.setMap((MapImage)control.getObject("map"));
			plot.setMapAlpha(control.getInt("map_alpha"));
			plot.setTracksVisible(control.getBoolean("tracks_visible"));
			plot.plot();
			return obj;
		}
	}

	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

}
