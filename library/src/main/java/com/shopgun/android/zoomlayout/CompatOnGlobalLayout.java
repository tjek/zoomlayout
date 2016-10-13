package com.shopgun.android.zoomlayout;

import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;

public class CompatOnGlobalLayout implements ViewTreeObserver.OnGlobalLayoutListener {

    private final View mView;
    private int mLeft, mTop, mRight, mBottom;
    private ArrayList<OnLayoutChangeListener> mOnLayoutChangeListener;

    public static CompatOnGlobalLayout listen(View view, OnLayoutChangeListener listener) {
        CompatOnGlobalLayout cogl = new CompatOnGlobalLayout(view, listener);
        view.getViewTreeObserver().addOnGlobalLayoutListener(cogl);
        return cogl;
    }

    public CompatOnGlobalLayout(View view, OnLayoutChangeListener listener) {
        mView = view;
        addOnLayoutChangeListener(listener);
    }

    /**
     * Add a listener that will be called when the bounds of the view change due to
     * layout processing.
     *
     * @param listener The listener that will be called when layout bounds change.
     */
    public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
        if (mOnLayoutChangeListener == null) {
            mOnLayoutChangeListener = new ArrayList<OnLayoutChangeListener>();
        }
        if (!mOnLayoutChangeListener.contains(listener)) {
            mOnLayoutChangeListener.add(listener);
        }
    }

    /**
     * Remove a listener for layout changes.
     *
     * @param listener The listener for layout bounds change.
     */
    public void removeOnLayoutChangeListener(OnLayoutChangeListener listener) {
        if (mOnLayoutChangeListener == null) {
            return;
        }
        mOnLayoutChangeListener.remove(listener);
    }

    @Override
    public void onGlobalLayout() {

        int oldL = mLeft;
        int oldT = mTop;
        int oldR = mRight;
        int oldB = mBottom;
        mLeft = mView.getLeft();
        mTop = mView.getTop();
        mRight = mView.getRight();
        mBottom = mView.getBottom();

        boolean changed = oldL != mLeft || oldT != mTop || oldR != mRight || oldB != mBottom;
        if (mOnLayoutChangeListener != null) {
            ArrayList<OnLayoutChangeListener> listenersCopy =
                    (ArrayList<OnLayoutChangeListener>)mOnLayoutChangeListener.clone();
            int numListeners = listenersCopy.size();
            for (int i = 0; i < numListeners; ++i) {
                listenersCopy.get(i).onLayoutChange(mView, changed, mLeft, mTop, mRight, mBottom, oldL, oldT, oldR, oldB);
            }
        }
    }

    /**
     * Interface definition for a callback to be invoked when the layout bounds of a view
     * changes due to layout processing.
     */
    public interface OnLayoutChangeListener {
        /**
         * Called when the layout bounds of a view changes due to layout processing.
         *
         * @param v The view whose bounds have changed.
         * @param changed True if the dimensions changed, else false
         * @param left The new value of the view's left property.
         * @param top The new value of the view's top property.
         * @param right The new value of the view's right property.
         * @param bottom The new value of the view's bottom property.
         * @param oldLeft The previous value of the view's left property.
         * @param oldTop The previous value of the view's top property.
         * @param oldRight The previous value of the view's right property.
         * @param oldBottom The previous value of the view's bottom property.
         */
        void onLayoutChange(View v, boolean changed, int left, int top, int right, int bottom,
                            int oldLeft, int oldTop, int oldRight, int oldBottom);
    }

}
