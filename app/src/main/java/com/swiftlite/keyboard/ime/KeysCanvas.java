package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.graphics.Canvas;
import android.view.inputmethod.EditorInfo;

import com.swiftlite.keyboard.theme.KeyboardTheme;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class KeysCanvas extends BaseKeyCanvas {

    private boolean mShiftOn, mCapsLock;
    private boolean mShowingNumbers = false;
    private boolean mIsEmailField   = false;
    private boolean mIsSearchField  = false;
    private final KeyboardLayout mLayout;

    public KeysCanvas(Context context, SwiftLiteIME ime, KeyboardView parent) {
        super(context, ime, parent);
        mLayout = KeyboardLayout.getInstance(context);
    }

    @Override void rebuildKeys() {
        mKeys.clear(); if (mWidth == 0) return;

        boolean numRow = mIME.getThemeManager().isNumberRowEnabled();
        int y = mPad;
        if (numRow) {
            layoutEqualRow((mShiftOn || mCapsLock) ? mLayout.numRowShifted : mLayout.numRow, y, 10);
            y += mKeyHeight + mPad;
        }

        layoutEqualRow(mLayout.row1, y, 10);
        y += mKeyHeight + mPad;
        layoutRow2(y);
        y += mKeyHeight + mPad;
        layoutRow3(y);
        y += mKeyHeight + mPad;
        layoutRow4(y);
        
        clampActionHitRects(); extendEdgeHitRects();
        
        if (numRow) {
            // If number row is enabled, we might want to remove numeric sublabels from ROW1
            for (Key k : mKeys) {
                if (k.y >= (mPad + mKeyHeight + mPad) && k.y < (mPad + (mKeyHeight + mPad) * 2)) {
                    if (k.subLabel != null && !k.subLabel.isEmpty() && Character.isDigit(k.subLabel.charAt(0))) {
                        char ch = Character.toLowerCase((char) k.code);
                        String[] alts = mLayout.longPress.get(ch);
                        k.subLabel = (alts != null && alts.length > 0) ? alts[0] : "";
                    }
                }
            }
        }

        if (mIsEmailField) {
            for (Key k : mKeys) {
                if (k.code == (int) ',' || ",".equals(k.label)) {
                    k.code = (int) '@';
                    k.label = "@";
                    k.subLabel = "";
                    break;
                }
            }
        }

        if (mIME != null) {
            int enterIcon = KeyIcons.resolveEnterIcon(mIME.getCurrentInputEditorInfo());
            for (Key k : mKeys) {
                if (k.code == KeyboardView.KEY_ENTER) { k.label = ""; k.icon = enterIcon; break; }
            }
        }
    }

    @Override boolean hasLongPressOptions(Key k) {
        if (k.code <= 0) return false;
        if (mIsEmailField && k.code == (int) '@' && "@".equals(k.label)) return true;
        if (mIsSearchField && k.code == (int) '.' && ".".equals(k.label)) return true;
        char ch = Character.toLowerCase((char) k.code);
        return (k.subLabel != null && !k.subLabel.isEmpty()) || mLayout.longPress.containsKey(ch) || mLayout.subLongPress.containsKey(ch);
    }

    @Override void onLongPress(Key key) {
        if (mIsEmailField && key.code == (int) '@' && "@".equals(key.label)) {
            mPopupManager.showScrollablePopup(key, mLayout.emailDomains, mTheme);
            mLongPressFired = true; vibrate(); return;
        }
        if (mIsSearchField && key.code == (int) '.' && ".".equals(key.label)) {
            mPopupManager.showScrollablePopup(key, mLayout.searchDomains, mTheme);
            mLongPressFired = true; vibrate(); return;
        }
        char ch = (char) key.code; String sub = key.subLabel;
        String[] alts  = mLayout.longPress.get(Character.toLowerCase(ch));
        String[] sAlts = mLayout.subLongPress.get(Character.toLowerCase(ch));
        if (sAlts == null && sub != null && !sub.isEmpty()) sAlts = mLayout.subLongPress.get(sub.charAt(0));
        List<String> opts = new ArrayList<>();
        if (sub != null && !sub.isEmpty()) opts.add(sub);
        if (alts  != null) Collections.addAll(opts, alts);
        if (sAlts != null) for (String a : sAlts) { if (!opts.contains(a)) opts.add(a); }
        if (!opts.isEmpty()) { mPopupManager.showPopup(key, opts, mTheme); mLongPressFired = true; vibrate(); }
    }

    @Override void onNormalTap(Key key) { mIME.onKeyPress(key.code, key.label); }
    @Override boolean showKeyPreviewOnDown(Key key) { return !key.isAction && key.label != null && !key.label.isEmpty(); }
    @Override String previewLabelFor(Key key) { return (mShiftOn || mCapsLock) ? key.label.toUpperCase() : key.label; }

    @Override void drawIcon(Canvas canvas, Key key, float cx, float cy, int color) {
        int icId = key.icon;
        if (key.code == KeyboardView.KEY_SHIFT)
            icId = mCapsLock ? KeyIcons.IC_CAPS : (mShiftOn ? KeyIcons.IC_SHIFT_ON : KeyIcons.IC_SHIFT);
        else if (key.code == KeyboardView.KEY_NUMBERS)
            icId = mShowingNumbers ? KeyIcons.IC_ALPHA : KeyIcons.IC_NUMBERS;
        KeyIcons.draw(canvas, icId, cx, cy, ICON_DP * mFontSizeMultiplier, mDensity, color);
    }

    @Override String buildLabel(Key key) {
        if (key.code > 0 && (mShiftOn || mCapsLock)) return key.label.toUpperCase();
        return key.label;
    }

    private void layoutEqualRow(Object[][] defs, int yTop, int count) {
        float keyW = (mWidth - mPad * (count + 1)) / (float) count; float x = mPad;
        for (Object[] def : defs) { mKeys.add(makeKey(def, x, yTop, keyW, mKeyHeight)); x += keyW + mPad; }
    }

    private void layoutRow2(int yTop) {
        int count = mLayout.row2.length;
        float keyW = (mWidth - mPad * (count + 1)) / (float) count;
        float x = (mWidth - (keyW * count + mPad * (count - 1))) / 2f;
        for (Object[] def : mLayout.row2) { mKeys.add(makeKey(def, x, yTop, keyW, mKeyHeight)); x += keyW + mPad; }
    }

    private void layoutRow3(int yTop) {
        float unitW = (mWidth - mPad * 10) / 10f, specialW = unitW * 1.5f, x = mPad;
        Key shift = makeKey(mLayout.row3[0], x, yTop, specialW, mKeyHeight);
        shift.isSpecial = true; shift.icon = KeyIcons.IC_SHIFT; shift.isAction = true; mKeys.add(shift); x += specialW + mPad;
        for (int i = 1; i <= 7; i++) { mKeys.add(makeKey(mLayout.row3[i], x, yTop, unitW, mKeyHeight)); x += unitW + mPad; }
        Key del = makeKey(mLayout.row3[8], x, yTop, mWidth - x - mPad, mKeyHeight);
        del.isSpecial = true; del.icon = KeyIcons.IC_BACKSPACE; del.isAction = true; mKeys.add(del);
    }

    private void layoutRow4(int yTop) {
        float u      = (mWidth - mPad * 6) / 8f;
        float numsW  = u * 1.6f;
        float commaW = u * 0.7f;
        float dotW   = u * 0.7f;
        float enterW = u * 1.6f;
        float spaceW = mWidth - mPad * 6 - numsW - commaW - dotW - enterW;

        float x = mPad;
        Key nums = makeKey(mLayout.row4[0], x, yTop, numsW, mKeyHeight);
        nums.isSpecial = true; nums.icon = KeyIcons.IC_NUMBERS; nums.isAction = true; mKeys.add(nums); x += numsW + mPad;
        mKeys.add(makeKey(mLayout.row4[1], x, yTop, commaW, mKeyHeight)); x += commaW + mPad;
        Key space = makeKey(mLayout.row4[2], x, yTop, spaceW, mKeyHeight);
        space.isSpecial = true; space.icon = KeyIcons.IC_SPACE; space.isAction = true; mKeys.add(space); x += spaceW + mPad;
        mKeys.add(makeKey(mLayout.row4[3], x, yTop, dotW, mKeyHeight)); x += dotW + mPad;
        Key enter = makeKey(mLayout.row4[4], x, yTop, mWidth - x - mPad, mKeyHeight);
        enter.isSpecial = true; enter.isAccent = true; enter.icon = KeyIcons.IC_ENTER; enter.isAction = true; mKeys.add(enter);
    }

    private static boolean isActionCode(int code) {
        return code == KeyboardView.KEY_DELETE || code == KeyboardView.KEY_ENTER
            || code == KeyboardView.KEY_SHIFT  || code == KeyboardView.KEY_NUMBERS
            || code == KeyboardView.KEY_UNDO   || code == KeyboardView.KEY_SPACE;
    }

    private Key makeKey(Object[] def, float x, float y, float w, int h) {
        Key k = new Key();
        Object c = def[0];
        if (c instanceof Character) k.code = (Character) c;
        else if (c instanceof Integer) k.code = (Integer) c;
        else if (c instanceof String) k.code = ((String) c).charAt(0);
        k.label = (String) def[1]; k.subLabel = def.length > 2 ? (String) def[2] : "";
        k.x = x; k.y = y; k.w = w; k.h = h; k.hitX = x; k.hitW = w;
        k.isAction = isActionCode(k.code); return k;
    }

    @Override public void setTheme(KeyboardTheme theme) {
        super.setTheme(theme);
        rebuildKeys();
        requestLayout();
    }

    public void setShowingNumbers(boolean showing) { mShowingNumbers = showing; invalidate(); }
    public void updateShift(boolean shift, boolean caps) {
        boolean changed = (mShiftOn != shift) || (mCapsLock != caps);
        mShiftOn = shift; mCapsLock = caps;
        if (changed && mIME.getThemeManager().isNumberRowEnabled()) rebuildKeys();
        invalidate();
    }

    public void updateEditorInfo(EditorInfo info) {
        mIsEmailField = PrivacyHandler.isEmailField(info);
        mIsSearchField = PrivacyHandler.isSearchField(info);
        rebuildKeys();

        int enterIcon = KeyIcons.resolveEnterIcon(info);
        for (Key k : mKeys) {
            if (k.code != KeyboardView.KEY_ENTER) continue;
            k.label = "";
            k.icon  = enterIcon;
            break;
        }
        invalidate();
    }
}
