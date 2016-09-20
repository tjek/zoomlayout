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
    }

    @Override
    public void onPanBegin(ZoomLayout view, RectF viewPort) {
        log("onPanBegin", view);
    }

    @Override
    public void onPan(ZoomLayout view, RectF viewPort) {
        log("onPan", view);
    }

    @Override
    public void onPanEnd(ZoomLayout view, RectF viewPort) {
        log("onPanEnd", view);
    }

    @Override
    public void onZoomBegin(ZoomLayout view, float scale, RectF viewPort) {
        log("onZoomBegin", view);
    }

    @Override
    public void onZoom(ZoomLayout view, float scale, RectF viewPort) {
        log("onZoom", view);
    }

    @Override
    public void onZoomEnd(ZoomLayout view, float scale, RectF viewPort) {
        log("onZoomEnd", view);
    }

    public static final String FORMAT =
            "%s - scale:%.2f\n" +
            "ViewPort %s\n" +
            "DrawRect %s";

    private void log(String msg, ZoomLayout view) {
        String text = String.format(Locale.US, FORMAT, msg, view.getScale(), r(view.getViewPortRect()), r(view.getDrawingRect()));
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
