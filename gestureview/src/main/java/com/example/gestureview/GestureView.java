package com.example.gestureview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017-5-6.
 */

public class GestureView extends ViewGroup {

    private int mFingerOnColor;
    private int mFingerUpColor;
    private int mNoFingerOuterColor;
    private int mNoFingerInnerColor;

    /**
     * 正确路径
     */
    private int[] mCorrectPath = {0, 1, 2, 4, 6, 7, 8};

    /**
     * 子view的个数
     */
    public int mViewCount;
    /**
     * 子View的内填充
     */
    public int mViewPadding;
    /**
     * 每行的个数
     */
    private int mLineCount;
    /**
     * 画笔，用于绘制已选路径
     */
    private Paint mLinePaint;
    /**
     * 已选路径
     */
    private Path mLinePath;
    /**
     * 上次滑到的点
     */
    private Point mLastPoint;
    /**
     * 用于指示路径
     */
    private Point mTarget;
    /**
     * 保存所有的子View
     */
    public GestureLockView[] mGestureLockViews;
    /**
     * 保存所有的子View的布局信息
     */
    public GesturePoint[] mGesturePoints;
    /**
     * 保存当前路径
     */
    public List<Integer> mPaths = new ArrayList<>();
    /**
     * 保存当前选中的点的指示三角的角度
     */
    public List<Integer> mDegrees = new ArrayList<>();
    /**
     * 内半径
     */
    private int mInnerRadius;

    /**
     * 自动重置View
     */
    private class MyRunnable implements Runnable {
        @Override
        public void run() {
            reset();
        }
    }

    private MyRunnable mMyRunnable = new MyRunnable();

    public GestureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GestureView);
        mNoFingerInnerColor = a.getColor(R.styleable.GestureView_no_finger_inner_color, Color.BLACK);
        mNoFingerOuterColor = a.getColor(R.styleable.GestureView_no_finger_outer_color, Color.GRAY);
        mFingerOnColor = a.getColor(R.styleable.GestureView_finger_on_color, getResources().getColor(R.color.colorPrimary));
        mFingerUpColor = a.getColor(R.styleable.GestureView_finger_up_color, Color.RED);
        mLineCount = a.getInt(R.styleable.GestureView_view_count_per_line, 3);
        mViewCount = mLineCount * mLineCount;
        mViewPadding = a.getDimensionPixelSize(R.styleable.GestureView_view_padding, 0);
        a.recycle();

        mGestureLockViews = new GestureLockView[mViewCount];
        mGesturePoints = new GesturePoint[mViewCount];
        for (int i = 0; i < mViewCount; i++) {
            mGesturePoints[i] = new GesturePoint();
        }
        init();
    }

    private void init() {
        mTarget = new Point();
        mLinePath = new Path();
        mLinePaint = new Paint();
        mLinePaint.setColor(mFingerOnColor);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setAlpha((int) (0.5 * 255));

        for (int i = 0; i < mViewCount; i++) {
            GestureLockView gestureLockView = new GestureLockView(getContext(), mNoFingerInnerColor, mNoFingerOuterColor, mFingerOnColor, mFingerUpColor);
            mGestureLockViews[i] = gestureLockView;
            addView(gestureLockView);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int lineCount = (int) Math.sqrt(mViewCount);
        int childWidth = (getMeasuredWidth() - 2 * mViewPadding * lineCount) / lineCount;
        measureChildren(childWidth, childWidth);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            int lineCount = (int) Math.sqrt(mViewCount);
            int childWidthSpace = getWidth() / lineCount;
            int childHeightSpace = getHeight() / lineCount;
            int childWidth = (getMeasuredWidth() - 2 * mViewPadding * lineCount) / lineCount;
            int childHeight = (getMeasuredHeight() - 2 * mViewPadding * lineCount) / lineCount;
            for (int i = 0; i < mViewCount; i++) {
                int rowIndex = i % lineCount;
                int columnIndex = i / lineCount;
                int left = (childWidthSpace * rowIndex) + mViewPadding;
                int right = left + childWidth;
                int top = (childHeightSpace * columnIndex) + mViewPadding;
                int bottom = top + childHeight;
                mGestureLockViews[i].layout(left, top, right, bottom);

                mInnerRadius = mGestureLockViews[0].getInnerRadius();
                mLinePaint.setStrokeWidth(mInnerRadius * 3);

                int circleX = (int) (left + childWidth * 1.0f / 2);
                int circleY = (int) (top + 1.0f * childHeight / 2);
                mGesturePoints[i].setLeft(circleX - mInnerRadius * 3);
                mGesturePoints[i].setTop(circleY - mInnerRadius * 3);
                mGesturePoints[i].setRight(circleX + mInnerRadius * 3);
                mGesturePoints[i].setBottom(circleY + mInnerRadius * 3);
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        //绘制指示路径
        if (mLastPoint != null) {
            canvas.drawLine(mLastPoint.x, mLastPoint.y, mTarget.x, mTarget.y, mLinePaint);
        }
        //绘制选中路径
        canvas.drawPath(mLinePath, mLinePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //取消自动重置
                getHandler().removeCallbacks(mMyRunnable);
                //手动重置
                reset();
            case MotionEvent.ACTION_MOVE:
                GestureLockView gestureLockView = getChildByPosition(x, y);
                if (gestureLockView != null) {
                    int pos = getChildPosition(gestureLockView);
                    if (pos != -1) {
                        if (!mPaths.contains(pos)) {
                            mPaths.add(pos);
                            Point point = getChildPoint(pos);
                            if (mPaths.size() == 1) {
                                mLinePath.moveTo(point.x, point.y);
                            } else {
                                mLinePath.lineTo(point.x, point.y);
                            }
                            mLastPoint = new Point(point.x, point.y);
                            gestureLockView.setStatus(GestureLockView.FINGER_ON);
                        }
                    }
                }
                //更新指示路径
                mTarget.x = (int) x;
                mTarget.y = (int) y;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (mPaths.size() > 0) {
                    getDegrees();
                    for (int i = 0; i < mPaths.size() - 1; i++) {
                        int index = mPaths.get(i);
                        mGestureLockViews[index].setStatus(GestureLockView.FINGER_UP);
                        mGestureLockViews[index].setShouldDrawArrow(true);
                        mGestureLockViews[index].setArrowDegree(mDegrees.get(i));
                    }
                    mGestureLockViews[mPaths.get(mPaths.size() - 1)].setStatus(GestureLockView.FINGER_UP);

                    mLinePaint.setColor(mFingerUpColor);
                    mLinePaint.setAlpha((int) (0.3f * 255));
                    mLastPoint = null;
                    postInvalidate();
                    checkAnswer();
                }
                break;
        }
        return true;
    }

    /**
     *判断路径
     */
    private void checkAnswer() {
        if (mCorrectPath.length != mPaths.size()) {
            getHandler().postDelayed(mMyRunnable,2000);
            if (mAnswerListener != null) {
                mAnswerListener.answerWrong();
            }
            return;
        }
        for (int i = 0; i < mCorrectPath.length; i++) {
            if (mCorrectPath[i] != mPaths.get(i)) {
                getHandler().postDelayed(mMyRunnable,2000);
                if (mAnswerListener != null) {
                    mAnswerListener.answerWrong();
                }
                return;
            }
        }
        if (mAnswerListener != null) {
            mAnswerListener.answerRight();
        }
    }

    /**
     * 根据当前的xy获取所属的View
     *
     * @param x
     * @param y
     * @return
     */
    public GestureLockView getChildByPosition(float x, float y) {
        for (int index = 0; index < mViewCount; index++) {
            if (x > mGesturePoints[index].getLeft() && x < mGesturePoints[index].getRight()
                    && y > mGesturePoints[index].getTop() && y < mGesturePoints[index].getBottom()) {
                return mGestureLockViews[index];
            }
        }
        return null;
    }

    /**
     * 获取子View所在的下标
     *
     * @param gestureLockView
     * @return
     */
    public int getChildPosition(GestureLockView gestureLockView) {
        for (int index = 0; index < mViewCount; index++) {
            GestureLockView mGestureLockView1 = mGestureLockViews[index];
            if (mGestureLockView1 == gestureLockView) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 获取滑过的点的角度
     */
    public void getDegrees() {
        if (mPaths.size() < 2) {
            return;
        }
        for (int i = 0; i < mPaths.size() - 1; i++) {
            int firstIndex = mPaths.get(i);
            int nextIndex = mPaths.get(i + 1);
            GesturePoint p1 = mGesturePoints[firstIndex];
            GesturePoint p2 = mGesturePoints[nextIndex];
            int x1 = p1.getCenterX();
            int x2 = p2.getCenterX();
            int y1 = p1.getCenterY();
            int y2 = p2.getCenterY();
            mDegrees.add(getDegree(x1, y1, x2, y2));
        }
    }

    /**
     * 获取两点间的角度
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public int getDegree(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int angle = (int) Math.toDegrees(Math.atan2(dy, dx)) + 90;
        return angle;
    }

    //重置
    private void reset() {
        //重置已经设置的GestureLockView
        for (int index : mPaths) {
            mGestureLockViews[index].setStatus(GestureLockView.NO_FINGER);
            mGestureLockViews[index].setShouldDrawArrow(false);
            mGestureLockViews[index].setArrowDegree(0);
        }
        //重置路径
        mPaths.clear();
        //重置路径
        mLinePath.reset();
        //重置角度
        mDegrees.clear();
        //重置描绘路径的画笔
        mLinePaint.setColor(mFingerOnColor);
        mLinePaint.setAlpha((int) (0.5f * 255));
        postInvalidate();
    }

    /**
     * 获取子View的中心点
     *
     * @param index
     * @return
     */
    public Point getChildPoint(int index) {
        int childWidthSpace = getWidth() / mLineCount;
        int childHeightSpace = getHeight() / mLineCount;
        int childWidth = (getMeasuredWidth() - 2 * mViewPadding * mLineCount) / mLineCount;
        int childHeight = (getMeasuredHeight() - 2 * mViewPadding * mLineCount) / mLineCount;
        int rowIndex = index % mLineCount;
        int columnIndex = index / mLineCount;
        int left = (childWidthSpace * rowIndex) + mViewPadding;
        int right = left + childWidth;
        int top = (childHeightSpace * columnIndex) + mViewPadding;
        int bottom = top + childHeight;
        int circleX = (int) (left + childWidth * 1.0f / 2);
        int circleY = (int) (top + 1.0f * childHeight / 2);
        Point point = new Point();
        point.x = circleX;
        point.y = circleY;
        return point;
    }

    /**
     * 保存子View的位置
     */
    private class GesturePoint {
        int left;
        int right;
        int top;
        int bottom;

        public int getLeft() {
            return left;
        }

        public void setLeft(int left) {
            this.left = left;
        }

        public int getRight() {
            return right;
        }

        public void setRight(int right) {
            this.right = right;
        }

        public int getTop() {
            return top;
        }

        public void setTop(int top) {
            this.top = top;
        }

        public int getBottom() {
            return bottom;
        }

        public void setBottom(int bottom) {
            this.bottom = bottom;
        }

        public int getCenterX() {
            return (right + left) / 2;
        }

        public int getCenterY() {
            return (bottom + top) / 2;
        }
    }

    private onAnswerListener mAnswerListener;

    public void setAnswerListener(onAnswerListener onAnswerListener) {
        mAnswerListener = onAnswerListener;
    }

    //回调接口
    interface onAnswerListener {
        void answerRight();

        void answerWrong();
    }

    /**
     * 设置正确路径
     * @param correctPath
     */
    public void setCorrectPath(int[] correctPath) {
        mCorrectPath = correctPath;
    }
}
