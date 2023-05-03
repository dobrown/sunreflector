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

import java.time.LocalDate;
import java.time.Year;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;

/**
 * This defines the day number and time index of the visible ray.
 *
 * @author Douglas Brown
 */
public class SunMoment {

	int timeIndex, dayNumber;
	double[] hours; // the actual times associated with timeIndex
	int startDay; // day of the year on which dayNumber data begins

	/**
	 * Gets a formatted time string for a decimal hour time
	 * 
	 * @param time the time in decimal hours
	 * @return formatted time string
	 */
	public static String getTimeString(double time) {		
		String ampm = time+.01 >= 12? "pm": "am";
		time = time+0.11 >= 13? time-12: time;
		String s = String.valueOf(time);
		int m = s.indexOf(".");
		int minutes = (int)(60*(time%1.0)+.1);
		String min = String.valueOf(minutes);
		if (min.length()==1)
			min = "0"+min;
		String timeStr = s.substring(0, m)+":"+min;
		return timeStr+" "+ampm;
	}
	
	/**
	 * Gets a formatted date string for a day of year
	 * 
	 * @param dayOfYear the day of the year
	 * @return formatted date string
	 */
	public static String getDateString(int dayOfYear) {
		Year y = Year.of(2023) ;
		LocalDate ld = y.atDay(dayOfYear);
		String s = ld.toString().substring(5);
		int mo = Integer.parseInt(s.substring(0, 2));
		String day = s.substring(3, 5);
		switch(mo) {
		case 1:
			return "Jan "+day;
		case 2:
			return "Feb "+day;
		case 3:
			return "Mar "+day;
		case 4:
			return "Apr "+day;
		case 5:
			return "May "+day;
		case 6:
			return "Jun "+day;
		case 7:
			return "Jul "+day;
		case 8:
			return "Aug "+day;
		case 9:
			return "Sep "+day;
		case 10:
			return "Oct "+day;
		case 11:
			return "Nov "+day;
		case 12:
			return "Dec "+day;
		}
		return s;
	}
	
	/**
	 * Default constructor used as placeholder when no sun data
	 */
	SunMoment() {
		this(1, null);
	}
	
	/**
	 * Constructor
	 * 
	 * @param startDayOfData the start day of the sun data 
	 * @param hrs array of times in decimal hours
	 */
	SunMoment(int startDayOfData, double[] hrs) {
		startDay = startDayOfData;
		setHours(hrs == null? new double[] {0}: hrs);
	}
	
	/**
	 * Sets the array of sun data decimal hours 
	 * 
	 * @param hrs array of times in decimal hours 
	 */
	void setHours(double[] hrs) {
		if (hrs != null)
			hours = hrs;		
	}
	
	/**
	 * Gets the current time index. 
	 * 
	 * @return the time index
	 */
	int getTimeIndex() {
		return timeIndex;
	}

	/**
	 * Sets the time index. 
	 * 
	 * @param index the index
	 */
	void setTimeIndex(int index) {
		timeIndex = Math.min(hours.length-1, index);
		timeIndex = Math.max(0, timeIndex);		
	}

	/**
	 * Gets the current time in decimal hours 
	 * 
	 * @return the decimal hour time 
	 */
	double getTime() {
		return hours[timeIndex];
	}

	/**
	 * Sets the time in decimal hours 
	 * 
	 * @param t the decimal hour time 
	 */
	void setTime(double t) {
		for (int i = 0; i < hours.length; i++) {
			if (hours[i] >= t) {
				setTimeIndex(i);
				break;
			}
		}
	}

	/**
	 * Gets the day number.
	 * 
	 * @return the day number 
	 */
	int getDayNumber() {
		return dayNumber;
	}
	
	/**
	 * Sets the day number.
	 * 
	 * @param dayNum the day number 
	 */
	void setDayNumber(int dayNum) {
		dayNumber = dayNum;
	}
	
	/**
	 * Gets the day of year.
	 * 
	 * @return the day of year
	 */
	int getDayOfYear() {
		return dayNumber + startDay;
	}
	
	/**
	 * Sets the day of year.
	 * 
	 * @param dayOfYear the day of year
	 */
	void setDayOfYear(int dayOfYear) {
		// ignore if this is a default moment
		if (startDay == 1 && hours.length == 1)
			return;
		dayNumber = dayOfYear - startDay;
		dayNumber = Math.max(0, dayNumber);
	}
	
	/**
	 * Gets a formatted String of the current time.
	 * 
	 * @return the time String
	 */
	public String getTimeString() {		
		double time = hours[timeIndex];
		return getTimeString(time);
	}
	
	/**
	 * Gets a formatted String of the current date.
	 * 
	 * @return the date String
	 */
	public String getDateString() {
		int dayOfYear = startDay + dayNumber;
		return getDateString(dayOfYear);
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
			SunMoment moment = (SunMoment)obj;
			control.setValue("time", moment.getTimeIndex()); //$NON-NLS-1$
			control.setValue("day", moment.getDayNumber()); //$NON-NLS-1$
		}

		/**
		 * Creates a new object. Not used for SunMoment, the moment
		 * is created by SunApp when reading text data
		 *
		 * @param control the XMLControl with the object data
		 * @return the newly created object, here null
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
			SunMoment moment = (SunMoment)obj;
			moment.setTimeIndex(control.getInt("time")); //$NON-NLS-1$
			moment.setDayNumber(control.getInt("day")); //$NON-NLS-1$
			return obj;
		}
	}

	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

}
