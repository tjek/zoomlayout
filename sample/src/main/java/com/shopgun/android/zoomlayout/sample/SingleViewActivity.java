package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;
import com.shopgun.android.zoomlayout.sample.utils.SimpleZoomPanLog;

public class SingleViewActivity extends AppCompatActivity {

    public static final String TAG = SingleViewActivity.class.getSimpleName();
    
    ZoomLayout mZoomLayout;
    TextView mTextView;
    ImageView mImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_singleview);

        mZoomLayout = (ZoomLayout) findViewById(R.id.zoomLayout);
        mTextView = (TextView) findViewById(R.id.info);
        mImageView = (ImageView) findViewById(R.id.imageViewSingle);

        // setup ZoomLayout
        SimpleZoomPanLog log = new SimpleZoomPanLog(TAG, mTextView);
        mZoomLayout.setOnPanListener(log);
        mZoomLayout.setOnZoomListener(log);

    }

}
