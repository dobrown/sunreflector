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
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.undo.UndoManager;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.JarTool;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;
import javajs.async.AsyncDialog;
import javajs.async.AsyncFileChooser;

/**
 * This is the JFrame for SunReflector. It lays out the views and
 * handles the toolbar and menubar.
 * 
 * @author Douglas Brown
 */
public class SunFrame extends JFrame {
	
  static String version = "1.1.1";
  static int nameSuffix = 0;
	
	JTabbedPane tabbedPane;
	int prevSelectedTabIndex = -1;
	
	JMenu fileMenu, editMenu, helpMenu;
	JMenuItem newTabItem, closeTabItem, exitItem;
	JMenuItem openItem, saveItem, saveAsItem, loadSunDataItem;
	JMenuItem pasteImageItem;
	JMenuItem aboutItem, undoItem, redoItem;
	JMenuItem skylineEditItem;
	MenuListener menuListener;
	
	ArrayList<File> filesToZip = new ArrayList<File>();
	String tempDir;
	
	/**
	 * Constructor
	 * 
	 * @param tab the SunTab
	 */
	SunFrame() {
		createGUI();
		pack();
		enableDragNDrop();
		refreshDisplay();
	}
	
	void addTab(SunTab tab, String title) {
		synchronized (tabbedPane) {
			tab.frame = this;
			if (title == null || title.trim().equals(""))
				title = "untitled " + nameSuffix++;
			tabbedPane.addTab(title, new SunTabPanel(tab));
		}

	}
	
	void removeTab(int i) {
		if (!saveChanges(i))
			return;
		SunTab tab = getTab(i);
		tab.frame = null;
		if (tab.sunBlock.editor != null) {
			tab.sunBlock.editor.setVisible(false);
			tab.sunBlock.editor.dispose();
		}
		synchronized (tabbedPane) {
			tabbedPane.remove(i);
		}

	}
	
	void removeSelectedTab() {
		int i = tabbedPane.getSelectedIndex();
		removeTab(i);
	}
	
	int getTabIndex(String name) {
		synchronized (tabbedPane) {
			for (int i = tabbedPane.getTabCount(); --i >= 0;) {
				String title = tabbedPane.getTitleAt(i);
				if (name.equals(title)) {
					return i;
				}
			}
		}
		return -1;
	}
	
	int getTabIndex(File file) {
		String path = XML.forwardSlash(file.getAbsolutePath());
		synchronized (tabbedPane) {
			for (int i = tabbedPane.getTabCount(); --i >= 0;) {
				SunTabPanel next = ((SunTabPanel) tabbedPane.getComponentAt(i));
				String nextPath = XML.forwardSlash(next.tab.myFile.getAbsolutePath());
				if (path.equals(nextPath)) {
					return i;
				}
			}
		}
		return - 1;
	}
			
	int getTabIndex(SunTab tab) {
		synchronized (tabbedPane) {
			for (int i = tabbedPane.getTabCount(); --i >= 0;) {
				SunTabPanel next = ((SunTabPanel) tabbedPane.getComponentAt(i));
				if (next.tab == tab) {
					return i;
				}
			}
		}
		return - 1;
	}
			
	SunTabPanel getTabPanel(int i) {
		if (i < 0 || i >= tabbedPane.getTabCount())
			return null;
		return ((SunTabPanel) tabbedPane.getComponentAt(i));
	}
				
	SunTabPanel getSelectedTabPanel() {
		int i = tabbedPane.getSelectedIndex();
		return getTabPanel(i);
	}
				
	SunTab getTab(int i) {
		SunTabPanel panel = getTabPanel(i);
		return panel == null? null: panel.tab;
	}
				
	SunTab getSelectedTab() {
		SunTabPanel panel = getSelectedTabPanel();
		return panel == null? null: panel.tab;
	}
				
	/**
	 * main method, params unused
	 */
	public static void main(String[] params) {
		SunFrame frame = new SunFrame();
		SunTab tab = new SunTab();
		tab.isLoading = true;
		frame.addTab(tab, null);
		String textData = ResourceLoader.getString(SunTab.RESOURCE_PATH + SunTab.defaultSunData);
		tab.loadSunDataFromText(textData); // creates new SunMoment if successful		
		tab.updateReflections();
		tab.when.setDayOfYear(SunTab.DEFAULT_DAY_OF_YEAR);
		tab.when.setTime(SunTab.DEFAULT_HOUR);
		BufferedImage image = ResourceLoader.getBufferedImage(
				SunTab.RESOURCE_PATH + SunPlottingPanel.defaultMapImage); 
		if (image != null) {
			String path = SunTab.RESOURCE_PATH + SunPlottingPanel.defaultMapImage;
			MapImage map = new MapImage(image, path);
			tab.plot.setMap(map, false);
			tab.plot.getMap().setOrigin(
					SunPlottingPanel.defaultMapOrigin[0], 
					SunPlottingPanel.defaultMapOrigin[1]);
			tab.plot.getMap().setScaleWithFixedOrigin(
					SunPlottingPanel.defaultMapScale);			
		}
		tab.refreshViews();
		tab.isLoading = false;
		frame.pack();
		frame.refreshDisplay();
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);				
				for (int i = 0; i < frame.tabbedPane.getTabCount(); i++) {
					if (!frame.saveChanges(i))
						frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				}
			}
			@Override
			public void windowClosed(WindowEvent e) {
				if (frame.getDefaultCloseOperation() == JFrame.DO_NOTHING_ON_CLOSE)
					frame.setVisible(true);
			}
		});
	}
	
	
	/**
	 * Creates the GUI
	 */
	void createGUI() {
		
		setTitle("Sun Reflector");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(750, 800));
		setLocation(350, 0);
		
		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);
		tabbedPane = new JTabbedPane(SwingConstants.BOTTOM);
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (prevSelectedTabIndex > -1 && prevSelectedTabIndex < tabbedPane.getTabCount()) {
					SunTab tab = getTab(prevSelectedTabIndex);
					if (tab.sunBlock.editor != null) {
						tab.sunBlock.editor.wasVisible = tab.sunBlock.editor.isVisible();
						tab.sunBlock.editor.setVisible(false);
					}
					
				}
				int n = tabbedPane.getSelectedIndex();
				SunTab tab = getTab(n);
				if (tab == null)
					return;
				if (tab.sunBlock.editor != null) {
					tab.sunBlock.editor.setVisible(tab.sunBlock.editor.wasVisible);
				}
				prevSelectedTabIndex = n;
			}
		});
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (OSPRuntime.isPopupTrigger(e)) {
					int i = tabbedPane.getSelectedIndex();
					String name = tabbedPane.getTitleAt(i);
					JPopupMenu popup = new JPopupMenu();
					JMenuItem closeItem = new JMenuItem("Close \""+name+"\"");
					closeItem.addActionListener((ev) -> {
						removeTab(i);
					});
					popup.add(closeItem);
					popup.show(tabbedPane, e.getX(), e.getY());
				}
			}
		});
		
		createMenuBar();
	}
	
	/**
	 * Refreshes the display to reflect the current settings.
	 */
	void refreshDisplay() {
		SunTabPanel panel = getSelectedTabPanel();
		if (panel != null)
			panel.refreshDisplay();
	}
		
  /**
   * Creates the menu bar.
   */
  protected void createMenuBar() {
		fileMenu = new JMenu("File");
		editMenu = new JMenu("Edit");
		helpMenu = new JMenu("Help"); //$NON-NLS-1$

  	@SuppressWarnings("deprecation")
		int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();		
  	newTabItem = new JMenuItem("New Tab"); //$NON-NLS-1$
  	newTabItem.setAccelerator(KeyStroke.getKeyStroke('N', keyMask));
  	newTabItem.addActionListener((e) -> {
			SunTab tab = new SunTab();
			tab.isLoading = true;
			addTab(tab, null);
			String textData = ResourceLoader.getString(SunTab.RESOURCE_PATH + SunTab.defaultSunData);
			tab.loadSunDataFromText(textData); // creates new SunMoment if successful		
			tab.updateReflections();
			tab.when.setDayOfYear(SunTab.DEFAULT_DAY_OF_YEAR);
			tab.when.setTime(SunTab.DEFAULT_HOUR);
			tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
			tab.refreshViews();
			tab.isLoading = false;
		});

  	openItem = new JMenuItem("Open Tab..."); //$NON-NLS-1$
		openItem.setAccelerator(KeyStroke.getKeyStroke('O', keyMask));
		openItem.addActionListener((e) -> {
			open();
		});

  	closeTabItem = new JMenuItem("Close"); //$NON-NLS-1$
  	closeTabItem.addActionListener((e) -> {
			removeSelectedTab();
		});

  	saveItem = new JMenuItem("Save"); //$NON-NLS-1$
		saveItem.setAccelerator(KeyStroke.getKeyStroke('S', keyMask));
		saveItem.addActionListener((e) -> {
			SunTab tab = getSelectedTab();
			if (tab != null) {
				if (tab.myFile != null)
					save(tab.myFile.getAbsolutePath());
				else
					saveAs();
			} 
		});
		
		saveAsItem = new JMenuItem("Save Tab As..."); //$NON-NLS-1$
		saveAsItem.addActionListener((e) -> {
			saveAs();
		});
		
  	exitItem = new JMenuItem("Exit"); //$NON-NLS-1$
  	exitItem.addActionListener((e) -> {
			for (int i = 0; i < tabbedPane.getTabCount(); i++) {
				if (!saveChanges(i))
					return;
			}
			System.exit(0);
		});

		fileMenu.add(newTabItem);
		fileMenu.addSeparator();
		fileMenu.add(openItem);
		fileMenu.add(closeTabItem);
		fileMenu.addSeparator();
		fileMenu.add(saveItem);
		fileMenu.add(saveAsItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		
		// add undo and redo items
		undoItem = new JMenuItem("Undo");
		editMenu.add(undoItem);
		undoItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SunTab tab = getSelectedTab();
				if (tab != null) {
					tab.tabPanel.undoManager.undo();
					tab.tabPanel.refreshDisplay();
					tab.plot.repaint();
				}
			}

		});
		redoItem = new JMenuItem("Redo");
		editMenu.add(redoItem);
		redoItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SunTab tab = getSelectedTab();
				if (tab != null) {
					tab.tabPanel.undoManager.redo();
					tab.tabPanel.refreshDisplay();
					tab.plot.repaint();
				}
			}
		});
		
		pasteImageItem = new JMenuItem("Paste Map"); //$NON-NLS-1$
		pasteImageItem.setAccelerator(KeyStroke.getKeyStroke('V', keyMask));
		pasteImageItem.addActionListener((e) -> {
			pasteImage();
			refreshDisplay();
		});
		
		skylineEditItem = new JMenuItem("Skyline..."); //$NON-NLS-1$
		skylineEditItem.setAccelerator(KeyStroke.getKeyStroke('D', keyMask));
		skylineEditItem.addActionListener((e) -> {
			SunTab tab = getSelectedTab();
			if (tab != null) {
				tab.sunBlock.setEnabled(true);
				tab.sunBlock.edit();
				tab.tabPanel.refreshDisplay();
				tab.plot.repaint();
			}
		});
    		
		loadSunDataItem = new JMenuItem("Sun Data..."); //$NON-NLS-1$
		loadSunDataItem.addActionListener((e) -> {
			loadSunData();
			refreshDisplay();
		});
		
		editMenu.add(undoItem);
		editMenu.add(redoItem);
		editMenu.addSeparator();
		editMenu.add(pasteImageItem);
		editMenu.addSeparator();
		editMenu.add(skylineEditItem);
		editMenu.addSeparator();
		editMenu.add(loadSunDataItem);

		JMenuItem aboutItem = new JMenuItem("About..."); //$NON-NLS-1$
		aboutItem.addActionListener((e) -> {
			showAboutDialog();
		});
		helpMenu.add(aboutItem);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
    menuBar.removeAll();
    menuBar.add(fileMenu);
    menuBar.add(editMenu);
    menuBar.add(helpMenu);
    
		menuListener = new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				boolean canPaste = false;
				Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
				try {
					canPaste = t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor);
				} catch (Exception ex) {
				}
				pasteImageItem.setEnabled(canPaste);

				int i = tabbedPane.getSelectedIndex();
				boolean b = i >= 0;
				saveItem.setText("Save "+(!b? "": "\"" + tabbedPane.getTitleAt(i)+ "\""));
				saveItem.setEnabled(b);
				closeTabItem.setText("Close "+(!b? "": "\"" + tabbedPane.getTitleAt(i)+ "\""));
				closeTabItem.setEnabled(b);
				saveAsItem.setEnabled(b);
				loadSunDataItem.setEnabled(b);
				skylineEditItem.setEnabled(b);
				
				SunTab tab = getSelectedTab();
				if (tab != null) {
					UndoManager undo = tab.tabPanel.undoManager;
					undoItem.setEnabled(undo.canUndo());
					redoItem.setEnabled(undo.canRedo());
					undoItem.setText(undo.canUndo()?
							undo.getUndoPresentationName():
							"Undo");
					redoItem.setText(undo.canRedo()?
							undo.getRedoPresentationName():
							"Redo");
				}
			}

			@Override
			public void menuDeselected(MenuEvent e) {}

			@Override
			public void menuCanceled(MenuEvent e) {}
			
		};
		editMenu.addMenuListener(menuListener);
		fileMenu.addMenuListener(menuListener);
  }
  
	/**
	 * Offers to save changes to the tab at the specified index.
	 *
	 * @param i the tab index
	 * @return true unless canceled by the user
	 */
	protected boolean saveChanges(int i) {
		SunTab tab = getTab(i);
		if (!tab.changed) {
			return true;
		}
		String name = tabbedPane.getTitleAt(i);
		int selected = JOptionPane.showConfirmDialog(this, "The tab " + //$NON-NLS-1$
				" \"" + name + "\" has changed.\n" +
				"Do you wish to save the changes?", //$NON-NLS-1$ //$NON-NLS-2$
				"Save Changes", //$NON-NLS-1$
				JOptionPane.YES_NO_CANCEL_OPTION);
		if (selected == JOptionPane.CANCEL_OPTION) {
			return false;
		}
		if (selected == JOptionPane.YES_OPTION) {
			String path = tab.myFile == null? null: tab.myFile.getAbsolutePath();
			if (saveTab(tab, path) == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Opens a Sun Reflector tab (zip file) or warns if file is not valid.
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
		chooser.addChoosableFileFilter(SunTab.zipFileFilter);
		chooser.setFileFilter(SunTab.zipFileFilter);
		Runnable ok = new Runnable() {
			@Override
			public void run() {
				File file = chooser.getSelectedFile();
				OSPRuntime.setPreference("file_chooser_directory", file.getParent());
				OSPRuntime.savePreferences();
				open(file);
			}			
		};
		chooser.showOpenDialog(this, ok, () -> {});
	}
	
	/**
	 * Opens a Sun Reflector zip or image file or warns if file is not valid.
	 */  
	public void open(File file) {
		File theFile = file;
		// check for zip file and open the xml file inside, if any
		if (SunTab.zipFileFilter.accept(file)) {
			String path = file.getAbsolutePath();
			path = XML.forwardSlash(path);
			Map<String, ZipEntry> contents = ResourceLoader.getZipContents(path, true);
			if (contents != null) {
				for (String key: contents.keySet()) {
					ZipEntry entry = contents.get(key);
					String name = entry.getName();
					if (name != null && name.toLowerCase().endsWith(".xml")) {
						SunTab.zipFilePath = file.getAbsolutePath() + "!/";
						name = SunTab.zipFilePath + name;
						file = new File(name);
						break;
					}
				}
			}
			path = XML.forwardSlash(file.getAbsolutePath());
			XMLControlElement control = new XMLControlElement(path);
			Class<?> type = control.getObjectClass();
			if (type == SunTab.class) {
				SunTab tab = (SunTab)control.loadObject(null);
				tab.myFile = theFile;
				String zipName = SunTab.zipFilePath.substring(0, SunTab.zipFilePath.length() - 2);
				zipName = XML.forwardSlash(zipName);
				int n = zipName.lastIndexOf("/");
				zipName = zipName.substring(n+1, zipName.length());
				addTab(tab, zipName);
				tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
				refreshDisplay();
			}
			else {
				// show warning dialog
				JOptionPane.showMessageDialog(this, 
						"\""+theFile.getName()+"\" is not a SunReflector tab file.", //$NON-NLS-1$
						"Error", //$NON-NLS-1$
						JOptionPane.WARNING_MESSAGE);
			}
		}
		else if (SunTab.imageFileFilter.accept(file)){
			String path = file.getAbsolutePath();
			SunTab tab = getSelectedTab();
			if (tab != null) {
				SunPlottingPanel plot = tab.plot;
				MapImage map = new MapImage(path);
				if (map.getImagePath() != null) {
					plot.setMap(map, true);
					tab.changed = true;
					plot.repaint();
					tab.tabPanel.refreshDisplay();
				}
				else {
					// show warning dialog
					JOptionPane.showMessageDialog(this, 
							"\""+theFile.getName()+"\" is not a readable image file.", //$NON-NLS-1$
							"Error", //$NON-NLS-1$
							JOptionPane.WARNING_MESSAGE);
				}
			}
		}
		else {
			// show warning dialog
			JOptionPane.showMessageDialog(this, 
					"\""+theFile.getName()+"\" is not a SunReflector file.", //$NON-NLS-1$
					"Error", //$NON-NLS-1$
					JOptionPane.WARNING_MESSAGE);
			
		}
	}
	
	/**
	 * Saves a Sun Reflector zip with a file chooser.
	 * 
	 * @return the save path, or null if cancelled
	 */
	public String saveAs() {
		String[] path = new String[] {null};
		AsyncFileChooser chooser = OSPRuntime.getChooser();
		String chooserPath = (String)OSPRuntime.getPreference("file_chooser_directory");
		if (chooserPath != null) {
			chooser.setCurrentDirectory(new File(chooserPath));
		}
		chooser.setDialogTitle("Save As");
		chooser.resetChoosableFileFilters();
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(SunTab.zipFileFilter);
		chooser.setFileFilter(SunTab.zipFileFilter);
		
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
				path[0] = save(fileName);
			}			
		};
		chooser.showSaveDialog(this, ok, () -> {});
		chooser.resetChoosableFileFilters();
		return path[0];
	}
	
	/**
	 * Saves a Sun Reflector tab to a specified path.
	 * 
	 * @param zipFilePath the path
	 */
	public String save(String zipFilePath) {
		SunTab tab = getSelectedTab();
		return saveTab(tab, zipFilePath);
	}
	
	/**
	 * Saves a Sun Reflector tab to a specified path.
	 * 
	 * @param zipFilePath the path
	 */
	public String saveTab(SunTab tab, String zipFilePath) {
		if (tab == null)
			return null;
		if (zipFilePath == null)
			return saveAs();

		String baseName = XML.stripExtension(XML.getName(zipFilePath));
		tempDir = getTempDirectory();
		filesToZip.clear();
		
		// write or copy MapImage images to temp directory, add to filesToZip list
		if (tab.plot.map != null) {
			File imageFile = saveTempImage(tab.plot.map, baseName+"_map.jpg");
			if (imageFile != null && imageFile.exists()) {
				tab.plot.map.tempImageName = imageFile.getName();
				filesToZip.add(imageFile);
			}
		}		
		for (int i = 0; i < tab.sunBlock.images.size(); i++) {
			MapImage img = tab.sunBlock.images.get(i);
			File imageFile = saveTempImage(img, baseName+"_block.jpg");
			if (imageFile != null && imageFile.exists()) {
				img.tempImageName = imageFile.getName();
				filesToZip.add(imageFile);
			}
		}
		// write xml file to temp directory and add to filesToZip list
		XMLControl xml = new XMLControlElement(tab);		
		File xmlFile = new File(tempDir, baseName+".xml");
		xml.write(xmlFile.getAbsolutePath());
		if (xmlFile.exists())
			filesToZip.add(xmlFile);
			
		// now zip the files in fileToZip list and save, then delete the tempDir
		File target = new File(zipFilePath);
		if (JarTool.compress(filesToZip, target, null)) {
			tab.myFile = target;
			tab.changed = false;
			int i = getTabIndex(tab);
			tabbedPane.setTitleAt(i, XML.getName(zipFilePath));
			OSPRuntime.trigger(1000, (e) -> {
				ResourceLoader.deleteFile(new File(tempDir));
				tempDir = null;
			});
			return zipFilePath;
		}
		return null;
	}
	
	/**
	 * Opens a dialog to load new data from the NOAA spreadsheet
	 */
	public void loadSunData() {
		SunTab tab = getSelectedTab();
		if (tab != null) {
			// show dialog to load sun data from NOAA
			NOAAReader reader = new NOAAReader(tab);
			NOAAReaderControl control = new NOAAReaderControl(reader);
			if (control.ready)
				control.setVisible(true);
			else 
				// show warning dialog
				JOptionPane.showMessageDialog(tab.frame, 
						"The spreadsheet \""+ XML.getName(reader.pathToNOAA)+"\" was not found.", //$NON-NLS-1$
						"File Not Found", //$NON-NLS-1$
						JOptionPane.WARNING_MESSAGE);
		}
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
		chooser.addChoosableFileFilter(SunTab.imageFileFilter);
		chooser.setFileFilter(SunTab.imageFileFilter);
		Runnable ok = new Runnable() {
			@Override
			public void run() {
				File file = chooser.getSelectedFile();
				OSPRuntime.setPreference("file_chooser_directory", file.getParent());
				OSPRuntime.savePreferences();
				open(file);
			}			
		};
		chooser.showOpenDialog(this, ok, null);
	}

	/**
	 * Pastes a map image from the clipboard.
	 */
	public void pasteImage() {
		SunTab tab = getSelectedTab();
		if (tab == null) 
			return;
		
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
				Image image = (Image) t.getTransferData(DataFlavor.imageFlavor);
				if (image != null) {
					MapImage map = new MapImage(image);
					tab.plot.setMap(map, true);
					tab.changed = true;
					tab.plot.repaint();
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
		String aboutString = "Sun Reflector " + SunFrame.version 
				+ "\nBuild date " + date //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "\nCopyright (c) 2023 Douglas Brown"
				+ "\nhttps://github.com/dobrown";
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

	private void enableDragNDrop(){
		new DropTarget(this, new DropTargetListener(){			
      public void dragEnter(DropTargetDragEvent e){}     
      public void dragExit(DropTargetEvent e){}      
      public void dragOver(DropTargetDragEvent e){}     
      public void dropActionChanged(DropTargetDragEvent e){}
      
      @SuppressWarnings("unchecked")
			public void drop(DropTargetDropEvent e){
        try {
            // accept the drop first, important!
            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);            
            // get the list of dropped files
            List<Object> list=(List<Object>) e.getTransferable().
            		getTransferData(DataFlavor.javaFileListFlavor);           
            // open the first file in the list
            File file = (File)list.get(0);
            open(file);
            
        } catch(Exception ex){}
      }
    });
		
		SunTab tab = getSelectedTab();
		if (tab != null) {
			new DropTarget(tab.controls.info, new DropTargetListener(){			
	      public void dragEnter(DropTargetDragEvent e){}      
	      public void dragExit(DropTargetEvent e){}      
	      public void dragOver(DropTargetDragEvent e){}      
	      public void dropActionChanged(DropTargetDragEvent e){}
	      
	      @SuppressWarnings("unchecked")
				public void drop(DropTargetDropEvent e){
	        try {
	            // accept the drop first, important!
	            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);            
	            // get the list of dropped files
	            List<Object> list=(List<Object>) e.getTransferable().
	            		getTransferData(DataFlavor.javaFileListFlavor);           
	            // open the first file in the list
	            File file = (File)list.get(0);
	            open(file);
	            
	        } catch(Exception ex){}
	      }
	    });
		}
  }
	
}
