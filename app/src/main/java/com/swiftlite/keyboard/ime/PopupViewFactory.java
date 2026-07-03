package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.graphics.Canvas;
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
        float r = density * 10f;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipToOutline(true); row.setClipChildren(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(theme != null ? theme.specialKey : 0xFF374151); bg.setCornerRadius(r);
        if (theme != null) bg.setStroke(Math.round(0.9f * density * 1.5f), theme.keyBorder);
        row.setBackground(bg); row.setTag("popup_row");

        for (int i = 0; i < opts.size(); i++) {
            String opt = opts.get(i); View v;
            if (opt.startsWith("icon:")) {
                String[] parts = opt.split(":");
                final int ic = Integer.parseInt(parts[1]);
                final boolean active = parts.length > 2 && "active".equals(parts[2]);
                v = new View(context) {
                    @Override protected void onDraw(Canvas c) {
                        int col = (getTag() != null && (int)getTag() == -2) ? Color.WHITE : (active ? (theme != null ? theme.accent : Color.CYAN) : (theme != null ? theme.keyText : Color.WHITE));
                        KeyIcons.draw(c, ic, getWidth()/2f, getHeight()/2f, 18, density, col);
                    }
                };
            } else {
                TextView tv = new TextView(context); tv.setText(opt); tv.setTextSize(16);
                tv.setTextColor(theme != null ? theme.keyText : Color.WHITE);
                tv.setGravity(Gravity.CENTER); v = tv;
            }
            v.setLayoutParams(new LinearLayout.LayoutParams(itemSz, itemSz)); v.setTag(i); row.addView(v);
        }
        return row;
    }

    public static View createScrollablePopup(Context context, List<String> opts, KeyboardTheme theme, 
                                           float density, int maxW, float itemHDp, float padHDp, float textSp) {
        int itemH = (int)(density * itemHDp), hPad = (int)(density * padHDp);
        float szPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSp, context.getResources().getDisplayMetrics());
        Paint p = new Paint(1); p.setTextSize(szPx);
        int[] w = new int[opts.size()];
        for (int i = 0; i < opts.size(); i++) w[i] = (int) Math.ceil(p.measureText(opts.get(i))) + hPad;

        int maxIt = (opts.size() > 5) ? (opts.size() + 1) / 2 : opts.size();
        List<List<Integer>> rows = new ArrayList<>(); List<Integer> cur = new ArrayList<>(); int curW = 0;
        for (int i = 0; i < opts.size(); i++) {
            if (!cur.isEmpty() && (curW + w[i] > maxW || cur.size() >= maxIt)) { rows.add(cur); cur = new ArrayList<>(); curW = 0; }
            cur.add(i); curW += w[i];
        }
        if (!cur.isEmpty()) rows.add(cur);

        LinearLayout main = new LinearLayout(context); main.setOrientation(LinearLayout.VERTICAL); main.setTag("popup_main");
        float r = density * 10f; int bgCol = theme != null ? theme.specialKey : 0xFF374151, sCol = theme != null ? theme.keyBorder : 0, sW = Math.round(0.9f * density * 1.5f);

        for (int rIdx = 0; rIdx < rows.size(); rIdx++) {
            List<Integer> ids = rows.get(rIdx); LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setTag("popup_row_" + rIdx);
            GradientDrawable rb = new GradientDrawable(); rb.setColor(bgCol); if (theme != null) rb.setStroke(sW, sCol);
            float[] rad = new float[8]; if (rIdx == 0) rad[0] = rad[1] = rad[2] = rad[3] = r; if (rIdx == rows.size() - 1) rad[4] = rad[5] = rad[6] = rad[7] = r;
            rb.setCornerRadii(rad); row.setBackground(rb);

            int rowW = 0;
            for (int i : ids) {
                TextView tv = new TextView(context); tv.setText(opts.get(i)); tv.setTextSize(textSp);
                tv.setTextColor(theme != null ? theme.keyText : Color.WHITE); tv.setGravity(Gravity.CENTER); tv.setSingleLine(true);
                tv.setLayoutParams(new LinearLayout.LayoutParams(w[i], itemH)); tv.setTag(i); row.addView(tv); rowW += w[i];
            }
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(rowW, itemH);
            if (rIdx > 0) rowLp.topMargin = (int)(-1 * density);
            main.addView(row, rowLp);
        }
        return main;
    }
}
