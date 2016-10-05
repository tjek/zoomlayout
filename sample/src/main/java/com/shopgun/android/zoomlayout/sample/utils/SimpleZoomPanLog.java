package com.shopgun.android.zoomlayout.sample.utils;

import android.graphics.RectF;
import android.widget.TextView;

import com.shopgun.android.utils.log.L;
import com.shopgun.android.zoomlayout.ZoomLayout;

import java.util.Locale;

public class SimpleZoomPanLog implements ZoomLayout.OnZoomListener,
        ZoomLayout.OnPanListener,
        ZoomLayout.OnTapListener,
        ZoomLayout.OnDoubleTapListener,
        ZoomLayout.OnLongTapListener {

    public static final String TAG = SimpleZoomPanLog.class.getSimpleName();
    String mTag;
    TextView mTextView;

    public SimpleZoomPanLog(String tag) {
        this(tag, null);
    }

    public SimpleZoomPanLog(String tag, TextView textView) {
        mTag = tag;
        mTextView = textView;
        log("init", 1.0f, 0f, 0f, "none", "none");
    }

    public void setLogger(ZoomLayout zoomLayout) {
        zoomLayout.DEBUG = true;
        zoomLayout.setOnPanListener(this);
        zoomLayout.setOnZoomListener(this);
        zoomLayout.setOnTapListener(this);
        zoomLayout.setOnDoubleTapListener(this);
        zoomLayout.setOnLongTapListener(this);
    }

    @Override
    public void onPanBegin(ZoomLayout view) {
        log("onPanBegin", view);
    }

    @Override
    public void onPan(ZoomLayout view) {
        log("onPan", view);
    }

    @Override
    public void onPanEnd(ZoomLayout view) {
        log("onPanEnd", view);
    }

    @Override
    public void onZoomBegin(ZoomLayout view, float scale) {
        log("onZoomBegin", view);
    }

    @Override
    public void onZoom(ZoomLayout view, float scale) {
        log("onZoom", view);
    }

    @Override
    public void onZoomEnd(ZoomLayout view, float scale) {
        log("onZoomEnd", view);
    }

    public static final String FORMAT =
            "%s - s:%.2f, x:%.0f, y:%.0f\n" +
            "Rect %s\n" +
            "DrawRect %s";

    private void log(String msg, ZoomLayout view) {
        log(msg, view.getScale(), view.getPosX(), view.getPosY(), r(view.getTranslateBounds()), r(view.getDrawRect()));
    }

    private void log(String msg, float scale, float x, float y, String bounds, String drawRect) {
        String text = String.format(Locale.US, FORMAT, msg, scale, x, y, bounds, drawRect);
        if (mTextView != null) {
            mTextView.setText(text);
        }
//        L.d(mTag, text.replace("\n", " - "));
    }

    public static final String RECT_FORMAT = "[ %.0f, %.0f, %.0f, %.0f ]";
    private String r(RectF r) {
        return String.format(Locale.US, RECT_FORMAT, r.left, r.top, r.right, r.bottom);
    }

    @Override
    public boolean onContentTap(ZoomLayout view, float absX, float absY, float relX, float relY) {
        log("onContentTap", absX, absY, relX, relY);
        return false;
    }

    @Override
    public boolean onViewTap(ZoomLayout view, float absX, float absY, float relX, float relY) {
        log("onViewTap", absX, absY, relX, relY);
        return false;
    }

    @Override
    public boolean onContentDoubleTap(ZoomLayout view, float absX, float absY, float relX, float relY) {
        log("onContentDoubleTap", absX, absY, relX, relY);
        return false;
    }

    @Override
    public boolean onViewDoubleTap(ZoomLayout view, float absX, float absY, float relX, float relY) {
        log("onViewDoubleTap", absX, absY, relX, relY);
        return false;
    }

    @Override
    public void onContentLongTap(ZoomLayout view, float absX, float absY, float relX, float relY) {
        log("onContentLongTap", absX, absY, relX, relY);
    }

    @Override
    public void onViewLongTap(ZoomLayout view, float absX, float absY, float relX, float relY) {
        log("onViewLongTap", absX, absY, relX, relY);
    }

    private void log(String msg, float absX, float absY, float relX, float relY) {
        L.d(TAG, String.format(Locale.US, "%s[ absX:%.0f, absY:%.0f, relX:%.0f, relY:%.0f ]", msg, absX, absY, relX, relY));
    }


}
