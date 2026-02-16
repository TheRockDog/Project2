package com.example.project2.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IconCache {
    private static LruCache<String, Bitmap> memoryCache;
    private PackageManager packageManager;
    private ExecutorService executor = Executors.newFixedThreadPool(3);

    public IconCache(Context context) {
        this.packageManager = context.getPackageManager();

        if (memoryCache == null) {
            int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            int cacheSize = maxMemory / 8; // Выделяем 1/8 памяти под кэш

            memoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };
        }
    }

    public Bitmap getIcon(String packageName) {
        return memoryCache.get(packageName);
    }

    public Bitmap loadIcon(String packageName) {
        Bitmap cached = memoryCache.get(packageName);
        if (cached != null) {
            return cached;
        }

        try {
            Drawable drawable = packageManager.getApplicationIcon(packageName);
            Bitmap bitmap = drawableToBitmap(drawable);
            if (bitmap != null) {
                memoryCache.put(packageName, bitmap);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        if (width <= 0 || height <= 0) {
            width = 96;
            height = 96;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public void preloadIcons(String[] packageNames) {
        for (String packageName : packageNames) {
            executor.execute(() -> loadIcon(packageName));
        }
    }

    public void clear() {
        memoryCache.evictAll();
    }
}