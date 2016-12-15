package com.shopgun.android.zoomlayout.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;
import com.shopgun.android.zoomlayout.ZoomOnDoubleTapListener;
import com.shopgun.android.zoomlayout.sample.utils.Catalog;
import com.shopgun.android.zoomlayout.sample.utils.SimpleZoomPanLog;

public class ScaledImageViewActivity extends AppCompatActivity {

    public static final String TAG = ScaledImageViewActivity.class.getSimpleName();
    
    ZoomLayout mZoomLayout;
    TextView mTextView;
    ScaledImageView mImageView;
    Catalog mCatalog = Catalog.getNetto();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scaled_imageview);

        mZoomLayout = (ZoomLayout) findViewById(R.id.zoomLayout);
        mTextView = (TextView) findViewById(R.id.info);
        mImageView = (ScaledImageView) findViewById(R.id.imageViewScaled);

        loadImageFromBitmap();

        // setup ZoomLayout
        SimpleZoomPanLog log = new SimpleZoomPanLog(TAG, mTextView);
        log.setLogger(mZoomLayout);

        mZoomLayout.setMinScale(1f);
        mZoomLayout.setMaxScale(4f);
        mZoomLayout.addOnDoubleTapListener(new ZoomOnDoubleTapListener(false));

    }

    private void loadImageFromBitmap() {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.boat, options);
        float w = (float) b.getWidth();
        float h = (float) b.getHeight();
        float aspectRatio = w/h;
//        L.d(TAG, String.format(Locale.US, "Bitmap[ %.0f, %.0f ], aspectRatio: %.2f", w, h, aspectRatio));
        mImageView.setImageAspectRatio(aspectRatio);
        mImageView.setImageBitmap(b);

    }

}
