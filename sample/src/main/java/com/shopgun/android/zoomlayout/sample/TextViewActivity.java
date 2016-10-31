package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;
import com.shopgun.android.zoomlayout.sample.utils.SimpleZoomPanLog;

public class TextViewActivity extends AppCompatActivity {

    public static final String TAG = TextViewActivity.class.getSimpleName();
    
    ZoomLayout mZoomLayout;
    TextView mTextView;
    TextView mInfo;
    boolean mHorizontal = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textview);

        mZoomLayout = (ZoomLayout) findViewById(R.id.zoomLayout);
        mInfo = (TextView) findViewById(R.id.info);
        mTextView = (TextView) findViewById(R.id.textView);

        // setup ZoomLayout
        SimpleZoomPanLog log = new SimpleZoomPanLog(TAG, mInfo);
        log.setLogger(mZoomLayout);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Change Layout");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            mHorizontal = !mHorizontal;
            String text = mHorizontal ? "Hello, ZoomLayout!" : "H\ne\nl\nl\no\n,\n \nZ\no\no\nm\nL\na\ny\no\nu\nt\n!";
            mTextView.setText(text);
        }
        return super.onOptionsItemSelected(item);
    }

}
