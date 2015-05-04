package com.leia.media.phoenix.imagememorytest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Mike on 02.05.2015.
 */
public class ListAdapter extends ArrayAdapter<Integer> {

    private LayoutInflater inflater;
    private List<Integer> picsIds;
    private LruCache<Integer, Bitmap> mMemoryCache;

    public ListAdapter(Context context, List<Integer> picsIds, LruCache<Integer, Bitmap> cache) {
        super(context, R.layout.list_item, picsIds);
        this.picsIds = picsIds;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mMemoryCache = cache;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.list_item, parent, false);
        }
        ImageView image = (ImageView) view.findViewById(R.id.imageView);
        if (cancelPotentialDownload(image)) { // async loading
            ImageLoaderTask task = new ImageLoaderTask(image);
            LoadedDrawable loadedDrawable = new LoadedDrawable(task);
            image.setImageDrawable(loadedDrawable);
            task.execute(position);
        }
        return view;
    }

    private void addBitmapToMemoryCache(Integer key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(Integer key) {
        return mMemoryCache.get(key);
    }

    /**
     * Simple loader task, wraps ImageView into weak ref to ensure gc grabs it whenever
     */
    private class ImageLoaderTask extends AsyncTask<Integer, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewRef;

        public ImageLoaderTask(ImageView iv) {
            this.imageViewRef = new WeakReference<ImageView>(iv);
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            Integer key = params[0];
            Bitmap bitmap = getBitmapFromMemCache(key);
            if (bitmap == null) {
                if (((MainActivity) getContext()).isScaleImage()) {
                    bitmap = Utils.decodeSampledBitmapFromResource(getContext().getResources(), getItem(key),
                            100, 100); // set here wishing image size for aproximate scaling and display
                } else {
                    // loading raw image
                    bitmap = BitmapFactory.decodeResource(getContext().getResources(), getItem(key));
                }
                addBitmapToMemoryCache(key, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewRef != null) {
                ImageView imageView = imageViewRef.get();
                ImageLoaderTask imageLoaderTask = getBitmapLoaderTask(imageView);
                // Change bitmap only if this process is still associated with it
                if (this == imageLoaderTask) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    /**
     * Drawable wrapper for loading task to have possibility cancel it when corresponding view recycles
     */
    static class LoadedDrawable extends ColorDrawable {
        private final WeakReference<ImageLoaderTask> imageLoaderTaskReference;

        public LoadedDrawable(ImageLoaderTask imageLoaderTask) {
            super(Color.BLACK);
            imageLoaderTaskReference = new WeakReference<ImageLoaderTask>(imageLoaderTask);
        }

        public ImageLoaderTask getImageLoaderTask() {
            return imageLoaderTaskReference.get();
        }

    }

    private static boolean cancelPotentialDownload(ImageView imageView) {
        ImageLoaderTask imageLoaderTask = getBitmapLoaderTask(imageView);
        if (imageLoaderTask != null) {
            imageLoaderTask.cancel(true);
        }
        return true;
    }

    private static ImageLoaderTask getBitmapLoaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof LoadedDrawable) {
                LoadedDrawable downloadedDrawable = (LoadedDrawable) drawable;
                return downloadedDrawable.getImageLoaderTask();
            }
        }
        return null;
    }

}
