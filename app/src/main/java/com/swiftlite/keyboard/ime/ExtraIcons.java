package com.swiftlite.keyboard.ime;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public final class ExtraIcons {

    private static final Path sPath = new Path();
    private static final RectF sRect = new RectF();

    public static void drawSettings(Canvas c, float cx, float cy, float r, Paint sPaint) {
        float outerR = r * 0.85f;
        float innerR = r * 0.58f;
        float holeR  = r * 0.22f;
        c.drawCircle(cx, cy, innerR, sPaint);
        c.drawCircle(cx, cy, holeR, sPaint);
        for (int i = 0; i < 8; i++) {
            double a1 = Math.toRadians(i * 45 - 12);
            double a2 = Math.toRadians(i * 45 + 12);
            float x1 = cx + (float) Math.cos(a1) * innerR;
            float y1 = cy + (float) Math.sin(a1) * innerR;
            float x2 = cx + (float) Math.cos(a1) * outerR;
            float y2 = cy + (float) Math.sin(a1) * outerR;
            float x3 = cx + (float) Math.cos(a2) * outerR;
            float y3 = cy + (float) Math.sin(a2) * outerR;
            float x4 = cx + (float) Math.cos(a2) * innerR;
            float y4 = cy + (float) Math.sin(a2) * innerR;
            sPath.reset();
            sPath.moveTo(x1, y1);
            sPath.lineTo(x2, y2);
            sPath.lineTo(x3, y3);
            sPath.lineTo(x4, y4);
            sPath.close();
            c.drawPath(sPath, sPaint);
        }
    }

    public static void drawLogo(Canvas c, float cx, float cy, float r, Paint sPaint, Paint sFill, float ox, float oy, float sw) {
        float kw = r * 1.8f, kh = r * 1.1f;
        float ky = cy + r * 0.45f;
        sRect.set(cx - kw, ky - kh, cx + kw, ky + kh);
        c.drawRoundRect(sRect, r * 0.15f, r * 0.15f, sPaint);
        float inKeyW = kw * 0.25f, inKeyH = kh * 0.18f;
        float startY = ky - kh * 0.35f;
        for (int row = 0; row < 3; row++) {
            float inY = startY + row * (inKeyH + sw * 2.2f);
            int cols = (row % 2 == 0) ? 4 : 3;
            for (int col = 0; col < cols; col++) {
                float kx = cx + (col - (cols - 1) / 2f) * (inKeyW + sw * 2.2f);
                sRect.set(kx - inKeyW / 2, inY - inKeyH / 2, kx + inKeyW / 2, inY + inKeyH / 2);
                c.drawRoundRect(sRect, sw * 0.7f, sw * 0.7f, sPaint);
            }
        }
        float eyeR = r * 0.38f, eyeY = ky - kh, eyeX = r * 0.65f;
        c.drawCircle(cx - eyeX, eyeY, eyeR, sPaint);
        c.drawCircle(cx + eyeX, eyeY, eyeR, sPaint);
        float pR = eyeR * 0.45f;
        c.drawCircle(cx - eyeX + ox * (eyeR - pR), eyeY + oy * (eyeR - pR), pR, sFill);
        c.drawCircle(cx + eyeX + ox * (eyeR - pR), eyeY + oy * (eyeR - pR), pR, sFill);
    }

    public static void drawUndo(Canvas c, float cx, float cy, float r, float sw, Paint sPaint, Paint sFill) {
        float ar = r * 0.52f, acx = cx + r * 0.04f, acy = cy + r * 0.04f;
        sRect.set(acx - ar, acy - ar, acx + ar, acy + ar);
        c.drawArc(sRect, -30f, 210f, false, sPaint);
        float ex = acx - ar, ey = acy, ah = sw * 2.8f, aw = sw * 1.8f;
        sPath.reset(); sPath.moveTo(ex, ey); sPath.lineTo(ex - aw, ey - ah); sPath.lineTo(ex + aw, ey - ah); sPath.close();
        c.drawPath(sPath, sFill);
    }

    public static void drawEmoji(Canvas c, float cx, float cy, float r, float sw, Paint sPaint, Paint sFill) {
        c.drawCircle(cx, cy, r * 0.7f, sPaint);
        float eyeY = cy - r * 0.2f, eyeX = r * 0.24f;
        c.drawCircle(cx - eyeX, eyeY, sw * 0.9f, sFill);
        c.drawCircle(cx + eyeX, eyeY, sw * 0.9f, sFill);
        sRect.set(cx - r * 0.36f, cy - r * 0.05f, cx + r * 0.36f, cy + r * 0.42f);
        c.drawArc(sRect, 0, 180, false, sPaint);
    }

    public static void drawClipboard(Canvas c, float cx, float cy, float r, Paint sPaint) {
        float bw = r * 1.05f, bh = r * 1.25f, bTop = cy - bh * 0.44f;
        sRect.set(cx - bw * 0.5f, bTop, cx + bw * 0.5f, bTop + bh);
        c.drawRoundRect(sRect, r * 0.16f, r * 0.16f, sPaint);
        float clipW = r * 0.46f, clipH = r * 0.22f;
        sRect.set(cx - clipW * 0.5f, bTop - clipH * 0.5f, cx + clipW * 0.5f, bTop + clipH * 0.5f);
        c.drawRoundRect(sRect, clipH * 0.5f, clipH * 0.5f, sPaint);
        float lx0 = cx - bw * 0.28f, lx1 = cx + bw * 0.28f;
        c.drawLine(lx0, bTop + bh * 0.42f, lx1, bTop + bh * 0.42f, sPaint);
        c.drawLine(lx0, bTop + bh * 0.66f, lx1, bTop + bh * 0.66f, sPaint);
    }

    public static void drawClock(Canvas c, float cx, float cy, float r, Paint sPaint) {
        c.drawCircle(cx, cy, r * 0.72f, sPaint);
        c.drawLine(cx, cy, cx - r * 0.26f, cy - r * 0.36f, sPaint);
        c.drawLine(cx, cy, cx, cy - r * 0.50f, sPaint);
    }

    public static void drawFlag(Canvas c, float cx, float cy, float r, Paint sPaint, Paint sFill) {
        float poleX = cx - r * 0.28f, poleTop = cy - r * 0.68f, poleBot = cy + r * 0.68f;
        c.drawLine(poleX, poleTop, poleX, poleBot, sPaint);
        float flagBot = poleTop + r * 0.72f, flagMid = (poleTop + flagBot) / 2f;
        sPath.reset(); sPath.moveTo(poleX, poleTop); sPath.lineTo(poleX, flagBot); sPath.lineTo(poleX + r * 0.92f, flagMid); sPath.close();
        c.drawPath(sPath, sFill);
    }

    public static void drawSearch(Canvas c, float cx, float cy, float r, Paint sPaint) {
        float cr = r * 0.45f, ox = r * 0.15f, oy = r * 0.15f;
        c.drawCircle(cx - ox, cy - oy, cr, sPaint);
        double angle = Math.toRadians(45);
        c.drawLine(cx - ox + cr * (float) Math.cos(angle), cy - oy + cr * (float) Math.sin(angle), cx + r * 0.7f, cy + r * 0.7f, sPaint);
    }

    public static void drawSend(Canvas c, float cx, float cy, float r, Paint sPaint) {
        float x0 = cx - r * 0.7f, y0 = cy - r * 0.7f, x1 = cx + r * 0.8f, y1 = cy, x2 = cx - r * 0.7f, y2 = cy + r * 0.7f, xm = cx - r * 0.2f, ym = cy;
        sPath.reset(); sPath.moveTo(x0, y0); sPath.lineTo(x1, y1); sPath.lineTo(x2, y2); sPath.lineTo(xm, ym); sPath.close();
        c.drawPath(sPath, sPaint);
        c.drawLine(xm, ym, x1, y1, sPaint);
    }

    public static void drawDone(Canvas c, float cx, float cy, float r, Paint sPaint) {
        c.drawLine(cx - r * 0.6f, cy, cx - r * 0.15f, cy + r * 0.5f, sPaint);
        c.drawLine(cx - r * 0.15f, cy + r * 0.5f, cx + r * 0.7f, cy - r * 0.6f, sPaint);
    }
}
