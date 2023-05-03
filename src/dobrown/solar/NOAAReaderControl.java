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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

/**
 * This is a dialog to control the NOAAReader.
 * 
 * @author Douglas Brown
 */
public class NOAAReaderControl extends JDialog {
	
	NOAAReader reader;
	
	JButton loadButton, closeButton;
	JLabel latLabel, longLabel, zoneLabel;
	JLabel startDateLabel, endDateLabel;
	JLabel startTimeLabel, endTimeLabel;

	JTextField latField, longField;
	JSpinner zoneSpinner;
	JSpinner startDateSpinner, endDateSpinner;
	JSpinner startTimeSpinner, endTimeSpinner;
	
	String[] dates, times;
	boolean ready; // true if NOAA spreadsheet is found
	
	NOAAReaderControl(NOAAReader reader) {
		super(reader.app.frame, true);
		this.reader = reader;
		
		setTitle("Load NOAA Solar Data");
		setResizable(false);
		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);
		
		JPanel locPanel = new JPanel(new GridLayout(0, 2));
		locPanel.setBorder(BorderFactory.createTitledBorder("Location"));
		
		Border labelBorder = BorderFactory.createEmptyBorder(4, 0, 4, 6);
		
		latLabel = new JLabel("Latitude");
		latLabel.setHorizontalAlignment(JLabel.RIGHT);
		latLabel.setBorder(labelBorder);
		latField = new JTextField(8);
		locPanel.add(latLabel);
		locPanel.add(latField);
		
		longLabel = new JLabel("Longitude");
		longLabel.setHorizontalAlignment(JLabel.RIGHT);
		longLabel.setBorder(labelBorder);
		longField = new JTextField(8);
		locPanel.add(longLabel);
		locPanel.add(longField);
		
		zoneLabel = new JLabel("Time Zone");
		zoneLabel.setHorizontalAlignment(JLabel.RIGHT);
		zoneLabel.setBorder(labelBorder);
		SpinnerModel zoneModel = new SpinnerNumberModel(0, -11, 12, 1); // init, min, max, step
		zoneSpinner = new JSpinner(zoneModel);
		locPanel.add(zoneLabel);
		locPanel.add(zoneSpinner);

		double[] loc = reader.readLocation();
		ready = loc != null;
		if (ready) {
//			latField.setText("" + loc[0]);
//			longField.setText("" + loc[1]);
//			zoneSpinner.setValue(reader.app.round(loc[2]));			
			latField.setText("" + reader.app.latitude);
			longField.setText("" + reader.app.longitude);
			zoneSpinner.setValue(reader.app.timeZone);			
		}
		
		dates = new String[365];
		for (int i = 0; i < dates.length; i++) {
			dates[i] = SunMoment.getDateString(i+1);
		}
		JPanel datePanel = new JPanel(new GridLayout(0, 2));
		datePanel.setBorder(BorderFactory.createTitledBorder("Date"));
		
		startDateLabel = new JLabel("Start");
		startDateLabel.setHorizontalAlignment(JLabel.RIGHT);
		startDateLabel.setBorder(labelBorder);
		SpinnerModel dateModel = new SpinnerListModel(dates);
		startDateSpinner = new JSpinner(dateModel);
		startDateSpinner.setValue(SunMoment.getDateString(reader.app.startDay));
		datePanel.add(startDateLabel);
		datePanel.add(startDateSpinner);
		
		endDateLabel = new JLabel("End");
		endDateLabel.setHorizontalAlignment(JLabel.RIGHT);
		endDateLabel.setBorder(labelBorder);
		dateModel = new SpinnerListModel(dates);
		endDateSpinner = new JSpinner(dateModel);
		int endDay = reader.app.startDay + reader.app.dayCount - 1;
		endDateSpinner.setValue(SunMoment.getDateString(endDay));
		datePanel.add(endDateLabel);
		datePanel.add(endDateSpinner);
		
		times = new String[240];
		for (int i = 0; i < times.length; i++) {
			times[i] = SunMoment.getTimeString(0.1 * i);
		}
		JPanel timePanel = new JPanel(new GridLayout(0, 2));
		timePanel.setBorder(BorderFactory.createTitledBorder("Time"));
		
		startTimeLabel = new JLabel("Start");
		startTimeLabel.setHorizontalAlignment(JLabel.RIGHT);
		startTimeLabel.setBorder(labelBorder);
		SpinnerModel timeModel = new SpinnerListModel(times);
		startTimeSpinner = new JSpinner(timeModel);
		startTimeSpinner.setValue(SunMoment.getTimeString(reader.app.startHour));
		timePanel.add(startTimeLabel);
		timePanel.add(startTimeSpinner);
		
		endTimeLabel = new JLabel("End");
		endTimeLabel.setHorizontalAlignment(JLabel.RIGHT);
		endTimeLabel.setBorder(labelBorder);
		timeModel = new SpinnerListModel(times);
		endTimeSpinner = new JSpinner(timeModel);
		endTimeSpinner.setValue(SunMoment.getTimeString(reader.app.endHour));
		timePanel.add(endTimeLabel);
		timePanel.add(endTimeSpinner);
		
		Box box = Box.createVerticalBox();
		box.add(locPanel);
		box.add(datePanel);
		box.add(timePanel);
		
		contentPane.add(box, BorderLayout.NORTH);
		
		loadButton = new JButton("Load");
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				reader.app.frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				loadSunData();
			}
		});
		// cancel button
		closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		JPanel buttonBar = new JPanel();
		buttonBar.add(loadButton);
		buttonBar.add(closeButton);
		contentPane.add(buttonBar, BorderLayout.SOUTH);
		
		pack();
		setLocationRelativeTo(reader.app.frame);
	}
	
	/**
	 * Gets the day of the year from a formatted date string.
	 * 
	 * @param date the date string
	 * @return the day of year
	 */
	private int getDayOfYear(String date) {
		for (int i = 0; i < dates.length; i++) {
			if (dates[i].equals(date))
				return i+1;
		}
		return 1;
	}
	
	/**
	 * Gets the time of day from a formatted time string.
	 * 
	 * @param time the time string
	 * @return the time of day in decimal hours
	 */
	private double getTimeOfDay(String time) {
		for (int i = 0; i < times.length; i++) {
			if (times[i].equals(time))
				return 0.1 * i;
		}
		return 12.0;
	}
	
	/**
	 * Loads data from the NOAA spreadsheet for parameters defined
	 * in this control. First, the latitude, longitude and time zone of
	 * the spreadsheet are set. Then data are read for the days and times.
	 */
	void loadSunData() {
		double[] loc = reader.readLocation();
		try {
			double lat = Double.parseDouble(latField.getText());
			lat = Math.min(85, lat);
			lat = Math.max(-85, lat);
			loc[0] = lat;
			double longi = Double.parseDouble(longField.getText());
			longi = Math.min(180, longi);
			longi = Math.max(-180, longi);
			loc[1] = longi;
			int zone = (int) zoneSpinner.getValue();
			loc[2] = zone;
		} catch (Exception ex) {
			
		}
		
		String s = (String)startDateSpinner.getValue();
		int startDay = getDayOfYear(s);
		s = (String)endDateSpinner.getValue();
		int endDay = getDayOfYear(s);
		s = (String)startTimeSpinner.getValue();
		double startT = getTimeOfDay(s);
		s = (String)endTimeSpinner.getValue();
		double endT = getTimeOfDay(s);
		
		reader.setLatLongTimezone(loc);				
		loadSunData(startDay, endDay, startT, endT);
	}
	
	/**
	 * Loads data from the NOAA spreadsheet for a span of days and times
	 * 
	 * @param startDay the start day
	 * @param endDay the end day
	 * @param startT the start time
	 * @param endT the end time
	 */
	void loadSunData(int startDay, int endDay, double startT, double endT) {
		int count = Math.max(1, endDay - startDay + 1);
		String textData = reader.read(startDay, count,
				startT, endT);
		reader.app.loadSunDataFromText(textData);
		reader.app.updateReflections();
		reader.app.refreshViews();
	}
	
}
