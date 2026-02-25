package com.example.project2.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project2.models.AppInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppManager {
    private static final String PREFS_NAME = "app_cache";
    private static final String KEY_APPS_LIST = "cached_apps";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final long CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000; // 24 часа
    private static final String ICON_DIR = "app_icons";

    private static final Map<String, List<AppInfo>> categoryCache = new HashMap<>();
    public static final ExecutorService executor = Executors.newFixedThreadPool(3); // Пул потоков
    private static LruCache<String, Bitmap> iconCache = new LruCache<>(500); // Кэш иконок
    private static CategoryManager categoryManager;
    private static List<AppInfo> cachedAllApps = null;
    private static long lastCacheUpdate = 0;
    private static boolean isInitialized = false;

    private static MutableLiveData<List<AppInfo>> allAppsLiveData = new MutableLiveData<>(); // LiveData для всех приложений

    public interface AppLoadCallback { void onLoaded(List<AppInfo> apps); }
    public interface IconsLoadCallback { void onIconsLoaded(); }

    // Инициализация менеджера
    public static void init(Context context) {
        if (isInitialized) return;
        if (categoryManager == null) categoryManager = CategoryManager.getInstance(context);
        loadCachedAppsFromPrefs(context);
        if (!isCacheValid()) refreshCacheAsync(context, null);
        else loadIconsFromFilesAsync(context, null);
        isInitialized = true;
    }

    // LiveData для наблюдения
    public static LiveData<List<AppInfo>> getAllAppsLiveData() {
        return allAppsLiveData;
    }

    // Проверка актуальности кэша
    public static boolean isCacheValid() {
        return cachedAllApps != null && (System.currentTimeMillis() - lastCacheUpdate) < CACHE_VALIDITY_MS;
    }

    // Загрузка кэша из SharedPreferences
    private static void loadCachedAppsFromPrefs(Context context) {
        String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_APPS_LIST, null);
        lastCacheUpdate = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_UPDATE, 0);
        if (json != null) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<List<AppInfo>>(){}.getType();
                List<AppInfo> apps = gson.fromJson(json, type);
                if (categoryManager != null) categoryManager.updateAppsWithUserCategories(apps);
                cachedAllApps = apps;
                allAppsLiveData.postValue(apps);
            } catch (Exception e) { e.printStackTrace(); cachedAllApps = null; }
        }
    }

    // Сохранение кэша
    private static void saveCachedAppsToPrefs(Context context, List<AppInfo> apps) {
        Gson gson = new Gson();
        String json = gson.toJson(apps);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_APPS_LIST, json)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply();
    }

    // Сохранение иконки в файл
    private static void saveIconToFile(Context context, String packageName, Bitmap bitmap) {
        File iconDir = new File(context.getFilesDir(), ICON_DIR);
        if (!iconDir.exists()) iconDir.mkdirs();
        File iconFile = new File(iconDir, packageName.replace('.', '_') + ".png");
        try (FileOutputStream fos = new FileOutputStream(iconFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // Загрузка иконки из файла
    private static Bitmap loadIconFromFile(Context context, String packageName) {
        File iconFile = new File(context.getFilesDir(), ICON_DIR + "/" + packageName.replace('.', '_') + ".png");
        return iconFile.exists() ? BitmapFactory.decodeFile(iconFile.getAbsolutePath()) : null;
    }

    // Асинхронная загрузка всех иконок из файлов
    public static void loadIconsFromFilesAsync(Context context, IconsLoadCallback callback) {
        if (cachedAllApps == null) { if (callback != null) callback.onIconsLoaded(); return; }
        executor.execute(() -> {
            for (AppInfo app : cachedAllApps) {
                String pkg = app.getPackageName();
                Bitmap bitmap = iconCache.get(pkg);
                if (bitmap == null) {
                    bitmap = loadIconFromFile(context, pkg);
                    if (bitmap != null) {
                        iconCache.put(pkg, bitmap);
                        app.setIcon(new BitmapDrawable(context.getResources(), bitmap));
                    }
                } else {
                    app.setIcon(new BitmapDrawable(context.getResources(), bitmap));
                }
            }
            if (callback != null) new Handler(Looper.getMainLooper()).post(callback::onIconsLoaded);
        });
    }

    // Синхронное сканирование всех приложений
    private static List<AppInfo> scanAllAppsSync(Context context, boolean loadIcons) {
        List<AppInfo> apps = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        for (ResolveInfo ri : resolveInfos) {
            try {
                String packageName = ri.activityInfo.packageName;
                String appName = ri.loadLabel(pm).toString();
                Drawable iconDrawable = null;
                Bitmap iconBitmap = null;
                if (loadIcons) {
                    iconDrawable = ri.loadIcon(pm);
                    if (iconDrawable instanceof BitmapDrawable) {
                        iconBitmap = ((BitmapDrawable) iconDrawable).getBitmap();
                    } else {
                        iconBitmap = Bitmap.createBitmap(iconDrawable.getIntrinsicWidth(),
                                iconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(iconBitmap);
                        iconDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        iconDrawable.draw(canvas);
                    }
                    saveIconToFile(context, packageName, iconBitmap);
                    iconCache.put(packageName, iconBitmap);
                }
                AppInfo app = new AppInfo(packageName, appName, iconDrawable);
                app.setAutoCategory(detectCategory(packageName, appName));
                apps.add(app);
            } catch (Exception e) { e.printStackTrace(); }
        }
        if (categoryManager != null) categoryManager.updateAppsWithUserCategories(apps);
        return apps;
    }

    // Обновление кэша в фоне
    public static void refreshCacheAsync(Context context, AppLoadCallback callback) {
        executor.execute(() -> {
            List<AppInfo> apps = scanAllAppsSync(context, true);
            cachedAllApps = apps;
            saveCachedAppsToPrefs(context, apps);
            categoryCache.clear();
            allAppsLiveData.postValue(apps);
            if (callback != null) new Handler(Looper.getMainLooper()).post(() -> callback.onLoaded(apps));
        });
    }

    // Получение всех приложений асинхронно
    public static void getAllAppsAsync(Context context, AppLoadCallback callback) {
        init(context);
        if (cachedAllApps != null) callback.onLoaded(new ArrayList<>(cachedAllApps));
        else refreshCacheAsync(context, callback);
    }

    // Получение приложений по категории асинхронно
    public static void getAppsByCategoryAsync(Context context, String category, AppLoadCallback callback) {
        getAllAppsAsync(context, apps -> {
            List<AppInfo> filtered = new ArrayList<>();
            if (category == null || category.equals("All")) filtered = apps;
            else {
                for (AppInfo app : apps) {
                    if (category.equals(app.getAutoCategory())) filtered.add(app);
                }
            }
            callback.onLoaded(filtered);
        });
    }

    // Получение приложений по пользовательской категории
    public static void getAppsByUserCategoryAsync(Context context, int categoryId, AppLoadCallback callback) {
        getAllAppsAsync(context, apps -> {
            List<AppInfo> filtered = new ArrayList<>();
            for (AppInfo app : apps) {
                if (app.isInUserCategory(categoryId)) filtered.add(app);
            }
            callback.onLoaded(filtered);
        });
    }

    // Синхронное получение приложений (для виджета)
    public static List<AppInfo> getAppsSync(Context context, String category) {
        init(context);
        if (cachedAllApps == null) {
            loadCachedAppsFromPrefs(context);
            if (cachedAllApps == null) cachedAllApps = scanAllAppsSync(context, true);
        }
        List<AppInfo> result = new ArrayList<>();
        if (category == null || category.equals("All")) result.addAll(cachedAllApps);
        else if (category.startsWith("user_")) {
            int catId = Integer.parseInt(category.substring(5));
            for (AppInfo app : cachedAllApps) if (app.isInUserCategory(catId)) result.add(app);
        } else {
            for (AppInfo app : cachedAllApps) if (category.equals(app.getAutoCategory())) result.add(app);
        }
        // Подгрузка иконок из кэша
        for (AppInfo app : result) {
            Bitmap cached = iconCache.get(app.getPackageName());
            if (cached != null) app.setIcon(new BitmapDrawable(context.getResources(), cached));
            else {
                Bitmap fromFile = loadIconFromFile(context, app.getPackageName());
                if (fromFile != null) {
                    iconCache.put(app.getPackageName(), fromFile);
                    app.setIcon(new BitmapDrawable(context.getResources(), fromFile));
                }
            }
        }
        return result;
    }

    // Загрузка иконки в Bitmap
    public static Bitmap loadIconBitmap(Context context, String packageName) {
        Bitmap cached = iconCache.get(packageName);
        if (cached != null) return cached;
        Bitmap fromFile = loadIconFromFile(context, packageName);
        if (fromFile != null) {
            iconCache.put(packageName, fromFile);
            return fromFile;
        }
        try {
            Drawable drawable = context.getPackageManager().getApplicationIcon(packageName);
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) bitmap = ((BitmapDrawable) drawable).getBitmap();
            else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            iconCache.put(packageName, bitmap);
            saveIconToFile(context, packageName, bitmap);
            return bitmap;
        } catch (Exception e) { return null; }
    }

    // Получить иконку из кэша
    public static Bitmap getCachedIcon(String packageName) {
        return iconCache.get(packageName);
    }

    // Есть ли кэшированные приложения
    public static boolean hasCachedApps() {
        return cachedAllApps != null;
    }

    // Определение категории по имени/пакету
    private static String detectCategory(String packageName, String appName) {
        String lower = (packageName + " " + appName).toLowerCase();
        if (lower.contains("game") || lower.contains("play") || lower.contains("casino") ||
                lower.contains("puzzle") || lower.contains("word") || lower.contains("match") ||
                lower.contains("arcade") || lower.contains("adventure") || lower.contains("rpg") ||
                lower.contains("strategy")) return "Games";
        if (lower.contains("facebook") || lower.contains("instagram") || lower.contains("twitter") ||
                lower.contains("tiktok") || lower.contains("snapchat") || lower.contains("telegram") ||
                lower.contains("whatsapp") || lower.contains("vk") || lower.contains("vkontakte") ||
                lower.contains("messenger") || lower.contains("discord") || lower.contains("reddit") ||
                lower.contains("linkedin")) return "Social";
        if (lower.contains("doc") || lower.contains("sheet") || lower.contains("slide") ||
                lower.contains("word") || lower.contains("excel") || lower.contains("pdf") ||
                lower.contains("office") || lower.contains("drive") || lower.contains("mail") ||
                lower.contains("outlook") || lower.contains("calendar") || lower.contains("note") ||
                lower.contains("task") || lower.contains("meeting") || lower.contains("zoom") ||
                lower.contains("teams")) return "Work";
        return "Other";
    }

    // Очистка кэша
    public static void clearCache() {
        cachedAllApps = null;
        categoryCache.clear();
        iconCache.evictAll();
    }
}