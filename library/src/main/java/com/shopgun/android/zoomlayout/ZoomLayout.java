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
import android.widget.Scroller;

import com.shopgun.android.utils.NumberUtils;
import com.shopgun.android.utils.UnitUtils;
import com.shopgun.android.utils.log.L;

@SuppressWarnings("unused")
public class ZoomLayout extends FrameLayout {

    public static final String TAG = ZoomLayout.class.getSimpleName();

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

    // Listeners
    private OnZoomListener mZoomListener;
    private OnPanListener mPanListener;
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
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mTranslateMatrix.setTranslate(0, 0);
        mScaleMatrix.setScale(1, 1);
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                    L.d(TAG, "onLayoutChange");
                }
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec) -
                ViewCompat.getPaddingStart(this) - ViewCompat.getPaddingEnd(this);
        int h = MeasureSpec.getSize(heightMeasureSpec) -
                getPaddingTop() - getPaddingBottom();

        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    Paint mPaintXY;
    Paint mPaintFocus;
    private void ensurePaint() {
        if (mPaintXY == null) {
            mPaintXY = new Paint();
            mPaintXY.setColor(Color.YELLOW);
            mPaintFocus = new Paint();
            mPaintFocus.setColor(Color.MAGENTA);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(-mPosX, -mPosY);
        canvas.scale(mScaleFactor, mScaleFactor, mFocusX, mFocusY);
        super.dispatchDraw(canvas);
        ensurePaint();
        canvas.drawCircle(-mPosX, -mPosY, UnitUtils.dpToPx(2, getContext()), mPaintXY);
        canvas.drawCircle(mFocusX, mFocusY, UnitUtils.dpToPx(2, getContext()), mPaintFocus);
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ensurePaint();
        canvas.drawCircle(-mPosX, -mPosY, UnitUtils.dpToPx(2, getContext()), mPaintXY);
        canvas.drawCircle(mFocusX, mFocusY, UnitUtils.dpToPx(2, getContext()), mPaintFocus);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        screenPointsToScaledPoints(ev);
        return super.dispatchTouchEvent(ev);
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
    public boolean onTouchEvent(MotionEvent ev) {
        scaledPointsToScreenPoints(ev);

        boolean handled = mScaleDetector.onTouchEvent(ev);
        handled = mGestureDetector.onTouchEvent(ev) || handled;

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP: {

                if (mCurrentFlingRunnable != null && !mCurrentFlingRunnable.isFinished()) {
                    break;
                }

                RectF viewRect = getViewPortRect();
                RectF drawRect = getDrawingRect();
                L.d(TAG, String.format("Scale:%s, mPosX:%s, mPosY:%s", mScaleFactor, mPosX, mPosY));
//                printRects();

                AnimatedZoomRunnable animatedZoomRunnable = new AnimatedZoomRunnable();

                float newScale = mScaleFactor < mMinScale ? mMinScale : (mMaxScale < mScaleFactor ? mMaxScale : mScaleFactor);
                boolean needScale = !NumberUtils.isEqual(newScale, mScaleFactor);

                // Find current bounds before applying scale to do the rest of the calculations

                if (viewRect.contains(drawRect) ||
                        (viewRect.height() > drawRect.height() &&
                                viewRect.width() > drawRect.width())) {

                    animatedZoomRunnable.scaleIfNeeded(mScaleFactor, newScale, (float)getWidth()/2, (float)getHeight()/2);
                    animatedZoomRunnable.translateIfNeeded(mPosX, mPosY, 0, 0);
                    handled = true;

                } else if (!drawRect.contains(viewRect)) {

                    if (needScale) {
                        // we'll need measurements from the new scale/position
                        mScaleMatrix.setScale(newScale, newScale);
                        mScaleMatrix.invert(mScaleMatrixInverse);
                        // update rects with the new scale
                        viewRect = getViewPortRect();
                        drawRect = getDrawingRect();
                    }

                    // TODO handle this case nicely - not just re-center
                    animatedZoomRunnable.translateIfNeeded(mPosX, mPosY, 0, 0);

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
                    }

                    handled = true;

                } else {

                    // The ViewPort is inside the DrawRect - no problem
//                    animatedZoomRunnable.scaleIfNeeded(mScaleFactor, newScale, getWidth()/2, getHeight()/2);

                }

                if (animatedZoomRunnable.scale() || animatedZoomRunnable.translate()) {
                    post(animatedZoomRunnable);
                    handled = true;
                }

                break;
            }

        }
        return handled;
    }

    private void printRects(String tag) {
        getViewPortRect();
        getDrawingRect();
        L.d(TAG, tag + ": " + String.format("ViewRect %s, w:%s, h%s", mViewPortRect.toString(), mViewPortRect.width(), mViewPortRect.height()));
        L.d(TAG, tag + ": " + String.format("DrawRect %s, w:%s, h%s", mDrawingRect.toString(), mDrawingRect.width(), mDrawingRect.height()));
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return e2.getPointerCount() == 1 && !mScaleDetector.isInProgress() && moveBy(distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (getDrawingRect().contains(getViewPortRect())) {
                float newScale = mScaleFactor < mMinScale ? mMinScale : (mMaxScale < mScaleFactor ? mMaxScale : mScaleFactor);
                boolean needScale = !NumberUtils.isEqual(newScale, mScaleFactor);
                if (needScale) {
                    AnimatedZoomRunnable animatedZoomRunnable = new AnimatedZoomRunnable();
                    animatedZoomRunnable.scale(mScaleFactor, newScale, mFocusX, mFocusY);
                    CompatUtils.postOnAnimation(ZoomLayout.this, animatedZoomRunnable);
                } else {
                    mCurrentFlingRunnable = new FlingRunnable(getContext());
                    mCurrentFlingRunnable.fling((int) velocityX, (int) velocityY);
                    post(mCurrentFlingRunnable);
                }

                return true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
//            return new DefaultOnDoubleTapListener(ZoomLayout.this).onDoubleTap(e);
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            cancelFling();
            return super.onDown(e);
        }

    }

    private void cancelFling() {
        if (null != mCurrentFlingRunnable) {
            mCurrentFlingRunnable.cancelFling();
            mCurrentFlingRunnable = null;
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

        }
    }


    /**
     * The rectangle representing the location of the view inside the ZoomView. including scale and translations.
     */
    private RectF getDrawingRect() {
        mDrawingRect.set(0, 0, getWidth(), getHeight());
        scaledPointsToScreenPoints(mDrawingRect);
        return mDrawingRect;
    }

    /**
     * The visible region of ZoomView.
     */
    private RectF getViewPortRect() {
        mViewPortRect.set(0,0,getWidth(), getHeight());
        screenPointsToScaledPoints(mViewPortRect);
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

    public void setScale(float scale, float focusX, float focusY, boolean animate) {
        if (!mAllowOverScale) {
            scale = Math.max(mMinScale, Math.min(scale, mMaxScale));
        }
        if (animate) {
            AnimatedZoomRunnable animatedZoomRunnable = new AnimatedZoomRunnable();
            post(animatedZoomRunnable.scale(mScaleFactor, scale, focusX, focusY));
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

        RectF view = getViewPortRect();
        RectF draw = getDrawingRect();
        float tmpX = mPosX;
        float tmpY = mPosY;
        boolean horizontalOutOfBounds = draw.left >= view.left || draw.right <= view.right;
        if (!horizontalOutOfBounds) {
            tmpX = posX;
        }
        boolean verticalOutOfBounds = draw.top >= view.top || draw.bottom <= view.bottom;
        if (!verticalOutOfBounds) {
            tmpY = posY;
        }

        internalMove(tmpX, tmpY);
        return !verticalOutOfBounds && !horizontalOutOfBounds;
    }

    private void internalMove(float posX, float posY) {
        mPosX = posX;
        mPosY = posY;
        mTranslateMatrix.setTranslate(-mPosX, -mPosY);
        mTranslateMatrix.invert(mTranslateMatrixInverse);
        invalidate();
    }

    private class AnimatedZoomRunnable implements Runnable {

        boolean mScale = false;
        boolean mTranslate = false;
        boolean mCancel = false;

        private float mFocalX, mFocalY;
        private long mStartTime;
        private float mZoomStart, mZoomEnd;
        private Float mXStart, mYStart;
        private Float mXEnd, mYEnd;

        public AnimatedZoomRunnable() {
            mStartTime = System.currentTimeMillis();
        }

        public AnimatedZoomRunnable scaleIfNeeded(float currentZoom, float targetZoom, float focalX, float focalY) {
            if (!NumberUtils.isEqual(currentZoom, targetZoom) ||
                    !NumberUtils.isEqual(focalX, ZoomLayout.this.mFocusX) ||
                    !NumberUtils.isEqual(focalY, ZoomLayout.this.mFocusY)) {
                scale(currentZoom, targetZoom, focalX, focalY);
            }
            return this;
        }

        public AnimatedZoomRunnable scale(float currentZoom, float targetZoom, float focalX, float focalY) {
            mScale = true;
            mFocalX = focalX;
            mFocalY = focalY;
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
            L.d(TAG, String.format("AnimatedZoomRunnable.Scale: %s -> %s", mZoomStart, mZoomEnd));
            return this;
        }

        public AnimatedZoomRunnable translateIfNeeded(float currentX, float currentY, float targetX, float targetY) {
            if (!NumberUtils.isEqual(currentX, targetX) ||
                    !NumberUtils.isEqual(currentY, targetY)) {
                translate(currentX, currentY, targetX, targetY);
            }
            return this;
        }

        public AnimatedZoomRunnable translate(float currentX, float currentY, float targetX, float targetY) {
            mTranslate = true;
            mXStart = currentX;
            mYStart = currentY;
            mXEnd = targetX;
            mYEnd = targetY;
            L.d(TAG, String.format("AnimatedZoomRunnable.Translate: x[%s -> %s], y[%s -> %s]", mXStart, mXEnd, mYStart, mYEnd));
            return this;
        }

        public void cancel() {
            mCancel = true;
        }

        public boolean scale() {
            return mScale;
        }

        public boolean translate() {
            return mTranslate;
        }

        @Override
        public void run() {
            if (mCancel || (!scale() && !translate())) {
                L.d(TAG, "No work needed");
                return;
            }

            float t = interpolate();

            if (scale()) {
                float scale = mZoomStart + t * (mZoomEnd - mZoomStart);
                internalScale(scale, mFocalX, mFocalY);
            }

            if (translate()) {
                float x = mXStart + t * (mXEnd - mXStart);
                float y = mYStart + t * (mYEnd - mYStart);
                internalMove(x, y);
            }

            // We haven't hit our target scale yet, so post ourselves again
            if (t < 1f) {
                CompatUtils.postOnAnimation(ZoomLayout.this, this);
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration;
            t = Math.min(1f, t);
            return mAnimationInterpolator.getInterpolation(t);
        }
    }

    private class FlingRunnable implements Runnable {

        private final Scroller mScroller;
        private int mCurrentX, mCurrentY;

        public FlingRunnable(Context context) {
            mScroller = new Scroller(getContext());
        }

        public void cancelFling() {
            mScroller.forceFinished(true);
        }

        public void fling(int velocityX, int velocityY) {

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
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
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

                moveBy(mCurrentX - newX, mCurrentY - newY);

                mCurrentX = newX;
                mCurrentY = newY;

                // Post On animation
                CompatUtils.postOnAnimation(ZoomLayout.this, FlingRunnable.this);
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
        return mZoomListener;
    }

    public void setOnZoomListener(OnZoomListener zoomListener) {
        mZoomListener = zoomListener;
    }

    public OnPanListener getOnPanListener() {
        return mPanListener;
    }

    public void setOnPanListener(OnPanListener panListener) {
        mPanListener = panListener;
    }

    public interface OnZoomListener {
        void onZoomBegin(ZoomLayout view, Rect viewPort);
        void onZoom(ZoomLayout view, Rect viewPort);
        void onZoomEnd(ZoomLayout view, Rect viewPort);
    }

    public interface OnPanListener {
        void onPanBegin(ZoomLayout view, Rect viewPort);
        void onPan(ZoomLayout view, Rect viewPort);
        void onPanEnd(ZoomLayout view, Rect viewPort);
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

}
