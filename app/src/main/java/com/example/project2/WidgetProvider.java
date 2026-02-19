package com.example.project2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra("appWidgetId", appWidgetId);
        views.setRemoteAdapter(R.id.widget_list, intent);

        Intent clickIntent = new Intent(context, WidgetClickReceiver.class);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent);

        String category = getWidgetCategory(context, appWidgetId);
        String title = getWidgetTitle(category);
        views.setTextViewText(R.id.widget_header, title);

        Intent configureIntent = WidgetConfigureActivity.createIntent(context, appWidgetId);
        PendingIntent configurePendingIntent = PendingIntent.getActivity(
                context, appWidgetId, configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_header, configurePendingIntent); // Исправлено

        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);
    }

    private static String getWidgetCategory(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE);
        return prefs.getString("widget_category_" + appWidgetId, "All");
    }

    private static String getWidgetTitle(String category) {
        switch (category) {
            case "Games": return "Игры";
            case "Social": return "Соцсети";
            case "Work": return "Работа";
            default: return "Все приложения";
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
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