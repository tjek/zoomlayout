package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;
import com.shopgun.android.zoomlayout.ZoomOnDoubleTapListener;
import com.shopgun.android.zoomlayout.sample.utils.SimpleZoomPanLog;

public class SingleImageViewActivity extends AppCompatActivity {

    public static final String TAG = SingleImageViewActivity.class.getSimpleName();
    
    ZoomLayout mZoomLayout;
    TextView mTextView;
    ImageView mImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_single_imageview);

        mZoomLayout = (ZoomLayout) findViewById(R.id.zoomLayout);
        mTextView = (TextView) findViewById(R.id.info);
        mImageView = (ImageView) findViewById(R.id.imageViewSingle);

        // setup ZoomLayout
        SimpleZoomPanLog log = new SimpleZoomPanLog(TAG, mTextView);
        log.setLogger(mZoomLayout);

        mZoomLayout.setMinScale(1f);
        mZoomLayout.setMaxScale(4f);
        mZoomLayout.addOnDoubleTapListener(new ZoomOnDoubleTapListener(false));

    }

}
