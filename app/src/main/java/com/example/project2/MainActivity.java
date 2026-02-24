package com.example.project2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.project2.utils.AppManager;
import com.example.project2.utils.CategoryManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity implements CategoryEditDialog.CategoryEditListener {

    private ViewPager2 viewPager;               // Переключение вкладок
    private ProgressBar progressBar;             // Индикатор загрузки
    private FrameLayout progressOverlay;         // Затемнение экрана
    private SectionsPagerAdapter pagerAdapter;   // Адаптер для вкладок
    private boolean isRefreshing = false;        // Флаг обновления

    // Приёмник: изменения пакетов
    private BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAppList();
        }
    };

    // Приёмник: обновление виджета
    private BroadcastReceiver packageReplacedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WidgetProvider.updateAllWidgets(MainActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настройка тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.parody_title));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));

        // Инициализация ViewPager2 и TabLayout
        viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        progressOverlay = findViewById(R.id.progress_overlay);
        progressBar = findViewById(R.id.progress_bar);

        AppManager.init(this);

        // Создание адаптера с фрагментами
        pagerAdapter = new SectionsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Привязка TabLayout к ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position))
        ).attach();

        showLoading(true);
        refreshAppList();

        // Регистрация приёмников
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);

        IntentFilter replacedFilter = new IntentFilter(Intent.ACTION_MY_PACKAGE_REPLACED);
        registerReceiver(packageReplacedReceiver, replacedFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(packageReceiver);
        unregisterReceiver(packageReplacedReceiver);
    }

    // Показ/скрытие загрузки
    private void showLoading(boolean show) {
        if (!show && isRefreshing) return;
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Обновление списка приложений
    private void refreshAppList() {
        isRefreshing = true;
        showLoading(true);
        AppManager.refreshCacheAsync(this, apps -> {
            pagerAdapter.notifyDataChanged();   // Уведомить фрагменты
            WidgetProvider.updateAllWidgets(this);
            isRefreshing = false;
            showLoading(false);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_category) {
            // Диалог создания категории
            CategoryNameDialog.newInstance(name -> {
                CategoryEditDialog.newInstanceForCreate(name)
                        .show(getSupportFragmentManager(), "edit_category");
            }).show(getSupportFragmentManager(), "name_dialog");
            return true;
        } else if (id == R.id.action_refresh) {
            refreshAppList();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Диалог "О программе"
    private void showAboutDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            dialog.setMessage(getTitle() + " версия " + versionName +
                    "\r\n\nАвтор - Браганцов Андрей Викторович, гр. ИСП-430");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        dialog.setTitle("О программе");
        dialog.setNeutralButton("OK", (dialogInterface, which) -> dialogInterface.dismiss());
        dialog.setIcon(R.mipmap.ic_launcher_round);
        dialog.show();
    }

    // Переключение на вкладку категорий
    public void switchToCategoriesTab() {
        viewPager.setCurrentItem(pagerAdapter.getCategoryTabPosition(), true);
    }

    @Override
    public void onCategoryEdited() {
        pagerAdapter.notifyCategoryFragment(); // Обновить фрагмент категорий
    }
}