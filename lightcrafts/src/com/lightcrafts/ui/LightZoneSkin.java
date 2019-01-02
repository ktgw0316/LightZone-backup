/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2013-     Masahiro Kitagawa */

package com.lightcrafts.ui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.pushingpixels.substance.api.colorscheme.BaseDarkColorScheme;
import org.pushingpixels.substance.api.fonts.FontPolicy;
import org.pushingpixels.substance.api.fonts.FontSet;
import org.pushingpixels.substance.api.*;
import org.pushingpixels.substance.api.colorscheme.EbonyColorScheme;
import org.pushingpixels.substance.api.painter.border.FlatBorderPainter;
import org.pushingpixels.substance.api.painter.decoration.FlatDecorationPainter;
import org.pushingpixels.substance.api.painter.fill.MatteFillPainter;
import org.pushingpixels.substance.api.painter.highlight.ClassicHighlightPainter;
import org.pushingpixels.substance.api.shaper.ClassicButtonShaper;

import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;

import javax.media.jai.IHSColorSpace;

public class LightZoneSkin extends SubstanceSkin {
    public static final String NAME = "LightZone";

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

    private static Color relight(Color color, float amount) {
        IHSColorSpace ihs = IHSColorSpace.getInstance();

        float[] components = new float[3];
        components = ihs.fromRGB(color.getColorComponents(components));
        components[0] *= amount;
        components = ihs.toRGB(components);
        return new Color(components[0], components[1], components[2]);
    }

    @Getter
    public static class CustomColorScheme extends BaseDarkColorScheme {
        private final Color ultraLightColor;
        private final Color extraLightColor;
        private final Color lightColor;
        private final Color midColor;
        private final Color darkColor;
        private final Color ultraDarkColor;
        private final Color foregroundColor;

        public CustomColorScheme(Color baseColor) {
            super("Custom");

            ultraLightColor = relight(baseColor, 0.95f);
            extraLightColor = relight(baseColor, 0.85f);
            lightColor = relight(baseColor, 0.7f);
            midColor = relight(baseColor, 0.6f);
            darkColor = relight(baseColor, 0.5f);
            ultraDarkColor = relight(baseColor, 0.4f);
            foregroundColor = Color.white;
        }
    }

    public LightZoneSkin() {
        SubstanceColorScheme activeScheme = new EbonyColorScheme();
        SubstanceColorScheme enabledScheme = activeScheme.shade(0.2);
        SubstanceColorScheme disabledScheme = activeScheme.shade(0.6);

        SubstanceColorSchemeBundle defaultSchemeBundle = new SubstanceColorSchemeBundle(
                activeScheme, enabledScheme, disabledScheme);

        SubstanceColorScheme highlightScheme = activeScheme;
        defaultSchemeBundle.registerHighlightColorScheme(highlightScheme, 0.6f,
                ComponentState.ROLLOVER_UNSELECTED);
        defaultSchemeBundle.registerHighlightColorScheme(highlightScheme, 0.8f,
                ComponentState.SELECTED);
        defaultSchemeBundle.registerHighlightColorScheme(highlightScheme, 1.0f,
                ComponentState.ROLLOVER_SELECTED);
        defaultSchemeBundle.registerHighlightColorScheme(highlightScheme,
                0.75f, ComponentState.ARMED, ComponentState.ROLLOVER_ARMED);


        defaultSchemeBundle.registerColorScheme(new EbonyColorScheme(),
                ColorSchemeAssociationKind.HIGHLIGHT_BORDER, ComponentState.getActiveStates());

        defaultSchemeBundle.registerColorScheme(disabledScheme, 0.5f,
                ComponentState.DISABLED_UNSELECTED);
        defaultSchemeBundle.registerColorScheme(highlightScheme, 0.5f,
                ComponentState.DISABLED_SELECTED);

        defaultSchemeBundle.registerColorScheme(activeScheme,
                ColorSchemeAssociationKind.BORDER,
                ComponentState.DISABLED_SELECTED);



        this.registerDecorationAreaSchemeBundle(defaultSchemeBundle, DecorationAreaType.NONE);
        this.registerAsDecorationArea(activeScheme,
            DecorationAreaType.PRIMARY_TITLE_PANE,
            DecorationAreaType.SECONDARY_TITLE_PANE);

        this.registerAsDecorationArea(enabledScheme,
                DecorationAreaType.PRIMARY_TITLE_PANE,
                DecorationAreaType.SECONDARY_TITLE_PANE,
                DecorationAreaType.HEADER, DecorationAreaType.FOOTER,
                DecorationAreaType.GENERAL, DecorationAreaType.TOOLBAR);

        this.watermarkScheme = activeScheme.shade(0.4);

//        this.setTabFadeStart(0.3);
//        this.setTabFadeEnd(0.6);

        this.buttonShaper = new ClassicButtonShaper();
        this.fillPainter = new MatteFillPainter();
        this.decorationPainter = new FlatDecorationPainter();
        this.highlightPainter = new ClassicHighlightPainter();
        this.borderPainter = new FlatBorderPainter();
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Getter
    @RequiredArgsConstructor(access= AccessLevel.PRIVATE)
    private static class LightZoneFontSet implements FontSet {
        private final FontUIResource controlFont;
        private final FontUIResource menuFont;
        private final FontUIResource titleFont;
        private final FontUIResource windowTitleFont;
        private final FontUIResource smallFont;
        private final FontUIResource messageFont;

        private static final String fontFamily = Font.SANS_SERIF;

        public LightZoneFontSet() {
            this(new FontUIResource(fontFamily, Font.PLAIN, 13),
                    new FontUIResource(fontFamily, Font.PLAIN, 13),
                    new FontUIResource(fontFamily, Font.BOLD, 11),
                    new FontUIResource(fontFamily, Font.BOLD, 16),
                    new FontUIResource(fontFamily, Font.PLAIN, 13),
                    new FontUIResource(fontFamily, Font.BOLD, 13));
        }
    }

    public static final FontSet fontSet = new LightZoneFontSet();

    public static Border getImageBorder() {
        return getPaneBorder();
    }

    public static Border getPaneBorder() {
        return new EtchedBorder(EtchedBorder.LOWERED, new Color(48, 48, 48), new Color(23, 23, 23));
    }

    public static class LightZoneLookAndFeel extends SubstanceLookAndFeel {
        public LightZoneLookAndFeel() {
            super(new LightZoneSkin());
        }
    }

    private static final LookAndFeel substance = new LightZoneLookAndFeel();

    public static LookAndFeel getLightZoneLookAndFeel() {
        return substance;
    }

    public static void setLightZoneLookAndFeel() {
        try {
            UIManager.setLookAndFeel(substance);
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        UIManager.put("ToolTip.backgroundInactive", substance.getDefaults().get("ToolTip.background"));
        UIManager.put("ToolTip.foregroundInactive", substance.getDefaults().get("ToolTip.foreground"));

        SwingUtilities.invokeLater(() -> {
            SubstanceLookAndFeel.setSkin(new LightZoneSkin());
            final FontPolicy newFontPolicy = (lafName, uiDefaults) -> new LightZoneFontSet();
            SubstanceLookAndFeel.setFontPolicy(newFontPolicy);
        });
    }
}
