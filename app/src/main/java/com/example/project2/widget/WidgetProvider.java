package com.example.project2.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.project2.R;
import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;

import java.util.Arrays;

public class WidgetProvider extends AppWidgetProvider {

    private static final String PREFS_NAME = "widget_prefs";
    private static final String KEY_CATEGORY = "widget_category_";

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Настройка адаптера
        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra("appWidgetId", appWidgetId);
        intent.setData(Uri.parse("content://com.example.project2/widget/" + appWidgetId));
        views.setRemoteAdapter(R.id.widget_list, intent);

        // Шаблон для кликов по элементам
        //Intent clickIntent = new Intent(context, WidgetLaunchActivity.class);
        //PendingIntent clickPendingIntent = PendingIntent.getActivity(
        //        context, appWidgetId, clickIntent,
        //        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        //);
        //views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent);

        // Заголовок
        String category = getWidgetCategory(context, appWidgetId);
        String title = getWidgetTitle(context, category);
        views.setTextViewText(R.id.widget_header, title);

        // Конфигурация по клику на заголовок
        Intent configureIntent = WidgetConfigureActivity.createIntent(context, appWidgetId);
        configureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent configurePendingIntent = PendingIntent.getActivity(
                context, appWidgetId, configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_header, configurePendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);
    }

    private static String getWidgetCategory(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CATEGORY + appWidgetId, "All");
    }

    private static String getWidgetTitle(Context context, String category) {
        if (category.startsWith("user_")) {
            int id = Integer.parseInt(category.substring(5));
            Category cat = CategoryManager.getInstance(context).getCategory(id);
            if (cat != null) {
                return cat.getName();
            } else {
                return "All";
            }
        }
        return category;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("WidgetProvider", "onUpdate, ids: " + Arrays.toString(appWidgetIds));
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, WidgetProvider.class)
        );
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}