package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.view.inputmethod.EditorInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NumbersCanvas extends BaseKeyCanvas {

    private int mPage = 0;
    static final int KEY_PAGE = -20;
    private static final String SEP = "\u001F";

    private static String[][] P1_NUM, P1_SYM1, P1_SYM2;
    private static String[][] P2_MISC, P2_CURR, P2_MATH;
    private static boolean sLoaded = false;

    public static void init(Context ctx) {
        if (sLoaded) return;
        try {
            JSONObject obj = new JSONObject(readAsset(ctx, "numbers_layout.json"));
            P1_NUM  = parseRow(obj.getJSONArray("p1_num"));
            P1_SYM1 = parseRow(obj.getJSONArray("p1_sym1"));
            P1_SYM2 = parseRow(obj.getJSONArray("p1_sym2"));
            P2_MISC = parseRow(obj.getJSONArray("p2_misc"));
            P2_CURR = parseRow(obj.getJSONArray("p2_curr"));
            P2_MATH = parseRow(obj.getJSONArray("p2_math"));
            sLoaded = true;
        } catch (Exception e) { throw new RuntimeException("Failed to load numbers_layout.json", e); }
    }

    private static String[][] parseRow(JSONArray arr) throws Exception {
        String[][] row = new String[arr.length()][];
        for (int i = 0; i < arr.length(); i++) {
            JSONArray k = arr.getJSONArray(i); row[i] = new String[k.length()];
            for (int j = 0; j < k.length(); j++) row[i][j] = k.getString(j);
        }
        return row;
    }

    private static String readAsset(Context ctx, String path) throws Exception {
        try (InputStream is = ctx.getAssets().open(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    public NumbersCanvas(Context context, SwiftLiteIME ime, KeyboardView parent) {
        super(context, ime, parent); init(context);
    }

    @Override
    void rebuildKeys() {
        mKeys.clear();
        if (mWidth <= 0) mWidth = getWidth();
        if (mWidth <= 0) return;
        if (mIME.isNumberMode()) {
            layoutNumberPad();
        } else {
            int kh = mKeyHeight, pad = mPad;
            String[][][] page = mPage == 0
                    ? new String[][][]{P1_NUM, P1_SYM1, P1_SYM2}
                    : new String[][][]{P2_MISC, P2_CURR, P2_MATH};
            int[] counts = new int[]{10, 10, 7};
            for (int row = 0; row < 3; row++)
                layoutSymRow(page[row], pad + (kh + pad) * row, counts[row], row == 2);
            layoutBottomRow(pad + (kh + pad) * 3);
        }
        clampActionHitRects();
        extendEdgeHitRects();

        if (mIME != null) {
            int enterIcon = KeyIcons.resolveEnterIcon(mIME.getCurrentInputEditorInfo());
            for (Key k : mKeys) {
                if (k.code == KeyboardView.KEY_ENTER) {
                    k.label = "";
                    k.icon = enterIcon;
                    break;
                }
            }
        }
    }

    private void layoutNumberPad() {
        int kh = mKeyHeight, pad = mPad;
        float colW = (mWidth - pad * 5) / 4f;
        float x2 = pad + colW + pad, x3 = pad + (colW + pad) * 2, x4 = pad + (colW + pad) * 3;
        addKey("1", pad, pad, colW, kh); addKey("2", x2, pad, colW, kh);
        addKey("3", x3, pad, colW, kh);
        addSpecial(KeyboardView.KEY_DELETE, x4, pad, colW, kh, false, KeyIcons.IC_BACKSPACE);
        addKey("4", pad, pad+kh+pad, colW, kh); addKey("5", x2, pad+kh+pad, colW, kh);
        addKey("6", x3, pad+kh+pad, colW, kh);
        addSpecial(KeyboardView.KEY_ENTER, x4, pad+kh+pad, colW, kh*3+pad*2, true, KeyIcons.IC_ENTER);
        addKey("7", pad, pad+(kh+pad)*2, colW, kh); addKey("8", x2, pad+(kh+pad)*2, colW, kh);
        addKey("9", x3, pad+(kh+pad)*2, colW, kh);
        addSpecial(KeyboardView.KEY_NUMBERS, pad, pad+(kh+pad)*3, colW, kh, false, KeyIcons.IC_ALPHA);
        addKey("0", x2, pad+(kh+pad)*3, colW, kh);
        addKey(".", x3, pad+(kh+pad)*3, colW, kh);
    }

    private void addKey(String label, float x, float y, float w, float h) {
        Key k = new Key(); k.label = label; k.code = label.charAt(0);
        k.x = x; k.y = y; k.w = w; k.h = h; k.hitX = x; k.hitW = w;
        mKeys.add(k);
    }

    @Override boolean hasLongPressOptions(Key key) { return key.subLabel != null && !key.subLabel.isEmpty(); }

    @Override
    void onLongPress(Key key) {
        List<String> opts = new ArrayList<>();
        for (String p : splitSub(key.subLabel)) if (!p.isEmpty()) opts.add(p);
        if (!opts.isEmpty()) { mPopupManager.showPopup(key, opts, mTheme); mLongPressFired = true; vibrate(); }
    }

    @Override
    void onNormalTap(Key key) {
        if (key.code == KeyboardView.KEY_NUMBERS) mParent.showPanel(KeyboardView.PANEL_KEYS);
        else if (key.code == KEY_PAGE) { mPage = 1 - mPage; rebuildKeys(); invalidate(); }
        else mIME.onKeyPress(key.code, key.label);
    }

    @Override boolean showKeyPreviewOnDown(Key key) { return !key.isAction && key.label != null && !key.label.isEmpty(); }
    @Override String subLabelFor(Key key) { return firstSub(key.subLabel); }

    private void layoutSymRow(String[][] defs, int yTop, int count, boolean withPage) {
        float actionW = 1.5f;
        int actionCount = withPage ? 2 : 0;
        int gaps = count + 1 + actionCount;
        float keyW = (mWidth - mPad * gaps) / (count + actionW * actionCount);

        float x = mPad;

        if (withPage) {
            float pw = keyW * actionW;
            int pageIcon = mPage == 0 ? KeyIcons.IC_PAGE_NEXT : KeyIcons.IC_PAGE_PREV;
            addSpecial(KEY_PAGE, x, yTop, pw, mKeyHeight, false, pageIcon);
            x += pw + mPad;
        }

        for (String[] def : defs) {
            Key k = new Key();
            k.label = def[0]; k.subLabel = def.length > 1 ? def[1] : "";
            k.code = k.label.length() == 1 ? k.label.charAt(0) : 0;
            k.x = x; k.y = yTop; k.w = keyW; k.h = mKeyHeight;
            k.hitX = x; k.hitW = keyW; k.isAction = isActionCode(k.code);
            mKeys.add(k); x += keyW + mPad;
        }

        if (withPage) {
            float dw = keyW * actionW;
            Key del = new Key(); del.code = KeyboardView.KEY_DELETE; del.label = ""; del.subLabel = "";
            del.icon = KeyIcons.IC_BACKSPACE; del.x = x; del.y = yTop;
            del.w = mWidth - x - mPad; del.h = mKeyHeight;
            del.hitX = x; del.hitW = del.w; del.isSpecial = true; del.isAction = true;
            mKeys.add(del);
        }
    }

    private void layoutBottomRow(int yTop) {
        float u = (mWidth - mPad * 6) / 8.0f;
        float abcW = u * 1.6f, commaW = u * 0.7f, dotW = u * 0.7f, enterW = u * 1.6f;
        float spaceW = mWidth - mPad * 6 - abcW - commaW - dotW - enterW;
        float x = mPad;
        addSpecial(KeyboardView.KEY_NUMBERS, x, yTop, abcW,   mKeyHeight, false, KeyIcons.IC_ALPHA);  x += abcW   + mPad;
        addLiteralKey(",", x, yTop, commaW, mKeyHeight);                                               x += commaW + mPad;
        addSpecial(KeyboardView.KEY_SPACE,   x, yTop, spaceW, mKeyHeight, false, KeyIcons.IC_SPACE);  x += spaceW + mPad;
        addLiteralKey(".", x, yTop, dotW,   mKeyHeight);                                               x += dotW   + mPad;
        addSpecial(KeyboardView.KEY_ENTER,   x, yTop, mWidth - x - mPad, mKeyHeight, true, KeyIcons.IC_ENTER);
    }

    private void addLiteralKey(String label, float x, float y, float w, float h) {
        Key k = new Key(); k.label = label; k.code = label.charAt(0); k.subLabel = "";
        k.x = x; k.y = y; k.w = w; k.h = h; k.hitX = x; k.hitW = w; mKeys.add(k);
    }

    private void addSpecial(int code, float x, float y, float w, float h, boolean accent, int icon) {
        Key k = new Key(); k.code = code; k.label = ""; k.subLabel = "";
        k.x = x; k.y = y; k.w = w; k.h = h; k.hitX = x; k.hitW = w;
        k.isSpecial = true; k.isAccent = accent; k.icon = icon; k.isAction = true; mKeys.add(k);
    }

    public void updateEditorInfo(EditorInfo info) {
        rebuildKeys();
        for (Key k : mKeys) {
            if (k.code != KeyboardView.KEY_ENTER) continue;
            k.label = ""; k.icon = KeyIcons.resolveEnterIcon(info); break;
        }
        invalidate();
    }

    private static boolean isActionCode(int code) {
        return code == KeyboardView.KEY_DELETE || code == KeyboardView.KEY_ENTER
            || code == KeyboardView.KEY_NUMBERS || code == KeyboardView.KEY_UNDO
            || code == KeyboardView.KEY_SPACE   || code == KEY_PAGE;
    }

    private static String[] splitSub(String s) {
        return (s == null || s.isEmpty()) ? new String[0] : s.split(SEP, -1);
    }
    private static String firstSub(String s) {
        if (s == null || s.isEmpty()) return "";
        int i = s.indexOf(SEP); return i < 0 ? s : s.substring(0, i);
    }
}
