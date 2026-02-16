package com.example.project2.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import com.example.project2.models.AppInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppManager {
    private static final Map<String, List<AppInfo>> cache = new HashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    // Кэш иконок
    private static LruCache<String, Bitmap> iconCache = new LruCache<>(200);
    private static CategoryManager categoryManager;

    public interface AppLoadCallback {
        void onLoaded(List<AppInfo> apps);
    }

    // Инициализация
    public static void init(Context context) {
        if (categoryManager == null) {
            categoryManager = CategoryManager.getInstance(context);
        }
    }

    // Асинхронная загрузка
    public static void getAllAppsAsync(Context context, AppLoadCallback callback) {
        getAppsByCategoryAsync(context, "All", callback);
    }

    // Асинхронная загрузка по категории
    public static void getAppsByCategoryAsync(Context context, String category, AppLoadCallback callback) {
        init(context);
        String cacheKey = category == null ? "All" : category;

        if (cache.containsKey(cacheKey)) {
            callback.onLoaded(new ArrayList<>(cache.get(cacheKey)));
            return;
        }

        executor.execute(() -> {
            List<AppInfo> apps = loadAppsSync(context, cacheKey, true);
            cache.put(cacheKey, apps);
            new Handler(Looper.getMainLooper()).post(() ->
                    callback.onLoaded(apps)
            );
        });
    }

    // Асинхронная загрузка по пользовательской категории
    public static void getAppsByUserCategoryAsync(Context context, int categoryId, AppLoadCallback callback) {
        init(context);

        executor.execute(() -> {
            List<AppInfo> allApps = loadAppsSync(context, "All", true);
            List<AppInfo> filteredApps = new ArrayList<>();

            for (AppInfo app : allApps) {
                if (app.isInUserCategory(categoryId)) {
                    filteredApps.add(app);
                }
            }

            new Handler(Looper.getMainLooper()).post(() ->
                    callback.onLoaded(filteredApps)
            );
        });
    }

    // Синхронная загрузка для виджета
    public static List<AppInfo> loadAppsSync(Context context, String category, boolean loadIcons) {
        init(context);
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
                if (loadIcons) {
                    iconDrawable = getIconWithCache(pm, packageName, ri);
                }

                AppInfo app = new AppInfo(packageName, appName, iconDrawable);

                // Автоматическое определение категории
                String autoCategory = detectCategory(packageName, appName);
                app.setAutoCategory(autoCategory);

                apps.add(app);
            } catch (Exception e) {
                // Пропуск проблемных приложений
                e.printStackTrace();
            }
        }

        // Информация о пользовательских категориях
        categoryManager.updateAppsWithUserCategories(apps);

        return apps;
    }

    public static List<AppInfo> loadAppsSync(Context context, String category) {
        return loadAppsSync(context, category, true);
    }

    // Загрузка иконок с кэшированием
    private static Drawable getIconWithCache(PackageManager pm, String packageName, ResolveInfo ri) {
        try {
            Bitmap cached = iconCache.get(packageName);
            if (cached != null) {
                return new BitmapDrawable(pm.getResourcesForApplication(packageName), cached);
            }

            // Загрузка и кэширование
            Drawable drawable = ri.loadIcon(pm);
            if (drawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                iconCache.put(packageName, bitmap);
            }
            return drawable;
        } catch (Exception e) {
            return ri.loadIcon(pm);
        }
    }

    // Автоматическое определение категории
    private static String detectCategory(String packageName, String appName) {
        String lower = (packageName + " " + appName).toLowerCase();

        // Игры
        if (lower.contains("game") ||
                lower.contains("play") ||
                lower.contains("casino") ||
                lower.contains("puzzle") ||
                lower.contains("word") ||
                lower.contains("match") ||
                lower.contains("arcade") ||
                lower.contains("adventure") ||
                lower.contains("rpg") ||
                lower.contains("strategy")) {
            return "Games";
        }

        // Социальные сети
        if (lower.contains("facebook") ||
                lower.contains("instagram") ||
                lower.contains("twitter") ||
                lower.contains("tiktok") ||
                lower.contains("snapchat") ||
                lower.contains("telegram") ||
                lower.contains("whatsapp") ||
                lower.contains("vk") ||
                lower.contains("vkontakte") ||
                lower.contains("messenger") ||
                lower.contains("discord") ||
                lower.contains("reddit") ||
                lower.contains("linkedin")) {
            return "Social";
        }

        // Работа
        if (lower.contains("doc") ||
                lower.contains("sheet") ||
                lower.contains("slide") ||
                lower.contains("word") ||
                lower.contains("excel") ||
                lower.contains("pdf") ||
                lower.contains("office") ||
                lower.contains("drive") ||
                lower.contains("mail") ||
                lower.contains("outlook") ||
                lower.contains("calendar") ||
                lower.contains("note") ||
                lower.contains("task") ||
                lower.contains("meeting") ||
                lower.contains("zoom") ||
                lower.contains("teams")) {
            return "Work";
        }

        return "Other";
    }

    // Получение статистики
    public static Map<String, Integer> getCategoryStats(Context context) {
        init(context);
        Map<String, Integer> stats = new HashMap<>();
        stats.put("All", 0);
        stats.put("Games", 0);
        stats.put("Social", 0);
        stats.put("Work", 0);
        stats.put("Other", 0);

        List<AppInfo> allApps = loadAppsSync(context, "All", false);
        stats.put("All", allApps.size());

        for (AppInfo app : allApps) {
            String category = app.getAutoCategory();
            stats.put(category, stats.getOrDefault(category, 0) + 1);
        }

        return stats;
    }

    // Очистка кэша
    public static void clearCache() {
        cache.clear();
        iconCache.evictAll();
    }

    // Предзагрузка иконок
    public static void preloadIcons(Context context, List<AppInfo> apps) {
        executor.execute(() -> {
            for (AppInfo app : apps) {
                String packageName = app.getPackageName();
                if (iconCache.get(packageName) == null) {
                    try {
                        Drawable drawable = context.getPackageManager().getApplicationIcon(packageName);
                        if (drawable instanceof BitmapDrawable) {
                            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                            iconCache.put(packageName, bitmap);
                        }
                    } catch (Exception e) {
                        // Игнор ошибок
                    }
                }
            }
        });
    }

    // Поиск приложений
    public static List<AppInfo> searchApps(Context context, String query, List<AppInfo> sourceList) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(sourceList);
        }

        String lowerQuery = query.toLowerCase();
        List<AppInfo> results = new ArrayList<>();

        for (AppInfo app : sourceList) {
            if (app.getAppName().toLowerCase().contains(lowerQuery) ||
                    app.getPackageName().toLowerCase().contains(lowerQuery)) {
                results.add(app);
            }
        }

        return results;
    }

    // Получение количества приложений в пользовательской категории
    public static int getAppCountInUserCategory(Context context, int categoryId) {
        init(context);
        List<AppInfo> allApps = loadAppsSync(context, "All", false);
        int count = 0;

        for (AppInfo app : allApps) {
            if (app.isInUserCategory(categoryId)) {
                count++;
            }
        }

        return count;
    }
}