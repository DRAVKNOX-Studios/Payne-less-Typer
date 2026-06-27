package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.Random;

public class IconView extends View {
    private int mIcon = -1;
    private int mColor = 0xFFFFFFFF;
    private float mSizeDp = 24;

    private float mEyeX = 0, mEyeY = 0;
    private float mTargetX = 0, mTargetY = 0;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Random mRandom = new Random();
    private boolean mAnimating = false;

    public IconView(Context context) {
        super(context);
    }

    public void setIcon(int icon) {
        mIcon = icon;
        if (icon == KeyIcons.IC_LOGO && !mAnimating) {
            startAnimation();
        }
        invalidate();
    }

    public void setColor(int color) {
        mColor = color;
        invalidate();
    }

    public void setIconSize(float sizeDp) {
        mSizeDp = sizeDp;
        invalidate();
    }

    private void startAnimation() {
        mAnimating = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Math.abs(mEyeX - mTargetX) < 0.05f && Math.abs(mEyeY - mTargetY) < 0.05f) {
                    if (mRandom.nextInt(10) > 7) {
                        mTargetX = (mRandom.nextFloat() - 0.5f) * 1.4f;
                        mTargetY = (mRandom.nextFloat() - 0.5f) * 1.4f;
                    } else {
                        mTargetX = 0;
                        mTargetY = 0;
                    }
                }
                mEyeX += (mTargetX - mEyeX) * 0.15f;
                mEyeY += (mTargetY - mEyeY) * 0.15f;
                invalidate();
                mHandler.postDelayed(this, 30);
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mIcon != -1) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            KeyIcons.draw(canvas, mIcon, cx, cy, mSizeDp, getResources().getDisplayMetrics().density, mColor, mEyeX, mEyeY);
        }
    }
}
