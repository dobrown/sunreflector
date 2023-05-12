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

import org.opensourcephysics.display.OSPButton;

public class SunTabPanel extends JPanel {

	SunTab tab;
	JToolBar toolbar;
	JCheckBox showTracksCheckbox, showSkylineCheckbox;	
	JButton zoomButton, opacityButton;
	JSlider mapAlphaSlider;
	JLabel mapLabel, mapDragLabel;
	
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

		mapAlphaSlider = new JSlider(JSlider.HORIZONTAL, 100, 255, tab.plot.mapAlpha);
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

		mapLabel = new JLabel("Map:");
		mapDragLabel = new JLabel("| drag to move");
		
		buildToolbar();

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
		if (tab.plot.map != null) {
			toolbar.add(mapLabel);
			toolbar.add(zoomButton);
			toolbar.add(opacityButton);
//			toolbar.add(mapDragLabel);
			toolbar.add(Box.createHorizontalStrut(8));
		}
	}
	
	/**
	 * Refreshes the display to reflect the current settings.
	 */
	void refreshDisplay() {
		showTracksCheckbox.setSelected(tab.plot.isTracksVisible());
		zoomButton.setIcon(tab.zoomIcon);
		zoomButton.setText("Scale");
		zoomButton.setToolTipText("Resize the map image");
		opacityButton.setText("Opacity");
		opacityButton.setToolTipText("Set the image opacity");
		showTracksCheckbox.setText("Show tracks");
		showTracksCheckbox.setToolTipText("Show the sun and reflection sky tracks");

		showSkylineCheckbox.setSelected(tab.sunBlock.isEnabled());
		mapAlphaSlider.setValue(tab.plot.mapAlpha);
		
		mapLabel.setEnabled(tab.plot.map != null && tab.plot.map.isVisible());
		mapDragLabel.setEnabled(tab.plot.map != null && tab.plot.map.isVisible());
		showTracksCheckbox.setEnabled(tab.sunAzaltData != null);

		buildToolbar();
	}
		

}
