package com.example.project2.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class WidgetLaunchActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("WidgetClick", "WidgetLaunchActivity запущена, package=" + getIntent().getStringExtra("package"));
        Log.d("WidgetClick", "Activity запущена");
        String packageName = getIntent().getStringExtra("package");
        Log.d("WidgetClick", "packageName = " + packageName);
        finish();
    }
}