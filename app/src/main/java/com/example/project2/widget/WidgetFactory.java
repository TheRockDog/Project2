package com.example.project2.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.project2.R;
import com.example.project2.models.AppInfo;
import com.example.project2.utils.AppManager;

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
        Log.d("WidgetFactory", "onCreate for widget " + appWidgetId);
        loadData();
    }

    @Override
    public void onDataSetChanged() {
        Log.d("WidgetFactory", "onDataSetChanged for widget " + appWidgetId);
        loadData();
    }

    private void loadData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.categoryTag = prefs.getString(KEY_CATEGORY + appWidgetId, "All");

        List<AppInfo> allApps = AppManager.getAppsSync(context, categoryTag);

        for (AppInfo app : allApps) {
            Bitmap cachedIcon = AppManager.getCachedIcon(app.getPackageName());
            if (cachedIcon != null) {
                app.setCachedIcon(cachedIcon);
            }
        }

        apps.clear();
        apps.addAll(allApps);
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Log.d("WidgetFactory", "getViewAt position=" + position + ", apps.size=" + apps.size());
        if (position >= apps.size()) return null;

        AppInfo app = apps.get(position);
        Log.d("WidgetFactory", "app: " + app.getPackageName() + " - " + app.getAppName());

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.item_widget_app);
        views.setTextViewText(R.id.widget_app_name, app.getAppName());

        Bitmap cachedIcon = app.getCachedIcon();
        if (cachedIcon != null) {
            views.setImageViewBitmap(R.id.widget_app_icon, cachedIcon);
        } else {
            views.setImageViewResource(R.id.widget_app_icon, android.R.drawable.sym_def_app_icon);
            loadIconAsync(app, views, position);
        }

        // Прямой PendingIntent (без шаблона)
        Intent clickIntent = new Intent(context, WidgetLaunchActivity.class);
        clickIntent.putExtra("package", app.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, position, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_item_container, pendingIntent);

        return views;
    }

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
    public void onDestroy() {}
}