package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swiftlite.keyboard.theme.KeyboardTheme;

import java.util.ArrayList;
import java.util.List;

public class PopupViewFactory {

    public static View createStandardPopup(Context context, List<String> opts, KeyboardTheme theme, int itemSz, float density) {
        float rowCorner = density * 10f;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipToOutline(true);
        row.setClipChildren(true);

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(theme != null ? theme.specialKey : 0xFF374151);
        rowBg.setCornerRadius(rowCorner);
        if (theme != null)
            rowBg.setStroke(Math.round(0.9f * density * 1.5f), theme.keyBorder);
        row.setBackground(rowBg);
        row.setTag("popup_row");

        for (int i = 0; i < opts.size(); i++) {
            TextView tv = new TextView(context);
            tv.setText(opts.get(i));
            tv.setTextSize(16);
            tv.setTextColor(theme != null ? theme.keyText : Color.WHITE);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(itemSz, itemSz));
            tv.setTag(i);
            row.addView(tv);
        }
        return row;
    }

    public static View createScrollablePopup(Context context, List<String> opts, KeyboardTheme theme, 
                                           float density, int maxW, float itemHDp, float padHDp, float textSp) {
        int itemH = (int)(density * itemHDp);
        int hPad = (int)(density * padHDp);
        float sizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSp, context.getResources().getDisplayMetrics());

        Paint measPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        measPaint.setTextSize(sizePx);
        int[] itemWidths = new int[opts.size()];
        for (int i = 0; i < opts.size(); i++) {
            itemWidths[i] = (int) Math.ceil(measPaint.measureText(opts.get(i))) + hPad;
        }

        int maxItems = (opts.size() > 5) ? (opts.size() + 1) / 2 : opts.size();
        List<List<Integer>> rows = new ArrayList<>();
        List<Integer> currentRow = new ArrayList<>();
        int currentRowW = 0;
        for (int i = 0; i < opts.size(); i++) {
            int w = itemWidths[i];
            if (!currentRow.isEmpty() && (currentRowW + w > maxW || currentRow.size() >= maxItems)) {
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                currentRowW = 0;
            }
            currentRow.add(i);
            currentRowW += w;
        }
        if (!currentRow.isEmpty()) rows.add(currentRow);

        LinearLayout mainContainer = new LinearLayout(context);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setTag("popup_main");

        float r = density * 10f;
        int bgColor = theme != null ? theme.specialKey : 0xFF374151;
        int strokeColor = theme != null ? theme.keyBorder : 0;
        int strokeW = Math.round(0.9f * density * 1.5f);

        for (int rIdx = 0; rIdx < rows.size(); rIdx++) {
            List<Integer> itemIndices = rows.get(rIdx);
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setTag("popup_row_" + rIdx);

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(bgColor);
            if (theme != null) rowBg.setStroke(strokeW, strokeColor);

            float[] radii = new float[8];
            if (rIdx == 0) { radii[0] = radii[1] = radii[2] = radii[3] = r; }
            if (rIdx == rows.size() - 1) { radii[4] = radii[5] = radii[6] = radii[7] = r; }
            rowBg.setCornerRadii(radii);
            row.setBackground(rowBg);

            int rowW = 0;
            for (int i : itemIndices) {
                TextView tv = new TextView(context);
                tv.setText(opts.get(i));
                tv.setTextSize(textSp);
                tv.setTextColor(theme != null ? theme.keyText : Color.WHITE);
                tv.setGravity(Gravity.CENTER);
                tv.setSingleLine(true);
                tv.setLayoutParams(new LinearLayout.LayoutParams(itemWidths[i], itemH));
                tv.setTag(i);
                row.addView(tv);
                rowW += itemWidths[i];
            }
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(rowW, itemH);
            if (rIdx > 0) rowLp.topMargin = (int)(-1 * density);
            mainContainer.addView(row, rowLp);
        }
        return mainContainer;
    }
}
