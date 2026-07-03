package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.content.SharedPreferences;

public class LayoutController {
    public static final int MODE_NORMAL = 0;
    public static final int MODE_ONE_HANDED_L = 1;
    public static final int MODE_ONE_HANDED_R = 2;

    private final SharedPreferences prefs;
    private int mode;
    private float hScale;

    public LayoutController(Context ctx) {
        prefs = ctx.getSharedPreferences("kb_layout_v3", Context.MODE_PRIVATE);
        mode = prefs.getInt("mode", MODE_NORMAL);
        hScale = prefs.getFloat("h_scale", 1.0f);
    }

    public int getMode() { return mode; }
    public float getHScale() { return hScale; }

    public void setMode(int m) {
        mode = m;
        prefs.edit().putInt("mode", mode).apply();
    }

    public void setHScale(float h) {
        hScale = Math.min(Math.max(h, 0.5f), 1.5f);
        prefs.edit().putFloat("h_scale", hScale).apply();
    }

    public void toggleOneHanded(boolean left) {
        int target = left ? MODE_ONE_HANDED_L : MODE_ONE_HANDED_R;
        setMode(mode == target ? MODE_NORMAL : target);
    }
}
