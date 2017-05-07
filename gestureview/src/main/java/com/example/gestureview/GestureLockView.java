package com.example.gestureview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Administrator on 2017-5-6.
 */

public class GestureLockView extends View {

    public static final int FINGER_ON = 0;
    public static final int FINGER_UP = 1;
    public static final int NO_FINGER = 2;

    private Paint mInnerPaint;
    private Paint mOuterPaint;

    private int mNoFingerInnerColor = Color.RED;
    private int mNoFingerOuterColor = Color.GRAY;
    private int mFingerOnColor = Color.BLUE;
    private int mFingerUpColor = Color.YELLOW;

    private int mCircleX;
    private int mCircleY;
    private int mInnerRadius;
    private int mOuterRadius;
    private boolean shouldDrawArrow;
    private int mArrowDegree;

    public void setShouldDrawArrow(boolean shouldDrawArrow) {
        this.shouldDrawArrow = shouldDrawArrow;
    }

    public void setArrowDegree(int degree) {
        this.mArrowDegree = degree;
    }

    public int getInnerRadius() {
        return mInnerRadius;
    }

    @IntDef(value = {FINGER_ON, FINGER_UP, NO_FINGER})
    public @interface STATE {
    }

    public GestureLockView(Context context) {
        this(context, null);
    }

    public GestureLockView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureLockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GestureLockView(Context context, @ColorInt int noFingerInnerColor, @ColorInt int
            noFingerOuterColor, @ColorInt int fingerOnColor, @ColorInt int fingerUpColor) {
        this(context);
        mNoFingerOuterColor = noFingerOuterColor;
        mNoFingerInnerColor = noFingerInnerColor;
        mFingerOnColor = fingerOnColor;
        mFingerUpColor = fingerUpColor;
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mCircleX = getWidth() / 2;
            mCircleY = getHeight() / 2;
            mInnerRadius = (int) (getWidth() * 1.0f / 8);
            mOuterRadius = (int) (getWidth() * 0.8f / 2);
            mOuterPaint.setStrokeWidth(mInnerRadius / 6);
        }
    }

    private void init() {
        mInnerPaint = new Paint();
        mInnerPaint.setStyle(Paint.Style.FILL);
        mInnerPaint.setColor(mNoFingerInnerColor);

        mOuterPaint = new Paint();
        mOuterPaint.setStyle(Paint.Style.FILL);
        mOuterPaint.setColor(mNoFingerOuterColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //outer
        canvas.drawCircle(mCircleX, mCircleY, mOuterRadius, mOuterPaint);
        //inner
        canvas.drawCircle(mCircleX, mCircleY, mInnerRadius, mInnerPaint);
        if (shouldDrawArrow) {
            drawArrow(canvas, mArrowDegree);
        }
    }

    public void setStatus(@STATE int state) {
        if (state == FINGER_ON) {
            mInnerPaint.setColor(mFingerOnColor);
            mOuterPaint.setStyle(Paint.Style.STROKE);
            mOuterPaint.setColor(mFingerOnColor);
        } else if (state == FINGER_UP) {
            mInnerPaint.setColor(mFingerUpColor);
            mOuterPaint.setColor(mFingerUpColor);
            mOuterPaint.setStyle(Paint.Style.STROKE);
        } else {
            mInnerPaint.setColor(mNoFingerInnerColor);
            mOuterPaint.setColor(mNoFingerOuterColor);
            mOuterPaint.setStyle(Paint.Style.FILL);
        }
        postInvalidate();
    }

    public void drawArrow(Canvas canvas, int degree) {
        canvas.save();
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setColor(mFingerUpColor);

        Path path = new Path();
        path.moveTo(mCircleX, mCircleY - mOuterRadius * 0.8f);
        path.lineTo(mCircleX - mOuterRadius / 2, mCircleY - mOuterRadius / 2);
        path.lineTo(mCircleX + mOuterRadius / 2, mCircleY - mOuterRadius / 2);
        path.close();
        canvas.rotate(degree, mCircleX, mCircleY);
        canvas.drawPath(path, p);
        canvas.restore();
    }
}
