package com.shopgun.android.zoomlayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.shopgun.android.utils.UnitUtils;

import java.util.Locale;

public class ZoomUtils {

    private ZoomUtils() {
        // private
    }

    private static final String MATRIX_BACIS_FORMAT = "[ scale:%.2f, x:%.0f, y:%.0f ]";
    private static final String MATRIX_FORMAT = "[ %.2f, %.2f, %.2f ][ %.2f, %.2f, %.2f ][ %.2f, %.2f, %.2f ]";
    private static final float[] v = new float[9];
    private static final String RECT_FORMAT = "%s: %s [ %.0f, %.0f, %.0f, %.0f ], w:%s, h:%s";

    public static String getMatrixBasicInfo(Matrix m) {
        m.getValues(v);
        return String.format(Locale.US, MATRIX_BACIS_FORMAT, v[Matrix.MSCALE_X], v[Matrix.MTRANS_X], v[Matrix.MTRANS_Y]);
    }

    public static String getMatrixInfo(Matrix m) {
        m.getValues(v);
        return String.format(Locale.US, MATRIX_FORMAT, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
    }

    public static String getViewRectInfo(String tag, String name, RectF r) {
        return String.format(Locale.US, RECT_FORMAT, tag, name, r.left, r.top, r.right, r.bottom, r.width(), r.height());
    }

    private static Paint mDebugPaintBlue;
    private static Paint mDebugPaintWhite;
    private static Paint mDebugPaintYellow;
    private static Paint mDebugPaintRed;
    private static int mDebugRadius = 0;
    private static void ensureDebugOptions(Context context) {
        if (mDebugPaintBlue == null) {
            mDebugPaintWhite = new Paint();
            mDebugPaintWhite.setColor(Color.WHITE);
            mDebugPaintBlue = new Paint();
            mDebugPaintBlue.setColor(Color.BLUE);
            mDebugPaintYellow = new Paint();
            mDebugPaintYellow.setColor(Color.YELLOW);
            mDebugPaintRed = new Paint();
            mDebugPaintRed.setColor(Color.RED);
            mDebugRadius = UnitUtils.dpToPx(4, context);
        }
    }

    public static void debugDraw(Canvas canvas, Context context, float tx, float ty, float fx, float fy, float invScale) {
        ensureDebugOptions(context);
        int r = (int)((float)mDebugRadius * invScale);
        debugDrawCirc(canvas, tx, ty, r, mDebugPaintBlue);
        debugDrawCirc(canvas, 0, 0, r, mDebugPaintRed);
        debugDrawCirc(canvas, fx, fy, r, mDebugPaintYellow);
    }

    private static void debugDrawCirc(Canvas canvas, float cx, float cy, int r, Paint p) {
        canvas.drawCircle(cx, cy, r, mDebugPaintWhite);
        canvas.drawCircle(cx, cy, r/2, p);
    }

    /**
     * Round and set the values on the rectangle
     * @param rect the rectangle to set
     * @param array the array to read the values from
     */
    public static void setRect(Rect rect, float[] array) {
        setRect(rect, array[0], array[1], array[2], array[3]);
    }

    /**
     * Round and set the values on the rectangle
     * @param rect the rectangle to set
     * @param array the array to read the values from
     */
    public static void setRect(RectF rect, float[] array) {
        setRect(rect, array[0], array[1], array[2], array[3]);
    }

    /**
     * Round and set the values on the rectangle
     * @param rect the rectangle to set
     * @param l left
     * @param t top
     * @param r right
     * @param b bottom
     */
    public static void setRect(RectF rect, float l,  float t,  float r,  float b) {
        rect.set(Math.round(l), Math.round(t), Math.round(r), Math.round(b));
    }

    /**
     * Round and set the values on the rectangle
     * @param rect the rectangle to set
     * @param l left
     * @param t top
     * @param r right
     * @param b bottom
     */
    public static void setRect(Rect rect, float l, float t, float r, float b) {
        rect.set(Math.round(l), Math.round(t), Math.round(r), Math.round(b));
    }

    public static void setArray(float[] array, Rect rect) {
        array[0] = rect.left;
        array[1] = rect.top;
        array[2] = rect.right;
        array[3] = rect.bottom;
    }

    public static void setArray(float[] array, RectF rect) {
        array[0] = rect.left;
        array[1] = rect.top;
        array[2] = rect.right;
        array[3] = rect.bottom;
    }

}
