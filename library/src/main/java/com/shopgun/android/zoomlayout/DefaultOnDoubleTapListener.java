package com.shopgun.android.zoomlayout;

import android.view.MotionEvent;

public class DefaultOnDoubleTapListener implements android.view.GestureDetector.OnDoubleTapListener {

    private final ZoomLayout mZoomLayout;
    private final float mMediumScale;

    public DefaultOnDoubleTapListener(ZoomLayout zoomLayout) {
        mZoomLayout = zoomLayout;
        mMediumScale = zoomLayout.getMinScale() + ((zoomLayout.getMaxScale() - zoomLayout.getMinScale()) * 0.3f);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent ev) {
        try {
            float scale = mZoomLayout.getScale();
            float x = ev.getX();
            float y = ev.getY();

            if (scale < mMediumScale) {
                mZoomLayout.setScale(mMediumScale, x, y, true);
            } else if (scale >= mMediumScale && scale < mZoomLayout.getMaxScale()) {
                mZoomLayout.setScale(mZoomLayout.getMaxScale(), x, y, true);
            } else {
                mZoomLayout.setScale(mZoomLayout.getMinScale(), x, y, true);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // Can sometimes happen when getX() and getY() is called
        }

        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // Wait for the confirmed onDoubleTap() instead
        return false;
    }

}
