package dobrown.solar;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.OSPButton;

public class SunTabPanel extends JPanel {

	SunTab tab;
	JToolBar toolbar;
	JCheckBox showTracksCheckbox, showSkylineCheckbox;	
	JButton zoomButton, opacityButton, openMapButton, closeMapButton;
	JSlider mapAlphaSlider;
	JLabel mapLabel, mapDragLabel;
	UndoableEditSupport undoSupport;
	UndoManager undoManager;
	
	SunTabPanel(SunTab tab) {
		super(new BorderLayout());
		this.tab = tab;
		tab.tabPanel = this;
		createGUI();
	}
	
	void createGUI() {
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(tab.plot.getBackground());
		
		JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		leftRightSplit.setDividerSize(6);
		leftRightSplit.setResizeWeight(0.33);
//		leftRightSplit.setEnabled(false);
		add(leftRightSplit, BorderLayout.CENTER);
		
		JSplitPane topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		topBottomSplit.setDividerSize(3);
		topBottomSplit.setResizeWeight(0.95);
		topBottomSplit.setEnabled(false);
		
		leftRightSplit.setRightComponent(topBottomSplit);
		leftRightSplit.setLeftComponent(tab.controls);
		
		JPanel plotPanel = new JPanel(new BorderLayout());
		plotPanel.add(tab.plot, BorderLayout.CENTER);
		plotPanel.add(toolbar, BorderLayout.NORTH);

		topBottomSplit.setTopComponent(plotPanel);
		topBottomSplit.setBottomComponent(tab.pvDrawingPanel);
		
		showTracksCheckbox = new JCheckBox();
		showTracksCheckbox.setOpaque(false);
		showTracksCheckbox.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 6));
		showTracksCheckbox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				tab.plot.setTracksVisible(showTracksCheckbox.isSelected());
			}				
		});
		
		showSkylineCheckbox = new JCheckBox("Show skyline"); //$NON-NLS-1$
		showSkylineCheckbox.setSelected(tab.sunBlock.isEnabled());
		showSkylineCheckbox.addActionListener((e) -> {
			tab.sunBlock.setEnabled(showSkylineCheckbox.isSelected());
			tab.refreshViews();
		});

		zoomButton = new OSPButton();
		zoomButton.setHorizontalTextPosition(JButton.LEFT);
		zoomButton.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				MapImage map = tab.plot.map;
				if (map != null) {
					tab.plot.pvPanel.setInflation(0.2);
					tab.plot.repaint();
					
					double scale = map.getScale();
					// determine current slider scale power
					double power = Math.log(scale) / Math.log(SunTab.ZOOM_FACTOR);
					int n = tab.round(60 + power);
					n = Math.min(80, Math.max(0, n));
					
					JPopupMenu popup = new JPopupMenu();
					JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 0, 80, n);
					zoomSlider.addChangeListener(ev -> {
						int p = zoomSlider.getValue() - 60;
						double z = Math.pow(SunTab.ZOOM_FACTOR, p);
						map.setScaleWithFixedOrigin(z);
						tab.plot.repaint();
			    });
			    popup.add(zoomSlider);
			    popup.show(zoomButton, 0, zoomButton.getHeight());
				}				
			}				
		});

		mapAlphaSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, tab.plot.mapAlpha);
		mapAlphaSlider.addChangeListener(e -> {
      tab.plot.setMapAlpha(mapAlphaSlider.getValue());
    });		
		
		opacityButton = new OSPButton();
		opacityButton.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				MapImage map = tab.plot.map;
				if (map != null) {
					tab.plot.pvPanel.setInflation(0.2);
					tab.plot.repaint();
					
					JPopupMenu popup = new JPopupMenu();
			    popup.add(mapAlphaSlider);
			    popup.show(opacityButton, 0, opacityButton.getHeight());
				}				
			}				
		});

		openMapButton = new OSPButton();
		openMapButton.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				tab.frame.openImage();
				tab.frame.refreshDisplay();
			}				
		});
		
		closeMapButton = new OSPButton();
		closeMapButton.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (tab.plot.map == null)
					return;
				tab.plot.setMap(null, true);
				tab.changed = true;
				tab.plot.repaint();
				tab.frame.refreshDisplay();				
			}				
		});
				
		mapLabel = new JLabel("Map");
		mapDragLabel = new JLabel("| drag to move");
		
		buildToolbar();
		
		// set up the undo system
		undoManager = new UndoManager();
		undoSupport = new UndoableEditSupport();
		undoSupport.addUndoableEditListener(undoManager);

		leftRightSplit.setDividerLocation(0.42);
		topBottomSplit.setDividerLocation(0.65);
	}
	
	SunTab getSunTab() {
		return tab;
	}
	
	/**
	 * Rebuilds the toobar.
	 */
	void buildToolbar() {
		toolbar.removeAll();
		toolbar.add(Box.createHorizontalStrut(2));
		toolbar.add(showTracksCheckbox);
		toolbar.add(showSkylineCheckbox);
		toolbar.add(Box.createHorizontalGlue());
		
		boolean hasMap = tab.plot.map != null;
		if (!hasMap)
			toolbar.add(Box.createHorizontalStrut(80));
		toolbar.add(mapLabel);
		if (hasMap) {
			toolbar.add(zoomButton);
			toolbar.add(opacityButton);
//			toolbar.add(mapDragLabel);
		}
		toolbar.add(openMapButton);
		toolbar.add(closeMapButton);
//		toolbar.add(Box.createHorizontalStrut(8));
	}
	
	/**
	 * Refreshes the display to reflect the current settings.
	 */
	void refreshDisplay() {
		showTracksCheckbox.setSelected(tab.plot.isTracksVisible());
		openMapButton.setIcon(SunTab.openIcon);
		openMapButton.setToolTipText("Open a map image file");
		closeMapButton.setIcon(SunTab.closeIcon);
		closeMapButton.setToolTipText("Remove the map image");
		zoomButton.setIcon(SunTab.zoomIcon);
		zoomButton.setText("Scale");
		zoomButton.setToolTipText("Resize the map image");
		opacityButton.setText("Opacity");
		opacityButton.setToolTipText("Set the image opacity");
		showTracksCheckbox.setText("Show tracks");
		showTracksCheckbox.setToolTipText("Show the sun and reflection sky tracks");

		showSkylineCheckbox.setSelected(tab.sunBlock.isEnabled());
		mapAlphaSlider.setValue(tab.plot.mapAlpha);
		
//		mapLabel.setEnabled(tab.plot.map != null && tab.plot.map.isVisible());
		mapDragLabel.setEnabled(tab.plot.map != null && tab.plot.map.isVisible());
		showTracksCheckbox.setEnabled(tab.sunAzaltData != null);
		closeMapButton.setEnabled(tab.plot.map != null);

		buildToolbar();
	}
		
	/**
	 * An undoable edit class.
	 */
	class ReplaceMapEdit extends AbstractUndoableEdit {

		String undo; // xml string
		String redo; // xml string

		protected ReplaceMapEdit(String undoXML, String redoXML) {
			undo = undoXML;
			redo = redoXML;
		}

		@Override
		public void undo() throws CannotUndoException {
			super.undo();
			int i = tab.frame.getTabIndex(tab);
			if (i == -1)
				return;
			
			// if the tab.plot has a redoable map, update its xml
			if (redo != null)
				redo = new XMLControlElement(tab.plot.map).toXML();				

			// reload the MapImage
			SunPlottingPanel plot = tab.plot;
			XMLControl control = new XMLControlElement(undo);
			MapImage map = (MapImage) control.loadObject(null);
			if (map != null && map.getImagePath() != null) {
				plot.setMap(map, false);
				tab.changed = true;
				plot.repaint();
			}
		}

		@Override
		public void redo() throws CannotUndoException {
			super.redo();
			int i = tab.frame.getTabIndex(tab);
			if (i == -1)
				return;
			
			// update undo map
			undo = new XMLControlElement(tab.plot.map).toXML();			
			
			if (redo == null) {
				tab.plot.setMap(null, false);
				tab.changed = true;
				tab.plot.repaint();
				tab.frame.refreshDisplay();
			}
			else {
				// reload the redo MapImage
				SunPlottingPanel plot = tab.plot;
				XMLControl control = new XMLControlElement(redo);
				MapImage map = (MapImage) control.loadObject(null);
				if (map != null && map.getImagePath() != null) {
					plot.setMap(map, false);
					tab.changed = true;
					plot.repaint();
				}
				else 
					plot.setMap(null, false);
			}
		}

		@Override
    public String getPresentationName() {
      return redo==null? "remove map": "replace map";
		}

	}


}
