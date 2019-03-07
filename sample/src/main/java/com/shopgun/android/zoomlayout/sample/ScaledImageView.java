package com.shopgun.android.zoomlayout.sample;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class ScaledImageView extends AppCompatImageView {
    
    public static final String TAG = ScaledImageView.class.getSimpleName();

    float mImageAspectRatio;

    public ScaledImageView(Context context) {
        super(context);
    }

    public ScaledImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScaledImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setImageAspectRatio(float imageAspectRatio) {
        mImageAspectRatio = imageAspectRatio;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        float containerWidth = MeasureSpec.getSize(widthMeasureSpec);
        float containerHeight = MeasureSpec.getSize(heightMeasureSpec);
        float containerAspectRatio = containerWidth/containerHeight;

//        L.d(TAG, String.format(Locale.US, "MeasureSpec[ w:%.0f, h:%.0f, containerAspectRatio:%.2f ], pageAspectRatio:%.2f",
//                containerWidth, containerHeight, containerAspectRatio, mImageAspectRatio));

        if (mImageAspectRatio < containerAspectRatio) {
            containerWidth = containerHeight * mImageAspectRatio;
        } else if (mImageAspectRatio > containerAspectRatio) {
            containerHeight = containerWidth / mImageAspectRatio;
        }
        
        setMeasuredDimension((int) containerWidth, (int) containerHeight);

//        L.d(TAG, String.format(Locale.US, "Measured[ w:%s, h:%s ]", getMeasuredWidth(), getMeasuredHeight()));

    }

}
