package com.shopgun.android.zoomlayout;

public class ZoomOnDoubleTapListener implements ZoomLayout.OnDoubleTapListener {

    private final ZoomLayout mZoomLayout;
    private final float mMediumScale;

    public ZoomOnDoubleTapListener(ZoomLayout zoomLayout) {
        mZoomLayout = zoomLayout;
        mMediumScale = zoomLayout.getMinScale() + ((zoomLayout.getMaxScale() - zoomLayout.getMinScale()) * 0.3f);
    }

    @Override
    public boolean onContentDoubleTap(ZoomLayout view, float absX, float absY, float relX, float relY) {
        try {
            float scale = mZoomLayout.getScale();
            if (scale < mMediumScale) {
                mZoomLayout.setScale(mMediumScale, absX, absY, true);
            } else if (scale >= mMediumScale && scale < mZoomLayout.getMaxScale()) {
                mZoomLayout.setScale(mZoomLayout.getMaxScale(), absX, absY, true);
            } else {
                mZoomLayout.setScale(mZoomLayout.getMinScale(), absX, absY, true);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // Can sometimes happen when getX() and getY() is called
        }

        return true;
    }

    @Override
    public boolean onViewDoubleTap(ZoomLayout view, float absX, float absY) {
        return false;
    }
}
