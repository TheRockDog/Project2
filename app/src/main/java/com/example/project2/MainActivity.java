package com.example.project2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.project2.adapters.CategoryAdapter;
import com.example.project2.models.Category;
import com.example.project2.utils.AppManager;
import com.example.project2.utils.CategoryManager;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements CategoryEditDialog.CategoryEditListener {

    private GridView gridView;
    private ProgressBar progressBar;
    private FrameLayout progressOverlay;
    private AppAdapter appAdapter;
    private CategoryAdapter categoryAdapter;
    private TabLayout tabLayout;
    private String currentCategory = "All";
    private boolean showingCategories = false;
    private boolean isRefreshing = false;
    private GestureDetector gestureDetector;

    private BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isRefreshing = true;
            showLoading(true);
            AppManager.refreshCacheAsync(MainActivity.this, apps -> {
                if (!showingCategories) {
                    showAppsCategory(currentCategory);
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

        gridView = findViewById(R.id.grid_view);
        progressOverlay = findViewById(R.id.progress_overlay);
        progressBar = findViewById(R.id.progress_bar);
        tabLayout = findViewById(R.id.tab_layout);

        AppManager.init(this);

        setupTabs();

        showLoading(true);
        showAppsCategory("All");

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        int current = tabLayout.getSelectedTabPosition();
                        if (current > 0) {
                            TabLayout.Tab tab = tabLayout.getTabAt(current - 1);
                            if (tab != null) tab.select();
                        }
                    } else {
                        int current = tabLayout.getSelectedTabPosition();
                        if (current < tabLayout.getTabCount() - 1) {
                            TabLayout.Tab tab = tabLayout.getTabAt(current + 1);
                            if (tab != null) tab.select();
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        gridView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

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
                if (!showingCategories) {
                    showAppsCategory(currentCategory);
                }
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
            } catch (PackageManager.NameNotFoundException e) {
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

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Games"));
        tabLayout.addTab(tabLayout.newTab().setText("Social"));
        tabLayout.addTab(tabLayout.newTab().setText("Work"));
        tabLayout.addTab(tabLayout.newTab().setText("Other"));
        tabLayout.addTab(tabLayout.newTab().setText("Categories"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 5) {
                    showCategories();
                } else {
                    String category;
                    switch (tab.getPosition()) {
                        case 0: category = "All"; break;
                        case 1: category = "Games"; break;
                        case 2: category = "Social"; break;
                        case 3: category = "Work"; break;
                        default: category = "Other";
                    }
                    showAppsCategory(category);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showAppsCategory(String category) {
        showingCategories = false;
        currentCategory = category;

        AppManager.getAppsByCategoryAsync(this, category, apps -> {
            if (appAdapter == null) {
                appAdapter = new AppAdapter(MainActivity.this, apps);
            } else {
                appAdapter.updateApps(apps);
            }
            gridView.setAdapter(appAdapter);

            if (AppManager.hasCachedApps()) {
                AppManager.loadIconsFromFilesAsync(this, () -> {
                    appAdapter.notifyDataSetChanged();
                    if (!isRefreshing) showLoading(false);
                });
            } else {
                if (!isRefreshing) showLoading(false);
            }
        });
    }

    private void showCategories() {
        showingCategories = true;
        List<Category> allCategories = CategoryManager.getInstance(this).getAllCategories();
        List<Category> userCategories = allCategories.stream()
                .filter(c -> !c.isBuiltIn())
                .collect(Collectors.toList());
        if (categoryAdapter == null) {
            categoryAdapter = new CategoryAdapter(this, userCategories,
                    category -> {},
                    (category) -> {
                        if (category.isBuiltIn()) {
                            Toast.makeText(this, R.string.builtin_category_no_edit, Toast.LENGTH_SHORT).show();
                        } else {
                            showCategoryOptionsDialog(category);
                        }
                    });
        } else {
            categoryAdapter.updateCategories(userCategories);
        }
        gridView.setAdapter(categoryAdapter);
    }

    private void showCategoryOptionsDialog(Category category) {
        String[] options = {"Изменить", "Удалить"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(category.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        CategoryEditDialog.newInstance(category).show(getSupportFragmentManager(), "edit_category");
                    } else {
                        showDeleteCategoryConfirmDialog(category);
                    }
                })
                .show();
    }

    private void showDeleteCategoryConfirmDialog(Category category) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.delete_category_title)
                .setMessage(getString(R.string.delete_category_confirm, category.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    CategoryManager.getInstance(this).deleteCategory(category.getId());
                    Toast.makeText(this, R.string.category_deleted, Toast.LENGTH_SHORT).show();
                    if (showingCategories) showCategories();
                    WidgetProvider.updateAllWidgets(this);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public void switchToCategoriesTab() {
        TabLayout.Tab tab = tabLayout.getTabAt(5);
        if (tab != null) {
            tab.select();
        }
    }

    @Override
    public void onCategoryEdited() {
        if (showingCategories) {
            showCategories();
        }
    }
}