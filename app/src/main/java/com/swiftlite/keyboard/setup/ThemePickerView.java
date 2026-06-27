package com.swiftlite.keyboard.setup;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swiftlite.keyboard.SetupActivity;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.theme.ThemeManager;
import com.swiftlite.keyboard.utils.UIUtils;

public class ThemePickerView {

    public static View buildThemePicker(Context context, ThemeManager themeManager, KeyboardTheme theme, SetupActivity activity) {
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(3);
        grid.setPadding(0, UIUtils.dp(context, 4), 0, 0);

        String[] themeNames = themeManager.getThemeNames();
        int current = themeManager.getThemeId();
        for (int i = 0; i < themeNames.length; i++) {
            final int idx = i;
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(UIUtils.dp(context, 4), UIUtils.dp(context, 8),
                            UIUtils.dp(context, 4), UIUtils.dp(context, 8));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            card.setLayoutParams(lp);

            View swatch = new View(context);
            swatch.setLayoutParams(new LinearLayout.LayoutParams(UIUtils.dp(context, 56), UIUtils.dp(context, 32)));
            UIUtils.roundBg(swatch, previewColor(i), UIUtils.dp(context, 8));
            if (i == current) {
                GradientDrawable gd = (GradientDrawable) swatch.getBackground();
                gd.setStroke(UIUtils.dp(context, 2), theme.accent);
            }

            TextView name = new TextView(context);
            name.setText(themeNames[i]);
            name.setTextSize(11);
            name.setTextColor(i == current ? theme.accent : theme.keyText);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, UIUtils.dp(context, 4), 0, 0);

            card.addView(swatch);
            card.addView(name);
            card.setOnClickListener(v -> {
                themeManager.setTheme(idx);
                activity.setLastThemeId(idx);
                activity.recreate();
            });
            grid.addView(card);
        }
        return grid;
    }

    public static View buildAccentPicker(Context context, ThemeManager themeManager, KeyboardTheme theme, SetupActivity activity) {
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(6);
        grid.setPadding(0, UIUtils.dp(context, 4), 0, 0);

        int[] accentColors = themeManager.getAccentColors();
        int current = themeManager.getAccentColor();
        for (int i = 0; i < accentColors.length; i++) {
            final int color = accentColors[i];
            View dot = new View(context);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width  = UIUtils.dp(context, 36);
            lp.height = UIUtils.dp(context, 36);
            lp.setMargins(UIUtils.dp(context, 10), UIUtils.dp(context, 10),
                          UIUtils.dp(context, 10), UIUtils.dp(context, 10));
            dot.setLayoutParams(lp);

            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(color);
            if (color == current) gd.setStroke(UIUtils.dp(context, 3), theme.keyText);
            dot.setBackground(gd);

            dot.setOnClickListener(v -> {
                themeManager.setAccent(color);
                activity.setLastAccent(color);
                activity.recreate();
            });
            grid.addView(dot);
        }
        return grid;
    }

    private static int previewColor(int id) {
        switch (id) {
            case ThemeManager.THEME_DARK:   return 0xFF1E2433;
            case ThemeManager.THEME_AMOLED: return 0xFF000000;
            case ThemeManager.THEME_OCEAN:  return 0xFF0F2239;
            case ThemeManager.THEME_FOREST: return 0xFF0D2218;
            case ThemeManager.THEME_DUSK:   return 0xFF3B1A2E;
            default:                        return 0xFFD1D5DB;
        }
    }
}
