package com.swiftlite.keyboard.ime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.swiftlite.keyboard.theme.KeyboardTheme;

import java.util.Random;

public class GooglyEyesView extends View {

    private static final float[][] POSES = {
        {  0f,    0f,    0f,    0f   },
        {  0f,   -1f,   0f,   -1f   },
        {  0f,    1f,   0f,    1f   },
        { -1f,    0f,  -1f,    0f   },
        {  1f,    0f,   1f,    0f   },
        { -1f,    0f,   1f,    0f   },
        {  1f,    0f,  -1f,    0f   },
        {  0f,   -1f,   0f,    1f   },
        {  0f,    0f,   1f,    0f   },
        { -1f,    0f,   0f,    0f   },
        {  0f,    0f,   0f,   -1f   },
        {  0f,   -1f,   0f,    0f   },
        {  0f,    0f,   0f,    1f   },
        {  0f,    1f,   0f,    0f   },
        {  1f,   -1f,  -1f,    1f   },
        { -1f,   -1f,   1f,   -1f   },
        {  0.7f,  0.7f,-0.7f,  0.7f},
    };

    private float mEyeR;
    private float mPupilR;
    private float mMaxDrift;
    private float mLeftX, mRightX, mCentreY;

    private float mLeftPX,  mLeftPY;
    private float mRightPX, mRightPY;

    private final Paint mScleraPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPupilPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Random mRng = new Random();
    private AnimatorSet  mCurrentAnim;
    private boolean      mRunning = false;
    private int          mLastPose = 0;

    public GooglyEyesView(Context context) {
        super(context);
        mScleraPaint.setColor(Color.WHITE);
        mOutlinePaint.setColor(Color.BLACK);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mPupilPaint.setColor(Color.BLACK);
    }

    public void setBarHeight(int barHeightPx) {
        mEyeR     = barHeightPx * 0.45f;
        mPupilR   = mEyeR * 0.42f;
        mMaxDrift = mEyeR - mPupilR - mEyeR * 0.08f;
        mOutlinePaint.setStrokeWidth(mEyeR * 0.09f);
        mCentreY  = barHeightPx * 0.5f;
        mLeftPX = mLeftPY = mRightPX = mRightPY = 0;
        requestLayout();
        invalidate();
    }

    public void setTheme(KeyboardTheme theme) {
        mOutlinePaint.setColor(theme.isDark ? 0xFF888888 : Color.BLACK);
        mPupilPaint.setColor(theme.isDark ? 0xFF555555 : Color.BLACK);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        float gap = mEyeR * 1.8f;
        mLeftX  = w * 0.5f - gap - mEyeR;
        mRightX = w * 0.5f + gap + mEyeR;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (mEyeR <= 0) return;
        drawEye(c, mLeftX,  mCentreY, mLeftPX,  mLeftPY);
        drawEye(c, mRightX, mCentreY, mRightPX, mRightPY);
    }

    private void drawEye(Canvas c, float cx, float cy, float dx, float dy) {
        c.drawCircle(cx, cy, mEyeR, mScleraPaint);
        c.drawCircle(cx, cy, mEyeR, mOutlinePaint);
        c.drawCircle(cx + dx, cy + dy, mPupilR, mPupilPaint);
    }

    public void startAnimation() {
        mRunning = true;
        scheduleNextPose();
    }

    public void stopAnimation() {
        mRunning = false;
        if (mCurrentAnim != null) { mCurrentAnim.cancel(); mCurrentAnim = null; }
    }

    private void scheduleNextPose() {
        if (!mRunning) return;

        int next;
        do { next = mRng.nextInt(POSES.length); } while (next == mLastPose && POSES.length > 1);
        mLastPose = next;
        float[] pose = POSES[next];

        float targetLX = pose[0] * mMaxDrift;
        float targetLY = pose[1] * mMaxDrift;
        float targetRX = pose[2] * mMaxDrift;
        float targetRY = pose[3] * mMaxDrift;

        long transMs = 180 + mRng.nextInt(160);

        ValueAnimator lx = ValueAnimator.ofFloat(mLeftPX,  targetLX);
        ValueAnimator ly = ValueAnimator.ofFloat(mLeftPY,  targetLY);
        ValueAnimator rx = ValueAnimator.ofFloat(mRightPX, targetRX);
        ValueAnimator ry = ValueAnimator.ofFloat(mRightPY, targetRY);

        for (ValueAnimator a : new ValueAnimator[]{lx, ly, rx, ry}) {
            a.setDuration(transMs);
            a.setInterpolator(new OvershootInterpolator(2.2f));
        }

        lx.addUpdateListener(v -> { mLeftPX  = (float) v.getAnimatedValue(); invalidate(); });
        ly.addUpdateListener(v -> { mLeftPY  = (float) v.getAnimatedValue(); });
        rx.addUpdateListener(v -> { mRightPX = (float) v.getAnimatedValue(); });
        ry.addUpdateListener(v -> { mRightPY = (float) v.getAnimatedValue(); invalidate(); });

        AnimatorSet transition = new AnimatorSet();
        transition.playTogether(lx, ly, rx, ry);

        long holdMs = 600 + mRng.nextInt(1200);

        transition.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator anim) {
                if (!mRunning) return;
                mLeftPX  = targetLX; mLeftPY  = targetLY;
                mRightPX = targetRX; mRightPY = targetRY;
                invalidate();
                postDelayed(() -> { if (mRunning) scheduleNextPose(); }, holdMs);
            }
        });

        mCurrentAnim = transition;
        transition.start();
    }

    @Override protected void onAttachedToWindow()  { super.onAttachedToWindow();  startAnimation(); }
    @Override protected void onDetachedFromWindow() { stopAnimation(); super.onDetachedFromWindow(); }
}
