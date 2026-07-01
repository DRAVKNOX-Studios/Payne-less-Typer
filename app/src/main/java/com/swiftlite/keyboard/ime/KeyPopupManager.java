package com.swiftlite.keyboard.ime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swiftlite.keyboard.theme.KeyboardTheme;

import java.util.ArrayList;
import java.util.List;

public class KeyPopupManager {

    private static final float ITEM_H_DP        = 44f;
    private static final float ITEM_PAD_H_DP    = 14f;
    private static final float CORNER_DP        = 10f;
    private static final float TEXT_SP_START    = 13f;
    private static final float MAX_WIDTH_FRAC   = 0.96f;

    private final Context mContext;
    private final View mParentView;
    private final float mDensity;
    private FrameLayout mPopup;
    private List<String> mPopupOpts;
    private int mPopupHighlight = -1;
    private int mPopupX, mPopupY, mPopupItemSz;

    public KeyPopupManager(Context context, View parentView) {
        mContext = context;
        mParentView = parentView;
        mDensity = context.getResources().getDisplayMetrics().density;
    }

    @SuppressWarnings("deprecation")
    private int windowWidth() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return wm.getCurrentWindowMetrics().getBounds().width();
            } else {
                android.graphics.Point sz = new android.graphics.Point();
                wm.getDefaultDisplay().getSize(sz);
                if (sz.x > 0) return sz.x;
            }
        }
        View root = mParentView.getRootView();
        return root.getWidth() > 0 ? root.getWidth() : mParentView.getWidth();
    }

    public void showPopup(Key key, List<String> opts, KeyboardTheme theme) {
        dismissPopup();
        ViewGroup root = getRoot();
        if (root == null || opts == null || opts.isEmpty()) return;
        mPopupOpts = new ArrayList<>(opts);
        mPopupItemSz = (int)(mDensity * 44);
        
        View row = PopupViewFactory.createStandardPopup(mContext, opts, theme, mPopupItemSz, mDensity);
        int popW = Math.min(mPopupOpts.size() * mPopupItemSz, root.getWidth());

        int[] loc = new int[2]; mParentView.getLocationInWindow(loc);
        int[] rootLoc = new int[2]; root.getLocationInWindow(rootLoc);
        mPopupX = Math.max(0, Math.min((int) key.x + loc[0] - rootLoc[0], root.getWidth() - popW));
        mPopupY = Math.max(0, loc[1] + (int) key.y - rootLoc[1] - mPopupItemSz - (int)(mDensity * 6));

        addPopupToRoot(root, row, popW, mPopupItemSz);
    }

    public void showScrollablePopup(Key key, List<String> opts, KeyboardTheme theme) {
        dismissPopup();
        ViewGroup root = getRoot();
        if (root == null || opts == null || opts.isEmpty()) return;
        mPopupOpts = new ArrayList<>(opts);

        int winW = windowWidth();
        int maxW = (int)(winW * MAX_WIDTH_FRAC);
        View mainContainer = PopupViewFactory.createScrollablePopup(mContext, opts, theme, mDensity, maxW, ITEM_H_DP, ITEM_PAD_H_DP, TEXT_SP_START);
        
        mainContainer.measure(View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int totalW = mainContainer.getMeasuredWidth();
        int totalH = mainContainer.getMeasuredHeight();

        int[] loc = new int[2]; mParentView.getLocationInWindow(loc);
        int[] rootLoc = new int[2]; root.getLocationInWindow(rootLoc);

        int keyScreenX = (int) key.x + loc[0] - rootLoc[0];
        int centreX    = keyScreenX + (int)(key.w / 2f) - totalW / 2;
        mPopupX  = Math.max(0, Math.min(centreX, winW - totalW));
        mPopupY  = Math.max(0, loc[1] + (int) key.y - rootLoc[1] - totalH - (int)(mDensity * 6));
        mPopupItemSz = (int)(mDensity * ITEM_H_DP);

        addPopupToRoot(root, mainContainer, totalW, totalH);
    }

    private ViewGroup getRoot() {
        View rootView = mParentView.getRootView();
        return (rootView instanceof ViewGroup) ? (ViewGroup) rootView : null;
    }

    private void addPopupToRoot(ViewGroup root, View content, int w, int h) {
        mPopup = new FrameLayout(mContext);
        mPopup.setClickable(true);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.leftMargin = mPopupX; lp.topMargin = mPopupY;
        mPopup.addView(content, lp);
        root.addView(mPopup, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        animateIn();
        mPopupHighlight = -1;
    }

    private void animateIn() {
        mPopup.setScaleX(0.85f); mPopup.setScaleY(0.85f); mPopup.setAlpha(0f);
        mPopup.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120)
                .setInterpolator(new OvershootInterpolator(1.8f)).start();
    }

    public void refreshPopupHighlight(KeyboardTheme theme) {
        if (mPopup == null) return;
        LinearLayout main = mPopup.findViewWithTag("popup_main");
        if (main == null) main = mPopup.findViewWithTag("popup_row");
        if (main == null) return;

        float r = mDensity * CORNER_DP;
        float inset = mDensity * 2f;
        int accent = theme != null ? theme.accent : 0xFF3B82F6;

        for (int rIdx = 0; rIdx < main.getChildCount(); rIdx++) {
            View v = main.getChildAt(rIdx);
            if (v instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) v;
                for (int i = 0; i < row.getChildCount(); i++) updateHighlight(row.getChildAt(i), theme, accent, r, inset, main.getChildCount(), rIdx, i, row.getChildCount());
            } else if (v instanceof TextView) {
                updateHighlight(v, theme, accent, r, inset, 1, 0, rIdx, main.getChildCount());
            }
        }
    }

    private void updateHighlight(View child, KeyboardTheme theme, int accent, float r, float inset, int rowCount, int rIdx, int i, int rowLen) {
        if (!(child instanceof TextView)) return;
        int itemIdx = (Integer) child.getTag();
        if (itemIdx == mPopupHighlight) {
            boolean isFirst = (i == 0), isLast = (i == rowLen - 1);
            float tl = (rIdx == 0 && isFirst) ? r : 0f, tr = (rIdx == 0 && isLast) ? r : 0f;
            float bl = (rIdx == rowCount - 1 && isFirst) ? r : 0f, br = (rIdx == rowCount - 1 && isLast) ? r : 0f;
            GradientDrawable hl = new GradientDrawable(); hl.setColor(accent); hl.setCornerRadii(new float[]{tl,tl,tr,tr,br,br,bl,bl});
            child.setBackground(new InsetDrawable(hl, (int) inset));
            ((TextView) child).setTextColor(Color.WHITE);
        } else {
            child.setBackground(null);
            ((TextView) child).setTextColor(theme != null ? theme.keyText : Color.WHITE);
        }
    }

    public int popupIndexAt(float wx, float wy) {
        if (mPopupOpts == null || mPopup == null) return -1;
        LinearLayout main = mPopup.findViewWithTag("popup_main");
        if (main == null) main = mPopup.findViewWithTag("popup_row");
        if (main == null) return -1;

        float localY = wy - mPopupY, localX = wx - mPopupX;
        if (localY < 0) return -1;
        
        for (int rIdx = 0; rIdx < main.getChildCount(); rIdx++) {
            View v = main.getChildAt(rIdx);
            if (v instanceof LinearLayout) {
                if (localY >= v.getTop() && localY <= v.getBottom()) {
                    LinearLayout row = (LinearLayout) v;
                    for (int i = 0; i < row.getChildCount(); i++) {
                        View c = row.getChildAt(i);
                        if (localX >= c.getLeft() && localX <= c.getRight()) return (Integer) c.getTag();
                    }
                }
            } else if (v instanceof TextView) {
                if (localX >= v.getLeft() && localX <= v.getRight()) return (Integer) v.getTag();
            }
        }
        return -1;
    }

    public void dismissPopup() {
        if (mPopup != null) {
            final View p = mPopup; mPopup = null;
            p.animate().alpha(0f).setDuration(80).setListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    if (p.getParent() instanceof ViewGroup) ((ViewGroup) p.getParent()).removeView(p);
                }
            }).start();
        }
        mPopupOpts = null; mPopupHighlight = -1;
    }

    public boolean isPopupOpen()          { return mPopup != null; }
    public List<String> getPopupOpts()    { return mPopupOpts; }
    public void setPopupHighlight(int i)  { mPopupHighlight = i; }
    public int getPopupHighlight()        { return mPopupHighlight; }
}
