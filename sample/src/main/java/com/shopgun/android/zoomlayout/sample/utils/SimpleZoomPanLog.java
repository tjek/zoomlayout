package com.shopgun.android.zoomlayout.sample.utils;

import android.graphics.RectF;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;

import java.util.Locale;

public class SimpleZoomPanLog implements ZoomLayout.OnZoomListener, ZoomLayout.OnPanListener {

    String mTag;
    TextView mTextView;

    public SimpleZoomPanLog(String tag) {
        this(tag, null);
    }

    public SimpleZoomPanLog(String tag, TextView textView) {
        mTag = tag;
        mTextView = textView;
        log("init", 1.0f, 0f, 0f, "none", "none", "none");
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
            "ViewPort %s\n" +
            "DrawRect %s";

    private void log(String msg, ZoomLayout view) {
        log(msg, view.getScale(), view.getPosX(), view.getPosY(), r(view.getTranslateBounds()), r(view.getViewPortRect()), r(view.getDrawRect()));
    }

    private void log(String msg, float scale, float x, float y, String bounds, String vireRect, String drawRect) {
        String text = String.format(Locale.US, FORMAT, msg, scale, x, y, bounds, vireRect, drawRect);
        if (mTextView != null) {
            mTextView.setText(text);
        }
//        L.d(mTag, text);
    }

    public static final String RECT_FORMAT = "[ %.0f, %.0f, %.0f, %.0f ]";
    private String r(RectF r) {
        return String.format(Locale.US, RECT_FORMAT, r.left, r.top, r.right, r.bottom);
    }

}
