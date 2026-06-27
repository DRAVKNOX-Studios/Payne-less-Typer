package com.swiftlite.keyboard.emoji;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.swiftlite.keyboard.ime.KeyboardView;
import com.swiftlite.keyboard.ime.KeyIcons;
import com.swiftlite.keyboard.ime.SwiftLiteIME;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.utils.VibrationUtils;

import java.util.List;

public class EmojiPanel extends LinearLayout {

    private static final int[] TAB_ICON_IDS = {
        KeyIcons.IC_CLOCK, KeyIcons.IC_EMOJI, KeyIcons.IC_SHIFT, KeyIcons.IC_CLIPBOARD,
        KeyIcons.IC_PIN, KeyIcons.IC_ENTER, KeyIcons.IC_UNDO, KeyIcons.IC_NUMBERS,
        KeyIcons.IC_ALPHA, KeyIcons.IC_FLAG,
    };

    private final SwiftLiteIME mIME;
    private final KeyboardView mKeyboardView;
    private GridView           mGrid;
    private EmojiAdapter       mAdapter;
    private final View[]       mTabs = new View[TAB_ICON_IDS.length];
    private int                mActiveTab = 0;
    private KeyboardTheme      mTheme;
    private volatile String[][] mFiltered;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private final SkinTonePopupManager mSkinPopupManager;

    private boolean mLongFired = false;

    public EmojiPanel(Context context, SwiftLiteIME ime, KeyboardView keyboardView, KeyboardTheme theme) {
        super(context);
        mIME = ime;
        mKeyboardView = keyboardView;
        mTheme = theme;
        mSkinPopupManager = new SkinTonePopupManager(context, this);
        setOrientation(VERTICAL);
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(230)));

        EmojiData.init(context.getAssets());
        EmojiSkinToneHelper.init(context);
        String[] initial = mIME.getRecentEmojis();
        mFiltered = new String[EmojiData.ALL.length][];
        System.arraycopy(EmojiData.ALL, 0, mFiltered, 0, mFiltered.length);

        buildUI(initial);
    }

    private void buildUI(String[] initialEmojis) {
        int bg      = mTheme != null ? mTheme.keyboardBg : 0xFF111827;
        int surface = mTheme != null ? mTheme.specialKey : 0xFF1E2433;
        int text    = mTheme != null ? mTheme.keyText    : 0xFFF9FAFB;
        setBackgroundColor(bg);

        LinearLayout topBar = new LinearLayout(getContext());
        topBar.setOrientation(HORIZONTAL);
        topBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(44)));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(surface);

        LinearLayout tabRow = new LinearLayout(getContext());
        tabRow.setOrientation(HORIZONTAL);
        tabRow.setGravity(Gravity.CENTER_VERTICAL);
        tabRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        int tabCount = Math.min(TAB_ICON_IDS.length, EmojiData.ALL.length + 1);
        for (int i = 0; i < tabCount; i++) {
            final int idx = i;
            View tab = makeIconTab(getContext(), TAB_ICON_IDS[idx], text);
            tab.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            tab.setOnClickListener(v -> selectTab(idx));
            mTabs[idx] = tab;
            tabRow.addView(tab);
        }

        topBar.addView(tabRow);
        addView(topBar);

        mGrid = new GridView(getContext()) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (!mSkinPopupManager.isSkinPopupOpen()) return super.dispatchTouchEvent(ev);
                int action = ev.getActionMasked();
                if (action == MotionEvent.ACTION_MOVE) {
                    int[] gl = new int[2]; getLocationInWindow(gl);
                    int idx = mSkinPopupManager.skinIndexAt(ev.getX() + gl[0], ev.getY() + gl[1]);
                    if (idx != mSkinPopupManager.getSkinHighlight()) {
                        mSkinPopupManager.setSkinHighlight(idx);
                        mSkinPopupManager.refreshSkinHighlight(mTheme, mAdapter.mSkinModifier);
                    }
                    return true;
                } else if (action == MotionEvent.ACTION_UP) {
                    int[] gl = new int[2]; getLocationInWindow(gl);
                    int idx = mSkinPopupManager.skinIndexAt(ev.getX() + gl[0], ev.getY() + gl[1]);
                    int commit = (mSkinPopupManager.getSkinHighlight() >= 0) ? mSkinPopupManager.getSkinHighlight() : idx;
                    List<String> opts = mSkinPopupManager.getSkinOpts();
                    if (commit >= 0 && opts != null && commit < opts.size()) {
                        String tone = opts.get(commit);
                        mAdapter.setSkinModifier(tone);
                        mIME.saveSelectedEmojiSkin(tone);
                    }
                    mLongFired = false; mSkinPopupManager.dismissSkinPopup();
                    return true;
                } else if (action == MotionEvent.ACTION_CANCEL) {
                    mLongFired = false; mSkinPopupManager.dismissSkinPopup();
                    return true;
                }
                return true;
            }
        };

        LayoutParams gp = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        gp.weight = 1;
        mGrid.setLayoutParams(gp);
        mGrid.setNumColumns(8);
        mGrid.setVerticalSpacing(dpToPx(2));
        mGrid.setHorizontalSpacing(dpToPx(2));
        mGrid.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        mGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        mGrid.setBackgroundColor(bg);

        mAdapter = new EmojiAdapter(getContext(), initialEmojis, this, mTheme);
        mAdapter.setSkinModifier(mIME.getSelectedEmojiSkin());
        mGrid.setAdapter(mAdapter);
        addView(mGrid);
        updateTabHighlight();
    }

    void onCellTouch(MotionEvent ev, String base, String toCommit, View anchor) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            vibrate(VibrationUtils.VIBE_NORMAL);
            mLongFired = false;
            if (EmojiSkinToneHelper.isToneSupportedEmoji(base)) {
                mMain.postDelayed(() -> {
                    if (!mLongFired) {
                        mLongFired = true;
                        mSkinPopupManager.showSkinTonePopup(base, anchor, mTheme, mAdapter.mSkinModifier);
                        mGrid.requestDisallowInterceptTouchEvent(true);
                    }
                }, 400);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            mMain.removeCallbacksAndMessages(null);
            if (mSkinPopupManager.isSkinPopupOpen()) return;
            if (!mLongFired) mIME.commitEmoji(toCommit);
            mLongFired = false;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            mMain.removeCallbacksAndMessages(null); mLongFired = false;
        }
    }

    private View makeIconTab(Context ctx, int iconId, int color) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return new View(ctx) {
            { setWillNotDraw(false); }
            @Override protected void onDraw(@androidx.annotation.NonNull Canvas canvas) {
                super.onDraw(canvas);
                Object tag = getTag();
                int drawColor = (tag instanceof Integer) ? (Integer) tag : color;
                KeyIcons.draw(canvas, iconId, getWidth() / 2f, getHeight() / 2f, 16, density, drawColor);
            }
        };
    }

    private void selectTab(int idx) {
        vibrate(VibrationUtils.VIBE_UTIL);
        mActiveTab = idx;
        if (idx == 0) {
            mAdapter.setEmojis(mIME.getRecentEmojis());
        } else {
            int dataIdx = idx - 1;
            if (mFiltered[dataIdx] == EmojiData.ALL[dataIdx]) {
                Paint p = new Paint();
                p.setTextSize(dpToPx(20));
                mFiltered[dataIdx] = EmojiData.filter(EmojiData.ALL[dataIdx], p);
            }
            mAdapter.setEmojis(mFiltered[dataIdx]);
        }
        mGrid.smoothScrollToPosition(0);
        updateTabHighlight();
    }

    private void updateTabHighlight() {
        int bg = mTheme != null ? mTheme.keyboardBg : 0xFF111827;
        int surface = mTheme != null ? mTheme.specialKey : 0xFF1E2433;
        int accent = mTheme != null ? mTheme.accent : 0xFF4F98A3;
        int text = mTheme != null ? mTheme.keyText : 0xFFF9FAFB;
        int muted = (text & 0x00FFFFFF) | 0x88000000;
        for (int i = 0; i < mTabs.length; i++) {
            if (mTabs[i] == null) continue;
            mTabs[i].setBackgroundColor(i == mActiveTab ? bg : surface);
            mTabs[i].setTag(i == mActiveTab ? accent : muted);
            mTabs[i].invalidate();
        }
    }

    private void vibrate(int duration) {
        if (mIME != null && mIME.getThemeManager().isVibrateEnabled()) VibrationUtils.vibrate(getContext(), duration);
    }

    public void refreshRecents() { if (mActiveTab == 0) mAdapter.setEmojis(mIME.getRecentEmojis()); }

    public void setTheme(KeyboardTheme theme) {
        mTheme = theme;
        removeAllViews();
        String[] cur = mAdapter != null ? mAdapter.mEmojis : mIME.getRecentEmojis();
        buildUI(cur);
    }

    public static CharSequence processEmoji(String raw) {
        try {
            androidx.emoji2.text.EmojiCompat ec = androidx.emoji2.text.EmojiCompat.get();
            if (ec.getLoadState() == androidx.emoji2.text.EmojiCompat.LOAD_STATE_SUCCEEDED) return ec.process(raw);
        } catch (IllegalStateException ignored) {}
        return raw;
    }

    private int dpToPx(int dp) { return Math.round(dp * getContext().getResources().getDisplayMetrics().density); }
}
