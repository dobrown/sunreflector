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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.tools.FunctionEditor;

/**
 * A control panel with day/time/reflector sliders and help text area.
 */
public class SunControlPanel extends JPanel {
	
	SunApp app;

	JSlider dateSlider, timeSlider;
	JLabel dateLabel, timeLabel;
	JSlider axisSlider, rotationSlider, dipSlider;
	JLabel axisLabel, rotationLabel, dipLabel;
	JTextArea info;
	TitledBorder dataTimeTitledBorder, tiltTitledBorder;
	
	/**
	 * Constructor
	 * @param app the SunApp
	 */
	SunControlPanel(SunApp app) {
		super(new BorderLayout());
		this.app = app;
		createGUI();
		refreshDisplay();
		info.setText(getInfo());		
	}
	
	/**
	 * Creates the GUI.
	 */
	void createGUI() {
		setBackground(app.plot.getBackground());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		dateLabel = new JLabel();
		prepLabel(dateLabel);
		
		dateSlider = new JSlider(0, app.dayCount-1, 0);
		dateSlider.setOpaque(false);
		dateSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (app.isLoading)
					return;
				int i = dateSlider.getValue();
				app.when.setDayNumber(i);
				refreshDisplay();
				app.refreshViews();
			}

		});
		
		timeLabel = new JLabel();
		prepLabel(timeLabel);
		
		timeSlider = new JSlider(0, app.when.hours.length-1, 0);
		timeSlider.setOpaque(false);
		timeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (app.isLoading)
					return;
				int i = timeSlider.getValue();
				app.when.setTimeIndex(i);
				refreshDisplay();
				app.refreshViews();
			}
		});
		
		axisLabel = new JLabel();
		prepLabel(axisLabel);
		
		axisSlider = new JSlider(-90, 90, 0);
		axisSlider.setOpaque(false);
		axisSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int i = axisSlider.getValue();
				app.reflector.setTiltAxis(Math.toRadians(i));
				refreshDisplay();
				app.updateReflections();
				app.refreshViews();
			}

		});
		axisSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				app.visibleRotationAxis = SunApp.AXIS_UP;
				app.refreshViews();
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				app.visibleRotationAxis = SunApp.AXIS_NONE;
				app.refreshViews();
			}
		});
		
		rotationLabel = new JLabel();
		prepLabel(rotationLabel);
		
		rotationSlider = new JSlider(-90, 90, 0);
		rotationSlider.setOpaque(false);
		rotationSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int i = rotationSlider.getValue();
				app.reflector.setTilt(Math.toRadians(i));
				refreshDisplay();
				app.updateReflections();
				app.refreshViews();
			}
		});
		rotationSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				app.visibleRotationAxis = SunApp.AXIS_TILT;
				app.refreshViews();
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				app.visibleRotationAxis = SunApp.AXIS_NONE;
				app.refreshViews();
			}
		});
		
		dipLabel = new JLabel();
		prepLabel(dipLabel);
		
		dipSlider = new JSlider(0, 90, 0);
		dipSlider.setOpaque(false);
		dipSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int i = dipSlider.getValue();
				app.reflector.setDip(Math.toRadians(i));
				refreshDisplay();
				app.updateReflections();
				app.refreshViews();
			}
		});
		dipSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				app.visibleRotationAxis = SunApp.AXIS_DIP;
				app.refreshViews();
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				app.visibleRotationAxis = SunApp.AXIS_NONE;
				app.refreshViews();
			}
		});	
		
		JPanel upper = new JPanel(new BorderLayout());
		upper.setBackground(app.plot.getBackground());
		add(upper, BorderLayout.NORTH);
		
		JPanel dateAndTimePanel = new JPanel(new GridLayout(0, 1, 0, 2));
		dateAndTimePanel.setBackground(app.plot.getBackground());
		dataTimeTitledBorder = BorderFactory.createTitledBorder("Date and time");
		dataTimeTitledBorder.setTitleJustification(TitledBorder.CENTER);
		dateAndTimePanel.setBorder(dataTimeTitledBorder);
		upper.add(dateAndTimePanel, BorderLayout.NORTH);
		
		Box box = Box.createHorizontalBox();
		box.add(dateLabel);
		box.add(dateSlider);
		box.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		dateAndTimePanel.add(box);
		box = Box.createHorizontalBox();
		box.add(timeLabel);
		box.add(timeSlider);
		dateAndTimePanel.add(box);

		JPanel pvPanel = new JPanel(new GridLayout(0, 1, 0, 2));
		pvPanel.setBackground(app.plot.getBackground());
		tiltTitledBorder = BorderFactory.createTitledBorder("Solar panel orientation");
		tiltTitledBorder.setTitleJustification(TitledBorder.CENTER);
		pvPanel.setBorder(tiltTitledBorder);
		upper.add(pvPanel, BorderLayout.SOUTH);
		
		box = Box.createHorizontalBox();
		box.add(axisLabel);
		box.add(axisSlider);
		box.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		pvPanel.add(box);
		box = Box.createHorizontalBox();
		box.add(rotationLabel);
		box.add(rotationSlider);
		pvPanel.add(box);
		box = Box.createHorizontalBox();
		box.add(dipLabel);
		box.add(dipSlider);
		pvPanel.add(box);
		
		info = new JTextArea();
		info.setLineWrap(true);
		info.setWrapStyleWord(true);
		TitledBorder title = BorderFactory.createTitledBorder("How to use");
		title.setTitleJustification(TitledBorder.CENTER);
		info.setBorder(title);
		JScrollPane scroller = new JScrollPane(info);
		add(scroller, BorderLayout.CENTER);
				
	}
	
	/**
	 * Sets size, border, etc for a slider JLabel
	 * 
	 * @param label the slider JLabel
	 */
	private void prepLabel(JLabel label) {
		Dimension labelSize = new Dimension(60, 10);
		label.setPreferredSize(labelSize);
		label.setMinimumSize(labelSize);
		label.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
		label.setHorizontalAlignment(JLabel.RIGHT);
	}
	
	/**
	 * Updates the controls and labels to reflect the current settings.
	 */
	void refreshDisplay() {
		int degrees = getDegrees(app.reflector.tiltAxisAzimuth);
		axisSlider.setValue(degrees);
		String s = "Turn " + degrees + FunctionEditor.DEGREES;
		axisLabel.setText(s);
		degrees = getDegrees(app.reflector.tilt);
		rotationSlider.setValue(degrees);
		s = "Tilt " + degrees + FunctionEditor.DEGREES;
		rotationLabel.setText(s);
		degrees = getDegrees(app.reflector.dip);
		dipSlider.setValue(degrees);
		s = "Dip " + degrees + FunctionEditor.DEGREES;
		dipLabel.setText(s);

		dateSlider.setMaximum(app.dayCount-1);
		timeSlider.setMaximum(app.when.hours.length-1);
		int t = app.when.getTimeIndex();
		dateSlider.setValue(app.when.getDayNumber());
		timeSlider.setValue(t);
		dateLabel.setText(app.when.getDateString());
		timeLabel.setText(app.when.getTimeString());
		dateSlider.setEnabled(app.sunAzaltData != null);
		timeSlider.setEnabled(app.sunAzaltData != null);
		dateLabel.setEnabled(app.sunAzaltData != null);
		timeLabel.setEnabled(app.sunAzaltData != null);
		dataTimeTitledBorder.setTitle(app.sunAzaltData != null? "Date and time": "No sun data loaded");
		repaint();
	}
	
	/**
	 * Convenience method to convert radians to int degrees
	 * 
	 * @param radians the radians
	 * @return the degrees
	 */
	int getDegrees(double radians) {
		return Math.round(Math.round(Math.toDegrees(radians)));
	}
	
	/**
	 * Gets the text for the info text area
	 * 
	 * @return the info
	 */
	String getInfo() {
		return "Sun Reflector simulates reflections from a solar panel."
				+ " Use the sliders above to turn, tilt and dip the panel."
				+ " Its orientation is described by the azimuth"
				+ " and altitude of its \"normal vector\", shown as a"
				+ " blue pin perpendicular to its surface. The azimuth"
				+ " is in degrees clockwise from North, and the altitude is in degrees"
				+ " above the horizon."
				+ "\n\n"
				+ "The overhead view at the upper right shows the solar panel,"
				+ " an incident sun ray, the reflected ray, and a large circular horizon."
				+ " The reflected rays are green when high in the sky and red when"
				+ " near the horizon where they are seen as glare."
				+ " Check \"Show tracks\" to see the ray tracks across the sky"
				+ " for the entire day."
				+ "\n\n"
				+ "The horizontal view at the lower right shows the solar panel"
				+ " from a viewpoint on the horizon (green arrow in the overhead view)."
				+ " Drag the mouse horizontally to move the viewpoint."
				+ "\n\n"
				+ "Sun Reflector also determines the \"insolation\" (light intensity)"
				+ " striking the panel, expressed as a percentage of the maximum."
				+ " The insolation approaches 100% for small incident angles between"
				+ " the sun ray and the normal vector."
				+ " The total insolation over the course of a day"
				+ " is reported as \"sun hours\"."
				+ "\n\n"
				+ "Mountains, trees and other obstacles that block the sun will reduce"
				+ " the sun hours. To model these obstacles, choose Mountains|Edit."
				+ "\n\n"
				+ "Sun positions are loaded from the spreadsheet"
				+ " \"NOAA_Solar_Calculations.xls.\" See"
				+ " https://gml.noaa.gov/grad/solcalc/calcdetails.html for more information."
				+ " By default, the latitude/longitude/time zone are those of"
				+ " the South Lake Tahoe CA region and the date/time ranges"
				+ " are May 1-Sep 30 and 6:00 am-8:00 pm."
				+ " Choose Sun|Load Data to load data for other locations or times."
				+ "\n\n"
				+ getMapInfo();
	}
	
	/**
	 * Gets info for the map
	 * 
	 * @return the info
	 */
	String getMapInfo() {
		if (app.plot.map == null) {
			return "Choose Map|Open Image or Map|Paste Image to display a map"
					+ " in the background of the overhead view.";
		}
		return "The default background in the overhead view is a"
				+ " Google map image of the South Lake Tahoe region."
				+ " Choose Map|Open Image or Map|Paste Image to change the map.";
	}
	
}