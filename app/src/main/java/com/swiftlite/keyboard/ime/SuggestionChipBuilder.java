package com.swiftlite.keyboard.ime;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.utils.UIUtils;
import com.swiftlite.keyboard.utils.VibrationUtils;

public class SuggestionChipBuilder {

    public static void build(Context context, String[] fitting, int[] chipWidths, 
                            int chipPadPx, int sepPx, KeyboardTheme theme, 
                            SwiftLiteIME ime, LinearLayout container) {
        int childCount = container.getChildCount();
        int neededViews = fitting.length * 2 - 1;
        if (fitting.length == 0) neededViews = 0;

        if (childCount > neededViews) {
            container.removeViews(neededViews, childCount - neededViews);
        }

        for (int i = 0; i < fitting.length; i++) {
            final String word = fitting[i];
            int viewIdx = i * 2;
            TextView tv;
            if (viewIdx < container.getChildCount()) {
                View v = container.getChildAt(viewIdx);
                if (v instanceof TextView) { tv = (TextView) v; }
                else {
                    container.removeViewAt(viewIdx);
                    tv = new TextView(context);
                    container.addView(tv, viewIdx);
                }
            } else {
                tv = new TextView(context);
                container.addView(tv);
            }

            tv.setText(word);
            tv.setTextSize(12);
            tv.setPadding(chipPadPx, UIUtils.dp(context, 2), chipPadPx, UIUtils.dp(context, 2));
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setOnClickListener(v -> {
                if (ime.getThemeManager().isVibrateEnabled()) {
                    VibrationUtils.vibrate(context, VibrationUtils.VIBE_SUGGESTION);
                }
                ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.92f, 1f).setDuration(120).start();
                ime.commitSuggestion(word);
            });
            tv.setTypeface(null, i == 0 ? Typeface.BOLD : Typeface.NORMAL);
            if (theme != null) tv.setTextColor(theme.keyText);

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tv.getLayoutParams();
            if (lp == null) lp = new LinearLayout.LayoutParams(chipWidths[i], LinearLayout.LayoutParams.MATCH_PARENT);
            else { lp.width = chipWidths[i]; lp.height = LinearLayout.LayoutParams.MATCH_PARENT; }
            tv.setLayoutParams(lp);

            if (i < fitting.length - 1) {
                int sepIdx = viewIdx + 1;
                View sep;
                if (sepIdx < container.getChildCount()) {
                    sep = container.getChildAt(sepIdx);
                } else {
                    sep = new View(context);
                    container.addView(sep);
                }
                LinearLayout.LayoutParams sepLp = (LinearLayout.LayoutParams) sep.getLayoutParams();
                if (sepLp == null) sepLp = new LinearLayout.LayoutParams(sepPx, LinearLayout.LayoutParams.MATCH_PARENT);
                else { sepLp.width = sepPx; sepLp.height = LinearLayout.LayoutParams.MATCH_PARENT; }
                sep.setLayoutParams(sepLp);
                sep.setAlpha(0.2f);
                if (theme != null) sep.setBackgroundColor(theme.keyText);
            }
        }
    }
}
