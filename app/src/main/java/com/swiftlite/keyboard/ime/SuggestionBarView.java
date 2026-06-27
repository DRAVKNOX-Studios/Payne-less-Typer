package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swiftlite.keyboard.SetupActivity;
import com.swiftlite.keyboard.clipboard.ClipboardItem;
import com.swiftlite.keyboard.clipboard.ClipboardRepository;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.utils.SuggestionUtils;
import com.swiftlite.keyboard.utils.UIUtils;
import com.swiftlite.keyboard.utils.VibrationUtils;

import java.io.InputStream;

public class SuggestionBarView extends LinearLayout {

    private static final int CHIP_PAD_DP = 8;
    private final SwiftLiteIME mIME;
    private final KeyboardView mParent;
    private LinearLayout mSuggestionSpread;
    private IconButton mClipBtn, mEmojiBtn, mUndoBtn;
    private KeyboardTheme mTheme;
    private boolean mShowIdleItems = true;
    private String[] mPendingSuggestions = new String[0];
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public SuggestionBarView(Context context, SwiftLiteIME ime, KeyboardView parent) {
        super(context); mIME = ime; mParent = parent; init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setTag("suggestion_wrapper");

        LinearLayout bar = new LinearLayout(getContext());
        bar.setOrientation(HORIZONTAL);
        bar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, UIUtils.dp(getContext(), 40)));
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setTag("suggestion_bar");

        mClipBtn = iconBtn(KeyIcons.IC_CLIPBOARD, "clip_btn",
                v -> mParent.togglePanel(KeyboardView.PANEL_CLIPBOARD));
        bar.addView(mClipBtn);
        bar.addView(divider("div1"));

        mSuggestionSpread = new LinearLayout(getContext());
        mSuggestionSpread.setOrientation(HORIZONTAL);
        mSuggestionSpread.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
        mSuggestionSpread.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(mSuggestionSpread);

        bar.addView(divider("div2"));
        mUndoBtn = iconBtn(KeyIcons.IC_UNDO, "undo_btn",
                v -> mIME.onKeyPress(KeyboardView.KEY_UNDO, ""));
        bar.addView(mUndoBtn);
        bar.addView(divider("div_undo"));
        mEmojiBtn = iconBtn(KeyIcons.IC_EMOJI, "emoji_btn",
                v -> mParent.togglePanel(KeyboardView.PANEL_EMOJI));
        bar.addView(mEmojiBtn);

        addView(bar);
        updateToolIcons();
        View sep = new View(getContext());
        sep.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
        sep.setTag("suggestion_divider");
        addView(sep);
    }

    private IconButton iconBtn(int icon, String tag, View.OnClickListener l) {
        IconButton b = new IconButton(getContext(), icon, 0xFF888888);
        b.setLayoutParams(new LayoutParams(UIUtils.dp(getContext(), 40), LayoutParams.MATCH_PARENT));
        b.setOnClickListener(v -> {
            vibrate(VibrationUtils.VIBE_UTIL);
            l.onClick(v);
        });
        b.setTag(tag);
        return b;
    }

    private void vibrate(int duration) {
        if (mIME != null && mIME.getThemeManager() != null && mIME.getThemeManager().isVibrateEnabled()) {
            VibrationUtils.vibrate(getContext(), duration);
        }
    }

    private View divider(String tag) {
        View v = new View(getContext());
        LayoutParams lp = new LayoutParams(1, UIUtils.dp(getContext(), 20));
        lp.gravity = Gravity.CENTER_VERTICAL;
        v.setLayoutParams(lp);
        v.setAlpha(0.25f);
        v.setTag(tag);
        return v;
    }

    public void updateToolIcons() {
        int currentPanel = mParent.getCurrentPanelSafe();
        int basePanel    = mParent.getBasePanelSafe();
        int backIcon = (basePanel == KeyboardView.PANEL_NUMBERS) ? KeyIcons.IC_NUMBERS : KeyIcons.IC_ALPHA;

        if (mEmojiBtn != null)
            mEmojiBtn.setIcon(currentPanel == KeyboardView.PANEL_EMOJI ? backIcon : KeyIcons.IC_EMOJI);
        if (mClipBtn != null)
            mClipBtn.setIcon(currentPanel == KeyboardView.PANEL_CLIPBOARD ? backIcon : KeyIcons.IC_CLIPBOARD);
    }

    public boolean isShowingIdleItems() { return mShowIdleItems; }

    public void setTheme(KeyboardTheme theme) {
        mTheme = theme;
        View bar = findViewWithTag("suggestion_bar");
        if (bar != null) bar.setBackgroundColor(theme.suggestionBg);
        if (mClipBtn  != null) mClipBtn.setColor(theme.keyText);
        if (mUndoBtn  != null) mUndoBtn.setColor(theme.keyText);
        if (mEmojiBtn != null) mEmojiBtn.setColor(theme.keyText);
        View sep = findViewWithTag("suggestion_divider");
        if (sep != null) sep.setBackgroundColor(theme.isDark ? 0x22FFFFFF : 0x22000000);
        int dc = theme.isDark ? 0x44FFFFFF : 0x44000000;
        for (String t : new String[]{"div1", "div2", "div_undo"}) {
            View d = findViewWithTag(t); if (d != null) d.setBackgroundColor(dc);
        }
        if (mSuggestionSpread.getWidth() > 0) populateBar(mSuggestionSpread.getWidth());
    }

    public void setShowingIdleItems(boolean show) {
        mShowIdleItems = show;
        if (show) mPendingSuggestions = new String[0];
        schedulePopulate();
    }

    public void refreshIdleBar() {
        if (mShowIdleItems && mSuggestionSpread.getWidth() > 0)
            populateBar(mSuggestionSpread.getWidth());
    }

    public void updateSuggestions(String[] suggestions) {
        mPendingSuggestions = suggestions != null ? suggestions : new String[0];
        if (mPendingSuggestions.length > 0) mShowIdleItems = false;
        schedulePopulate();
    }

    private void schedulePopulate() {
        if (mSuggestionSpread.getWidth() > 0) {
            populateBar(mSuggestionSpread.getWidth());
        } else {
            mSuggestionSpread.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        mSuggestionSpread.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        populateBar(mSuggestionSpread.getWidth());
                    }
                });
        }
    }

    private void populateBar(int availablePx) {
        if (mShowIdleItems || mPendingSuggestions.length == 0) {
            mSuggestionSpread.removeAllViews();
            populateIdleBar(availablePx);
            return;
        }
        if (availablePx <= 0) return;

        float sd = getContext().getResources().getDisplayMetrics().scaledDensity;
        int cp = UIUtils.dp(getContext(), CHIP_PAD_DP);
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG); bp.setTextSize(12 * sd); bp.setTypeface(Typeface.DEFAULT_BOLD);
        Paint rp = new Paint(Paint.ANTI_ALIAS_FLAG); rp.setTextSize(12 * sd);

        String[] fitting = SuggestionUtils.filterToFit(mPendingSuggestions, availablePx, rp, bp, cp, 1);
        if (fitting.length == 0) return;

        int[] natural = new int[fitting.length]; int total = 0;
        for (int i = 0; i < fitting.length; i++) {
            natural[i] = (int) Math.ceil(((i == 0) ? bp : rp).measureText(fitting[i])) + cp * 2;
            total += natural[i];
        }
        int leftover = Math.max(0, availablePx - total - (fitting.length - 1));
        int[] widths = new int[fitting.length]; int alloc = 0;
        for (int i = 0; i < fitting.length; i++) {
            if (i < fitting.length - 1) { int e = total > 0 ? (int)((long)leftover * natural[i] / total) : 0; widths[i] = natural[i] + e; alloc += e; }
            else widths[i] = natural[i] + (leftover - alloc);
            if (fitting.length == 1 && widths[i] > availablePx) widths[i] = availablePx;
        }
        SuggestionChipBuilder.build(getContext(), fitting, widths, cp, 1, mTheme, mIME, mSuggestionSpread);
    }

    private void populateIdleBar(int availablePx) {
        int sw = UIUtils.dp(getContext(), 40);
        IconButton sb = new IconButton(getContext(), KeyIcons.IC_SETTINGS, 0xFF888888);
        if (mTheme != null) sb.setColor(mTheme.keyText);
        sb.setLayoutParams(new LayoutParams(sw, LayoutParams.MATCH_PARENT));
        sb.setOnClickListener(v -> {
            vibrate(VibrationUtils.VIBE_UTIL);
            Intent i = new Intent(getContext(), SetupActivity.class);
            i.putExtra("target_tab", 1); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        });
        mSuggestionSpread.addView(sb);
        if (availablePx - sw <= UIUtils.dp(getContext(), 50)) return;
        mSuggestionSpread.addView(divider("idle_div"));
        ClipboardRepository repo = mIME.getClipboardRepository();
        if (repo == null) return;
        mIME.getExecutor().execute(() -> {
            ClipboardItem latest = repo.getLatest();
            if (latest != null)
                mHandler.post(() -> { if (mShowIdleItems && mSuggestionSpread.getChildCount() <= 2) addClipboardChip(latest); });
        });
    }

    private void addClipboardChip(ClipboardItem item) {
        LinearLayout chip = new LinearLayout(getContext());
        chip.setOrientation(HORIZONTAL); chip.setGravity(Gravity.CENTER);
        chip.setPadding(UIUtils.dp(getContext(), 12), 0, UIUtils.dp(getContext(), 12), 0);
        chip.setOnClickListener(v -> {
            vibrate(VibrationUtils.VIBE_UTIL);
            if (item.isImage()) mIME.commitClipboardImage(item.imageUri);
            else mIME.commitClipboard(item.content);
            setShowingIdleItems(false);
        });
        if (item.isImage()) {
            ImageView iv = new ImageView(getContext());
            iv.setLayoutParams(new LayoutParams(UIUtils.dp(getContext(), 28), UIUtils.dp(getContext(), 28)));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(0, 0, UIUtils.dp(getContext(), 4), 0);
            GradientDrawable gd = new GradientDrawable(); gd.setCornerRadius(UIUtils.dp(getContext(), 4));
            iv.setClipToOutline(true); iv.setBackground(gd);
            mIME.getExecutor().execute(() -> {
                try (InputStream is = getContext().getContentResolver().openInputStream(Uri.parse(item.imageUri))) {
                    BitmapFactory.Options o = new BitmapFactory.Options(); o.inSampleSize = 2;
                    Bitmap bmp = BitmapFactory.decodeStream(is, null, o);
                    if (bmp != null) mHandler.post(() -> iv.setImageBitmap(bmp));
                } catch (Exception ignored) {}
            });
            chip.addView(iv);
            TextView tv = new TextView(getContext()); tv.setText("Image"); tv.setTextSize(12);
            if (mTheme != null) tv.setTextColor(mTheme.keyText); chip.addView(tv);
        } else if (item.content != null) {
            TextView tv = new TextView(getContext());
            String t = item.content.replace("\n", " ");
            tv.setText(t.length() > 30 ? t.substring(0, 27) + "..." : t);
            tv.setTextSize(12); tv.setSingleLine(true); tv.setEllipsize(TextUtils.TruncateAt.END);
            if (mTheme != null) tv.setTextColor(mTheme.keyText); chip.addView(tv);
        }
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        lp.weight = 1f; chip.setLayoutParams(lp);
        if (mSuggestionSpread != null) mSuggestionSpread.addView(chip);
    }

    public void updateEditorInfo(android.view.inputmethod.EditorInfo info) {
        boolean s = PrivacyHandler.isSensitiveField(info);
        setVisibility(mClipBtn, "clip_btn", "div1", s);
        setVisibility(mUndoBtn, "undo_btn", "div_undo", s);
        setVisibility(mEmojiBtn, "emoji_btn", "div2", s);
        if (s) updateSuggestions(new String[0]);
    }

    private void setVisibility(IconButton btn, String btnTag, String divTag, boolean hide) {
        int v = hide ? GONE : VISIBLE;
        if (btn != null) btn.setVisibility(v);
        View d = findViewWithTag(divTag); if (d != null) d.setVisibility(v);
    }

    @Override
    protected void onDetachedFromWindow() { super.onDetachedFromWindow(); }
}
