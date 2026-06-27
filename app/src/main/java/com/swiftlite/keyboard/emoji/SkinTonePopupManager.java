package com.swiftlite.keyboard.emoji;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swiftlite.keyboard.theme.KeyboardTheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SkinTonePopupManager {

    private final Context mContext;
    private final View mParentView;
    private FrameLayout mSkinPopup;
    private LinearLayout mSkinRow;
    private int mSkinHighlight = -1;
    private List<String> mSkinOpts;
    private int mSkinPopupX, mSkinPopupY, mSkinItemSz;
    private boolean mSkinPopupOpen = false;

    public SkinTonePopupManager(Context context, View parentView) {
        mContext = context;
        mParentView = parentView;
    }

    public void showSkinTonePopup(String base, View anchor, KeyboardTheme theme, String activeMod) {
        dismissSkinPopup();
        View rootView = mParentView.getRootView();
        if (!(rootView instanceof ViewGroup)) return;
        ViewGroup root = (ViewGroup) rootView;

        mSkinItemSz = dpToPx(44);
        mSkinOpts = new ArrayList<>();
        mSkinOpts.add(null);
        Collections.addAll(mSkinOpts, EmojiSkinToneHelper.SKIN_TONES);
        int count = mSkinOpts.size();
        int popW  = mSkinItemSz * count;

        int[] anchorLoc = new int[2]; anchor.getLocationInWindow(anchorLoc);
        int[] rootLoc   = new int[2]; root.getLocationInWindow(rootLoc);
        int anchorWinX  = anchorLoc[0] - rootLoc[0];
        int anchorWinY  = anchorLoc[1] - rootLoc[1];
        mSkinPopupX = Math.max(0, Math.min(anchorWinX, root.getWidth() - popW));
        mSkinPopupY = Math.max(0, anchorWinY - mSkinItemSz - dpToPx(4));

        int surfaceColor = theme != null ? theme.specialKey : 0xFF1E2433;
        int accentColor  = theme != null ? theme.accent     : 0xFF4F98A3;

        mSkinRow = new LinearLayout(mContext);
        mSkinRow.setOrientation(LinearLayout.HORIZONTAL);
        mSkinRow.setGravity(Gravity.CENTER_VERTICAL);
        mSkinRow.setTag("skin_row");
        GradientDrawable bgD = new GradientDrawable();
        bgD.setColor(surfaceColor);
        bgD.setCornerRadius(dpToPx(10));
        mSkinRow.setBackground(bgD);

        for (int i = 0; i < count; i++) {
            String modifier = mSkinOpts.get(i);
            String display  = EmojiSkinToneHelper.applyTone(base, modifier);
            TextView tv = new TextView(mContext);
            tv.setText(EmojiPanel.processEmoji(display));
            tv.setTextSize(20);
            tv.setTextColor(Color.WHITE);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(mSkinItemSz, mSkinItemSz));
            if (Objects.equals(modifier, activeMod)) tv.setBackground(makeHighlightBg(accentColor));
            mSkinRow.addView(tv);
        }

        mSkinPopup = new FrameLayout(mContext);
        mSkinPopup.setClickable(false);
        FrameLayout.LayoutParams rowLp = new FrameLayout.LayoutParams(popW, mSkinItemSz);
        rowLp.leftMargin = mSkinPopupX;
        rowLp.topMargin  = mSkinPopupY;
        mSkinPopup.addView(mSkinRow, rowLp);
        root.addView(mSkinPopup, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mSkinPopupOpen = true;
        mSkinHighlight = -1;
        mSkinPopup.setAlpha(0f);
        mSkinPopup.setScaleX(0.85f); mSkinPopup.setScaleY(0.85f);
        mSkinPopup.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120)
                .setInterpolator(new OvershootInterpolator(1.8f)).start();
    }

    public int skinIndexAt(float wx, float wy) {
        if (mSkinOpts == null || mSkinPopup == null) return -1;
        int[] rootLoc = new int[2]; mSkinPopup.getLocationInWindow(rootLoc);
        float lx = wx - rootLoc[0] - mSkinPopupX;
        float ly = wy - rootLoc[1] - mSkinPopupY;
        if (ly < -mSkinItemSz || ly > mSkinItemSz * 2) return -1;
        int idx = (int) (lx / mSkinItemSz);
        return (idx < 0 || idx >= mSkinOpts.size()) ? -1 : idx;
    }

    public void refreshSkinHighlight(KeyboardTheme theme, String activeMod) {
        if (mSkinRow == null || mSkinOpts == null) return;
        int accent = theme != null ? theme.accent : 0xFF4F98A3;
        boolean sliding = mSkinHighlight >= 0;
        for (int i = 0; i < mSkinOpts.size(); i++) {
            View child = mSkinRow.getChildAt(i);
            if (child == null) continue;
            if (sliding) {
                child.setBackground(i == mSkinHighlight ? makeHighlightBg(accent) : null);
            } else {
                String modifier = mSkinOpts.get(i);
                child.setBackground(Objects.equals(modifier, activeMod) ? makeHighlightBg(accent) : null);
            }
        }
    }

    private GradientDrawable makeHighlightBg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dpToPx(6)); return d;
    }

    public void dismissSkinPopup() {
        mSkinPopupOpen = false;
        if (mSkinPopup == null) return;
        final FrameLayout pop = mSkinPopup;
        mSkinPopup = null; mSkinRow = null; mSkinHighlight = -1; mSkinOpts = null;
        pop.animate().alpha(0f).setDuration(80)
                .setListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator a) {
                        ViewGroup p = (ViewGroup) pop.getParent();
                        if (p != null) p.removeView(pop);
                    }
                }).start();
    }

    public boolean isSkinPopupOpen() { return mSkinPopupOpen; }
    public List<String> getSkinOpts() { return mSkinOpts; }
    public void setSkinHighlight(int idx) { mSkinHighlight = idx; }
    public int getSkinHighlight() { return mSkinHighlight; }

    private int dpToPx(int dp) {
        return Math.round(dp * mContext.getResources().getDisplayMetrics().density);
    }
}
