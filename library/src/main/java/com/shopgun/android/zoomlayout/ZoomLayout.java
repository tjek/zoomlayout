package com.shopgun.android.zoomlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
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
    private int mZoomDuration = 250;

    // allow parent views to intercept any touch events that we do not consume
    boolean mAllowParentInterceptOnEdge = true;
    // allow parent views to intercept any touch events that we do not consume even if we are in a scaled state
    boolean mAllowParentInterceptOnScaled = false;
    // minimum scale of the content
    private float mMinScale = 1.0f;
    // maximum scale of the content
    private float mMaxScale = 3.0f;

    private boolean mAllowZoom = true;

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
//            log("### MotionEvent.START ###");
        }
        boolean consumed = mScaleDetector.onTouchEvent(ev);
        consumed = mGestureDetector.onTouchEvent(ev) || consumed;

        if (action == MotionEvent.ACTION_UP) {

            // manually call up
            consumed = mGestureListener.onUp(ev) || consumed;

            float scale = getScale();
            float newScale = NumberUtils.clamp(mMinScale, scale, mMaxScale);
            if (!NumberUtils.isEqual(scale, newScale)) {
                mAnimatedZoomRunnable = new AnimatedZoomRunnable();
                mAnimatedZoomRunnable.scale(scale, newScale, mFocusX, mFocusY);
                ViewCompat.postOnAnimation(ZoomLayout.this, mAnimatedZoomRunnable);
                consumed = true;
            }

            if (mViewPortRect.height() > mDrawRect.height() &&
                            mViewPortRect.width() > mDrawRect.width()) {
//                log("ViewRect can contain DrawRect - translate and scale");

            } else if (!mDrawRect.contains(mViewPortRect)) {
//                log("ViewRect in on an edge of DrawRect - translate");

            } else {
//                log("DrawRect contains ViewRect - scale");

            }

//            log("### MotionEvent.END ###");
        }

        return consumed;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private float mScaleOnActionDown;
        private boolean mScrolling = false;

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mTapListener != null) {
                PointF p = getRelPosition(e, getChildAt(0));
                if (mDrawRect.contains(e.getX(), e.getY())) {
                    return mTapListener.onContentTap(ZoomLayout.this, e.getX(), e.getY(), p.x, p.y);
                } else {
                    return mTapListener.onViewTap(ZoomLayout.this, e.getX(), e.getY(), p.x, p.y);
                }
            }
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            switch (e.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    mScaleOnActionDown = getScale();
                    break;
                case MotionEvent.ACTION_UP:
                    if (mDoubleTapListener != null && NumberUtils.isEqual(getScale(), mScaleOnActionDown)) {
                        PointF p = getRelPosition(e, getChildAt(0));
                        if (mDrawRect.contains(e.getX(), e.getY())) {
                            return mDoubleTapListener.onContentDoubleTap(ZoomLayout.this, e.getX(), e.getY(), p.x, p.y);
                        } else {
                            return mDoubleTapListener.onViewDoubleTap(ZoomLayout.this, e.getX(), e.getY(), p.x, p.y);
                        }
                    }
                    break;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (!mScaleDetector.isInProgress()) {
                if (mLongTapListener != null) {
                    PointF p = getRelPosition(e, getChildAt(0));
                    if (mDrawRect.contains(e.getX(), e.getY())) {
                        mLongTapListener.onContentLongTap(ZoomLayout.this, e.getX(), e.getY(), p.x, p.y);
                    } else {
                        mLongTapListener.onViewLongTap(ZoomLayout.this, e.getX(), e.getY(), p.x, p.y);
                    }
                }
            }
        }

        private PointF getRelPosition(MotionEvent e, View v) {
            mArray[0] = e.getX();
            mArray[1] = e.getY();
            screenPointsToScaledPoints(mArray);
            return new PointF(mArray[0]-v.getLeft(), mArray[1]-v.getTop());
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean consumed = false;
            if (e2.getPointerCount() == 1 && !mScaleDetector.isInProgress()) {
                // only drag if we have one pointer and aren't already scaling
                if (!mScrolling) {
                    mPanDispatcher.onPanBegin();
                    mScrolling = true;
                }
                consumed = internalMoveBy(distanceX, distanceY, true);
//                PointF p = ensureTranslationBounds();
//                internalMove(p.x, p.y, false);
                if (consumed) {
                    mPanDispatcher.onPan();
                }
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

        @Override
        public void onShowPress(MotionEvent e) {
            super.onShowPress(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            requestDisallowInterceptTouchEvent(true);
            cancelFling();
            cancelZoom();
            return false;
        }

        boolean onUp(MotionEvent e) {
            if (mScrolling) {
                mPanDispatcher.onPanEnd();
                mScrolling = false;
                return true;
            }
            return false;
        }

    }

    /**
     * When setting a new focus point, the translations on scale-matrix will change,
     * to counter that we'll first read old translation values, then apply the new focus-point
     * (with the old scale), then read the new translation values. Lastly we'll translate
     * out translate-matrix by the delta given by the scale-matrix translations.
     * @param focusX focus-focusX in screen coordinate
     * @param focusY focus-focusY in screen coordinate
     */
    private void fixFocusPoint(float focusX, float focusY) {
        mArray[0] = focusX;
        mArray[1] = focusY;
        screenPointsToScaledPoints(mArray);
        // The first scale event translates the content, so we'll counter that translate
        float x1 = getMatrixValue(mScaleMatrix, Matrix.MTRANS_X);
        float y1 = getMatrixValue(mScaleMatrix, Matrix.MTRANS_Y);
        internalScale(getScale(), mArray[0], mArray[1]);
        float dX = getMatrixValue(mScaleMatrix, Matrix.MTRANS_X)-x1;
        float dY = getMatrixValue(mScaleMatrix, Matrix.MTRANS_Y)-y1;
        internalMove(dX + getPosX(), dY + getPosY(), false);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (mAllowZoom) {
                mZoomDispatcher.onZoomBegin(getScale());
                fixFocusPoint(detector.getFocusX(), detector.getFocusY());
            }
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = getScale() * detector.getScaleFactor();
            if (mAllowZoom) {
                internalScale(scale, mFocusX, mFocusY);
                mZoomDispatcher.onZoom(scale);
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mAllowZoom) {
                ensureTranslationBounds();
                mZoomDispatcher.onZoomEnd(getScale());
            }
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

    public void setMaxScale(float maxScale) {
        mMaxScale = maxScale;
    }

    public float getMinScale() {
        return mMinScale;
    }

    public void setMinScale(float minScale) {
        mMinScale = minScale;
    }

    public boolean isAllowZoom() {
        return mAllowZoom;
    }

    public void setAllowZoom(boolean allowZoom) {
        mAllowZoom = allowZoom;
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
        if (!mAllowZoom) {
            return;
        }
        fixFocusPoint(focusX, focusY);
        focusX = mFocusX;
        focusY = mFocusY;
        if (!mAllowOverScale) {
            scale = NumberUtils.clamp(mMinScale, scale, mMaxScale);
        }
        if (animate) {
            mAnimatedZoomRunnable = new AnimatedZoomRunnable();
            mAnimatedZoomRunnable.scale(getScale(), scale, focusX, focusY);
            ViewCompat.postOnAnimation(ZoomLayout.this, mAnimatedZoomRunnable);
        } else {
            mZoomDispatcher.onZoomBegin(getScale());
            internalScale(scale, focusX, focusY);
            mZoomDispatcher.onZoom(scale);
            mZoomDispatcher.onZoomEnd(scale);
        }
    }

    public boolean moveBy(float dX, float dY) {
        return moveTo(dX + getPosX(), dY + getPosY());
    }

    public boolean moveTo(float posX, float posY) {
        mPanDispatcher.onPanBegin();
        if (internalMove(posX, posY, true)) {
            mPanDispatcher.onPan();
        }
        mPanDispatcher.onPanEnd();
        return true;
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

    private boolean internalMoveBy(float dX, float dY, boolean clamp) {
        return internalMove(dX + getPosX(), dY + getPosY(), clamp);
    }

    private boolean internalMove(float posX, float posY, boolean clamp) {
        if (clamp) {
            RectF r = getTranslateBounds();
            if (r.isEmpty()) {
                return false;
            }
            posX = NumberUtils.clamp(r.left, posX, r.right);
            posY = NumberUtils.clamp(r.top, posY, r.bottom);
        }
        if (!NumberUtils.isEqual(posX, getPosX()) ||
                !NumberUtils.isEqual(posY, getPosY())) {
            mTranslateMatrix.setTranslate(-posX, -posY);
            matrixUpdated();
            invalidate();
            return true;
        }
        return false;
    }

    private void ensureTranslationBounds() {
        float x = getPosX();
        float y = getPosY();
        PointF p = new PointF(x, y);
        if (mDrawRect.width() < mViewPortRect.width()) {
            p.x += mDrawRect.centerX() - mViewPortRect.centerX();
        } else if (mDrawRect.right < mViewPortRect.right) {
            p.x += mDrawRect.right - mViewPortRect.right;
        } else if (mDrawRect.left > mViewPortRect.left) {
            p.x += mDrawRect.left - mViewPortRect.left;
        }

        if (mDrawRect.height() < mViewPortRect.height()) {
            p.y += mDrawRect.centerY() - mViewPortRect.centerY();
        } else if (mDrawRect.bottom < mViewPortRect.bottom) {
            p.y += mDrawRect.bottom - mViewPortRect.bottom;
        } else if (mDrawRect.top > mViewPortRect.top) {
            p.y += mDrawRect.top - mViewPortRect.top;
        }

        if (!NumberUtils.isEqual(p.x, x) || NumberUtils.isEqual(p.y, y)) {
//            L.d(TAG, String.format(Locale.US, "ensureTranslationBounds x[ %.0f -> %.0f ], y[ %.0f -> %.0f ] ", x, p.x, y, p.y));
            internalMove(p.x, p.y, false);
        }

    }

    private void internalScale(float scale, float focusX, float focusY) {
        mFocusX = focusX;
        mFocusY = focusY;
        mScaleMatrix.setScale(scale, scale, mFocusX, mFocusY);
        matrixUpdated();
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
        } else {
            ZoomUtils.setRect(mDrawRect, -1f, -1f, -1f, -1f);
        }
        ZoomUtils.setRect(mViewPortRect, 0, 0, getWidth(), getHeight());
    }

    /**
     * Get the current x-translation
     */
    public float getPosX() {
        return -getMatrixValue(mTranslateMatrix, Matrix.MTRANS_X);
    }

    /**
     * Get the current y-translation
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

        boolean mCancelled = false;
        boolean mFinished = false;

        private long mStartTime;
        private float mZoomStart, mZoomEnd, mFocalX, mFocalY;
        private float mXStart, mYStart, mXEnd, mYEnd;

        AnimatedZoomRunnable() {
            mStartTime = System.currentTimeMillis();
        }

        AnimatedZoomRunnable scale(float currentZoom, float targetZoom, float focalX, float focalY) {
            mFocalX = focalX;
            mFocalY = focalY;
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
//            log(String.format("AnimatedZoomRunnable.Scale: %s -> %s", mZoomStart, mZoomEnd));
            mZoomDispatcher.onZoomBegin(getScale());
            return this;
        }

        void cancel() {
            mCancelled = true;
            finish();
        }

        private void finish() {
            if (!mFinished) {
                mZoomDispatcher.onZoomEnd(getScale());
            }
            mFinished = true;
        }

        @Override
        public void run() {

            if (mCancelled) {
                return;
            }

            float t = interpolate();

            float newScale = mZoomStart + t * (mZoomEnd - mZoomStart);
            internalScale(newScale, mFocalX, mFocalY);
            ensureTranslationBounds();
            mZoomDispatcher.onZoom(newScale);

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
                minX = Math.round(mDrawRect.left);
                maxX = Math.round(mDrawRect.width() - mViewPortRect.width());
            } else {
                minX = maxX = startX;
            }

            final int startY = Math.round(mViewPortRect.top);
            final int minY, maxY;
            if (mViewPortRect.height() < mDrawRect.height()) {
                minY = Math.round(mDrawRect.top);
                maxY = Math.round(mDrawRect.bottom - mViewPortRect.bottom);
            } else {
                minY = maxY = startY;
            }

            mCurrentX = startX;
            mCurrentY = startY;

//            log(String.format("fling. x[ %s - %s ], y[ %s - %s ]", minX, maxX, minY, maxY));
            // If we actually can move, fling the scroller
            if (startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
                mPanDispatcher.onPanBegin();
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
                mPanDispatcher.onPanEnd();
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
                if (internalMoveBy(mCurrentX - newX, mCurrentY - newY, true)) {
                    mPanDispatcher.onPan();
                }

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
        boolean onContentTap(ZoomLayout view, float absX, float absY, float relX, float relY);
        boolean onViewTap(ZoomLayout view, float absX, float absY, float relX, float relY);
    }

    public interface OnDoubleTapListener {
        boolean onContentDoubleTap(ZoomLayout view, float absX, float absY, float relX, float relY);
        boolean onViewDoubleTap(ZoomLayout view, float absX, float absY, float relX, float relY);
    }

    public interface OnLongTapListener {
        void onContentLongTap(ZoomLayout view, float absX, float absY, float relX, float relY);
        void onViewLongTap(ZoomLayout view, float absX, float absY, float relX, float relY);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        throw new IllegalStateException("Cannot set OnClickListener, please use OnTapListener.");
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        throw new IllegalStateException("Cannot set OnLongClickListener, please use OnLongTabListener.");
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        throw new IllegalStateException("Cannot set OnTouchListener.");
    }

    private class ZoomDispatcher {

        int mCount = 0;
        OnZoomListener mZoomListener;

        void onZoomBegin(float scale) {
            if (mCount++ == 0) {
                if (mZoomListener != null) {
                    mZoomListener.onZoomBegin(ZoomLayout.this, scale);
                }
            }
        }

        void onZoom(float scale) {
            if (mZoomListener != null) {
                mZoomListener.onZoom(ZoomLayout.this, scale);
            }
        }

        void onZoomEnd(float scale) {
            if (--mCount == 0) {
                if (mZoomListener != null) {
                    mZoomListener.onZoomEnd(ZoomLayout.this, scale);
                }
            }
        }
    }

    private class PanDispatcher {

        int mCount = 0;
        OnPanListener mPanListener;

        void onPanBegin() {
            if (mCount++ == 0) {
                if (mPanListener != null) {
                    mPanListener.onPanBegin(ZoomLayout.this);
                }
            }
        }

        void onPan() {
            if (mPanListener != null) {
                mPanListener.onPan(ZoomLayout.this);
            }
        }

        void onPanEnd() {
            if (--mCount == 0) {
                if (mPanListener != null) {
                    mPanListener.onPanEnd(ZoomLayout.this);
                }
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
