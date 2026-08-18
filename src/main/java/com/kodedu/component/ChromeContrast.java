package com.kodedu.component;

import javafx.scene.Parent;
import javafx.scene.paint.Color;

/**
 * Live chrome contrast: outline (borders/icons) and background fills.
 * Does not change AsciiDoc document preview stylesheets.
 */
public final class ChromeContrast {

    private ChromeContrast() {
    }

    public static void apply(Parent root, boolean dark, double outlineContrast, double backgroundContrast,
                             String fontFamily) {
        if (root == null) {
            return;
        }
        Palette palette = dark ? Palette.DARK : Palette.LIGHT;
        double outline = clamp(outlineContrast, 0.8, 1.2);
        double background = clamp(backgroundContrast, 0.8, 1.2);

        Color frameBg = shiftFrom(palette.sceneBg, palette.frameBg, background);
        Color editorBg = shiftFrom(palette.sceneBg, palette.editorBg, background);
        Color previewBg = shiftFrom(palette.sceneBg, palette.previewBg, background);
        Color workdirBg = shiftFrom(palette.sceneBg, palette.workdirBg, background);
        Color topBg = shiftFrom(palette.sceneBg, palette.topBg, background);
        Color buttonBg = shiftFrom(palette.sceneBg, palette.buttonBg, background);

        Color frameBorder = shiftFrom(frameBg, palette.frameBorder, outline);
        Color buttonBorder = shiftFrom(buttonBg, palette.buttonBorder, outline);
        Color icon = shiftFrom(frameBg, palette.icon, outline);

        StringBuilder style = new StringBuilder();
        style.append("-chrome-frame-bg: ").append(css(frameBg)).append(";");
        style.append("-chrome-editor-bg: ").append(css(editorBg)).append(";");
        style.append("-chrome-preview-bg: ").append(css(previewBg)).append(";");
        style.append("-chrome-workdir-bg: ").append(css(workdirBg)).append(";");
        style.append("-chrome-top-bg: ").append(css(topBg)).append(";");
        style.append("-chrome-button-bg: ").append(css(buttonBg)).append(";");
        style.append("-chrome-frame-border: ").append(css(frameBorder)).append(";");
        style.append("-chrome-button-border: ").append(css(buttonBorder)).append(";");
        style.append("-chrome-icon: ").append(css(icon)).append(";");
        if (fontFamily != null && !fontFamily.isBlank()) {
            style.append(String.format(" -fx-font-family: '%s';", fontFamily.replace("'", "\\'")));
        }
        root.setStyle(style.toString());
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
                           Color topBg, Color buttonBg, Color frameBorder, Color buttonBorder, Color icon) {
        static final Palette DARK = new Palette(
                Color.web("#191A1B"),
                Color.web("#1d1e20"),
                Color.web("#252628"),
                Color.web("#2a2c2e"),
                Color.web("#222324"),
                Color.web("#1a1b1c"),
                Color.web("#2c2d2e"),
                Color.web("#3a3b3d"),
                Color.web("#4a4b4d"),
                Color.web("#c8c8c8")
        );
        static final Palette LIGHT = new Palette(
                Color.web("#e4e5e7"),
                Color.web("#ececef"),
                Color.web("#f3f3f3"),
                Color.web("#f6f6f5"),
                Color.web("#efefef"),
                Color.web("#e8e9eb"),
                Color.web("#f0f0f0"),
                Color.web("#c8c9cc"),
                Color.web("#d0d1d4"),
                Color.web("#3a3a3a")
        );
    }
}
