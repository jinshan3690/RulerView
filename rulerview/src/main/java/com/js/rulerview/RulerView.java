package com.js.rulerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.IntegerRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import java.math.BigDecimal;


/**
 * Created by yjwfn on 15-5-27.
 */
public class RulerView extends View {

    final String TAG = RulerView.class.getSimpleName();

    private int mSelectedPosition;

    private int mBeginRange;
    private int[] mBeginRanges;

    private int mEndRange;
    private int[] mEndRanges;

    private int[] mRangeColors;

    private float mPrecision;

    private int mInnerWidth;

    private int mIndicatePadding;

    private Paint mIndicatePaint;

    private Paint mTextPaint;

    private int mIndicateWidth;

    private float mIndicateScale;

    private int mLastMotionX;

    private boolean mIsDragged;

    private boolean mIsAutoAlign = true;

    private boolean mIsWithText = true;

    //view
    private int mTextColor;

    private float mTextSize;

    private int mIndicateColor;
    private int mSelectedColor;

    private OnScaleListener mListener;

    private int mGravity;

    private Rect mIndicateLoc;


    //滚动相关参数
    private OverScroller mOverScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;


    public RulerView(Context context) {
        this(context, null);
    }

    public RulerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RulerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);


        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RulerView);
        mIndicateColor = ta.getColor(R.styleable.RulerView_indicateColor, Color.BLACK);
        mSelectedColor = ta.getColor(R.styleable.RulerView_selectedColor, Color.RED);
        mTextColor = ta.getColor(R.styleable.RulerView_textColor, Color.GRAY);
        mTextSize = ta.getDimension(R.styleable.RulerView_textSize, 18);
        mSelectedPosition = ta.getInt(R.styleable.RulerView_selectedPosition, 0);
        mBeginRange = ta.getInt(R.styleable.RulerView_begin, 0);
        mEndRange = ta.getInt(R.styleable.RulerView_end, 100);
        mPrecision = ta.getFloat(R.styleable.RulerView_precision, 1);
        mBeginRanges = convertToInt(ta.getString(R.styleable.RulerView_beginRanges));
        mEndRanges = convertToInt(ta.getString(R.styleable.RulerView_endRanges));
        mRangeColors = convertToColor(ta.getString(R.styleable.RulerView_rangeColors));
        mIndicateWidth = (int) ta.getDimension(R.styleable.RulerView_indicateWidth, 5);
        mIndicatePadding = (int) ta.getDimension(R.styleable.RulerView_indicatePadding, 15);
        ta.recycle();

        int[] indices = new int[]{android.R.attr.gravity};
        ta = context.obtainStyledAttributes(attrs, indices);
        mGravity = ta.getInt(ta.getIndex(0), Gravity.BOTTOM);
        ta.recycle();

        mIndicateScale = 0.7f;

        initValue();
        post(new Runnable() {
            @Override
            public void run() {
                smoothScrollTo(mSelectedPosition);
            }
        });
    }

    private int[] convertToInt(String string) {
        int[] val = null;
        if (string != null && string.trim().length() != 0){
            String[] strs = string.split(",");
            val = new int[strs.length];
            for (int i = 0; i < strs.length; i++) {
                val[i] = Integer.valueOf(strs[i]);
            }
        }
        return val;
    }

    private int[] convertToColor(String string) {
        int[] val = null;
        if (string != null && string.trim().length() != 0){
            String[] strs = string.split(",");
            val = new int[strs.length];
            for (int i = 0; i < strs.length; i++) {
                val[i] = Color.parseColor(strs[i]);
            }
        }
        return val;
    }

    private void initValue() {

        mOverScroller = new OverScroller(getContext());
        setOverScrollMode(OVER_SCROLL_ALWAYS);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();


        mIndicatePaint = new Paint();
        mIndicatePaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(mTextSize);
        mInnerWidth = (mEndRange - mBeginRange) * getIndicateWidth();

        mIndicateLoc = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int count = canvas.save();
        for (int value = mBeginRange, position = 0; value <= mEndRange; value++, position++) {
            drawIndicate(canvas, position, value);
            if (mIsWithText)
                drawText(canvas, position, String.valueOf(new BigDecimal(value).multiply(new BigDecimal(Float.toString(mPrecision))).floatValue()));
        }
        canvas.restoreToCount(count);
    }

    private void drawIndicate(Canvas canvas, int position, int value) {
        computeIndicateLoc(mIndicateLoc, position);
        int left = mIndicateLoc.left + mIndicatePadding;
        int right = mIndicateLoc.right - mIndicatePadding;
        int top = mIndicateLoc.top;
        int bottom = mIndicateLoc.bottom;

        if (position % 5 != 0) {
            int indicateHeight = bottom - top;
            if (isAlignTop()) {
                bottom = (int) (top + indicateHeight * mIndicateScale);
            } else {
                top = (int) (bottom - indicateHeight * mIndicateScale);
            }
        }

        if (position == mSelectedPosition)
            mIndicatePaint.setColor(mSelectedColor);
        else {
            mIndicatePaint.setColor(mIndicateColor);
            if(mBeginRanges.length == mEndRanges.length)
                for (int i = 0; i < mBeginRanges.length; i++) {
                    if (mBeginRanges[i] <= value && mEndRanges[i] >= value){
                        mIndicatePaint.setColor(mRangeColors[i]);
                        break;
                    }
                }
        }
        canvas.drawRect(left, top, right, bottom, mIndicatePaint);
    }

    private void drawText(Canvas canvas, int position, String text) {
        if (position % 5 != 0)
            return;

        computeIndicateLoc(mIndicateLoc, position);
        float textHeight = computeTextHeight();

        if (position == mSelectedPosition)
            mTextPaint.setColor(mSelectedColor);
        else
            mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        int x = (mIndicateLoc.left + mIndicateLoc.right) / 2;
        float y = mIndicateLoc.bottom + textHeight;

        if (!isAlignTop()) {
            y = mIndicateLoc.top;
            mTextPaint.getTextBounds(text, 0, text.length(), mIndicateLoc);
            y += mIndicateLoc.top / 2;  //增加一些偏移
        }

        canvas.drawText(text, x, y, mTextPaint);
    }

    /**
     * 计算indicate的位置
     *
     * @param outRect
     * @param position
     */
    private void computeIndicateLoc(Rect outRect, int position) {
        if (outRect == null)
            return;

        int height = getHeight();
        int indicate = getIndicateWidth();

        int left = (indicate * position);
        int right = left + indicate;
        int top = getPaddingTop();
        int bottom = height - getPaddingBottom();

        if (mIsWithText) {
            float textHeight = computeTextHeight();
            if (isAlignTop())
                bottom -= textHeight;
            else
                top += textHeight;
        }

        int offsets = getStartOffsets();
        left += offsets;
        right += offsets;
        outRect.set(left, top, right, bottom);
    }

    /**
     * 开始偏移，如果要包含文字的话才需要偏移。
     *
     * @return
     */
    private int getStartOffsets() {
        if (mIsWithText) {
            String text = String.valueOf(mBeginRange);
            int textWidth = (int) mTextPaint.measureText(text, 0, text.length());
            return textWidth / 2;
        }
        return 0;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();

        mVelocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //
                if (mIsDragged = !mOverScroller.isFinished()) {
                    if (getParent() != null)
                        getParent().requestDisallowInterceptTouchEvent(true);
                }

                if (!mOverScroller.isFinished())
                    mOverScroller.abortAnimation();

                mLastMotionX = (int) event.getX();

                return true;

            case MotionEvent.ACTION_MOVE:

                int curX = (int) event.getX();
                int deltaX = mLastMotionX - curX;

                if (!mIsDragged && Math.abs(deltaX) > mTouchSlop) {
                    if (getParent() != null)
                        getParent().requestDisallowInterceptTouchEvent(true);

                    mIsDragged = true;

                    if (deltaX > 0) {
                        deltaX -= mTouchSlop;
                    } else {
                        deltaX += mTouchSlop;
                    }
                }

                if (mIsDragged) {
                    mLastMotionX = curX;

                    if (getScrollX() <= 0 || getScrollX() >= getMaximumScroll())
                        deltaX *= 0.7;


                    if (overScrollBy(deltaX, 0, getScrollX(), getScrollY(), getMaximumScroll(), 0, getWidth(), 0, true)) {
                        mVelocityTracker.clear();
                    }

                }

                break;
            case MotionEvent.ACTION_UP: {
                if (mIsDragged) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) mVelocityTracker.getXVelocity();

                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        fling(-initialVelocity);
                    } else {
                        //alignCenter();
                        sprintBack();
                    }
                }

                mIsDragged = false;
                recycleVelocityTracker();
                break;
            }
            case MotionEvent.ACTION_CANCEL: {

                if (mIsDragged && mOverScroller.isFinished()) {
                    sprintBack();
                }

                mIsDragged = false;

                recycleVelocityTracker();
                break;
            }
        }

        return true;
    }


    private void refreshValues() {
        mInnerWidth = (mEndRange - mBeginRange) * getIndicateWidth();
        invalidateView();

    }

    private int getIndicateWidth() {
        return mIndicateWidth + mIndicatePadding + mIndicatePadding;
    }

    /**
     * 获取最小滚动值。
     *
     * @return
     */
    private int getMinimumScroll() {
        return -(getWidth() - getIndicateWidth()) / 2 + getStartOffsets();
    }

    /**
     * 获取最大滚动值。
     *
     * @return
     */
    private int getMaximumScroll() {
        return mInnerWidth + getMinimumScroll();
    }

    /**
     * 调整indicate，使其居中。
     */
    private void adjustIndicate() {
        if (!mOverScroller.isFinished())
            mOverScroller.abortAnimation();

        int position = computeSelectedPosition();
        int scrollX = getScrollByPosition(position);
        scrollX -= getScrollX();

        if (scrollX != 0) {
            mOverScroller.startScroll(getScrollX(), getScrollY(), scrollX, 0);
            invalidateView();
        }
    }

    public void fling(int velocityX) {
        mOverScroller.fling(getScrollX(), getScrollY(), velocityX, 0, getMinimumScroll(), getMaximumScroll(), 0, 0, getWidth() / 2, 0);
        invalidateView();
    }

    public void sprintBack() {
        mOverScroller.springBack(getScrollX(), getScrollY(), getMinimumScroll(), getMaximumScroll(), 0, 0);
        invalidateView();
    }


    public void setOnScaleListener(OnScaleListener listener) {
        if (listener != null) {
            mListener = listener;
        }
    }

    /**
     * 获取position的绝对滚动位置。
     *
     * @param position
     * @return
     */
    private int getScrollByPosition(int position) {
        computeIndicateLoc(mIndicateLoc, position);
        int scrollX = mIndicateLoc.left - getStartOffsets() + getMinimumScroll();
        return scrollX;
    }

    /**
     * 计算当前已选择的位置
     *
     * @return
     */
    public int computeSelectedPosition() {
        int centerX = getScrollX() - getMinimumScroll() + getIndicateWidth() / 2;
        centerX = Math.max(0, Math.min(mInnerWidth, centerX));
        int position = centerX / getIndicateWidth();
        return position;
    }

    public void smoothScrollTo(int position) {
        if (position < 0 || mBeginRange + position > mEndRange)
            return;

        if (!mOverScroller.isFinished())
            mOverScroller.abortAnimation();

        int scrollX = getScrollByPosition(position);
        mOverScroller.startScroll(getScrollX(), getScrollY(), scrollX - getScrollX(), 0);
        invalidateView();
    }

    public void smoothScrollToValue(float value) {
        int position = (int) (new BigDecimal(value).divide(new BigDecimal(mPrecision)).floatValue() - mBeginRange);
        smoothScrollTo(position);
    }


    private void onScaleChanged(int scale) {
        if (mListener != null && mSelectedPosition != scale) {
            mListener.onScaleChanged(new BigDecimal(scale).multiply(new BigDecimal(Float.toString(mPrecision))).floatValue());
        }
        mSelectedPosition = scale;
    }


    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (!mOverScroller.isFinished()) {
            final int oldX = getScrollX();
            final int oldY = getScrollY();
            setScrollX(scrollX);
            onScrollChanged(scrollX, scrollY, oldX, oldY);
            if (clampedX) {
                //sprintBack();
            }
        } else {
            super.scrollTo(scrollX, scrollY);
        }

        if (mListener != null) {
            int position = computeSelectedPosition();
            onScaleChanged(position + mBeginRange);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);


    }

    private float computeTextHeight() {
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        float textHeight = fontMetrics.descent - fontMetrics.ascent;
        return textHeight;
    }


    private boolean isAlignTop() {
        return (mGravity & Gravity.TOP) == Gravity.TOP;
    }


    public void setGravity(int gravity) {
        this.mGravity = gravity;
        invalidateView();
    }

    @Override
    public void computeScroll() {
        if (mOverScroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mOverScroller.getCurrX();
            int y = mOverScroller.getCurrY();
            overScrollBy(x - oldX, y - oldY, oldX, oldY, getMaximumScroll(), 0, getWidth(), 0, false);
            invalidateView();
        } else if (!mIsDragged && mIsAutoAlign) {
            adjustIndicate();
        }

    }

    @Override
    protected int computeHorizontalScrollRange() {
        return getMaximumScroll();
    }


    public void invalidateView() {
        if (Build.VERSION.SDK_INT >= 16) {
            postInvalidateOnAnimation();
        } else
            invalidate();
    }


    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public interface OnScaleListener {
        void onScaleChanged(float scale);

    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public float getSelectedValue() {
        return new BigDecimal(mSelectedPosition).multiply(new BigDecimal(Float.toString(mPrecision))).floatValue();
    }

    public void setIndicateWidth(@IntegerRes int indicateWidth) {
        this.mIndicateWidth = indicateWidth;
        refreshValues();
    }

    public void setIndicatePadding(@IntegerRes int indicatePadding) {
        this.mIndicatePadding = indicatePadding;
        refreshValues();
    }

    public void setWithText(boolean withText) {
        this.mIsWithText = withText;
        refreshValues();
    }

    public void setAutoAlign(boolean autoAlign) {
        this.mIsAutoAlign = autoAlign;
        refreshValues();
    }

    public boolean isWithText() {
        return mIsWithText;
    }

    public boolean isAutoAlign() {
        return mIsAutoAlign;
    }
}
