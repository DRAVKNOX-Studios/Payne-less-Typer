package com.swiftlite.keyboard.utils;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;

public class UIUtils {

    public static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static void roundBg(View v, int color, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        v.setBackground(gd);
    }

    public static View vspace(Context context, int h, float weight) {
        View v = new View(context);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h, weight));
        return v;
    }

    public static TextView makeLabel(Context context, String text, int accentColor) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(accentColor);
        tv.setPadding(0, dp(context, 12), 0, dp(context, 8));
        return tv;
    }
}
