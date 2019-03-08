package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
        log.setLogger(zoomLayout);

    }
}
