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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.opensourcephysics.display.DatasetManager;
import org.opensourcephysics.display.OSPRuntime;

/**
 * A class to read sun angles from the NOAA spreadsheet.
 * 
 * @author Douglas Brown
 */
public class NOAAReader {
	
  static final int ALTITUDE = 32, AZIMUTH = 33, TIME = 4;
  static final int DATE_ROW = 6, DATE_COL = 1;
  static final int LAT_ROW = 2, LONG_ROW = 3, ZONE_ROW = 4;
  static final int JAN_1_2023 = 44926; // NOAA's day numbering
	String noaaFile = "NOAA_Solar_Calculations.xls";
  
  SunApp app;
	FileInputStream inputStream;
	Workbook workbook;
  Sheet sheet; 
  int day, startDay;
  double startTime, endTime;
	String sunAngleData = "";
	// maps day to time list of {azimuth, altitude} in degrees
	TreeMap<Integer, ArrayList<double[]>> sunAngles = new TreeMap<Integer, ArrayList<double[]>>();
	ArrayList<Double> hoursOfDay = new ArrayList<Double>();
	DatasetManager sunData;
	String pathToNOAA;

	/**
	 * Constructor
	 * 
	 * @param app the SunApp
	 */
	NOAAReader(SunApp app) {
		this.app = app;
		String jarDir = OSPRuntime.getLaunchJarDirectory();
		pathToNOAA = jarDir + "/" + noaaFile;
	}
	
	/**
	 * Reads the NOAA spreadsheet and returns text data
	 * for the specified days and hours.
	 * 
	 * @param startDay the start day
	 * @param dayCount the day count
	 * @param startHour the start hour
	 * @param endHour the end hour
	 */
  String read(int startDay, int dayCount,
  		double startHour, double endHour) {
  	
  	this.startDay = startDay;
  	startTime = startHour;
  	endTime = endHour;
  	
	  if (!openWorkbook())
	  	return null;

		sunAngleData = "";
		StringBuffer buf = new StringBuffer();
		for (int j = 0; j < dayCount; j++) {
			day = j;
			ArrayList<double[]> dayData = readDay(day); // get data including reflections
			sunAngles.put(day, dayData);

			buf.delete(0, buf.length());
			int n = sunAngleData.indexOf("\n");
			if (n > 1) {
				String line = sunAngleData.substring(0, n);
				sunAngleData = sunAngleData.substring(n + 1);
				buf.append(line + "\t");
				buf.append("x" + day + "\ty" + day + "\n");
			}
			else {
				buf.append("t\t" +"x" + day + "\ty" + day + "\n");
			}
	
	    ArrayList<double[]> data = sunAngles.get(day);
	    for(int i = 0; i < data.size(); i++) {
	    	double[] next = data.get(i);
				n = sunAngleData.indexOf("\n");
				if (n > 1) {
					String line = sunAngleData.substring(0, n-1);
					sunAngleData = sunAngleData.substring(n + 1);
					buf.append(line + "\t");
			    buf.append(next[0] + "\t" + next[1] + "\n");					
				}
				else {
					double hr = hoursOfDay.get(i);
			    buf.append(hr + "\t" + next[0] + "\t" + next[1] + "\n");									
				}
	    }
			sunAngleData = buf.toString();
		}
		
		double[] loc = readLocation();
		
    closeWorkbook();
    
    String name = app.START_DAY + startDay + app.LATITUDE + loc[0]
    		+ app.LONGITUDE + loc[1] + app.ZONE + loc[2];
    
		return name + "\n" + buf.toString();		
  }
  
	/**
	 * Opens a workbook for access to the spreadsheet.
	 * 
	 * @return true if successful
	 */
  private boolean openWorkbook() {
  	if (workbook != null)
  		return true;
	  // create workbook instance
		try {
			inputStream = new FileInputStream(new File(pathToNOAA));  
			workbook = WorkbookFactory.create(inputStream);
		  sheet = workbook.getSheetAt(0); 	  
		} catch (Exception e) {
			return false;
		} 
		return true;
  }
  
	/**
	 * Closes the workbook
	 */
  private void closeWorkbook() {
  	if (workbook != null)
  		return;
    try {
			workbook.close();
	    inputStream.close();
	    workbook = null;
	    inputStream = null;
	    sheet = null;
		} catch (IOException e) {
		} 	
  }
  
	/**
	 * Writes text data to a file.
	 * 
	 * @param data the text data string
	 * @param filePath the path to write to
	 */
  String writeDataTo(String data, String filePath) {
		int n = filePath.lastIndexOf("/"); //$NON-NLS-1$
		if (n < 0) {
			n = filePath.lastIndexOf("\\"); //$NON-NLS-1$
		}
		if (n > 0) {
			String dir = filePath.substring(0, n + 1);
			File file = new File(dir);
			if (!file.exists() && !file.mkdir()) {
				return null;
			}
		}
		File file = null;
		try {
			file = new File(filePath);
			FileOutputStream stream = new FileOutputStream(file);
			Charset charset = Charset.forName("UTF-8"); //$NON-NLS-1$
			Writer out = new OutputStreamWriter(stream, charset);
			Writer output = new BufferedWriter(out);
			output.write(data);
			output.flush();
			output.close();
			out.close();
			stream.close();
		} catch (IOException ex) {
		}
		return file == null? null: file.getAbsolutePath();
  }
  
	/**
	 * Reads and returns one day of sun data.
	 * 
	 * @param day the day number
	 * @return ArrayList of {az, alt} sun positions at 0.1 hr intervals
	 */
	private ArrayList<double[]> readDay(int day) {
		ArrayList<double[]> data = new ArrayList<double[]>();
		
		// set day
	  Row row = sheet.getRow(DATE_ROW);
	  Cell dateCell = row.getCell(DATE_COL);
	  dateCell.setCellValue(day + startDay + JAN_1_2023);
	  HSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);

	  int n = sheet.getLastRowNum();
		for (int i = 1; i <= n; i++) {
		  row = sheet.getRow(i);
			// determine time of day
		  double hourOfDay = row.getCell(TIME).getNumericCellValue() * 24;
		  if (hourOfDay > endTime)
		  	break;
		  if (startTime - hourOfDay > 0.01)
		  	continue;
		  int k = (int)(Math.round(100*hourOfDay));
		  hourOfDay = k/100.0;
		  hoursOfDay.add(hourOfDay);
		  
			// add {azim, elev} to sun angle list
		  double[] angle = new double[2];
		  data.add(angle);
		  // cell values are in degrees
		  angle[0] = row.getCell(AZIMUTH).getNumericCellValue();  		  
		  angle[1] = row.getCell(ALTITUDE).getNumericCellValue();
		}
		return data;
	}

	/**
	 * Reads and returns the latitude, longitude and time zone of the
	 * NOAA spreadsheet.
	 * 
	 * @return {lat, long, timezone}
	 */
	double[] readLocation() {
		if (!openWorkbook())
			return null;
	  Row row = sheet.getRow(LAT_ROW);
	  double latitude = row.getCell(DATE_COL).getNumericCellValue();
	  row = sheet.getRow(LONG_ROW);
	  double longitude = row.getCell(DATE_COL).getNumericCellValue();
	  row = sheet.getRow(ZONE_ROW);
	  double timezone = row.getCell(DATE_COL).getNumericCellValue();
	  closeWorkbook();
		return new double[] {latitude, longitude, timezone};
	}

	/**
	 * Sets the latitude, longitude and time zone.
	 * 
	 * @param loc {lat, long, timezone}
	 */
	void setLatLongTimezone(double[] loc) {
		if (!openWorkbook())
			return;
		
		// set latitude
	  Row row = sheet.getRow(LAT_ROW);
	  Cell cell = row.getCell(DATE_COL);
	  cell.setCellValue(loc[0]);
		// set longitude
	  row = sheet.getRow(LONG_ROW);
	  cell = row.getCell(DATE_COL);
	  cell.setCellValue(loc[1]);
		// set time zone
	  row = sheet.getRow(ZONE_ROW);
	  cell = row.getCell(DATE_COL);
	  cell.setCellValue(loc[2]);
	}

}
