package com.swiftlite.keyboard.ime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.swiftlite.keyboard.emoji.EmojiPanel;
import com.swiftlite.keyboard.theme.KeyboardTheme;

public class PanelManager {

    private static final int ANIM_MS = 140;

    private final Context         mCtx;
    private final SwiftLiteIME    mIME;
    private final KeyboardView    mKbv;
    private final FrameLayout     mContainer;
    private int mPanelPx;
    private final SuggestionBarView mSuggestionBar;

    private KeysCanvas         mKeysCanvas;
    private NumbersCanvas      mNumbersCanvas;
    private EmojiPanel         mEmojiPanel;
    private ClipboardPanelView mClipboardPanel;
    private KeyboardTheme      mTheme;

    private int mCurrentPanel = KeyboardView.PANEL_KEYS;
    private int mBasePanel    = KeyboardView.PANEL_KEYS;

    PanelManager(Context ctx, SwiftLiteIME ime, KeyboardView kbv,
                 FrameLayout container, int unused, SuggestionBarView bar) {
        mCtx = ctx; mIME = ime; mKbv = kbv;
        mContainer = container; mSuggestionBar = bar;
        mPanelPx = calculatePanelPx();

        mKeysCanvas = new KeysCanvas(ctx, ime, kbv);
        mContainer.addView(mKeysCanvas, matchPanel());

        mNumbersCanvas = new NumbersCanvas(ctx, ime, kbv);
        mNumbersCanvas.setVisibility(View.GONE);
        mContainer.addView(mNumbersCanvas, matchPanel());
    }

    private FrameLayout.LayoutParams matchPanel() {
        return new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mPanelPx);
    }

    public void toggle(int panel)  { show(mCurrentPanel == panel ? mBasePanel : panel, true); }
    public void show(int panel)    { show(panel, true); }

    public void show(int panel, boolean animate) {
        if (panel == KeyboardView.PANEL_NUMBERS && mNumbersCanvas != null)
            mNumbersCanvas.rebuildKeys();
        if (panel == mCurrentPanel) {
            View v = get(panel); if (v != null) v.setVisibility(View.VISIBLE); return;
        }
        for (int p : new int[]{KeyboardView.PANEL_KEYS, KeyboardView.PANEL_NUMBERS,
                               KeyboardView.PANEL_EMOJI, KeyboardView.PANEL_CLIPBOARD}) {
            View v = get(p);
            if (v != null) {
                v.animate().cancel();
                if (p != mCurrentPanel && p != panel) { v.setVisibility(View.GONE); v.setAlpha(1f); v.setTranslationY(0); }
            }
        }
        boolean toNumbers   = panel == KeyboardView.PANEL_NUMBERS;
        boolean fromNumbers = mCurrentPanel == KeyboardView.PANEL_NUMBERS;
        if (mKeysCanvas != null && (toNumbers || fromNumbers))
            mKeysCanvas.setShowingNumbers(toNumbers);

        View out = get(mCurrentPanel);
        mCurrentPanel = panel;
        if (panel == KeyboardView.PANEL_KEYS || panel == KeyboardView.PANEL_NUMBERS)
            mBasePanel = panel;
        View in = get(panel);

        if (mContainer.getLayoutParams() != null) {
            mContainer.getLayoutParams().height = mPanelPx;
            mContainer.requestLayout();
        }

        transition(out, in, animate, dpToPx(6));
        if (panel == KeyboardView.PANEL_CLIPBOARD && mClipboardPanel != null)
            mClipboardPanel.refreshClipboard();
        if (mSuggestionBar != null) mSuggestionBar.updateToolIcons();
    }

    private void transition(View out, View in, boolean animate, int slide) {
        if (out != null) {
            if (animate) {
                out.animate().alpha(0f).translationY(slide).setDuration(ANIM_MS)
                        .setInterpolator(new DecelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override public void onAnimationEnd(Animator a)    { reset(out, View.GONE); }
                            @Override public void onAnimationCancel(Animator a) { reset(out, View.GONE); }
                        }).start();
            } else reset(out, View.GONE);
        }
        if (in != null) {
            if (animate) {
                in.setAlpha(0f); in.setTranslationY(slide); in.setVisibility(View.VISIBLE);
                in.animate().alpha(1f).translationY(0).setDuration(ANIM_MS)
                        .setInterpolator(new DecelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override public void onAnimationEnd(Animator a)    { in.setAlpha(1f); in.setTranslationY(0); }
                            @Override public void onAnimationCancel(Animator a) { }
                        }).start();
            } else { in.setVisibility(View.VISIBLE); in.setAlpha(1f); in.setTranslationY(0); }
        }
    }

    private static void reset(View v, int vis) { v.setVisibility(vis); v.setAlpha(1f); v.setTranslationY(0); }

    public View get(int panel) {
        switch (panel) {
            case KeyboardView.PANEL_KEYS:    return mKeysCanvas;
            case KeyboardView.PANEL_NUMBERS: return mNumbersCanvas;
            case KeyboardView.PANEL_EMOJI:
                if (mEmojiPanel == null) {
                    KeyboardTheme t = mTheme != null ? mTheme : mIME.getThemeManager().getCurrentTheme();
                    mEmojiPanel = new EmojiPanel(mCtx, mIME, mKbv, t);
                    mContainer.addView(mEmojiPanel, matchPanel());
                }
                return mEmojiPanel;
            case KeyboardView.PANEL_CLIPBOARD:
                if (mClipboardPanel == null) {
                    KeyboardTheme t = mTheme != null ? mTheme : mIME.getThemeManager().getCurrentTheme();
                    mClipboardPanel = new ClipboardPanelView(mCtx, mIME, mKbv, mPanelPx, t);
                    mContainer.addView(mClipboardPanel, matchPanel());
                }
                return mClipboardPanel;
            default: return null;
        }
    }

    public void applyTheme(KeyboardTheme t) {
        mTheme = t;
        mPanelPx = calculatePanelPx();
        if (mContainer.getLayoutParams() != null) {
            mContainer.getLayoutParams().height = mPanelPx;
            mContainer.requestLayout();
        }
        if (mKeysCanvas     != null) {
            mKeysCanvas.setTheme(t);
        }
        if (mNumbersCanvas  != null) mNumbersCanvas.setTheme(t);
        if (mEmojiPanel     != null) {
            mEmojiPanel.setTheme(t);
            mEmojiPanel.getLayoutParams().height = mPanelPx;
        }
        if (mClipboardPanel != null) {
            mClipboardPanel.setTheme(t);
            mClipboardPanel.updateHeight(mPanelPx);
        }
    }

    private int calculatePanelPx() {
        float density = mCtx.getResources().getDisplayMetrics().density;
        int kh = Math.round(BaseKeyCanvas.KEY_HEIGHT_DP * density);
        int pad = Math.round(BaseKeyCanvas.KEY_PAD_DP * density);
        int rows = mIME.getThemeManager().isNumberRowEnabled() ? 5 : 4;
        return kh * rows + pad * (rows * 2 + 2);
    }

    public void trimMemory() {
        if (mEmojiPanel     != null) { mContainer.removeView(mEmojiPanel);     mEmojiPanel = null; }
        if (mClipboardPanel != null) { mContainer.removeView(mClipboardPanel); mClipboardPanel = null; }
    }

    public int  current()  { return mCurrentPanel; }
    public int  base()     { return mBasePanel; }
    public KeysCanvas         keys()      { return mKeysCanvas; }
    public NumbersCanvas      numbers()   { return mNumbersCanvas; }
    public EmojiPanel         emoji()     { return mEmojiPanel; }
    public ClipboardPanelView clipboard() { return mClipboardPanel; }

    private int dpToPx(int dp) {
        return Math.round(dp * mCtx.getResources().getDisplayMetrics().density);
    }
}
