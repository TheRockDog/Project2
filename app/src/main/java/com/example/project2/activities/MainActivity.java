package com.example.project2.activities;

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

import com.example.project2.R;
import com.example.project2.adapters.SectionsPagerAdapter;
import com.example.project2.dialogs.CategoryNameDialog;
import com.example.project2.dialogs.CategoryEditDialog;
import com.example.project2.widget.WidgetProvider;
import com.example.project2.utils.AppManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity implements CategoryEditDialog.CategoryEditListener {

    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private FrameLayout progressOverlay;
    private SectionsPagerAdapter pagerAdapter;
    private boolean isRefreshing = false;

    private BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAppList();
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
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        progressOverlay = findViewById(R.id.progress_overlay);
        progressBar = findViewById(R.id.progress_bar);

        AppManager.init(this);

        pagerAdapter = new SectionsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position))
        ).attach();

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

    private void refreshAppList() {
        if (isRefreshing) return;
        isRefreshing = true;
        showLoading(true);
        AppManager.refreshCacheAsync(this, apps -> {
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
            CategoryNameDialog.newInstance(new CategoryNameDialog.Listener() {
                @Override
                public void onNameEntered(String name) {
                    CategoryEditDialog.newInstanceForCreate(name)
                            .show(getSupportFragmentManager(), "edit_category");
                }
                @Override
                public void onCancel() {
                }
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

    public void switchToCategoriesTab() {
        viewPager.setCurrentItem(pagerAdapter.getCategoryTabPosition(), true);
    }

    @Override
    public void onCategoryEdited() {
        // Фрагмент категорий обновится через LiveData
    }
}