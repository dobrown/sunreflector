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
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.ZipEntry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.OSPButton;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.JarTool;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.AsyncDialog;
import javajs.async.AsyncFileChooser;

/**
 * This is the JFrame for SunApp. It lays out the views and
 * handles the toolbar and menubar.
 * 
 * @author Douglas Brown
 */
public class SunFrame extends JFrame {
	
	SunApp app;
	
	JToolBar toolbar;
	JCheckBox showTracksCheckbox;	
	JButton zoomButton;
	
	JMenu fileMenu, sunDataMenu, mapMenu, sunBlockMenu, helpMenu;
	JMenuItem openItem, saveItem, saveAsItem, loadSunDataItem;
	JMenuItem openImageItem, pasteImageItem, closeImageItem;
	JMenuItem aboutItem;
	JMenu mapAlphaMenu;
	JMenuItem mapEditItem, shadeEditItem;
	JCheckBoxMenuItem sunBlockEnabledCheckbox;
	JSlider mapAlphaSlider;
	JLabel mapLabel, mapDragLabel;
	MenuListener menuListener;
	
	ArrayList<File> filesToZip = new ArrayList<File>();
	String tempDir;
	File myFile;


	/**
	 * Constructor
	 * 
	 * @param app the SunApp
	 */
	SunFrame(SunApp app) {
		this.app = app;
		createGUI();
		refreshDisplay();
	}
	
	/**
	 * Creates the GUI
	 */
	void createGUI() {
		
		setTitle("Sun Reflector");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(800, 800));
		setLocation(350, 0);
		
		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);
		
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(app.plot.getBackground());
		
		JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		leftRightSplit.setDividerSize(6);
		leftRightSplit.setResizeWeight(0.33);
//		leftRightSplit.setEnabled(false);
		getContentPane().add(leftRightSplit, BorderLayout.CENTER);
		
		JSplitPane topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		topBottomSplit.setDividerSize(3);
		topBottomSplit.setResizeWeight(0.95);
		topBottomSplit.setEnabled(false);
		
		leftRightSplit.setRightComponent(topBottomSplit);
		leftRightSplit.setLeftComponent(app.controls);
		
		JPanel plotPanel = new JPanel(new BorderLayout());
		plotPanel.add(app.plot, BorderLayout.CENTER);
		plotPanel.add(toolbar, BorderLayout.NORTH);

		topBottomSplit.setTopComponent(plotPanel);
		topBottomSplit.setBottomComponent(app.pvDrawingPanel);
		
		showTracksCheckbox = new JCheckBox();
		showTracksCheckbox.setOpaque(false);
		showTracksCheckbox.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 6));
		showTracksCheckbox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				app.plot.setTracksVisible(showTracksCheckbox.isSelected());
			}				
		});
		
		zoomButton = new OSPButton();
		zoomButton.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				MapImage map = app.plot.map;
				if (map != null) {
					app.plot.pvPanel.setInflation(0.2);
					app.plot.repaint();
					
					double scale = map.getScale();
					// determine current slider scale power
					double power = Math.log(scale) / Math.log(SunApp.ZOOM_FACTOR);
					int n = app.round(60 + power);
					n = Math.min(80, Math.max(0, n));
					
					JPopupMenu popup = new JPopupMenu();
					JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 0, 80, n);
					zoomSlider.addChangeListener(ev -> {
						int p = zoomSlider.getValue() - 60;
						double z = Math.pow(SunApp.ZOOM_FACTOR, p);
						map.setScaleWithFixedOrigin(z);
						app.plot.repaint();
			    });
			    popup.add(zoomSlider);
			    popup.show(zoomButton, -50, zoomButton.getHeight());
				}				
			}				
		});

		mapLabel = new JLabel("Map:");
		mapDragLabel = new JLabel("drag to move");
		
		buildToolbar();
		
		createMenuBar();
		pack();
		leftRightSplit.setDividerLocation(0.42);
		topBottomSplit.setDividerLocation(0.65);
	}
	
	/**
	 * Rebuilds the toobar.
	 */
	void buildToolbar() {
		toolbar.removeAll();
		toolbar.add(Box.createHorizontalStrut(2));
		toolbar.add(showTracksCheckbox);
		toolbar.add(Box.createHorizontalGlue());
		if (app.plot.map != null) {
			toolbar.add(mapLabel);
			toolbar.add(zoomButton);
			toolbar.add(mapDragLabel);
			toolbar.add(Box.createHorizontalStrut(8));
		}
	}
	
	/**
	 * Refreshes the frame controls to reflect the current settings.
	 */
	void refreshDisplay() {
		showTracksCheckbox.setSelected(app.plot.isTracksVisible());
		zoomButton.setIcon(app.zoomIcon);
		zoomButton.setToolTipText("Resize the map image");
		showTracksCheckbox.setText("Show tracks");
		showTracksCheckbox.setToolTipText("Show the sun and reflection sky tracks");

		sunBlockEnabledCheckbox.setSelected(app.sunBlock.isEnabled());
		closeImageItem.setEnabled(app.plot.map != null);
		mapAlphaMenu.setEnabled(app.plot.map != null);
		mapAlphaSlider.setValue(app.plot.mapAlpha);
		
		mapLabel.setEnabled(app.plot.map != null && app.plot.map.isVisible());
		mapDragLabel.setEnabled(app.plot.map != null && app.plot.map.isVisible());
		showTracksCheckbox.setEnabled(app.sunAzaltData != null);
		sunDataMenu.setToolTipText("<HTML><p>Current sun data:<br>   Latitude "+app.latitude+
				"<br>   Longitude "+app.longitude+"<br>Time zone "+app.timeZone+"</p>");

		saveItem.setEnabled(myFile != null);
		buildToolbar();
	}
		
  /**
   * Creates the menu bar.
   */
  protected void createMenuBar() {
  	int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();		
  	openItem = new JMenuItem("Open..."); //$NON-NLS-1$
		openItem.setAccelerator(KeyStroke.getKeyStroke('O', keyMask));
		openItem.addActionListener((e) -> {
			open();
		});

		saveItem = new JMenuItem("Save"); //$NON-NLS-1$
		saveItem.addActionListener((e) -> {
			if (myFile != null)
				save(myFile.getAbsolutePath());
		});
		
		saveAsItem = new JMenuItem("Save As..."); //$NON-NLS-1$
		saveAsItem.addActionListener((e) -> {
			saveAs();
		});
		
		fileMenu = new JMenu("File");
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.add(saveAsItem);
		
		loadSunDataItem = new JMenuItem("Load Data..."); //$NON-NLS-1$
		loadSunDataItem.addActionListener((e) -> {
			loadSunData();
			refreshDisplay();
		});
		sunDataMenu = new JMenu("Sun");
		sunDataMenu.add(loadSunDataItem);

		openImageItem = new JMenuItem("Open Image..."); //$NON-NLS-1$
		openImageItem.addActionListener((e) -> {
			openImage();
			refreshDisplay();
		});

		pasteImageItem = new JMenuItem("Paste Image"); //$NON-NLS-1$
		pasteImageItem.addActionListener((e) -> {
			pasteImage();
			refreshDisplay();
		});
		
		closeImageItem = new JMenuItem("Close Image"); //$NON-NLS-1$
		closeImageItem.addActionListener((e) -> {
			app.plot.setMap(null);
			app.plot.repaint();
			refreshDisplay();
		});
		
		mapAlphaMenu = new JMenu("Opacity"); //$NON-NLS-1$
		
		mapMenu = new JMenu("Map");
		mapMenu.add(openImageItem);
		mapMenu.add(pasteImageItem);
		mapMenu.add(closeImageItem);
		mapMenu.addSeparator();
		mapMenu.add(mapAlphaMenu);
		mapAlphaSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, app.plot.mapAlpha);
		mapAlphaSlider.addChangeListener(e -> {
      app.plot.setMapAlpha(mapAlphaSlider.getValue());
    });
    mapAlphaMenu.add(mapAlphaSlider);
    mapMenu.add(mapAlphaMenu);
		
		
		sunBlockEnabledCheckbox = new JCheckBoxMenuItem("Enabled"); //$NON-NLS-1$
		sunBlockEnabledCheckbox.setSelected(app.sunBlock.isEnabled());
		sunBlockEnabledCheckbox.addActionListener((e) -> {
			app.sunBlock.setEnabled(sunBlockEnabledCheckbox.isSelected());
		});
		shadeEditItem = new JMenuItem("Edit..."); //$NON-NLS-1$
		shadeEditItem.addActionListener((e) -> {
			app.sunBlock.setEnabled(true);
			app.sunBlock.edit();
			refreshDisplay();
			app.plot.repaint();
		});
    
		sunBlockMenu = new JMenu("Mountains");
		sunBlockMenu.add(sunBlockEnabledCheckbox);
		sunBlockMenu.add(shadeEditItem);
		
		helpMenu = new JMenu("Help"); //$NON-NLS-1$
		JMenuItem aboutItem = new JMenuItem("About..."); //$NON-NLS-1$
		aboutItem.addActionListener((e) -> {
			showAboutDialog();
		});
		helpMenu.add(aboutItem);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
    menuBar.removeAll();
    menuBar.add(fileMenu);
    menuBar.add(sunDataMenu);
    menuBar.add(mapMenu);
    menuBar.add(sunBlockMenu);
    menuBar.add(helpMenu);
    
		menuListener = new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
				boolean enable = false;
				try {
					enable = t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor);
					pasteImageItem.setEnabled(enable);
				} catch (Exception ex) {
				}
			}

			@Override
			public void menuDeselected(MenuEvent e) {}

			@Override
			public void menuCanceled(MenuEvent e) {}
			
		};
		mapMenu.addMenuListener(menuListener);
  }
  
	/**
	 * Opens a Sun Reflector zip file or warns if file is not valid.
	 */  
	public void open() {
		AsyncFileChooser chooser = OSPRuntime.getChooser();
		if (chooser == null) {
			return;
		}
		String chooserPath = (String)OSPRuntime.getPreference("file_chooser_directory");
		if (chooserPath != null) {
			chooser.setCurrentDirectory(new File(chooserPath));
		}
		chooser.setDialogTitle("Open");
		chooser.resetChoosableFileFilters();
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(SunApp.zipFileFilter);
		chooser.setFileFilter(SunApp.zipFileFilter);
		Runnable ok = new Runnable() {
			@Override
			public void run() {
				File file = chooser.getSelectedFile();
				OSPRuntime.setPreference("file_chooser_directory", file.getParent());
				OSPRuntime.savePreferences();
				String path = file.getAbsolutePath();
				
				// check for zip file and open the xml file inside, if any
				if (SunApp.zipFileFilter.accept(file)) {
					Map<String, ZipEntry> contents = ResourceLoader.getZipContents(path, true);
					if (contents != null) {
						for (String key: contents.keySet()) {
							ZipEntry entry = contents.get(key);
							String name = entry.getName();
							if (name != null && name.toLowerCase().endsWith(".xml")) {
								SunApp.zipFilePath = file.getAbsolutePath() + "!/";
								name = SunApp.zipFilePath + name;
								file = new File(name);
								break;
							}
						}
					}
				}
				XMLControlElement control = new XMLControlElement(file.getAbsolutePath());
				Class<?> type = control.getObjectClass();
				if (type == SunApp.class) {
					control.loadObject(app);
					myFile = chooser.getSelectedFile();
					refreshDisplay();
				}
				else {
					// show warning dialog
					JOptionPane.showMessageDialog(app.frame, 
							"\""+chooser.getSelectedFile().getName()+"\" is not a SunReflector file.", //$NON-NLS-1$
							"Error", //$NON-NLS-1$
							JOptionPane.WARNING_MESSAGE);
				}
			}			
		};
		chooser.showOpenDialog(this, ok, () -> {});
	}
	
	/**
	 * Saves a Sun Reflector zip with a file chooser.
	 */
	public void saveAs() {
		AsyncFileChooser chooser = OSPRuntime.getChooser();
		String chooserPath = (String)OSPRuntime.getPreference("file_chooser_directory");
		if (chooserPath != null) {
			chooser.setCurrentDirectory(new File(chooserPath));
		}
		chooser.setDialogTitle("Save As");
		chooser.resetChoosableFileFilters();
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(SunApp.zipFileFilter);
		chooser.setFileFilter(SunApp.zipFileFilter);
		
		Runnable ok = new Runnable() {
			@Override
			public void run() {
				File file = chooser.getSelectedFile();
				OSPRuntime.setPreference("file_chooser_directory", file.getParent());
				OSPRuntime.savePreferences();
				// check to see if file already exists
				org.opensourcephysics.display.OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
				String fileName = file.getAbsolutePath();
				if ((fileName == null) || fileName.trim().equals("")) {
					return;
				}
				String baseName = XML.stripExtension(fileName);
				String ext = XML.getExtension(fileName.toLowerCase());
				if (!"zip".equals(ext)) {
					fileName = baseName + ".zip";
					file = new File(fileName);
				}
				if (file.exists()) {
					int selected = JOptionPane.showConfirmDialog(null, "Replace existing " + file.getName() + "?",
							"Replace File", JOptionPane.YES_NO_CANCEL_OPTION);
					if (selected != JOptionPane.YES_OPTION) {
						return;
					}
				}
				save(fileName);
			}			
		};
		chooser.showSaveDialog(this, ok, () -> {});
		chooser.resetChoosableFileFilters();
	}
	
	/**
	 * Saves a Sun Reflector zip file to a specified path.
	 * 
	 * @param zipFilePath the path
	 */
	public void save(String zipFilePath) {
		String baseName = XML.stripExtension(XML.getName(zipFilePath));
		tempDir = getTempDirectory();
		filesToZip.clear();
		
		// write or copy MapImage images to temp directory, add to filesToZip list
		if (app.plot.map != null) {
			File imageFile = saveTempImage(app.plot.map, baseName+"_map.jpg");
			if (imageFile != null && imageFile.exists()) {
				app.plot.map.tempImageName = imageFile.getName();
				filesToZip.add(imageFile);
			}
		}		
		if (app.sunBlock.getMapImage() != null) {
			File imageFile = saveTempImage(app.sunBlock.getMapImage(), baseName+"_block.jpg");
			if (imageFile != null && imageFile.exists()) {
				app.sunBlock.getMapImage().tempImageName = imageFile.getName();
				filesToZip.add(imageFile);
			}
		}
		// write xml file to temp directory and add to filesToZip list
		XMLControl xml = new XMLControlElement(app);		
		File xmlFile = new File(tempDir, baseName+".xml");
		xml.write(xmlFile.getAbsolutePath());
		if (xmlFile.exists())
			filesToZip.add(xmlFile);
			
		// now zip the files in fileToZip list and save, then delete the tempDir
		File target = new File(zipFilePath);
		if (JarTool.compress(filesToZip, target, null)) {
			myFile = target;
			OSPRuntime.trigger(1000, (e) -> {
				ResourceLoader.deleteFile(new File(tempDir));
			});
		}
	}
	
	/**
	 * Opens a dialog to load new data from the NOAA spreadsheet
	 */
	public void loadSunData() {
		// show dialog to load sun data from NOAA
		NOAAReader reader = new NOAAReader(app);
		NOAAReaderControl control = new NOAAReaderControl(reader);
		if (control.ready)
			control.setVisible(true);
		else 
			// show warning dialog
			JOptionPane.showMessageDialog(app.frame, 
					"The spreadsheet \""+ XML.getName(reader.pathToNOAA)+"\" was not found.", //$NON-NLS-1$
					"File Not Found", //$NON-NLS-1$
					JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Saves an image to the current tempDir in preparation for saving zip
	 * 
	 * @param map the MapImage
	 * @param fileName a file name used when saving pasted images
	 * @return the saved File, or null if failed
	 */
	private File saveTempImage(MapImage map, String fileName) {
		if (map.getImage() == null) {
			map.tempImageName = null;
			return null;
		}
		boolean success = false;
		String path = map.getImagePath();
		if (path == null) {
			// this is a pasted image, so save it as jpg file			
			try {
				File targetFile = new File(tempDir, fileName);
				OutputStream stream = new BufferedOutputStream(
						new FileOutputStream(targetFile.getAbsolutePath()));
				javax.imageio.ImageIO.write(map.getImage(), "jpg", stream);
				stream.close();
				success = true;
				fileName = targetFile.getAbsolutePath();
			} catch (Exception e) {
			}
		}
		else {
			// image loaded from file
			File targetFile = new File(tempDir, XML.getName(path));
			success = copyOrExtractFile(path, targetFile);
			fileName = targetFile.getAbsolutePath();
		}		
		return success? new File(fileName): null;
	}

	/**
	 * Opens a map image using a file chooser.
	 */
	public void openImage() {
		AsyncFileChooser chooser = OSPRuntime.getChooser();
		if (chooser == null) {
			return;
		}
		String chooserPath = (String)OSPRuntime.getPreference("file_chooser_directory");
		if (chooserPath != null) {
			chooser.setCurrentDirectory(new File(chooserPath));
		}
		chooser.setDialogTitle("Open Map Image");
		chooser.resetChoosableFileFilters();
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(SunApp.imageFileFilter);
		chooser.setFileFilter(SunApp.imageFileFilter);
		Runnable ok = new Runnable() {
			@Override
			public void run() {
				File file = chooser.getSelectedFile();
				OSPRuntime.setPreference("file_chooser_directory", file.getParent());
				OSPRuntime.savePreferences();
				String path = file.getAbsolutePath();
				SunPlottingPanel plot = app.plot;
				MapImage map = new MapImage(path);
				if (map.getImagePath() != null) {
					plot.setMap(map);
					plot.repaint();
				}
				else {
					// show warning dialog
					JOptionPane.showMessageDialog(app.frame, 
							"\""+chooser.getSelectedFile().getName()+"\" is not a readable image file.", //$NON-NLS-1$
							"Error", //$NON-NLS-1$
							JOptionPane.WARNING_MESSAGE);
				}
			}			
		};
		chooser.showOpenDialog(this, ok, null);
	}

	/**
	 * Pastes a map image from the clipboard.
	 */
	public void pasteImage() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
				Image image = (Image) t.getTransferData(DataFlavor.imageFlavor);
				if (image != null) {
					MapImage map = new MapImage(image);
					app.plot.setMap(map);
					app.plot.repaint();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Shows the About dialog
	 */
	public void showAboutDialog() {
		String date = OSPRuntime.getLaunchJarBuildDate();		
		String aboutString = "Sun Reflector " + app.version + "\nBuild date " + date + "\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "Douglas Brown, author\nBuilt with Open Source Physics"; //$NON-NLS-1$
		new AsyncDialog().showMessageDialog(this, 
				aboutString, 
				"About Sun Reflector", 
				(e) -> {});
	}

	/**
	 * Copies, downloads or extracts a file to a target.
	 * 
	 * @param filePath   the path
	 * @param targetFile the target file
	 * @return true if successful
	 */
	private boolean copyOrExtractFile(String filePath, File targetFile) {
		String lowercase = filePath.toLowerCase();
		// if file is on server, download it
		if (ResourceLoader.isHTTP(filePath)) {
			targetFile = ResourceLoader.download(filePath, targetFile, false);
		}
		// if file is in zip or jar, then extract it
		else if (lowercase.contains("trz!") || lowercase.contains("jar!") || lowercase.contains("zip!")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			targetFile = ResourceLoader.extract(filePath, targetFile);
		}
		// otherwise copy it
		else {
			Resource res = ResourceLoader.getResource(filePath);
			if (res.getFile() != null) {
				ResourceLoader.copyFile(res.getFile(), targetFile);
			}
		}
		return targetFile.exists();
	}


	/**
	 * Gets the temp directory. Creates a new one if none.
	 * 
	 * @return the tempDir
	 */
	private String getTempDirectory() {
		if (tempDir == null) {
			try {
				Path p = Files.createTempDirectory("sunreflect-", new FileAttribute<?>[0]);
				tempDir = p.toString();
			} catch (IOException e) {
			}
		}
		return tempDir;
	}

}
