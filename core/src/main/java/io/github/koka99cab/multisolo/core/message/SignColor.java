package io.github.koka99cab.multisolo.core.message;

/** Legacy protocol v2 field, retained for stored messages and ignored by rendering. */
public record SignColor(int rgb) {
    public static final SignColor DEFAULT = new SignColor(0x000000);
    public SignColor {
        if (rgb < 0 || rgb > 0xFFFFFF) throw new IllegalArgumentException("Expected an RGB color");
    }
}
