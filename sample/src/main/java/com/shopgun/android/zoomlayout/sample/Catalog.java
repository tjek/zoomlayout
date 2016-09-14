package com.shopgun.android.zoomlayout.sample;

import java.util.Locale;

public class Catalog {

    public static Catalog getRema() {
        return new Catalog("063fSX3", 1171, 2008); // image sizes: 124x212, 586x1024, 1171x2008
    }

    public static Catalog getNetto() {
        return new Catalog("08cdIX3", 1525, 2008); // image sizes: 177x202, 762x1004, 1525x2008
    }

    public static Catalog getKvickly() {
        return new Catalog("10ab1h3", 1536, 1750); // image sizes: 161x212, 768x875, 1536x1750
    }

    public enum Size {

        THUMB("thumb"), VIEW("thumb"), ZOOM("thumb");

        final String mSize;

        Size(String size) {
            mSize = size;
        }

        @Override
        public String toString() {
            return mSize;
        }
    }

    public static final String URL = "https://akamai.shopgun.com/img/catalog/%s/%s-%s.jpg?m=ocbm9q";
    final String mId;
    final int mPageWidth;
    final int mPageHeight;

    public Catalog(String id, int pageWidth, int pageHeight) {
        mId = id;
        mPageWidth = pageWidth;
        mPageHeight = pageHeight;
    }

    public int getPageWidth() {
        return mPageWidth;
    }

    public String getPageUrl(Size size, int page) {
        return String.format(Locale.US, URL, size.toString(), mId, page);
    }

    public String getThumb(int page) {
        return getPageUrl(Size.THUMB, page);
    }

    public String getView(int page) {
        return getPageUrl(Size.VIEW, page);
    }

    public String getZoom(int page) {
        return getPageUrl(Size.ZOOM, page);
    }

}
