package com.swiftlite.keyboard.ime;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseKeyCanvas extends View {

    static final int   KEY_HEIGHT_DP = 46;
    static final int   KEY_PAD_DP    = 5;
    static final float CORNER_DP     = 10f;
    static final float ICON_DP       = 16f;
    static final float BORDER_W_DP   = 0.9f;
    static final float SHADOW_DY     = 2f;
    static final float SHADOW_R      = 3f;

    final SwiftLiteIME  mIME;
    final KeyboardView  mParent;
    int   mWidth, mKeyHeight, mPad;
    final float mDensity;
    KeyboardTheme mTheme;

    final List<Key> mKeys = new ArrayList<>();
    final Paint mKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG), mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG), mShadPaint = new Paint(Paint.ANTI_ALIAS_FLAG), mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG), mSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF mRect = new RectF();

    Key mPressedKey;
    boolean mRepeatActive, mLongPressFired;
    float mPressScale = 1f;
    Key mAnimKey;

    final KeyPreviewManager mPreviewManager;
    final KeyPopupManager mPopupManager;
    float mFontSizeMultiplier = 1.0f;

    private final Runnable mRepeatDelete = new Runnable() {
        @Override public void run() {
            if (mRepeatActive && mPressedKey != null && mPressedKey.code == KeyboardView.KEY_DELETE) {
                mIME.onKeyPress(KeyboardView.KEY_DELETE, null);
                removeCallbacks(this); postDelayed(this, 50);
            }
        }
    };

    private final Runnable mLongPressRunnable;

    BaseKeyCanvas(Context context, SwiftLiteIME ime, KeyboardView parent) {
        super(context);
        mIME = ime; mParent = parent; mDensity = context.getResources().getDisplayMetrics().density;
        mPreviewManager = new KeyPreviewManager(context, this); mPopupManager = new KeyPopupManager(context, this);
        mFontSizeMultiplier = mIME.getThemeManager().getFontSizeMultiplier();
        mLongPressRunnable = () -> {
            if (mPressedKey == null) return;
            mPreviewManager.dismissKeyPreview();
            if (hasLongPressOptions(mPressedKey)) onLongPress(mPressedKey);
        };
        mShadPaint.setStyle(Paint.Style.FILL); mBorderPaint.setStyle(Paint.Style.STROKE); mBorderPaint.setStrokeWidth(BORDER_W_DP * mDensity);
    }

    abstract void rebuildKeys();
    abstract boolean hasLongPressOptions(Key key);
    abstract void onLongPress(Key key);
    abstract void onNormalTap(Key key);

    boolean showKeyPreviewOnDown(Key key) { return false; }
    String previewLabelFor(Key key) { return key.label != null ? key.label : ""; }

    @Override
    protected void onVisibilityChanged(@androidx.annotation.NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE) {
            mPreviewManager.dismissKeyPreview();
            mPopupManager.dismissPopup();
            mRepeatActive = false;
            removeCallbacks(mRepeatDelete);
            removeCallbacks(mLongPressRunnable);
        }
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        mKeyHeight = Math.round(KEY_HEIGHT_DP * mDensity);
        mPad = Math.round(KEY_PAD_DP * mDensity);
        int rows = mIME.getThemeManager().isNumberRowEnabled() ? 5 : 4;
        setMeasuredDimension(w, mKeyHeight * rows + mPad * (rows * 2 + 2));
    }

    @Override protected void onSizeChanged(int w, int h, int oW, int oH) { mWidth = w; rebuildKeys(); }

    void clampActionHitRects() { for (Key k : mKeys) if (k.isAction) { k.hitX = k.x; k.hitW = k.w; } }

    void extendEdgeHitRects() {
        java.util.Map<Integer, Key> left = new java.util.HashMap<>(), right = new java.util.HashMap<>();
        for (Key k : mKeys) {
            if (k.isAction) continue;
            int row = Math.round(k.y / (mKeyHeight + mPad));
            if (!left.containsKey(row) || k.hitX < left.get(row).hitX) left.put(row, k);
            if (!right.containsKey(row) || k.hitX + k.hitW > right.get(row).hitX + right.get(row).hitW) right.put(row, k);
        }
        for (Key k : left.values()) { float ext = k.hitX; k.hitX = 0; k.hitW += ext; }
        for (Key k : right.values()) k.hitW = mWidth - k.hitX;
    }

    Key findKey(float x, float y) {
        float p = mPad / 2f;
        for (Key k : mKeys) if (x >= k.x - p && x < k.x + k.w + p && y >= k.y - p && y < k.y + k.h + p) return k;
        Key best = null; float bestD = Float.MAX_VALUE;
        for (Key k : mKeys) {
            float dx = x - (k.hitX + k.hitW / 2f), dy = y - (k.y + k.h / 2f);
            float d = dx * dx + dy * dy; if (d < bestD) { bestD = d; best = k; }
        }
        return best;
    }

    @Override protected void onDraw(@androidx.annotation.NonNull Canvas canvas) {
        if (mTheme == null) return;
        float cornerR = CORNER_DP * mDensity, bHalf = (BORDER_W_DP * mDensity) / 2f;
        
        mTextPaint.setTextSize(mDensity * 16 * mFontSizeMultiplier);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mSubPaint.setTextSize(mDensity * 9 * mFontSizeMultiplier);
        mSubPaint.setTextAlign(Paint.Align.RIGHT);

        for (Key k : mKeys) {
            boolean pressed = (k == mPressedKey);
            float scale = (pressed && mAnimKey == k) ? mPressScale : 1f;
            float cx = k.x + k.w / 2f, cy = k.y + k.h / 2f;
            
            drawKeyBackground(canvas, k, pressed, scale, cornerR, bHalf);
            
            int textColor = (k.isAccent || pressed) ? 0xFFFFFFFF : mTheme.keyText;
            if (k.icon >= 0) {
                drawIcon(canvas, k, cx, cy, textColor);
            } else {
                String label = buildLabel(k);
                if (label != null && !label.isEmpty()) {
                    mTextPaint.setColor(textColor);
                    canvas.drawText(label, cx, cy - (mTextPaint.descent() + mTextPaint.ascent()) / 2, mTextPaint);
                }
            }

            String sub = subLabelFor(k);
            if (sub != null && !sub.isEmpty()) {
                mSubPaint.setColor(mTheme.keyText); mSubPaint.setAlpha(90);
                canvas.drawText(sub, k.x + k.w - mDensity * 4, k.y + mDensity * 11, mSubPaint);
            }
        }
    }

    void drawKeyBackground(Canvas canvas, Key k, boolean pressed, float scale, float cornerR, float bHalf) {
        float cx = k.x + k.w / 2f, cy = k.y + k.h / 2f;
        float hw = k.w / 2f * scale, hh = k.h / 2f * scale;
        int bgColor = pressed ? mTheme.accent : k.isAccent ? mTheme.accent : k.isSpecial ? mTheme.specialKey : mTheme.keyBg;
        mShadPaint.setColor(bgColor);
        mRect.set(cx - hw, cy - hh, cx + hw, cy + hh);
        canvas.drawRoundRect(mRect, cornerR, cornerR, mShadPaint);
        mKeyPaint.setColor(bgColor);
        canvas.drawRoundRect(mRect, cornerR, cornerR, mKeyPaint);
        if (!pressed) {
            mRect.set(cx - hw + bHalf, cy - hh + bHalf, cx + hw - bHalf, cy + hh - bHalf);
            canvas.drawRoundRect(mRect, cornerR - bHalf, cornerR - bHalf, mBorderPaint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX(), ty = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                mPopupManager.dismissPopup(); mLongPressFired = false; mPressedKey = findKey(tx, ty); mAnimKey = mPressedKey;
                if (mPressedKey != null) {
                    animatePress(true);
                    if (showKeyPreviewOnDown(mPressedKey)) { String lbl = previewLabelFor(mPressedKey); if (lbl != null && !lbl.isEmpty()) mPreviewManager.showKeyPreview(mPressedKey, lbl, mTheme); }
                    if (mPressedKey.code == KeyboardView.KEY_DELETE) { mRepeatActive = true; postDelayed(mRepeatDelete, 500); }
                    if (hasLongPressOptions(mPressedKey)) postDelayed(mLongPressRunnable, 400);
                    KeyVibrator.vibrate(getContext(), mIME.getThemeManager(), mPressedKey);
                }
                invalidate(); return true;
            case MotionEvent.ACTION_MOVE:
                if (mLongPressFired && mPopupManager.isPopupOpen()) {
                    int[] loc = new int[2]; getLocationInWindow(loc);
                    int idx = mPopupManager.popupIndexAt(tx + loc[0], ty + loc[1]);
                    if (idx != mPopupManager.getPopupHighlight()) { mPopupManager.setPopupHighlight(idx); mPopupManager.refreshPopupHighlight(mTheme); }
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mRepeatActive = false; removeCallbacks(mRepeatDelete); removeCallbacks(mLongPressRunnable); animatePress(false);
                if (mLongPressFired) {
                    int h = mPopupManager.getPopupHighlight(); List<String> opts = mPopupManager.getPopupOpts();
                    if (h >= 0 && opts != null && h < opts.size()) { String c = opts.get(h); mIME.onKeyPress(0, c); if (mPressedKey != null) mPreviewManager.showKeyPreview(mPressedKey, c, mTheme); }
                    mPopupManager.dismissPopup();
                } else if (mPressedKey != null) onNormalTap(mPressedKey);
                mPressedKey = null; mLongPressFired = false; invalidate(); return true;
            case MotionEvent.ACTION_CANCEL:
                mRepeatActive = false; removeCallbacks(mRepeatDelete); removeCallbacks(mLongPressRunnable); animatePress(false); mLongPressFired = false;
                mPopupManager.dismissPopup(); mPreviewManager.dismissKeyPreview(); mPressedKey = null; invalidate(); return true;
        }
        return false;
    }

    void animatePress(boolean down) {
        if (down) { mPressScale = 1.12f; invalidate(); }
        else {
            ValueAnimator va = ValueAnimator.ofFloat(mPressScale, 1f);
            va.setDuration(180); va.setInterpolator(new OvershootInterpolator(2.5f));
            va.addUpdateListener(a -> { mPressScale = (float) a.getAnimatedValue(); invalidate(); });
            va.start();
        }
    }

    void vibrate() { KeyVibrator.vibrate(getContext(), mIME.getThemeManager(), mPressedKey); }

    String buildLabel(Key key) { return key.label; }
    String subLabelFor(Key key) { return key.subLabel; }

    void drawIcon(Canvas canvas, Key key, float cx, float cy, int color) {
        KeyIcons.draw(canvas, key.icon, cx, cy, 16f * mFontSizeMultiplier, mDensity, color);
    }

    public void setTheme(KeyboardTheme theme) {
        mTheme = theme; mFontSizeMultiplier = mIME.getThemeManager().getFontSizeMultiplier();
        mBorderPaint.setColor(theme.keyBorder);
        float sR = SHADOW_R * mDensity, sD = SHADOW_DY * mDensity;
        if (mTheme.isDark) mShadPaint.setShadowLayer(sR, 0, sD, 0x44FFFFFF); else mShadPaint.setShadowLayer(sR, 0, sD, 0x33000000);
        invalidate();
    }
}
