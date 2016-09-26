package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;
import com.shopgun.android.zoomlayout.sample.utils.Catalog;
import com.shopgun.android.zoomlayout.sample.utils.SimpleZoomPanLog;
import com.squareup.picasso.Picasso;

public class DoubleImageViewActivity extends AppCompatActivity {

    public static final String TAG = DoubleImageViewActivity.class.getSimpleName();
    
    ZoomLayout mZoomLayout;
    TextView mTextView;

    Catalog mCatalog = Catalog.getNetto();
    ImageView mLeft;
    ImageView mRight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_double_imageview);

        mZoomLayout = (ZoomLayout) findViewById(R.id.zoomLayout);
        mTextView = (TextView) findViewById(R.id.info);

        SimpleZoomPanLog log = new SimpleZoomPanLog(TAG, mTextView);
        log.setLogger(mZoomLayout);

        // Load content to zoom
        mLeft = (ImageView) findViewById(R.id.imageViewLeft);
        mRight = (ImageView) findViewById(R.id.imageViewRight);
        Picasso.with(this).load(mCatalog.getView(26)).into(mLeft);
        Picasso.with(this).load(mCatalog.getView(27)).into(mRight);

    }

}
