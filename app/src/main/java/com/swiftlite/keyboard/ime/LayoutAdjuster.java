package com.swiftlite.keyboard.ime;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class LayoutAdjuster {
    public static void apply(KeyboardView kb, LayoutController lc) {
        LinearLayout root = (LinearLayout) kb.getChildAt(0);
        int mode = lc.getMode();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) root.getLayoutParams();
        int sw = kb.getResources().getDisplayMetrics().widthPixels;

        lp.leftMargin = 0; lp.topMargin = 0;
        if (mode == LayoutController.MODE_ONE_HANDED_L || mode == LayoutController.MODE_ONE_HANDED_R) {
            lp.width = (int) (sw * 0.85f);
            lp.gravity = (mode == LayoutController.MODE_ONE_HANDED_L) ? Gravity.START | Gravity.BOTTOM : Gravity.END | Gravity.BOTTOM;
            lp.bottomMargin = Math.round(10 * kb.getResources().getDisplayMetrics().density);
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.gravity = Gravity.BOTTOM;
            lp.bottomMargin = 0;
        }
        root.setLayoutParams(lp);
        kb.requestLayout();
        View overlay = kb.findViewWithTag("resize_overlay"); if (overlay != null) overlay.invalidate();
    }
}
