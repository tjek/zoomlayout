package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.shopgun.android.zoomlayout.ZoomLayout;
import com.shopgun.android.zoomlayout.sample.utils.SimpleZoomPanLog;

public class ViewPagerActivity extends AppCompatActivity {

    public static final String TAG = ViewPagerActivity.class.getSimpleName();

    ViewPager mViewPager;
    TextView mTextView;
    static SimpleZoomPanLog mLogger;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);
        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mTextView = (TextView) findViewById(R.id.info);
        mLogger = new SimpleZoomPanLog(TAG, mTextView);
        mViewPager.setAdapter(new ZoomLayoutAdapter(getSupportFragmentManager()));
    }

    public static class ZoomLayoutAdapter extends FragmentStatePagerAdapter {

        public ZoomLayoutAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return ZoomLayoutFragment.newInstance(new int[] { R.drawable.irongate });
                case 1:
                    return ZoomLayoutFragment.newInstance(new int[] { R.drawable.irongate,  R.drawable.mill });
                case 2:
                    return ZoomLayoutFragment.newInstance(new int[] { R.drawable.boat });
                default:
                    throw new IllegalStateException("wat");
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

    }

    public static class ZoomLayoutFragment extends Fragment {

        int[] mImageIds;

        public static ZoomLayoutFragment newInstance(int[] ids) {
            Bundle b = new Bundle();
            b.putIntArray("images", ids);
            ZoomLayoutFragment fragment = new ZoomLayoutFragment();
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mImageIds = getArguments().getIntArray("images");
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

            View view;
            if (mImageIds.length > 1) {
                view = inflater.inflate(R.layout.imageview_double, container, false);
                ImageView left = (ImageView) view.findViewById(R.id.imageViewLeft);
                ImageView right = (ImageView) view.findViewById(R.id.imageViewRight);
                left.setImageResource(mImageIds[0]);
                right.setImageResource(mImageIds[1]);
            } else {
                view = inflater.inflate(R.layout.imageview_single, container, false);
                ImageView imageView = (ImageView) view.findViewById(R.id.imageViewSingle);
                imageView.setImageResource(mImageIds[0]);
            }

            ZoomLayout zoomLayout = (ZoomLayout) view.findViewById(R.id.zoomLayout);
            zoomLayout.setOnZoomListener(mLogger);
            zoomLayout.setOnPanListener(mLogger);

            return view;
        }
    }

}
