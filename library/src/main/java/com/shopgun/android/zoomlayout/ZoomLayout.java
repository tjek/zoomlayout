package com.shopgun.android.zoomlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.shopgun.android.utils.NumberUtils;
import com.shopgun.android.utils.log.L;

@SuppressWarnings("unused")
public class ZoomLayout extends FrameLayout {

    public static final String TAG = ZoomLayout.class.getSimpleName();

    public boolean DEBUG = false;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private GestureListener mGestureListener;

    private Matrix mScaleMatrix = new Matrix();
    private Matrix mScaleMatrixInverse = new Matrix();
    private Matrix mTranslateMatrix = new Matrix();
    private Matrix mTranslateMatrixInverse = new Matrix();
    // helper array to save heap
    private float[] mMatrixValues = new float[9];

    private float mFocusY;
    private float mFocusX;

    // Helper array to save heap
    private float[] mArray = new float[6];

    // for set scale
    private boolean mAllowOverScale = true;

    RectF mDrawRect = new RectF();
    RectF mViewPortRect = new RectF();

    private FlingRunnable mFlingRunnable;
    private AnimatedZoomRunnable mAnimatedZoomRunnable;
    private Interpolator mAnimationInterpolator = new AccelerateDecelerateInterpolator();
    private int mZoomDuration = 200;

    // allow parent views to intercept any touch events that we do not consume
    boolean mAllowParentInterceptOnEdge = true;
    // allow parent views to intercept any touch events that we do not consume even if we are in a scaled state
    boolean mAllowParentInterceptOnScaled = false;
    // minimum scale of the content
    private float mMinScale = 1.0f;
    // maximum scale of the content
    private float mMaxScale = 4.0f;

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

        private int mLeft, mTop, mRight, mBottom;

        @Override
        public void onGlobalLayout() {
            // SDK v11 supports OnLayoutChangeListener, but we don't have that, so we'll do this
            if (getLeft() != mLeft || getTop() != mTop || getRight() != mRight || getBottom() != mBottom) {
                matrixUpdated();
                mLeft = getLeft();
                mTop = getTop();
                mRight = getRight();
                mBottom = getBottom();
            }
        }
    };

    // Listeners
    private ZoomDispatcher mZoomDispatcher = new ZoomDispatcher();
    private PanDispatcher mPanDispatcher = new PanDispatcher();
    private OnTapListener mTapListener;
    private OnDoubleTapListener mDoubleTapListener;
    private OnLongTapListener mLongTapListener;

    public ZoomLayout(Context context) {
        super(context);
        init(context, null);
    }

    public ZoomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ZoomLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(21)
    public ZoomLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, mGestureListener = new GestureListener());
        mTranslateMatrix.setTranslate(0, 0);
        mScaleMatrix.setScale(1, 1);
        getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(-getPosX(), -getPosY());
        float scale = getScale();
        canvas.scale(scale, scale, mFocusX, mFocusY);
        super.dispatchDraw(canvas);
        if (DEBUG) {
            ZoomUtils.debugDraw(canvas, getContext(), getPosX(), getPosY(), mFocusX, mFocusY, getMatrixValue(mScaleMatrixInverse, Matrix.MSCALE_X));
        }
        canvas.restore();
    }

    /**
     * Although the docs say that you shouldn't override this, I decided to do
     * so because it offers me an easy way to change the invalidated area to my
     * likening.
     */
    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        scaledPointsToScreenPoints(dirty);
        float scale = getScale();
        location[0] *= scale;
        location[1] *= scale;
        return super.invalidateChildInParent(location, dirty);
    }

    private void scaledPointsToScreenPoints(Rect rect) {
        ZoomUtils.setArray(mArray, rect);
        mArray = scaledPointsToScreenPoints(mArray);
        ZoomUtils.setRect(rect, mArray);
    }

    private void scaledPointsToScreenPoints(RectF rect) {
        ZoomUtils.setArray(mArray, rect);
        mArray = scaledPointsToScreenPoints(mArray);
        ZoomUtils.setRect(rect, mArray);
    }

    private float[] scaledPointsToScreenPoints(float[] a) {
        mScaleMatrix.mapPoints(a);
        mTranslateMatrix.mapPoints(a);
        return a;
    }

    private float[] screenPointsToScaledPoints(float[] a){
        mTranslateMatrixInverse.mapPoints(a);
        mScaleMatrixInverse.mapPoints(a);
        return a;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mArray[0] = ev.getX();
        mArray[1] = ev.getY();
        screenPointsToScaledPoints(mArray);
        ev.setLocation(mArray[0], mArray[1]);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mArray[0] = ev.getX();
        mArray[1] = ev.getY();
        scaledPointsToScreenPoints(mArray);
        ev.setLocation(mArray[0], mArray[1]);

        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_DOWN) {
            log("### MotionEvent.ACTION_DOWN ###");
        }
        boolean consumed = mScaleDetector.onTouchEvent(ev);
        consumed = mGestureDetector.onTouchEvent(ev) || consumed;
        consumed = mGestureListener.onUp(ev) || consumed;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                requestDisallowInterceptTouchEvent(true);
                cancelFling();
                cancelZoom();
                break;

            case MotionEvent.ACTION_UP: {

                mAnimatedZoomRunnable = new AnimatedZoomRunnable();

                float newScale = NumberUtils.clamp(mMinScale, getScale(), mMaxScale);

                // Find current bounds before applying scale to do the rest of the calculations

                if (mViewPortRect.contains(mDrawRect) ||
                        (mViewPortRect.height() > mDrawRect.height() &&
                                mViewPortRect.width() > mDrawRect.width())) {

                    log("ViewRect can contain DrawRect");

                    // The ViewRect does (or can) contain DrawRect
                    // We need to center the DrawRect and ensure the scale bounds

                    mAnimatedZoomRunnable.scaleIfNeeded(getScale(), newScale, mFocusX, mFocusY);
                    mAnimatedZoomRunnable.translateIfNeeded(getPosX(), getPosY(), 0, 0);

                } else if (!mDrawRect.contains(mViewPortRect)) {

                    log("ViewRect in on an edge of DrawRect");

                    mAnimatedZoomRunnable.scaleIfNeeded(getScale(), newScale, mFocusX, mFocusY);
                    // TODO handle this case nicely - not just re-center
                    mAnimatedZoomRunnable.translateIfNeeded(getPosX(), getPosY(), 0, 0);

                    if (mViewPortRect.left > mDrawRect.left) {
                        log("left");
                    }

                    if (mViewPortRect.right > mDrawRect.right) {
                        log("right");
                    }

                    if (mViewPortRect.top < mDrawRect.top) {
                        log("top");
                    }

                    if (mViewPortRect.bottom < mDrawRect.bottom) {
                        log("bottom");
                    }

                } else {

                    log("ViewRect is inside DrawRect");

                    // The ViewPort is inside the DrawRect - no problem
                    mAnimatedZoomRunnable.scaleIfNeeded(getScale(), newScale, mFocusX, mFocusY);

                }

                if (mAnimatedZoomRunnable.scale() || mAnimatedZoomRunnable.translate()) {
                    ViewCompat.postOnAnimation(ZoomLayout.this, mAnimatedZoomRunnable);
                    consumed = true;
                } else {
                    cancelZoom();
                }

                log("### MotionEvent.ACTION_UP ###");

                break;
            }

        }

        return consumed;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private boolean mScrolling = false;

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mTapListener != null) {
                if (mDrawRect.contains(e.getX(), e.getY())) {
                    float percentX = (e.getX()-mDrawRect.left)/mDrawRect.width();
                    float percentY = (e.getY()-mDrawRect.top)/mDrawRect.height();
                    return mTapListener.onContentTap(ZoomLayout.this, percentX, percentY);
                } else {
                    return mTapListener.onViewTap(ZoomLayout.this);
                }
            }
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if ((e.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                if (mDoubleTapListener != null) {
                    if (mDrawRect.contains(e.getX(), e.getY())) {
                        float percentX = (e.getX()-mDrawRect.left)/mDrawRect.width();
                        float percentY = (e.getY()-mDrawRect.top)/mDrawRect.height();
                        return mDoubleTapListener.onContentDoubleTap(ZoomLayout.this, percentX, percentY);
                    } else {
                        return mDoubleTapListener.onViewDoubleTap(ZoomLayout.this);
                    }
                }
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (!mScaleDetector.isInProgress()) {
                if (mLongTapListener != null) {
                    if (mDrawRect.contains(e.getX(), e.getY())) {
                        float percentX = (e.getX()-mDrawRect.left)/mDrawRect.width();
                        float percentY = (e.getY()-mDrawRect.top)/mDrawRect.height();
                        mLongTapListener.onContentLongTap(ZoomLayout.this, percentX, percentY);
                    } else {
                        mLongTapListener.onViewLongTap(ZoomLayout.this);
                    }
                }
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean consumed = false;
            if (e2.getPointerCount() == 1 && !mScaleDetector.isInProgress()) {
                // only drag if we have one pointer and aren't already scaling
                if (!mScrolling) {
                    mPanDispatcher.onPanBegin(ZoomLayout.this);
                    mScrolling = true;
                }
                consumed = moveBy(distanceX, distanceY);
                if (mAllowParentInterceptOnEdge && !consumed && (!isScaled() || mAllowParentInterceptOnScaled)) {
                    requestDisallowInterceptTouchEvent(false);
                }
            }
            return consumed;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mDrawRect.contains(mViewPortRect)) {
                float newScale = NumberUtils.clamp(mMinScale, getScale(), mMaxScale);
                if (NumberUtils.isEqual(newScale, getScale())) {
                    // only fling if no scale is needed - scale will happen on ACTION_UP
                    mFlingRunnable = new FlingRunnable(getContext());
                    mFlingRunnable.fling((int) velocityX, (int) velocityY);
                    ViewCompat.postOnAnimation(ZoomLayout.this, mFlingRunnable);
                    return true;
                }
            }
            return false;
        }

        public boolean onUp(MotionEvent e) {
            if (mScrolling) {
                mPanDispatcher.onPanEnd(ZoomLayout.this);
                mScrolling = false;
                return true;
            }
            return false;
        }

    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mArray[0] = detector.getFocusX();
            mArray[1] = detector.getFocusY();
            screenPointsToScaledPoints(mArray);
            // The first scale event translates the content, so we'll counter that translate
            float x1 = getMatrixValue(mScaleMatrix, Matrix.MTRANS_X);
            float y1 = getMatrixValue(mScaleMatrix, Matrix.MTRANS_Y);
            internalScale(getScale(), mArray[0], mArray[1]);
            float dX = getMatrixValue(mScaleMatrix, Matrix.MTRANS_X)-x1;
            float dY = getMatrixValue(mScaleMatrix, Matrix.MTRANS_Y)-y1;
            moveBy(dX, dY, false);
            mZoomDispatcher.onZoomBegin(ZoomLayout.this, getScale());
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = getScale() * detector.getScaleFactor();
            internalScale(scale, mFocusX, mFocusY);
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mZoomDispatcher.onZoomEnd(ZoomLayout.this, getScale());
        }
    }

    private void cancelFling() {
        if (mFlingRunnable != null) {
            mFlingRunnable.cancelFling();
            mFlingRunnable = null;
        }
    }

    private void cancelZoom() {
        if (mAnimatedZoomRunnable != null) {
            mAnimatedZoomRunnable.cancel();
            mAnimatedZoomRunnable = null;
        }
    }

    /**
     * The rectangle representing the location of the view inside the ZoomView. including scale and translations.
     */
    public RectF getDrawRect() {
        return new RectF(mDrawRect);
    }

    public boolean isAllowOverScale() {
        return mAllowOverScale;
    }

    public void setAllowOverScale(boolean allowOverScale) {
        mAllowOverScale = allowOverScale;
    }

    public boolean isAllowParentInterceptOnEdge() {
        return mAllowParentInterceptOnEdge;
    }

    public void setAllowParentInterceptOnEdge(boolean allowParentInterceptOnEdge) {
        mAllowParentInterceptOnEdge = allowParentInterceptOnEdge;
    }

    public boolean isAllowParentInterceptOnScaled() {
        return mAllowParentInterceptOnScaled;
    }

    public void setAllowParentInterceptOnScaled(boolean allowParentInterceptOnScaled) {
        mAllowParentInterceptOnScaled = allowParentInterceptOnScaled;
    }

    public int getZoomDuration() {
        return mZoomDuration;
    }

    public void setZoomDuration(int zoomDuration) {
        mZoomDuration = zoomDuration;
    }

    public float getMaxScale() {
        return mMaxScale;
    }

    public float getMinScale() {
        return mMinScale;
    }

    public void setMinScale(float minScale) {
        mMinScale = minScale;
    }

    public float getScale() {
        return getMatrixValue(mScaleMatrix, Matrix.MSCALE_X);
    }

    public void setScale(float scale) {
        setScale(scale, true);
    }

    public void setScale(float scale, boolean animate) {
        setScale(scale, mFocusX, mFocusY, animate);
    }

    public boolean isScaled() {
        return !NumberUtils.isEqual(getScale(), 1.0f, 0.05f);
    }

    public void setScale(float scale, float focusX, float focusY, boolean animate) {
        if (!mAllowOverScale) {
            scale = NumberUtils.clamp(mMinScale, scale, mMaxScale);
        }
        if (animate) {
            mAnimatedZoomRunnable = new AnimatedZoomRunnable();
            mAnimatedZoomRunnable.scale(getScale(), scale, focusX, focusY);
            ViewCompat.postOnAnimation(ZoomLayout.this, mAnimatedZoomRunnable);
        } else {
            internalScale(scale, focusX, focusY);
        }
    }

    public boolean moveBy(float dX, float dY) {
//        log(String.format(Locale.US, "moveBy[ %.2f, %.2f ]", dX, dY));
        return moveBy(dX, dY, true);
    }

    public boolean moveTo(float posX, float posY) {
        return moveTo(posX, posY, false);
    }

    private boolean moveBy(float dX, float dY, boolean clamp) {
//        log(String.format(Locale.US, "moveBy[ %.2f, %.2f ]", dX, dY));
        return moveTo(dX + getPosX(), dY + getPosY(), clamp);
    }

    private boolean moveTo(float posX, float posY, boolean clamp) {
        if (clamp) {
            RectF bounds = getTranslateBounds();
            if (bounds.isEmpty()) {
                return false;
            }
//        log(String.format(Locale.US, "move x[ %.1f -> %.1f ], y[ %.1f -> %.1f ], bounds: %s", getPosX(), posX, getPosY(), posY, bounds.toString()));
            posX = NumberUtils.clamp(bounds.left, posX, bounds.right);
            posY = NumberUtils.clamp(bounds.top, posY, bounds.bottom);
        }
        return internalMove(posX, posY);
    }

    /**
     * Get the bounds that x and y coordinates must stay within when translating the view
     * if we want to ensure that the ViewPort stays inside the DrawRect.
     * @return A rect
     */
    public RectF getTranslateBounds() {
        RectF r = new RectF();
        r.left = getMatrixValue(mScaleMatrix, Matrix.MTRANS_X);
        r.top = getMatrixValue(mScaleMatrix, Matrix.MTRANS_Y);
        r.right = (mDrawRect.width() - getWidth()) - Math.abs(r.left);
        r.bottom = (mDrawRect.height() - getHeight()) - Math.abs(r.top);
        return r;
    }

    private boolean internalMove(float posX, float posY) {
        if (!NumberUtils.isEqual(posX, getPosX()) ||
                !NumberUtils.isEqual(posY, getPosY())) {
            mTranslateMatrix.setTranslate(-posX, -posY);
            matrixUpdated();
            mPanDispatcher.onPan(ZoomLayout.this);
            invalidate();
            return true;
        }
        return false;
    }

    private void internalScale(float scale, float focusX, float focusY) {
        mFocusX = focusX;
        mFocusY = focusY;
        mScaleMatrix.setScale(scale, scale, mFocusX, mFocusY);
        matrixUpdated();
        mZoomDispatcher.onZoom(ZoomLayout.this, scale);
        invalidate();
        requestLayout();
    }

    /**
     * Update all variables that rely on the Matrix'es.
     */
    private void matrixUpdated() {
        // First inverse matrixes
        mScaleMatrix.invert(mScaleMatrixInverse);
        mTranslateMatrix.invert(mTranslateMatrixInverse);
        // Update Draw and View rects
        // first measure the DrawRect - Then set the coordinates accordingly
        final View child = getChildAt(0);
        if (child != null) {
            ZoomUtils.setRect(mDrawRect, child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            scaledPointsToScreenPoints(mDrawRect);
        }
        ZoomUtils.setRect(mViewPortRect, 0, 0, getWidth(), getHeight());
    }

    /**
     * Get the current x-translation
     * @return
     */
    public float getPosX() {
        return -getMatrixValue(mTranslateMatrix, Matrix.MTRANS_X);
    }

    /**
     * Get the current y-translation
     * @return
     */
    public float getPosY() {
        return -getMatrixValue(mTranslateMatrix, Matrix.MTRANS_Y);
    }

    /**
     * Read a specific value from a given matrix
     * @param matrix The Matrix to read a value from
     * @param value The value-position to read
     * @return The value at a given position
     */
    private float getMatrixValue(Matrix matrix, int value) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[value];
    }

    private class AnimatedZoomRunnable implements Runnable {

        boolean mPerformScale = false;
        boolean mPerformTranslate = false;
        boolean mCancelled = false;
        boolean mFinished = false;

        private long mStartTime;
        private float mZoomStart, mZoomEnd, mFocalX, mFocalY;
        private float mXStart, mYStart, mXEnd, mYEnd;

        AnimatedZoomRunnable() {
            mStartTime = System.currentTimeMillis();
        }

        AnimatedZoomRunnable scaleIfNeeded(float currentZoom, float targetZoom, float focalX, float focalY) {
            if (performScale(currentZoom, targetZoom, focalX, focalY)) {
                scale(currentZoom, targetZoom, focalX, focalY);
            }
            return this;
        }

        boolean performScale(float currentZoom, float targetZoom, float focalX, float focalY) {
            return !NumberUtils.isEqual(currentZoom, targetZoom) ||
                    !NumberUtils.isEqual(focalX, ZoomLayout.this.mFocusX) ||
                    !NumberUtils.isEqual(focalY, ZoomLayout.this.mFocusY);
        }

        AnimatedZoomRunnable scale(float currentZoom, float targetZoom, float focalX, float focalY) {
            mPerformScale = true;
            mFocalX = focalX;
            mFocalY = focalY;
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
            log(String.format("AnimatedZoomRunnable.Scale: %s -> %s", mZoomStart, mZoomEnd));
            mZoomDispatcher.onZoomBegin(ZoomLayout.this, getScale());
            return this;
        }

        boolean scale() {
            return mPerformScale;
        }

        AnimatedZoomRunnable translateIfNeeded(float currentX, float currentY, float targetX, float targetY) {
            if (performTranslate(currentX, currentY, targetX, targetY)) {
                translate(currentX, currentY, targetX, targetY);
            }
            return this;
        }

        boolean performTranslate(float currentX, float currentY, float targetX, float targetY) {
            return !NumberUtils.isEqual(currentX, targetX) ||
                    !NumberUtils.isEqual(currentY, targetY);
        }

        AnimatedZoomRunnable translate(float currentX, float currentY, float targetX, float targetY) {
            mPerformTranslate = true;
            mXStart = currentX;
            mYStart = currentY;
            mXEnd = targetX;
            mYEnd = targetY;
            log(String.format("AnimatedZoomRunnable.Translate: x[%s -> %s], y[%s -> %s]", mXStart, mXEnd, mYStart, mYEnd));
            mPanDispatcher.onPanBegin(ZoomLayout.this);
            return this;
        }

        boolean translate() {
            return mPerformTranslate;
        }

        void cancel() {
            mCancelled = true;
            finish();
        }

        private void finish() {
            if (!mFinished) {
                if (mPerformScale) {
                    mZoomDispatcher.onZoomEnd(ZoomLayout.this, getScale());
                }
                if (mPerformTranslate) {
                    mPanDispatcher.onPanEnd(ZoomLayout.this);
                }
            }
            mFinished = true;
        }

        @Override
        public void run() {

            if (mCancelled || (!mPerformScale && !mPerformTranslate)) {
                return;
            }

            float t = interpolate();

            if (mPerformScale) {
                float newScale = mZoomStart + t * (mZoomEnd - mZoomStart);
                internalScale(newScale, mFocalX, mFocalY);
            }

            if (mPerformTranslate) {
                float x = mXStart + t * (mXEnd - mXStart);
                float y = mYStart + t * (mYEnd - mYStart);
                internalMove(x, y);
            }

            // We haven't hit our target scale yet, so post ourselves again
            if (t < 1f) {
                ViewCompat.postOnAnimation(ZoomLayout.this, this);
            } else {
                finish();
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration;
            t = Math.min(1f, t);
            return mAnimationInterpolator.getInterpolation(t);
        }

    }

    private class FlingRunnable implements Runnable {

        private final ScrollerCompat mScroller;
        private int mCurrentX, mCurrentY;
        private boolean mFinished = false;

        FlingRunnable(Context context) {
            mScroller = ScrollerCompat.getScroller(context);
        }

        void fling(int velocityX, int velocityY) {

            final int startX = Math.round(mViewPortRect.left);
            final int minX, maxX;
            if (mViewPortRect.width() < mDrawRect.width()) {
                minX = 0;
                maxX = Math.round(mDrawRect.width() - mViewPortRect.width());
            } else {
                minX = maxX = startX;
            }

            final int startY = Math.round(mViewPortRect.top);
            final int minY, maxY;
            if (mViewPortRect.height() < mDrawRect.height()) {
                minY = 0;
                maxY = Math.round(mDrawRect.height() - mViewPortRect.height());
            } else {
                minY = maxY = startY;
            }

            mCurrentX = startX;
            mCurrentY = startY;

            log(String.format("fling. StartX:%s StartY:%s MaxX:%s MaxY:%s doScroll:%s", startX, startY, maxX, maxY, (startX != maxX || startY != maxY)));

            // If we actually can move, fling the scroller
            if (startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
                mPanDispatcher.onPanBegin(ZoomLayout.this);
            } else {
                mFinished = true;
            }

        }

        void cancelFling() {
            mScroller.forceFinished(true);
            finish();
        }

        private void finish() {
            if (!mFinished) {
                mPanDispatcher.onPanEnd(ZoomLayout.this);
            }
            mFinished = true;
        }

        public boolean isFinished() {
            return mScroller.isFinished();
        }

        @Override
        public void run() {
            if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {

                final int newX = mScroller.getCurrX();
                final int newY = mScroller.getCurrY();

//                log(String.format("mCurrentX:%s, newX:%s, mCurrentY:%s, newY:%s", mCurrentX, newX, mCurrentY, newY));
                moveBy(mCurrentX - newX, mCurrentY - newY);

                mCurrentX = newX;
                mCurrentY = newY;

                // Post On animation
                ViewCompat.postOnAnimation(ZoomLayout.this, FlingRunnable.this);
            } else {
                finish();
            }
        }
    }

    public OnTapListener getOnTabListener() {
        return mTapListener;
    }

    public void setOnTapListener(OnTapListener tabListener) {
        mTapListener = tabListener;
    }

    public OnDoubleTapListener getOnDoubleTapListener() {
        return mDoubleTapListener;
    }

    public void setOnDoubleTapListener(OnDoubleTapListener doubleTapListener) {
        mDoubleTapListener = doubleTapListener;
    }

    public OnLongTapListener getOnLongTapListener() {
        return mLongTapListener;
    }

    public void setOnLongTapListener(OnLongTapListener longTapListener) {
        mLongTapListener = longTapListener;
    }

    public OnZoomListener getOnZoomListener() {
        return mZoomDispatcher.mZoomListener;
    }

    public void setOnZoomListener(OnZoomListener zoomListener) {
        mZoomDispatcher.mZoomListener = zoomListener;
    }

    public OnPanListener getOnPanListener() {
        return mPanDispatcher.mPanListener;
    }

    public void setOnPanListener(OnPanListener panListener) {
        mPanDispatcher.mPanListener = panListener;
    }

    public interface OnZoomListener {
        void onZoomBegin(ZoomLayout view, float scale);
        void onZoom(ZoomLayout view, float scale);
        void onZoomEnd(ZoomLayout view, float scale);
    }

    public interface OnPanListener {
        void onPanBegin(ZoomLayout view);
        void onPan(ZoomLayout view);
        void onPanEnd(ZoomLayout view);
    }

    public interface OnTapListener {
        boolean onContentTap(ZoomLayout view, float x, float y);
        boolean onViewTap(ZoomLayout view);
    }

    public interface OnDoubleTapListener {
        boolean onContentDoubleTap(ZoomLayout view, float x, float y);
        boolean onViewDoubleTap(ZoomLayout view);
    }

    public interface OnLongTapListener {
        void onContentLongTap(ZoomLayout view, float x, float y);
        void onViewLongTap(ZoomLayout view);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        throw new IllegalStateException("Cannot set OnClickListener, please use OnTapListener");
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        throw new IllegalStateException("Cannot set OnLongClickListener, please use OnLongTabListener");
    }

    private class ZoomDispatcher implements OnZoomListener {

        int mCount = 0;
        OnZoomListener mZoomListener;

        @Override
        public void onZoomBegin(ZoomLayout view, float scale) {
            if (mCount++ > 0) {
                throw new IllegalStateException("onZoomBegin() called twice without onZoomEnd()");
            }
            if (mZoomListener != null) {
                mZoomListener.onZoomBegin(view, scale);
            }
        }

        @Override
        public void onZoom(ZoomLayout view, float scale) {
            if (mZoomListener != null) {
                mZoomListener.onZoom(view, scale);
            }
        }

        @Override
        public void onZoomEnd(ZoomLayout view, float scale) {
            if (mCount-- < 0) {
                throw new IllegalStateException("onZoomEnd() called twice without onZoomBegin()");
            }
            if (mZoomListener != null) {
                mZoomListener.onZoomEnd(view, scale);
            }
        }
    }

    private class PanDispatcher implements OnPanListener {

        int mCount = 0;
        OnPanListener mPanListener;

        @Override
        public void onPanBegin(ZoomLayout view) {
            if (mCount++ > 0) {
                throw new IllegalStateException("onPanBegin() called twice without onPanEnd()");
            }
            if (mPanListener != null) {
                mPanListener.onPanBegin(ZoomLayout.this);
            }
        }

        @Override
        public void onPan(ZoomLayout view) {
            if (mPanListener != null) {
                mPanListener.onPan(ZoomLayout.this);
            }
        }

        @Override
        public void onPanEnd(ZoomLayout view) {
            if (mCount-- < 0) {
                throw new IllegalStateException("onPanEnd() called twice without onPanBegin()");
            }
            if (mPanListener != null) {
                mPanListener.onPanEnd(ZoomLayout.this);
            }
        }
    }

    private void log(String msg) {
        if (DEBUG) {
            L.d(TAG, msg);
        }
    }

    private void printMatrixInfo(String tag, boolean regulare, boolean inverted) {
        if (regulare) {
            log(String.format("%s: mScaleMatrix            %s", tag, ZoomUtils.getMatrixBasicInfo(mScaleMatrix)));
            log(String.format("%s: mTranslateMatrix        %s", tag, ZoomUtils.getMatrixBasicInfo(mTranslateMatrix)));
        }
        if (inverted) {
            log(String.format("%s: mScaleMatrixInverse     %s", tag, ZoomUtils.getMatrixBasicInfo(mScaleMatrixInverse)));
            log(String.format("%s: mTranslateMatrixInverse %s", tag, ZoomUtils.getMatrixBasicInfo(mTranslateMatrixInverse)));
        }
    }

    private void printViewRects(String tag) {
        log(ZoomUtils.getViewRectInfo(tag, "ViewRect", mViewPortRect));
        log(ZoomUtils.getViewRectInfo(tag, "DrawRect", mDrawRect));
    }

}
