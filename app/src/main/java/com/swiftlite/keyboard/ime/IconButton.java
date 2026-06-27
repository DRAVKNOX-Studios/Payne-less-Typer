package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import androidx.annotation.NonNull;

public class IconButton extends View {
    private int mIcon;
    private int mColor;
    private final float mDensity;

    public IconButton(Context ctx, int icon, int color) {
        super(ctx);
        mIcon = icon;
        mColor = color;
        mDensity = ctx.getResources().getDisplayMetrics().density;
    }

    public void setIcon(int icon) {
        mIcon = icon;
        invalidate();
    }

    public void setColor(int color) {
        mColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        KeyIcons.draw(canvas, mIcon, cx, cy, 20, mDensity, mColor);
    }
}
