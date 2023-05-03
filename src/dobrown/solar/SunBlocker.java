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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.Drawable;
import org.opensourcephysics.display.DrawableTextLine;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.display.OSPButton;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display.TextLine;
import org.opensourcephysics.display.axes.CustomAxes;
import org.opensourcephysics.tools.FunctionEditor;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.AsyncFileChooser;

/**
 * A Drawable class that defines an altitude profile that blocks sun rays.
 *
 * @author Douglas Brown
 */
public class SunBlocker implements Drawable {
	
	static int defaultAlpha = 80;
	
	Cursor pencil_cursor;	
	SunApp app;
	Dataset plotOutline;
	boolean enabled = true;
	double[] altitudes = new double[360]; // mountain altitudes at 1 degree increments
	double[] xOutline, yOutline; // length 722, inside+outside edges+closedPaths
	BlockEditorDialog editor;
	BlockPlottingPanel blockPlot; 
	SunPlottingPanel plot;
	Color fillColor = new Color(50, 50, 100, 50);
	Color edgeColor = Color.GRAY;
	boolean invalidOutline;
	MapImage image;
	int imageAlpha = defaultAlpha;
	
	/**
	 * Constructor
	 * 
	 * @param app the SunApp
	 */
	SunBlocker(SunApp app) {
		this.app = app;
		ImageIcon icon = ResourceLoader.getImageIcon(app.RESOURCE_PATH+"pencil_cursor.gif"); //$NON-NLS-1$
		pencil_cursor = GUIUtils.createCustomCursor(icon.getImage(), new Point(1, 15),
				"", Cursor.MOVE_CURSOR); //$NON-NLS-1$
		plotOutline = initPlotOutline();
		xOutline = plotOutline.getValidXPoints();
		yOutline = plotOutline.getValidYPoints();
	}
	
	/**
	 * Gets the altitude below which the sun is blocked at a particular azimuth.
	 *
	 * @param az the azimuth in radians
	 * @return the altitude in radians
	 */
	public double getAltitude(double az) {
		if (!isEnabled())
			return 0;	
		az = az % (Math.PI * 2);
		if (az < 0)
			az += Math.PI * 2;
		return altitudes[app.round(Math.toDegrees(az))];
	}
	
	/**
	 * Sets the altitude below which the sun is blocked at a particular azimuth.
	 * 
	 * @param az the azimuth in radians
	 * @param alt the altitude in radians
	 */
	public void setAltitude(double az, double alt) {
		az = Math.max(az, Math.toRadians(-180));
		az = Math.min(az, Math.toRadians(179.1));
		if (az < -app.ONE_DEGREE/2)
			az += 2*Math.PI;
		if (az > 2*Math.PI-app.ONE_DEGREE/2)
			az -= 2*Math.PI;
		alt = Math.max(alt, 0);
		alt = Math.min(alt, Math.PI / 2);
		altitudes[app.round(Math.toDegrees(az))] = alt;
		invalidOutline = true;
	}
	
	/**
	 * Sets the enabled property. Blocking is ignored if not enabled.
	 * 
	 * @param b true to enable
	 */
	public void setEnabled(boolean b) {
		enabled = b;
	}

	/**
	 * Gets the enabled property. Blocking is ignored if not enabled.
	 * 
	 * @return true if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}		
	
	/**
	 * Opens the editor dialog.
	 */
	public void edit() {
		if (editor == null) {
			editor = new BlockEditorDialog();
		}
		blockPlot.refreshEditorProfileDataset();
		editor.refreshDisplay();
		editor.setVisible(true);
	}
	
	/**
	 * Gets the current map image.
	 * 
	 * @return the map image, or null if none
	 */
	MapImage getMapImage() {
		return image;
	}

	/**
	 * Sets the map image. May be null.
	 * 
	 * @param image the map image
	 */
	void setMapImage(MapImage image) {
		this.image = image;
	}

	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		if (invalidOutline) {
			loadPlotOutlineFromAltitudes();
			invalidOutline = false;
		}
		if (isEnabled())
			plotOutline.draw(panel, g);		
	}
	
	/**
	 * Initializes the plot outline dataset
	 * 
	 * @return the outline dataset
	 */
	Dataset initPlotOutline() {
		if (plotOutline == null) {
			plotOutline = new Dataset();
			plotOutline.setMarkerShape(Dataset.AREA);
			plotOutline.setMarkerColor(fillColor, edgeColor);
			// initialize
			xOutline = new double[722];
			yOutline = new double[722];
			for (int j = 0; j < 360; j++) {
				double az = Math.toRadians(j) % (2*Math.PI);
				xOutline[j] = 90 * Math.sin(az);
				xOutline[j+361] = -xOutline[j];
				yOutline[j] = yOutline[j+361] = 90 * Math.cos(az);
			}
			xOutline[360] = xOutline[0];
			yOutline[360] = yOutline[0];
			xOutline[721] = xOutline[0];
			yOutline[721] = yOutline[0];
			plotOutline.set(xOutline, yOutline);
		}
		return plotOutline;		
	}
	
	/**
	 * Loads the plot outline dataset from the current altitudes array
	 */
	void loadPlotOutlineFromAltitudes() {
		for (int j = 0; j < 360; j++) {
			double az = Math.toRadians(j) % (2*Math.PI);
			double d = 90 * (1 - (2 * altitudes[j] / Math.PI));
			xOutline[j] = d * Math.sin(az);
			yOutline[j] = d * Math.cos(az);
		}
		xOutline[360] = xOutline[0];
		yOutline[360] = yOutline[0];
		xOutline[721] = xOutline[0];
		yOutline[721] = yOutline[0];
		plotOutline.set(xOutline, yOutline);		
	}

	/**
	 * Inner dialog that displays the profile plotting panel.
	 */
	class BlockEditorDialog extends JDialog {
		
		JMenuItem closeImageItem;
		JButton zoomButton, zoomImageButton;
		JLabel zoomLabel, dragLabel, zoomImageLabel, dragImageLabel;
		JLabel drawLabel;
		JCheckBox drawCheckbox;
		JToolBar toolbar;
		JMenuItem pasteImageItem;
		JMenu alphaMenu;
		MenuListener menuListener;

		BlockEditorDialog() {
			super(app.frame, false);
			setTitle("Mountains");
			setResizable(false);
			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.setBorder(BorderFactory.createEtchedBorder());
			setContentPane(contentPane);

			blockPlot = new BlockPlottingPanel();			
			setPreferredSize(new Dimension(820, 360));	
			contentPane.add(blockPlot, BorderLayout.CENTER);
			blockPlot.setImageAlpha(imageAlpha);
			
			drawCheckbox = new JCheckBox("Draw");
			drawCheckbox.setOpaque(false);
			drawCheckbox.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 6));
			drawCheckbox.setAction(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					app.plot.setTracksVisible(drawCheckbox.isSelected());
				}				
			});
			
			drawLabel = new JLabel("Draw: control-drag");
						
			zoomButton = new OSPButton();
			zoomButton.setAction(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					JPopupMenu popup = new JPopupMenu();
					int n = app.round((blockPlot.zoomFactor - 1) * 20);
					n = Math.min(Math.max(0, n), 100);
					double center = (blockPlot.getPreferredXMax() + blockPlot.getPreferredXMin()) / 2;
					JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, n);
					zoomSlider.addChangeListener(ev -> {
			      blockPlot.zoom(1 + zoomSlider.getValue()/20.0, center);
			    });
			    popup.add(zoomSlider);
			    popup.show(zoomButton, -50, zoomButton.getHeight());
				}				
			});
			
			zoomImageButton = new OSPButton();
			zoomImageButton.setAction(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					if (image != null) {
						double scale = image.getScale();
						// determine current slider value
						double power = Math.log(scale) / Math.log(SunApp.ZOOM_FACTOR);
						int n = app.round(90 + power);
						n = Math.max(0, Math.min(n, 110));
						
						JPopupMenu popup = new JPopupMenu();
						JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 0, 110, n);
						
						zoomSlider.addChangeListener(ev -> {
							int pow = zoomSlider.getValue() - 90;
							double z = Math.pow(SunApp.ZOOM_FACTOR, pow);
							image.setScaleWithFixedImageCenter(z);
							repaint();
				    });
				    popup.add(zoomSlider);
				    popup.show(zoomImageButton, -50, zoomImageButton.getHeight());
					}				
				}				
			});
			
			zoomLabel = new JLabel("Plot:");
			dragLabel = new JLabel("drag to move");
			zoomImageLabel = new JLabel("Image:");
			dragImageLabel = new JLabel("shift-drag to move");

			toolbar = new JToolBar();
			toolbar.setFloatable(false);
			contentPane.add(toolbar, BorderLayout.NORTH);

			toolbar.add(Box.createHorizontalStrut(8));
			toolbar.add(zoomLabel);
			toolbar.add(zoomButton);
			toolbar.add(dragLabel);
			toolbar.add(Box.createHorizontalGlue());
			toolbar.add(drawLabel);
			toolbar.add(Box.createHorizontalGlue());
			toolbar.add(zoomImageLabel);
			toolbar.add(zoomImageButton);
			toolbar.add(dragImageLabel);
			toolbar.add(Box.createHorizontalStrut(8));
			
			pack();
			setLocationRelativeTo(app.frame);
			
			JMenuBar menubar = new JMenuBar();
			JMenu fileMenu = new JMenu("Image");
			menubar.add(fileMenu);
			JMenuItem openImageItem = new JMenuItem("Open Image");
			fileMenu.add(openImageItem);
			openImageItem.addActionListener(e -> {
				openImage();
				refreshDisplay();
			});
			pasteImageItem = new JMenuItem("Paste Image");
			fileMenu.add(pasteImageItem);
			pasteImageItem.addActionListener(e -> {
				pasteImage();
				refreshDisplay();
			});
			closeImageItem = new JMenuItem("Close Image");
			fileMenu.add(closeImageItem);
			closeImageItem.addActionListener(e -> {
				image = null;
				refreshDisplay();
				repaint();
			});
			
			fileMenu.addSeparator();
			
			alphaMenu = new JMenu("Opacity");
			fileMenu.add(alphaMenu);
			JSlider alphaSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, imageAlpha);
			alphaSlider.addChangeListener(e -> {
	      blockPlot.setImageAlpha(alphaSlider.getValue());
	    });
	    alphaMenu.add(alphaSlider);
	    fileMenu.add(alphaMenu);

			setJMenuBar(menubar);
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
			fileMenu.addMenuListener(menuListener);
			refreshDisplay();
		}
		
		/**
		 * Opens an image suing the file chooser
		 */
		public void openImage() {
			AsyncFileChooser chooser = OSPRuntime.getChooser();
			if (chooser == null) {
				return;
			}
			chooser.setDialogTitle("Open Image");
			chooser.resetChoosableFileFilters();
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.addChoosableFileFilter(SunApp.imageFileFilter);
			chooser.setFileFilter(SunApp.imageFileFilter);
			String chooserPath = (String)OSPRuntime.getPreference("file_chooser_directory");
			if (chooserPath != null) {
				chooser.setCurrentDirectory(new File(chooserPath));
			}
			Runnable ok = new Runnable() {
				@Override
				public void run() {
					File file = chooser.getSelectedFile();
					OSPRuntime.setPreference("file_chooser_directory", file.getParent());
					OSPRuntime.savePreferences();
					String path = file.getAbsolutePath();
					MapImage map = new MapImage(path);
					if (map.getImagePath() != null) {
						image = map;
						repaint();
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
		 * Pastes an image from the clipboard
		 */
		public void pasteImage() {
			Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			try {
				if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
					Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
					if (img != null) {
						MapImage map = new MapImage(img);
						image = map;
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		/**
		 * Refreshes this dialog
		 */
		void refreshDisplay() {
			zoomButton.setIcon(app.zoomIcon);
			zoomButton.setToolTipText("Magnify the plot");
			zoomImageButton.setIcon(app.zoomIcon);
			zoomImageButton.setToolTipText("Magnify the image");
			zoomImageButton.setEnabled(image != null);
			closeImageItem.setEnabled(image != null);
			zoomLabel.setEnabled(true);
			dragLabel.setEnabled(true);
			zoomImageLabel.setEnabled(image != null);
			dragImageLabel.setEnabled(image != null);
			blockPlot.refreshPlotTitle();
			// rebuild toolbar
			toolbar.removeAll();
			toolbar.add(Box.createHorizontalStrut(8));
			toolbar.add(zoomLabel);
			toolbar.add(zoomButton);
			toolbar.add(dragLabel);
			toolbar.add(Box.createHorizontalGlue());
			toolbar.add(drawLabel);
			toolbar.add(Box.createHorizontalGlue());
			if (image != null) {
				toolbar.add(zoomImageLabel);
				toolbar.add(zoomImageButton);
				toolbar.add(dragImageLabel);
				toolbar.add(Box.createHorizontalStrut(8));
			}
			else 
				toolbar.add(Box.createHorizontalStrut(130));
			alphaMenu.setEnabled(image != null);
			repaint();
		}

	}
	
	/**
	 * A plotting panel for the profile data
	 */
	class BlockPlottingPanel extends PlottingPanel {
		
		Dataset editorProfileDataset;
		double xPrevProfile = -181;
		int mouseX0;
		double xmin0, plotWidth;
		SunPoint profilerPt;
		SunPoint utilityPt;
		SunPoint moverPt;
		double zoomFactor = 1;

		BlockPlottingPanel() {
			super("", "", "");
			axes = new MyAxes(this);
			refreshPlotTitle();

			setPreferredMinMax(-180, 180, 0, 90);
			setSquareAspect(true);
			removeOptionController();
			setBorder(BorderFactory.createEtchedBorder());
			setShowCoordinates(false);
			
			utilityPt = new SunPoint(this);
			moverPt = new SunPoint(this) {			
				@Override
				public void setXY(double x, double y) {
					// x, y in scaled units
					double dx = x - this.x;
					double dy = y - this.y;
					super.setXY(x, y);
					if (mouseEvent.isShiftDown()) {
						// move map
						if (image != null) {
							double[] xy = image.getOrigin();
							xy[0] -= dx / image.getScale();
							xy[1] += dy / image.getScale();
							image.setOrigin(xy[0], xy[1]);
						}
					}
					else if (!mouseEvent.isControlDown()){
						dx = (mouseEvent.getX() - mouseX0) / getXPixPerUnit();
						// move plot limits if zoomed
						double xmin = xmin0 - dx;
						double xmax = xmin + plotWidth;
						if (xmin < -180) {
							xmin = -180;
							xmax = xmin + plotWidth;
						}
						else if (xmax > 180) {
							xmax = 180;
							xmin = xmax - plotWidth;
						}
						setPreferredMinMaxX(xmin, xmax);
						repaint();
					}
				}	
				
				@Override
				public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
					if (mouseEvent.getButton() != MouseEvent.BUTTON1)
						return null;
					return mouseEvent.isControlDown()? null: this;
				}
			};

			editorProfileDataset = new Dataset();
			editorProfileDataset.setConnected(true);
			editorProfileDataset.setMarkerShape(Dataset.AREA);
			editorProfileDataset.setMarkerColor(fillColor, edgeColor);
			refreshEditorProfileDataset();
			
			profilerPt = new SunPoint(this) {
				
				@Override
				public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
					if (mouseEvent.getButton() != MouseEvent.BUTTON1)
					return null;
					return mouseEvent.isControlDown()? this: null;
				}

				@Override
				public void setXY(double x, double y) {
					super.setXY(x, y);
					x = Math.toRadians(x);
					y = Math.toRadians(y);
					setAltitude(x, y);
					if (xPrevProfile >= -180 && Math.abs(x - xPrevProfile) > Math.toRadians(1)) {
						double smallerX = Math.min(x, xPrevProfile);
						double delta = Math.abs(x - xPrevProfile);
						for (int i = 0; i < Math.toDegrees(delta); i++) {
							setAltitude(smallerX + Math.toRadians(i), y);							
						}
					}
					else {
						setAltitude(x, y);
					}
					refreshEditorProfileDataset();
					repaint();
					xPrevProfile = x; 
					loadPlotOutlineFromAltitudes();
					app.refreshViews();
				}
			};
			
			addMouseListener(new MouseAdapter() {
				
				@Override
				public void mousePressed(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						Point p = e.getPoint();
						mouseX0 = p.x;
						moverPt.setScreenPosition(p.x, p.y);
						xmin0 = getPreferredXMin();
						plotWidth = Math.min(360, getPreferredXMax() - xmin0);
					}
				}	
				
				@Override
				public void mouseReleased(MouseEvent e) {
					xPrevProfile = -181;
					if (e.isPopupTrigger()) {
						JPopupMenu popup = getPopupMenu();
						popup.show(BlockPlottingPanel.this, e.getX(), e.getY());
					}
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						setAutoscaleX(false);
						setAutoscaleY(false);
						setPreferredMinMax(-180, 180, 0, 90);
						repaint();
					}
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					setMouseCursor(null);	// setMouseCursor will set correct cursor
				}

				@Override
				public void mouseExited(MouseEvent e) {
					setMouseCursor(Cursor.getDefaultCursor());	
				}
			});
			
			addMouseWheelListener(new MouseAdapter() {
				public void mouseWheelMoved(MouseWheelEvent e){
					int delta = -e.getWheelRotation();
					if (e.isControlDown() 
							|| e.isShiftDown()) {
						double factor = 1 + delta/20.0;
						if (image != null) {
							image.setScaleWithFixedImageCenter(image.getScale() * factor);
							repaint();
							setMouseCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));	
						}
					}
					else {
						double factor = Math.pow(SunApp.ZOOM_FACTOR, delta) ;
						Point p = e.getPoint();
						utilityPt.setScreenPosition(p.x,  p.y);
						zoom(zoomFactor * factor, utilityPt.getX());
					}
				}
			});
			
			addDrawable(editorProfileDataset);
			addDrawable(profilerPt);
			addDrawable(moverPt);
			
		};
		
		@Override
		public void setMouseCursor(Cursor cursor) {
			if (mouseEvent != null && mouseEvent.isControlDown()) {
				setCursor(pencil_cursor);					
			}
			else if (image != null && mouseEvent != null && mouseEvent.isShiftDown())
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));	
			else if (zoomFactor > 1.05)
				setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));	
			else
				setCursor(Cursor.getDefaultCursor());
		}
		
		/**
		 * Sets the image alpha (opacity)
		 * 
		 * @param alfa 0-255
		 */
		void setImageAlpha(int alfa) {
			imageAlpha = alfa % 256;
			axes.setInteriorBackground(new Color(255,255,255,255-imageAlpha));
			repaint();
		}
		
		/**
		 * Zooms the plot to a specified factor, keeping one spot fixed
		 * 
		 * @param alfa 0-255
		 * @param fixedX the x value that remains fixed in the plot
		 */
		void zoom(double factor, double fixedX) {
			factor = Math.max(factor, 1);
			double df = factor / zoomFactor;
			zoomFactor = factor;
			double w = 360 / factor;
			double xmin = getPreferredXMin();
			xmin = (xmin + (df - 1) * fixedX) / df;
			double xmax = xmin + w;
			
			if (xmin < -180) {
				xmin = -180;
				xmax = xmin + w;
			}
			else if (xmax > 180) {
				xmax = 180;
				xmin = xmax - w;
			}

			double ymin = 0;
			double ymax = (xmax - xmin) / 4; // ratio of w/h always 360/90 = 4
			
			this.setPreferredMinMaxX(xmin, xmax);
			this.setPreferredMinMaxY(ymin, ymax);
			repaint();
		}
		
		@Override
		public JPopupMenu getPopupMenu() {
			buildPopupMenu();
			return popupmenu;
		}

		@Override
		protected void buildPopupMenu() {
			if (popupmenu == null) {
				popupmenu = new JPopupMenu();
			} else {
				popupmenu.removeAll();
			}
			autoscaleItem = new JMenuItem("Full Scale"); //$NON-NLS-1$
			autoscaleItem.addActionListener((e) -> {
				setPreferredMinMax(-180, 180, 0, 90);
				zoomBox.setVisible(false);				
				repaint();
			});
			popupmenu.add(autoscaleItem);
		}
		
		/**
		 * Refreshes the profile dataset
		 */
		void refreshEditorProfileDataset() {
			double[] x = new double[altitudes.length+2];
			double[] y = new double[altitudes.length+2];
			x[0] = y[0] = y[x.length-1] = -180;
			x[x.length-1] = 180;
			for (int i = 0; i < altitudes.length; i++) {
				x[i+1] = i-180;
				int n = i>179? i-180: i+180;
				y[i+1] = Math.toDegrees(altitudes[n]);
			}
			editorProfileDataset.set(x, y);
		}
		
		@Override
		protected void paintFirst(Graphics g) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight()); // fill the component with the background color
			
			// set clip to interior area of plot
			utilityPt.setLocation(getPreferredXMin(),  getPreferredYMax());
			Point p0 = new Point(utilityPt.getScreenPosition());
			utilityPt.setLocation(getPreferredXMax(),  0);
			Point p1 = utilityPt.getScreenPosition();
      
			g.setColor(Color.WHITE);
      g.fillRect(p0.x, p0.y, p1.x-p0.x, p1.y-p0.y);
      
			// draw image, if any
			if (image != null) {
				Shape clip = g.getClip();
	      g.setClip(p0.x, p0.y, p1.x-p0.x, p1.y-p0.y);
				image.draw(this, g);
				g.setClip(clip);
			}
			
			g.setColor(Color.BLACK);
			axes.draw(this, g); // draw the axes
		}
		
		/**
		 * Refreshes the plot title
		 */
		void refreshPlotTitle() {
			String s = image==null? 
					"Control-drag the mouse to draw mountains and trees. Open an image to trace for accuracy.":
					"Control-drag the mouse to draw mountains and trees.";
			axes.setTitle(s, "Arial-PLAIN-14");			
		}

	};
	
	/**
	 * The axes for the plot. 
	 */
	class MyAxes extends CustomAxes {
		
		SunPoint sunPt;
		Rectangle clipRect = new Rectangle();
		FontMetrics labelFontMetrics;
	  DrawableTextLine azLabel = new DrawableTextLine("x", 0, 0); //$NON-NLS-1$
	  DrawableTextLine altLabel = new DrawableTextLine("y", 0, 0); //$NON-NLS-1$
		
		/**
		 * Constructor
		 * @param panel the plotting panel
		 */
		MyAxes(PlottingPanel panel) {
			super(panel);
			this.setDefaultGutters(40, 40, 40, 45);
			this.resetPanelGutters();
			labelFont = labelFont.deriveFont(10f);
	    labelFontMetrics = panel.getFontMetrics(labelFont);
      azLabel.setText("Azimuth");
      azLabel.setFont(labelFont.deriveFont(12f).deriveFont(Font.BOLD));
      azLabel.setJustification(TextLine.CENTER);
      azLabel.setPixelXY(true);
      altLabel.setText("Altitude");
      altLabel.setFont(labelFont.deriveFont(12f).deriveFont(Font.BOLD));
      altLabel.setJustification(TextLine.CENTER);
      altLabel.setTheta(Math.PI/2);
      altLabel.setPixelXY(true);
      titleLine.setFont(labelFont.deriveFont(16f));
      titleLine.setJustification(TextLine.CENTER);
      titleLine.setPixelXY(true);
			sunPt = new SunPoint(panel);
			
		}
		
	  @Override
	  public void draw(DrawingPanel panel, Graphics g) {
	  	
	  	Graphics2D g2 = (Graphics2D)g;
	  	
  		sunPt.setLocation(drawingPanel.getPreferredXMin(), drawingPanel.getPreferredYMax());
  		Point p1 = new Point(sunPt.getScreenPosition());
  		sunPt.setLocation(drawingPanel.getPreferredXMax(), 0);
  		Point p2 = sunPt.getScreenPosition();
  		clipRect.setRect(p1.x-1, p1.y-1, p2.x-p1.x+2, p2.y-p1.y+2);	  	
	  	g.setClip(clipRect);
	  	
      g.setColor(interiorColor);
      g2.fill(clipRect);

	  	g.setColor(Color.GRAY);
      g2.draw(new Rectangle(p1.x, p1.y, p2.x-p1.x, p2.y-p1.y));
	  	
	  	int interval = blockPlot.zoomFactor > 4? 5: 
	  		blockPlot.zoomFactor > 2? 10: 30;
	  	Stroke dotted = new BasicStroke(0.7f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 8, new float[] {2f, 4f}, 0);
	  		
	  	for (int i = 0; i < 91; i += interval) {
	  		boolean heavy = interval == 30 || i%30 == 0;
		  	g2.setStroke(heavy? app.getStroke(0.7f): dotted);
	  		sunPt.setLocation(blockPlot.getPreferredXMin(), i);
	  		p1 = new Point(sunPt.getScreenPosition());
	  		sunPt.setLocation(blockPlot.getPreferredXMax(), i);
	  		p2 = sunPt.getScreenPosition();
	  		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	  	}
	  	for (int i = -180; i < 181; i += interval) {
	  		boolean heavy = interval == 30 || i%30 == 0;
		  	g2.setStroke(heavy? app.getStroke(0.7f): dotted);
	  		sunPt.setLocation(i, 0);
	  		p1 = new Point(sunPt.getScreenPosition());
	  		sunPt.setLocation(i, blockPlot.getPreferredYMax());
	  		p2 = sunPt.getScreenPosition();
	  		g.drawLine(p1.x, p1.y, p1.x, p2.y);
	  	}
	  	g.setClip(null);
	  	g.setFont(labelFont.deriveFont(10f));
	  	g.setColor(Color.BLACK);
      int h = labelFontMetrics.getHeight();
	  	for (int i = 0; i < 91; i += interval) {
	  		if (i > blockPlot.getPreferredYMax())
	  			break;
	  		sunPt.setLocation(blockPlot.getPreferredXMin(), i);
	  		p1 = new Point(sunPt.getScreenPosition());
	  		String s = String.valueOf(i);
	      int w = labelFontMetrics.stringWidth(s);
	  		g.drawString(s+FunctionEditor.DEGREES, p1.x-5-w, p1.y + h/2 -1);
	  	}
	  	for (int i = -180; i < 181; i += interval) {
	  		if (i < blockPlot.getPreferredXMin())
	  			continue;
	  		if (i > blockPlot.getPreferredXMax())
	  			break;
	  		sunPt.setLocation(i, 0);
	  		p1 = new Point(sunPt.getScreenPosition());
	  		String s = String.valueOf(i)+FunctionEditor.DEGREES;
	  		if (i==0)
	  			s = "North";
	  		else if (i==90)
	  			s = "East";
	  		else if (i==-180 || i==180)
	  			s = "South";
	  		else if (i==-90)
	  			s = "West";
	      int w = labelFontMetrics.stringWidth(s);
	  		g.drawString(s,	p1.x-w/2, p1.y + h + 2);
	  	}
	  	
	  	double xx = (blockPlot.getPreferredXMin() + blockPlot.getPreferredXMax()) / 2;
	  	sunPt.setLocation(xx, 0);
	  	p1 = sunPt.getScreenPosition();
      azLabel.setX(p1.x);
      azLabel.setY(p1.y + 2*h + 5);
      azLabel.setColor(Color.BLACK);
      azLabel.draw(panel, g);

      double yy = blockPlot.getPreferredYMax() / 2;
	  	sunPt.setLocation(blockPlot.getPreferredXMin(), yy);
	  	p1 = sunPt.getScreenPosition();
      altLabel.setX(p1.x - h - 10);
      altLabel.setY(p1.y);
      altLabel.setColor(Color.BLACK);
      altLabel.draw(panel, g);

	  	sunPt.setLocation(xx, 2*yy);
	  	p1 = sunPt.getScreenPosition();
      titleLine.setX(p1.x);
      titleLine.setY(p1.y - h);
      titleLine.setColor(Color.BLACK);
      titleLine.draw(panel, g);
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
			SunBlocker sunBlock = (SunBlocker)obj;
			control.setValue("enabled", sunBlock.isEnabled()); //$NON-NLS-1$
			control.setValue("altitudes", sunBlock.altitudes); //$NON-NLS-1$
			control.setValue("image", sunBlock.getMapImage());
			control.setValue("image_alpha", sunBlock.imageAlpha);
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
			SunBlocker sunBlock = (SunBlocker)obj;
			sunBlock.altitudes = (double[])control.getObject("altitudes"); //$NON-NLS-1$
			sunBlock.loadPlotOutlineFromAltitudes();
			sunBlock.setEnabled(control.getBoolean("enabled")); //$NON-NLS-1$
			sunBlock.setMapImage((MapImage)control.getObject("image"));
			sunBlock.imageAlpha = control.getInt("image_alpha");
			if (sunBlock.blockPlot != null) {
				sunBlock.blockPlot.refreshEditorProfileDataset();
				sunBlock.blockPlot.setImageAlpha(sunBlock.imageAlpha);
				sunBlock.editor.refreshDisplay();
				sunBlock.editor.repaint();
			}
			return obj;
		}
	}

	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

}
