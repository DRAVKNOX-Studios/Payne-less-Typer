package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.swiftlite.keyboard.theme.KeyboardTheme;

public class KeyboardView extends FrameLayout {

    public static final int KEY_DELETE      = -5;
    public static final int KEY_ENTER       = -4;
    public static final int KEY_SPACE       = -3;
    public static final int KEY_SHIFT       = -2;
    public static final int KEY_EMOJI       = -10;
    public static final int KEY_CLIPBOARD   = -11;
    public static final int KEY_SWITCH_LANG = -12;
    public static final int KEY_NUMBERS     = -13;
    public static final int KEY_UNDO        = -14;

    public static final int PANEL_KEYS      = 0;
    public static final int PANEL_EMOJI     = 1;
    public static final int PANEL_CLIPBOARD = 2;
    public static final int PANEL_NUMBERS   = 3;

    private static final int GOOGLY_ODDS     = 20;

    private static final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final SwiftLiteIME   mIME;
    private SuggestionBarView    mSuggestionBar;
    private PanelManager         mPanels;
    private GooglyEyesView       mGooglyEyes;
    private KeyboardTheme        mTheme;
    private Paint                mSuggestionPaintNormal;
    private Paint                mSuggestionPaintBold;

    public KeyboardView(Context context, SwiftLiteIME ime) {
        super(context);
        mIME = ime;
        setLayoutParams(new ViewGroup.LayoutParams(MP, WC));
        setClipChildren(false);
        setClipToPadding(false);
        buildSuggestionPaints();
        buildLayout();
    }

    private void buildSuggestionPaints() {
        float sd = getContext().getResources().getDisplayMetrics().scaledDensity;
        mSuggestionPaintNormal = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSuggestionPaintNormal.setTextSize(12 * sd);
        mSuggestionPaintBold = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSuggestionPaintBold.setTextSize(12 * sd);
        mSuggestionPaintBold.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LayoutParams(MP, WC));
        root.setClipChildren(false);
        root.setClipToPadding(false);

        mSuggestionBar = new SuggestionBarView(getContext(), mIME, this);
        root.addView(mSuggestionBar);

        FrameLayout container = new FrameLayout(getContext());
        container.setLayoutParams(new LinearLayout.LayoutParams(MP, WC));
        root.addView(container);
        addView(root);

        mPanels = new PanelManager(getContext(), mIME, this, container, 0, mSuggestionBar);
    }

    public void maybeShowGooglyEyes() {
        dismissGooglyEyes();
        if (mIME.hasTypedThisSession()) return;
        if (mSuggestionBar != null && !mSuggestionBar.isShowingIdleItems()) return;
        if (java.util.concurrent.ThreadLocalRandom.current().nextInt(GOOGLY_ODDS) != 0) return;
        spawnGooglyEyes();
    }

    public void forceShowGooglyEyes() {
        dismissGooglyEyes();
        spawnGooglyEyes();
    }

    private void spawnGooglyEyes() {
        if (mSuggestionBar == null) return;
        mGooglyEyes = new GooglyEyesView(getContext());
        if (mTheme != null) mGooglyEyes.setTheme(mTheme);
        final GooglyEyesView eyes = mGooglyEyes;
        mSuggestionBar.post(() -> {
            if (eyes != mGooglyEyes) return;
            int barH = mSuggestionBar != null ? mSuggestionBar.getHeight() : 0;
            if (barH <= 0) return;
            eyes.setBarHeight(barH);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MP, barH);
            lp.topMargin = 0;
            addView(eyes, lp);
            eyes.startAnimation();
        });
    }

    public void dismissGooglyEyes() {
        if (mGooglyEyes != null) {
            mGooglyEyes.stopAnimation();
            removeView(mGooglyEyes);
            mGooglyEyes = null;
        }
    }

    public void togglePanel(int panel)                { mPanels.toggle(panel); }
    public void showPanel(int panel)                  { mPanels.show(panel); }
    public void showPanel(int panel, boolean animate) { mPanels.show(panel, animate); }
    public void closePanels()                         { mPanels.show(mPanels.base()); }

    public int getCurrentPanelSafe()  { return mPanels != null ? mPanels.current() : PANEL_KEYS; }
    public int getBasePanelSafe()     { return mPanels != null ? mPanels.base()    : PANEL_KEYS; }
    public int getCurrentPanel()      { return mPanels.current(); }
    public int getBasePanel()         { return mPanels.base(); }

    public void trimMemory() { mPanels.trimMemory(); }

    public void notifyClipboardChanged() {
        if (mPanels != null && mPanels.current() == PANEL_CLIPBOARD && mPanels.clipboard() != null)
            mPanels.clipboard().refreshClipboard();
        if (mSuggestionBar != null && !mIME.hasTypedThisSession())
            mSuggestionBar.refreshIdleBar();
    }

    public void notifyEmojiUsed() {
        if (mPanels != null) {
            if (mPanels.emoji() != null) mPanels.emoji().refreshRecents();
            if (mPanels.numbers() != null) mPanels.numbers().refreshRecents();
        }
    }

    public void setTheme(KeyboardTheme theme) {
        mTheme = theme;
        setBackgroundColor(theme.keyboardBg);
        if (mSuggestionBar != null) mSuggestionBar.setTheme(theme);
        if (mPanels        != null) mPanels.applyTheme(theme);
        if (mGooglyEyes    != null) mGooglyEyes.setTheme(theme);
    }

    public int   getSuggestionWidth()             { return mSuggestionBar != null ? mSuggestionBar.getWidth() : 0; }
    public int   getSuggestionPadding()           { return dpToPx(8); }
    public Paint getSuggestionPaint(boolean bold) { return bold ? mSuggestionPaintBold : mSuggestionPaintNormal; }
    public void  updateSuggestions(String[] s)    { if (mSuggestionBar != null) mSuggestionBar.updateSuggestions(s); }
    public void  setShowingIdleItems(boolean b)   { if (mSuggestionBar != null) mSuggestionBar.setShowingIdleItems(b); }

    public void updateShiftState(boolean shift, boolean caps) {
        if (mPanels != null && mPanels.keys() != null) mPanels.keys().updateShift(shift, caps);
    }

    public void updateEditorInfo(EditorInfo info) {
        if (mPanels != null) {
            if (mPanels.keys()    != null) mPanels.keys().updateEditorInfo(info);
            if (mPanels.numbers() != null) mPanels.numbers().updateEditorInfo(info);
        }
        if (mSuggestionBar != null) mSuggestionBar.updateEditorInfo(info);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }
}
