package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;
import com.shopgun.android.zoomlayout.sample.utils.SimpleZoomPanLog;

public class TextViewActivity extends AppCompatActivity {

    public static final String TAG = TextViewActivity.class.getSimpleName();
    
    ZoomLayout mZoomLayout;
    TextView mTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_textview);

        mZoomLayout = (ZoomLayout) findViewById(R.id.zoomLayout);
        mTextView = (TextView) findViewById(R.id.info);
        TextView mTv = (TextView) findViewById(R.id.textView);
//        mTv.setText("x\nx\nx\nx\nx\nx\nx\nx\nx\nx\nx\nx\nx\nx\nx\nx\n");

        // setup ZoomLayout
        SimpleZoomPanLog log = new SimpleZoomPanLog(TAG, mTextView);
        log.setLogger(mZoomLayout);

    }

}
