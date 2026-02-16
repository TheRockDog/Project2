package com.example.project2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.project2.models.AppInfo;
import com.example.project2.utils.AppManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GridView gridView;
    private AppAdapter adapter;
    private TabLayout tabLayout;
    private String currentCategory = "All";
    private MaterialButton btnAddWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настройка Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Пародия на Fences");

        // Инициализация UI
        gridView = findViewById(R.id.grid_view);
        tabLayout = findViewById(R.id.tab_layout);
        btnAddWidget = findViewById(R.id.btn_add_widget);

        // Асинхронная загрузка
        AppManager.getAllAppsAsync(this, apps -> {
            adapter = new AppAdapter(MainActivity.this, apps);
            gridView.setAdapter(adapter);
        });
        setupTabs();

        btnAddWidget.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Чтобы добавить виджет: удерживайте пустое место на рабочем столе → Виджеты → Project2",
                    Toast.LENGTH_LONG).show();
        });
    }

    private void setupTabs() {
        // Вкладки
        tabLayout.addTab(tabLayout.newTab().setText("Все"));
        tabLayout.addTab(tabLayout.newTab().setText("Игры"));
        tabLayout.addTab(tabLayout.newTab().setText("Соц. сети"));
        tabLayout.addTab(tabLayout.newTab().setText("Работа"));
        tabLayout.addTab(tabLayout.newTab().setText("Другое"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String category;
                switch (tab.getPosition()) {
                    case 0: category = "All"; break;
                    case 1: category = "Games"; break;
                    case 2: category = "Social"; break;
                    case 3: category = "Work"; break;
                    default: category = "Other";
                }

                filterApps(category);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterApps(String category) {
        currentCategory = category;

        // Прогресс
        gridView.setAdapter(null);

        AppManager.getAppsByCategoryAsync(this, category, apps -> {
            adapter = new AppAdapter(MainActivity.this, apps);
            gridView.setAdapter(adapter);
        });
    }
}