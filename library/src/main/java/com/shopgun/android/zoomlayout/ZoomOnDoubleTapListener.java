package com.shopgun.android.zoomlayout;

public class ZoomOnDoubleTapListener implements ZoomLayout.OnDoubleTapListener {

    private boolean mThreeStep = false;

    public ZoomOnDoubleTapListener(boolean threeStep) {
        mThreeStep = threeStep;
    }

    @Override
    public boolean onDoubleTap(ZoomLayout view, ZoomLayout.TapInfo info) {
        try {
            if (mThreeStep) {
                threeStep(view, info.getX(), info.getY());
            } else {
                twoStep(view, info.getX(), info.getY());
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // Can sometimes happen when getX() and getY() is called
        }
        return true;
    }

    private void twoStep(ZoomLayout view, float x, float y) {
        if (view.getScale() < view.getMaxScale()) {
            view.setScale(view.getMaxScale(), x, y, true);
        } else {
            view.setScale(view.getMinScale(), true);
        }
    }

    private void threeStep(ZoomLayout view, float x, float y) {
        float scale = view.getScale();
        float medium = view.getMinScale() + ((view.getMaxScale() - view.getMinScale()) * 0.3f);
        if (scale < medium) {
            view.setScale(medium, x, y, true);
        } else if (scale >= medium && scale < view.getMaxScale()) {
            view.setScale(view.getMaxScale(), x, y, true);
        } else {
            view.setScale(view.getMinScale(), true);
        }
    }

}
