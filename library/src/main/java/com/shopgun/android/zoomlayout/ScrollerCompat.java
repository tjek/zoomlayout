package com.shopgun.android.zoomlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.widget.OverScroller;
import android.widget.Scroller;

public abstract class ScrollerCompat {

    public static ScrollerCompat getScroller(Context context) {
        if (VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            return new PreGingerScroller(context);
        } else {
            return new GingerScroller(context);
        }
    }

//    private int mPrevX;
//    private int mPrevY;
//
//    private int getPrevX() {
//        return mPrevX;
//    }
//
//    public void setPrevX(int prevX) {
//        mPrevX = prevX;
//    }
//
//    public int getPrevY() {
//        return mPrevY;
//    }
//
//    public void setPrevY(int prevY) {
//        mPrevY = prevY;
//    }

    public abstract boolean computeScrollOffset();

    public abstract void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY,
                               int maxY, int overX, int overY);

    public abstract void forceFinished(boolean finished);

    public abstract boolean isFinished();

    public abstract int getCurrX();

    public abstract int getCurrY();

    private static class PreGingerScroller extends ScrollerCompat {

        Scroller mScroller;

        public PreGingerScroller(Context context) {
            mScroller = new Scroller(context);
        }

        @Override
        public boolean computeScrollOffset() {

            return mScroller.computeScrollOffset();
        }

        @Override
        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY, int overX, int overY) {
//            setPrevX(startX);
//            setPrevY(startY);
            mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
        }

        @Override
        public void forceFinished(boolean finished) {
            mScroller.forceFinished(finished);
        }

        @Override
        public boolean isFinished() {
            return mScroller.isFinished();
        }

        @Override
        public int getCurrX() {
            return mScroller.getCurrX();
        }

        @Override
        public int getCurrY() {
            return mScroller.getCurrY();
        }

    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static class GingerScroller extends ScrollerCompat {

        OverScroller mScroller;

        public GingerScroller(Context context) {
            mScroller = new OverScroller(context);
        }

        @Override
        public boolean computeScrollOffset() {
            return mScroller.computeScrollOffset();
        }

        @Override
        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY, int overX, int overY) {
//            setPrevX(startX);
//            setPrevY(startY);
            mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
        }

        @Override
        public void forceFinished(boolean finished) {
            mScroller.forceFinished(finished);
        }

        @Override
        public boolean isFinished() {
            return mScroller.isFinished();
        }

        @Override
        public int getCurrX() {
            return mScroller.getCurrX();
        }

        @Override
        public int getCurrY() {
            return mScroller.getCurrY();
        }

    }

}
