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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.OSPButton;
import org.opensourcephysics.tools.FunctionEditor;

/**
 * A control panel with day/time/reflector sliders and help text area.
 */
public class SunControlPanel extends JPanel {
	
	SunTab tab;

	JSlider dateSlider, timeSlider;
	JLabel dateLabel, timeLabel;
	JLabel latLabel, longLabel, timezoneLabel;
	JTextField latField, longField, timezoneField;
	JSlider axisSlider, rotationSlider, dipSlider;
	JLabel axisLabel, rotationLabel, dipLabel;
	JTextArea info;
	JLabel loadLabel;
	TitledBorder dataTimeTitledBorder, tiltTitledBorder;
	
	/**
	 * Constructor
	 * @param tab the SunApp
	 */
	SunControlPanel(SunTab tab) {
		super(new BorderLayout());
		this.tab = tab;
		createGUI();
		refreshDisplay();
		info.setText(getInfo());		
	}
	
	/**
	 * Creates the GUI.
	 */
	void createGUI() {
		setBackground(tab.plot.getBackground());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		
		loadLabel = new JLabel();
		Border empty = BorderFactory.createEmptyBorder(4, 4, 4, 7);
		Border smaller = BorderFactory.createEmptyBorder(2, 2, 2, 5);
		Border etched = BorderFactory.createEtchedBorder();
		Border combo = BorderFactory.createCompoundBorder(etched, smaller);
		loadLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				loadLabel.setBorder(combo);	
			}

			@Override
			public void mouseExited(MouseEvent e) {
				loadLabel.setBorder(empty);	
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					tab.frame.loadSunData();
					refreshDisplay();
				}
			}	
		});
		loadLabel.setBorder(empty);	
		
		dateLabel = new JLabel();
		prepLabel(dateLabel);
		
		dateSlider = new JSlider(0, tab.dayCount-1, 0);
		dateSlider.setOpaque(false);
		dateSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (tab.isLoading)
					return;
				int i = dateSlider.getValue();
				tab.when.setDayNumber(i);
				refreshDisplay();
				tab.refreshViews();
			}

		});
		
		timeLabel = new JLabel();
		prepLabel(timeLabel);
		
		timeSlider = new JSlider(0, tab.when.hours.length-1, 0);
		timeSlider.setOpaque(false);
		timeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (tab.isLoading)
					return;
				int i = timeSlider.getValue();
				tab.when.setTimeIndex(i);
				refreshDisplay();
				tab.refreshViews();
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
				tab.reflector.setTiltAxis(Math.toRadians(i));
				refreshDisplay();
				tab.updateReflections();
				tab.refreshViews();
			}

		});
		axisSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				tab.visibleRotationAxis = SunTab.AXIS_UP;
				tab.refreshViews();
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				tab.visibleRotationAxis = SunTab.AXIS_NONE;
				tab.refreshViews();
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
				tab.reflector.setTilt(Math.toRadians(i));
				refreshDisplay();
				tab.updateReflections();
				tab.refreshViews();
			}
		});
		rotationSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				tab.visibleRotationAxis = SunTab.AXIS_TILT;
				tab.refreshViews();
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				tab.visibleRotationAxis = SunTab.AXIS_NONE;
				tab.refreshViews();
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
				tab.reflector.setDip(Math.toRadians(i));
				refreshDisplay();
				tab.updateReflections();
				tab.refreshViews();
			}
		});
		dipSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				tab.visibleRotationAxis = SunTab.AXIS_DIP;
				tab.refreshViews();
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				tab.visibleRotationAxis = SunTab.AXIS_NONE;
				tab.refreshViews();
			}
		});	
		
		latLabel = new JLabel("latitude");
		latField = new JTextField(4);
		latField.setEditable(false);
		longLabel = new JLabel("longitude");
		longField = new JTextField(4);
		longField.setEditable(false);
		timezoneLabel = new JLabel("timezone");
		timezoneField = new JTextField(2);
		timezoneField.setEditable(false);
		
		
		JPanel upper = new JPanel(new BorderLayout());
		upper.setBackground(tab.plot.getBackground());
		add(upper, BorderLayout.NORTH);
		
		JPanel uppertop = new JPanel(new BorderLayout());
		uppertop.setBackground(tab.plot.getBackground());
		upper.add(uppertop, BorderLayout.NORTH);
		
		JPanel locPanel = new JPanel();
		
//		locPanel.add(loadButton);
		locPanel.add(latLabel);
		locPanel.add(latField);
		locPanel.add(longLabel);
		locPanel.add(longField);
		locPanel.add(loadLabel);
//		locPanel.add(timezoneLabel);
//		locPanel.add(timezoneField);
		locPanel.setBackground(tab.plot.getBackground());
		TitledBorder border = BorderFactory.createTitledBorder("Location");
		border.setTitleJustification(TitledBorder.CENTER);
		locPanel.setBorder(border);
		uppertop.add(locPanel, BorderLayout.NORTH);
		
		JPanel dateAndTimePanel = new JPanel(new GridLayout(0, 1, 0, 2));
		dateAndTimePanel.setBackground(tab.plot.getBackground());
		dataTimeTitledBorder = BorderFactory.createTitledBorder("Date and time");
		dataTimeTitledBorder.setTitleJustification(TitledBorder.CENTER);
		dateAndTimePanel.setBorder(dataTimeTitledBorder);
		uppertop.add(dateAndTimePanel, BorderLayout.SOUTH);
		
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
		pvPanel.setBackground(tab.plot.getBackground());
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
		loadLabel.setIcon(SunTab.openIcon);
		loadLabel.setToolTipText("Load sun data for another location or date/time range");
		// should use formatter here
		String lat = String.valueOf(tab.latitude);
		latField.setText(lat + FunctionEditor.DEGREES);
		String longi = String.valueOf(tab.longitude);
		longField.setText(longi + FunctionEditor.DEGREES);
		String zone = String.valueOf(tab.timeZone);
		timezoneField.setText(zone);
		
		int degrees = getDegrees(tab.reflector.tiltAxisAzimuth);
		axisSlider.setValue(degrees);
		String s = "Turn " + degrees + FunctionEditor.DEGREES;
		axisLabel.setText(s);
		degrees = getDegrees(tab.reflector.tilt);
		rotationSlider.setValue(degrees);
		s = "Tilt " + degrees + FunctionEditor.DEGREES;
		rotationLabel.setText(s);
		degrees = getDegrees(tab.reflector.dip);
		dipSlider.setValue(degrees);
		s = "Dip " + degrees + FunctionEditor.DEGREES;
		dipLabel.setText(s);

		dateSlider.setMaximum(tab.dayCount-1);
		timeSlider.setMaximum(tab.when.hours.length-1);
		int t = tab.when.getTimeIndex();
		dateSlider.setValue(tab.when.getDayNumber());
		timeSlider.setValue(t);
		dateLabel.setText(tab.when.getDateString());
		timeLabel.setText(tab.when.getTimeString());
		dateSlider.setEnabled(tab.sunAzaltData != null);
		timeSlider.setEnabled(tab.sunAzaltData != null);
		dateLabel.setEnabled(tab.sunAzaltData != null);
		timeLabel.setEnabled(tab.sunAzaltData != null);
		dataTimeTitledBorder.setTitle(tab.sunAzaltData != null? "Date and time": "No sun data loaded");
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
		return "Sun Reflector simulates sunlight incident on and"
				+ " reflecting from a solar panel."
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
				+ "A map image may be added to the overhead view to help"
				+ " evaluate possible glare. Drag to move the map"
				+ " or use the toolbar buttons to control its scale and opacity."
				+ " To replace the map, drag and drop an image file,"
				+ " paste an image from the clipboard,"
				+ " or click the open map button."
				+ "\n\n"
				+ "The horizontal view at the lower right shows the solar panel"
				+ " from a viewpoint on the horizon (green arrow in the overhead view)."
				+ " Drag the mouse horizontally to move the viewpoint."
				+ "\n\n"
				+ "The \"insolation\" is the intensity of the sunlight"
				+ " striking the panel at a given time"
				+ " expressed as a percentage of the maximum."
				+ " The insolation approaches 100% for small incident angles between"
				+ " the sun ray and the normal vector."
				+ " The total insolation over the course of a day"
				+ " is reported as \"sun hours\"."
				+ "\n\n"
				+ "At some locations the skyline may reduce"
				+ " the sun hours significantly. To account for this choose Edit|Skyline"
				+ " and create a skyline in the Skyline Editor."
				+ " Check \"Show skyline\" to draw the skyline"
				+ " and determine its effect on sun hours."
				+ "\n\n"
				+ "Sun data are loaded from the NOAA solar position calculator"
				+ " (bundled spreadsheet"
				+ " \"NOAA_Solar_Calculations.xls\")."
				+ " By default, the location is"
				+ " South Lake Tahoe CA and the date/time ranges"
				+ " are May 1-Sep 30 and 6:00 am-8:00 pm."
				+ " Click the open data button to load data for other locations or times.";
	}
	
	/**
	 * Gets info for the map
	 * 
	 * @return the info
	 */
	String getMapInfo() {
		if (tab.plot.map == null) {
			return "Drag and drop an image file, paste an image from the clipboard,"
					+ " or choose File|Open Map to add a map image"
					+ " to the background of the overhead view.";
		}
		return "The default background in the overhead view is a"
				+ " Google map image of the South Lake Tahoe region."
				+ " Drag and drop an image file, paste an image from the clipboard,"
				+ " or choose Map|Open Image to change the map image.";
	}
	
}
