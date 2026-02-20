package com.example.project2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;

import java.util.List;

public class WidgetConfigureActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "widget_prefs";
    private static final String KEY_CATEGORY = "widget_category_";

    private int appWidgetId;
    private RadioGroup categoryGroup;
    private Button saveButton;
    private LinearLayout radioContainer;
    private CategoryManager categoryManager;

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

        categoryManager = CategoryManager.getInstance(this);
        radioContainer = findViewById(R.id.radio_container);
        saveButton = findViewById(R.id.save_button);

        buildRadioButtons();

        saveButton.setOnClickListener(v -> {
            int selectedId = categoryGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Выберите категорию", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selected = findViewById(selectedId);
            String tag = (String) selected.getTag();

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_CATEGORY + appWidgetId, tag)
                    .apply();

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            WidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        });
    }

    // Создание радиокнопок
    private void buildRadioButtons() {
        categoryGroup = new RadioGroup(this);
        categoryGroup.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        categoryGroup.setOrientation(LinearLayout.VERTICAL);

        // Стандартные
        addRadioButton("All", "All");
        addRadioButton("Games", "Games");
        addRadioButton("Social", "Social");
        addRadioButton("Work", "Work");
        addRadioButton("Other", "Other");

        // Пользовательские
        List<Category> userCategories = categoryManager.getAllCategories();
        for (Category cat : userCategories) {
            if (!cat.isBuiltIn()) {
                addRadioButton(cat.getName(), "user_" + cat.getId());
            }
        }

        radioContainer.addView(categoryGroup);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedTag = prefs.getString(KEY_CATEGORY + appWidgetId, "All");
        setCheckedRadioButton(savedTag);
    }

    // Добавление одной радиокнопки
    private void addRadioButton(String text, String tag) {
        RadioButton radio = new RadioButton(this);
        radio.setText(text);
        radio.setTag(tag);
        radio.setLayoutParams(new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
        ));
        radio.setPadding(8, 8, 8, 8);
        categoryGroup.addView(radio);
    }

    // Установка выбранной кнопки по тегу
    private void setCheckedRadioButton(String tag) {
        for (int i = 0; i < categoryGroup.getChildCount(); i++) {
            View child = categoryGroup.getChildAt(i);
            if (child instanceof RadioButton) {
                RadioButton radio = (RadioButton) child;
                if (tag.equals(radio.getTag())) {
                    radio.setChecked(true);
                    break;
                }
            }
        }
    }

    public static Intent createIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(context, WidgetConfigureActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return intent;
    }
}