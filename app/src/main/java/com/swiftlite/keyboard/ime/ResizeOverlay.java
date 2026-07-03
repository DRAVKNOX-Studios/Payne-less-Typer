package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class ResizeOverlay extends View {
    private final KeyboardView kb;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float startY, startH;
    private final int[] loc = new int[2], kbLoc = new int[2];

    public ResizeOverlay(Context context, KeyboardView kb, boolean unused) {
        super(context); this.kb = kb; setTag("resize_overlay");
        p.setColor(0xAA4F98A3); p.setStrokeWidth(Math.round(4 * context.getResources().getDisplayMetrics().density));
        p.setStyle(Paint.Style.STROKE);
    }

    @Override protected void onDraw(Canvas canvas) {
        View root = kb.getChildAt(0);
        root.getLocationInWindow(loc); kb.getLocationInWindow(kbLoc);
        float x = loc[0] - kbLoc[0], y = loc[1] - kbLoc[1], w = root.getWidth(), h = root.getHeight();
        canvas.drawRect(x, y, x + w, y + h, p);
        float hand = 30 * getResources().getDisplayMetrics().density;
        p.setAlpha(255); canvas.drawLine(x + w/2 - hand, y, x + w/2 + hand, y, p);
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        float ry = ev.getRawY(); View root = kb.getChildAt(0);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            startY = ry; startH = root.getHeight();
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            LayoutController lc = kb.getIME().getLayoutController();
            float bh = AdaptiveHeight.getBaseHeight(getContext()) / lc.getHScale();
            lc.setHScale(Math.max(0.5f, (startH + (startY - ry)) / bh));
            kb.updateLayout(); kb.setTheme(kb.getIME().getThemeManager().getCurrentTheme());
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            ((FrameLayout) getParent()).removeView(this);
            kb.notifyToolIcons();
        }
        return true;
    }
}
