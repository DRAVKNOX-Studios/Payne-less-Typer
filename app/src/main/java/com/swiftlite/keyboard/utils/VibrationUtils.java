package com.swiftlite.keyboard.utils;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibrationUtils {

    public static final int VIBE_NORMAL = 5;
    public static final int VIBE_ACTION = 10;
    public static final int VIBE_UTIL = 15;
    public static final int VIBE_SUGGESTION = 20;

    public static void vibrate(Context context, int duration) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
