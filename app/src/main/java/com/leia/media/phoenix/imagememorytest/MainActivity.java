package com.leia.media.phoenix.imagememorytest;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Mike on 02.05.2015.
 */
public class MainActivity extends ActionBarActivity {

    private String memoryData = ""; // memory info to display
    private ListAdapter listAdapter; // adapter for listview with images
    private ArrayList<Integer> picsIds = new ArrayList<Integer>(); // array with images' res ids
    private LruCache<Integer, Bitmap> mMemoryCache; // memory cache
    private boolean scaleImage = true; // using this parameter when loading image from resources in adapter

    private static final String IDS_RESTORE = "savedArr"; // alias to get ids from saved bundle when activity recreated

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setting up memory cache size
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory());
        final int cacheSize = maxMemory / 4;

        RetainFragment retainFragment =
                RetainFragment.findOrCreateRetainFragment(getFragmentManager());
        mMemoryCache = retainFragment.mRetainedCache;
        if (mMemoryCache == null) {
            mMemoryCache = new LruCache<Integer, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(Integer key, Bitmap bitmap) {
                    return bitmap.getByteCount();
                }
            };
            retainFragment.mRetainedCache = mMemoryCache;
        }

        // checking bundle for saved ids array
        if (savedInstanceState != null) {
            picsIds = savedInstanceState.getIntegerArrayList(IDS_RESTORE);
        }

        listAdapter = new ListAdapter(this, picsIds, mMemoryCache);
        ((ListView) findViewById(R.id.listView)).setAdapter(listAdapter);

        ((CheckBox) findViewById(R.id.checkBox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scaleImage = isChecked;
            }
        });

        updateStats();
    }

    @Override
    protected void onDestroy() {
        listAdapter = null;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putIntegerArrayList(IDS_RESTORE, picsIds);
        super.onSaveInstanceState(outState);
    }

    /**
     * Method will add new sample image from resources to list view adapter
     */
    public void add(View view) {
        picsIds.add(R.raw.andersen);
        listAdapter.notifyDataSetChanged();
    }

    /**
     * Method will delete last element from list view adapter
     */
    public void delete(View view) {
        if (picsIds.size() > 0) {
            picsIds.remove(picsIds.size() - 1);
            mMemoryCache.remove(picsIds.size() - 1);
            mMemoryCache.evictAll(); // force cleaning
            listAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Method will refresh memory indicators
     */
    public void refresh(View view) {
        updateStats();
    }

    /**
     * Method will run garbage collection and refresh memory indicators
     */
    public void gc(View view) {
        Runtime.getRuntime().gc();
        updateStats();
    }

    /**
     * Method will refresh memory indicators
     */
    private void updateStats() {
        Runtime runtime = Runtime.getRuntime();
        memoryData = "Max memory: " + (runtime.maxMemory() / 1024) + "kb\n" +
                "Current memory: " + (runtime.totalMemory() / 1024) + "kb\n" +
                "Free memory in current: " + (runtime.freeMemory() / 1024) + "kb\n" +
                "Used memory: " + ((runtime.totalMemory() - runtime.freeMemory()) / 1024) + "kb\n" +
                "Total free memory: " + ((runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / 1024) + "kb";
        ((TextView) findViewById(R.id.textView2)).setText(memoryData);
    }

    public boolean isScaleImage() {
        return scaleImage;
    }

    /**
     * This fragment will help to retain cache and not create it from scratch, if screen rotates
     */
    public static class RetainFragment extends Fragment {
        private static final String TAG = "RetainFragment";
        public LruCache<Integer, Bitmap> mRetainedCache;

        public RetainFragment() {
        }

        public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
            RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = new RetainFragment();
                fm.beginTransaction().add(fragment, TAG).commit();
            }
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }

}
