package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;
import com.shopgun.android.zoomlayout.sample.utils.SimpleZoomPanLog;

public class MultipleViewActivity extends AppCompatActivity {

    public static final String TAG = MultipleViewActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiple_view);

        ZoomLayout zoomLayout = (ZoomLayout) findViewById(R.id.zoomLayout);
        TextView textView = (TextView) findViewById(R.id.info);
        SimpleZoomPanLog log = new SimpleZoomPanLog(TAG, textView);
        zoomLayout.setOnPanListener(log);
        zoomLayout.setOnZoomListener(log);

    }
}
