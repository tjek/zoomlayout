package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;
import com.squareup.picasso.Picasso;

public class MultiViewActivity extends AppCompatActivity {

    public static final String TAG = MultiViewActivity.class.getSimpleName();
    
    ZoomLayout mZoomLayout;
    TextView mTextView;

    Catalog mCatalog = Catalog.getNetto();
    ImageView mLeft;
    ImageView mRight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_multipleview);

        mZoomLayout = (ZoomLayout) findViewById(R.id.zoomLayout);
        mTextView = (TextView) findViewById(R.id.info);

        SimpleZoomPanLog log = new SimpleZoomPanLog(TAG, mTextView);
        mZoomLayout.setOnPanListener(log);
        mZoomLayout.setOnZoomListener(log);

        // Load content to zoom
        mLeft = (ImageView) findViewById(R.id.imageViewLeft);
        mRight = (ImageView) findViewById(R.id.imageViewRight);
        Picasso.with(this).load(mCatalog.getView(26)).into(mLeft);
        Picasso.with(this).load(mCatalog.getView(27)).into(mRight);

    }

}
