package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.util.DisplayMetrics;

public class AdaptiveHeight {
    public static int getBaseHeight(Context ctx) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        boolean land = dm.widthPixels > dm.heightPixels;
        float ratio = land ? 0.45f : 0.38f;
        float scale = 1.0f;
        if (ctx instanceof SwiftLiteIME) {
            LayoutController lc = ((SwiftLiteIME) ctx).getLayoutController();
            if (lc != null) scale = lc.getHScale();
        }
        int h = (int) (dm.heightPixels * ratio * scale);
        int min = (int) (150 * dm.density);
        int max = (int) (600 * dm.density);
        return Math.min(Math.max(h, min), max);
    }
}
