/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.model.LayerMode;
import com.lightcrafts.model.Operation;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.layout.Box;
import static com.lightcrafts.ui.operation.Locale.LOCALE;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;
import org.jvnet.substance.color.ColorScheme;

import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

class OpFooter extends Box implements PropertyChangeListener {
    private LayerControls layerControls;
    private InvertRegionCheckBox invertRegionSwitch;
    private ColorSelectionControls colorControls;
    private JTabbedPane tabPane;

    public void propertyChange(PropertyChangeEvent evt) {
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport( this );

    OpFooter(OpControl control, List<LayerMode> layerModes) {
        super(BoxLayout.X_AXIS);

        layerControls = new LayerControls(control, layerModes, pcs);
        invertRegionSwitch = new InvertRegionCheckBox(control, pcs);
        colorControls = new ColorSelectionControls(control, pcs);

        Box blendBox = Box.createVerticalBox();
        blendBox.add(Box.createVerticalStrut(5));
        blendBox.add(layerControls);
        blendBox.add(invertRegionSwitch);
        blendBox.setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        layerControls.setAlignmentX( Component.LEFT_ALIGNMENT );
        invertRegionSwitch.setAlignmentX( Component.LEFT_ALIGNMENT );

        tabPane = new JTabbedPane();
        tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabPane.add(LOCALE.get( "ToolSettingsTabName" ), blendBox);
        tabPane.add(LOCALE.get( "ColorSelectionTabName"), colorControls);

        add(tabPane, BorderLayout.NORTH);

        setBackground(LightZoneSkin.Colors.ToolPanesBackground);

        pcs.addPropertyChangeListener( this );
    }

    boolean isRegionsInverted() {
        return invertRegionSwitch.isRegionsInverted();
    }

    void operationChanged(Operation op) {
        layerControls.operationChanged(op);
        colorControls.operationChanged(op);
    }

    private final static String TabIndexTag = "layerControlsIndex";

    void save(XmlNode node) {
        layerControls.save(node);
        invertRegionSwitch.save(node);
        colorControls.save(node);
        node.setAttribute(TabIndexTag, Integer.toString(tabPane.getSelectedIndex()));
    }

    void restore(XmlNode node) throws XMLException {
        layerControls.restore(node);
        invertRegionSwitch.restore(node);
        colorControls.restore(node);
        if (node.hasAttribute(TabIndexTag)) {
            tabPane.setSelectedIndex(Integer.parseInt(node.getAttribute(TabIndexTag)));
        }
    }
}
