package com.kodedu.component;

import javafx.scene.Parent;
import javafx.scene.paint.Color;

/**
 * Live chrome contrast. Does not change AsciiDoc document preview stylesheets.
 * <p>
 * Sliders (range 0.4–1.4, default 0.8 — the old minimum, now center-ish):
 * <ul>
 *   <li>outline — borders and control outlines</li>
 *   <li>background — editor, preview, files, and top-bar fills</li>
 *   <li>chrome — tab fills, menus, status, remaining chrome, and control/icon text</li>
 * </ul>
 * Overlay is a tint mixed onto fills only, not document body text.
 */
public final class ChromeContrast {

    public static final double MIN = 0.4;
    public static final double MAX = 1.4;
    public static final double DEFAULT = 0.8;
    private static final double OVERLAY_MIX = 0.18;

    private ChromeContrast() {
    }

    public static void apply(Parent root, boolean dark, double outlineContrast, double backgroundContrast,
                             double chromeContrast, String overlayHex, String fontFamily) {
        if (root == null) {
            return;
        }
        Palette palette = dark ? Palette.DARK : Palette.LIGHT;
        double outline = clamp(outlineContrast, MIN, MAX);
        double background = clamp(backgroundContrast, MIN, MAX);
        double chrome = clamp(chromeContrast, MIN, MAX);
        Color overlay = parseOverlay(overlayHex);

        Color frameBg = tint(shiftFrom(palette.sceneBg, palette.frameBg, background), overlay);
        Color editorBg = tint(shiftFrom(palette.sceneBg, palette.editorBg, background), overlay);
        Color previewBg = tint(shiftFrom(palette.sceneBg, palette.previewBg, background), overlay);
        Color workdirBg = tint(shiftFrom(palette.sceneBg, palette.workdirBg, background), overlay);
        Color topBg = tint(shiftFrom(palette.sceneBg, palette.topBg, background), overlay);
        Color buttonBg = tint(shiftFrom(palette.sceneBg, palette.buttonBg, chrome), overlay);
        Color tabBg = tint(shiftFrom(palette.sceneBg, palette.tabBg, chrome), overlay);
        Color menuBg = tint(shiftFrom(palette.sceneBg, palette.menuBg, chrome), overlay);
        Color statusBg = tint(shiftFrom(palette.sceneBg, palette.statusBg, chrome), overlay);

        Color frameBorder = shiftFrom(frameBg, palette.frameBorder, outline);
        Color buttonBorder = shiftFrom(buttonBg, palette.buttonBorder, outline);
        Color text = shiftFrom(frameBg, palette.text, chrome);
        Color icon = shiftFrom(frameBg, palette.icon, chrome);

        StringBuilder style = new StringBuilder();
        style.append("-chrome-frame-bg: ").append(css(frameBg)).append(";");
        style.append("-chrome-editor-bg: ").append(css(editorBg)).append(";");
        style.append("-chrome-preview-bg: ").append(css(previewBg)).append(";");
        style.append("-chrome-workdir-bg: ").append(css(workdirBg)).append(";");
        style.append("-chrome-top-bg: ").append(css(topBg)).append(";");
        style.append("-chrome-button-bg: ").append(css(buttonBg)).append(";");
        style.append("-chrome-tab-bg: ").append(css(tabBg)).append(";");
        style.append("-chrome-menu-bg: ").append(css(menuBg)).append(";");
        style.append("-chrome-status-bg: ").append(css(statusBg)).append(";");
        style.append("-chrome-frame-border: ").append(css(frameBorder)).append(";");
        style.append("-chrome-button-border: ").append(css(buttonBorder)).append(";");
        style.append("-chrome-text: ").append(css(text)).append(";");
        style.append("-chrome-icon: ").append(css(icon)).append(";");
        if (fontFamily != null && !fontFamily.isBlank()) {
            style.append(String.format(" -fx-font-family: '%s';", fontFamily.replace("'", "\\'")));
        }
        root.setStyle(style.toString());
    }

    private static Color parseOverlay(String overlayHex) {
        if (overlayHex == null || overlayHex.isBlank()) {
            return null;
        }
        try {
            Color color = Color.web(overlayHex.trim());
            return color.getOpacity() <= 0 ? null : color;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Color tint(Color color, Color overlay) {
        if (overlay == null) {
            return color;
        }
        return shiftFrom(color, overlay, OVERLAY_MIX);
    }

    private static Color shiftFrom(Color base, Color target, double contrast) {
        double r = clamp01(base.getRed() + (target.getRed() - base.getRed()) * contrast);
        double g = clamp01(base.getGreen() + (target.getGreen() - base.getGreen()) * contrast);
        double b = clamp01(base.getBlue() + (target.getBlue() - base.getBlue()) * contrast);
        return Color.color(r, g, b);
    }

    private static String css(Color color) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp01(double value) {
        return clamp(value, 0, 1);
    }

    private record Palette(Color sceneBg, Color frameBg, Color editorBg, Color previewBg, Color workdirBg,
                           Color topBg, Color buttonBg, Color tabBg, Color menuBg, Color statusBg,
                           Color frameBorder, Color buttonBorder, Color icon, Color text) {
        static final Palette DARK = new Palette(
                Color.web("#191A1B"),
                Color.web("#1d1e20"),
                Color.web("#252628"),
                Color.web("#2a2c2e"),
                Color.web("#222324"),
                Color.web("#1a1b1c"),
                Color.web("#2c2d2e"),
                Color.web("#2a2b2d"),
                Color.web("#252628"),
                Color.web("#18191a"),
                Color.web("#3a3b3d"),
                Color.web("#4a4b4d"),
                Color.web("#c8c8c8"),
                Color.web("#d6d6d6")
        );
        static final Palette LIGHT = new Palette(
                Color.web("#e4e5e7"),
                Color.web("#ececef"),
                Color.web("#f3f3f3"),
                Color.web("#f6f6f5"),
                Color.web("#efefef"),
                Color.web("#e8e9eb"),
                Color.web("#f0f0f0"),
                Color.web("#e6e6e8"),
                Color.web("#f4f4f4"),
                Color.web("#e4e5e7"),
                Color.web("#c8c9cc"),
                Color.web("#d0d1d4"),
                Color.web("#3a3a3a"),
                Color.web("#2a2a2a")
        );
    }
}
