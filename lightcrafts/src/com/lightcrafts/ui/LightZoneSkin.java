/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui;

import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.*;
import javax.swing.UIManager.*;
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

    public static void setLookAndFeel()
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, UnsupportedLookAndFeelException {
        // Primary Colors
        UIManager.put("control", new Color(22, 22, 22));
        UIManager.put("info", new Color(38, 38, 38));
        UIManager.put("nimbusBase", new Color(22, 22, 22));
        UIManager.put("nimbusDisabledText", Color.GRAY);
        UIManager.put("nimbusFocus", Colors.LZOrange);
        UIManager.put("nimbusLightBackground", Color.GRAY);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusSelectionBackground", new Color(38, 38, 38));
        UIManager.put("text", new Color(230, 230, 230));

        // Secondary Colors
        UIManager.put("nimbusBlueGrey", new Color(22, 22, 22));
        UIManager.put("textForeground", Color.WHITE);

        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();

        // TODO: Make font sizes changeable
        final String fontFamily = Font.SANS_SERIF;
        int size = 13;

        Font defaultFont = new Font(fontFamily, Font.PLAIN, size);
        // Font controlFont = new Font(fontFamily, Font.PLAIN, size);
        Font menuFont    = new Font(fontFamily, Font.PLAIN, size);
        Font titleFont   = new Font(fontFamily, Font.BOLD, (int)(0.85 * size));
        // Font windowTitleFont = new Font(fontFamily, Font.BOLD, (int)(1.23 * size));
        // Font smallFont   = new Font(fontFamily, Font.PLAIN, size);
        // Font messageFont = new Font(fontFamily, Font.BOLD, size);

        defaults.put("defaultFont", defaultFont);

        // Menu
        defaults.put("Menu[Disabled].textForeground", Color.DARK_GRAY);
        defaults.put("Menu[Enabled].textForeground", Color.WHITE);
        defaults.put("Menu[Enabled+Selected].textForeground", Color.WHITE);
        defaults.put("Menu.font", menuFont);

        // MenuItem
        defaults.put("MenuItem[Disabled].textForeground", Color.DARK_GRAY);
        defaults.put("MenuItem[Enabled].textForeground", Color.WHITE);
        // defaults.put("MenuItem.font", menuFont);

        // ComboBox
        defaults.put("ComboBox.background", Color.BLACK);
        defaults.put("ComboBox.foreground", Color.WHITE);
        // defaults.put("ComboBox.font", defaultFont);

        // Button
        defaults.put("Button.contentMargins", new Insets(6, 6, 6, 6));
        defaults.put("Button[Default].backgroundPainter", null);
        defaults.put("Button[Enabled].backgroundPainter", null);
        defaults.put("Button[Disabled].backgroundPainter", null);
        // defaults.put("Button.font", defaultFont);

        // ToggleButton
        defaults.put("ToggleButton.contentMargins", new Insets(6, 6, 6, 6));
        defaults.put("Button[Default].backgroundPainter", null);
        defaults.put("Button[Enabled].backgroundPainter", null);
        defaults.put("Button[Disabled+Selected].backgroundPainter", null);
        defaults.put("Button[Disabled].backgroundPainter", null);
        // defaults.put("ToggleButton.font", defaultFont);

        // BoxedButton
        defaults.put("TitledBorder.font", titleFont);
    }
}
