package com.example.project2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager; // Добавлен импорт
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
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.project2.adapters.CategoryPagerAdapter;
import com.example.project2.models.Category;
import com.example.project2.utils.AppManager;
import com.example.project2.utils.CategoryManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements CategoryEditDialog.CategoryEditListener {

    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private FrameLayout progressOverlay;
    private TabLayout tabLayout;
    private boolean isRefreshing = false;
    private CategoryPagerAdapter pagerAdapter;

    private BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isRefreshing = true;
            showLoading(true);
            AppManager.refreshCacheAsync(MainActivity.this, apps -> {
                if (pagerAdapter != null) {
                    pagerAdapter.notifyDataSetChanged();
                }
                WidgetProvider.updateAllWidgets(MainActivity.this);
                isRefreshing = false;
                showLoading(false);
            });
        }
    };

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.parody_title));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));

        viewPager = findViewById(R.id.view_pager);
        progressOverlay = findViewById(R.id.progress_overlay);
        progressBar = findViewById(R.id.progress_bar);
        tabLayout = findViewById(R.id.tab_layout);

        AppManager.init(this);

        pagerAdapter = new CategoryPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        String[] tabTitles = {"All", "Games", "Social", "Work", "Other", "Categories"};
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tabTitles[position])).attach();

        showLoading(true);

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

    private void showLoading(boolean show) {
        if (!show && isRefreshing) return;
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
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
            CategoryNameDialog.newInstance(name -> {
                CategoryEditDialog.newInstanceForCreate(name).show(getSupportFragmentManager(), "edit_category");
            }).show(getSupportFragmentManager(), "name_dialog");
            return true;
        } else if (id == R.id.action_refresh) {
            isRefreshing = true;
            showLoading(true);
            AppManager.refreshCacheAsync(this, apps -> {
                pagerAdapter.notifyDataSetChanged();
                isRefreshing = false;
                showLoading(false);
            });
            return true;
        } else if (id == R.id.action_about) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                dialog.setMessage(getTitle() + " версия " + versionName +
                        "\r\n\nАвтор - Браганцов Андрей Викторович, гр. ИСП-430");
            } catch (PackageManager.NameNotFoundException e) { // теперь ошибка исправлена
                e.printStackTrace();
            }
            dialog.setTitle("О программе");
            dialog.setNeutralButton("OK", (dialogInterface, which) -> dialogInterface.dismiss());
            dialog.setIcon(R.mipmap.ic_launcher_round);
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void switchToCategoriesTab() {
        viewPager.setCurrentItem(5, true);
    }

    @Override
    public void onCategoryEdited() {
        // Обработка редактирования категорий (пока пусто)
    }
}