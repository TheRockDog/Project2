package com.example.project2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.project2.models.AppInfo;
import com.example.project2.models.Category;
import com.example.project2.utils.AppManager;
import com.example.project2.utils.CategoryManager;

import java.util.ArrayList;
import java.util.List;

public class WidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final String PREFS_NAME = "widget_prefs";
    private static final String KEY_CATEGORY = "widget_category_";
    private Context context;
    private List<AppInfo> apps = new ArrayList<>();
    private int appWidgetId;
    private String categoryTag = "All";

    public WidgetFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra("appWidgetId", 0);
    }

    @Override
    public void onCreate() {
        loadData();
    }

    @Override
    public void onDataSetChanged() {
        loadData();
    }

    // Загружает данные в зависимости от выбранной категории
    private void loadData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.categoryTag = prefs.getString(KEY_CATEGORY + appWidgetId, "All");

        if (categoryTag.startsWith("user_")) {
            int categoryId = Integer.parseInt(categoryTag.substring(5));
            List<AppInfo> allApps = AppManager.loadAppsSync(context, "All", false);
            Category category = CategoryManager.getInstance(context).getCategory(categoryId);
            if (category != null) {
                List<AppInfo> filtered = new ArrayList<>();
                for (AppInfo app : allApps) {
                    if (category.containsPackage(app.getPackageName())) {
                        filtered.add(app);
                    }
                }
                this.apps = filtered;
            } else {
                this.apps = new ArrayList<>();
            }
        } else {
            this.apps = AppManager.loadAppsSync(context, categoryTag, false);
        }

        // Используем кэшированные иконки
        for (AppInfo app : apps) {
            Bitmap cachedIcon = AppManager.getCachedIcon(app.getPackageName());
            if (cachedIcon != null) {
                app.setCachedIcon(cachedIcon);
            }
        }
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= apps.size()) return null;

        AppInfo app = apps.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.item_widget_app);

        views.setTextViewText(R.id.widget_app_name, app.getAppName());

        Bitmap cachedIcon = app.getCachedIcon();
        if (cachedIcon != null) {
            views.setImageViewBitmap(R.id.widget_app_icon, cachedIcon);
        } else {
            views.setImageViewResource(R.id.widget_app_icon, android.R.drawable.sym_def_app_icon);
            loadIconAsync(app, views, position);
        }

        Intent fillIntent = new Intent();
        fillIntent.putExtra("package", app.getPackageName());
        views.setOnClickFillInIntent(R.id.widget_item_container, fillIntent);
        return views;
    }

    // Асинхронная загрузка иконки и обновление элемента
    private void loadIconAsync(AppInfo app, RemoteViews views, int position) {
        new Thread(() -> {
            Bitmap bitmap = AppManager.loadIconBitmap(context, app.getPackageName());
            if (bitmap != null) {
                app.setCachedIcon(bitmap);
                new Handler(Looper.getMainLooper()).post(() -> {
                    RemoteViews updatedViews = new RemoteViews(context.getPackageName(), R.layout.item_widget_app);
                    updatedViews.setImageViewBitmap(R.id.widget_app_icon, bitmap);
                    updatedViews.setTextViewText(R.id.widget_app_name, app.getAppName());

                    Intent fillIntent = new Intent();
                    fillIntent.putExtra("package", app.getPackageName());
                    updatedViews.setOnClickFillInIntent(R.id.widget_item_container, fillIntent);

                    AppWidgetManager.getInstance(context)
                            .partiallyUpdateAppWidget(appWidgetId, updatedViews);
                });
            }
        }).start();
    }

    @Override
    public RemoteViews getLoadingView() {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.item_widget_app);
        views.setTextViewText(R.id.widget_app_name, "Loading...");
        views.setImageViewResource(R.id.widget_app_icon, android.R.drawable.progress_indeterminate_horizontal);
        return views;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDestroy() {
        // Освобождение ресурсов при необходимости
    }
}