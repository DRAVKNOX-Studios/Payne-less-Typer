package com.swiftlite.keyboard.ime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.swiftlite.keyboard.theme.KeyboardTheme;

public class KeyPreviewManager {

    private static final int   PREVIEW_HOLD_MS = 300;
    private static final float PREVIEW_SIZE_DP = 42f;
    private static final float PREVIEW_RISE_DP = 10f;
    private static final float PREVIEW_GAP_DP  = 4f;

    private final Context mContext;
    private final View mParentView;
    private FrameLayout mPreviewPopup;
    private final float mDensity;
    private final Runnable mDismissPreview = this::dismissKeyPreview;

    public KeyPreviewManager(Context context, View parentView) {
        mContext = context;
        mParentView = parentView;
        mDensity = context.getResources().getDisplayMetrics().density;
    }

    public void showKeyPreview(Key key, String label, KeyboardTheme theme) {
        if (label == null || label.isEmpty()) return;
        View rootView = mParentView.getRootView();
        if (!(rootView instanceof ViewGroup)) return;
        ViewGroup root = (ViewGroup) rootView;

        mParentView.removeCallbacks(mDismissPreview);
        if (mPreviewPopup != null) {
            mPreviewPopup.animate().cancel();
            ViewGroup p = (ViewGroup) mPreviewPopup.getParent();
            if (p != null) p.removeView(mPreviewPopup);
            mPreviewPopup = null;
        }

        int sizePx    = (int) (PREVIEW_SIZE_DP * mDensity);
        int accent    = theme != null ? theme.accent : 0xFF3B82F6;
        float cornerR = mDensity * 12f;

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(accent);
        bg.setCornerRadius(cornerR);

        TextView tv = new TextView(mContext);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(20f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(bg);

        mPreviewPopup = new FrameLayout(mContext);
        mPreviewPopup.addView(tv, new FrameLayout.LayoutParams(sizePx, sizePx));

        int[] loc     = new int[2]; mParentView.getLocationInWindow(loc);
        int[] rootLoc = new int[2]; root.getLocationInWindow(rootLoc);
        int keyCentreX = loc[0] - rootLoc[0] + (int)(key.x + key.w / 2f);
        int keyTopY    = loc[1] - rootLoc[1] + (int) key.y;
        int gapPx      = (int)(PREVIEW_GAP_DP * mDensity);
        int left = Math.max(0, Math.min(keyCentreX - sizePx / 2, root.getWidth() - sizePx));
        int top  = Math.max(0, keyTopY - sizePx - gapPx);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        lp.leftMargin = left;
        lp.topMargin  = top;
        root.addView(mPreviewPopup, lp);

        mPreviewPopup.setScaleX(0.4f);
        mPreviewPopup.setScaleY(0.4f);
        mPreviewPopup.setAlpha(0f);
        mPreviewPopup.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(80)
                .setInterpolator(new OvershootInterpolator(2.0f))
                .start();

        mParentView.postDelayed(mDismissPreview, PREVIEW_HOLD_MS);
    }

    public void dismissKeyPreview() {
        mParentView.removeCallbacks(mDismissPreview);
        if (mPreviewPopup == null) return;
        final FrameLayout bubble = mPreviewPopup;
        mPreviewPopup = null;
        float risePx = PREVIEW_RISE_DP * mDensity;
        bubble.animate()
                .alpha(0f)
                .translationYBy(-risePx)
                .setDuration(140)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator a) {
                        ViewGroup p = (ViewGroup) bubble.getParent();
                        if (p != null) p.removeView(bubble);
                    }
                }).start();
    }
}
