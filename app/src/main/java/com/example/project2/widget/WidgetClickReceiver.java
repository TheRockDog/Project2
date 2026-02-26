package com.example.project2.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class WidgetClickReceiver extends BroadcastReceiver {
    public static final String ACTION_APP_LAUNCH = "com.example.project2.ACTION_APP_LAUNCH";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_APP_LAUNCH.equals(intent.getAction())) {
            String packageName = intent.getStringExtra("package");
            Log.d("WidgetClick", "Получен клик по пакету: " + packageName);
            if (packageName != null) {
                try {
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(launchIntent);
                    } else {
                        Toast.makeText(context, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}