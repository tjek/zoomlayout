package com.shopgun.android.zoomlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.shopgun.android.utils.NumberUtils;
import com.shopgun.android.utils.UnitUtils;
import com.shopgun.android.utils.log.L;

import java.util.Locale;

@SuppressWarnings("unused")
public class ZoomLayout extends FrameLayout {

    public static final String TAG = ZoomLayout.class.getSimpleName();

    public boolean DEBUG_DRAW = false;

    private float mMinScale = 1.0f;
    private float mMaxScale = 4.0f;

    private ScaleGestureDetector mScaleDetector;
    private Matrix mScaleMatrix = new Matrix();
    private Matrix mScaleMatrixInverse = new Matrix();

    private GestureDetector mGestureDetector;

    private float mPosX;
    private float mPosY;
    private Matrix mTranslateMatrix = new Matrix();
    private Matrix mTranslateMatrixInverse = new Matrix();

    private float mFocusY;
    private float mFocusX;

    private float[] mArray = new float[6];

    // for set scale
    private boolean mAllowOverScale = true;

    // for animation runnable
    private Interpolator mAnimationInterpolator = new AccelerateDecelerateInterpolator();
    private int mZoomDuration = 200;

    RectF mDrawingRect = new RectF();
    RectF mViewPortRect = new RectF();

    private FlingRunnable mCurrentFlingRunnable;

    private AnimatedZoomRunnable mCurrentAnimatedZoomRunnable;

    // allow parent views to intercept any touch events that we do not consume
    boolean mAllowParentInterceptOnEdge = true;
    // allow parent views to intercept any touch events that we do not consume even if we are in a scaled state
    boolean mAllowParentInterceptOnScaled = false;

    // Listeners
    private ZoomDispatcher mZoomDispatcher = new ZoomDispatcher();
    private PanDispatcher mPanDispatcher = new PanDispatcher();
    private TapDispatcher mTapDispatcher = new TapDispatcher();
    private DoubleTapDispatcher mDoubleTapDispatcher = new DoubleTapDispatcher();
    private LongTapDispatcher mLongTapDispatcher = new LongTapDispatcher();

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
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mTranslateMatrix.setTranslate(0, 0);
        mScaleMatrix.setScale(1, 1);
    }

    Paint mDebugPaintXY;
    Paint mDebugPaintFocus;
    int mDebugRadius = 0;
    private void ensureDebugOptions() {
        if (mDebugPaintXY == null) {
            mDebugPaintXY = new Paint();
            mDebugPaintXY.setColor(Color.BLUE);
            mDebugPaintFocus = new Paint();
            mDebugPaintFocus.setColor(Color.MAGENTA);
            mDebugRadius = UnitUtils.dpToPx(2, getContext());
        }
    }

    protected void debugDraw(Canvas canvas) {
        if (DEBUG_DRAW) {
            ensureDebugOptions();
            canvas.drawCircle(-mPosX, -mPosY, mDebugRadius, mDebugPaintXY);
            canvas.drawCircle(mPosX, mPosY, mDebugRadius, mDebugPaintXY);
            canvas.drawCircle(mFocusX, mFocusY, mDebugRadius, mDebugPaintFocus);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(-mPosX, -mPosY);
        float scale = getScale();
        canvas.scale(scale, scale, mFocusX, mFocusY);
        super.dispatchDraw(canvas);
        debugDraw(canvas);
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        debugDraw(canvas);
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
        mArray[0] = rect.left;
        mArray[1] = rect.top;
        mArray[2] = rect.right;
        mArray[3] = rect.bottom;
        mArray = scaledPointsToScreenPoints(mArray);
        rect.set(Math.round(mArray[0]), Math.round(mArray[1]),
                Math.round(mArray[2]), Math.round(mArray[3]));
    }

    private void scaledPointsToScreenPoints(RectF rect) {
        mArray[0] = rect.left;
        mArray[1] = rect.top;
        mArray[2] = rect.right;
        mArray[3] = rect.bottom;
        mArray = scaledPointsToScreenPoints(mArray);
        rect.set(Math.round(mArray[0]), Math.round(mArray[1]),
                Math.round(mArray[2]), Math.round(mArray[3]));
    }

    private void screenPointsToScaledPoints(RectF rect) {
        mArray[0] = rect.left;
        mArray[1] = rect.top;
        mArray[2] = rect.right;
        mArray[3] = rect.bottom;
        mArray = screenPointsToScaledPoints(mArray);
        rect.set(Math.round(mArray[0]), Math.round(mArray[1]),
                Math.round(mArray[2]), Math.round(mArray[3]));
    }

    private void scaledPointsToScreenPoints(MotionEvent ev) {
        mArray[0] = ev.getX();
        mArray[1] = ev.getY();
        mArray = scaledPointsToScreenPoints(mArray);
        ev.setLocation(mArray[0], mArray[1]);
    }

    private void screenPointsToScaledPoints(MotionEvent ev) {
        mArray[0] = ev.getX();
        mArray[1] = ev.getY();
        mArray = screenPointsToScaledPoints(mArray);
        ev.setLocation(mArray[0], mArray[1]);
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
        screenPointsToScaledPoints(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        scaledPointsToScreenPoints(ev);

        boolean handled = mScaleDetector.onTouchEvent(ev);
        handled = mGestureDetector.onTouchEvent(ev) || handled;

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                requestDisallowInterceptTouchEvent(true);
                cancelFling();
                cancelZoom();
                break;

            case MotionEvent.ACTION_UP: {

                RectF viewRect = getViewPortRect();
                RectF drawRect = getDrawingRect();
                mCurrentAnimatedZoomRunnable = new AnimatedZoomRunnable();

                float currentScale = getScale();
                float newScale = currentScale < mMinScale ? mMinScale : (mMaxScale < currentScale ? mMaxScale : currentScale);
                boolean needScale = !NumberUtils.isEqual(newScale, currentScale);

                // Find current bounds before applying scale to do the rest of the calculations

                if (viewRect.contains(drawRect) ||
                        (viewRect.height() > drawRect.height() &&
                                viewRect.width() > drawRect.width())) {

                    mCurrentAnimatedZoomRunnable.scaleIfNeeded(currentScale, newScale, (float)getWidth()/2, (float)getHeight()/2);
                    mCurrentAnimatedZoomRunnable.translateIfNeeded(mPosX, mPosY, 0, 0);

                } else if (!drawRect.contains(viewRect)) {

                    if (needScale) {
                        // we'll need measurements from the new scale/position
                        mScaleMatrix.setScale(newScale, newScale);
                        matrixUpdated();
                    }

                    // TODO handle this case nicely - not just re-center
                    mCurrentAnimatedZoomRunnable.translateIfNeeded(mPosX, mPosY, 0, 0);

                    if (viewRect.left < drawRect.left) {
                        L.d(TAG, "left");
                    }

                    if (viewRect.right > drawRect.right) {
                        L.d(TAG, "right");
                    }

                    if (viewRect.top < drawRect.top) {
                        L.d(TAG, "top");
                    }

                    if (viewRect.bottom > drawRect.bottom) {
                        L.d(TAG, "bottom");
                    }

                    if (needScale) {
                        // reset the scale/position
                        mScaleMatrix.setScale(currentScale, currentScale);
                        matrixUpdated();
                    }

                } else {

                    // The ViewPort is inside the DrawRect - no problem
                    mCurrentAnimatedZoomRunnable.scaleIfNeeded(currentScale, newScale, mFocusX, mFocusY);

                }

                if (mCurrentAnimatedZoomRunnable.scale() || mCurrentAnimatedZoomRunnable.translate()) {
                    ViewCompat.postOnAnimation(ZoomLayout.this, mCurrentAnimatedZoomRunnable);
                    handled = true;
                } else {
                    cancelZoom();
                }

                break;
            }

        }

        return handled;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return mTapDispatcher.onTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return mDoubleTapDispatcher.onDoubleTap(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            mLongTapDispatcher.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean consumed = false;
            if (e2.getPointerCount() == 1 && !mScaleDetector.isInProgress()) {
                // only drag if we have one pointer and aren't already scaling
                consumed = moveBy(distanceX, distanceY);
                if (mAllowParentInterceptOnEdge && !consumed && (!isScaled() || mAllowParentInterceptOnScaled)) {
                    requestDisallowInterceptTouchEvent(false);
                }
            }
            return consumed;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (getDrawingRect().contains(getViewPortRect())) {
                float newScale = getScale() < mMinScale ? mMinScale : (mMaxScale < getScale() ? mMaxScale : getScale());
                if (NumberUtils.isEqual(newScale, getScale())) {
                    // only fling if no scale is needed - scale will happen on ACTION_UP
                    mCurrentFlingRunnable = new FlingRunnable(getContext());
                    mCurrentFlingRunnable.fling((int) velocityX, (int) velocityY);
                    ViewCompat.postOnAnimation(ZoomLayout.this, mCurrentFlingRunnable);
                    return true;
                }
            }
            return false;
        }

    }

    private void cancelFling() {
        if (mCurrentFlingRunnable != null) {
            mCurrentFlingRunnable.cancelFling();
            mCurrentFlingRunnable = null;
        }
    }

    private void cancelZoom() {
        if (mCurrentAnimatedZoomRunnable != null) {
            mCurrentAnimatedZoomRunnable.cancel();
            mCurrentAnimatedZoomRunnable = null;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float mFocusX;
        private float mFocusY;
        private float mCurrentX;
        private float mCurrentY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mCurrentX = mFocusX = mArray[0] = detector.getFocusX();
            mCurrentY = mFocusY = mArray[1] = detector.getFocusY();
            screenPointsToScaledPoints(mArray);
            mFocusX = mArray[0];
            mFocusY = mArray[1];
            mZoomDispatcher.onZoomBegin(ZoomLayout.this, getScale());
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = getScale() * detector.getScaleFactor();
//            moveBy(mCurrentX - detector.getFocusX(), mCurrentY - detector.getFocusY());
            internalScale(scale, mFocusX, mFocusY);
            mCurrentX = detector.getFocusX();
            mCurrentY = detector.getFocusY();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mZoomDispatcher.onZoomEnd(ZoomLayout.this, getScale());
        }
    }

    private View getChild() {
        return getChildAt(0);
    }

    private int getChildViewWidth() {
        final View v = getChild();
        return v == null ? 0 : v.getWidth();
    }

    private int getChildViewHeight() {
        final View v = getChild();
        return v == null ? 0 : v.getHeight();
    }

    /**
     * The rectangle representing the location of the view inside the ZoomView. including scale and translations.
     */
    public RectF getDrawingRect() {
        return mDrawingRect;
    }

    /**
     * The visible region of ZoomView.
     */
    public RectF getViewPortRect() {
        return mViewPortRect;
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

    public boolean isScaled() {
        return !NumberUtils.isEqual(getScale(), 1.0f, 0.05f);
    }

    public void setScale(float scale, float focusX, float focusY, boolean animate) {
        if (!mAllowOverScale) {
            scale = Math.max(mMinScale, Math.min(scale, mMaxScale));
        }
        if (animate) {
            mCurrentAnimatedZoomRunnable = new AnimatedZoomRunnable();
            mCurrentAnimatedZoomRunnable.scale(getScale(), scale, focusX, focusY);
            ViewCompat.postOnAnimation(ZoomLayout.this, mCurrentAnimatedZoomRunnable);
        } else {
            internalScale(scale, focusX, focusY);
        }
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

    public boolean moveBy(float dX, float dY) {
        return moveTo((dX + mPosX), (dY + mPosY));
    }

    public boolean moveTo(float posX, float posY) {
        if (NumberUtils.isEqual(posX, mPosX) &&
                NumberUtils.isEqual(posY, mPosY)) {
            return false;
        }

        L.d(TAG, String.format(Locale.US, "scale:%.2f, x[ %s -> %s ], y[ %s -> %s ], focusX:%s, focusY:%s", getScale(), mPosX, posX, mPosY, posY, mFocusX, mFocusY));
        mTranslateMatrix.setTranslate(-posX, -posY);
        matrixUpdated();

        float tmpX = mPosX;
        float tmpY = mPosY;
        boolean horizontalOutOfBounds = mDrawingRect.left >= mViewPortRect.left || mDrawingRect.right <= mViewPortRect.right;
        if (!horizontalOutOfBounds) {
            tmpX = posX;
        }

        boolean verticalOutOfBounds = mDrawingRect.top >= mViewPortRect.top || mDrawingRect.bottom <= mViewPortRect.bottom;
        if (!verticalOutOfBounds) {
            tmpY = posY;
        }

//        L.d(TAG, "verticalOutOfBounds: " + verticalOutOfBounds + ", horizontalOutOfBounds: " + horizontalOutOfBounds);

        internalMove(tmpX, tmpY);
        return !verticalOutOfBounds && !horizontalOutOfBounds;
    }

    private void internalMove(float posX, float posY) {
        mPosX = posX;
        mPosY = posY;
//        L.d(TAG, "mPosX: " + mPosX + ", mPosY: " + mPosY);
        mTranslateMatrix.setTranslate(-posX, -posY);
        matrixUpdated();
        mPanDispatcher.onPan(ZoomLayout.this);
        invalidate();
    }

    private void matrixUpdated() {

        mScaleMatrix.invert(mScaleMatrixInverse);
        mTranslateMatrix.invert(mTranslateMatrixInverse);

        mDrawingRect.set(0, 0, getChildViewWidth(), getChildViewHeight());
        scaledPointsToScreenPoints(mDrawingRect);
        mViewPortRect.set(0, 0, getWidth(), getHeight());
        screenPointsToScaledPoints(mViewPortRect);

        printMatrixInfo("MatrixUpdate");
    }


    private float[] mMatrixValues = new float[9];
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
            L.d(TAG, String.format("AnimatedZoomRunnable.Scale: %s -> %s", mZoomStart, mZoomEnd));
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
            L.d(TAG, String.format("AnimatedZoomRunnable.Translate: x[%s -> %s], y[%s -> %s]", mXStart, mXEnd, mYStart, mYEnd));
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

            final RectF viewRect = getViewPortRect();
            final RectF drawRect = getDrawingRect();

            final int startX = Math.round(viewRect.left);
            final int minX, maxX;
            if (viewRect.width() < drawRect.width()) {
                minX = 0;
                maxX = Math.round(drawRect.width() - viewRect.width());
            } else {
                minX = maxX = startX;
            }

            final int startY = Math.round(viewRect.top);
            final int minY, maxY;
            if (viewRect.height() < drawRect.height()) {
                minY = 0;
                maxY = Math.round(drawRect.height() - viewRect.height());
            } else {
                minY = maxY = startY;
            }

            mCurrentX = startX;
            mCurrentY = startY;

            L.d(TAG, String.format("fling. StartX:%s StartY:%s MaxX:%s MaxY:%s doScroll:%s", startX, startY, maxX, maxY, (startX != maxX || startY != maxY)));

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

//                L.d(TAG, String.format("mCurrentX:%s, newX:%s, mCurrentY:%s, newY:%s", mCurrentX, newX, mCurrentY, newY));
                L.d(TAG, String.format("dX:%s, dY:%s", (mCurrentX - newX), (mCurrentY - newY)));
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
        return mTapDispatcher.mTapListener;
    }

    public void setOnTapListener(OnTapListener tabListener) {
        mTapDispatcher.mTapListener = tabListener;
    }

    public OnDoubleTapListener getOnDoubleTapListener() {
        return mDoubleTapDispatcher.mDoubleTapListener;
    }

    public void setOnDoubleTapListener(OnDoubleTapListener doubleTapListener) {
        mDoubleTapDispatcher.mDoubleTapListener = doubleTapListener;
    }

    public OnLongTapListener getOnLongTapListener() {
        return mLongTapDispatcher.mLongTapListener;
    }

    public void setOnLongTapListener(OnLongTapListener longTapListener) {
        mLongTapDispatcher.mLongTapListener = longTapListener;
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
        void onContentTap(ZoomLayout view, float posX, float posY);
        void onViewTap(ZoomLayout view, float posX, float posY);
    }

    public interface OnDoubleTapListener {
        void onContentDoubleTap(ZoomLayout view, float posX, float posY);
        void onViewDoubleTap(ZoomLayout view, float posX, float posY);
    }

    public interface OnLongTapListener {
        void onContentLongTap(ZoomLayout view, float posX, float posY);
        void onViewLongTap(ZoomLayout view, float posX, float posY);
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

    private class TapDispatcher implements OnTapListener {

        OnTapListener mTapListener;

        boolean onTap(MotionEvent e) {
            L.d(TAG, "onTap: " + e.toString());
            return true;
        }

        @Override
        public void onContentTap(ZoomLayout view, float posX, float posY) {
            if (mTapListener != null) {
                mTapListener.onContentTap(view, posX, posY);
            }
        }

        @Override
        public void onViewTap(ZoomLayout view, float posX, float posY) {
            if (mTapListener != null) {
                mTapListener.onContentTap(view, posX, posY);
            }
        }
    }

    private class DoubleTapDispatcher implements OnDoubleTapListener {

        OnDoubleTapListener mDoubleTapListener;

        boolean onDoubleTap(MotionEvent e) {
            L.d(TAG, "onDoubleTap: " + e.toString());
            return true;
        }

        @Override
        public void onContentDoubleTap(ZoomLayout view, float posX, float posY) {
            if (mDoubleTapListener != null) {
                mDoubleTapListener.onContentDoubleTap(view, posX, posY);
            }
        }

        @Override
        public void onViewDoubleTap(ZoomLayout view, float posX, float posY) {
            if (mDoubleTapListener != null) {
                mDoubleTapListener.onViewDoubleTap(view, posX, posY);
            }
        }
    }

    private class LongTapDispatcher implements OnLongTapListener {

        OnLongTapListener mLongTapListener;

        boolean onLongPress(MotionEvent e) {
            L.d(TAG, "onLongPress: " + e.toString());
            return true;
        }

        @Override
        public void onContentLongTap(ZoomLayout view, float posX, float posY) {
            if (mLongTapListener != null) {
                mLongTapListener.onContentLongTap(view, posX, posY);
            }
        }

        @Override
        public void onViewLongTap(ZoomLayout view, float posX, float posY) {
            if (mLongTapListener != null) {
                mLongTapListener.onViewLongTap(view, posX, posY);
            }
        }
    }

    private void printMatrixInfo(String tag) {
        L.d(TAG, String.format("%s: mScaleMatrix     %s", tag, getMatrixBasicInfo(mScaleMatrix)));
        L.d(TAG, String.format("%s: mTranslateMatrix %s", tag, getMatrixBasicInfo(mTranslateMatrix)));
    }

    private static final String MATRIX_BACIS_FORMAT = "[ scale:%.2f, x:%.2f, y:%.2f ]";
    private static final float[] v = new float[9];
    private String getMatrixBasicInfo(Matrix m) {
        m.getValues(v);
        return String.format(Locale.US, MATRIX_BACIS_FORMAT, v[Matrix.MSCALE_X], v[Matrix.MTRANS_X], v[Matrix.MTRANS_X]);
    }

    private static final String MATRIX_FORMAT = "[ %.2f, %.2f, %.2f ][ %.2f, %.2f, %.2f ][ %.2f, %.2f, %.2f ]";
    private String getMatrixInfo(Matrix m) {
        m.getValues(v);
        return String.format(Locale.US, MATRIX_FORMAT, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
    }

    private void printViewRects(String tag) {
        printViewRect(tag, "ViewRect", mViewPortRect);
        printViewRect(tag, "DrawRect", mDrawingRect);
    }

    private static final String RECT_FORMAT = "%s: %s [ %.0f, %.0f, %.0f, %.0f ], w:%s, h%s";
    private void printViewRect(String tag, String name, RectF r) {
        L.d(TAG, String.format(Locale.US, RECT_FORMAT, tag, name, r.left, r.top, r.right, r.bottom, r.width(), r.height()));
    }

}
