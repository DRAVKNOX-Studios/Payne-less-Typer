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
    private static String[][] P1_NUM, P1_SYM1, P1_SYM2, P2_MISC, P2_CURR, P2_MATH;
    private static boolean sLoaded = false;
    private boolean mIsSearchField = false;

    public static void init(Context ctx) {
        if (sLoaded) return;
        try {
            JSONObject obj = new JSONObject(readAsset(ctx, "numbers_layout.json"));
            P1_NUM = parseRow(obj.getJSONArray("p1_num")); P1_SYM1 = parseRow(obj.getJSONArray("p1_sym1")); P1_SYM2 = parseRow(obj.getJSONArray("p1_sym2"));
            P2_MISC = parseRow(obj.getJSONArray("p2_misc")); P2_CURR = parseRow(obj.getJSONArray("p2_curr")); P2_MATH = parseRow(obj.getJSONArray("p2_math"));
            com.swiftlite.keyboard.emoji.EmojiData.init(ctx.getAssets()); com.swiftlite.keyboard.emoji.EmojiSkinToneHelper.init(ctx); sLoaded = true;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String[][] parseRow(JSONArray arr) throws Exception {
        String[][] r = new String[arr.length()][];
        for (int i=0; i<arr.length(); i++) { JSONArray k = arr.getJSONArray(i); r[i] = new String[k.length()]; for (int j=0; j<k.length(); j++) r[i][j] = k.getString(j); }
        return r;
    }
    private static String readAsset(Context ctx, String p) throws Exception { try (InputStream is = ctx.getAssets().open(p); BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) { StringBuilder sb = new StringBuilder(); String l; while ((l = br.readLine()) != null) sb.append(l); return sb.toString(); } }

    public NumbersCanvas(Context context, SwiftLiteIME ime, KeyboardView parent) { super(context, ime, parent); init(context); }

    private void layoutEmojiRow(int y) {
        String skin = mIME.getSelectedEmojiSkin(); String[] rec = mIME.getRecentEmojis(); List<String> ems = new ArrayList<>();
        if (rec != null) for (String e : rec) { if (ems.size()>=10) break; String t = com.swiftlite.keyboard.emoji.EmojiSkinToneHelper.isToneSupportedEmoji(e) ? com.swiftlite.keyboard.emoji.EmojiSkinToneHelper.applyTone(e, skin) : e; if (mTextPaint.hasGlyph(t) && !ems.contains(t)) ems.add(t); }
        if (ems.size()<10 && com.swiftlite.keyboard.emoji.EmojiData.ALL != null) for (String[] cat : com.swiftlite.keyboard.emoji.EmojiData.ALL) { if (ems.size()>=10) break; for (String e : cat) { if (ems.size()>=10) break; String t = com.swiftlite.keyboard.emoji.EmojiSkinToneHelper.isToneSupportedEmoji(e) ? com.swiftlite.keyboard.emoji.EmojiSkinToneHelper.applyTone(e, skin) : e; if (mTextPaint.hasGlyph(t) && !ems.contains(t)) ems.add(t); } }
        float kw = (mWidth - mPad*11)/10f; float x = mPad;
        for (String e : ems) { Key k = new Key(); k.label = e; k.code = 0; k.x = x; k.y = y; k.w = kw; k.h = mKeyHeight; k.hitX = x; k.hitW = kw; mKeys.add(k); x += kw + mPad; }
    }

    private void layoutNumberOnlySymbolRow(int y) { layoutSymRow(new String[][]{{"(",")"},{"-","—"},{"/","\\"},{":",""},{"+ ",""},{"* ",""},{"#",""}}, y, 10, false); }

    @Override void rebuildKeys() {
        updateDimensions(); mKeys.clear(); if (mWidth <= 0) mWidth = getWidth(); if (mWidth <= 0) return;
        int kh = mKeyHeight, pad = mPad; boolean nr = mIME.getThemeManager().isNumberRowEnabled(); int sr = nr ? 1 : 0;
        if (nr) { if (mIME.isNumberMode()) layoutNumberOnlySymbolRow(pad); else layoutEmojiRow(pad); }
        if (mIME.isNumberMode()) layoutNumberPad(sr);
        else { String[][][] p = mPage==0 ? new String[][][]{P1_NUM, P1_SYM1, P1_SYM2} : new String[][][]{P2_MISC, P2_CURR, P2_MATH}; int[] c = {10,10,7}; for (int i=0; i<3; i++) layoutSymRow(p[i], pad+(kh+pad)*(i+sr), c[i], i==2); layoutBottomRow(pad+(kh+pad)*(3+sr)); }
        clampActionHitRects(); extendEdgeHitRects();
        int ei = KeyIcons.resolveEnterIcon(mIME.getCurrentInputEditorInfo());
        for (Key k : mKeys) {
            if (k.code == KeyboardView.KEY_ENTER) { k.label = ""; k.icon = ei; k.subLabel = "icon:" + KeyIcons.IC_ONEHAND_R; }
            else if (k.code == KeyboardView.KEY_NUMBERS) { k.subLabel = "icon:" + KeyIcons.IC_ONEHAND_L; }
            else if (k.code == (int) ',') { k.subLabel = "«"; }
            else if (k.code == (int) '.') { k.subLabel = mIsSearchField ? ".com" : "…"; }
        }
    }

    private void layoutNumberPad(int sr) {
        int kh = mKeyHeight, pad = mPad, yO = sr*(kh+pad); float cw = (mWidth-pad*5)/4f;
        addKey("1", pad, pad+yO, cw, kh); addKey("2", pad+cw+pad, pad+yO, cw, kh); addKey("3", pad+(cw+pad)*2, pad+yO, cw, kh); addSpecial(KeyboardView.KEY_DELETE, pad+(cw+pad)*3, pad+yO, cw, kh, false, KeyIcons.IC_BACKSPACE);
        addKey("4", pad, pad+kh+pad+yO, cw, kh); addKey("5", pad+cw+pad, pad+kh+pad+yO, cw, kh); addKey("6", pad+(cw+pad)*2, pad+kh+pad+yO, cw, kh); addSpecial(KeyboardView.KEY_ENTER, pad+(cw+pad)*3, pad+kh+pad+yO, cw, kh*3+pad*2, true, KeyIcons.IC_ENTER);
        addKey("7", pad, pad+(kh+pad)*2+yO, cw, kh); addKey("8", pad+cw+pad, pad+(kh+pad)*2+yO, cw, kh); addKey("9", pad+(cw+pad)*2, pad+(kh+pad)*2+yO, cw, kh);
        addSpecial(KeyboardView.KEY_NUMBERS, pad, pad+(kh+pad)*3+yO, cw, kh, false, KeyIcons.IC_ALPHA); addKey("0", pad+cw+pad, pad+(kh+pad)*3+yO, cw, kh); addKey(".", pad+(cw+pad)*2, pad+(kh+pad)*3+yO, cw, kh);
    }

    private void addKey(String l, float x, float y, float w, float h) { Key k = new Key(); k.label = l; k.code = l.charAt(0); k.x = x; k.y = y; k.w = w; k.h = h; k.hitX = x; k.hitW = w; mKeys.add(k); }
    @Override boolean hasLongPressOptions(Key k) { if (k.code == KeyboardView.KEY_NUMBERS || k.code == KeyboardView.KEY_ENTER) return true; if (k.code == (int) ',') return true; return (mIsSearchField && k.code == (int) '.' && ".".equals(k.label)) || (k.subLabel != null && !k.subLabel.isEmpty()); }
    @Override void onLongPress(Key k) {
        LayoutController lc = mIME.getLayoutController();
        if (k.code == KeyboardView.KEY_NUMBERS) { mPopupManager.showPopup(k, java.util.Arrays.asList("icon:" + KeyIcons.IC_ONEHAND_L + (lc.getMode()==1?":active":"")), mTheme); mLongPressFired = true; vibrate(); return; }
        if (k.code == KeyboardView.KEY_ENTER) { mPopupManager.showPopup(k, java.util.Arrays.asList("icon:" + KeyIcons.IC_ONEHAND_R + (lc.getMode()==2?":active":"")), mTheme); mLongPressFired = true; vibrate(); return; }
        if (mIsSearchField && k.code == (int) '.' && ".".equals(k.label)) { mPopupManager.showScrollablePopup(k, KeyboardLayout.getInstance(getContext()).searchDomains, mTheme); mLongPressFired = true; vibrate(); return; }
        List<String> opts = new ArrayList<>();
        String sL = k.subLabel == null ? "" : k.subLabel;
        for (String s : sL.split(SEP, -1)) if (!s.isEmpty() && !s.startsWith("icon:") && !s.equals("…") && !s.equals(".com")) opts.add(s);
        if (k.code == (int) ',') { String[] lAlts = KeyboardLayout.getInstance(getContext()).longPress.get(','); if (lAlts != null) for (String a : lAlts) if (!a.equals("…")) opts.add(a); }
        if (!opts.isEmpty()) { mPopupManager.showPopup(k, opts, mTheme); mLongPressFired = true; vibrate(); }
    }
    @Override void onNormalTap(Key k) { if (k.code == KeyboardView.KEY_NUMBERS) mParent.showPanel(0); else if (k.code == KEY_PAGE) { mPage = 1-mPage; rebuildKeys(); invalidate(); } else if (k.code == 0 && k.label != null && !k.label.isEmpty()) mIME.commitEmoji(k.label); else mIME.onKeyPress(k.code, k.label); }
    @Override boolean showKeyPreviewOnDown(Key k) { return (k.code != 0 || k.label == null || k.label.length() <= 1) && !k.isAction && k.label != null && !k.label.isEmpty(); }
    @Override String subLabelFor(Key k) { String s = k.subLabel; if (s==null||s.isEmpty()) return ""; int i = s.indexOf(SEP); return i<0?s:s.substring(0,i); }
    private void layoutSymRow(String[][] defs, int y, int count, boolean withPage) { float aw = 1.5f; int ac = withPage ? 2 : 0; float kw = (mWidth - mPad*(count+1+ac))/(count + aw*ac); float x = mPad; if (withPage) { addSpecial(KEY_PAGE, x, y, kw*aw, mKeyHeight, false, mPage==0?KeyIcons.IC_PAGE_NEXT:KeyIcons.IC_PAGE_PREV); x += kw*aw+mPad; } for (String[] d : defs) { Key k = new Key(); k.label = d[0]; k.subLabel = d.length>1?d[1]:""; k.code = k.label.length()==1?k.label.charAt(0):0; k.x = x; k.y = y; k.w = kw; k.h = mKeyHeight; k.hitX = x; k.hitW = kw; k.isAction = (k.code==KeyboardView.KEY_SPACE); mKeys.add(k); x += kw+mPad; } if (withPage) { Key k = new Key(); k.code = KeyboardView.KEY_DELETE; k.icon = KeyIcons.IC_BACKSPACE; k.x = x; k.y = y; k.w = mWidth-x-mPad; k.h = mKeyHeight; k.hitX = x; k.hitW = k.w; k.isSpecial = k.isAction = true; mKeys.add(k); } }
    private void layoutBottomRow(int y) { float u = (mWidth-mPad*6)/8.0f; float abcW = u*1.6f, cW = u*0.7f, dW = u*0.7f, eW = u*1.6f, sW = mWidth-mPad*6-abcW-cW-dW-eW, x = mPad; addSpecial(KeyboardView.KEY_NUMBERS, x, y, abcW, mKeyHeight, false, KeyIcons.IC_ALPHA); x += abcW+mPad; addKey(",", x, y, cW, mKeyHeight); x += cW+mPad; addSpecial(KeyboardView.KEY_SPACE, x, y, sW, mKeyHeight, false, KeyIcons.IC_SPACE); x += sW+mPad; addKey(".", x, y, dW, mKeyHeight); x += dW+mPad; addSpecial(KeyboardView.KEY_ENTER, x, y, mWidth-x-mPad, mKeyHeight, true, KeyIcons.IC_ENTER); }
    private void addSpecial(int c, float x, float y, float w, float h, boolean ac, int ic) { Key k = new Key(); k.code = c; k.x = x; k.y = y; k.w = w; k.h = h; k.hitX = x; k.hitW = w; k.isSpecial = k.isAction = true; k.isAccent = ac; k.icon = ic; mKeys.add(k); }
    public void refreshRecents() { if (mIME.getThemeManager().isNumberRowEnabled()) { rebuildKeys(); invalidate(); } }
    public void updateEditorInfo(EditorInfo info) { mIsSearchField = PrivacyHandler.isSearchField(info); rebuildKeys(); for (Key k : mKeys) if (k.code == KeyboardView.KEY_ENTER) { k.label = ""; k.icon = KeyIcons.resolveEnterIcon(info); break; } invalidate(); }
    @Override public void setTheme(com.swiftlite.keyboard.theme.KeyboardTheme theme) { super.setTheme(theme); rebuildKeys(); }
}
