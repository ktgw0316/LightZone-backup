/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2013 Masahiro Kitagawa */

package com.lightcrafts.ui;

import org.pushingpixels.substance.api.*;
import org.pushingpixels.substance.api.colorscheme.EbonyColorScheme;
import org.pushingpixels.substance.api.colorscheme.SteelBlueColorScheme;
import org.pushingpixels.substance.api.colorscheme.BaseColorScheme;
import org.pushingpixels.substance.api.painter.border.ClassicBorderPainter;
import org.pushingpixels.substance.api.painter.decoration.ClassicDecorationPainter;
import org.pushingpixels.substance.api.painter.fill.GlassFillPainter;
import org.pushingpixels.substance.api.painter.highlight.ClassicHighlightPainter;
import org.pushingpixels.substance.api.shaper.ClassicButtonShaper;

import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;

import org.pushingpixels.substance.api.fonts.FontSet;
import org.pushingpixels.substance.api.fonts.FontPolicy;
import com.lightcrafts.mediax.jai.IHSColorSpace;

public class LightZoneSkin extends SubstanceSkin {
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

    public static class CustomColorScheme extends BaseColorScheme {
        private final Color mainUltraLightColor;
        private final Color mainExtraLightColor;
        private final Color mainLightColor;
        private final Color mainMidColor;
        private final Color mainDarkColor;
        private final Color mainUltraDarkColor;
        private final Color foregroundColor;

        public CustomColorScheme(Color baseColor) {
            super("Custom", false);

            mainUltraLightColor = relight(baseColor, 0.95f);
            mainExtraLightColor = relight(baseColor, 0.85f);
            mainLightColor = relight(baseColor, 0.7f);
            mainMidColor = relight(baseColor, 0.6f);
            mainDarkColor = relight(baseColor, 0.5f);
            mainUltraDarkColor = relight(baseColor, 0.4f);
            foregroundColor = Color.white;
        }

        public Color getForegroundColor() { return foregroundColor; }
        public Color getUltraLightColor() { return mainUltraLightColor; }
        public Color getExtraLightColor() { return mainExtraLightColor; }
        public Color getLightColor() { return mainLightColor; }
        public Color getMidColor() { return mainMidColor; }
        public Color getDarkColor() { return mainDarkColor; }
        public Color getUltraDarkColor() { return mainUltraDarkColor; }
    }

    public static class CustomSkin extends LightZoneSkin {
        private String CustomSkinName;
        public CustomSkin(SubstanceColorScheme colorScheme, String name) {
            SubstanceColorScheme basicScheme = new EbonyColorScheme().tint(0.05);
            SubstanceColorScheme activeScheme = colorScheme;
            SubstanceColorScheme enabledScheme = basicScheme.shade(0.2);
            SubstanceColorScheme disabledScheme = basicScheme.shade(0.6);
 
            SubstanceColorSchemeBundle defaultSchemeBundle = new SubstanceColorSchemeBundle(
                activeScheme, enabledScheme, disabledScheme);
            this.registerDecorationAreaSchemeBundle(defaultSchemeBundle, DecorationAreaType.NONE);
 
            this.setSelectedTabFadeStart(0.4);
            this.setSelectedTabFadeEnd(0.7);
 
            CustomSkinName = name;
        }
 
        public String getDisplayName() {
            return CustomSkinName ;
        }
    }

    public static final SubstanceSkin orangeSkin = new CustomSkin(new CustomColorScheme(Colors.LZOrange), "Orange");

    public LightZoneSkin() {
        SubstanceColorScheme basicScheme = new EbonyColorScheme();
        SubstanceColorScheme activeScheme = basicScheme;
        SubstanceColorScheme enabledScheme = basicScheme.shade(0.2);
        SubstanceColorScheme disabledScheme = basicScheme.shade(0.6);

        SubstanceColorSchemeBundle defaultSchemeBundle = new SubstanceColorSchemeBundle(
            activeScheme, enabledScheme, disabledScheme);
        SubstanceColorScheme kitchenSinkScheme = basicScheme;
        SubstanceColorScheme highlightColorScheme = basicScheme;
        defaultSchemeBundle.registerHighlightColorScheme(highlightColorScheme);

        this.registerDecorationAreaSchemeBundle(defaultSchemeBundle, DecorationAreaType.NONE);
        this.registerAsDecorationArea(activeScheme,
            DecorationAreaType.PRIMARY_TITLE_PANE,
            DecorationAreaType.SECONDARY_TITLE_PANE);

        this.registerAsDecorationArea(enabledScheme,
            DecorationAreaType.PRIMARY_TITLE_PANE_INACTIVE,
            DecorationAreaType.SECONDARY_TITLE_PANE_INACTIVE);

        this.registerAsDecorationArea(kitchenSinkScheme,
            DecorationAreaType.GENERAL, DecorationAreaType.HEADER);

        this.buttonShaper = new ClassicButtonShaper();
        this.fillPainter = new GlassFillPainter();
        this.decorationPainter = new ClassicDecorationPainter();
        this.borderPainter = new ClassicBorderPainter();
        this.highlightPainter = new ClassicHighlightPainter();
        this.watermark = null;

        this.setSelectedTabFadeStart(0.3);
        this.setSelectedTabFadeEnd(0.6);
    }

    public String getDisplayName() {
        return NAME;
    }

    public static class LightZoneFontSet implements FontSet {
        FontUIResource controlFont;
        FontUIResource menuFont;
        FontUIResource titleFont;
        FontUIResource windowTitleFont;
        FontUIResource smallFont;
        FontUIResource messageFont;

        String fontFamily = Font.SANS_SERIF;

        public LightZoneFontSet() {
            controlFont = new FontUIResource(fontFamily, Font.BOLD, 11);
            menuFont = new FontUIResource(fontFamily, Font.PLAIN, 11);
            titleFont = new FontUIResource(fontFamily, Font.BOLD, 9);
            windowTitleFont = new FontUIResource(fontFamily, Font.BOLD, 14);
            smallFont = new FontUIResource(fontFamily, Font.PLAIN, 11);
            messageFont = new FontUIResource(fontFamily, Font.BOLD, 11);
        }

        public FontUIResource getControlFont() {
            return controlFont;
        }

        public FontUIResource getMenuFont() {
            return menuFont;
        }

        public FontUIResource getTitleFont() {
            return titleFont;
        }

        public FontUIResource getWindowTitleFont() {
            return windowTitleFont;
        }

        public FontUIResource getSmallFont() {
            return smallFont;
        }

        public FontUIResource getMessageFont() {
            return messageFont;
        }
    }

    public static final FontSet fontSet = new LightZoneFontSet();

    public static Border getImageBorder() {
        return getPaneBorder(); // new CompoundBorder(getPaneBorder(), new MatteBorder(6, 6, 6, 6, Colors.EditorBackground));
    }

    public static Border getPaneBorder() {
        return new EtchedBorder(EtchedBorder.LOWERED, new Color(48, 48, 48), new Color(23, 23, 23));
    }

    private static final RenderingHints aliasingRenderHints;

    static {
        aliasingRenderHints = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );
    }


    public static class LightZoneLookAndFeel extends SubstanceLookAndFeel {
        protected void initClassDefaults(UIDefaults table) {
            super.initClassDefaults(table);
        }

        public LightZoneLookAndFeel() {
            super(new LightZoneSkin());
        }
    }

    public static LookAndFeel getLightZoneLookAndFeel() {
        LookAndFeel substance = new LightZoneLookAndFeel();

        LightZoneLookAndFeel.setSkin(new LightZoneSkin());

        FontPolicy newFontPolicy = new FontPolicy() {
            public FontSet getFontSet(String lafName,
                                      UIDefaults table) {
                return new LightZoneSkin.LightZoneFontSet();
            }
        };

        LightZoneLookAndFeel.setFontPolicy(newFontPolicy);

        UIManager.put("ToolTip.backgroundInactive", substance.getDefaults().get("ToolTip.background"));
        UIManager.put("ToolTip.foregroundInactive", substance.getDefaults().get("ToolTip.foreground"));

        return substance;
    }
}
