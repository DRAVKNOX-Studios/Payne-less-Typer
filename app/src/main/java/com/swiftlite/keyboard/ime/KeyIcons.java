package com.swiftlite.keyboard.ime;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;

public final class KeyIcons {

    public static final int IC_BACKSPACE  = 0;
    public static final int IC_SHIFT      = 1;
    public static final int IC_SHIFT_ON   = 2;
    public static final int IC_CAPS       = 3;
    public static final int IC_ENTER      = 4;
    public static final int IC_SPACE      = 5;
    public static final int IC_UNDO       = 6;
    public static final int IC_EMOJI      = 7;
    public static final int IC_CLIPBOARD  = 8;
    public static final int IC_BACK       = 9;
    public static final int IC_PAGE_NEXT  = 10;
    public static final int IC_PAGE_PREV  = 11;
    public static final int IC_PIN        = 12;
    public static final int IC_NUMBERS    = 13;
    public static final int IC_CROSS_RED  = 14;
    public static final int IC_ALPHA      = 15;
    public static final int IC_CLOCK      = 16;
    public static final int IC_FLAG       = 17;
    public static final int IC_LOGO       = 18;
    public static final int IC_SETTINGS   = 19;
    public static final int IC_SEARCH     = 20;
    public static final int IC_DONE       = 21;
    public static final int IC_SEND       = 22;
    public static final int IC_GO         = 23;
    public static final int IC_NEXT       = 24;

    private static final Paint sPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint sFill  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path  sPath  = new Path();
    private static final RectF sRect  = new RectF();
    private static final Paint sTextP = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        sPaint.setStyle(Paint.Style.STROKE); sPaint.setStrokeCap(Paint.Cap.ROUND); sPaint.setStrokeJoin(Paint.Join.ROUND);
        sFill.setStyle(Paint.Style.FILL); sTextP.setTextAlign(Paint.Align.CENTER); sTextP.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    }

    public static int resolveEnterIcon(EditorInfo info) {
        if (info == null) return IC_ENTER;
        int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean isMultiLine = (info.inputType & InputType.TYPE_CLASS_TEXT) != 0 && (info.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        if ((info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return IC_ENTER;
        switch (action) {
            case EditorInfo.IME_ACTION_NEXT:   return IC_NEXT;
            case EditorInfo.IME_ACTION_SEARCH: return IC_SEARCH;
            case EditorInfo.IME_ACTION_GO:     return PrivacyHandler.isUriField(info) ? IC_SEARCH : IC_GO;
            case EditorInfo.IME_ACTION_SEND:   return IC_SEND;
            case EditorInfo.IME_ACTION_DONE:   return isMultiLine ? IC_ENTER : IC_DONE;
            default:                           return isMultiLine ? IC_ENTER : IC_DONE;
        }
    }

    private KeyIcons() {}

    public static void draw(Canvas c, int icon, float cx, float cy, float sizeDp, float density, int color) { draw(c, icon, cx, cy, sizeDp, density, color, 0, 0); }

    public static void draw(Canvas c, int icon, float cx, float cy, float sizeDp, float density, int color, float ox, float oy) {
        float r = sizeDp * density * 0.5f, sw = density * 1.8f;
        sPaint.setColor(color); sPaint.setStrokeWidth(sw); sFill.setColor(color); sPath.reset();
        switch (icon) {
            case IC_BACKSPACE:  drawBackspace(c, cx, cy, r, sw); break;
            case IC_SHIFT:      drawShift(c, cx, cy, r, sw, false); break;
            case IC_SHIFT_ON:   drawShift(c, cx, cy, r, sw, true); break;
            case IC_CAPS:       drawCaps(c, cx, cy, r, sw); break;
            case IC_ENTER:      drawEnter(c, cx, cy, r, sw); break;
            case IC_SPACE:      drawSpace(c, cx, cy, r, sw); break;
            case IC_UNDO:       ExtraIcons.drawUndo(c, cx, cy, r, sw, sPaint, sFill); break;
            case IC_EMOJI:      ExtraIcons.drawEmoji(c, cx, cy, r, sw, sPaint, sFill); break;
            case IC_CLIPBOARD:  ExtraIcons.drawClipboard(c, cx, cy, r, sPaint); break;
            case IC_BACK:       case IC_PAGE_PREV: drawChevron(c, cx, cy, r, sw, false); break;
            case IC_PAGE_NEXT:  drawChevron(c, cx, cy, r, sw, true); break;
            case IC_PIN:        drawPin(c, cx, cy, r, sw); break;
            case IC_NUMBERS:    drawLabel(c, cx, cy, r, color, "123"); break;
            case IC_ALPHA:      drawLabel(c, cx, cy, r, color, "ABC"); break;
            case IC_CROSS_RED:  drawCrossRed(c, cx, cy, r, sw); break;
            case IC_CLOCK:      ExtraIcons.drawClock(c, cx, cy, r, sPaint); break;
            case IC_FLAG:       ExtraIcons.drawFlag(c, cx, cy, r, sPaint, sFill); break;
            case IC_LOGO:       ExtraIcons.drawLogo(c, cx, cy, r, sPaint, sFill, ox, oy, sw); break;
            case IC_SETTINGS:   ExtraIcons.drawSettings(c, cx, cy, r, sPaint); break;
            case IC_SEARCH:     ExtraIcons.drawSearch(c, cx, cy, r, sPaint); break;
            case IC_DONE:       ExtraIcons.drawDone(c, cx, cy, r, sPaint); break;
            case IC_SEND:       case IC_GO: ExtraIcons.drawSend(c, cx, cy, r, sPaint); break;
            case IC_NEXT:       drawNext(c, cx, cy, r, sw); break;
        }
    }

    private static void drawBackspace(Canvas c, float cx, float cy, float r, float sw) {
        float bw = r * 1.3f, bh = r * 0.78f, tipX = cx - r * 0.72f, flatX = cx + bw * 0.42f, notchW = r * 0.28f, xc = cx + r * 0.08f;
        sPath.rewind(); sPath.moveTo(tipX, cy); sPath.lineTo(cx - r * 0.28f, cy - bh); sPath.lineTo(flatX, cy - bh); sPath.lineTo(flatX, cy + bh); sPath.lineTo(cx - r * 0.28f, cy + bh); sPath.close();
        c.drawPath(sPath, sPaint); c.drawLine(xc - notchW, cy - notchW, xc + notchW, cy + notchW, sPaint); c.drawLine(xc + notchW, cy - notchW, xc - notchW, cy + notchW, sPaint);
    }

    private static void drawShift(Canvas c, float cx, float cy, float r, float sw, boolean filled) {
        float hw = r * 0.62f, stemW = r * 0.38f, top = cy - r * 0.7f, mid = cy - r * 0.05f, bot = cy + r * 0.7f;
        sPath.rewind(); sPath.moveTo(cx, top); sPath.lineTo(cx + hw, mid); sPath.lineTo(cx + stemW, mid); sPath.lineTo(cx + stemW, bot); sPath.lineTo(cx - stemW, bot); sPath.lineTo(cx - stemW, mid); sPath.lineTo(cx - hw, mid); sPath.close();
        c.drawPath(sPath, filled ? sFill : sPaint);
    }

    private static void drawCaps(Canvas c, float cx, float cy, float r, float sw) { drawShift(c, cx, cy - r * 0.12f, r * 0.82f, sw, true); float lineY = cy + r * 0.7f; c.drawLine(cx - r * 0.52f, lineY, cx + r * 0.52f, lineY, sPaint); }

    private static void drawEnter(Canvas c, float cx, float cy, float r, float sw) {
        float x0 = cx + r * 0.6f, x1 = cx - r * 0.52f, yTop = cy - r * 0.38f, yMid = cy + r * 0.18f, arrLen = r * 0.35f;
        c.drawLine(x0, yTop, x0, yMid, sPaint); c.drawLine(x0, yMid, x1, yMid, sPaint); c.drawLine(x1, yMid, x1 + arrLen, yMid - arrLen, sPaint); c.drawLine(x1, yMid, x1 + arrLen, yMid + arrLen, sPaint);
    }

    private static void drawNext(Canvas c, float cx, float cy, float r, float sw) {
        float tailX = cx - r * 0.65f, headX = cx + r * 0.65f, arrLen = r * 0.38f;
        c.drawLine(tailX, cy, headX, cy, sPaint); c.drawLine(headX, cy, headX - arrLen, cy - arrLen * 0.7f, sPaint); c.drawLine(headX, cy, headX - arrLen, cy + arrLen * 0.7f, sPaint);
    }

    private static void drawSpace(Canvas c, float cx, float cy, float r, float sw) {
        float w = r * 0.85f, barH = sw * 1.5f, barY = cy + r * 0.18f, legTop = cy - r * 0.18f;
        sRect.set(cx - w, barY, cx + w, barY + barH); c.drawRoundRect(sRect, barH / 2, barH / 2, sFill); c.drawLine(cx - w, legTop, cx - w, barY, sPaint); c.drawLine(cx + w, legTop, cx + w, barY, sPaint);
    }

    private static void drawChevron(Canvas c, float cx, float cy, float r, float sw, boolean right) {
        float arm = r * 0.52f;
        if (right) { c.drawLine(cx - arm * 0.55f, cy - arm, cx + arm * 0.55f, cy, sPaint); c.drawLine(cx + arm * 0.55f, cy, cx - arm * 0.55f, cy + arm, sPaint); }
        else { c.drawLine(cx + arm * 0.55f, cy - arm, cx - arm * 0.55f, cy, sPaint); c.drawLine(cx - arm * 0.55f, cy, cx + arm * 0.55f, cy + arm, sPaint); }
    }

    private static void drawPin(Canvas c, float cx, float cy, float r, float sw) { c.drawCircle(cx, cy - r * 0.28f, r * 0.38f, sPaint); c.drawLine(cx, cy - r * 0.28f + r * 0.38f, cx, cy + r * 0.62f, sPaint); c.drawLine(cx - r * 0.28f, cy + r * 0.62f, cx + r * 0.28f, cy + r * 0.62f, sPaint); }

    private static void drawLabel(Canvas c, float cx, float cy, float r, int color, String text) { sTextP.setColor(color); sTextP.setTextSize(r * 1.05f); c.drawText(text, cx, cy - (sTextP.descent() + sTextP.ascent()) / 2f, sTextP); }

    private static void drawCrossRed(Canvas c, float cx, float cy, float r, float sw) { sPaint.setColor(0xFFE05252); float arm = r * 0.52f; c.drawLine(cx - arm, cy - arm, cx + arm, cy + arm, sPaint); c.drawLine(cx + arm, cy - arm, cx - arm, cy + arm, sPaint); sPaint.setColor(sFill.getColor()); }
}
