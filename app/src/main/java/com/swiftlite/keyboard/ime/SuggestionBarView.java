package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.swiftlite.keyboard.SetupActivity;
import com.swiftlite.keyboard.clipboard.ClipboardItem;
import com.swiftlite.keyboard.clipboard.ClipboardRepository;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.utils.SuggestionUtils;
import com.swiftlite.keyboard.utils.UIUtils;
import com.swiftlite.keyboard.utils.VibrationUtils;

public class SuggestionBarView extends LinearLayout {
    private final SwiftLiteIME mIME;
    private final KeyboardView mParent;
    private LinearLayout mSuggestionSpread;
    private IconButton mClipBtn, mEmojiBtn, mUndoBtn, mSettingsBtn, mResizeBtn;
    private KeyboardTheme mTheme;
    private boolean mShowIdleItems = true;
    private String[] mPendingSuggestions = new String[0];
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public SuggestionBarView(Context context, SwiftLiteIME ime, KeyboardView parent) {
        super(context); mIME = ime; mParent = parent; init();
    }

    private void init() {
        setOrientation(VERTICAL); setLayoutParams(new LayoutParams(-1, -2));
        LinearLayout bar = new LinearLayout(getContext());
        bar.setOrientation(HORIZONTAL); bar.setLayoutParams(new LayoutParams(-1, UIUtils.dp(getContext(), 40)));
        bar.setGravity(Gravity.CENTER_VERTICAL); bar.setTag("suggestion_bar");

        mClipBtn = iconBtn(KeyIcons.IC_CLIPBOARD, "clip", v -> mParent.togglePanel(KeyboardView.PANEL_CLIPBOARD));
        mSettingsBtn = iconBtn(KeyIcons.IC_SETTINGS, "settings", v -> {
            Intent i = new Intent(getContext(), SetupActivity.class); i.putExtra("target_tab", 1); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); getContext().startActivity(i);
        });
        bar.addView(mClipBtn); bar.addView(mSettingsBtn); bar.addView(divider("d1"));

        mSuggestionSpread = new LinearLayout(getContext());
        mSuggestionSpread.setOrientation(HORIZONTAL); mSuggestionSpread.setLayoutParams(new LayoutParams(0, -1, 1));
        mSuggestionSpread.setGravity(Gravity.CENTER_VERTICAL); bar.addView(mSuggestionSpread);

        bar.addView(divider("d2"));
        mUndoBtn = iconBtn(KeyIcons.IC_UNDO, "undo", v -> mIME.onKeyPress(KeyboardView.KEY_UNDO, ""));
        mResizeBtn = iconBtn(KeyIcons.IC_RESIZE, "resize", v -> {
            View o = mParent.findViewWithTag("resize_overlay");
            if (o != null) { ((android.view.ViewGroup)o.getParent()).removeView(o); }
            else { mParent.addView(new ResizeOverlay(getContext(), mParent, false), new FrameLayout.LayoutParams(-1, -1)); }
            refreshIdleBar();
        });
        mEmojiBtn = iconBtn(KeyIcons.IC_EMOJI, "emoji", v -> mParent.togglePanel(KeyboardView.PANEL_EMOJI));
        bar.addView(mUndoBtn); bar.addView(mResizeBtn); bar.addView(mEmojiBtn);

        addView(bar);
        View sep = new View(getContext()); sep.setLayoutParams(new LayoutParams(-1, 1));
        sep.setTag("suggestion_divider"); addView(sep);
    }

    private IconButton iconBtn(int ic, String tag, View.OnClickListener l) {
        IconButton b = new IconButton(getContext(), ic, 0xFF888888);
        b.setLayoutParams(new LayoutParams(UIUtils.dp(getContext(), 38), -1));
        b.setOnClickListener(v -> { if (mIME.getThemeManager().isVibrateEnabled()) VibrationUtils.vibrate(getContext(), VibrationUtils.VIBE_UTIL); l.onClick(v); });
        b.setTag(tag); return b;
    }

    private View divider(String t) {
        View v = new View(getContext()); LayoutParams lp = new LayoutParams(1, UIUtils.dp(getContext(), 20));
        lp.gravity = Gravity.CENTER_VERTICAL; v.setLayoutParams(lp); v.setAlpha(0.25f); v.setTag(t); return v;
    }

    public void setTheme(KeyboardTheme t) {
        mTheme = t; View bar = findViewWithTag("suggestion_bar"); if (bar != null) bar.setBackgroundColor(t.suggestionBg);
        for (IconButton b : new IconButton[]{mClipBtn, mUndoBtn, mEmojiBtn, mSettingsBtn, mResizeBtn}) if (b != null) b.setColor(t.keyText);
        View sep = findViewWithTag("suggestion_divider"); if (sep != null) sep.setBackgroundColor(t.isDark ? 0x22FFFFFF : 0x22000000);
        int dc = t.isDark ? 0x44FFFFFF : 0x44000000;
        for (String s : new String[]{"d1", "d2"}) { View d = findViewWithTag(s); if (d != null) d.setBackgroundColor(dc); }
        refreshIdleBar();
    }

    public void setShowingIdleItems(boolean s) { mShowIdleItems = s; if (s) mPendingSuggestions = new String[0]; schedulePopulate(); }
    public void refreshIdleBar() { mParent.updateLayout(); if (mShowIdleItems && mSuggestionSpread.getWidth() > 0) populateBar(mSuggestionSpread.getWidth()); }
    public void updateSuggestions(String[] s) { mPendingSuggestions = s != null ? s : new String[0]; if (mPendingSuggestions.length > 0) mShowIdleItems = false; schedulePopulate(); }

    private void schedulePopulate() {
        if (mSuggestionSpread.getWidth() > 0) populateBar(mSuggestionSpread.getWidth());
        else mSuggestionSpread.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() { mSuggestionSpread.getViewTreeObserver().removeOnGlobalLayoutListener(this); populateBar(mSuggestionSpread.getWidth()); }
        });
    }

    private void populateBar(int av) {
        mSuggestionSpread.removeAllViews();
        if (mShowIdleItems || mPendingSuggestions.length == 0) { populateIdleBar(); return; }
        float sz = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());
        int cp = UIUtils.dp(getContext(), 8);
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG); bp.setTextSize(sz); bp.setTypeface(Typeface.DEFAULT_BOLD);
        Paint rp = new Paint(Paint.ANTI_ALIAS_FLAG); rp.setTextSize(sz);
        String[] fit = SuggestionUtils.filterToFit(mPendingSuggestions, av, rp, bp, cp, 1);
        if (fit.length == 0) return;
        int[] w = new int[fit.length]; int tot = 0;
        for (int i=0; i<fit.length; i++) { w[i] = (int)Math.ceil((i==0?bp:rp).measureText(fit[i])) + cp*2; tot += w[i]; }
        int l = Math.max(0, av - tot); for (int i=0; i<fit.length; i++) w[i] += l / fit.length;
        SuggestionChipBuilder.build(getContext(), fit, w, cp, 1, mTheme, mIME, mSuggestionSpread);
    }

    private void populateIdleBar() {
        mSuggestionSpread.setGravity(Gravity.CENTER_VERTICAL);
        boolean res = mParent.findViewWithTag("resize_overlay") != null;
        if (mResizeBtn != null) mResizeBtn.setColor(res ? mTheme.accent : mTheme.keyText);

        mIME.getExecutor().execute(() -> {
            ClipboardItem it = mIME.getClipboardRepository().getLatest();
            mHandler.post(() -> {
                if (!mShowIdleItems) return;
                if (it != null && mSuggestionSpread.getChildCount() == 0) addClipboardChip(it);
            });
        });
    }

    private void addClipboardChip(ClipboardItem it) {
        View c = SuggestionChipFactory.createClipboardChip(getContext(), it, mTheme, mIME, mHandler, item -> {
            if (mIME.getThemeManager().isVibrateEnabled()) VibrationUtils.vibrate(getContext(), VibrationUtils.VIBE_UTIL);
            if (item.isImage()) mIME.commitClipboardImage(item.imageUri); else mIME.commitClipboard(item.content); setShowingIdleItems(false);
        });
        if (mSuggestionSpread != null) mSuggestionSpread.addView(c);
    }

    public void updateEditorInfo(android.view.inputmethod.EditorInfo info) {
        boolean s = PrivacyHandler.isSensitiveField(info);
        int v = s ? GONE : VISIBLE;
        if (mClipBtn != null) mClipBtn.setVisibility(v); if (mSettingsBtn != null) mSettingsBtn.setVisibility(v);
        if (mUndoBtn != null) mUndoBtn.setVisibility(v); if (mResizeBtn != null) mResizeBtn.setVisibility(v);
        if (mEmojiBtn != null) mEmojiBtn.setVisibility(v);
        View d1 = findViewWithTag("d1"), d2 = findViewWithTag("d2");
        if (d1 != null) d1.setVisibility(v); if (d2 != null) d2.setVisibility(v);
        if (s) updateSuggestions(new String[0]);
    }

    public void updateToolIcons() {
        int cp = mParent.getCurrentPanelSafe(); int bp = mParent.getBasePanelSafe();
        int bIcon = (bp == KeyboardView.PANEL_NUMBERS) ? KeyIcons.IC_NUMBERS : KeyIcons.IC_ALPHA;
        if (mEmojiBtn != null) mEmojiBtn.setIcon(cp == KeyboardView.PANEL_EMOJI ? bIcon : KeyIcons.IC_EMOJI);
        if (mClipBtn != null) mClipBtn.setIcon(cp == KeyboardView.PANEL_CLIPBOARD ? bIcon : KeyIcons.IC_CLIPBOARD);
    }
    public boolean isShowingIdleItems() { return mShowIdleItems; }
}
