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

    private float mScaleFactor = 1;
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

    private float[] mTranslateArray = new float[6];

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
//        addOnLayoutChangeListener(new OnLayoutChangeListener() {
//            @Override
//            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
////                    L.d(TAG, "onLayoutChange");
//                }
//            }
//        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int w = MeasureSpec.getSize(widthMeasureSpec) -
//                ViewCompat.getPaddingStart(this) - ViewCompat.getPaddingEnd(this);
//        int h = MeasureSpec.getSize(heightMeasureSpec) -
//                getPaddingTop() - getPaddingBottom();
//
//        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST);
//        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST);
//
//        measureChildren(childWidthMeasureSpec, childHeightMeasureSpec);
//        final int mCount = getChildCount();
//        for (int i = 0; i < mCount; i++) {
//            final View child = getChildAt(i);
//            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
//        }
//        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
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
        canvas.scale(mScaleFactor, mScaleFactor, mFocusX, mFocusY);
//        canvas.setMatrix(getDrawMatrix());
        super.dispatchDraw(canvas);
        debugDraw(canvas);
        canvas.restore();
    }

    Matrix mDrawMatrix = new Matrix();
    private Matrix getDrawMatrix() {
        mDrawMatrix.reset();
        mDrawMatrix.set(mScaleMatrix);
        mDrawMatrix.postConcat(mTranslateMatrix);
        return mDrawMatrix;
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
        location[0] *= mScaleFactor;
        location[1] *= mScaleFactor;
        return super.invalidateChildInParent(location, dirty);
    }

    private void scaledPointsToScreenPoints(Rect rect) {
        mTranslateArray[0] = rect.left;
        mTranslateArray[1] = rect.top;
        mTranslateArray[2] = rect.right;
        mTranslateArray[3] = rect.bottom;
        mTranslateArray = scaledPointsToScreenPoints(mTranslateArray);
        rect.set(Math.round(mTranslateArray[0]), Math.round(mTranslateArray[1]),
                Math.round(mTranslateArray[2]), Math.round(mTranslateArray[3]));
    }

    private void scaledPointsToScreenPoints(RectF rect) {
        mTranslateArray[0] = rect.left;
        mTranslateArray[1] = rect.top;
        mTranslateArray[2] = rect.right;
        mTranslateArray[3] = rect.bottom;
        mTranslateArray = scaledPointsToScreenPoints(mTranslateArray);
        rect.set(Math.round(mTranslateArray[0]), Math.round(mTranslateArray[1]),
                Math.round(mTranslateArray[2]), Math.round(mTranslateArray[3]));
    }

    private void screenPointsToScaledPoints(RectF rect) {
        mTranslateArray[0] = rect.left;
        mTranslateArray[1] = rect.top;
        mTranslateArray[2] = rect.right;
        mTranslateArray[3] = rect.bottom;
        mTranslateArray = screenPointsToScaledPoints(mTranslateArray);
        rect.set(Math.round(mTranslateArray[0]), Math.round(mTranslateArray[1]),
                Math.round(mTranslateArray[2]), Math.round(mTranslateArray[3]));
    }

    private void scaledPointsToScreenPoints(MotionEvent ev) {
        mTranslateArray[0] = ev.getX();
        mTranslateArray[1] = ev.getY();
        mTranslateArray = scaledPointsToScreenPoints(mTranslateArray);
        ev.setLocation(mTranslateArray[0], mTranslateArray[1]);
    }

    private void screenPointsToScaledPoints(MotionEvent ev) {
        mTranslateArray[0] = ev.getX();
        mTranslateArray[1] = ev.getY();
        mTranslateArray = screenPointsToScaledPoints(mTranslateArray);
        ev.setLocation(mTranslateArray[0], mTranslateArray[1]);
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

                float newScale = mScaleFactor < mMinScale ? mMinScale : (mMaxScale < mScaleFactor ? mMaxScale : mScaleFactor);
                boolean needScale = !NumberUtils.isEqual(newScale, mScaleFactor);

                // Find current bounds before applying scale to do the rest of the calculations

                if (viewRect.contains(drawRect) ||
                        (viewRect.height() > drawRect.height() &&
                                viewRect.width() > drawRect.width())) {

                    mCurrentAnimatedZoomRunnable.scaleIfNeeded(mScaleFactor, newScale, (float)getWidth()/2, (float)getHeight()/2);
                    mCurrentAnimatedZoomRunnable.translateIfNeeded(mPosX, mPosY, 0, 0);

                } else if (!drawRect.contains(viewRect)) {

                    if (needScale) {
                        // we'll need measurements from the new scale/position
                        mScaleMatrix.setScale(newScale, newScale);
                        mScaleMatrix.invert(mScaleMatrixInverse);
                        // update rects with the new scale
                        updateViewRects();
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
                        mScaleMatrix.setScale(mScaleFactor, mScaleFactor);
                        mScaleMatrix.invert(mScaleMatrixInverse);
                        updateViewRects();
                    }

                } else {

                    // The ViewPort is inside the DrawRect - no problem
                    mCurrentAnimatedZoomRunnable.scaleIfNeeded(mScaleFactor, newScale, mFocusX, mFocusY);

                }

                if (mCurrentAnimatedZoomRunnable.scale() || mCurrentAnimatedZoomRunnable.translate()) {
                    ViewCompat.postOnAnimation(ZoomLayout.this, mCurrentAnimatedZoomRunnable);
                    handled = true;
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
                float newScale = mScaleFactor < mMinScale ? mMinScale : (mMaxScale < mScaleFactor ? mMaxScale : mScaleFactor);
                boolean needScale = !NumberUtils.isEqual(newScale, mScaleFactor);
                if (needScale) {
                    mCurrentAnimatedZoomRunnable = new AnimatedZoomRunnable();
                    mCurrentAnimatedZoomRunnable.scale(mScaleFactor, newScale, mFocusX, mFocusY);
                    ViewCompat.postOnAnimation(ZoomLayout.this, mCurrentAnimatedZoomRunnable);
                } else {
                    mCurrentFlingRunnable = new FlingRunnable(getContext());
                    mCurrentFlingRunnable.fling((int) velocityX, (int) velocityY);
                    post(mCurrentFlingRunnable);
                }

                return true;
            }
            return false;
        }

    }

    private void cancelFling() {
        if (null != mCurrentFlingRunnable) {
            mCurrentFlingRunnable.cancelFling();
            mCurrentFlingRunnable = null;
        }
    }

    private void cancelZoom() {
        if (null != mCurrentAnimatedZoomRunnable) {
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
            mCurrentX = mTranslateArray[0] = detector.getFocusX();
            mCurrentY = mTranslateArray[1] = detector.getFocusY();
            screenPointsToScaledPoints(mTranslateArray);
            mFocusX = mTranslateArray[0];
            mFocusY = mTranslateArray[1];
            mZoomDispatcher.onZoomBegin(ZoomLayout.this, mScaleFactor, getViewPortRect());
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = mScaleFactor * detector.getScaleFactor();
            moveBy(mCurrentX - detector.getFocusX(), mCurrentY - detector.getFocusY());
            internalScale(scale, mFocusX, mFocusY);
            mCurrentX = detector.getFocusX();
            mCurrentY = detector.getFocusY();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mZoomDispatcher.onZoomEnd(ZoomLayout.this, mScaleFactor, getViewPortRect());
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
        return mScaleFactor;
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
        return !NumberUtils.isEqual(mScaleFactor, 1.0f, 0.05f);
    }

    public void setScale(float scale, float focusX, float focusY, boolean animate) {
        if (!mAllowOverScale) {
            scale = Math.max(mMinScale, Math.min(scale, mMaxScale));
        }
        if (animate) {
            mCurrentAnimatedZoomRunnable = new AnimatedZoomRunnable();
            mCurrentAnimatedZoomRunnable.scale(mScaleFactor, scale, focusX, focusY);
            ViewCompat.postOnAnimation(ZoomLayout.this, mCurrentAnimatedZoomRunnable);
        } else {
            internalScale(scale, focusX, focusY);
        }
    }

    private void internalScale(float scale, float focusX, float focusY) {
        mScaleFactor = scale;
        mFocusX = focusX;
        mFocusY = focusY;
        mScaleMatrix.setScale(mScaleFactor, mScaleFactor, mFocusX, mFocusY);
        mScaleMatrix.invert(mScaleMatrixInverse);
        updateViewRects();
        mZoomDispatcher.onZoom(ZoomLayout.this, mScaleFactor, getViewPortRect());
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

        mTranslateMatrix.setTranslate(-posX, -posY);
        mTranslateMatrix.invert(mTranslateMatrixInverse);
        updateViewRects();

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
        mTranslateMatrix.setTranslate(-mPosX, -mPosY);
        mTranslateMatrix.invert(mTranslateMatrixInverse);
        updateViewRects();
        mPanDispatcher.onPan(ZoomLayout.this, getViewPortRect());
        invalidate();
    }

    private void updateViewRects() {
        mDrawingRect.set(0, 0, getChildViewWidth(), getChildViewHeight());
        scaledPointsToScreenPoints(mDrawingRect);
        mViewPortRect.set(0, 0, getWidth(), getHeight());
        screenPointsToScaledPoints(mViewPortRect);
    }

    private class AnimatedZoomRunnable implements Runnable {

        boolean mPerformScale = false;
        boolean mPerformTranslate = false;
        boolean mCancel = false;

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
            mZoomDispatcher.onZoomBegin(ZoomLayout.this, mScaleFactor, getViewPortRect());
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
            mPanDispatcher.onPanBegin(ZoomLayout.this, getViewPortRect());
            return this;
        }

        boolean translate() {
            return mPerformTranslate;
        }

        void cancel() {
            if (mPerformScale) {
                mZoomDispatcher.onZoomEnd(ZoomLayout.this, mScaleFactor, getViewPortRect());
            }
            if (mPerformTranslate) {
                mPanDispatcher.onPanEnd(ZoomLayout.this, getViewPortRect());
            }
            mCancel = true;
        }

        @Override
        public void run() {

            if (mCancel || (!mPerformScale && !mPerformTranslate)) {
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
                if (mPerformScale) {
                    mZoomDispatcher.onZoomEnd(ZoomLayout.this, mScaleFactor, getViewPortRect());
                }
                if (mPerformTranslate) {
                    mPanDispatcher.onPanEnd(ZoomLayout.this, getViewPortRect());
                }
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

        FlingRunnable(Context context) {
            mScroller = ScrollerCompat.getScroller(context);
        }

        void cancelFling() {
            mScroller.forceFinished(true);
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
                mPanDispatcher.onPanBegin(ZoomLayout.this, getViewPortRect());
            }
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
                mPanDispatcher.onPanEnd(ZoomLayout.this, getViewPortRect());
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
        void onZoomBegin(ZoomLayout view, float scale, RectF viewPort);
        void onZoom(ZoomLayout view, float scale, RectF viewPort);
        void onZoomEnd(ZoomLayout view, float scale, RectF viewPort);
    }

    public interface OnSpreadChangedListener {
        void onChangeBegin(int currentLeftSpread, int currentCenterSpread, int currentRightSpread);
        void onChange(int currentLeftSpread, int currentCenterSpread, int currentRightSpread);
        void onChangeEnd(int currentLeftSpread, int currentCenterSpread, int currentRightSpread);
    }

    public interface OnPanListener {
        void onPanBegin(ZoomLayout view, RectF viewPort);
        void onPan(ZoomLayout view, RectF viewPort);
        void onPanEnd(ZoomLayout view, RectF viewPort);
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
        public void onZoomBegin(ZoomLayout view, float scale, RectF viewPort) {
            if (mCount++ > 0) {
//                throw new IllegalStateException("onZoomBegin() called twice without onZoomEnd()");
            }
            if (mZoomListener != null) {
                mZoomListener.onZoomBegin(view, scale, viewPort);
            }
        }

        @Override
        public void onZoom(ZoomLayout view, float scale, RectF viewPort) {
            if (mZoomListener != null) {
                mZoomListener.onZoom(view, scale, viewPort);
            }
        }

        @Override
        public void onZoomEnd(ZoomLayout view, float scale, RectF viewPort) {
            if (mCount-- < 0) {
//                throw new IllegalStateException("onZoomEnd() called twice without onZoomBegin()");
            }
            if (mZoomListener != null) {
                mZoomListener.onZoomEnd(view, scale, viewPort);
            }
        }
    }

    private class PanDispatcher implements OnPanListener {

        int mCount = 0;
        OnPanListener mPanListener;

        @Override
        public void onPanBegin(ZoomLayout view, RectF viewPort) {
            if (mCount++ > 0) {
                throw new IllegalStateException("onPanBegin() called twice without onPanEnd()");
            }
            if (mPanListener != null) {
                mPanListener.onPanBegin(ZoomLayout.this, getViewPortRect());
            }
        }

        @Override
        public void onPan(ZoomLayout view, RectF viewPort) {
            if (mPanListener != null) {
                mPanListener.onPan(ZoomLayout.this, getViewPortRect());
            }
        }

        @Override
        public void onPanEnd(ZoomLayout view, RectF viewPort) {
            if (mCount-- < 0) {
                throw new IllegalStateException("onPanEnd() called twice without onPanBegin()");
            }
            if (mPanListener != null) {
                mPanListener.onPanEnd(ZoomLayout.this, getViewPortRect());
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
        L.d(TAG, String.format("%s: mScaleMatrix            %s", tag, mScaleMatrix.toString()));
        L.d(TAG, String.format("%s: mScaleMatrixInverse     %s", tag, mScaleMatrixInverse.toString()));
        L.d(TAG, String.format("%s: mTranslateMatrix        %s", tag, mTranslateMatrix.toString()));
        L.d(TAG, String.format("%s: mTranslateMatrixInverse %s", tag, mTranslateMatrixInverse.toString()));
        L.d(TAG, String.format("%s: mDrawMatrix             %s", tag, mDrawMatrix.toString()));
    }

    private void printViewRects(String tag) {
        printViewRect(tag, "ViewRect", mViewPortRect);
        printViewRect(tag, "DrawRect", mDrawingRect);
    }

    private static final String RECT_FORMAT = "%s: %s [ %.0f, %.0f, %.0f, %.0f ], w:%s, h%s";
    private void printViewRect(String tag, String name, RectF r) {
        L.d(TAG, String.format(Locale.US, RECT_FORMAT, tag, name, r.left, r.top, r.right, r.bottom, mViewPortRect.width(), mViewPortRect.height()));
    }

}
