package com.shopgun.android.zoomlayout.sample;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
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
    static boolean mAllowParentInterceptOnScaled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);
        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mTextView = (TextView) findViewById(R.id.info);
        mLogger = new SimpleZoomPanLog(TAG, mTextView);
        mAllowParentInterceptOnScaled = true;
        mViewPager.setAdapter(new ZoomLayoutAdapter(getSupportFragmentManager()));
    }

    private static int[][] s_images = new int[][] {
        new int[] { R.drawable.irongate },
        new int[] { R.drawable.irongate,  R.drawable.mill },
        new int[] { R.drawable.boat },
        new int[] { R.drawable.mill, R.drawable.irongate },
        new int[] { R.drawable.irongate,  R.drawable.boat },
        new int[] { R.drawable.mill,  R.drawable.boat },
        new int[] { R.drawable.mill }
    };

    public static class ZoomLayoutAdapter extends FragmentStatePagerAdapter {

        public ZoomLayoutAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            int[] img = s_images[position%s_images.length];
            return ZoomLayoutFragment.newInstance(img);
        }

        @Override
        public int getCount() {
            return 14;
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
            zoomLayout.setAllowParentInterceptOnScaled(mAllowParentInterceptOnScaled);
            mLogger.setLogger(zoomLayout);

            return view;
        }
    }

}
