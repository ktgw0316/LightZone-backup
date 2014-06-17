/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui;

import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;

import com.lightcrafts.mediax.jai.IHSColorSpace;

public class LightZoneSkin {
    public static String NAME = "LightZone";

    public static class Colors {
        public final static Color NeutralGray;

        static {
            float[] comps = ColorSpace.getInstance(ColorSpace.CS_sRGB).fromCIEXYZ(
                    ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB).toCIEXYZ(new float[]{0.18f, 0.18f, 0.18f}));
            NeutralGray = new Color(comps[0], comps[0], comps[0]);
        }

        static {
            Color temp = NeutralGray.darker();
            BrowserImageTypeLabelBackground = new Color(
                    temp.getRed(), temp.getGreen(), temp.getBlue(), 128
            );
        }

        public final static Color EditorBackground = NeutralGray.darker();

        public final static Color FrameBackground = new Color(28, 28, 28);

        public final static Color ToolPanesBackground = new Color(62, 62, 62);

        public final static Color LabelForeground = new Color(229, 229, 229);

        public final static Color ToolsBackground = ToolPanesBackground;
        public final static Color ToolTitleTextColor = LabelForeground;
        public final static Color ToolPanesForeground = LabelForeground;

        public final static Color BrowserBackground = NeutralGray.darker();

        public final static Color BrowserSelectHighlight = new Color(188, 188, 154);
        public final static Color BrowserLabelBackground = new Color(38, 38, 38);

        public final static Color BrowserLabelForeground = LabelForeground;

        public final static Color BrowserGroupColor = Color.gray;
        public final static Color BrowserImageTypeLabelBackground;

        public final static Color LZOrange = new Color(254, 155, 14);
        public final static Color SelectedToolBorder = relight(LZOrange, 0.7f);
    }

    static Color relight(Color color, float amount) {
        IHSColorSpace ihs = IHSColorSpace.getInstance();

        float components[] = new float[3];
        components = ihs.fromRGB(color.getColorComponents(components));
        components[0] *= amount;
        components = ihs.toRGB(components);
        return new Color(components[0], components[1], components[2]);
    }

    public static Border getImageBorder() {
        return getPaneBorder(); // new CompoundBorder(getPaneBorder(), new MatteBorder(6, 6, 6, 6, Colors.EditorBackground));
    }

    public static Border getPaneBorder() {
        return new EtchedBorder(EtchedBorder.LOWERED, new Color(48, 48, 48), new Color(23, 23, 23));
    }

    public static LookAndFeel getLightZoneLookAndFeel() {
        // Primary Colors
        UIManager.put("control", new Color(22, 22, 22)); 
        UIManager.put("info", new Color(128, 128, 128));
        UIManager.put("nimbusBase", new Color(62, 62, 62)); 
        UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
        UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
        UIManager.put("nimbusFocus", new Color(188, 188, 188)); 
        UIManager.put("nimbusGreen", new Color(176,179,50));
        UIManager.put("nimbusInfoBlue", new Color( 66, 139, 221));
        UIManager.put("nimbusLightBackground", new Color(176,176,176)); 
        UIManager.put("nimbusOrange", new Color(254,155,14));
        UIManager.put("nimbusRed", new Color(169,46,34));
        UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
        UIManager.put("nimbusSelectionBackground", new Color(90,130,195)); 
        UIManager.put("text", new Color(230, 230, 230));
        // Secondary Colors
        // UIManager.put("activeCaption", new Color(38, 38, 38));
        UIManager.put("background", new Color(62, 62, 62));
        UIManager.put("menu", new Color(62, 62, 62));
        UIManager.put("menuText", new Color(255, 255, 255));
        UIManager.put("nimbusBorder", new Color(62, 62, 62));
        UIManager.put("nimbusSelection", new Color(254, 155, 14));
        UIManager.put("scrollbar", new Color(176,176,176)); 
        UIManager.put("textForeground", new Color(255, 255, 255));

        LookAndFeel nimbus = new NimbusLookAndFeel();
        return nimbus;
    }
}
