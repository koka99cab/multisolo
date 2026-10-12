package io.github.koka99cab.multisolo.core.message;

import java.util.Locale;

/** Author-selected opaque RGB color for a phantom sign. */
public record SignColor(int rgb) {
    public static final SignColor DEFAULT = new SignColor(0xC5D8CC);
    public SignColor {
        if (rgb < 0 || rgb > 0xFFFFFF) throw new IllegalArgumentException("Expected an RGB color");
    }
    public static SignColor parse(String hex) {
        if (hex == null || !hex.matches("#?[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Expected #RRGGBB");
        return new SignColor(Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16));
    }
    public String hex() { return String.format(Locale.ROOT, "#%06X", rgb); }
}
