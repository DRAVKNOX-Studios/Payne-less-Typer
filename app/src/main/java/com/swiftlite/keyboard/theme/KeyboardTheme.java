package com.swiftlite.keyboard.theme;

import android.graphics.Color;

public class KeyboardTheme {
    public final int keyBg;
    public final int keyboardBg;
    public final int keyText;
    public final int specialKey;
    public final int suggestionBg;
    public final int accent;
    public final int keyBorder;
    public final boolean isDark;

    public KeyboardTheme(int keyBg, int keyboardBg, int keyText,
                         int specialKey, int suggestionBg, int accent,
                         int keyBorder, boolean isDark) {
        this.keyBg        = keyBg;
        this.keyboardBg   = keyboardBg;
        this.keyText      = keyText;
        this.specialKey   = specialKey;
        this.suggestionBg = suggestionBg;
        this.accent       = accent;
        this.keyBorder    = keyBorder;
        this.isDark       = isDark;
    }

    public static KeyboardTheme light(int accent) {
        return new KeyboardTheme(
            Color.WHITE,
            0xFFD1D5DB,
            0xFF111827,
            0xFFB0B8C4,
            0xFFF3F4F6,
            accent,
            0x47000000,
            false
        );
    }

    public static KeyboardTheme dark(int accent) {
        return new KeyboardTheme(
            0xFF1E2433,
            0xFF111827,
            0xFFF9FAFB,
            0xFF374151,
            0xFF1F2937,
            accent,
            0x55FFFFFF,
            true
        );
    }

    public static KeyboardTheme amoled(int accent) {
        return new KeyboardTheme(
            0xFF0D0D0D,
            0xFF000000,
            0xFFE5E7EB,
            0xFF1A1A1A,
            0xFF050505,
            accent,
            0x44FFFFFF,
            true
        );
    }

    public static KeyboardTheme ocean(int accent) {
        return new KeyboardTheme(
            0xFF1E3A5F,
            0xFF0F2239,
            0xFFE0F2FE,
            0xFF1E4976,
            0xFF0F2A45,
            accent,
            0x55A0C8FF,
            true
        );
    }

    public static KeyboardTheme forest(int accent) {
        return new KeyboardTheme(
            0xFF1A3B2A,
            0xFF0D2218,
            0xFFD1FAE5,
            0xFF1E4A34,
            0xFF0F2A1E,
            accent,
            0x5580C8A0,
            true
        );
    }

    public static KeyboardTheme dusk(int accent) {
        return new KeyboardTheme(
            0xFF3B1A2E,
            0xFF220D1C,
            0xFFFCE4EC,
            0xFF4E2040,
            0xFF2A1124,
            accent,
            0x55FFB0D0,
            true
        );
    }
}
