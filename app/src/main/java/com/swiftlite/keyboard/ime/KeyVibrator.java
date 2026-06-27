package com.swiftlite.keyboard.ime;

import android.content.Context;
import com.swiftlite.keyboard.theme.ThemeManager;
import com.swiftlite.keyboard.utils.VibrationUtils;

public class KeyVibrator {

    public static void vibrate(Context context, ThemeManager themeManager, Key key) {
        if (!themeManager.isVibrateEnabled()) return;
        int duration = VibrationUtils.VIBE_NORMAL;
        if (key != null) {
            switch (key.code) {
                case KeyboardView.KEY_ENTER:
                case KeyboardView.KEY_SPACE:
                case KeyboardView.KEY_DELETE:
                case KeyboardView.KEY_SHIFT:
                case KeyboardView.KEY_NUMBERS:
                case -20:
                    duration = VibrationUtils.VIBE_ACTION;
                    break;
                case KeyboardView.KEY_CLIPBOARD:
                case KeyboardView.KEY_EMOJI:
                case KeyboardView.KEY_UNDO:
                case KeyboardView.KEY_SWITCH_LANG:
                    duration = VibrationUtils.VIBE_UTIL;
                    break;
            }
        }
        VibrationUtils.vibrate(context, duration);
    }
}
