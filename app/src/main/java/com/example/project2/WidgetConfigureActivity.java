package com.example.project2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class WidgetConfigureActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "widget_prefs";
    private static final String KEY_CATEGORY = "widget_category_";

    private int appWidgetId;
    private RadioGroup categoryGroup;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_configure);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
            );
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        categoryGroup = findViewById(R.id.category_group);
        saveButton = findViewById(R.id.save_button);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedCategory = prefs.getString(KEY_CATEGORY + appWidgetId, "All");

        switch (savedCategory) {
            case "Games":
                categoryGroup.check(R.id.radio_games);
                break;
            case "Social":
                categoryGroup.check(R.id.radio_social);
                break;
            case "Work":
                categoryGroup.check(R.id.radio_work);
                break;
            default:
                categoryGroup.check(R.id.radio_all);
                break;
        }

        saveButton.setOnClickListener(v -> {
            String category = "All";
            int selectedId = categoryGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.radio_games) {
                category = "Games";
            } else if (selectedId == R.id.radio_social) {
                category = "Social";
            } else if (selectedId == R.id.radio_work) {
                category = "Work";
            }

            // Сохранение
            prefs.edit()
                    .putString(KEY_CATEGORY + appWidgetId, category)
                    .apply();

            // Обновление виджета
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            WidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        });
    }

    public static Intent createIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(context, WidgetConfigureActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return intent;
    }
}